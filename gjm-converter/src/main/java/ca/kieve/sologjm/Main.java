package ca.kieve.sologjm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        // Initialize Managers
        LogManager.getInstance();
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        setUserAgentStylesheet(STYLESHEET_MODENA);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout/main.fxml"));
        Parent root = loader.load();
        ((MainController) loader.getController()).setStage(stage);

        stage.setTitle("SOLO GJM Converter | By kieve");
        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }
}
