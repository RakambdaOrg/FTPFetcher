package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import java.io.File;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
public class DownloadElement
{
	private final String folder;
	private final ChannelSftp.LsEntry file;
	private final File fileOut;
	
	public DownloadElement(String folder, ChannelSftp.LsEntry file, File fileOut)
	{
		this.folder = folder;
		this.file = file;
		this.fileOut = fileOut;
	}
	
	public String getFolder()
	{
		return folder;
	}
	
	public ChannelSftp.LsEntry getFile()
	{
		return file;
	}
	
	public File getFileOut()
	{
		return fileOut;
	}
}
