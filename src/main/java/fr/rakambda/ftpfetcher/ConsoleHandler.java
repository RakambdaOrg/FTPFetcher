package fr.rakambda.ftpfetcher;

import fr.rakambda.ftpfetcher.downloader.FTPFetcher;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Scanner;

/**
 * Handles commands sent in the standard input.
 */
@Log4j2
class ConsoleHandler extends Thread implements AutoCloseable {
	private static final int WAIT_DELAY = 10000;
	private final Collection<FTPFetcher> fetchers = new LinkedList<>();
	private boolean stop;
	
	ConsoleHandler(){
		super();
		stop = false;
		setDaemon(true);
		setName("Console watcher");
		log.info("Console handler created");
	}
	
	@Override
	public void run(){
		log.info("Console handler started");
		try(var sc = new Scanner(System.in)){
			while(!stop){
				try{
					if(!sc.hasNext()){
						try{
							Thread.sleep(WAIT_DELAY);
						}
						catch(InterruptedException ignored){
						}
						continue;
					}
					var line = sc.nextLine();
					var args = new LinkedList<>(Arrays.asList(line.split(" ")));
					if(args.isEmpty()){
						continue;
					}
					var command = args.poll();
					if("q".equals(command)){
						log.info("Exiting");
						fetchers.forEach(FTPFetcher::resume);
						fetchers.forEach(FTPFetcher::close);
					}
					else if("p".equals(command)){
						log.info("Pausing");
						fetchers.forEach(FTPFetcher::pause);
					}
					else if("r".equals(command)){
						log.info("Resuming");
						fetchers.forEach(FTPFetcher::resume);
					}
					else if("bps".equals(command)){
						var bps = Optional.ofNullable(args.poll())
								.map(Double::parseDouble)
								.orElse(null);
						log.info("Setting bps to {}", bps);
						fetchers.forEach(f -> f.setBytesPerSecond(bps));
					}
				}
				catch(Exception e){
					log.warn("Error executing console command", e);
				}
			}
		}
	}
	
	/**
	 * Close the console handler.
	 */
	@Override
	public void close() {
		stop = true;
	}
	
	public void addFetcher(@NonNull FTPFetcher fetcher){
		fetchers.add(fetcher);
	}
}
