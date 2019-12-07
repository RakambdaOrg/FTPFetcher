package fr.raksrinana.ftpfetcher.downloader;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.raksrinana.ftpfetcher.Database;
import fr.raksrinana.ftpfetcher.cli.Settings;
import fr.raksrinana.ftpfetcher.model.DownloadElement;
import fr.raksrinana.ftpfetcher.model.DownloadResult;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;

@Slf4j
public class FTPFetcher implements Callable<Collection<DownloadResult>>{
	private static final int MARK_DOWNLOADED_THRESHOLD = 25;
	private final Settings settings;
	private final Database database;
	private final Queue<DownloadElement> downloadElements;
	private final JSch jsch;
	private final ProgressBarHandler progressBar;
	
	public FTPFetcher(final JSch jsch, final Settings settings, final Database database, final Queue<DownloadElement> downloadElements, final ProgressBarHandler progressBar){
		this.jsch = jsch;
		this.settings = settings;
		this.database = database;
		this.downloadElements = downloadElements;
		this.progressBar = progressBar;
	}
	
	@Override
	public Collection<DownloadResult> call() throws JSchException{
		final var results = new LinkedList<DownloadResult>();
		final var toMarkDownloaded = new ArrayList<DownloadElement>(MARK_DOWNLOADED_THRESHOLD);
		try(final var connection = new FTPConnection(jsch, settings)){
			DownloadElement element;
			while((element = downloadElements.poll()) != null){
				final var startDownload = System.currentTimeMillis();
				final var result = new DownloadResult(element, false);
				results.add(result);
				final var fileOut = element.getFileOut().toFile();
				var downloaded = fileOut.exists();
				log.debug("Downloading file {}", element.getRemotePath());
				progressBar.setExtraMessage(element.getSftpFile().getFilename());
				if(!downloaded){
					try(final var fos = Files.newOutputStream(element.getFileOut())){
						connection.getSftpChannel().get(element.getRemotePath(), fos);
						fos.flush();
					}
					catch(final IOException e){
						log.warn("IO - Error downloading file", e);
						fileOut.deleteOnExit();
						continue;
					}
					catch(final SftpException e){
						log.warn("SFTP - Error downloading file", e);
						fileOut.deleteOnExit();
						if(e.getCause().getMessage().contains("inputstream is closed") || e.getCause().getMessage().contains("Pipe closed")){
							connection.reopen();
						}
						continue;
					}
					setAttributes(element.getFileOut(), FileTime.fromMillis(element.getSftpFile().getAttrs().getATime() * 1000L));
					final var fileLength = fileOut.length();
					final var expectedFileLength = element.getSftpFile().getAttrs().getSize();
					if(expectedFileLength == fileLength){
						downloaded = true;
					}
					else{
						log.warn("Sizes mismatch expected:{} actual:{} difference: {}", expectedFileLength, fileLength, fileLength - expectedFileLength);
					}
				}
				if(downloaded){
					element.setDownloadedAt(LocalDateTime.now());
					result.setDownloaded(true);
					toMarkDownloaded.add(element);
					progressBar.step();
				}
				if(toMarkDownloaded.size() >= MARK_DOWNLOADED_THRESHOLD){
					markDownloaded(toMarkDownloaded);
				}
				result.setDownloadTime(System.currentTimeMillis() - startDownload);
				log.debug("Downloaded file in {}", Duration.ofMillis(result.getDownloadTime()));
			}
		}
		while(!markDownloaded(toMarkDownloaded)){
			try{
				Thread.sleep(1000);
			}
			catch(final InterruptedException e){
				log.error("Error sleeping", e);
			}
		}
		return results;
	}
	
	private boolean markDownloaded(final Collection<DownloadElement> elements){
		if(!elements.isEmpty()){
			final var updated = database.setDownloaded(elements);
			if(updated == elements.size()){
				elements.clear();
				return true;
			}
			return false;
		}
		return true;
	}
	
	private static void setAttributes(final Path path, final FileTime fileTime){
		for(final var attribute : Arrays.asList("creationTime", "lastAccessTime", "lastModifiedTime")){
			try{
				Files.setAttribute(path, attribute, fileTime);
			}
			catch(final Exception e){
				log.warn("Error setting file attributes for {}", path, e);
			}
		}
	}
}
