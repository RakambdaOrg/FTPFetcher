package fr.mrcraftcod.ftpfetcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Main
{
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException, IOException
	{
		if(args.length > 0)
			Settings.getInstance(args[0]);
		
		Configuration config = new Configuration();
		config.removeUseless();
		
		try
		{
			new FTPFetcher().run(config, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder")));
			new FTPFetcher().run(config, Settings.getString("ftpFolder2"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder2")));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
