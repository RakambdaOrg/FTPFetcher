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
import fr.raksrinana.utils.base.FileUtils;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
public class Main{
	private static ExecutorService executor;
	private static ConsoleHandler consoleHandler;
	
	public static void main(final String[] args) throws IOException{
		final var parameters = new CLIParameters();
		final var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(final CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		
		final var lockFile = parameters.getDatabasePath()
				.resolveSibling(parameters.getDatabasePath().getFileName() + ".lock")
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
			catch(final IOException e){
				log.error("Failed to delete lock file {}", lockFile);
			}
		}));
		consoleHandler = new ConsoleHandler();
		consoleHandler.start();
		Settings.loadSettings(parameters.getProperties()).ifPresentOrElse(settings -> {
			try(final var database = new Database(parameters.getDatabasePath().toAbsolutePath())){
				JSch.setConfig("StrictHostKeyChecking", "no");
				final var jsch = new JSch();
				final var knownHostsFilename = FileUtils.getHomeFolder().resolve(".ssh").resolve("known_hosts");
				jsch.setKnownHosts(knownHostsFilename.toAbsolutePath().toString());
				final var deletedUseless = removeUselessDownloadsInDb(database);
				log.info("Removed {} useless entries", deletedUseless);
				final var startFetch = System.currentTimeMillis();
				final var downloadSet = new ConcurrentLinkedQueue<DownloadElement>();
				for(final var folderSettings : settings.getFolders()){
					try(final var connection = new FTPConnection(jsch, settings)){
						downloadSet.addAll(fetchFolder(database, connection, folderSettings.getFtpFolder(), folderSettings.getLocalFolder(), folderSettings.isRecursive(), Pattern.compile(folderSettings.getFileFilter()), folderSettings.isDeleteOnSuccess()));
					}
					catch(final JSchException | SftpException e){
						if(e.getMessage().equals("No such file")){
							log.warn("Folder {} doesn't exist", folderSettings.getFtpFolder());
						}
						else{
							log.error("Error fetching folder {}", folderSettings.getFtpFolder(), e);
						}
					}
					catch(final Exception e){
						log.error("Error fetching folder {}", folderSettings, e);
					}
				}
				log.info("Found {} elements to download in {}ms", downloadSet.size(), System.currentTimeMillis() - startFetch);
				if(!downloadSet.isEmpty()){
					downloadElements(parameters, jsch, settings, database, downloadSet);
				}
			}
			catch(final Exception e){
				log.error("Uncaught exception", e);
			}
		}, () -> log.error("Failed to load settings in {}", parameters.getProperties()));
		consoleHandler.close();
	}
	
	private static void downloadElements(final CLIParameters parameters, final JSch jsch, final Settings settings, final Database database, final Queue<DownloadElement> downloadElements){
		log.info("Starting to download {} ({}) with {} downloaders", downloadElements.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadElements.stream().mapToLong(r -> r.getSftpFile().getAttrs().getSize()).sum()), parameters.getThreadCount());
		final var startDownload = System.currentTimeMillis();
		executor = Executors.newFixedThreadPool(parameters.getThreadCount());
		List<Future<Collection<DownloadResult>>> futures = new ArrayList<>();
		final List<DownloadResult> results;
		try(final var progressBar = new ProgressBar("", downloadElements.size())){
			final var progressBarHandler = new ProgressBarHandler(progressBar);
			try{
				futures = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> {
					final var fetcher = new FTPFetcher(jsch, settings, database, downloadElements, progressBarHandler);
					consoleHandler.addFetcher(fetcher);
					return fetcher;
				}).map(executor::submit).collect(Collectors.toList());
			}
			catch(final Exception e){
				log.error("Error building fetchers", e);
			}
			executor.shutdown();
			results = futures.parallelStream().filter(Objects::nonNull).map(f -> {
				try{
					return f.get();
				}
				catch(final InterruptedException | ExecutionException e){
					log.error("Error waiting for fetcher", e);
				}
				return null;
			}).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
		}
		final var downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).collect(Collectors.toList());
		log.info("Downloaded {}/{} elements ({}) in {} (avg: {})", downloadedSuccessfully.size(), results.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadedSuccessfully.stream().mapToLong(r -> r.getElement().getSftpFile().getAttrs().getSize()).sum()), Duration.ofMillis(System.currentTimeMillis() - startDownload), Duration.ofMillis((long) downloadedSuccessfully.stream().mapToLong(DownloadResult::getDownloadTime).average().orElse(0L)));
	}
	
	private static int removeUselessDownloadsInDb(final Database database){
		try{
			return database.removeUseless();
		}
		catch(final SQLException throwables){
			log.error("Failed to remove useless entries", throwables);
		}
		return 0;
	}
	
	private static Collection<? extends DownloadElement> fetchFolder(final Database database, final FTPConnection connection, final String folder, final Path outPath, final boolean recursive, final Pattern fileFilter, final boolean deleteOnSuccess) throws SftpException, SQLException{
		log.info("Fetching folder {}", folder);
		final var array = connection.getSftpChannel().ls(folder).toArray();
		log.info("Fetched folder {}, {} elements found, verifying them", folder, array.length);
		final var toDL = database.getOnlyNotDownloaded(folder, Arrays.stream(array).map(o -> (ChannelSftp.LsEntry) o).collect(Collectors.toList())).stream().sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).filter(f -> {
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
					return fetchFolder(database, connection, folder + (folder.endsWith("/") ? "" : "/") + f.getFilename() + "/", outPath.resolve(f.getFilename()), true, fileFilter, deleteOnSuccess).stream();
				}
				if(!f.getAttrs().isDir() && fileFilter.matcher(f.getFilename()).matches()){
					return Stream.of(createDownload(folder, f, outPath, deleteOnSuccess));
				}
				return Stream.empty();
			}
			catch(final Exception e){
				log.error("Error fetching folder {}", f.getLongname(), e);
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
		log.info("Verified folder {}, {} elements to download", folder, toDL.size());
		return toDL;
	}
	
	private static DownloadElement createDownload(final String folder, final ChannelSftp.LsEntry file, final Path folderOut, final boolean deleteOnSuccess) throws IOException{
		final var fileOut = folderOut.resolve(file.getFilename());
		if(Files.exists(fileOut)){
			return null;
		}
		Files.createDirectories(fileOut.getParent());
		return new DownloadElement(folder, file, fileOut, deleteOnSuccess, LocalDateTime.MIN);
	}
}
