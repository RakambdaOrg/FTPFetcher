package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.SftpProgressMonitor;

public class DownloadProgressMonitor implements SftpProgressMonitor
{
	private static final int MAX_CHAR = 140;
	private final DownloadFileTask task;
	private long max                = 0;
	private long count              = 0;
	
	public DownloadProgressMonitor(DownloadFileTask task)
	{
		this.task = task;
	}
	
	public void init(int op, java.lang.String src, java.lang.String dest, long max)
	{
		this.max = max;
	}
	
	public boolean count(long bytes)
	{
		this.count += bytes;
		task.updateDownloaded(count, max);
		return true;
	}
	
	public void end()
	{
	}
}
