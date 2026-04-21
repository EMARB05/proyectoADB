package com.example.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.Controller.ADBService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdbWifiController {

    @FXML private Label lblEstado;
    @FXML private Label lblPunto;

    private String serial;
    private ADBService adbService = new ADBService();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private enum AdbMode { USB, WIFI }
    private AdbMode adbMode = AdbMode.USB;
    private String deviceIp = "";

    public void setSerial(String serial) {
        this.serial = serial;
        // Detecta el modo actual al abrir
        actualizarEstadoInicial();
    }

    private void actualizarEstadoInicial() {
        new Thread(() -> {
            String salida = adbService.ejecutarComandoSincrono(serial, "shell ip addr show");
            if (salida == null) return;

            boolean tieneWifi = salida.contains("wlan0") && salida.contains("inet ");
            boolean tieneTethering = (salida.contains("rndis0") || salida.contains("usb0"))
                    && salida.contains("inet ");

            Platform.runLater(() -> {
                if (tieneTethering) {
                    setEstado("Conexión: USB Tethering", "#89b4fa");
                } else if (tieneWifi) {
                    setEstado("Conexión: WiFi activa", "#a6e3a1");
                } else {
                    setEstado("Conexión: Cable ADB", "#6c7086");
                }
            });
        }).start();
    }

    private void setEstado(String texto, String color) {
        lblEstado.setText(texto);
        lblPunto.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
    }

    // ───────────────────── CONECTAR WIFI ─────────────────────
    @FXML
    private void conectarAdbWifi() {
        System.out.println("[ADB] Iniciando conexión WiFi...");
        Platform.runLater(() -> setEstado("Conectando por WiFi...", "#f9e2af"));

        taskExecutor.execute(() -> {
            try {
                System.out.println("[ADB] Activando modo TCP en puerto 5555...");
                String tcpResult = ejecutarComandoAdb(new String[]{"adb", "-s", serial, "tcpip", "5555"});
                System.out.println("[ADB] tcpip resultado: " + tcpResult);
                Thread.sleep(2000);

                detectarIpDispositivo();
                System.out.println("[ADB] IP detectada: " + deviceIp);

                String connectResult = ejecutarComandoAdb(
                        new String[]{"adb", "connect", deviceIp + ":5555"});
                System.out.println("[ADB] Resultado conexión: " + connectResult);

                if (connectResult.contains("connected")) {
                    adbMode = AdbMode.WIFI;
                    System.out.println("[ADB] ✔ Conexión WiFi establecida con " + deviceIp);
                    Platform.runLater(() -> setEstado("WiFi activo — " + deviceIp, "#a6e3a1"));
                } else {
                    System.out.println("[ADB] ✖ Falló: " + connectResult);
                    Platform.runLater(() -> setEstado("Error: " + connectResult, "#f38ba8"));
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[ADB] ✖ Conexión interrumpida");
            } catch (Exception e) {
                System.out.println("[ADB] ✖ Excepción: " + e.getMessage());
                Platform.runLater(() -> setEstado("Error: " + e.getMessage(), "#f38ba8"));
            }
        });
    }

    // ───────────────────── CONECTAR USB ─────────────────────
    @FXML
    private void conectarAdbUsb() {
        System.out.println("[ADB] Volviendo a modo USB...");
        Platform.runLater(() -> setEstado("Desconectando WiFi...", "#f9e2af"));

        taskExecutor.execute(() -> {
            // Detecta IP actual antes de desconectar por si cambió
            detectarIpDispositivo();

            String result = ejecutarComandoAdb(
                    new String[]{"adb", "disconnect", deviceIp + ":5555"});
            System.out.println("[ADB] Disconnect resultado: " + result);
            adbMode = AdbMode.USB;
            System.out.println("[ADB] ✔ Modo USB activado");
            Platform.runLater(() -> setEstado("USB activo", "#89b4fa"));
        });
    }

    // ───────────────────── HELPERS ─────────────────────
    private void detectarIpDispositivo() {
        String out = ejecutarComandoAdb(new String[]{"adb", "-s", serial, "shell", "ip", "route"});
        System.out.println("[ADB] ip route output: " + out);

        for (String line : out.split("\n")) {
            if (line.contains("wlan0") && line.contains("src")) {
                Matcher m = Pattern.compile("src\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
                if (m.find()) {
                    deviceIp = m.group(1);
                    System.out.println("[ADB] IP wlan0 detectada: " + deviceIp);
                    return;
                }
            }
        }
        System.out.println("[ADB] No se pudo detectar IP wlan0");
    }

    private String ejecutarComandoAdb(String[] args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor(10, TimeUnit.SECONDS);
            return sb.toString().trim();

        } catch (Exception e) {
            System.out.println("[ADB] Error ejecutando comando: " + e.getMessage());
            return "";
        }
    }
}