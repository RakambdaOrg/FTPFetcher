package fr.raksrinana.ftpfetcher.downloader;

import fr.raksrinana.ftpfetcher.cli.Settings;
import lombok.Getter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;

public class FTPConnection implements AutoCloseable{
	private final SSHClient ssh;
	private final Settings settings;
	
	@Getter
	private SFTPClient sftp;
	
	public FTPConnection(@NotNull SSHClient ssh, @NotNull Settings settings) throws IOException{
		this.ssh = ssh;
		this.settings = settings;
		
		connect();
	}
	
	private void connect() throws IOException{
		ssh.connect(settings.getFtpHost());
		ssh.authPassword(settings.getFtpUser(), settings.getFtpPass());
		
		sftp = ssh.newSFTPClient();
		sftp.getFileTransfer().setPreserveAttributes(true);
	}
	
	@Override
	public void close() throws IOException{
		if(sftp != null){
			sftp.close();
		}
		if(ssh != null){
			ssh.disconnect();
		}
	}
}
