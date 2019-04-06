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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
class Configuration extends SQLiteManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	
	Configuration(final File dbFile) throws ClassNotFoundException, InterruptedException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee));").waitSafely();
	}
	
	Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(final String folder, final Collection<ChannelSftp.LsEntry> entries) throws InterruptedException{
		final var files = new HashMap<String, ChannelSftp.LsEntry>();
		for(final var entry : entries){
			files.put(Paths.get(folder).resolve(entry.getFilename().replace(":", ".")).toString().replace("\\", "/"), entry);
		}
		final var filesQuery = files.keySet().stream().map(val -> "\"" + val + "\"").collect(Collectors.joining(","));
		sendQueryRequest("SELECT Filee FROM Downloaded WHERE Filee IN (" + filesQuery + ")").done(resultSet -> {
			try{
				while(resultSet.next()){
					files.remove(resultSet.getString("Filee"));
				}
			}
			catch(final SQLException e){
				LOGGER.error("Error getting downloaded files for path {}", folder, e);
			}
		}).waitSafely();
		return files.values();
	}
	
	boolean isDownloaded(final Path path) throws InterruptedException{
		final var downloaded = new boolean[1];
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
	
	int removeUseless(){
		LOGGER.info("Removing useless entries from database");
		final var result = new AtomicInteger(-1);
		try{
			sendUpdateRequest("DELETE FROM Downloaded WHERE DateDownload < DATETIME('now','-8 days');").done(result::set).waitSafely();
		}
		catch(final InterruptedException e){
			LOGGER.error("Error removing useless entries in DB", e);
		}
		return result.get();
	}
	
	void setDownloaded(final Path path) throws InterruptedException{
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES(?,?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString().replace("\\", "/")), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString()))).waitSafely();
	}
	
	void setDownloaded(final Collection<Path> paths) throws InterruptedException{
		final var placeHolders = IntStream.range(0, paths.size()).mapToObj(o -> "(?,?)").collect(Collectors.joining(","));
		final var values = paths.stream().map(path -> path.toString().replace("\\", "/")).flatMap(path -> List.of(new SQLValue(SQLValue.Type.STRING, path), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString())).stream()).toArray(SQLValue[]::new);
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES " + placeHolders + ";", new PreparedStatementFiller(values)).waitSafely();
		LOGGER.info("Set downloaded status for {} items", paths.size());
	}
}
