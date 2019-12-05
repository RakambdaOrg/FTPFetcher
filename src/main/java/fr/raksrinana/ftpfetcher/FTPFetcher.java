package fr.raksrinana.ftpfetcher;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fr.raksrinana.ftpfetcher.settings.Settings;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class FTPFetcher implements Callable<List<DownloadResult>>{
	private final Settings settings;
	private final Configuration config;
	private final ConcurrentLinkedQueue<DownloadElement> downloadSet;
	private final JSch jsch;
	private final ProgressBarHandler progressBar;
	
	public FTPFetcher(final JSch jsch, final Settings settings, final Configuration config, final ConcurrentLinkedQueue<DownloadElement> downloadSet, final ProgressBarHandler progressBar){
		this.jsch = jsch;
		this.settings = settings;
		this.config = config;
		this.downloadSet = downloadSet;
		this.progressBar = progressBar;
	}
	
	@Override
	public List<DownloadResult> call() throws IOException, JSchException{
		final var connection = new FTPConnection(jsch, settings);
		final var results = new LinkedList<DownloadResult>();
		final var toSetDownloaded = new ArrayList<Path>();
		DownloadElement element;
		while((element = downloadSet.poll()) != null){
			final var startDownload = System.currentTimeMillis();
			final var result = new DownloadResult(element, false);
			results.add(result);
			final var fileOut = element.getFileOut().toFile();
			var downloaded = fileOut.exists();
			log.debug("Downloading file {}{}", element.getFolder(), element.getFile().getFilename());
			progressBar.setExtraMessage(element.getFile().getFilename());
			if(!downloaded){
				try(final var fos = Files.newOutputStream(element.getFileOut())){
					connection.getClient().get(element.getFolder() + element.getFile().getFilename(), fos);
					fos.flush();
					setAttributes(element.getFileOut(), FileTime.fromMillis(element.getFile().getAttrs().getATime() * 1000L));
					final var fileLength = fileOut.length();
					final var expectedFileLength = element.getFile().getAttrs().getSize();
					if(expectedFileLength == fileLength){
						downloaded = true;
					}
					else{
						log.warn("Sizes mismatch expected:{} actual:{} difference: {}", expectedFileLength, fileLength, fileLength - expectedFileLength);
					}
				}
				catch(final IOException e){
					log.warn("IO - Error downloading file", e);
					fileOut.deleteOnExit();
				}
				catch(final SftpException e){
					log.warn("SFTP - Error downloading file", e);
					fileOut.deleteOnExit();
					if(e.getCause().getMessage().contains("inputstream is closed") || e.getCause().getMessage().contains("Pipe closed")){
						connection.reopen();
					}
				}
			}
			if(downloaded){
				toSetDownloaded.add(Paths.get(element.getFolder()).resolve(element.getFile().getFilename().replace(":", ".")));
				progressBar.step();
			}
			if(toSetDownloaded.size() > 50){
				writeDownloaded(toSetDownloaded);
			}
			final var fileLength = fileOut.length();
			result.setDownloaded(downloaded && fileLength != 0 && fileLength == element.getFile().getAttrs().getSize());
			result.setDownloadTime(System.currentTimeMillis() - startDownload);
			log.debug("Downloaded file in {}", Duration.ofMillis(result.getDownloadTime()));
		}
		connection.close();
		writeDownloaded(toSetDownloaded);
		return results;
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
	
	private void writeDownloaded(final List<Path> toSetDownloaded){
		// try{
		if(toSetDownloaded.size() > 0){
			config.setDownloaded(toSetDownloaded);
			toSetDownloaded.clear();
		}
		// }
		// catch(final InterruptedException | TimeoutException | ExecutionException e){
		// 	log.error("Error setting downloaded status in DB", e);
		// }
	}
}
