package application;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.Video;
import view.ConverterVideoController;

public class ConverterVideoApp extends Application {
	
	private Stage stage;
	private TabPane rootLayout;
	
	private ObservableList<Video> videosAConverter = FXCollections.observableArrayList();
	
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setTitle("Mp4 Video Converter");
		stage.setResizable(false);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

		initRootLayout();
	}
	
	
	private void initRootLayout() throws IOException {
		FXMLLoader loader = new FXMLLoader();
				loader.setLocation(ConverterVideoApp.class.getResource("/ConverterVideoView.fxml"));
		rootLayout = (TabPane) loader.load();
		
		Scene scene = new Scene(rootLayout);
		stage.setScene(scene);

		ConverterVideoController controller = loader.getController();
		controller.setConverterVideoApp(this);

		stage.show();
	}

	public ObservableList<Video> getVideosAConverter() {
		return videosAConverter;
	}
	
}
