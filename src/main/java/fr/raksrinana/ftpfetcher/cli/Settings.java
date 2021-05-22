package fr.raksrinana.ftpfetcher.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
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
	@NotNull
	private String ftpHost;
	@JsonProperty("ftpUser")
	@NotNull
	private String ftpUser;
	@JsonProperty("ftpPass")
	@NotNull
	private String ftpPass;
	@JsonProperty("folders")
	@NotNull
	private List<FolderSettings> folders;
	
	@NonNull
	public static Optional<Settings> loadSettings(@NotNull Path path){
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
