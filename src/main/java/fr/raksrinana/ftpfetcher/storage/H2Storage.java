package fr.raksrinana.ftpfetcher.storage;

import com.jcraft.jsch.ChannelSftp;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import fr.raksrinana.utils.config.H2Manager;
import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Log4j2
public class H2Storage extends H2Manager implements IStorage{
	public H2Storage(Path dbFile) throws SQLException, IOException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee))");
	}
	
	@NotNull
	public Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<ChannelSftp.LsEntry> entries) throws SQLException{
		var files = new HashMap<String, ChannelSftp.LsEntry>();
		for(var entry : entries){
			files.put(Paths.get(folder)
							.resolve(entry.getFilename().replace(":", "."))
							.toString()
							.replace("\\", "/"),
					entry);
		}
		var filesFilter = files.keySet().stream()
				.map(str -> str.replace("'", "\\'"))
				.map(str -> "'" + str + "'")
				.collect(Collectors.joining(","));
		sendQueryRequest("SELECT Filee FROM Downloaded WHERE Filee IN (" + filesFilter + ")", rs -> rs.getString("Filee")).forEach(files::remove);
		return files.values();
	}
	
	public int removeUseless() throws SQLException{
		log.info("Removing useless entries from database");
		return sendUpdateRequest("DELETE FROM Downloaded WHERE DateDownload < DATEADD('DAY',-15,CURRENT_DATE)");
	}
	
	public int setDownloaded(@NotNull Collection<DownloadElement> elements){
		var downloadDate = LocalDateTime.now().toString();
		try{
			var result = sendPreparedBatchUpdateRequest("MERGE INTO Downloaded(Filee,DateDownload) VALUES(?,?)", elements.stream()
					.map(elem -> new PreparedStatementFiller(
							new SQLValue(SQLValue.Type.STRING, elem.getRemotePath().replace("\\", "/")),
							new SQLValue(SQLValue.Type.STRING, downloadDate)))
					.collect(Collectors.toList()));
			log.debug("Set downloaded status for {} items", result);
			return result;
		}
		catch(SQLException e){
			log.error("Failed to set elements ({}) downloaded", elements.size(), e);
		}
		return 0;
	}
}
