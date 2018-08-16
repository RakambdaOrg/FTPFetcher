package fr.mrcraftcod.ftpfetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class Settings
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String DEFAULT_NAME = "./FTPFetcher.properties";
	private final Properties properties;
	private static Settings INSTANCE = null;
	
	private Settings(final String name) throws IOException
	{
		this.properties = new Properties();
		final InputStream is = new FileInputStream(new File(name));
		try(is)
		{
			this.properties.load(is);
		}
		catch(final IOException e)
		{
			LOGGER.error("Error reading configuration file");
		}
	}
	
	static String getString(final String key) throws IOException
	{
		return getInstance().properties.getProperty(key);
	}
	
	private static Settings getInstance() throws IOException
	{
		return getInstance(DEFAULT_NAME);
	}
	
	static Settings getInstance(final String name) throws IOException
	{
		if(INSTANCE == null)
			INSTANCE = new Settings(name);
		return INSTANCE;
	}
}
