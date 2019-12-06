package fr.raksrinana.ftpfetcher.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class DownloadResult{
	private final DownloadElement element;
	@Setter
	private boolean downloaded;
	@Setter
	private long downloadTime;
	
	public DownloadResult(final DownloadElement element, final boolean downloaded){
		this.element = element;
		this.downloaded = downloaded;
		this.downloadTime = 0;
	}
}
