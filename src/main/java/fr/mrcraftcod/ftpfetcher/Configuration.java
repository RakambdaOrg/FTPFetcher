package fr.mrcraftcod.ftpfetcher;

import fr.mrcraftcod.utils.config.PreparedStatementFiller;
import fr.mrcraftcod.utils.config.SQLValue;
import fr.mrcraftcod.utils.config.SQLiteManager;
import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Configuration extends SQLiteManager
{
	public Configuration() throws ClassNotFoundException, InterruptedException
	{
		super(new File(".", "FTPFetcher.db"), false);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee));").waitSafely();
	}
	
	public boolean isDownloaded(Path path) throws InterruptedException
	{
		final boolean[] downloaded = new boolean[1];
		sendQueryRequest("SELECT * FROM Downloaded WHERE Filee='" + path.toString().replace("\\", "/") + "';").done(resultSet -> {
			try
			{
				downloaded[0] = resultSet.next();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}).waitSafely();
		return downloaded[0];
	}
	
	public void setDownloaded(Path path) throws InterruptedException
	{
		sendPreparedUpdateRequest("INSERT INTO Downloaded(Filee,DateDownload) VALUES(?,?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString()), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString()))).waitSafely();
	}
}
