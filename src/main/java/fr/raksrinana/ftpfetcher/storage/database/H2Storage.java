package fr.raksrinana.ftpfetcher.storage.database;

import com.zaxxer.hikari.HikariDataSource;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Log4j2
public class H2Storage extends BaseDatabase{
	public H2Storage(HikariDataSource dataSource){
		super(dataSource);
	}
	
	@Override
	public void initDatabase() throws SQLException{
		execute("""
				CREATE TABLE IF NOT EXISTS Downloaded(
				    Filee VARCHAR(512) NOT NULL,
				    DateDownload DATETIME,
				    PRIMARY KEY(Filee)
				)""");
	}
	
	@Override
	public int removeUseless() throws SQLException{
		log.info("Removing useless entries from database");
		return execute("DELETE FROM Downloaded WHERE DateDownload < DATEADD('DAY',-15,CURRENT_DATE)");
	}
	
	@NotNull
	public Collection<RemoteResourceInfo> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<RemoteResourceInfo> entries) throws SQLException{
		var files = new HashMap<String, RemoteResourceInfo>();
		for(var entry : entries){
			files.put(Paths.get(folder)
							.resolve(entry.getName().replace(":", "."))
							.toString()
							.replace("\\", "/"),
					entry);
		}
		var filesFilter = files.keySet().stream()
				.map(str -> str.replace("'", "\\'"))
				.map(str -> "'" + str + "'")
				.collect(Collectors.joining(","));
		
		try(var conn = getConnection();
				var statement = conn.prepareStatement("""
						SELECT Filee FROM Downloaded
						WHERE Filee IN (?)""")){
			statement.setString(1, filesFilter);
			
			try(var result = statement.executeQuery()){
				while(result.next()){
					files.remove(result.getString("Filee"));
				}
			}
		}
		
		return files.values();
	}
	
	@Override
	public int setDownloaded(@NotNull Collection<DownloadElement> elements){
		var downloadDate = LocalDateTime.now().toString();
		try(var conn = getConnection();
				var statement = conn.prepareStatement("""
						MERGE INTO Downloaded(Filee,DateDownload)
						VALUES(?,?)""")){
			
			for(var element : elements){
				statement.setString(1, element.getRemotePath().replace("\\", "/"));
				statement.setString(2, downloadDate);
				statement.addBatch();
			}
			
			var result = Arrays.stream(statement.executeBatch()).sum();
			log.debug("Set downloaded status for {} items", result);
			return result;
		}
		catch(SQLException e){
			log.error("Failed to set elements ({}) downloaded", elements.size(), e);
		}
		return 0;
	}
}
