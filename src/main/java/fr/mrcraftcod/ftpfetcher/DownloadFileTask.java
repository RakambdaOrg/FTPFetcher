package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.FileUtils;
import fr.mrcraftcod.utils.base.Log;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/03/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-03-09
 */
public class DownloadFileTask extends Task<Boolean>
{
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
	private final ChannelSftp client;
	private final String folder;
	private final ChannelSftp.LsEntry file;
	private final File folderOut;
	private final MainApplication parent;
	
	public DownloadFileTask(MainApplication parent, ChannelSftp client, String folder, ChannelSftp.LsEntry file, File folderOut)
	{
		this.parent = parent;
		this.client = client;
		this.folder = folder;
		this.file = file;
		this.folderOut = folderOut;
	}
	
	@Override
	protected Boolean call() throws Exception
	{
		if(Thread.interrupted() || this.isCancelled())
		{
			updateValue(false);
			return false;
		}
		final long tid = Thread.currentThread().getId();
		Platform.runLater(() -> parent.bindProgressBar(tid, this));
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
		updateMessage(String.format("Downloading file %s", file.getFilename()));
		try(FileOutputStream fos = new FileOutputStream(fileOut))
		{
			client.get(folder + file.getFilename(), fos, new DownloadProgressMonitor(this));
			Files.setAttribute(Paths.get(fileOut.toURI()), "creationTime", FileTime.fromMillis(file.getAttrs().getATime() * 1000));
		}
		catch(IOException | SftpException e)
		{
			Log.warning("Error downloading file: " + e.getMessage(), e);
			updateValue(false);
			return false;
		}
		boolean result = fileOut.length() == file.getAttrs().getSize();
		updateValue(result);
		return result;
	}
	
	public void updateDownloaded(long count, long max)
	{
		super.updateProgress(count, max);
	}
	
	public Path getRelativePath()
	{
		return Paths.get(folder).resolve(file.getFilename());
	}
	
	public static List<DownloadFileTask> fetchFolder(MainApplication parent, Configuration config, ChannelSftp client, String folder, Path outPath)
	{
		List<DownloadFileTask> toDownload = new ArrayList<>();
		try
		{
			List<ChannelSftp.LsEntry> files = Arrays.stream(client.ls(folder).toArray()).map(o -> (ChannelSftp.LsEntry) o).sorted(Comparator.comparing(ChannelSftp.LsEntry::getFilename)).collect(Collectors.toList());
			List<ChannelSftp.LsEntry> newFiles = files.stream().filter(f -> {
				try
				{
					if(f.getFilename().equals(".") || f.getFilename().equals(".."))
						return false;
					if(f.getAttrs().isDir())
					{
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
			for(ChannelSftp.LsEntry file : newFiles)
			{
				if(file.getAttrs().isDir())
				{
					toDownload.addAll(fetchFolder(parent, config, client, folder + file.getFilename() + "/", outPath.resolve(file.getFilename())));
				}
				else
				{
					toDownload.add(new DownloadFileTask(parent, client, folder, file, outPath.toFile()));
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return toDownload;
	}
}
