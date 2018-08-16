package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.*;
import java.io.IOException;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
class FTPConnection
{
	private Session session;
	private ChannelSftp sftpChannel;
	private final JSch jsch;
	
	FTPConnection(final JSch jsch) throws JSchException, IOException
	{
		this.jsch = jsch;
		connect();
	}
	
	private void connect() throws JSchException, IOException
	{
		session = jsch.getSession(Settings.getString("ftpUser"), Settings.getString("ftpHost"));
		session.setPassword(Settings.getString("ftpPass"));
		
		session.connect();
		
		final Channel channel = session.openChannel("sftp");
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
