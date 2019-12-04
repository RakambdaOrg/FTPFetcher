package fr.raksrinana.ftpfetcher.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class FolderSettings{
	@JsonProperty("recursive")
	private final boolean recursive = false;
	@JsonProperty("fileFilter")
	private final String fileFilter = ".*";
	@JsonProperty("localFolder")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path localFolder;
	@JsonProperty("ftpFolder")
	private String ftpFolder;
}
