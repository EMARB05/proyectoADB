package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Cargamos el archivo FXML que acabamos de crear
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/example/View/MainView.fxml"));
            
            // Creamos la escena con un tamaño inicial
            Scene scene = new Scene(fxmlLoader.load(), 750, 500);
            
            // Configuramos la ventana principal
            stage.setTitle("AEA Mobile Suite - Ingeniería de Dispositivos");
            stage.setScene(scene);
            
            // Evitamos que se pueda deformar demasiado el diseño
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            
            stage.show();
        } catch (IOException e) {
            System.err.println("Error cargando el archivo FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Método estático que lanza la aplicación JavaFX
        launch();
    }
}