package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.*;
import fr.mrcraftcod.utils.base.FileUtils;
import fr.mrcraftcod.utils.base.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class FTPFetcher
{
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
	
	public void run(Configuration config, String folder, Path outputFolder) throws InterruptedException
	{
		JSch.setConfig("StrictHostKeyChecking", "no");
		
		Session session = null;
		ChannelSftp sftpChannel = null;
		
		try
		{
			JSch jsch = new JSch();
			String knownHostsFilename = "/home/mrcraftcod/.ssh/known_hosts";
			jsch.setKnownHosts(knownHostsFilename);
			
			session = jsch.getSession(Settings.getString("ftpUser"), Settings.getString("ftpHost"));
			session.setPassword(Settings.getString("ftpPass"));
			
			session.connect();
			
			Channel channel = session.openChannel("sftp");
			channel.connect();
			
			session.setServerAliveInterval(20000);
			sftpChannel = (ChannelSftp) channel;
			System.out.printf("Downloaded %d files\n", fetchFolder(config, sftpChannel, folder, outputFolder));
		}
		catch(JSchException | SftpException e)
		{
			Log.warning("Error downloading folder: " + e.getMessage());
		}
		catch(IOException e)
		{
			Log.error("Error opening settings", e);
		}
		finally
		{
			if(sftpChannel != null && sftpChannel.isConnected())
				sftpChannel.exit();
			if(session != null && session.isConnected())
				session.disconnect();
		}
	}
	
	private int fetchFolder(Configuration config, ChannelSftp client, String folder, Path outPath) throws InterruptedException, SftpException
	{
		AtomicInteger folders = new AtomicInteger();
		int downloaded = 0;
		System.out.format("Fetching folder %s\n", folder);
		List<ChannelSftp.LsEntry> files = Arrays.stream(client.ls(folder).toArray()).map(o -> (ChannelSftp.LsEntry) o).sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).collect(Collectors.toList());
		List<ChannelSftp.LsEntry> newFiles = files.stream().filter(f -> {
			try
			{
				if(f.getFilename().equals(".") || f.getFilename().equals(".."))
					return false;
				if(f.getAttrs().isDir())
				{
					folders.getAndIncrement();
					return true;
				}
				if(!config.isDownloaded(Paths.get(folder).resolve(f.getFilename())))
					return true;
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return false;
		}).collect(Collectors.toList());
		System.out.format("Downloading folder %s (New: %d, Total: %d)\n", folder, newFiles.size() - folders.get(), files.size());
		for(ChannelSftp.LsEntry file : newFiles)
		{
			if(file.getAttrs().isDir())
			{
				if(file.getFilename().equals(".") || file.getFilename().equals(".."))
					continue;
				downloaded += fetchFolder(config, client, folder + file.getFilename() + "/", outPath.resolve(file.getFilename()));
			}
			else
			{
				downloaded += downloadFile(config, client, folder, file, outPath.toFile()) ? 1 : 0;
			}
		}
		return downloaded;
	}
	
	private boolean downloadFile(Configuration config, ChannelSftp client, String folder, ChannelSftp.LsEntry file, File folderOut)
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
		System.out.format("|-Downloading file %s%s\n", folder, file.getFilename());
		try(FileOutputStream fos = new FileOutputStream(fileOut))
		{
			client.get(folder + file.getFilename(), fos, new ProgressMonitor());
			Files.setAttribute(Paths.get(fileOut.toURI()), "creationTime", FileTime.fromMillis(file.getAttrs().getATime() * 1000));
			config.setDownloaded(Paths.get(folder).resolve(file.getFilename()));
		}
		catch(IOException | InterruptedException | SftpException e)
		{
			System.out.println("ERR");
			Log.warning("Error downloading file: " + e.getMessage());
			fileOut.deleteOnExit();
			return false;
		}
		return fileOut.length() != 0 && fileOut.length() == file.getAttrs().getSize();
	}
}
