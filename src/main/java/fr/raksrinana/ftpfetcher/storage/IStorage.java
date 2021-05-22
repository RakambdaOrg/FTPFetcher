package fr.raksrinana.ftpfetcher.storage;

import com.jcraft.jsch.ChannelSftp;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.Collection;

public interface IStorage extends AutoCloseable{
	int removeUseless() throws SQLException;
	
	@NotNull
	Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<ChannelSftp.LsEntry> entries) throws SQLException;
	
	int setDownloaded(@NotNull Collection<DownloadElement> elements);
}
