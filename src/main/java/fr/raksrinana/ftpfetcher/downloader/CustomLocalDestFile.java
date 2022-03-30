package fr.raksrinana.ftpfetcher.downloader;

import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class CustomLocalDestFile extends FileSystemFile{
	private final int bufferSize;
	
	public CustomLocalDestFile(@NotNull File file, int bufferSize){
		super(file);
		this.bufferSize = bufferSize;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException{
		return new BufferedOutputStream(super.getOutputStream(), bufferSize);
	}
}
