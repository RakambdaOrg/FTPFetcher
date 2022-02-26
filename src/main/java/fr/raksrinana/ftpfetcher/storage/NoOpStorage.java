package fr.raksrinana.ftpfetcher.storage;

import fr.raksrinana.ftpfetcher.model.DownloadElement;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
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
	@NotNull
	public Collection<RemoteResourceInfo> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<RemoteResourceInfo> entries){
		return entries;
	}
	
	@Override
	public int setDownloaded(@NotNull Collection<DownloadElement> elements){
		return elements.size();
	}
}
