package fr.raksrinana.ftpfetcher.downloader;

import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;

public class ProgressBarHandler{
	private final ProgressBar progressBar;
	private final Object stepLock;
	
	public ProgressBarHandler(@NotNull ProgressBar progressBar){
		this.progressBar = progressBar;
		stepLock = new Object();
	}
	
	public void step(){
		synchronized(stepLock){
			progressBar.step();
		}
	}
	
	public void setExtraMessage(String s){
		progressBar.setExtraMessage(s);
	}
}
