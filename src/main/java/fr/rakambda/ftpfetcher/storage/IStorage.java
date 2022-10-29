package fr.rakambda.ftpfetcher.storage;

import fr.rakambda.ftpfetcher.model.DownloadElement;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.Collection;

public interface IStorage extends AutoCloseable{
	int removeUseless() throws SQLException;
	
	@NotNull
	Collection<RemoteResourceInfo> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<RemoteResourceInfo> entries) throws SQLException;
	
	int setDownloaded(@NotNull Collection<DownloadElement> elements);
}
