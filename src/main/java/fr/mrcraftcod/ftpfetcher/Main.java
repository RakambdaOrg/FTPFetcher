package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.FileUtils;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Main{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
	
	public static void main(final String[] args) throws IOException, InterruptedException, ClassNotFoundException{
		final Path lockFile = Paths.get(".ftpFetcher.lock").normalize().toAbsolutePath();
		if(lockFile.toFile().exists()){
			LOGGER.error("Program is already running, lock file {} is present", lockFile.toFile());
			System.exit(1);
		}
		touch(lockFile.toFile());
		final Configuration config = new Configuration();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			config.close();
			LOGGER.info("Program shutting down, removing lock file");
			FileUtils.forceDelete(lockFile.toFile());
		}));
		
		try{
			final Options options = new Options();
			
			final Option input = new Option("o", "options", true, "the settings properties");
			input.setRequired(true);
			options.addOption(input);
			
			final Option output = new Option("t", "threads", true, "the number of threads to use");
			output.setRequired(false);
			options.addOption(output);
			
			final CommandLineParser parser = new DefaultParser();
			final HelpFormatter formatter = new HelpFormatter();
			CommandLine cmd = null;
			
			try{
				cmd = parser.parse(options, args);
			}
			catch(final ParseException e){
				LOGGER.warn(e.getMessage());
				formatter.printHelp("FTPFetcher", options);
				
				System.exit(1);
			}
			
			Settings.getInstance(cmd.getOptionValue("options"));
			
			JSch.setConfig("StrictHostKeyChecking", "no");
			
			final JSch jsch = new JSch();
			final File knownHostsFilename = FileUtils.getHomeFolder(".ssh/known_hosts");
			jsch.setKnownHosts(knownHostsFilename.getAbsolutePath());
			
			config.removeUseless();
			
			final long startFetch = System.currentTimeMillis();
			
			final FTPConnection connection = new FTPConnection(jsch);
			final ConcurrentLinkedQueue<DownloadElement> downloadSet = new ConcurrentLinkedQueue<>(fetchFolder(config, connection, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder"))));
			connection.close();
			LOGGER.info("Found {} elements to download in {}ms", downloadSet.size(), System.currentTimeMillis() - startFetch);
			
			final int fetchers = Integer.parseInt(cmd.getOptionValue("threads", "1"));
			
			LOGGER.info("Starting with {} downloaders", fetchers);
			
			final long startDownload = System.currentTimeMillis();
			final ExecutorService executor = Executors.newFixedThreadPool(fetchers);
			List<Future<List<DownloadResult>>> futures = new ArrayList<>();
			
			try{
				final Configuration finalConfig = config;
				futures = IntStream.range(0, fetchers).mapToObj(i -> new FTPFetcher(jsch, finalConfig, downloadSet)).map(executor::submit).collect(Collectors.toList());
			}
			catch(final Exception e){
				LOGGER.error("Error building fetchers", e);
			}
			
			executor.shutdown();
			final List<DownloadResult> results = futures.parallelStream().map(f -> {
				try{
					return f.get();
				}
				catch(InterruptedException | ExecutionException e){
					LOGGER.error("Error waiting for fetcher", e);
				}
				return null;
			}).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
			
			final List<DownloadResult> downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).collect(Collectors.toList());
			
			LOGGER.info("Downloaded {}/{} elements ({}) in {} (avg: {})", downloadedSuccessfully.size(), results.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadedSuccessfully.stream().mapToLong(r -> r.getElement().getFile().getAttrs().getSize()).sum()), Duration.ofMillis(System.currentTimeMillis() - startDownload), Duration.ofMillis((long) downloadedSuccessfully.stream().mapToLong(DownloadResult::getDownloadTime).average().orElse(0L)));
		}
		catch(final Exception e){
			LOGGER.error("Uncaught exception", e);
		}
	}
	
	private static Collection<? extends DownloadElement> fetchFolder(final Configuration config, final FTPConnection connection, final String folder, final Path outPath) throws SftpException{
		LOGGER.info("Fetching folder {}", folder);
		
		return Arrays.stream(connection.getClient().ls(folder).toArray()).map(o -> (ChannelSftp.LsEntry) o).sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).filter(f -> {
			try{
				if(f.getFilename().equals(".") || f.getFilename().equals("..")){
					return false;
				}
				if(f.getAttrs().isDir()){
					return true;
				}
				if(!config.isDownloaded(Paths.get(folder).resolve(f.getFilename().replace(":", ".")))){
					return true;
				}
			}
			catch(InterruptedException e){
				LOGGER.error("", e);
			}
			return false;
		}).flatMap(f -> {
			try{
				if(f.getAttrs().isDir()){
					return fetchFolder(config, connection, f.getFilename() + "/", outPath.resolve(f.getFilename())).stream();
				}
				return Stream.of(downloadFile(folder, f, outPath.toFile()));
			}
			catch(SftpException e){
				LOGGER.error("Error fetching folder {}", f.getLongname(), e);
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	private static DownloadElement downloadFile(final String folder, final ChannelSftp.LsEntry file, final File folderOut){
		String date;
		try{
			date = dateFormatter.format(new Date(Long.parseLong(file.getFilename().substring(0, file.getFilename().indexOf("."))) * 1000));
		}
		catch(final NumberFormatException e){
			date = OffsetDateTime.parse(file.getFilename().substring(0, file.getFilename().indexOf("."))).format(dateTimeFormatter);
		}
		final File fileOut = new File(folderOut, date + file.getFilename().substring(file.getFilename().lastIndexOf(".")));
		if(fileOut.exists()){
			return null;
		}
		FileUtils.createDirectories(fileOut);
		
		return new DownloadElement(folder, file, fileOut);
	}
	
	public static void touch(final File file) throws IOException{
		if(!file.exists()){
			new FileOutputStream(file).close();
		}
	}
}
