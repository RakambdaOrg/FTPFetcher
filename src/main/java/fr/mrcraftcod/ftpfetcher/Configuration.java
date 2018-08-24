package fr.mrcraftcod.ftpfetcher;

import fr.mrcraftcod.utils.config.PreparedStatementFiller;
import fr.mrcraftcod.utils.config.SQLValue;
import fr.mrcraftcod.utils.config.SQLiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class Configuration extends SQLiteManager
{private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	Configuration() throws ClassNotFoundException, InterruptedException
	{
		super(new File(".", "FTPFetcher.db"));
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee));").waitSafely();
	}
	
	boolean isDownloaded(final Path path) throws InterruptedException
	{
		final boolean[] downloaded = new boolean[1];
		sendQueryRequest("SELECT * FROM Downloaded WHERE Filee='" + path.toString().replace("\\", "/") + "';").done(resultSet -> {
			try
			{
				downloaded[0] = resultSet.next();
			}
			catch(SQLException e)
			{
				LOGGER.error("Error getting downloaded status for {}", path, e);
			}
		}).waitSafely();
		return downloaded[0];
	}
	
	void removeUseless()
	{
		try{
			sendUpdateRequest("DELETE FROM Downloaded WHERE DateDownload < DATETIME('now','-8 days');").waitSafely();
		}
		catch(final InterruptedException e){
			LOGGER.error("Error removing useless entries in DB", e);
		}
	}
	
	void setDownloaded(final Path path) throws InterruptedException
	{
		sendPreparedUpdateRequest("INSERT INTO Downloaded(Filee,DateDownload) VALUES(?,?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString()), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString()))).waitSafely();
	}
}
