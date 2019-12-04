package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import fr.raksrinana.utils.config.SQLiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Configuration extends SQLiteManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	
	Configuration(final Path dbFile) throws ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee));").get(30, TimeUnit.SECONDS);
	}
	
	Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(final String folder, final Collection<ChannelSftp.LsEntry> entries) throws InterruptedException, ExecutionException, TimeoutException{
		final var files = new HashMap<String, ChannelSftp.LsEntry>();
		for(final var entry : entries){
			files.put(Paths.get(folder).resolve(entry.getFilename().replace(":", ".")).toString().replace("\\", "/"), entry);
		}
		final var filesQuery = files.keySet().stream().map(val -> "\"" + val + "\"").collect(Collectors.joining(","));
		sendQueryRequest("SELECT Filee FROM Downloaded WHERE Filee IN (" + filesQuery + ")").thenAccept(resultSet -> {
			try{
				while(resultSet.next()){
					files.remove(resultSet.getString("Filee"));
				}
			}
			catch(final SQLException e){
				LOGGER.error("Error getting downloaded files for path {}", folder, e);
			}
		}).get(30, TimeUnit.SECONDS);
		return files.values();
	}
	
	boolean isDownloaded(final Path path) throws InterruptedException, TimeoutException, ExecutionException{
		final var downloaded = new boolean[1];
		sendQueryRequest("SELECT * FROM Downloaded WHERE Filee='" + path.toString().replace("\\", "/") + "';").thenAccept(resultSet -> {
			try{
				downloaded[0] = resultSet.next();
			}
			catch(SQLException e){
				LOGGER.error("Error getting downloaded status for {}", path, e);
			}
		}).get(30, TimeUnit.SECONDS);
		return downloaded[0];
	}
	
	@Override
	public void close(){
		LOGGER.info("Closing SQL Connection...");
		super.close();
	}
	
	int removeUseless(){
		LOGGER.info("Removing useless entries from database");
		try{
			return sendUpdateRequest("DELETE FROM Downloaded WHERE DateDownload < DATETIME('now','-15 days');").get(30, TimeUnit.SECONDS);
		}
		catch(final InterruptedException | ExecutionException | TimeoutException e){
			LOGGER.error("Error removing useless entries in DB", e);
		}
		return 0;
	}
	
	void setDownloaded(final Path path) throws InterruptedException, TimeoutException, ExecutionException{
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES(?,?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString().replace("\\", "/")), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString()))).get(30, TimeUnit.SECONDS);
	}
	
	void setDownloaded(final Collection<Path> paths) throws InterruptedException, TimeoutException, ExecutionException{
		final var placeHolders = IntStream.range(0, paths.size()).mapToObj(o -> "(?,?)").collect(Collectors.joining(","));
		final var values = paths.stream().map(path -> path.toString().replace("\\", "/")).flatMap(path -> List.of(new SQLValue(SQLValue.Type.STRING, path), new SQLValue(SQLValue.Type.STRING, LocalDateTime.now().toString())).stream()).toArray(SQLValue[]::new);
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES " + placeHolders + ";", new PreparedStatementFiller(values)).get(30, TimeUnit.SECONDS);
		LOGGER.debug("Set downloaded status for {} items", paths.size());
	}
}
