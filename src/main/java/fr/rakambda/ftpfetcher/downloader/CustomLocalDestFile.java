package fr.rakambda.ftpfetcher.downloader;

import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

@Log4j2
public class CustomLocalDestFile extends FileSystemFile{
	private final int bufferSize;
	private final Double bytesPerSecond;
	
	public CustomLocalDestFile(@NonNull File file, int bufferSize, @Nullable Double bytesPerSecond){
		super(file);
		this.bufferSize = bufferSize;
		this.bytesPerSecond = bytesPerSecond;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException{
		var os = new BufferedOutputStream(super.getOutputStream(), bufferSize);
		
		if(Objects.isNull(bytesPerSecond) || bytesPerSecond <= 0){
			log.debug("Created un-throttled output stream");
			return os;
		}

		log.debug("Created throttled output stream with bps {}", bytesPerSecond);
		return new ThrottledOutputStream(os, bytesPerSecond);
	}
}
