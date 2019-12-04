package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import fr.raksrinana.ftpfetcher.settings.Settings;
import java.io.IOException;

class FTPConnection{
	private Session session;
	private ChannelSftp sftpChannel;
	private final JSch jsch;
	private final Settings settings;
	
	FTPConnection(final JSch jsch, final Settings settings) throws JSchException{
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
	
	void close()
	{
		if(sftpChannel != null && sftpChannel.isConnected())
			sftpChannel.exit();
		if(session != null && session.isConnected())
			session.disconnect();
	}
	
	void reopen() throws IOException, JSchException
	{
		close();
		connect();
	}
	
	ChannelSftp getClient()
	{
		return sftpChannel;
	}
}
