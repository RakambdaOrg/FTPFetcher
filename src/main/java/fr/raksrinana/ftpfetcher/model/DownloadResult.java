package fr.raksrinana.ftpfetcher.model;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
public class DownloadResult{
	private final DownloadElement element;
	@Setter
	private boolean downloaded;
	@Setter
	private long downloadTime;
	
	public DownloadResult(@NotNull DownloadElement element, boolean downloaded){
		this.element = element;
		this.downloaded = downloaded;
		downloadTime = 0;
	}
}
