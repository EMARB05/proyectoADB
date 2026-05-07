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
import java.util.function.Consumer;

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

public class MainController implements NavegacionHandler {

    @FXML
    private Label lblEstadoAdb;
    @FXML
    private ListView<String> listaDispositivos;
    @FXML
    private StackPane panelCentral;
    @FXML
    private javafx.scene.layout.VBox panelLista;
    @FXML
    private StackPane rootPane;

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
            if (stage != null)
                setupResponsive(stage);
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
            Thread t = new Thread(r, "adb-poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            if (consultaAdbEnCurso)
                return;
            consultaAdbEnCurso = true;
            try {
                // ADB en hilo secundario ✔
                Map<String, String> dispositivos = adbService.obtenerDispositivosConectados();
                // UI en hilo FX ✔
                Platform.runLater(() -> actualizarLista(dispositivos));
            } catch (IOException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al ejecutar ADB"));
            } finally {
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
        if (androidId == null)
            return;
        String serial = mapaAndroidIdSerial.get(androidId);
        if (serial == null)
            return;

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

                    cambiarVistaCentral("/fxml/formulario_alta.fxml", desdeAdb, (dispositivoGuardado) -> {
                        cambiarVistaCentral("/fxml/vista_diagnostico.fxml", dispositivoGuardado, null);
                    });

                } else {
                    cambiarVistaCentral("/fxml/vista_diagnostico.fxml", dispositivo, null);
                }

            } catch (IOException | SQLException e) {
                Platform.runLater(() -> lblEstadoAdb.setText("● Error al cargar dispositivo"));
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── NAVEGACIÓN ─────────────────────
    // private void cargarFormularioAlta(Dispositivo desdeAdb,
    // FormularioAltaController controller, Node panel) {
    // controller.setDispositivo(desdeAdb);
    // controller.setRootPane(rootPane);
    // controller.setOnGuardadoExitoso(dispositivoGuardado -> {
    // new Thread(() -> {
    // try {
    // FXMLLoader loader = new
    // FXMLLoader(getClass().getResource("/fxml/vista_diagnostico.fxml"));
    // Node pnl = loader.load();
    // DispositivoAware ctrl = loader.getController();

    // Platform.runLater(() -> {
    // ctrl.setDispositivo(dispositivoGuardado);
    // panelCentral.getChildren().setAll(pnl);
    // });
    // } catch (IOException e) {
    // lblEstadoAdb.setText("● Error al cargar la ficha técnica");
    // e.printStackTrace();
    // }
    // }).start();
    // });

    // panelCentral.getChildren().setAll(panel);
    // }

    @Override
    public void cambiarVistaCentral(String fxmlPath, Dispositivo dispositivo, Consumer<Dispositivo> alFinalizar) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Node panel = loader.load();
                Object controller = loader.getController();

                if (controller instanceof DispositivoAware && dispositivo != null) {
                    ((DispositivoAware) controller).setDispositivo(dispositivo);
                }

                if (controller instanceof DiagnosticoController) {
                    ((DiagnosticoController) controller).setDispositivo(dispositivo);
                }
                // 3. ¡ESTA ES LA PARTE QUE TE FALTA!
                // Añade este bloque para el comparador de XML
               

                if (controller instanceof SelectorComparadorController) {
                    ((SelectorComparadorController) controller).setNavegacionHandler(this);
                }
                
                if (controller instanceof FormularioAltaController && alFinalizar != null) {
                    ((FormularioAltaController) controller).setOnGuardadoExitoso(alFinalizar);
                }
                panelCentral.getChildren().setAll(panel);

                if (!(controller instanceof FormularioAltaController) && alFinalizar != null) {
                    alFinalizar.accept(dispositivo);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void onActualizarAPNs() throws IOException {
        cambiarVistaCentral("/fxml/selector_comparador.fxml", new Dispositivo(), null);
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

    public void mostrarToast(String mensaje) {
        if (rootPane == null) {
            System.out.println("RootPane es null en main");
            return;
        }
        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24; -fx-background-radius: 24; -fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));
        rootPane.getChildren().add(toast);
        toast.toFront();
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400),
                toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));
        javafx.animation.PauseTransition delayEntrada = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(150));
        new javafx.animation.SequentialTransition(
                delayEntrada,
                fadeIn,
                pausa,
                fadeOut).play();
    }

}