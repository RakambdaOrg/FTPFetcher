package fr.raksrinana.ftpfetcher;

import fr.raksrinana.ftpfetcher.downloader.FTPFetcher;
import lombok.extern.log4j.Log4j2;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Handles commands sent in the standard input.
 */
@Log4j2
class ConsoleHandler extends Thread{
	private static final int WAIT_DELAY = 10000;
	private final Collection<FTPFetcher> fetchers = new LinkedList<>();
	private boolean stop;
	
	ConsoleHandler(){
		super();
		this.stop = false;
		this.setDaemon(true);
		this.setName("Console watcher");
		log.info("Console handler created");
	}
	
	@Override
	public void run(){
		log.info("Console handler started");
		try(final var sc = new Scanner(System.in)){
			while(!this.stop){
				try{
					if(!sc.hasNext()){
						try{
							Thread.sleep(WAIT_DELAY);
						}
						catch(final InterruptedException ignored){
						}
						continue;
					}
					final var line = sc.nextLine();
					final var args = new LinkedList<>(Arrays.asList(line.split(" ")));
					if(args.isEmpty()){
						continue;
					}
					final var command = args.poll();
					if("q".equals(command)){
						fetchers.forEach(FTPFetcher::resume);
						fetchers.forEach(FTPFetcher::close);
					}
					else if("p".equals(command)){
						fetchers.forEach(FTPFetcher::pause);
					}
					else if("r".equals(command)){
						fetchers.forEach(FTPFetcher::resume);
					}
				}
				catch(final Exception e){
					log.warn("Error executing console command", e);
				}
			}
		}
	}
	
	/**
	 * Close the console handler.
	 */
	void close(){
		this.stop = true;
	}
	
	public void addFetcher(final FTPFetcher fetcher){
		this.fetchers.add(fetcher);
	}
}
