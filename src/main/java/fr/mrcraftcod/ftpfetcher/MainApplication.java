package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.*;
import fr.mrcraftcod.utils.base.FileUtils;
import fr.mrcraftcod.utils.base.Log;
import fr.mrcraftcod.utils.javafx.ApplicationBase;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 13/03/2018.
 *
 * @author Thomas Couchoud
 * @since 2018-03-13
 */
public class MainApplication extends ApplicationBase
{
	private static final int THREAD_COUNT = 1;//Runtime.getRuntime().availableProcessors();
	private final ArrayList<ProgressBar> progressBars = new ArrayList<>();
	private ExecutorService executor;
	private Configuration config;
	private List<Future<?>> futures;
	private Session session;
	private ChannelSftp sftpChannel;
	private Button start;
	
	public static void main(String[] args)
	{
		launch(args);
	}
	
	public void bindProgressBar(long id, Task task)
	{
		try
		{
			ProgressBar bar = progressBars.get((int) (id % THREAD_COUNT));
			bar.progressProperty().unbind();
			bar.setProgress(-1);
			bar.progressProperty().bind(task.progressProperty());
			start.textProperty().unbind();
			start.textProperty().bind(task.messageProperty());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public String getFrameTitle()
	{
		return "FTPFetcher";
	}
	
	@Override
	public void preInit() throws Exception
	{
		super.preInit();
		config = new Configuration();
		config.removeUseless();
		executor = Executors.newFixedThreadPool(THREAD_COUNT);
		
		JSch.setConfig("StrictHostKeyChecking", "no");
		session = null;
		sftpChannel = null;
		
		try
		{
			JSch jsch = new JSch();
			jsch.setKnownHosts(FileUtils.getHomeFolder("/home/mrcraftcod/.ssh/known_hosts").getAbsolutePath());
			
			session = jsch.getSession(Settings.getString("ftpUser"), Settings.getString("ftpHost"));
			session.setPassword(Settings.getString("ftpPass"));
			
			session.connect();
			
			Channel channel = session.openChannel("sftp");
			channel.connect();
			
			session.setServerAliveInterval(20000);
			sftpChannel = (ChannelSftp) channel;
		}
		catch(JSchException e)
		{
			Log.warning("Error downloading folder: " + e.getMessage());
		}
	}
	
	@Override
	public Consumer<Stage> getStageHandler()
	{
		return stage -> stage.setOnCloseRequest(evt -> {
			executor.shutdownNow();
			config.close();
			if(sftpChannel != null && sftpChannel.isConnected())
				sftpChannel.exit();
			if(session != null && session.isConnected())
				session.disconnect();
		});
	}
	
	@Override
	public Consumer<Stage> getOnStageDisplayed() throws Exception
	{
		return null;
	}
	
	@Override
	public Parent createContent(Stage stage)
	{
		VBox box = new VBox();
		for(int i = 0; i < THREAD_COUNT; i++)
		{
			ProgressBar progressBar = new ProgressBar();
			progressBar.setMaxWidth(Double.MAX_VALUE);
			progressBar.setMaxHeight(Double.MAX_VALUE);
			box.getChildren().add(progressBar);
			progressBars.add(progressBar);
			VBox.setVgrow(progressBar, Priority.ALWAYS);
		}
		
		start = new Button("Start");
		start.setMaxWidth(Double.MAX_VALUE);
		start.setMaxHeight(Double.MAX_VALUE);
		start.setOnAction(evt -> {
			start.setDisable(true);
			start.setText("Fetching folders...");
			
			new Thread(() -> {
				List<DownloadFileTask> tasks = DownloadFileTask.fetchFolder(this, config, sftpChannel, Settings.getString("ftpFolder"), Paths.get(new File(".").toURI()).resolve(Settings.getString("localFolder")));
				Platform.runLater(() -> start.setText("Downloading..."));
				futures = tasks.stream().map(t -> {
					t.setOnSucceeded(evt2 -> {
						if(t.getValue())
						{
							try
							{
								config.setDownloaded(t.getRelativePath());
							}
							catch(InterruptedException e)
							{
								e.printStackTrace();
							}
						}
					});
					return executor.submit(t);
				}).collect(Collectors.toList());
				
				new Thread(() -> {
					futures.forEach(f -> {
						try
						{
							if(!f.isCancelled())
								f.get();
						}
						catch(CancellationException ignored)
						{
						
						}
						catch(InterruptedException | ExecutionException e)
						{
							e.printStackTrace();
						}
					});
					Platform.runLater(() -> {
						start.setDisable(false);
						start.textProperty().unbind();
						start.setText("Start");
					});
				}).start();
			}).start();
		});
		
		Button stop = new Button("Stop");
		stop.setMaxWidth(Double.MAX_VALUE);
		stop.setMaxHeight(Double.MAX_VALUE);
		stop.disableProperty().bind(start.disabledProperty().not());
		stop.setOnAction(evt -> {
			futures.forEach(f -> f.cancel(false));
			start.textProperty().unbind();
			start.setText("Start");
			start.setDisable(false);
		});
		
		box.getChildren().add(start);
		box.getChildren().add(stop);
		VBox.setVgrow(start, Priority.NEVER);
		VBox.setVgrow(stop, Priority.NEVER);
		
		
		return box;
	}
}
