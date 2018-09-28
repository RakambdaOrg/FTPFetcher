package fr.mrcraftcod.ftpfetcher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.kohsuke.args4j.Option;
import java.io.File;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 01/09/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-09-01
 */
public class Parameters{
	private int threadCount = 1;
	private File properties;
	
	enum LogLevel{
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL,
	}
	
	public Parameters(){
	}
	
	public File getProperties(){
		return this.properties;
	}
	
	@Option(name = "-p", aliases = "--properties", usage = "The log4j2 properties to use")
	public void setProperties(final File value){
		this.properties = value;
	}
	
	public int getThreadCount(){
		return this.threadCount;
	}
	
	@Option(name = "-t", aliases = "--threads", usage = "The number of threads to use (must be >= 1)")
	public void setThreadCount(final int value){
		if(value < -0){
			throw new IllegalArgumentException("value must be >= 0");
		}
		
		this.threadCount = value;
	}
	
	@Option(name = "-l", aliases = "--logLevel", usage = "The log level")
	public void setLogLevel(final LogLevel value){
		final String levelName = value.toString();
		final Level level = Level.getLevel(levelName);
		Configurator.setRootLevel(level);
		Configurator.setLevel("fr.mrcraftcod.utils", level);
		Configurator.setLevel("fr.mrcraftcod.ftpfetcher", level);
	}
}
