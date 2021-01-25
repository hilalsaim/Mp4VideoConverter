package view;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import conversor.VideoInformationProvider;
import org.apache.commons.io.FilenameUtils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.Duration;
import application.ConverterVideoApp;
import conversor.FFMpegWrapper;
import conversor.FFProbeWrapper;
import conversor.ProgressListener;
import model.Video;
import utils.DirectoryUtils;
import utils.NumberUtils;

public class ConverterVideoController implements Initializable {

	private final List<String> formatVideoCompativeis = new ArrayList<String>(Arrays.asList("mp4", "webm", "mkv", "flv"
			, "vob", "mov", "avi", "wmv", "m4p", "3gp", "mpeg", "mpg"));

	FFMpegWrapper ffmpegWrapper;
	FFProbeWrapper ffprobeWrapper;
	private ConverterVideoApp converterVideoApp;

	int currentFile = 1;

	@FXML private TabPane tabPane;

	@FXML private Button btChooseFiles;
	@FXML private Button btStartConversion;

	@FXML private TextField txOutputPath;

	@FXML private Label lbSize;
	@FXML private Label lbSpeed;
	@FXML private ProgressBar pbProgressConversion;
	@FXML private ProgressBar pbProgressConversionCurrentFile;
	@FXML private Label lbCompatibleFormats;
	@FXML private Label lbCurrentFileName;
	@FXML private Label lbArchiveCurrent;
	@FXML private Label lbTotalTimeArchiveCurrent;
	@FXML private Label lbCurrentTimeCurrentFile;


	@FXML private TableView<Video> tvVideoAConverter;
	@FXML private TableColumn<Video, String> colName;
	@FXML private TableColumn<Video, String> colPath;
	@FXML private TableColumn<Video, String> colSize;
	@FXML private TableColumn<Video, String> colResolution;
	@FXML private TableColumn<Video, String> colActions;


	public void initialize(URL location, ResourceBundle resources) {
		initializeTableColumns();
		initializeTooltips();
		initializeListeners();
		configureView();
	}

	private void configureView() {
		lbCompatibleFormats.setText("Supported Formats:"+formatVideoCompativeis);
	}

	private void initializeTableColumns() {
		colName.setCellValueFactory(cellData -> cellData.getValue().getName());
		colPath.setCellValueFactory(cellData -> cellData.getValue().getPath());
		colSize.setCellValueFactory(cellData -> cellData.getValue().getSize());
		colResolution.setCellValueFactory(cellData -> cellData.getValue().getResolution());
		colActions.setCellFactory( createActionCellValueFactory() ); //Important action in this method to add buttons dynamically
	}

	private void initializeTooltips() {
		Tooltip ttAllowedFormats = new Tooltip();
		ttAllowedFormats.setText("Video: "+ formatVideoCompativeis);
		hackTooltipStartTiming(ttAllowedFormats);
		btChooseFiles.setTooltip(ttAllowedFormats);
	}

	private void initializeListeners() {
		//tableViewListener
		tvVideoAConverter.setRowFactory(new Callback<TableView<Video>, TableRow<Video>>() {
			@Override  
			public TableRow<Video> call(TableView<Video> tableView) {
				final TableRow<Video> row = new TableRow<>();
				final ContextMenu contextMenu = new ContextMenu();  
				final MenuItem removeMenuItem = new MenuItem("Remove");
				removeMenuItem.setOnAction(new EventHandler<ActionEvent>() {  
					@Override  
					public void handle(ActionEvent event) {  
						tvVideoAConverter.getItems().remove(row.getItem());
					}  
				});  
				contextMenu.getItems().add(removeMenuItem);  
				// Set context menu on row, but use a binding to make it only show for non-empty rows:  
				row.contextMenuProperty().bind(  
						Bindings.when(row.emptyProperty())  
						.then((ContextMenu)null)  
						.otherwise(contextMenu)  
						);  
				return row;  
			}  
		});
	}

	@FXML 
	private void actionChooseFiles() throws Exception {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setTitle("Choose the file");
		List<String> allowedFormats = new ArrayList<String>();
		allowedFormats.addAll(formatVideoCompativeis);

		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Vídeo: ("+String.join(", ", allowedFormats)+")"
				, "*.mp4", "*.webm", "*.mkv", "*.flv"
				, "*.vob", "*.mov", "*.avi", "*.wmv"
				,"*.m4p", "*.3gp", "*.mpeg", "*.mp3"
				, "*.wav", "*.aac","*.flac", "*.wav"
				, "*.wma", "*.ogg", "*.mpg"); 

		fileChooser.getExtensionFilters().add(extFilter);

		List<File> arquivosEscolhidos = fileChooser.showOpenMultipleDialog(btChooseFiles.getScene().getWindow());

		if (arquivosEscolhidos != null) {
			VideoInformationProvider mediaInfoProvider = new VideoInformationProvider();
			for (File video : arquivosEscolhidos) {
				try {
					converterVideoApp.getVideosAConverter().add(new Video(video.getName(),
							video.getAbsolutePath(),
							VideoInformationProvider.bytesToMB(video.length()) + " Mb",
							mediaInfoProvider.getMediaResolutionAsString(video),
							video));
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
			tvVideoAConverter.setItems(converterVideoApp.getVideosAConverter());
		}
		btStartConversion.setDisable(false);
	}

	@FXML
	private void actionEscolherDestino() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Choose the target directory of the files");
		dirChooser.setInitialDirectory(getUserDirectory());

		File selectedDirectory = dirChooser.showDialog(btChooseFiles.getScene().getWindow());

		if (selectedDirectory != null)
			txOutputPath.setText(selectedDirectory.getAbsolutePath() + File.separator);
	}

	@FXML
	private void actionProximaEtapa() {
		tabPane.getSelectionModel().selectNext();
	}



	@FXML
	private void actionIniciarConversao() throws Exception {

		this.tabPane.getSelectionModel().selectNext();
		ffmpegWrapper = new FFMpegWrapper();
		ffprobeWrapper = new FFProbeWrapper();
		ProgressListener progressListener = createProgressListener();
		DirectoryUtils.createDirIfNonExistant(txOutputPath.getText());

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				for (Video mediaAtual : tvVideoAConverter.getItems()) {
					Platform.runLater(() -> updateCurrentFileInfo(mediaAtual));
					handleConversao(mediaAtual, progressListener);
					currentFile++;
				}
				return null;
			}
		};
		Thread conversorThread = new Thread(task);
		conversorThread.setDaemon(true); //If main closes, daemon threads also close
		conversorThread.start();
	}



	private void onConversaoConcluida() {
		pbProgressConversion.setProgress(1);
		pbProgressConversionCurrentFile.setProgress(1);

		showDialog(AlertType.INFORMATION, 
				"Conversion complete");

		tabPane.getTabs().get(0).setDisable(true);
		tabPane.getTabs().get(1).setDisable(true);
	}

	private void onErroConversao() {
		String fileConversionStatus = new String();
		for (int i = 0; i < getArquivosAConverter().size(); i++) {
			if (i < currentFile - 1) {
				fileConversionStatus.concat("Converted: " + getArquivosAConverter().get(i).getName() + "\n");
			} else {
				fileConversionStatus.concat("NOT converted: " + getArquivosAConverter().get(i).getName() + "\n");
			}
		}

		showDialog(AlertType.ERROR,
				"An error occurred while converting the file "
				);

	}

	private void updateProgressBars(String time) {
		int secondsConversionCurrent = NumberUtils.timeStringToSeconds(time);
		int durationArchiveCurrent = NumberUtils.timeStringToSeconds(lbTotalTimeArchiveCurrent.getText());
		double progressCurrent = secondsConversionCurrent / (double) durationArchiveCurrent;

		pbProgressConversionCurrentFile.setProgress(progressCurrent);

	}



	/**
	 * Updates the total time, current filename, and "current file" text
	 * @param videoAtual
	 */
	private void updateCurrentFileInfo (Video videoAtual) {
		lbCurrentFileName.setText(videoAtual.getName().get());
		lbArchiveCurrent.setText(currentFile + " de " + getArquivosAConverter().size());
	}

	private void handleConversao(Video videoAtual, ProgressListener progressListener) {
		File arquivoConversaoAtual = new File(videoAtual.getPath().get());
		int[] resolution = ffprobeWrapper.getMediaResolutionAsArray(arquivoConversaoAtual);

		ArrayList<String> params = new ArrayList<String>();
		String extensaoMidia = FilenameUtils.getExtension(videoAtual.getFile().getAbsolutePath());
		if (formatVideoCompativeis.contains(extensaoMidia.toLowerCase())) {
			params = ffmpegWrapper.construirParametrosFfmpegVideo(
					arquivoConversaoAtual.getAbsolutePath(),
					arquivoConversaoAtual.getName(),
					txOutputPath.getText(),
					"192000",
					resolution[0],
					resolution[1]);
		}

		if (!params.isEmpty())
			ffmpegWrapper.converterVideo(params, progressListener);
		else {
			showDialog(AlertType.ERROR, "Conversion parameters could not be built");
		}
	}

	private ProgressListener createProgressListener() {
		return new ProgressListener() {

			public void progress(int arquivoAtual, int porcentagem) {
				Platform.runLater ( () ->pbProgressConversion.setProgress(porcentagem /(double) 100));
			}

			@Override
			public void progress(int porcentagem) {
				Platform.runLater ( () ->pbProgressConversionCurrentFile.setProgress(porcentagem /(double) 100));

			}

			@Override
			public void message(String mensagem) {
				Platform.runLater(() -> updateStatusProperties(mensagem));
			}

			@Override
			public void complete(String pathToConvertedFile) {

				if (currentFile == getArquivosAConverter().size()) {
					Platform.runLater(() -> onConversaoConcluida());
				}
			}

			@Override
			public void error() {
				Platform.runLater(() -> onErroConversao());
			}
		};
	}


	/**
	 * Updates all messages in the status bar by splitting the message output from FFMPEG's Stream
	 * Updates the progress bars also
	 * @param mensagem
	 */
	private void updateStatusProperties(String mensagem) {
		try {
			HashMap<String, String> properties = ffmpegWrapper.getFFmpegPropertiesAsHash(mensagem);
			if (properties != null) {
				if (properties.containsKey("size"))
					lbSize.setText("Size: " + properties.get("size"));
				if (properties.containsKey("speed"))
					lbSpeed.setText("velocity: " + properties.get("speed"));
				if (properties.containsKey("time")) {
					lbCurrentTimeCurrentFile.setText(properties.get("time"));
					updateProgressBars(properties.get("time"));
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	/**
	 * Método utilizado para customizar o delay até o aparecimento de uma tooltip
	 * @param tooltip
	 */
	public static void hackTooltipStartTiming(Tooltip tooltip) {
		try {
			Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
			fieldBehavior.setAccessible(true);
			Object objBehavior = fieldBehavior.get(tooltip);

			Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
			fieldTimer.setAccessible(true);
			Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

			objTimer.getKeyFrames().clear();
			objTimer.getKeyFrames().add(new KeyFrame(new Duration(250)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Callback<TableColumn<Video, String>, TableCell<Video, String>> createActionCellValueFactory() {

		return new Callback<TableColumn<Video, String>, TableCell<Video, String>>()
		{
			@Override
			public TableCell call( final TableColumn<Video, String> param )
			{
				TableCell<Video, String> cell = new TableCell<Video, String>()
				{

					Button btExcluir = new Button( "Remove");

					@Override
					public void updateItem( String item, boolean empty )
					{
						super.updateItem( item, empty );
						if ( empty )
						{
							setGraphic( null );
							setText( null );
						}
						else
						{
							btExcluir.setOnAction( ( ActionEvent event ) ->
							{
								Video video = getTableView().getItems().remove( getIndex());
								System.out.println("Removing "+ video.getName() + "   " + video.getResolution() );
							} );
							setGraphic( btExcluir );
							setText( null );
						}
					}
				};
				return cell;
			}
		};
	}

	public void showDialog(AlertType type, String header) {
		Alert alert = new Alert(type);

		switch (type) {
		case ERROR:
			alert.setTitle("Error");
			break;
		case INFORMATION:
			alert.setTitle("Information");
			break;
		case CONFIRMATION:
			alert.setTitle("Success");
			break;
		case WARNING:
			alert.setTitle("Warning");
			break;
		}
		alert.setHeaderText(header);
		alert.showAndWait();
	}

	public File getUserDirectory() {
		String userDirectoryString = System.getProperty("user.home");
		File userDirectory = new File(userDirectoryString);
		if(!userDirectory.canRead()) {
			userDirectory = new File("c:/");
		}
		return userDirectory;
	}

	public List<File> getArquivosAConverter() {
		return tvVideoAConverter.getItems().stream().map(Video::getFile).collect(Collectors.toList());
	}

	public void setConverterVideoApp(ConverterVideoApp converterVideoApp) {
		this.converterVideoApp = converterVideoApp;
	}


}
