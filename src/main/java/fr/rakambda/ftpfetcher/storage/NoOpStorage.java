package fr.rakambda.ftpfetcher.storage;

import fr.rakambda.ftpfetcher.model.DownloadElement;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jspecify.annotations.NonNull;
import java.util.Collection;

public class NoOpStorage implements IStorage{
	@Override
	public void close(){
	}
	
	@Override
	public int removeUseless(){
		return 0;
	}
	
	@Override
	@NonNull
	public Collection<RemoteResourceInfo> getOnlyNotDownloaded(@NonNull String folder, @NonNull Collection<RemoteResourceInfo> entries){
		return entries;
	}
	
	@Override
	public int setDownloaded(@NonNull Collection<DownloadElement> elements){
		return elements.size();
	}
}
