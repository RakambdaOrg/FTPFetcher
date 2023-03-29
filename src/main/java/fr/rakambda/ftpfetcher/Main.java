package fr.rakambda.ftpfetcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.rakambda.ftpfetcher.cli.CLIParameters;
import fr.rakambda.ftpfetcher.cli.Settings;
import fr.rakambda.ftpfetcher.downloader.FTPConnection;
import fr.rakambda.ftpfetcher.downloader.FTPFetcher;
import fr.rakambda.ftpfetcher.downloader.ProgressBarHandler;
import fr.rakambda.ftpfetcher.model.DownloadElement;
import fr.rakambda.ftpfetcher.model.DownloadResult;
import fr.rakambda.ftpfetcher.storage.IStorage;
import fr.rakambda.ftpfetcher.storage.NoOpStorage;
import fr.rakambda.ftpfetcher.storage.database.H2Storage;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarBuilder;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
				var deletedUseless = removeUselessDownloadsInDb(storage);
				log.info("Removed {} useless entries", deletedUseless);
				
				var startFetch = System.currentTimeMillis();
				var downloadSet = new LinkedList<DownloadElement>();
				for(var folderSettings : settings.getFolders()){
					try(var connection = new FTPConnection(settings)){
						var permissions = Optional.ofNullable(folderSettings.getFilePermissions()).map(PosixFilePermissions::fromString).orElse(null);
						downloadSet.addAll(fetchFolder(storage, connection, folderSettings.getFtpFolder(), folderSettings.getLocalFolder(), folderSettings.isRecursive(), Pattern.compile(folderSettings.getFileFilter()), folderSettings.isDeleteOnSuccess(), permissions));
					}
					catch(IOException e){
						log.error("Error fetching folder {}", folderSettings.getFtpFolder(), e);
					}
					catch(Exception e){
						log.error("Error fetching folder {}", folderSettings, e);
					}
				}
				log.info("Found {} elements to download in {}ms", downloadSet.size(), System.currentTimeMillis() - startFetch);
				if(!downloadSet.isEmpty()){
					downloadElements(parameters, settings, storage, downloadSet);
				}
			}
			catch(Exception e){
				log.error("Uncaught exception", e);
			}
		}, () -> log.error("Failed to load settings in {}", parameters.getProperties()));
		consoleHandler.close();
	}
	
	private static void downloadElements(@NotNull CLIParameters parameters, @NotNull Settings settings, @NotNull IStorage storage, @NotNull List<DownloadElement> downloadElements){
		log.info("Starting to download {} ({}) with {} downloaders", downloadElements.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadElements.stream().mapToLong(DownloadElement::getFileSize).sum()), parameters.getThreadCount());
		var startDownload = System.currentTimeMillis();
		var futures = new ArrayList<Future<Collection<DownloadResult>>>();
		var results = new LinkedList<DownloadResult>();
		var partitions = Math.min(parameters.getThreadCount(), downloadElements.size());
		
		executor = Executors.newFixedThreadPool(partitions);
		
		var lists = split(downloadElements, partitions, DownloadElement::getFileSize);
		
		try(var closingStack = new ClosingStack()){
			var count = new AtomicInteger(0);
			lists.stream()
					.map(list -> {
						var progressBarBuilder = new ProgressBarBuilder()
								.setTaskName("Downloader #" + count.incrementAndGet())
								.setInitialMax(list.stream().mapToLong(DownloadElement::getFileSize).sum())
								.setConsumer(new InteractiveConsoleProgressBarConsumer(new PrintStream(new FileOutputStream(FileDescriptor.err)), -1))
								.setUnit("MiB", 1048576);
						var progressBar = closingStack.add(progressBarBuilder.build());
						var progressBarHandler = new ProgressBarHandler(progressBar);
						
						var fetcher = new FTPFetcher(settings, storage, list, progressBarHandler, parameters.getBytesPerSecond());
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
		
		var downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).toList();
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
	private static Collection<? extends DownloadElement> fetchFolder(@NotNull IStorage storage, @NotNull FTPConnection connection, @NotNull String folder, @NotNull Path outPath, boolean recursive, @NotNull Pattern fileFilter, boolean deleteOnSuccess, Set<PosixFilePermission> permissions) throws SQLException, IOException{
		log.info("Fetching folder {}", folder);
		if(Objects.isNull(connection.getSftp().statExistence(folder))){
			log.warn("Input path {} does not exists", folder);
			return List.of();
		}
		var files = connection.getSftp().ls(folder);
		log.info("Fetched folder {}, {} elements found, verifying them", folder, files.size());
		var toDL = storage.getOnlyNotDownloaded(folder, files).stream().sorted(Comparator.comparing(RemoteResourceInfo::getName)).filter(f -> {
			if(f.getName().equals(".") || f.getName().equals("..")){
				return false;
			}
			if(f.isDirectory()){
				return true;
			}
			return true;
		}).flatMap(f -> {
			try{
				if(recursive && f.isDirectory()){
					return fetchFolder(storage, connection, folder + (folder.endsWith("/") ? "" : "/") + f.getName() + "/", outPath.resolve(f.getName()), true, fileFilter, deleteOnSuccess, permissions).stream();
				}
				if(!f.isDirectory() && fileFilter.matcher(f.getName()).matches()){
					return Stream.of(createDownload(folder, f, outPath, deleteOnSuccess, permissions));
				}
				return Stream.empty();
			}
			catch(Exception e){
				log.error("Error fetching folder {}", f.getPath(), e);
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
		log.info("Verified folder {}, {} elements to download", folder, toDL.size());
		return toDL;
	}
	
	@Nullable
	private static DownloadElement createDownload(@NotNull String folder, @NotNull RemoteResourceInfo file, @NotNull Path folderOut, boolean deleteOnSuccess, @Nullable Set<PosixFilePermission> permissions) throws IOException{
		var fileOut = folderOut.resolve(file.getName());
		if(Files.exists(fileOut)){
			return null;
		}
		createDirectoryWithPermission(fileOut.getParent(), permissions);
		return new DownloadElement(folder, file, fileOut, deleteOnSuccess, LocalDateTime.MIN, permissions);
	}
	
	private static void createDirectoryWithPermission(@Nullable Path path, Set<PosixFilePermission> permissions) throws IOException{
		if(Objects.isNull(path) || Files.exists(path)){
			return;
		}
		createDirectoryWithPermission(path.getParent(), permissions);
		
		log.debug("Creating directory {}", path.toAbsolutePath());
		var attributes = Optional.ofNullable(permissions).map(PosixFilePermissions::asFileAttribute).stream().toArray(FileAttribute[]::new);
		Files.createDirectory(path, attributes);
	}
	
	@NotNull
	private static IStorage getStorage(@NotNull CLIParameters parameters) throws SQLException{
		if(Objects.isNull(parameters.getDatabasePath())){
			return new NoOpStorage();
		}
		
		var h2 = new H2Storage(createH2Datasource(parameters.getDatabasePath()));
		h2.initDatabase();
		return h2;
	}
	
	@NotNull
	private static HikariDataSource createH2Datasource(@NotNull Path path){
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.h2.Driver");
		config.setJdbcUrl("jdbc:h2:" + path.toAbsolutePath());
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.setMaximumPoolSize(1);
		return new HikariDataSource(config);
	}
}
