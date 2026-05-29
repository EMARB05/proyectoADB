package com.example.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    private static final int ADB_WIFI_PORT = 5555;
    private static final int TCP_POLL_TIMEOUT_MS = 1000; // timeout por intento de socket
    private static final int TCP_POLL_INTERVAL_MS = 500; // pausa entre intentos
    private static final int TCP_POLL_MAX_MS = 15_000; // espera total máxima
    private static final int ADB_CONNECT_RETRIES = 3;
    private static final int ADB_CONNECT_RETRY_MS = 1500;

    @FXML
    private Label lblEstado;
    @FXML
    private Label lblPunto;

    private String serial;
    private String deviceIp = "";

    private final ADBService adbService = new ADBService();
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    private enum AdbMode {
        USB, WIFI
    }

    private AdbMode adbMode = AdbMode.USB;

    // ─────────────────────────────────────────────
    // SERIAL
    // ─────────────────────────────────────────────

    public void setSerial(String serial) {
        if (serial != null && !serial.isBlank()) {
            this.serial = serial;
            System.out.println("[ADB] Serial asignado: " + serial);
        }
        actualizarEstadoInicial();
    }

    // ─────────────────────────────────────────────
    // ESTADO INICIAL
    // ─────────────────────────────────────────────

    private void actualizarEstadoInicial() {
        final String dev = this.serial;
        taskExecutor.execute(() -> {
            String salida = adbService.ejecutarComandoSincrono(dev, "shell ip addr show");
            if (salida == null)
                return;

            boolean tieneWifi = salida.contains("wlan0") && salida.contains("inet ");
            boolean tieneTethering = (salida.contains("rndis0") || salida.contains("usb0"))
                    && salida.contains("inet ");

            Platform.runLater(() -> {
                if (tieneTethering)
                    setEstado("Conexión: USB Tethering", "#89b4fa");
                else if (tieneWifi)
                    setEstado("Conexión: WiFi activa", "#a6e3a1");
                else
                    setEstado("Conexión: Cable ADB", "#6c7086");
            });
        });
    }

    // ─────────────────────────────────────────────
    // CONECTAR WIFI
    // ─────────────────────────────────────────────

    @FXML
    private void conectarAdbWifi() {
        final String dev = this.serial;
        System.out.println("[ADB] Serial en uso para tcpip: '" + dev + "'");
        System.out.println(
                "[ADB] Tipo: " + (dev != null && dev.contains(".") ? "IP (INCORRECTO)" : "Serial USB (correcto)"));
        System.out.println("[ADB] Iniciando conexión WiFi para: " + dev);
        Platform.runLater(() -> setEstado("Detectando IP...", "#f9e2af"));

        taskExecutor.execute(() -> {
            try {
                // 1. Detectar IP (wlan0 primero, luego fallback a ip addr show)
                String ip = detectarIp(dev);
                if (ip == null || ip.isBlank()) {
                    Platform.runLater(() -> setEstado("Error: no se detectó IP WiFi", "#f38ba8"));
                    return;
                }
                this.deviceIp = ip;
                System.out.println("[ADB] IP detectada: " + ip);

                // 2. Activar modo TCP/IP
                Platform.runLater(() -> setEstado("Activando tcpip 5555...", "#f9e2af"));
                String tcpRes = ejecutarComandoAdb("adb", "-s", dev, "tcpip",
                        String.valueOf(ADB_WIFI_PORT));
                System.out.println("[ADB] tcpip → " + tcpRes);

                // 3. Espera activa: poll TCP hasta que el puerto responda
                Platform.runLater(() -> setEstado("Esperando adbd en puerto 5555...", "#f9e2af"));
                boolean puertoAbierto = esperarPuertoTcp(ip, ADB_WIFI_PORT,
                        TCP_POLL_MAX_MS, TCP_POLL_INTERVAL_MS);
                if (!puertoAbierto) {
                    Platform.runLater(() -> setEstado(
                            "Error: dispositivo no responde en " + ip + ":5555 — "
                                    + "¿WiFi activa y misma red?",
                            "#f38ba8"));
                    return;
                }
                System.out.println("[ADB] Puerto 5555 accesible en " + ip);

                // 4. Conectar (pocos reintentos, el poll ya garantizó disponibilidad)
                String endpoint = ip + ":" + ADB_WIFI_PORT;
                String connectRes = "";

                for (int i = 1; i <= ADB_CONNECT_RETRIES; i++) {
                    System.out.println("[ADB] Intento " + i + " → adb connect " + endpoint);
                    connectRes = ejecutarComandoAdb("adb", "connect", endpoint);
                    System.out.println("[ADB] Resultado: " + connectRes);

                    if (resultadoValido(connectRes.toLowerCase())) {
                        // 5. Verificar que aparece en adb devices
                        if (verificarDispositivoConectado(endpoint)) {
                            adbMode = AdbMode.WIFI;
                            final String ipFinal = ip;
                            Platform.runLater(() -> setEstado("WiFi activo — " + ipFinal, "#a6e3a1"));
                        } else {
                            Platform.runLater(() -> setEstado("Conectado pero no aparece en devices", "#f9e2af"));
                        }
                        return;
                    }
                    Thread.sleep(ADB_CONNECT_RETRY_MS);
                }

                final String err = connectRes;
                Platform.runLater(() -> setEstado("Error: " + err, "#f38ba8"));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> setEstado("Conexión interrumpida", "#f38ba8"));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setEstado("Error: " + e.getMessage(), "#f38ba8"));
            }
        });
    }

    // ─────────────────────────────────────────────
    // DESCONECTAR WIFI
    // ─────────────────────────────────────────────

    @FXML
    private void conectarAdbUsb() {
        final String ip = this.deviceIp;
        Platform.runLater(() -> setEstado("Desconectando WiFi...", "#f9e2af"));

        taskExecutor.execute(() -> {
            try {
                if (ip != null && !ip.isBlank()) {
                    String res = ejecutarComandoAdb("adb", "disconnect", ip + ":" + ADB_WIFI_PORT);
                    System.out.println("[ADB] Disconnect: " + res);
                }
                adbMode = AdbMode.USB;
                Platform.runLater(() -> setEstado("USB activo", "#89b4fa"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ─────────────────────────────────────────────
    // DETECTAR IP (ip route → fallback ip addr show)
    // ─────────────────────────────────────────────

    private String detectarIp(String dev) {

        // Intento 1: ip route src
        String route = ejecutarComandoAdb("adb", "-s", dev, "shell", "ip", "route");
        for (String line : route.split("\n")) {
            if (line.contains("wlan0") && line.contains("src")) {
                Matcher m = Pattern.compile("src\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
                if (m.find())
                    return m.group(1);
            }
        }

        // Intento 2: ip addr show wlan0
        String addr = ejecutarComandoAdb("adb", "-s", dev, "shell", "ip", "addr", "show", "wlan0");
        for (String line : addr.split("\n")) {
            if (line.trim().startsWith("inet ")) {
                Matcher m = Pattern.compile("inet\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
                if (m.find())
                    return m.group(1);
            }
        }

        System.out.println("[ADB] No se encontró IP wlan0. Salida ip addr:\n" + addr);
        return "";
    }

    // ─────────────────────────────────────────────
    // POLL TCP — espera activa hasta que el puerto responde
    // ─────────────────────────────────────────────

    /**
     * Devuelve true en cuanto el puerto TCP esté accesible,
     * false si se supera maxWaitMs sin éxito.
     */
    private boolean esperarPuertoTcp(String host, int port,
            int maxWaitMs, int intervalMs)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + maxWaitMs;

        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), TCP_POLL_TIMEOUT_MS);
                return true; // ¡puerto accesible!
            } catch (Exception ignored) {
                // todavía no responde
            }
            Thread.sleep(intervalMs);
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // VERIFICAR DISPOSITIVO EN ADB DEVICES
    // ─────────────────────────────────────────────

    private boolean verificarDispositivoConectado(String endpoint) {
        String devices = ejecutarComandoAdb("adb", "devices");
        System.out.println("\n========= ADB DEVICES =========\n" + devices
                + "\n================================\n");
        return devices.contains(endpoint);
    }

    // ─────────────────────────────────────────────
    // EJECUTAR COMANDO ADB
    // ─────────────────────────────────────────────

    private String ejecutarComandoAdb(String... args) {
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line).append("\n");
            }

            boolean done = p.waitFor(12, TimeUnit.SECONDS);
            if (!done) {
                System.out.println("[ADB] Timeout en: " + String.join(" ", args));
                p.destroyForcibly();
            }
            return sb.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (p != null && p.isAlive())
                p.destroy();
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private boolean resultadoValido(String res) {
        return res.contains("connected") || res.contains("already connected");
    }

    private void setEstado(String texto, String color) {
        lblEstado.setText(texto);
        lblPunto.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
    }
}