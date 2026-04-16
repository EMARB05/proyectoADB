package com.example.View;

import java.io.IOException;
import com.example.Controller.ADBService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class AdbWifiController {
    @FXML
    private ToggleButton switchConexion;
    @FXML
    private Label lblEstado;

    private String serial;
    private ADBService adbService = new ADBService();
    private ChangeListener<Boolean> toggleListener;
    private volatile boolean monitoreando = true;

    private static final String ESTILO_USB = """
            -fx-background-color: #89b4fa;
            -fx-border-color: #74c7ec;
            -fx-border-radius: 20;
            -fx-background-radius: 20;
            -fx-border-width: 2;
            -fx-cursor: hand;
            -fx-text-fill: #1e1e2e;
            -fx-font-size: 18px;
            -fx-padding: 0 5 0 5;
            -fx-alignment: CENTER_RIGHT;
            """;

    private static final String ESTILO_WIFI = """
            -fx-background-color: #313244;
            -fx-border-color: #45475a;
            -fx-border-radius: 20;
            -fx-background-radius: 20;
            -fx-border-width: 2;
            -fx-cursor: hand;
            -fx-text-fill: #89b4fa;
            -fx-font-size: 18px;
            -fx-padding: 0 5 0 5;
            -fx-alignment: CENTER_LEFT;
            """;

    public void setSerial(String serial) {
        this.serial = serial;
        configurarListener();
        actualizarInterfazSegunRed(); // Sincronización inicial
        iniciarMonitoreoRed();
    }

    private void configurarListener() {
        toggleListener = (obs, wasSelected, isSelected) -> {
            if (isSelected) {
                lblEstado.setText("Conectando por WiFi...");
                new Thread(() -> {
                    try {
                        activarYConectarWifi(serial);
                    } catch (IOException e) {
                        Platform.runLater(() -> lblEstado.setText("Error: " + e.getMessage()));
                    }
                }).start();
            } else {
                lblEstado.setText("Abriendo ajustes de Tethering...");
                abrirMenuTethering(serial);
            }
        };
        switchConexion.selectedProperty().addListener(toggleListener);
    }

    private void iniciarMonitoreoRed() {
        Thread monitorThread = new Thread(() -> {
            while (monitoreando) {
                try {
                    Thread.sleep(2000); 
                    actualizarInterfazSegunRed();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void actualizarInterfazSegunRed() {
        String salidaInterfaces = adbService.ejecutarComandoSincrono(serial, "shell ip addr show");
        if (salidaInterfaces == null) return;

        // DETECCION CON JERARQUÍA
        boolean tieneTethering = (salidaInterfaces.contains("rndis0") || salidaInterfaces.contains("usb0")) 
                                 && salidaInterfaces.contains("inet ");
        
        boolean tieneWifi = salidaInterfaces.contains("wlan0") && salidaInterfaces.contains("inet ");

        Platform.runLater(() -> {
            // Evitamos disparar el listener al actualizar el botón
            switchConexion.selectedProperty().removeListener(toggleListener);

            if (tieneTethering) {
                // PRIORIDAD 1: USB Tethering activo (aunque haya WiFi)
                switchConexion.setStyle(ESTILO_USB);
                switchConexion.setSelected(false);
                lblEstado.setText("Conexión: USB Tethering");
            } else if (tieneWifi) {
                // PRIORIDAD 2: Solo WiFi activo
                switchConexion.setStyle(ESTILO_WIFI);
                switchConexion.setSelected(true);
                lblEstado.setText("Conexión: WiFi");
            } else {
                // PRIORIDAD 3: Ninguno (solo cable ADB)
                switchConexion.setStyle(ESTILO_USB);
                switchConexion.setSelected(false);
                lblEstado.setText("Conexión: Cable ADB (Carga)");
            }

            switchConexion.selectedProperty().addListener(toggleListener);
        });
    }

    private void abrirMenuTethering(String serial) {
        adbService.ejecutarComandoSincrono(serial, "shell am start -n com.android.settings/.TetherSettings");
    }

    private void activarYConectarWifi(String serial) throws IOException {
        adbService.ejecutarAccionHilo(serial, "tcpip 5555");
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        String ip = obtenerIP(serial);
        if (ip == null) throw new IOException("No se pudo obtener la IP");
        adbService.ejecutarComandoDirecto("connect", ip + ":5555");
    }

    private String obtenerIP(String serial) {
        String salida = adbService.ejecutarComandoSincrono(serial, "shell ip addr show wlan0");
        if (salida == null) return null;
        for (String linea : salida.split("\n")) {
            if (linea.trim().startsWith("inet ")) {
                return linea.trim().split("\\s+")[1].split("/")[0];
            }
        }
        return null;
    }
}