package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.raksrinana.ftpfetcher.cli.CLIParameters;
import fr.raksrinana.ftpfetcher.cli.Settings;
import fr.raksrinana.ftpfetcher.downloader.FTPConnection;
import fr.raksrinana.ftpfetcher.downloader.FTPFetcher;
import fr.raksrinana.ftpfetcher.downloader.ProgressBarHandler;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import fr.raksrinana.ftpfetcher.model.DownloadResult;
import fr.raksrinana.ftpfetcher.storage.H2Storage;
import fr.raksrinana.ftpfetcher.storage.IStorage;
import fr.raksrinana.ftpfetcher.storage.NoOpStorage;
import fr.raksrinana.utils.base.FileUtils;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
public class Main{
	private static ExecutorService executor;
	private static ConsoleHandler consoleHandler;
	
	public static void main(String[] args) throws IOException{
		var parameters = new CLIParameters();
		var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		
		var lockFile = Paths.get(System.getProperty("java.io.tmpdir"))
				.resolve("FTPFetcher.lock")
				.normalize()
				.toAbsolutePath();
		if(Files.exists(lockFile)){
			log.error("Program is already running, lock file {} is present", lockFile);
			System.exit(1);
		}
		Files.createFile(lockFile);
		lockFile.toFile().deleteOnExit();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(executor != null){
				executor.shutdownNow();
			}
			try{
				Files.deleteIfExists(lockFile);
			}
			catch(IOException e){
				log.error("Failed to delete lock file {}", lockFile);
			}
		}));
		consoleHandler = new ConsoleHandler();
		consoleHandler.start();
		Settings.loadSettings(parameters.getProperties()).ifPresentOrElse(settings -> {
			try(var storage = getStorage(parameters)){
				JSch.setConfig("StrictHostKeyChecking", "no");
				var jsch = new JSch();
				var knownHostsFilename = FileUtils.getHomeFolder().resolve(".ssh").resolve("known_hosts");
				jsch.setKnownHosts(knownHostsFilename.toAbsolutePath().toString());
				
				var deletedUseless = removeUselessDownloadsInDb(storage);
				log.info("Removed {} useless entries", deletedUseless);
				
				var startFetch = System.currentTimeMillis();
				var downloadSet = new LinkedList<DownloadElement>();
				for(var folderSettings : settings.getFolders()){
					try(var connection = new FTPConnection(jsch, settings)){
						downloadSet.addAll(fetchFolder(storage, connection, folderSettings.getFtpFolder(), folderSettings.getLocalFolder(), folderSettings.isRecursive(), Pattern.compile(folderSettings.getFileFilter()), folderSettings.isDeleteOnSuccess()));
					}
					catch(JSchException | SftpException e){
						if(e.getMessage().equals("No such file")){
							log.warn("Folder {} doesn't exist", folderSettings.getFtpFolder());
						}
						else{
							log.error("Error fetching folder {}", folderSettings.getFtpFolder(), e);
						}
					}
					catch(Exception e){
						log.error("Error fetching folder {}", folderSettings, e);
					}
				}
				log.info("Found {} elements to download in {}ms", downloadSet.size(), System.currentTimeMillis() - startFetch);
				if(!downloadSet.isEmpty()){
					downloadElements(parameters, jsch, settings, storage, downloadSet);
				}
			}
			catch(Exception e){
				log.error("Uncaught exception", e);
			}
		}, () -> log.error("Failed to load settings in {}", parameters.getProperties()));
		consoleHandler.close();
	}
	
	private static void downloadElements(@NotNull CLIParameters parameters, @NotNull JSch jsch, @NotNull Settings settings, @NotNull IStorage storage, @NotNull List<DownloadElement> downloadElements){
		log.info("Starting to download {} ({}) with {} downloaders", downloadElements.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadElements.stream().mapToLong(DownloadElement::getFileSize).sum()), parameters.getThreadCount());
		var startDownload = System.currentTimeMillis();
		executor = Executors.newFixedThreadPool(parameters.getThreadCount());
		var futures = new ArrayList<Future<Collection<DownloadResult>>>();
		var results = new LinkedList<DownloadResult>();
		
		var lists = split(downloadElements, parameters.getThreadCount(), DownloadElement::getFileSize);
		
		try(var closingStack = new ClosingStack()){
			var count = new AtomicInteger(0);
			lists.stream()
					.map(list -> {
						var progressBarBuilder = new ProgressBarBuilder()
								.setTaskName("Downloader #" + count.incrementAndGet())
								.setInitialMax(list.stream().mapToLong(DownloadElement::getFileSize).sum())
								.setUnit("MiB", 1048576);
						var progressBar = closingStack.add(progressBarBuilder.build());
						var progressBarHandler = new ProgressBarHandler(progressBar);
						
						var fetcher = new FTPFetcher(jsch, settings, storage, list, progressBarHandler);
						consoleHandler.addFetcher(fetcher);
						return fetcher;
					})
					.map(executor::submit)
					.forEach(futures::add);
			
			executor.shutdown();
			futures.parallelStream()
					.filter(Objects::nonNull)
					.map(f -> {
						try{
							return f.get();
						}
						catch(InterruptedException | ExecutionException e){
							log.error("Error waiting for fetcher", e);
						}
						return null;
					})
					.filter(Objects::nonNull)
					.flatMap(Collection::stream)
					.forEach(results::add);
		}
		catch(ClosingStack.ClosingException e){
			log.error("Error while closing progress bars", e);
		}
		
		var downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).collect(Collectors.toList());
		log.info("Downloaded {}/{} elements ({}) in {} (avg: {})", downloadedSuccessfully.size(), results.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadedSuccessfully.stream().mapToLong(r -> r.getElement().getFileSize()).sum()), Duration.ofMillis(System.currentTimeMillis() - startDownload), Duration.ofMillis((long) downloadedSuccessfully.stream().mapToLong(DownloadResult::getDownloadTime).average().orElse(0L)));
	}
	
	private static <T> List<SumSplitCollection<T>> split(Collection<T> elements, int partCount, Function<T, Long> propertyExtractor){
		var parts = new ArrayList<SumSplitCollection<T>>();
		IntStream.range(0, partCount).forEach(i -> parts.add(new SumSplitCollection<>(propertyExtractor)));
		
		elements.forEach(element -> parts.stream().sorted().findFirst().ifPresent(part -> part.add(element)));
		
		return parts;
	}
	
	private static int removeUselessDownloadsInDb(@NotNull IStorage storage){
		try{
			return storage.removeUseless();
		}
		catch(SQLException throwables){
			log.error("Failed to remove useless entries", throwables);
		}
		return 0;
	}
	
	@NotNull
	private static Collection<? extends DownloadElement> fetchFolder(@NotNull IStorage storage, @NotNull FTPConnection connection, @NotNull String folder, @NotNull Path outPath, boolean recursive, @NotNull Pattern fileFilter, boolean deleteOnSuccess) throws SftpException, SQLException{
		log.info("Fetching folder {}", folder);
		var array = connection.getSftpChannel().ls(folder).toArray();
		log.info("Fetched folder {}, {} elements found, verifying them", folder, array.length);
		var toDL = storage.getOnlyNotDownloaded(folder, Arrays.stream(array).map(o -> (ChannelSftp.LsEntry) o).collect(Collectors.toList())).stream()
				.sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename))
				.filter(f -> {
					if(f.getFilename().equals(".") || f.getFilename().equals("..")){
						return false;
					}
					if(f.getAttrs().isDir()){
						return true;
					}
					return true;
				}).flatMap(f -> {
					try{
						if(recursive && f.getAttrs().isDir()){
							return fetchFolder(storage, connection, folder + (folder.endsWith("/") ? "" : "/") + f.getFilename() + "/", outPath.resolve(f.getFilename()), true, fileFilter, deleteOnSuccess).stream();
						}
						if(!f.getAttrs().isDir() && fileFilter.matcher(f.getFilename()).matches()){
							return Stream.of(createDownload(folder, f, outPath, deleteOnSuccess));
						}
						return Stream.empty();
					}
					catch(Exception e){
						log.error("Error fetching folder {}", f.getLongname(), e);
					}
					return null;
				}).filter(Objects::nonNull).collect(Collectors.toList());
		log.info("Verified folder {}, {} elements to download", folder, toDL.size());
		return toDL;
	}
	
	@Nullable
	private static DownloadElement createDownload(@NotNull String folder, @NotNull ChannelSftp.LsEntry file, @NotNull Path folderOut, boolean deleteOnSuccess) throws IOException{
		var fileOut = folderOut.resolve(file.getFilename());
		if(Files.exists(fileOut)){
			return null;
		}
		Files.createDirectories(fileOut.getParent());
		return new DownloadElement(folder, file, fileOut, deleteOnSuccess, LocalDateTime.MIN);
	}
	
	@NotNull
	private static IStorage getStorage(@NotNull CLIParameters parameters) throws SQLException, IOException{
		if(Objects.isNull(parameters.getDatabasePath())){
			return new NoOpStorage();
		}
		return new H2Storage(parameters.getDatabasePath());
	}
}
