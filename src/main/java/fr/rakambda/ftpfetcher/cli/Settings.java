package fr.rakambda.ftpfetcher.cli;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
@Log4j2
public class Settings{
	@JsonIgnore
	private static final ObjectReader objectReader;
	@JsonProperty("ftpHost")
	@NonNull
	private String ftpHost;
	@JsonProperty("ftpUser")
	@NonNull
	private String ftpUser;
	@JsonProperty("ftpPass")
	@NonNull
	private String ftpPass;
	@JsonProperty("knownHosts")
	private String knownHosts;
	@JsonProperty("folders")
	@NonNull
	private List<FolderSettings> folders;
	
	@NonNull
	public static Optional<Settings> loadSettings(@NonNull Path path){
		if(path.toFile().exists()){
			try(var fis = new FileInputStream(path.toFile())){
				return Optional.ofNullable(objectReader.readValue(fis));
			}
			catch(IOException e){
				log.error("Failed to read settings in {}", path, e);
			}
		}
		return Optional.empty();
	}
	
	static{
		var mapper = new ObjectMapper();
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectReader = mapper.readerFor(Settings.class);
	}
}
