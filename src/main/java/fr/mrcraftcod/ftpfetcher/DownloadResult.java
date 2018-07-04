package fr.mrcraftcod.ftpfetcher;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
public class DownloadResult
{
	private final DownloadElement element;
	private boolean downloaded;
	
	public DownloadResult(DownloadElement element, boolean downloaded)
	{
		this.element = element;
		this.downloaded = downloaded;
	}
	
	public boolean isDownloaded()
	{
		return downloaded;
	}
	
	public void setDownloaded(boolean downloaded)
	{
		this.downloaded = downloaded;
	}
}
