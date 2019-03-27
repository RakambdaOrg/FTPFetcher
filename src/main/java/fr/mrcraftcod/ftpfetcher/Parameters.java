package fr.mrcraftcod.ftpfetcher;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import java.io.File;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 01/09/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-09-01
 */
public class Parameters{
	@Parameter(names = {
			"-t",
			"--threads"
	}, description = "The number of threads to use (must be >= 1)")
	private final int threadCount = 1;
	
	@Parameter(names = {
			"-p",
			"--properties"
	}, description = "The settings properties to use", converter = FileConverter.class, required = true)
	private File properties;
	
	public Parameters(){
	}
	
	public File getProperties(){
		return this.properties;
	}
	
	public int getThreadCount(){
		return this.threadCount;
	}
}
