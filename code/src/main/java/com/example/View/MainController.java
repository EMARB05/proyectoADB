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
import com.example.Model.Banda;
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
import javafx.scene.layout.VBox;
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
    private final Map<String, String> mapaAndroidIdSerial = new LinkedHashMap<>();

    @FXML
    private VBox dropZone;
    @FXML
    private ListView<String> listaApks;
    
    private volatile boolean consultaAdbEnCurso = false;

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
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r,"adb-poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            if(consultaAdbEnCurso) return;
            consultaAdbEnCurso = true;
            try {
                // ADB en hilo secundario ✔
                Map<String, String> dispositivos = adbService.obtenerDispositivosConectados();
                // UI en hilo FX ✔
                Platform.runLater(() -> actualizarLista(dispositivos));
            } catch (IOException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al ejecutar ADB"));
            }finally{
                consultaAdbEnCurso = false;
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    // ───────────────────── LISTA ─────────────────────
    private void actualizarLista(Map<String, String> dispositivos) {
        if (lblEstadoAdb == null)
            return;
        if (dispositivos.isEmpty()) {
            lblEstadoAdb.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● Ningún dispositivo conectado");
        } else {
            lblEstadoAdb.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px;");
            lblEstadoAdb.setText("● " + dispositivos.size() + " dispositivo(s) conectado(s)");
        }

        mapaAndroidIdSerial.clear();
        mapaAndroidIdSerial.putAll(dispositivos);

        List<String> androidIds = new ArrayList<>(dispositivos.keySet());
        if (!listaDispositivos.getItems().equals(androidIds)) {
            listaDispositivos.setItems(FXCollections.observableArrayList(androidIds));
        }

        listaDispositivos.setCellFactory(lv -> new ListCell<>() {
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
        new Thread(() -> {
            try {
                Map<String, String> dispositivos = adbService.obtenerDispositivosConectados();
                Platform.runLater(() -> {
                    actualizarLista(dispositivos);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblEstadoAdb.setText("● Error al ejecutar ADB");
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── SELECCIONAR ─────────────────────
    @FXML
    private void onSeleccionarDispositivo() {
        String androidId = listaDispositivos.getSelectionModel().getSelectedItem();
        if (androidId == null) return;
        String serial = mapaAndroidIdSerial.get(androidId);
        if (serial == null) return;

        lblEstadoAdb.setText("● Cargando dispositivo...");

        // BD y ADB en hilo secundario ✔
        new Thread(() -> {
            try {
                Dispositivo dispositivo = dispositivoDAO.buscarPorAndroidId(androidId);

                if (dispositivo == null) {
                    // ADB pesado en hilo secundario ✔
                    Dispositivo desdeAdb = adbService.obtenerProps(serial);
                    List<Banda> bandas = adbService.obtenerBandas(serial);
                    desdeAdb.setBandasTemporales(bandas);

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/formulario_alta.fxml"));
                    Node panel = loader.load();
                    FormularioAltaController controller = loader.getController();

                    Platform.runLater(() -> {
                        cargarFormularioAlta(desdeAdb, controller, panel);
                    });

                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vista_diagnostico.fxml"));
                    Node panel = loader.load();
                    DispositivoAware controller = loader.getController();

                    Platform.runLater(() -> {
                        controller.setDispositivo(dispositivo);
                        panelCentral.getChildren().setAll(panel);
                    });
                }

            } catch (IOException | SQLException e) {
                Platform.runLater(() ->
                        lblEstadoAdb.setText("● Error al cargar dispositivo"));
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── NAVEGACIÓN ─────────────────────
    private void cargarFormularioAlta(Dispositivo desdeAdb, FormularioAltaController controller, Node panel) {
        controller.setDispositivo(desdeAdb);
        controller.setRootPane(rootPane);
        controller.setOnGuardadoExitoso(dispositivoGuardado -> {
            new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vista_diagnostico.fxml"));
                    Node pnl = loader.load();
                    DispositivoAware ctrl = loader.getController();

                    Platform.runLater(() -> {
                        ctrl.setDispositivo(dispositivoGuardado);
                        panelCentral.getChildren().setAll(pnl);
                    });
                } catch (IOException e) {
                    lblEstadoAdb.setText("● Error al cargar la ficha técnica");
                    e.printStackTrace();
                }
            }).start();
        });

        panelCentral.getChildren().setAll(panel);
    }

    // ───────────────────── STOP ─────────────────────
    public void detener() {
        if (scheduler != null && !scheduler.isShutdown())
            scheduler.shutdownNow();
    }

    @FXML
    private void onMostrarMasivo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vista_masiva.fxml"));
            // Importante: No uses Parent si vas a meterlo en un StackPane que ya existe
            Node vistaMasiva = loader.load();

            // OPCIÓN CORRECTA: Cambiar solo el contenido del panelCentral
            // Esto mantiene la lista de dispositivos y el label de estado visibles
            panelCentral.getChildren().setAll(vistaMasiva);

        } catch (IOException e) {
            e.printStackTrace();
            lblEstadoAdb.setText("● Error al cargar vista masiva");
        }
    }

}