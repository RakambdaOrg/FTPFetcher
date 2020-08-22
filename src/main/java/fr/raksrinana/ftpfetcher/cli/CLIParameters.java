package fr.raksrinana.ftpfetcher.cli;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("FieldMayBeFinal")
@NoArgsConstructor
@Getter
@CommandLine.Command(name = "ftpfetcher", mixinStandardHelpOptions = true)
public class CLIParameters{
	@CommandLine.Option(names = {
			"-t",
			"--threads"
	}, description = "The number of threads to use (must be >= 1)")
	private int threadCount = 1;
	@CommandLine.Option(names = {
			"-p",
			"--properties"
	}, description = "The settings properties to use", required = true)
	private Path properties;
	@CommandLine.Option(names = {
			"-db",
			"--database"
	}, description = "The path to the database to store downloads to")
	private Path databasePath = Paths.get("FTPFetcher.db");
}
