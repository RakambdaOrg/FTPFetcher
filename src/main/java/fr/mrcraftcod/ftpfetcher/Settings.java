package fr.mrcraftcod.ftpfetcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 24/01/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-01-24
 */
class Settings{
	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	private static final String DEFAULT_NAME = "./FTPFetcher.json";
	private final JSONObject json;
	private static Settings INSTANCE = null;
	
	private Settings(final String name) throws IOException{
		json = new JSONObject(String.join("\n", Files.readAllLines(Paths.get(name))));
	}
	
	static String getString(final String key) throws IOException{
		return getInstance().json.getString(key);
	}
	
	private static Settings getInstance() throws IOException{
		return getInstance(DEFAULT_NAME);
	}
	
	static Settings getInstance(final String name) throws IOException{
		if(INSTANCE == null){
			INSTANCE = new Settings(name);
		}
		return INSTANCE;
	}
	
	public static JSONArray getArray(String key) throws IOException{
		return getInstance(DEFAULT_NAME).json.getJSONArray(key);
	}
}
