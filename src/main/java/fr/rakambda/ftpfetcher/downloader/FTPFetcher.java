package fr.rakambda.ftpfetcher.downloader;

import fr.rakambda.ftpfetcher.cli.Settings;
import fr.rakambda.ftpfetcher.model.DownloadElement;
import fr.rakambda.ftpfetcher.model.DownloadResult;
import fr.rakambda.ftpfetcher.storage.IStorage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.xfer.LocalDestFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Callable;

@Log4j2
public class FTPFetcher implements Callable<Collection<DownloadResult>>{
	private static final int MARK_DOWNLOADED_THRESHOLD = 25;
	private final Settings settings;
	private final IStorage storage;
	private final Collection<DownloadElement> downloadElements;
	private final ProgressBarHandler progressBar;
	@Setter
	private Double bytesPerSecond;
	private boolean stop;
	private boolean pause;
	
	public FTPFetcher(@NotNull Settings settings, @NotNull IStorage storage, @NotNull Collection<DownloadElement> downloadElements, @NotNull ProgressBarHandler progressBar, @Nullable Double bytesPerSecond){
		this.settings = settings;
		this.storage = storage;
		this.downloadElements = downloadElements;
		this.progressBar = progressBar;
		this.bytesPerSecond = bytesPerSecond;
		stop = false;
		pause = false;
	}
	
	@Override
	@NotNull
	public Collection<DownloadResult> call() throws IOException{
		var results = new LinkedList<DownloadResult>();
		var toMarkDownloaded = new ArrayList<DownloadElement>(MARK_DOWNLOADED_THRESHOLD);
		try(var connection = new FTPConnection(settings)){
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
				if(downloaded){
					try{
						long fileLength = Files.size(fileOut);
						var expectedFileLength = element.getFileSize();
						if(expectedFileLength != fileLength){
							downloaded = false;
							log.warn("Sizes mismatch expected:{} actual:{} difference: {}", expectedFileLength, fileLength, fileLength - expectedFileLength);
						}
					}
					catch(IOException ignored){
					}
				}
				else{
					try{
						var dest = new CustomLocalDestFile(element.getFileOut().toFile(), 512 * 1024, bytesPerSecond);
						connection.getSftp().get(element.getRemotePath(), dest);
						setAttributes(element, dest);
						setFilePermissions(element);
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
	
	private void setFilePermissions(@NotNull DownloadElement element) throws IOException{
		try{
			if(Objects.nonNull(element.getPermissions())){
				Files.setPosixFilePermissions(element.getFileOut(), element.getPermissions());
			}
		}
		catch(Exception e){
			log.warn("Error setting file permissions for {}", element.getFileOut(), e);
		}
	}
	
	private static void setAttributes(@NotNull DownloadElement element, @NotNull LocalDestFile dest){
		try{
			var attrs = element.getAttributes();
			if(attrs.has(FileAttributes.Flag.ACMODTIME)){
				dest.setLastAccessedTime(attrs.getAtime());
				dest.setLastModifiedTime(attrs.getMtime());
			}
		}
		catch(Exception e){
			log.warn("Error setting file attributes for {}", element.getFileOut(), e);
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
