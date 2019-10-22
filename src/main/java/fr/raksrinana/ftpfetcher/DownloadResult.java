package fr.raksrinana.ftpfetcher;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com)
 *
 * @author Thomas Couchoud
 * @since 2018-07-04
 */
class DownloadResult
{
	private final DownloadElement element;
	private boolean downloaded;
	private long downloadTime;
	
	DownloadResult(final DownloadElement element, final boolean downloaded)
	{
		this.element = element;
		this.downloaded = downloaded;
		this.downloadTime = 0;
	}
	
	long getDownloadTime()
	{
		return downloadTime;
	}
	
	void setDownloadTime(final long time)
	{
		this.downloadTime = time;
	}
	
	DownloadElement getElement()
	{
		return element;
	}
	
	boolean isDownloaded()
	{
		return downloaded;
	}
	
	void setDownloaded(final boolean downloaded)
	{
		this.downloaded = downloaded;
	}
}
