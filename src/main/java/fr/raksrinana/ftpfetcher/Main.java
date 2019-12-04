package fr.raksrinana.ftpfetcher;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.raksrinana.ftpfetcher.settings.Settings;
import fr.raksrinana.utils.base.FileUtils;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class Main{
	private static final DateFormat outDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter outDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ssZ");
	private static ExecutorService executor;
	
	public static void main(final String[] args) throws IOException{
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
		}
		catch(final ParameterException e){
			log.error("Failed to parse arguments", e);
			e.usage();
			return;
		}
		final var lockFile = parameters.getDatabasePath().resolveSibling(parameters.getDatabasePath().getFileName() + ".lock").normalize().toAbsolutePath();
		if(lockFile.toFile().exists()){
			log.error("Program is already running, lock file {} is present", lockFile.toFile());
			System.exit(1);
		}
		touch(lockFile.toFile());
		lockFile.toFile().deleteOnExit();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(executor != null){
				executor.shutdownNow();
			}
		}));
		Settings.loadSettings(parameters.getProperties()).ifPresentOrElse(settings -> {
			try(final var config = new Configuration(parameters.getDatabasePath().toAbsolutePath())){
				JSch.setConfig("StrictHostKeyChecking", "no");
				final var jsch = new JSch();
				final var knownHostsFilename = FileUtils.getHomeFolder().resolve(".ssh").resolve("known_hosts");
				jsch.setKnownHosts(knownHostsFilename.toAbsolutePath().toString());
				log.info("Removed {} useless entries", config.removeUseless());
				final var startFetch = System.currentTimeMillis();
				final var downloadSet = new ConcurrentLinkedQueue<DownloadElement>();
				for(final var folderFetchObj : settings.getFolders()){
					try{
						final var connection = new FTPConnection(jsch, settings);
						downloadSet.addAll(fetchFolder(config, connection, folderFetchObj.getFtpFolder(), folderFetchObj.getLocalFolder(), folderFetchObj.isRecursive(), Pattern.compile(folderFetchObj.getFileFilter())));
						connection.close();
					}
					catch(final JSchException | SftpException e){
						if(e.getMessage().equals("No such file")){
							log.warn("Folder {} doesn't exist", folderFetchObj.getFtpFolder());
						}
						else{
							log.error("Error fetching folder {}", folderFetchObj.getFtpFolder(), e);
						}
					}
					catch(final Exception e){
						log.error("Error fetching folder {}", folderFetchObj, e);
					}
				}
				log.info("Found {} elements to download in {}ms", downloadSet.size(), System.currentTimeMillis() - startFetch);
				if(!downloadSet.isEmpty()){
					log.info("Starting to download {} ({}) with {} downloaders", downloadSet.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadSet.stream().mapToLong(r -> r.getFile().getAttrs().getSize()).sum()), parameters.getThreadCount());
					final var startDownload = System.currentTimeMillis();
					executor = Executors.newFixedThreadPool(parameters.getThreadCount());
					List<Future<List<DownloadResult>>> futures = new ArrayList<>();
					final List<DownloadResult> results;
					try(final var progressBar = new ProgressBar("", downloadSet.size())){
						final var progressBarHandler = new ProgressBarHandler(progressBar);
						try{
							futures = IntStream.range(0, parameters.getThreadCount()).mapToObj(i -> new FTPFetcher(jsch, settings, config, downloadSet, progressBarHandler)).map(executor::submit).collect(Collectors.toList());
						}
						catch(final Exception e){
							log.error("Error building fetchers", e);
						}
						executor.shutdown();
						results = futures.parallelStream().map(f -> {
							try{
								return f.get();
							}
							catch(InterruptedException | ExecutionException e){
								log.error("Error waiting for fetcher", e);
							}
							return null;
						}).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
					}
					final var downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).collect(Collectors.toList());
					log.info("Downloaded {}/{} elements ({}) in {} (avg: {})", downloadedSuccessfully.size(), results.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadedSuccessfully.stream().mapToLong(r -> r.getElement().getFile().getAttrs().getSize()).sum()), Duration.ofMillis(System.currentTimeMillis() - startDownload), Duration.ofMillis((long) downloadedSuccessfully.stream().mapToLong(DownloadResult::getDownloadTime).average().orElse(0L)));
				}
			}
			catch(final Exception e){
				log.error("Uncaught exception", e);
			}
		}, () -> log.error("Failed to load settings in {}", parameters.getProperties()));
	}
	
	private static Collection<? extends DownloadElement> fetchFolder(final Configuration config, final FTPConnection connection, final String folder, final Path outPath, final boolean recursive, final Pattern fileFilter) throws SftpException, InterruptedException, TimeoutException, ExecutionException{
		log.info("Fetching folder {}", folder);
		final var array = connection.getClient().ls(folder).toArray();
		log.info("Fetched folder {}, {} elements found, verifying them", folder, array.length);
		final var toDL = config.getOnlyNotDownloaded(folder, Arrays.stream(array).map(o -> (ChannelSftp.LsEntry) o).collect(Collectors.toList())).stream().sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).filter(f -> {
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
					return fetchFolder(config, connection, folder + (folder.endsWith("/") ? "" : "/") + f.getFilename() + "/", outPath.resolve(f.getFilename()), true, fileFilter).stream();
				}
				if(!f.getAttrs().isDir() && fileFilter.matcher(f.getFilename()).matches()){
					return Stream.of(downloadFile(folder, f, outPath));
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
	
	private static void touch(final File file) throws IOException{
		if(!file.exists()){
			new FileOutputStream(file).close();
		}
	}
	
	private static DownloadElement downloadFile(final String folder, final ChannelSftp.LsEntry file, final Path folderOut) throws IOException{
		final String date;
		final var datePart = file.getFilename().substring(0, file.getFilename().lastIndexOf("."));
		try{
			if(datePart.chars().allMatch(Character::isDigit)){
				date = outDateFormatter.format(new Date(Long.parseLong(datePart) * 1000));
			}
			else{
				date = OffsetDateTime.from(dateTimeFormatter.parse(datePart)).format(outDateTimeFormatter);
			}
		}
		catch(final NumberFormatException e){
			log.error("Error parsing filename {}", datePart);
			return null;
		}
		final var fileOut = folderOut.resolve(date + file.getFilename().substring(file.getFilename().lastIndexOf(".")));
		if(fileOut.toFile().exists()){
			return null;
		}
		Files.createDirectories(fileOut.getParent());
		return new DownloadElement(folder, file, fileOut);
	}
}
