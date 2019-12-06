package fr.raksrinana.ftpfetcher.downloader;

import me.tongfei.progressbar.ProgressBar;

public class ProgressBarHandler{
	private final ProgressBar progressBar;
	private final Object stepLock;
	
	public ProgressBarHandler(final ProgressBar progressBar){
		this.progressBar = progressBar;
		this.stepLock = new Object();
	}
	
	public void step(){
		synchronized(stepLock){
			progressBar.step();
		}
	}
	
	public void setExtraMessage(final String s){
		this.progressBar.setExtraMessage(s);
	}
}
