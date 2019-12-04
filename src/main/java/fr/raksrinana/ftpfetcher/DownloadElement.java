package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import lombok.Getter;
import java.nio.file.Path;

@Getter
class DownloadElement{
	private final String folder;
	private final ChannelSftp.LsEntry file;
	private final Path fileOut;
	
	DownloadElement(final String folder, final ChannelSftp.LsEntry file, final Path fileOut){
		this.folder = folder + (folder.endsWith("/") ? "" : "/");
		this.file = file;
		this.fileOut = fileOut;
	}
	
	@Override
	public String toString(){
		return getFolder() + getFile().getFilename();
	}
}
