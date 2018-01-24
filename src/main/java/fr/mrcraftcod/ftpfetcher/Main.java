package fr.mrcraftcod.ftpfetcher;

import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Paths;


/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Main
{
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException, MalformedURLException, URISyntaxException, UnirestException
	{
		Configuration config = new Configuration();
		if(args.length == 0 || args[0].equals("--ftp"))
		{
			try
			{
				new FTPFetcher().run(config, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder")));
			}
			catch(Exception e)
			{
			}
		}
		else
		{
			new APIFetcher().run(config, Settings.getString("apiValue"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder")));
		}
	}
}
