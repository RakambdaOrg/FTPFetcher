package fr.mrcraftcod.ftpfetcher;

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
	public static final String DEFAULT_NAME = "./FTPFetcher.properties";
	private final Properties properties;
	private static Settings INSTANCE = null;
	
	private Settings(String name) throws IOException
	{
		this.properties = new Properties();
		InputStream is = new FileInputStream(new File(name));
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
	
	public static Settings getInstance() throws IOException
	{
		return getInstance(DEFAULT_NAME);
	}
	
	public static Settings getInstance(String name) throws IOException
	{
		if(INSTANCE == null)
			INSTANCE = new Settings(name);
		return INSTANCE;
	}
}
