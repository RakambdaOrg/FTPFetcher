package fr.mrcraftcod.ftpfetcher;

import fr.mrcraftcod.utils.resources.ResourcesBase;
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
	
	private Settings()
	{
		this.properties = new Properties();
		try(InputStream is = new ResourcesBase(Main.class).getResource(() -> "", "settings.properties").openStream())
		{
			this.properties.load(is);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getString(String key)
	{
		return getInstance().properties.getProperty(key);
	}
	
	private static Settings getInstance()
	{
		if(INSTANCE == null)
			INSTANCE = new Settings();
		return INSTANCE;
	}
}
