package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class GestionPantallaController {

    @FXML
    private ComboBox<String> comboResolucion;

    private String serial;
    private final ADBService adbService = new ADBService();
    private final ScrcpyService scrcpyService = new ScrcpyService();

    private int rotacionActual = 0;

    public void setSerial(String serial) {
        this.serial = serial;
        sincronizarEstadoInicial();
    }

    /**
     * Obtiene el estado real del dispositivo al conectar o refrescar.
     */
    public void sincronizarEstadoInicial() {
        new Thread(() -> {
            try {
                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                String rotStatus = adbService.ejecutarComandoSincrono(serial, "shell settings get system user_rotation");

                Platform.runLater(() -> {
                    actualizarUIResolucion(resStatus);
                    try {
                        rotacionActual = Integer.parseInt(rotStatus.trim()) * 90;
                    } catch (NumberFormatException e) {
                        rotacionActual = 0;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void cambiarResolucion(ActionEvent event) {
        String seleccion = comboResolucion.getValue();
        if (seleccion == null || serial == null) return;

        String resolucion = seleccion.split(" ")[0];

        new Thread(() -> {
            try {
                // Secuencia limpia: Reset -> Nueva Res -> Nuevo DPI
                adbService.ejecutarComandoSincrono(serial, "shell wm size reset");
                adbService.ejecutarComandoSincrono(serial, "shell wm size " + resolucion);
                
                int nuevoDpi = calcularDpiProporcional(resolucion);
                adbService.ejecutarComandoSincrono(serial, "shell wm density " + nuevoDpi);

                // Esperamos un momento a que el sistema procese y refrescamos UI
                Thread.sleep(500);
                String nuevoEstado = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                Platform.runLater(() -> actualizarUIResolucion(nuevoEstado));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void resetearResolucion(ActionEvent event) {
        if (serial == null) return;

        new Thread(() -> {
            try {
                // Reset de ambos parámetros para evitar interfaces deformes
                adbService.ejecutarComandoSincrono(serial, "shell wm size reset");
                adbService.ejecutarComandoSincrono(serial, "shell wm density reset");

                Thread.sleep(600); // Tiempo para que el WindowManager respire

                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                Platform.runLater(() -> actualizarUIResolucion(resStatus));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Procesa el string de ADB "Physical size: ... Override size: ..." 
     * y selecciona el valor correcto en el ComboBox.
     */
    private void actualizarUIResolucion(String rawStatus) {
        try {
            // Buscamos la última línea que contenga la resolución activa
            String linea = rawStatus.trim().lines()
                    .reduce((first, second) -> second)
                    .orElse("");
            
            // Extraemos solo el formato 000x000
            String resActual = linea.replaceAll(".*: ", "").trim(); 
            
            comboResolucion.getItems().stream()
                    .filter(item -> item.contains(resActual))
                    .findFirst()
                    .ifPresent(match -> comboResolucion.setValue(match));
        } catch (Exception e) {
            System.err.println("Error al parsear resolución: " + rawStatus);
        }
    }

    private int calcularDpiProporcional(String resolucion) {
        try {
            int ancho = Integer.parseInt(resolucion.split("x")[0]);
            if (ancho <= 480) return 160;
            if (ancho <= 720) return 240;
            if (ancho <= 1080) return 420;
            return 560;
        } catch (Exception e) {
            return 480;
        }
    }

    @FXML
    private void rotarPantalla90(ActionEvent event) {
        new Thread(() -> {
            adbService.ejecutarComandoSincrono(serial, "shell settings put system accelerometer_rotation 0");
            rotacionActual = (rotacionActual + 90) % 360;
            adbService.ejecutarComandoSincrono(serial, "shell settings put system user_rotation " + (rotacionActual / 90));
        }).start();
    }

    @FXML
    private void flipPantalla(ActionEvent event) {
        new Thread(() -> {
            adbService.ejecutarComandoSincrono(serial, "shell settings put system accelerometer_rotation 0");
            int valorSistema = (rotacionActual / 90 + 2) % 4;
            rotacionActual = valorSistema * 90;
            adbService.ejecutarComandoSincrono(serial, "shell settings put system user_rotation " + valorSistema);
        }).start();
    }

    @FXML
    private void onLanzarScrcpy(ActionEvent event) {
        if (serial != null && !serial.isEmpty()) {
            scrcpyService.launch(serial);
        }
    }
}