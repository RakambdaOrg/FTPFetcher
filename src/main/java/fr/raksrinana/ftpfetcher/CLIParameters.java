package fr.raksrinana.ftpfetcher;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 01/09/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-09-01
 */
@SuppressWarnings("FieldMayBeFinal")
public class CLIParameters{
	@Parameter(names = {
			"-t",
			"--threads"
	}, description = "The number of threads to use (must be >= 1)")
	private int threadCount = 1;
	@Parameter(names = {
			"-p",
			"--properties"
	}, description = "The settings properties to use", converter = PathConverter.class, required = true)
	private Path properties;
	
	@Parameter(names = {
			"-db",
			"--database"
	}, description = "The path to the database to store downloads to", converter = PathConverter.class)
	private Path databaseFile = Paths.get("FTPFetcher.db");
	
	@Parameter(names = {
			"-h",
			"--help"
	}, help = true)
	private boolean help = false;
	
	public CLIParameters(){
	}
	
	public Path getDatabasePath(){
		return databaseFile;
	}
	
	public int getThreadCount(){
		return this.threadCount;
	}
	
	public Path getProperties(){
		return this.properties;
	}
}
