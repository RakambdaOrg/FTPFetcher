package fr.rakambda.ftpfetcher.model;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

@Getter
public class DownloadResult{
	private final DownloadElement element;
	@Setter
	private boolean downloaded;
	@Setter
	private long downloadTime;
	
	public DownloadResult(@NonNull DownloadElement element, boolean downloaded){
		this.element = element;
		this.downloaded = downloaded;
		downloadTime = 0;
	}
}
