package fr.rakambda.ftpfetcher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

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
	private Set<PosixFilePermission> permissions;
	
	public DownloadElement(@NotNull String folder, @NotNull RemoteResourceInfo sftpFile, @NotNull Path fileOut, boolean deleteOnSuccess, @NotNull LocalDateTime downloadedAt, @Nullable Set<PosixFilePermission> permissions){
		this.folder = folder + (folder.endsWith("/") ? "" : "/");
		this.sftpFile = sftpFile;
		attributes = sftpFile.getAttributes();
		remotePath = folder + sftpFile.getName();
		this.fileOut = fileOut;
		this.deleteOnSuccess = deleteOnSuccess;
		this.downloadedAt = downloadedAt;
		this.permissions = permissions;
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
