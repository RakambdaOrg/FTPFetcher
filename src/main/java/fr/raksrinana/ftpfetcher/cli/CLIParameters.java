package fr.raksrinana.ftpfetcher.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@NoArgsConstructor
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
	private Path databasePath = Paths.get("FTPFetcher.db");
	@Parameter(names = {
			"-h",
			"--help"
	}, help = true)
	private boolean help = false;
}
