package fr.rakambda.ftpfetcher.storage;

import fr.rakambda.ftpfetcher.model.DownloadElement;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jspecify.annotations.NonNull;
import java.sql.SQLException;
import java.util.Collection;

public interface IStorage extends AutoCloseable{
	int removeUseless() throws SQLException;
	
	@NonNull
	Collection<RemoteResourceInfo> getOnlyNotDownloaded(@NonNull String folder, @NonNull Collection<RemoteResourceInfo> entries) throws SQLException;
	
	int setDownloaded(@NonNull Collection<DownloadElement> elements);
}
