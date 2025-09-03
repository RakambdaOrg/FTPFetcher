package fr.rakambda.ftpfetcher.downloader;

import fr.rakambda.ftpfetcher.cli.Settings;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

@Log4j2
public class FTPConnection implements AutoCloseable{
	private final SSHClient ssh;
	private final Settings settings;
	
	@Getter
	private SFTPClient sftp;
	
	public FTPConnection(@NonNull Settings settings) throws IOException{
		this.settings = settings;
		
		ssh = new SSHClient();
		connect();
	}
	
	private void connect() throws IOException{
		var knownHostsFilename = Optional.ofNullable(settings.getKnownHosts()).map(Paths::get)
				.orElseGet(() -> Paths.get(System.getProperty("user.home")).resolve(".ssh").resolve("known_hosts"));
		
		log.debug("Loading known hosts from {}", knownHostsFilename.toAbsolutePath());
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
