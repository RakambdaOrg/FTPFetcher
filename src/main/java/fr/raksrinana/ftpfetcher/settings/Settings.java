package fr.raksrinana.ftpfetcher.settings;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class Settings{
	@JsonIgnore
	private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
	@JsonIgnore
	private static final ObjectReader objectReader;
	@JsonProperty("ftpHost")
	private String ftpHost;
	@JsonProperty("ftpUser")
	private String ftpUser;
	@JsonProperty("ftpPass")
	private String ftpPass;
	@JsonProperty("folders")
	private List<FolderSettings> folders;
	
	@NonNull
	public static Optional<Settings> loadSettings(final Path path){
		if(path.toFile().exists()){
			try(final var fis = new FileInputStream(path.toFile())){
				return Optional.ofNullable(objectReader.readValue(fis));
			}
			catch(final IOException e){
				LOGGER.error("Failed to read settings in {}", path, e);
			}
		}
		return Optional.empty();
	}
	
	static{
		final var mapper = new ObjectMapper();
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE).withSetterVisibility(JsonAutoDetect.Visibility.NONE).withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectReader = mapper.readerFor(Settings.class);
	}
}
