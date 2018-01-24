package fr.mrcraftcod.ftpfetcher;

import fr.mrcraftcod.utils.base.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
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
import java.util.Date;

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
		FTPClient client = new FTPClient();
		try
		{
			client.connect(Settings.getString("ftpHost"));
			client.enterLocalPassiveMode();
			client.login(Settings.getString("ftpUser"), Settings.getString("ftpPass"));
			fetchFolder(config, client, folder, outputFolder);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				client.disconnect();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void fetchFolder(Configuration config, FTPClient client, String folder, Path outPath) throws IOException, InterruptedException
	{
		FTPFile[] files = client.listFiles(folder);
		System.out.format("Fetching folder %s (%d)\n", folder, files.length);
		for(FTPFile file : files)
		{
			if(file.isFile())
			{
				if(!config.isDownloaded(Paths.get(folder).resolve(file.getName())))
					downloadFile(config, client, folder, file, outPath.toFile());
				// else
				// 	System.out.format("File %s/%s is already downloaded\n", folder, file.getName());
			}
			else if(file.isDirectory())
			{
				if(file.getName().equals(".") || file.getName().equals(".."))
					continue;
				fetchFolder(config, client, folder + file.getName() + "/", outPath.resolve(file.getName()));
			}
		}
	}
	
	private void downloadFile(Configuration config, FTPClient client, String folder, FTPFile file, File folderOut)
	{
		String date;
		try
		{
			date = dateFormatter.format(new Date(Long.parseLong(file.getName().substring(0, file.getName().indexOf("."))) * 1000));
		}
		catch(NumberFormatException e)
		{
			date = OffsetDateTime.parse(file.getName().substring(0, file.getName().indexOf("."))).format(dateTimeFormatter);
		}
		File fileOut = new File(folderOut, date + ".png");
		FileUtils.createDirectories(fileOut);
		System.out.format("Downloading file %s%s\n", folder, file.getName());
		try(FileOutputStream fos = new FileOutputStream(fileOut))
		{
			if(client.retrieveFile(folder + file.getName(), fos))
			{
				Files.setAttribute(Paths.get(fileOut.toURI()), "creationTime", FileTime.fromMillis(file.getTimestamp().getTimeInMillis()));
				config.setDownloaded(Paths.get(folder).resolve(file.getName()));
			}
		}
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
