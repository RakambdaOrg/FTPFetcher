package fr.rakambda.ftpfetcher.downloader;

import fr.rakambda.ftpfetcher.cli.Settings;
import lombok.Getter;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Paths;

public class FTPConnection implements AutoCloseable{
	private final SSHClient ssh;
	private final Settings settings;
	
	@Getter
	private SFTPClient sftp;
	
	public FTPConnection(@NotNull Settings settings) throws IOException{
		this.settings = settings;
		
		ssh = new SSHClient();
		connect();
	}
	
	private void connect() throws IOException{
		var knownHostsFilename = Paths.get(System.getProperty("user.home")).resolve(".ssh").resolve("known_hosts");
		ssh.loadKnownHosts(knownHostsFilename.toFile());
		
		ssh.connect(settings.getFtpHost());
		ssh.authPassword(settings.getFtpUser(), settings.getFtpPass());
		
		sftp = ssh.newSFTPClient();
		sftp.getFileTransfer().setPreserveAttributes(false);
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