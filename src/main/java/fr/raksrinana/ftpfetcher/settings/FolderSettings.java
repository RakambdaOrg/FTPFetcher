package fr.raksrinana.ftpfetcher.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderSettings{
	@SuppressWarnings("FieldMayBeFinal")
	@JsonProperty("recursive")
	private boolean recursive = false;
	@SuppressWarnings("FieldMayBeFinal")
	@JsonProperty("fileFilter")
	private String fileFilter = ".*";
	@JsonProperty("localFolder")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path localFolder;
	@JsonProperty("ftpFolder")
	private String ftpFolder;
	
	public FolderSettings(){
	}
	
	public String getFileFilter(){
		return fileFilter;
	}
	
	public String getFtpFolder(){
		return ftpFolder;
	}
	
	public Path getLocalFolder(){
		return localFolder;
	}
	
	public boolean isRecursive(){
		return recursive;
	}
}
