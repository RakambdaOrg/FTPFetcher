package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.FileUtils;
import fr.mrcraftcod.utils.base.Log;
import org.apache.commons.cli.*;
import java.io.File;
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
import java.util.stream.Stream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Main
{
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
	
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException, IOException, JSchException, SftpException
	{
		Options options = new Options();
		
		Option input = new Option("o", "options", true, "the settings properties");
		input.setRequired(true);
		options.addOption(input);
		
		Option output = new Option("t", "threads", false, "the number of threads to use");
		output.setRequired(true);
		options.addOption(output);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		
		try
		{
			cmd = parser.parse(options, args);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
			formatter.printHelp("FTPFetcher", options);
			
			System.exit(1);
		}
		
		Settings.getInstance(cmd.getOptionValue("threads"));
		
		JSch.setConfig("StrictHostKeyChecking", "no");
		
		JSch jsch = new JSch();
		File knownHostsFilename = FileUtils.getHomeFolder(".ssh/known_hosts");
		jsch.setKnownHosts(knownHostsFilename.getAbsolutePath());
		
		Configuration config = new Configuration();
		config.removeUseless();
		
		long startFetch = System.currentTimeMillis();
		
		FTPConnection connection = new FTPConnection(jsch);
		ConcurrentLinkedQueue<DownloadElement> downloadSet = new ConcurrentLinkedQueue<>(fetchFolder(config, connection, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder"))));
		connection.close();
		Log.info("Found %d elements to download in %dms", downloadSet.size(), System.currentTimeMillis() - startFetch);
		
		long startDownload = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(cmd.getOptionValue("threads", "1")));
		final List<Future<List<DownloadResult>>> futures = new ArrayList<>();
		
		try
		{
			futures.add(executor.submit(new FTPFetcher(jsch, config, downloadSet)));
			futures.add(executor.submit(new FTPFetcher(jsch, config, downloadSet)));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		executor.shutdown();
		List<DownloadResult> results = futures.parallelStream().map(f -> {
			try
			{
				return f.get();
			}
			catch(InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
			return null;
		}).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
		
		List<DownloadResult> downloadedSuccessfully = results.stream().filter(DownloadResult::isDownloaded).collect(Collectors.toList());
		
		Log.info("Downloaded %d/%d elements (%s) in %s (avg: %s)", downloadedSuccessfully.size(), results.size(), org.apache.commons.io.FileUtils.byteCountToDisplaySize(downloadedSuccessfully.stream().mapToLong(r -> r.getElement().getFile().getAttrs().getSize()).sum()), Duration.ofMillis(System.currentTimeMillis() - startDownload), Duration.ofMillis((long) downloadedSuccessfully.stream().mapToLong(DownloadResult::getDownloadTime).average().orElse(-1L)));
	}
	
	private static Collection<? extends DownloadElement> fetchFolder(Configuration config, FTPConnection connection, String folder, Path outPath) throws InterruptedException, SftpException
	{
		ArrayList<DownloadElement> downloadElements = new ArrayList<>();
		Log.info("%s - Fetching folder %s", Thread.currentThread().getName(), folder);
		
		return Arrays.stream(connection.getClient().ls(folder).toArray()).map(o -> (ChannelSftp.LsEntry) o).sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).filter(f -> {
			try
			{
				if(f.getFilename().equals(".") || f.getFilename().equals(".."))
					return false;
				if(f.getAttrs().isDir())
					return true;
				if(!config.isDownloaded(Paths.get(folder).resolve(f.getFilename().replace(":", "."))))
					return true;
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return false;
		}).flatMap(f -> {
			try
			{
				if(f.getAttrs().isDir())
					return fetchFolder(config, connection, f.getFilename() + "/", outPath.resolve(f.getFilename())).stream();
				return Stream.of(downloadFile(config, folder, f, outPath.toFile()));
			}
			catch(InterruptedException | SftpException e)
			{
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList());
	}
	
	private static DownloadElement downloadFile(Configuration config, String folder, ChannelSftp.LsEntry file, File folderOut)
	{
		String date;
		try
		{
			date = dateFormatter.format(new Date(Long.parseLong(file.getFilename().substring(0, file.getFilename().indexOf("."))) * 1000));
		}
		catch(NumberFormatException e)
		{
			date = OffsetDateTime.parse(file.getFilename().substring(0, file.getFilename().indexOf("."))).format(dateTimeFormatter);
		}
		File fileOut = new File(folderOut, date + file.getFilename().substring(file.getFilename().lastIndexOf(".")));
		FileUtils.createDirectories(fileOut);
		
		return new DownloadElement(folder, file, fileOut);
	}
}
