package fr.rakambda.ftpfetcher.downloader;

import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class CustomLocalDestFile extends FileSystemFile{
	private final int bufferSize;
	private final Double bytesPerSecond;
	
	public CustomLocalDestFile(@NotNull File file, int bufferSize, @Nullable Double bytesPerSecond){
		super(file);
		this.bufferSize = bufferSize;
		this.bytesPerSecond = bytesPerSecond;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException{
		var os = new BufferedOutputStream(super.getOutputStream(), bufferSize);
		
		if(Objects.isNull(bytesPerSecond) || bytesPerSecond <= 0){
			return os;
		}
		
		return new ThrottledOutputStream(os, bytesPerSecond);
	}
}
