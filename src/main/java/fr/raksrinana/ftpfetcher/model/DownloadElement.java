package fr.raksrinana.ftpfetcher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class DownloadElement{
	private String folder;
	private RemoteResourceInfo sftpFile;
	private Path fileOut;
	private String remotePath;
	private boolean deleteOnSuccess;
	@Setter
	private LocalDateTime downloadedAt;
	private FileAttributes attributes;
	
	public DownloadElement(@NotNull String folder, @NotNull RemoteResourceInfo sftpFile, @NotNull Path fileOut, boolean deleteOnSuccess, @NotNull LocalDateTime downloadedAt){
		this.folder = folder + (folder.endsWith("/") ? "" : "/");
		this.sftpFile = sftpFile;
		attributes = sftpFile.getAttributes();
		remotePath = folder + sftpFile.getName();
		this.fileOut = fileOut;
		this.deleteOnSuccess = deleteOnSuccess;
		this.downloadedAt = downloadedAt;
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(remotePath);
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || getClass() != o.getClass()){
			return false;
		}
		DownloadElement that = (DownloadElement) o;
		return Objects.equals(remotePath, that.remotePath);
	}
	
	@Override
	public String toString(){
		return getRemotePath();
	}
	
	public FileAttributes getAttributes(){
		return attributes;
	}
	
	public long getFileSize(){
		return attributes.getSize();
	}
}
