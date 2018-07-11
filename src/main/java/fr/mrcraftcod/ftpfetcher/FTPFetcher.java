package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class FTPFetcher implements Callable<List<DownloadResult>>
{
	
	private final Configuration config;
	private final ConcurrentLinkedQueue<DownloadElement> downloadSet;
	private final JSch jsch;
	
	public FTPFetcher(JSch jsch, Configuration config, ConcurrentLinkedQueue<DownloadElement> downloadSet)
	{
		this.jsch = jsch;
		this.config = config;
		this.downloadSet = downloadSet;
	}
	
	@Override
	public List<DownloadResult> call() throws IOException, JSchException
	{
		FTPConnection connection = new FTPConnection(jsch);
		List<DownloadResult> results = new LinkedList<>();
		
		DownloadElement element;
		while((element = downloadSet.poll()) != null)
		{
			long startDownload = System.currentTimeMillis();
			DownloadResult result = new DownloadResult(element, false);
			results.add(result);
			boolean downloaded = false;
			
			Log.info("%s - Downloading file %s%s", Thread.currentThread().getName(), element.getFolder(), element.getFile().getFilename());
			try(FileOutputStream fos = new FileOutputStream(element.getFileOut()))
			{
				connection.getClient().get(element.getFolder() + element.getFile().getFilename(), fos);
				
				setAttributes(Paths.get(element.getFileOut().toURI()), FileTime.fromMillis(element.getFile().getAttrs().getATime() * 1000L));
				
				config.setDownloaded(Paths.get(element.getFolder()).resolve(element.getFile().getFilename().replace(":", ".")));
				
				downloaded = true;
			}
			catch(IOException | InterruptedException e)
			{
				Log.warning(e, "1: %s - Error downloading file", Thread.currentThread().getName());
				element.getFileOut().deleteOnExit();
			}
			catch(SftpException e)
			{
				Log.warning(e, "2: %s - Error downloading file", Thread.currentThread().getName());
				element.getFileOut().deleteOnExit();
				if(e.getCause().getMessage().contains("inputstream is closed") || e.getCause().getMessage().contains("Pipe closed"))
					connection.reopen();
			}
			result.setDownloaded(downloaded && element.getFileOut().length() != 0 && element.getFileOut().length() == element.getFile().getAttrs().getSize());
			result.setDownloadTime(System.currentTimeMillis() - startDownload);
			
			Log.info("%s - Downloaded file in %s", Thread.currentThread().getName(), Duration.ofMillis(result.getDownloadTime()));
		}
		
		connection.close();
		return results;
	}
	
	private static void setAttributes(Path path, FileTime fileTime)
	{
		for(String attribute : Arrays.asList("creationTime", "lastAccessTime", "lastModifiedTime"))
			try
			{
				Files.setAttribute(path, attribute, fileTime);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}
}
