package fr.mrcraftcod.ftpfetcher;

import com.mashape.unirest.http.exceptions.UnirestException;
import fr.mrcraftcod.utils.http.RequestHandler;
import fr.mrcraftcod.utils.http.requestssenders.get.StringGetRequestSender;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class APIFetcher
{
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private static final DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	class APIFile
	{
		private final String username;
		private final String path;
		private final String type;
		private final String date;
		private final int ID;
		
		public APIFile(int ID, String username, String path, String type, String date)
		{
			this.username = username;
			this.path = path;
			this.type = type;
			this.date = date;
			this.ID = ID;
		}
		
		public boolean saveTo(Path path)
		{
			HashMap<String, String> params = new HashMap<>();
			params.put("user", "" + getID());
			params.put("pass", Settings.getString("apiPass"));
			try
			{
				RequestHandler<String> rs = new StringGetRequestSender(new URL(Settings.getString("apiDataEndpoint")), null, params).getRequestHandler();
				if(rs.getStatus() != 200)
					return false;
				
				JSONObject obj = new JSONObject(rs.getRequestResult());
				if(obj.has("data"))
				{
					String base64 = obj.getString("data");
				}
			}
			catch(UnirestException | URISyntaxException | MalformedURLException e)
			{
				e.printStackTrace();
			}
			
			return true;
		}
		
		public String getDate()
		{
			return this.date;
		}
		
		public int getID()
		{
			return ID;
		}
		
		public String getUsername()
		{
			return this.username;
		}
		
		public String getPath()
		{
			return this.path;
		}
		
		public String getType()
		{
			return this.type;
		}
	}
	
	public void run(Configuration config, String user, Path outputFolder) throws MalformedURLException, URISyntaxException, UnirestException
	{
		listFiles(user).stream().filter(file -> {
			try
			{
				return config.isDownloaded(Paths.get(file.getUsername()).resolve(file.getType()).resolve(file.getPath()).normalize());
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return false;
		}).forEach(file -> {
			try
			{
				Date date = dateTimeFormatter.parse(file.getDate());
				if(file.saveTo(outputFolder.resolve(file.getType()).resolve(dateFormatter.format(date) + file.getPath().substring(file.getPath().lastIndexOf(".")))))
				{
					//config.setDownloaded(Paths.get(file.getUsername()).resolve(file.getType()).resolve(file.getPath()).normalize());
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		});
	}
	
	public Collection<APIFile> listFiles(String user) throws MalformedURLException, URISyntaxException, UnirestException
	{
		HashMap<String, String> params = new HashMap<>();
		params.put("user", user);
		params.put("pass", Settings.getString("apiPass"));
		RequestHandler<String> rs = new StringGetRequestSender(new URL(Settings.getString("apiListAllEndpoint")), null, params).getRequestHandler();
		if(rs.getStatus() != 200)
			throw new RuntimeException("HTTP STATUS " + rs.getStatus());
		
		LinkedList<APIFile> files = new LinkedList<>();
		JSONObject obj = new JSONObject(rs.getRequestResult());
		if(obj.has("files"))
		{
			JSONArray fls = obj.getJSONArray("files");
			for(int i = 0; i < fls.length(); i++)
			{
				JSONObject oo = fls.getJSONObject(i);
				files.add(new APIFile(oo.getInt("ID"), oo.getString("username"), oo.getString("path"), oo.getString("type"), oo.getString("date")));
			}
		}
		return files;
	}
}
