package fr.raksrinana.ftpfetcher.downloader;

import fr.raksrinana.ftpfetcher.cli.Settings;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import fr.raksrinana.ftpfetcher.model.DownloadResult;
import fr.raksrinana.ftpfetcher.storage.IStorage;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;

@Log4j2
public class FTPFetcher implements Callable<Collection<DownloadResult>>{
	private static final int MARK_DOWNLOADED_THRESHOLD = 25;
	private final Settings settings;
	private final IStorage storage;
	private final Collection<DownloadElement> downloadElements;
	private final SSHClient sshClient;
	private final ProgressBarHandler progressBar;
	private boolean stop;
	private boolean pause;
	
	public FTPFetcher(@NotNull SSHClient sshClient, @NotNull Settings settings, @NotNull IStorage storage, @NotNull Collection<DownloadElement> downloadElements, @NotNull ProgressBarHandler progressBar){
		this.sshClient = sshClient;
		this.settings = settings;
		this.storage = storage;
		this.downloadElements = downloadElements;
		this.progressBar = progressBar;
		stop = false;
		pause = false;
	}
	
	@Override
	@NotNull
	public Collection<DownloadResult> call() throws IOException{
		var results = new LinkedList<DownloadResult>();
		var toMarkDownloaded = new ArrayList<DownloadElement>(MARK_DOWNLOADED_THRESHOLD);
		try(var connection = new FTPConnection(sshClient, settings)){
			for(var element : downloadElements){
				if(stop){
					throw new StopDownloaderException("Executor stopped");
				}
				
				var startDownload = System.currentTimeMillis();
				var result = new DownloadResult(element, false);
				results.add(result);
				var fileOut = element.getFileOut();
				var downloaded = Files.exists(fileOut);
				log.debug("Downloading file {}", element.getRemotePath());
				progressBar.setExtraMessage(element.getSftpFile().getName());
				if(!downloaded){
					try{
						connection.getSftp().get(element.getRemotePath(), new FileSystemFile(element.getFileOut().toFile()));
					}
					catch(IOException e){
						log.warn("IO - Error downloading file {}", element, e);
						try{
							Files.deleteIfExists(fileOut);
						}
						catch(IOException ignored){
						}
						continue;
					}
					setAttributes(element.getFileOut(), FileTime.fromMillis(element.getSftpFile().getAttributes().getAtime() * 1000L));
					try{
						long fileLength = Files.size(fileOut);
						var expectedFileLength = element.getFileSize();
						if(expectedFileLength == fileLength){
							downloaded = true;
						}
						else{
							log.warn("Sizes mismatch expected:{} actual:{} difference: {}", expectedFileLength, fileLength, fileLength - expectedFileLength);
						}
					}
					catch(IOException ignored){
					}
				}
				if(downloaded){
					element.setDownloadedAt(LocalDateTime.now());
					result.setDownloaded(true);
					toMarkDownloaded.add(element);
					if(element.isDeleteOnSuccess()){
						try{
							log.debug("Deleting remote file {}", element.getRemotePath());
							connection.getSftp().rm(element.getRemotePath());
						}
						catch(IOException e){
							log.error("Failed to delete remote file {} after a successful download", element.getRemotePath(), e);
						}
					}
					progressBar.stepBy(element.getFileSize());
				}
				if(toMarkDownloaded.size() >= MARK_DOWNLOADED_THRESHOLD){
					markDownloaded(toMarkDownloaded);
				}
				result.setDownloadTime(System.currentTimeMillis() - startDownload);
				log.debug("Downloaded file in {}", Duration.ofMillis(result.getDownloadTime()));
				while(pause){
					try{
						Thread.sleep(10_000);
					}
					catch(InterruptedException e){
						log.error("Error while sleeping", e);
					}
				}
			}
		}
		catch(StopDownloaderException e){
			//skip
		}
		while(!markDownloaded(toMarkDownloaded)){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
				log.error("Error sleeping", e);
			}
		}
		return results;
	}
	
	private static void setAttributes(@NotNull Path path, @NotNull FileTime fileTime){
		for(var attribute : Arrays.asList("creationTime", "lastAccessTime", "lastModifiedTime")){
			try{
				Files.setAttribute(path, attribute, fileTime);
			}
			catch(Exception e){
				log.warn("Error setting file attributes for {}", path, e);
			}
		}
	}
	
	private boolean markDownloaded(@NotNull Collection<DownloadElement> elements){
		if(!elements.isEmpty()){
			var updated = storage.setDownloaded(elements);
			if(updated == elements.size()){
				elements.clear();
				return true;
			}
			return false;
		}
		return true;
	}
	
	public void close(){
		stop = true;
	}
	
	public void resume(){
		pause = false;
	}
	
	public void pause(){
		pause = true;
	}
}
