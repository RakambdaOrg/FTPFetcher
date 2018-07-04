package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
		if(args.length > 0)
			Settings.getInstance(args[0]);
		
		Configuration config = new Configuration();
		config.removeUseless();
		
		FTPConnection connection = new FTPConnection();
		ConcurrentLinkedQueue<DownloadElement> downloadSet = new ConcurrentLinkedQueue<>(fetchFolder(config, connection, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder"))));
		connection.close();
		
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<List<DownloadResult>>> futures = new ArrayList<>();
		
		try
		{
			executor.submit(new FTPFetcher(config, downloadSet));
			executor.submit(new FTPFetcher(config, downloadSet));
			executor.submit(new FTPFetcher(config, downloadSet));
			executor.submit(new FTPFetcher(config, downloadSet));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		executor.shutdown();
		List<DownloadResult> results = futures.stream().map(f -> {
			try
			{
				return f.get();
			}
			catch(InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
			return null;
		}).flatMap(Collection::stream).collect(Collectors.toList());
		
		System.out.format("Downloaded %d/%d elements\n", results.stream().filter(DownloadResult::isDownloaded).count(), results.size());
	}
	
	private static Collection<? extends DownloadElement> fetchFolder(Configuration config, FTPConnection connection, String folder, Path outPath) throws InterruptedException, SftpException
	{
		ArrayList<DownloadElement> downloadElements = new ArrayList<>();
		System.out.format("%s - Fetching folder %s\n", Thread.currentThread().getName(), folder);
		
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
