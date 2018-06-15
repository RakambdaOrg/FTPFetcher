package fr.mrcraftcod.ftpfetcher;

import fr.mrcraftcod.utils.resources.ResourcesBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 24/01/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-01-24
 */
public class Settings
{
	private final Properties properties;
	private static Settings INSTANCE = null;
	
	private Settings() throws IOException
	{
		this.properties = new Properties();
		InputStream is = new File(".", "FTPFetcher.properties").exists() ? new FileInputStream(new File(".", "FTPFetcher.properties")) : new ResourcesBase(Main.class).getResource(() -> "", "FTPFetcher.properties").openStream();
		try(is)
		{
			this.properties.load(is);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getString(String key) throws IOException
	{
		return getInstance().properties.getProperty(key);
	}
	
	private static Settings getInstance() throws IOException
	{
		if(INSTANCE == null)
			INSTANCE = new Settings();
		return INSTANCE;
	}
}
