package fr.raksrinana.ftpfetcher;

import me.tongfei.progressbar.ProgressBar;

/**
 * Created by mrcraftcod (MrCraftCod - zerderr@gmail.com) on 2019-05-05.
 *
 * @author Thomas Couchoud
 * @since 2019-05-05
 */
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
