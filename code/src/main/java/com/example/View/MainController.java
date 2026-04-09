package com.example.View;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.Controller.ADBService;
import com.example.Controller.DispositivoDAO;
import com.example.Model.Dispositivo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML
    private Label lblEstadoAdb;
    @FXML
    private ListView<String> listaDispositivos;
    @FXML
    private StackPane panelCentral;

    private final ADBService adbService = new ADBService();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();

    // Hilo que comprueba dispositivos conectados cada 3 segundos
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        iniciarDeteccionAutomatica();
    }

    // Arranca un hilo que refresca la lista cada 3 segundos
    private void iniciarDeteccionAutomatica() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<String> seriales = adbService.obtenerDispositivosConectados();
                // Actualizamos la UI siempre en el hilo de JavaFX
                Platform.runLater(() -> actualizarLista(seriales));
            } catch (IOException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al ejecutar ADB"));
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void actualizarLista(List<String> seriales) {
        if (seriales.isEmpty()) {
            lblEstadoAdb.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● Ningún dispositivo conectado");
        } else {
            lblEstadoAdb.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● " + seriales.size() + " dispositivo(s) conectado(s)");
        }
        listaDispositivos.setItems(FXCollections.observableArrayList(seriales));
    }

    @FXML
    private void onRefrescar() {
        try {
            List<String> seriales = adbService.obtenerDispositivosConectados();
            actualizarLista(seriales);
        } catch (IOException e) {
            lblEstadoAdb.setText("● Error al ejecutar ADB");
        }
    }

    @FXML
    private void onSeleccionarDispositivo() {
        String serial = listaDispositivos.getSelectionModel().getSelectedItem();
        if (serial == null)
            return;

        try {
            Dispositivo dispositivo = dispositivoDAO.buscarPorSerial(serial);

            if (dispositivo == null) {
                // Serial desconocido → formulario de alta con datos de ADB
                Dispositivo desdeAdb = adbService.obtenerProps(serial);
                cargarPanel("/fxml/formulario_alta.fxml", desdeAdb);
            } else {
                // Serial conocido → ficha técnica desde la BBDD
                cargarPanel("/fxml/ficha_tecnica.fxml", dispositivo);
            }

        } catch (IOException | SQLException e) {
            lblEstadoAdb.setText("● Error al cargar dispositivo");
            e.printStackTrace();
        }
    }

    // Carga un fxml en el panel central y le pasa el dispositivo al controlador
    private void cargarPanel(String fxmlPath, Dispositivo dispositivo)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Pane panel = loader.load();

        // El controlador de cada panel implementa esta interfaz
        DispositivoAware controller = loader.getController();
        controller.setDispositivo(dispositivo);

        panelCentral.getChildren().setAll(panel);
    }

    // Detenemos el scheduler al cerrar la ventana para no dejar hilos huérfanos
    public void detener() {
        if (scheduler != null)
            scheduler.shutdown();
    }
}
