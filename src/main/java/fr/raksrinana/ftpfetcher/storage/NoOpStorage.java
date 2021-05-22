package fr.raksrinana.ftpfetcher.storage;

import com.jcraft.jsch.ChannelSftp;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
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
	public Collection<ChannelSftp.LsEntry> getOnlyNotDownloaded(@NotNull String folder, @NotNull Collection<ChannelSftp.LsEntry> entries){
		return entries;
	}
	
	@Override
	public int setDownloaded(@NotNull Collection<DownloadElement> elements){
		return elements.size();
	}
}
