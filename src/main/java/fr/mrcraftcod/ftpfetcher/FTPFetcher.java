package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.mrcraftcod.utils.base.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
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
	
	public FTPFetcher(Configuration config, ConcurrentLinkedQueue<DownloadElement> downloadSet)
	{
		this.config = config;
		this.downloadSet = downloadSet;
	}
	
	@Override
	public List<DownloadResult> call() throws IOException, JSchException
	{
		FTPConnection connection = new FTPConnection();
		List<DownloadResult> results = new LinkedList<>();
		
		DownloadElement element;
		while((element = downloadSet.poll()) != null)
		{
			DownloadResult result = new DownloadResult(element, false);
			results.add(result);
			boolean downloaded = false;
			
			Log.info(String.format("%s - Downloading file %s%s", Thread.currentThread().getName(), element.getFolder(), element.getFile().getFilename()));
			try(FileOutputStream fos = new FileOutputStream(element.getFileOut()))
			{
				connection.getClient().get(element.getFolder() + element.getFile().getFilename(), fos, new ProgressMonitor());
				
				setAttributes(Paths.get(element.getFileOut().toURI()), FileTime.fromMillis(element.getFile().getAttrs().getATime() * 1000L));
				
				config.setDownloaded(Paths.get(element.getFolder()).resolve(element.getFile().getFilename().replace(":", ".")));
				
				downloaded = true;
			}
			catch(IOException | InterruptedException | SftpException e)
			{
				Log.warning("Error downloading file: " + e.getMessage());
				element.getFileOut().deleteOnExit();
			}
			result.setDownloaded(downloaded && element.getFileOut().length() != 0 && element.getFileOut().length() == element.getFile().getAttrs().getSize());
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
