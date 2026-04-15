package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class AjustesRapidosController {

    private String serial; // Se debe asignar al abrir la ventana
    private ADBService adbService = new ADBService();
    private ScrcpyService scrcpyService = new ScrcpyService();

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
    }

    // Se llama desde AjustesController.setSerial() una vez que el serial está
    // disponible
    public void setSerial(String serial) {
        this.serial = serial;
        sincronizarEstadoInicial();
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
                    btnBluetooth.setSelected("1".equals(btStatus.trim()));
                    btnModoAvion.setSelected("1".equals(avionStatus.trim()));
                    btnGPS.setSelected("3".equals(gpsStatus.trim()));
                    actualizarBoton(btnBluetooth.isSelected(), btnBluetooth);
                    actualizarBoton(btnModoAvion.isSelected(), btnModoAvion);
                    actualizarBoton(btnGPS.isSelected(), btnGPS);

                    // Sincronizar Sliders
                    try {
                        if (!brilloStatus.isEmpty() && !"null".equals(brilloStatus)) {
                            sliderBrillo.setValue(Double.parseDouble(brilloStatus.trim()));
                        }
                        sliderVolumen.setValue(Double.parseDouble(volStatus));
                        ultimoValorEntero = Double.parseDouble(volStatus); // Importante para que no salte el listener
                    } catch (NumberFormatException e) {
                        System.err.println("Error al parsear valores numéricos");
                    }
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

    // 2. Crea el método que el FXML está buscando
    @FXML
    private void onLanzarScrcpy(ActionEvent event) {
        if (serial != null && !serial.isEmpty()) {
            System.out.println("Lanzando Scrcpy para: " + serial);
            scrcpyService.launch(serial);
        } else {
            System.err.println("No hay serial seleccionado para lanzar Scrcpy");
        }
    }

    @FXML
    private void onVolver(ActionEvent event) {
        // Esto cierra la ventana actual de ajustes
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}

// @FXML
// private void onLanzarScrcpy() {
// String serial = lblSerial.getText().replace("Serial: ", "").trim();
// if (!serial.isEmpty() && !serial.equals("—")) {
// scrcpyService.launch(serial);
// }
// }

// @FXML
// private void onToggleModoAvion() {
// String serialLimpio = lblSerial.getText().replace("Serial: ", "").trim();
// if (serialLimpio.isEmpty() || serialLimpio.equals("—"))
// return;

// try {
// // 1. Consultamos el estado actual (este método es sincrónico, está bien así)
// boolean actualmenteActivo = adbService.isModoAvionActivo(serialLimpio);
// boolean nuevoEstado = !actualmenteActivo;

// // 2. Ejecutamos la acción usando el nuevo método de hilos del Service
// String valor = nuevoEstado ? "1" : "0";
// String broadcastValor = nuevoEstado ? "true" : "false";

// // Cambiamos el ajuste
// adbService.ejecutarAccionHilo(serialLimpio, "shell settings put global
// airplane_mode_on " + valor);

// // Enviamos el broadcast para refrescar la interfaz del móvil
// adbService.ejecutarAccionHilo(serialLimpio,
// "shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " +
// broadcastValor);

// // 3. Feedback visual en tu botón
// actualizarBotonAvion(nuevoEstado);

// } catch (IOException e) {
// System.err.println("Error al detectar estado del modo avión: " +
// e.getMessage());
// }
// }

// // Método auxiliar para no repetir código de estilos
// private void actualizarBotonAvion(boolean activo) {
// if (activo) {
// btnModoAvion.setStyle(
// "-fx-background-color: #fab387; -fx-text-fill: #1e1e2e; -fx-font-weight:
// bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 18 10
// 18;");
// btnModoAvion.setText("✈ Modo Avión: ON");
// } else {
// btnModoAvion.setStyle(
// "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight:
// bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 10 18 10
// 18;");
// btnModoAvion.setText("✈ Modo Avión: OFF");
// }
// }