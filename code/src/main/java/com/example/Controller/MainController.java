package com.example.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private ListView<String> lvDispositivos;
    @FXML private StackPane contentArea; // El centro del BorderPane

    private ADBService adbService = new ADBService();

    @FXML
    public void initialize() {
        // Iniciamos el escaneo en segundo plano para el Sidebar
        iniciarEscaneoAutomatico();
        
        // Cargamos la vista por defecto (el Dashboard inicial)
        loadView("HomeView"); 
        lvDispositivos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
    if (newSelection != null) {
        System.out.println("Dispositivo seleccionado: " + newSelection);
        // Aquí es donde cargaremos la pantalla de especificaciones
        mostrarDetallesDispositivo(newSelection);
    }
});
    }

    private void iniciarEscaneoAutomatico() {
        ScheduledService<List<String>> servicioEscaneo = new ScheduledService<>() {
            @Override
            protected Task<List<String>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<String> call() throws Exception {
                        return adbService.obtenerDispositivosConectados();
                    }
                };
            }
        };
        servicioEscaneo.setPeriod(Duration.seconds(5));
        servicioEscaneo.setOnSucceeded(e -> {
            List<String> conectados = servicioEscaneo.getValue();
            if (conectados != null && !lvDispositivos.getItems().equals(conectados)) {
                lvDispositivos.getItems().setAll(conectados);
            }
        });
        servicioEscaneo.start();
    }

    // Método para cambiar de pantalla en el centro
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/View/" + fxmlFile + ".fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Error cargando " + fxmlFile + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleMenuDashboard() { loadView("HomeView"); }

    @FXML
    private void handleMenuDispositivos() { loadView("DispositivosView"); }

   private void mostrarDetallesDispositivo(String serial) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/View/DispositivosView.fxml"));
        Parent view = loader.load();
        
        // --- AQUÍ ESTÁ EL TRUCO ---
        // Accedemos al controlador de la vista que acabamos de cargar
        DispositivosViewController controller = loader.getController();
        
        // Le pasamos el serial para que se ponga a escanear
        controller.cargarDatos(serial);

        contentArea.getChildren().setAll(view);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}