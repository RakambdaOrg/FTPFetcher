package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.*;
import fr.mrcraftcod.utils.base.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
public class FTPConnection
{
	private final Session session;
	private final ChannelSftp sftpChannel;
	
	public FTPConnection() throws JSchException, IOException
	{
		JSch.setConfig("StrictHostKeyChecking", "no");
		
		JSch jsch = new JSch();
		File knownHostsFilename = FileUtils.getHomeFolder(".ssh/known_hosts");
		jsch.setKnownHosts(knownHostsFilename.getAbsolutePath());
		
		session = jsch.getSession(Settings.getString("ftpUser"), Settings.getString("ftpHost"));
		session.setPassword(Settings.getString("ftpPass"));
		
		session.connect();
		
		Channel channel = session.openChannel("sftp");
		channel.connect();
		
		session.setServerAliveInterval(20000);
		sftpChannel = (ChannelSftp) channel;
	}
	
	public void close()
	{
		if(sftpChannel != null && sftpChannel.isConnected())
			sftpChannel.exit();
		if(session != null && session.isConnected())
			session.disconnect();
	}
	
	public ChannelSftp getClient()
	{
		return sftpChannel;
	}
}
