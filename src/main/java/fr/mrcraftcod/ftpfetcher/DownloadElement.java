package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.ChannelSftp;
import java.io.File;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
class DownloadElement
{
	private final String folder;
	private final ChannelSftp.LsEntry file;
	private final File fileOut;
	
	DownloadElement(final String folder, final ChannelSftp.LsEntry file, final File fileOut)
	{
		this.folder = folder;
		this.file = file;
		this.fileOut = fileOut;
	}
	
	String getFolder()
	{
		return folder;
	}
	
	ChannelSftp.LsEntry getFile()
	{
		return file;
	}
	
	File getFileOut()
	{
		return fileOut;
	}
	
	@Override
	public String toString(){
		return getFolder() + getFile().getFilename();
	}
}
