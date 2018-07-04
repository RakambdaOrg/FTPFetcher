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
	private long downloadTime;
	
	public DownloadResult(DownloadElement element, boolean downloaded)
	{
		this.element = element;
		this.downloaded = downloaded;
		this.downloadTime = 0;
	}
	
	public long getDownloadTime()
	{
		return downloadTime;
	}
	
	public void setDownloadTime(long time)
	{
		this.downloadTime = time;
	}
	
	public DownloadElement getElement()
	{
		return element;
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
