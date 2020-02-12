package fr.raksrinana.ftpfetcher.model;

import com.jcraft.jsch.ChannelSftp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class DownloadElement{
	private String folder;
	private ChannelSftp.LsEntry sftpFile;
	private Path fileOut;
	private String remotePath;
	private boolean deleteOnSuccess;
	@Setter
	private LocalDateTime downloadedAt;
	
	public DownloadElement(final String folder, final ChannelSftp.LsEntry sftpFile, final Path fileOut, final boolean deleteOnSuccess, final LocalDateTime downloadedAt){
		this.folder = folder + (folder.endsWith("/") ? "" : "/");
		this.sftpFile = sftpFile;
		this.remotePath = folder + sftpFile.getFilename();
		this.fileOut = fileOut;
		this.deleteOnSuccess = deleteOnSuccess;
		this.downloadedAt = downloadedAt;
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(remotePath);
	}
	
	@Override
	public boolean equals(final Object o){
		if(this == o){
			return true;
		}
		if(o == null || getClass() != o.getClass()){
			return false;
		}
		final DownloadElement that = (DownloadElement) o;
		return Objects.equals(remotePath, that.remotePath);
	}
	
	@Override
	public String toString(){
		return getRemotePath();
	}
}
