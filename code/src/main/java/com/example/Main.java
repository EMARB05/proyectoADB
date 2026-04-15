package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.example.View.MainController;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/main.fxml")
        );
        Scene scene = new Scene(loader.load(), 900, 600);

        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        // stage.setMaximized(true);
        stage.setTitle("Android Engineering & Automation Suite");
        stage.setScene(scene);
        
        // Parar el scheduler al cerrar
        MainController controller = loader.getController();
        stage.setOnCloseRequest(e -> controller.detener());

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}