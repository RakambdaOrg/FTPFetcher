package fr.rakambda.ftpfetcher.downloader;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;

@Log4j2
@SuppressWarnings("UnstableApiUsage")
public final class ThrottledOutputStream extends OutputStream{
	private final OutputStream out;
	private final RateLimiter rateLimiter;
	
	public ThrottledOutputStream(@NonNull OutputStream out, double bytesPerSecond){
		this.out = out;
		rateLimiter = RateLimiter.create(bytesPerSecond);
	}
	
	@Override
	public void write(int b) throws IOException{
		var waited = rateLimiter.acquire();
		log.debug("Waited {} seconds", waited);
		out.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException{
		var waited = rateLimiter.acquire(b.length);
		log.debug("Waited {} seconds", waited);
		out.write(b);
	}
	
	@Override
	public void write(byte @NonNull [] b, int off, int len) throws IOException{
		var waited = rateLimiter.acquire(len);
		log.debug("Waited {} seconds", waited);
		out.write(b, off, len);
	}
	
	@Override
	public void flush() throws IOException{
		out.flush();
	}
	
	@Override
	public void close() throws IOException{
		out.close();
	}
	
	public void setRate(double bytesPerSecond){
		rateLimiter.setRate(bytesPerSecond);
	}
}
