package com.example.View;

import java.io.IOException;
import java.util.List;

import com.example.Controller.ADBService;

import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;

public class AjustesRapidosController {

    private String serial; // Se debe asignar al abrir la ventana
    private ADBService adbService = new ADBService();

    @FXML
    private Slider sliderBrillo;
    @FXML
    private Slider sliderVolumen;
    private double ultimoValorEntero = 0;

    @FXML
    private ToggleButton btnModoAvion;
    @FXML
    private ToggleButton btnBluetooth;
    @FXML
    private ToggleButton btnGPS;
    @FXML
    private TextField txtBuscador;
    @FXML
    private ListView<String> listaPaquetes;
    @FXML
    private Button btnListar;

    @FXML
    private StackPane rootPane;

    private ObservableList<String> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // --- BRILLO (Rango 0-255) ---
        sliderBrillo.setMin(0);
        sliderBrillo.setMax(255);
        sliderBrillo.valueProperty().addListener((obs, oldVal, newVal) -> {
            int valor = newVal.intValue();
            adbService.ejecutarAccionHilo(serial, "shell settings put system screen_brightness " + valor);
        });

        // --- VOLUMEN (Rango 0-15) ---
        sliderVolumen.setMin(0);
        sliderVolumen.setMax(15);
        sliderVolumen.setBlockIncrement(1);

        sliderVolumen.valueProperty().addListener((obs, oldVal, newVal) -> {
            int actual = newVal.intValue();

            // Solo actuamos si el valor ha cambiado de unidad (evita ráfagas)
            if (actual != ultimoValorEntero) {
                if (actual > ultimoValorEntero) {
                    // El usuario subió el slider -> Mandamos tecla de subir
                    adbService.ejecutarAccionHilo(serial, "shell input keyevent 24");
                } else {
                    // El usuario bajó el slider -> Mandamos tecla de bajar
                    adbService.ejecutarAccionHilo(serial, "shell input keyevent 25");
                }
                ultimoValorEntero = actual;
            }
        });

        listaPaquetes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String seleccionado = listaPaquetes.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    abrirSelectorYDescargar(seleccionado);
                }
            }
        });
    }

    public void setSerial(String androidId) {
        // Resuelve el serial activo (IP si está por WiFi, serial si está por USB)
        new Thread(() -> {
            try {
                String serialActivo = adbService.getSerialActivo(androidId);
                this.serial = serialActivo;
                System.out.println("[AJUSTES] Serial activo resuelto: " + serial);
                Platform.runLater(() -> {
                    sincronizarEstadoInicial();
                    adbService.ejecutarAccionHilo(serial, "shell input keyevent KEYCODE_WAKEUP");
                });
            } catch (IOException e) {
                // Fallback: usa lo que llega si falla la resolución
                this.serial = androidId;

                System.out.println("[AJUSTES] Fallback serial: " + serial);
                Platform.runLater(() -> sincronizarEstadoInicial());
            }
        }).start();
    }

    private void sincronizarEstadoInicial() {
        // Ejecutar en un hilo separado para no congelar la UI
        new Thread(() -> {
            try {
                // Obtenemos el valor inicial
                String btStatus = adbService.ejecutarComandoSincrono(serial, "shell settings get global bluetooth_on");
                String avionStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell settings get global airplane_mode_on");
                String gpsStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell settings get secure location_mode");
                String brilloStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell settings get system screen_brightness");
                // Este comando devuelve mucho texto, necesitamos extraer el número tras
                // "index:"
                String volStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell settings get system volume_music_speaker");

                Platform.runLater(() -> {
                    try {
                        if (!brilloStatus.isEmpty() && !"null".equals(brilloStatus.trim())) {
                            sliderBrillo.setValue(Double.parseDouble(brilloStatus.trim()));
                        }
                        double vol = Double.parseDouble(volStatus.trim());
                        ultimoValorEntero = vol; // ← primero ultimoValorEntero
                        sliderVolumen.setValue(vol); // ← luego setValue (listener lee ultimoValorEntero)
                    } catch (NumberFormatException e) {
                        System.err.println("Error al parsear valores numéricos");
                    }
                    btnBluetooth.setSelected("1".equals(btStatus.trim()));
                    btnModoAvion.setSelected("1".equals(avionStatus.trim()));
                    btnGPS.setSelected("3".equals(gpsStatus.trim()));
                    actualizarBoton(btnBluetooth.isSelected(), btnBluetooth);
                    actualizarBoton(btnModoAvion.isSelected(), btnModoAvion);
                    actualizarBoton(btnGPS.isSelected(), btnGPS);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void toggleBluetooth(ActionEvent event) {
        actualizarBoton(btnBluetooth.isSelected(), btnBluetooth);
        String estado = btnBluetooth.isSelected() ? "enable" : "disable";
        adbService.ejecutarAccionHilo(serial, "shell cmd bluetooth_manager " + estado);
    }

    @FXML
    private void toggleAirplane(ActionEvent event) {
        actualizarBoton(btnModoAvion.isSelected(), btnModoAvion);
        int valor = btnModoAvion.isSelected() ? 1 : 0;
        adbService.ejecutarAccionHilo(serial, "shell settings put global airplane_mode_on " + valor);
        // Comando necesario para refrescar el estado en el móvil
        adbService.ejecutarAccionHilo(serial, "shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state "
                + (btnModoAvion.isSelected() ? "true" : "false"));
    }

    @FXML
    private void toggleGPS(ActionEvent event) {
        actualizarBoton(btnGPS.isSelected(), btnGPS);
        // En Android moderno se usa 'location'
        adbService.ejecutarAccionHilo(serial,
                "shell settings put secure location_mode " + (btnGPS.isSelected() ? "3" : "0"));
    }

    // Método auxiliar para cambiar el estilo de los botones
    private void actualizarBoton(boolean activo, ToggleButton btn) {
        if (activo) {
            btn.setStyle(
                    "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 18 10 18;");
        } else {
            btn.setStyle(
                    "-fx-background-color: #313244; -fx-text-fill: #CDD5F3; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 18 10 18;");
        }
    }

    @FXML
    private void handleListarPaquetes() {
        try {
            List<String> paquetes = adbService.listarPaquetes(serial);
            masterData.setAll(paquetes);

            // Filtrado
            FilteredList<String> filteredData = new FilteredList<>(masterData, p -> true);

            txtBuscador.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(paquete -> {
                    if (newValue == null || newValue.isEmpty())
                        return true;
                    return paquete.toLowerCase().contains(newValue.toLowerCase());
                });
            });

            listaPaquetes.setItems(filteredData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirSelectorYDescargar(String paquete) {
        // Selector de directorios
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Selecciona dónde guardar el APK de " + paquete);

        // Recogemos los datos de la ventana
        java.io.File carpetaDestino = selector.showDialog(listaPaquetes.getScene().getWindow());

        if (carpetaDestino != null) {
            try {
                String ruta = carpetaDestino.getAbsolutePath().replace("\\", "/");
                listaPaquetes.getScene().setCursor(Cursor.WAIT);
                adbService.descargarApk(serial, paquete, ruta);
                listaPaquetes.getScene().setCursor(Cursor.DEFAULT);
                Platform.runLater(() -> mostrarToast("✅ APK extraída: " + paquete));
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> mostrarToast("❌ Error: " + e.getMessage()));
            }
        }
    }

    /**
     * Muestra un Toast flotante en la parte inferior del rootPane.
     */
    private void mostrarToast(String mensaje) {
        if (rootPane == null)
            return;

        // Crear el Label con tu estilo Mocha
        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);

        // Posicionamiento en el StackPane
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));

        rootPane.getChildren().add(toast);

        // Secuencia de animación
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pausa = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));

        new SequentialTransition(fadeIn, pausa, fadeOut).play();
    }
}
