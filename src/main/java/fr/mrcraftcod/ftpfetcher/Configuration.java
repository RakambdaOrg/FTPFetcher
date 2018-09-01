package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import fr.mrcraftcod.utils.config.PreparedStatementFiller;
import fr.mrcraftcod.utils.config.SQLValue;
import fr.mrcraftcod.utils.config.SQLiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
class Configuration extends SQLiteManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	
	Configuration() throws ClassNotFoundException, InterruptedException{
		super(new File(".", "FTPFetcher.db"));
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee));").waitSafely();
	}
	
	public Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(String folder, Collection<ChannelSftp.LsEntry> array) throws InterruptedException{
		HashMap<String, ChannelSftp.LsEntry> files = new HashMap<>();
		for(ChannelSftp.LsEntry entry : array){
			files.put(Paths.get(folder).resolve(entry.getFilename().replace(":", ".")).toString().replace("\\", "/"), entry);
		}
		String filesQuery = files.keySet().stream().map(val -> "\"" + val + "\"").collect(Collectors.joining(","));
		sendQueryRequest("SELECT Filee FROM Downloaded WHERE Filee IN (" + filesQuery + ")").done(resultSet -> {
			try{
				while(resultSet.next()){
					files.remove(resultSet.getString("Filee"));
				}
			}
			catch(SQLException e){
				LOGGER.error("Error getting downloaded files for path {}", folder, e);
			}
		}).waitSafely();
		return files.values();
	}
	
	boolean isDownloaded(final Path path) throws InterruptedException{
		final boolean[] downloaded = new boolean[1];
		sendQueryRequest("SELECT * FROM Downloaded WHERE Filee='" + path.toString().replace("\\", "/") + "';").done(resultSet -> {
			try{
				downloaded[0] = resultSet.next();
			}
			catch(SQLException e){
				LOGGER.error("Error getting downloaded status for {}", path, e);
			}
		}).waitSafely();
		return downloaded[0];
	}
	
	@Override
	public void close(){
		LOGGER.info("Closing SQL Connection...");
		super.close();
	}
	
	void removeUseless(){
		LOGGER.info("Removing useless entries from database");
		try{
			sendUpdateRequest("DELETE FROM Downloaded WHERE DateDownload < DATETIME('now','-8 days');").waitSafely();
		}
		catch(final InterruptedException e){
			LOGGER.error("Error removing useless entries in DB", e);
		}
	}
	
	void setDownloaded(final Path path) throws InterruptedException{
		sendPreparedUpdateRequest("INSERT INTO Downloaded(Filee,DateDownload) VALUES(?,?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString()), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString()))).waitSafely();
	}
}
