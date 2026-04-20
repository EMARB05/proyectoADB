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

    // referencia al StackPane raíz de la escena para los Toasts
    @FXML
    private StackPane rootPane;

    private final ADBService adbService = new ADBService();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();

    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) panelLista.getScene().getWindow();
            if (stage != null) {
                setupResponsive(stage);
            }
        });

        iniciarDeteccionAutomatica();
    }

    public void setupResponsive(Stage stage) {
        Runnable checkState = () -> {
            boolean expandido = stage.isFullScreen() || stage.isMaximized();
            actualizarMargen(expandido);
        };

        stage.fullScreenProperty().addListener((obs, old, isNowFull) -> checkState.run());
        stage.maximizedProperty().addListener((obs, old, isNowMax) -> checkState.run());
        checkState.run();
    }

    private void actualizarMargen(boolean esPantallaCompleta) {
        if (esPantallaCompleta) {
            HBox.setMargin(panelLista, new Insets(0, 0, 0, 0));
            panelLista.setPadding(new Insets(30, 24, 30, 24));
            panelLista.setMinWidth(320);
            panelLista.setMaxWidth(320);
        } else {
            HBox.setMargin(panelLista, new Insets(0, 0, 0, 0));
            panelLista.setPadding(new Insets(16));
            panelLista.setMinWidth(240);
            panelLista.setMaxWidth(240);
        }
    }

    private void iniciarDeteccionAutomatica() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<String> seriales = adbService.obtenerDispositivosConectados();
                Platform.runLater(() -> actualizarLista(seriales));
            } catch (IOException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al ejecutar ADB"));
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

  private void actualizarLista(List<String> seriales) {
    // 1. Actualización del estado visual superior
    if (seriales.isEmpty()) {
        lblEstadoAdb.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");
        lblEstadoAdb.setText("● Ningún dispositivo conectado");
    } else {
        lblEstadoAdb.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px;");
        lblEstadoAdb.setText("● " + seriales.size() + " dispositivo(s) conectado(s)");
    }

    // 2. Solo actualizamos los items si la lista ha cambiado (evita parpadeos)
    if (!listaDispositivos.getItems().equals(seriales)) {
        listaDispositivos.setItems(FXCollections.observableArrayList(seriales));
    }

    // 3. Celda personalizada para traducir Serial -> Android ID
    listaDispositivos.setCellFactory(lv -> new ListCell<String>() {
        @Override
        protected void updateItem(String serial, boolean empty) {
            super.updateItem(serial, empty);
            
            if (empty || serial == null) {
                setText(null);
                setGraphic(null);
            } else {
                try {
                    // Intentamos buscar el dispositivo por serial para obtener su Android ID
                    Dispositivo d = dispositivoDAO.buscarPorSerial(serial);
                    
                    if (d != null && d.getAndroid_id() != null && !d.getAndroid_id().isEmpty()) {
                        // Si existe en la BD, mostramos el Android ID
                        setText(d.getAndroid_id());
                    } else {
                        // Si es nuevo o no tiene ID en BD, mostramos el serial como respaldo
                        setText(serial + " (Nuevo)");
                    }
                } catch (SQLException e) {
                    // Si falla la BD, mostramos el serial para no dejar la celda vacía
                    setText(serial);
                }

                // Estilo para asegurar que use Poppins y se vea limpio
                setCursor(javafx.scene.Cursor.HAND);
                setStyle("-fx-font-family: 'Poppins'; -fx-padding: 8 12;");
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
            e.printStackTrace();
        }
    }

  @FXML
private void onSeleccionarDispositivo() {
    String serial = listaDispositivos.getSelectionModel().getSelectedItem();
    if (serial == null) return;

    try {
        // Busca primero por serial
        Dispositivo dispositivo = dispositivoDAO.buscarPorSerial(serial);

        // Si no lo encuentra (puede ser IP en modo WiFi), obtiene props de ADB
        // y busca por android_id para no abrir el formulario si ya está registrado
        if (dispositivo == null) {
            Dispositivo desdeAdb = adbService.obtenerProps(serial);
            String androidId = desdeAdb.getAndroid_id();

            if (androidId != null && !androidId.isBlank()) {
                dispositivo = dispositivoDAO.buscarPorAndroidId(androidId);
            }

            if (dispositivo == null) {
                // Realmente es nuevo — abre el formulario de alta
                System.out.println("[MAIN] Dispositivo nuevo, abriendo formulario de alta");
                cargarFormularioAlta(desdeAdb);
            } else {
                // Ya existe pero conectado por WiFi — va directo a la ficha
                System.out.println("[MAIN] Dispositivo ya registrado (detectado por android_id), cargando ficha");
                cargarPanel("/fxml/ficha_tecnica.fxml", dispositivo);
            }
        } else {
            // Encontrado por serial directamente
            System.out.println("[MAIN] Dispositivo encontrado por serial, cargando ficha");
            cargarPanel("/fxml/ficha_tecnica.fxml", dispositivo);
        }

    } catch (IOException | SQLException e) {
        lblEstadoAdb.setText("● Error al cargar dispositivo");
        e.printStackTrace();
    }
}

    /**
     * Carga el formulario de alta e inyecta:
     * - el Dispositivo con los datos de ADB
     * - el rootPane para mostrar el Toast
     * - el callback que, al guardar, navega automáticamente a la ficha técnica
     */
    private void cargarFormularioAlta(Dispositivo desdeAdb) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/formulario_alta.fxml"));
        Node panel = loader.load();

        FormularioAltaController controller = loader.getController();
        controller.setDispositivo(desdeAdb);

        // Inyectamos el rootPane para que el Toast flote sobre toda la UI
        controller.setRootPane(rootPane);

        // Al guardar con éxito, navegamos a la ficha técnica del dispositivo guardado
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

    /** Carga cualquier panel genérico (DispositivoAware) en el centro. */
    private void cargarPanel(String fxmlPath, Dispositivo dispositivo) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Node panel = loader.load();

        DispositivoAware controller = loader.getController();
        controller.setDispositivo(dispositivo);

        panelCentral.getChildren().setAll(panel);
    }

    public void detener() {
        if (scheduler != null)
            scheduler.shutdown();
    }
}