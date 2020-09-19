package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import fr.raksrinana.utils.config.H2Manager;
import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
public class Database extends H2Manager{
	public Database(final Path dbFile) throws SQLException, IOException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Downloaded(Filee VARCHAR(512) NOT NULL, DateDownload DATETIME,PRIMARY KEY(Filee))");
	}
	
	Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(final String folder, final Collection<ChannelSftp.LsEntry> entries) throws SQLException{
		final var files = new HashMap<String, ChannelSftp.LsEntry>();
		for(final var entry : entries){
			files.put(Paths.get(folder)
							.resolve(entry.getFilename().replace(":", "."))
							.toString()
							.replace("\\", "/"),
					entry);
		}
		final var filesFilter = files.keySet().stream()
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
	
	public int setDownloaded(final Collection<DownloadElement> elements){
		final var downloadDate = LocalDateTime.now().toString();
		try{
			final var result = this.sendPreparedBatchUpdateRequest("MERGE INTO Downloaded(Filee,DateDownload) VALUES(?,?)", elements.stream()
					.map(elem -> new PreparedStatementFiller(
							new SQLValue(SQLValue.Type.STRING, elem.getRemotePath().replace("\\", "/")),
							new SQLValue(SQLValue.Type.STRING, downloadDate)))
					.collect(Collectors.toList()));
			log.debug("Set downloaded status for {} items", result);
			return result;
		}
		catch(final SQLException e){
			log.error("Failed to set elements ({}) downloaded", elements.size(), e);
		}
		return 0;
	}
}
