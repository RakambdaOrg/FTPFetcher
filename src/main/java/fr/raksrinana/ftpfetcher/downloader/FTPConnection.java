package fr.raksrinana.ftpfetcher.downloader;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import fr.raksrinana.ftpfetcher.cli.Settings;
import lombok.Getter;

public class FTPConnection implements AutoCloseable{
	private Session session;
	@Getter
	private ChannelSftp sftpChannel;
	private final JSch jsch;
	private final Settings settings;
	
	public FTPConnection(final JSch jsch, final Settings settings) throws JSchException{
		this.jsch = jsch;
		this.settings = settings;
		connect();
	}
	
	private void connect() throws JSchException{
		session = jsch.getSession(settings.getFtpUser(), settings.getFtpHost());
		session.setPassword(settings.getFtpPass());
		session.connect();
		final var channel = session.openChannel("sftp");
		channel.connect();
		session.setServerAliveInterval(20000);
		sftpChannel = (ChannelSftp) channel;
	}
	
	void reopen() throws JSchException{
		close();
		connect();
	}
	
	@Override
	public void close(){
		if(sftpChannel != null && sftpChannel.isConnected()){
			sftpChannel.exit();
		}
		if(session != null && session.isConnected()){
			session.disconnect();
		}
	}
}
