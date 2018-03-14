package fr.mrcraftcod.ftpfetcher;

import javafx.application.Application;
import java.io.File;
import java.nio.file.Paths;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Main
{
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException
	{
		if(args.length > 0 && args[0].equals("--ui"))
		{
			Application.launch(MainApplication.class, args);
		}
		else
		{
			Configuration config = new Configuration();
			config.removeUseless();
			
			try
			{
				new FTPFetcher().run(config, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder")));
			}
			catch(Exception e)
			{
			}
		}
	}
}
