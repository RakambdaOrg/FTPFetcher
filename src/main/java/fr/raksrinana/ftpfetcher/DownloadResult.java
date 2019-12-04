package fr.raksrinana.ftpfetcher;

import lombok.Getter;
import lombok.Setter;

@Getter
class DownloadResult{
	private final DownloadElement element;
	@Setter
	private boolean downloaded;
	@Setter
	private long downloadTime;
	
	DownloadResult(final DownloadElement element, final boolean downloaded){
		this.element = element;
		this.downloaded = downloaded;
		this.downloadTime = 0;
	}
}
