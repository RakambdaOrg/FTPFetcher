package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
public class Database implements AutoCloseable{
	private final HikariDataSource datasource;
	
	public Database(final Path dbFile) throws SQLException{
		final var config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.setMaximumPoolSize(1);
		config.setAutoCommit(true);
		datasource = new HikariDataSource(config);
		final var connection = datasource.getConnection();
		final var statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee))");
		statement.close();
		connection.close();
	}
	
	Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(final String folder, final Collection<ChannelSftp.LsEntry> entries){
		final var files = new HashMap<String, ChannelSftp.LsEntry>();
		for(final var entry : entries){
			files.put(Paths.get(folder).resolve(entry.getFilename().replace(":", ".")).toString().replace("\\", "/"), entry);
		}
		try(final var connection = datasource.getConnection(); final var statement = connection.createStatement()){
			final var filesFilter = files.keySet().stream().map(str -> str.replace("'", "\\'")).map(str -> "'" + str + "'").collect(Collectors.joining(","));
			try(final var result = statement.executeQuery("SELECT Filee FROM Downloaded WHERE Filee IN (" + filesFilter + ")")){
				while(result.next()){
					files.remove(result.getString("Filee"));
				}
			}
		}
		catch(final SQLException e){
			log.error("Error getting downloaded files for path {}", folder, e);
		}
		return files.values();
	}
	
	public boolean isDownloaded(final DownloadElement element) throws SQLException{
		try(final var connection = datasource.getConnection(); final var statement = connection.prepareStatement("SELECT * FROM Downloaded WHERE Filee=?")){
			statement.setString(1, element.getRemotePath().replace("\\", "/"));
			try(final var result = statement.executeQuery()){
				return result.next();
			}
		}
	}
	
	@Override
	public void close(){
		log.info("Closing SQL Connection...");
		datasource.close();
	}
	
	public int removeUseless(){
		log.info("Removing useless entries from database");
		try(final var connection = datasource.getConnection(); final var statement = connection.createStatement()){
			return statement.executeUpdate("DELETE FROM Downloaded WHERE DateDownload < DATETIME('now','-15 days')");
		}
		catch(final SQLException e){
			log.error("Error removing useless entries in DB", e);
		}
		return 0;
	}
	
	public int setDownloaded(final DownloadElement element){
		try(final var connection = datasource.getConnection(); final var statement = connection.prepareStatement("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES(?,?)")){
			statement.setString(1, element.getRemotePath().replace("\\", "/"));
			statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
			return statement.executeUpdate();
		}
		catch(final SQLException e){
			log.error("Failed to set element downloaded", e);
		}
		return 0;
	}
	
	public int setDownloaded(final Collection<DownloadElement> elements){
		try(final var connection = datasource.getConnection(); final var statement = connection.prepareStatement("INSERT OR IGNORE INTO Downloaded(Filee,DateDownload) VALUES(?,?)")){
			for(final var element : elements){
				statement.setString(1, element.getRemotePath().replace("\\", "/"));
				statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
				statement.addBatch();
			}
			final var result = Arrays.stream(statement.executeBatch()).sum();
			log.debug("Set downloaded status for {} items", result);
			return result;
		}
		catch(final SQLException e){
			log.error("Failed to set elements ({}) downloaded", elements.size(), e);
		}
		return 0;
	}
}
