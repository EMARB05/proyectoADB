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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainController {
    @FXML
    private Label lblEstadoAdb;
    @FXML
    private ListView<String> listaDispositivos;
    @FXML
    private StackPane panelCentral;
    @FXML
    private javafx.scene.layout.VBox panelLista;

    private final ADBService adbService = new ADBService();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();

    // Hilo que comprueba dispositivos conectados cada 3 segundos
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        // Usamos runLater para esperar a que la UI esté montada
        Platform.runLater(() -> {
            // Obtenemos el Stage a partir de cualquier nodo (el panelLista, por ejemplo)
            Stage stage = (Stage) panelLista.getScene().getWindow();
            if (stage != null) {
                setupResponsive(stage);
            }
        });

        iniciarDeteccionAutomatica();
    }

    public void setupResponsive(Stage stage) {
        // 1. Definir una función que compruebe ambos estados
        Runnable checkState = () -> {
            boolean expandido = stage.isFullScreen() || stage.isMaximized();
            actualizarMargen(expandido);
        };

        // 2. Escuchar cambios en FullScreen
        stage.fullScreenProperty().addListener((obs, old, isNowFull) -> checkState.run());

        // 3. Escuchar cambios en Maximized
        stage.maximizedProperty().addListener((obs, old, isNowMax) -> checkState.run());

        // 4. Aplicar estado inicial
        checkState.run();
    }

    private void actualizarMargen(boolean esPantallaCompleta) {
        if (esPantallaCompleta) {
            HBox.setMargin(panelLista, new Insets(0, 0, 0, 50));
            panelLista.setPadding(new Insets(30, 24, 30, 24)); 
            panelLista.setMinWidth(320); 
            panelLista.setMaxWidth(320);
        } else {
            HBox.setMargin(panelLista, new Insets(0, 0, 0, 0));
            panelLista.setPadding(new Insets(16)); // Padding estándar
            panelLista.setMinWidth(240);
            panelLista.setMaxWidth(240);
        }
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
        // Override de setCellFactory, para cambiar el estilo del cursor para los
        // dispositivos cargados
        listaDispositivos.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setCursor(javafx.scene.Cursor.DEFAULT); // Flecha normal si está vacío
                } else {
                    setText(item);
                    setCursor(javafx.scene.Cursor.HAND); // Mano si hay un dispositivo
                }
            }
        });
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
    private void cargarPanel(String fxmlPath, Dispositivo dispositivo) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Node panel = loader.load();

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
