package com.example.View;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @FXML private Label lblEstadoAdb;
    @FXML private ListView<String> listaDispositivos;
    @FXML private StackPane panelCentral;
    @FXML private javafx.scene.layout.VBox panelLista;
    @FXML private StackPane rootPane;
    private final ADBService adbService = new ADBService();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private ScheduledExecutorService scheduler;

    // Mapa interno: androidId -> serial (serial solo para comandos ADB, nunca se muestra)
    private final Map<String, String> mapaAndroidIdSerial = new LinkedHashMap<>();

    // ───────────────────── INIT ─────────────────────
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) panelLista.getScene().getWindow();
            if (stage != null) setupResponsive(stage);
        });

        iniciarDeteccionAutomatica();
    }

    public void setupResponsive(Stage stage) {
        Runnable checkState = () -> {
            boolean expandido = stage.isFullScreen() || stage.isMaximized();
            actualizarMargen(expandido);
        };
        stage.fullScreenProperty().addListener((obs, old, val) -> checkState.run());
        stage.maximizedProperty().addListener((obs, old, val) -> checkState.run());
        checkState.run();
    }

    private void actualizarMargen(boolean esPantallaCompleta) {
        if (esPantallaCompleta) {
            HBox.setMargin(panelLista, new Insets(0));
            panelLista.setPadding(new Insets(30, 24, 30, 24));
            panelLista.setMinWidth(320);
            panelLista.setMaxWidth(320);
        } else {
            HBox.setMargin(panelLista, new Insets(0));
            panelLista.setPadding(new Insets(16));
            panelLista.setMinWidth(240);
            panelLista.setMaxWidth(240);
        }
    }

    // ───────────────────── DETECCIÓN AUTOMÁTICA ─────────────────────
    
    private void iniciarDeteccionAutomatica() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, String> dispositivos = adbService.obtenerDispositivosConectados();
                Platform.runLater(() -> actualizarLista(dispositivos));
            } catch (IOException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al ejecutar ADB"));
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    // ───────────────────── LISTA ─────────────────────
    private void actualizarLista(Map<String, String> dispositivos) {
        if (dispositivos.isEmpty()) {
            lblEstadoAdb.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● Ningún dispositivo conectado");
        } else {
            lblEstadoAdb.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● " + dispositivos.size() + " dispositivo(s) conectado(s)");
        }

        // Actualiza el mapa interno
        mapaAndroidIdSerial.clear();
        mapaAndroidIdSerial.putAll(dispositivos);

        // La lista solo muestra android_ids
        List<String> androidIds = new ArrayList<>(dispositivos.keySet());
        if (!listaDispositivos.getItems().equals(androidIds)) {
            listaDispositivos.setItems(FXCollections.observableArrayList(androidIds));
        }

        // Celda simple — mostramos directamente el android_id
        listaDispositivos.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String androidId, boolean empty) {
                super.updateItem(androidId, empty);
                if (empty || androidId == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(androidId);
                    setCursor(javafx.scene.Cursor.HAND);
                    setStyle("-fx-font-family: 'Poppins'; -fx-padding: 8 12;");
                }
            }
        });
    }

    // ───────────────────── REFRESCAR ─────────────────────
    @FXML
    private void onRefrescar() {
        try {
            Map<String, String> dispositivos = adbService.obtenerDispositivosConectados();
            actualizarLista(dispositivos);
        } catch (IOException e) {
            lblEstadoAdb.setText("● Error al ejecutar ADB");
            e.printStackTrace();
        }
    }

    // ───────────────────── SELECCIONAR ─────────────────────
    
@FXML
private void onSeleccionarDispositivo() {
    String androidId = listaDispositivos.getSelectionModel().getSelectedItem();
    if (androidId == null) return;
    String serial = mapaAndroidIdSerial.get(androidId);
    if (serial == null) return;

    new Thread(() -> {
        try {
            Dispositivo dispositivo = dispositivoDAO.buscarPorAndroidId(androidId);
            if (dispositivo == null) {
                Dispositivo desdeAdb = adbService.obtenerProps(serial);
                Platform.runLater(() -> {
                    try {
                        cargarFormularioAlta(desdeAdb);
                    } catch (IOException e) {
                        lblEstadoAdb.setText("● Error al cargar formulario");
                        e.printStackTrace();
                    }
                });
            } else {
                Platform.runLater(() -> {
                    try {
                        cargarPanel("/fxml/vista_diagnostico.fxml", dispositivo);
                    } catch (IOException e) {
                        lblEstadoAdb.setText("● Error al cargar panel");
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException | SQLException e) {
            Platform.runLater(() -> lblEstadoAdb.setText("● Error al cargar dispositivo"));
            e.printStackTrace();
        }
    }).start();
}

    // ───────────────────── NAVEGACIÓN ─────────────────────
    private void cargarFormularioAlta(Dispositivo desdeAdb) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/formulario_alta.fxml"));
        Node panel = loader.load();

        FormularioAltaController controller = loader.getController();
        controller.setDispositivo(desdeAdb);
        controller.setRootPane(rootPane);
        controller.setOnGuardadoExitoso(dispositivoGuardado -> {
            try {
                cargarPanel("/fxml/ficha_tecnica.fxml", dispositivoGuardado);
            } catch (IOException e) {
                lblEstadoAdb.setText("● Error al cargar la ficha técnica");
                e.printStackTrace();
            }
        });

        panelCentral.getChildren().setAll(panel);
    }

    private void cargarPanel(String fxmlPath, Dispositivo dispositivo) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Node panel = loader.load();

        DispositivoAware controller = loader.getController();
        controller.setDispositivo(dispositivo);

        panelCentral.getChildren().setAll(panel);
    }

    // ───────────────────── STOP ─────────────────────
    public void detener() {
        if (scheduler != null) scheduler.shutdown();
    }
}