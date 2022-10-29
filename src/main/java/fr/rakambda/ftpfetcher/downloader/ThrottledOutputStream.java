package fr.rakambda.ftpfetcher.downloader;

import com.google.common.util.concurrent.RateLimiter;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.OutputStream;

@SuppressWarnings("UnstableApiUsage")
public final class ThrottledOutputStream extends OutputStream{
	private final OutputStream out;
	private final RateLimiter rateLimiter;
	
	public ThrottledOutputStream(@NotNull OutputStream out, double bytesPerSecond){
		this.out = out;
		rateLimiter = RateLimiter.create(bytesPerSecond);
	}
	
	@Override
	public void write(int b) throws IOException{
		rateLimiter.acquire();
		out.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException{
		rateLimiter.acquire(b.length);
		out.write(b);
	}
	
	@Override
	public void write(byte @NotNull [] b, int off, int len) throws IOException{
		rateLimiter.acquire(len);
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
