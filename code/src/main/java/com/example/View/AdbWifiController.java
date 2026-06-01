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
    private static final int TCP_POLL_TIMEOUT_MS = 1000;
    private static final int TCP_POLL_INTERVAL_MS = 500;
    private static final int TCP_POLL_MAX_MS = 15_000;
    private static final int ADB_CONNECT_RETRIES = 3;
    private static final int ADB_CONNECT_RETRY_MS = 1500;

    @FXML
    private Label lblEstado;
    @FXML
    private Label lblPunto;

    private String serialUsb = ""; // serial USB real (UOLNFALJNFCEDUUW)
    private String deviceIp = "";

    private final ADBService adbService = new ADBService();
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    private enum AdbMode {
        USB, WIFI
    }

    private AdbMode adbMode = AdbMode.USB;

    // ─────────────────────────────────────────────
    // SERIAL — recibe androidId, resuelve serial USB
    // ─────────────────────────────────────────────

    public void setSerial(String androidId) {
        taskExecutor.execute(() -> {
            try {
                // Resuelve el serial USB real a partir del androidId
                String resolved = adbService.getSerialActivo(androidId);
                // Si getSerialActivo devuelve una IP, no sirve para tcpip
                // Buscamos el serial USB explícitamente
                this.serialUsb = resolverSerialUsb(resolved);
                System.out.println("[WIFI] serialUsb resuelto: " + serialUsb);
            } catch (Exception e) {
                this.serialUsb = androidId;
                System.out.println("[WIFI] Fallback serialUsb: " + serialUsb);
            }
            actualizarEstadoInicial();
        });
    }

    /**
     * Si el serial resuelto es una IP (contiene '.'), busca el primer
     * serial USB real en adb devices. Si no encuentra ninguno, devuelve
     * lo que llegó como fallback.
     */
    private String resolverSerialUsb(String serialResuelto) {
        if (!serialResuelto.contains(".")) {
            return serialResuelto; // ya es un serial USB
        }
        // Es una IP — buscar serial USB en adb devices
        String devices = ejecutarComandoAdb("adb", "devices");
        for (String line : devices.split("\n")) {
            if (line.contains("\tdevice")) {
                String candidate = line.split("\t")[0].trim();
                if (!candidate.contains(".")) {
                    System.out.println("[WIFI] Serial USB encontrado: " + candidate);
                    return candidate;
                }
            }
        }
        System.out.println("[WIFI] No se encontró serial USB, fallback: " + serialResuelto);
        return serialResuelto;
    }

    // ─────────────────────────────────────────────
    // ESTADO INICIAL
    // ─────────────────────────────────────────────

    private void actualizarEstadoInicial() {
        taskExecutor.execute(() -> {
            String salida = adbService.ejecutarComandoSincrono(serialUsb, "shell ip addr show");
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
        System.out.println("[WIFI] Iniciando proceso de conexión inalámbrica. Serial USB: " + serialUsb);

        Platform.runLater(() -> setEstado("Detectando IP...", "#f9e2af"));

        taskExecutor.execute(() -> {
            try {
                if (serialUsb == null || serialUsb.isBlank()) {
                    Platform.runLater(() -> setEstado("Error: conecta el cable USB primero", "#f38ba8"));
                    return;
                }

                // 1. Detectar IP del dispositivo
                String ip = detectarIp(serialUsb);
                if (ip == null || ip.isBlank()) {
                    Platform.runLater(() -> setEstado(
                            "Error: no se detectó IP WiFi — ¿WiFi del móvil activa y conectada?", "#f38ba8"));
                    return;
                }
                this.deviceIp = ip;
                String endpoint = ip + ":" + ADB_WIFI_PORT;
                System.out.println("[WIFI] IP Detectada: " + endpoint);

                // CORRECCIÓN 1: Forzar desconexión previa total para limpiar sockets fantasmas
                // en la PC
                ejecutarComandoAdb("adb", "disconnect", endpoint);
                Thread.sleep(300); // Pequeña pausa para que el sistema operativo libere el socket

                // 2. Comprobar si el puerto ya responde
                boolean puertoYaAbierto = probarPuertoTcp(ip, ADB_WIFI_PORT);
                System.out.println("[WIFI] ¿Puerto 5555 ya estaba abierto? " + puertoYaAbierto);

                if (!puertoYaAbierto) {
                    Platform.runLater(() -> setEstado("Activando tcpip 5555...", "#f9e2af"));

                    // Forzamos el comando apuntando estrictamente al serial USB
                    String tcpRes = ejecutarComandoAdb("adb", "-s", serialUsb, "tcpip", String.valueOf(ADB_WIFI_PORT));
                    System.out.println("[WIFI] Resultado tcpip → " + tcpRes.trim());

                    // CORRECCIÓN 2: Pausa obligatoria defensiva. 'tcpip 5555' reinicia el adbd del
                    // celular.
                    // Si consultas el socket inmediatamente, la respuesta es impredecible.
                    Thread.sleep(1500);

                    Platform.runLater(() -> setEstado("Esperando puerto 5555...", "#f9e2af"));
                    boolean listo = esperarPuertoTcp(ip, ADB_WIFI_PORT, TCP_POLL_MAX_MS, TCP_POLL_INTERVAL_MS);

                    if (!listo) {
                        // Reintento rápido por si el demonio tardó de más en responder
                        System.out.println("[WIFI] Segundo intento de validación de puerto...");
                        listo = probarPuertoTcp(ip, ADB_WIFI_PORT);
                        if (!listo) {
                            Platform.runLater(() -> setEstado("Error: " + ip + ":5555 no responde", "#f38ba8"));
                            return;
                        }
                    }
                }

                System.out.println("[WIFI] Puerto 5555 verificado y abierto. Procediendo al enlace...");
                Platform.runLater(() -> setEstado("Conectando...", "#f9e2af"));

                // 3. Bucle elástico de conexión 'adb connect'
                String connectRes = "";
                for (int i = 1; i <= ADB_CONNECT_RETRIES; i++) {
                    System.out.println(
                            "[WIFI] Intento " + i + " de " + ADB_CONNECT_RETRIES + " → adb connect " + endpoint);
                    connectRes = ejecutarComandoAdb("adb", "connect", endpoint);
                    System.out.println("[WIFI] Respuesta de ADB → " + connectRes.trim());

                    // Validamos tanto el string de éxito como la presencia real en el listado de
                    // dispositivos
                    if (connectRes.toLowerCase().contains("connected") || estaConectadoPorWifi(endpoint)) {

                        // CORRECCIÓN 3: Confirmación de handshake seguro.
                        // A veces dice 'connected' pero el estado es 'unauthorized' en el dispositivo.
                        Thread.sleep(500);
                        if (verificarConexionReal(endpoint)) {
                            adbMode = AdbMode.WIFI;
                            final String ipFinal = ip;
                            resolverSerialUsbAsync();
                            Platform.runLater(() -> setEstado("WiFi activo — " + ipFinal, "#a6e3a1"));
                            return;
                        }
                    }

                    // Si falló, limpiamos el intento fallido antes del próximo reintento
                    ejecutarComandoAdb("adb", "disconnect", endpoint);
                    Thread.sleep(ADB_CONNECT_RETRY_MS);
                }

                // Si salimos del bucle es porque falló definitivamente
                final String errFinal = connectRes.isBlank() ? "Tiempo de espera agotado" : connectRes.trim();
                Platform.runLater(() -> setEstado("Error conexión: " + errFinal, "#f38ba8"));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> setEstado("Proceso interrumpido", "#f38ba8"));
            } catch (Exception e) {
                System.err.println("[WIFI] Error crítico en flujo: " + e.getMessage());
                Platform.runLater(() -> setEstado("Error crítico: " + e.getMessage(), "#f38ba8"));
            }
        });
    }

    private boolean verificarConexionReal(String endpoint) {
        String devices = ejecutarComandoAdb("adb", "devices");
        for (String line : devices.split("\n")) {
            if (line.contains(endpoint) && line.contains("\tdevice")) {
                return true;
            }
        }
        return false;
    }

    // Prueba el puerto UNA vez sin espera — para saber si ya está abierto
    private boolean probarPuertoTcp(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), TCP_POLL_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean estaConectadoPorWifi(String endpoint) {
        String devices = ejecutarComandoAdb("adb", "devices");
        for (String line : devices.split("\n")) {
            if (line.startsWith(endpoint) && line.contains("\tdevice")) {
                return true;
            }
        }
        return false;
    }

    // Refresca el serialUsb en background tras conectar por WiFi
    private void resolverSerialUsbAsync() {
        new Thread(() -> {
            String devices = ejecutarComandoAdb("adb", "devices");
            for (String line : devices.split("\n")) {
                if (line.contains("\tdevice")) {
                    String candidate = line.split("\t")[0].trim();
                    if (!candidate.contains(".")) {
                        serialUsb = candidate;
                        System.out.println("[WIFI] serialUsb actualizado: " + serialUsb);
                        return;
                    }
                }
            }
        }).start();
    }
    // ─────────────────────────────────────────────
    // DESCONECTAR WIFI
    // ─────────────────────────────────────────────

    @FXML
    private void conectarAdbUsb() {
        Platform.runLater(() -> setEstado("Desconectando WiFi...", "#f9e2af"));
        taskExecutor.execute(() -> {
            if (!deviceIp.isBlank()) {
                String res = ejecutarComandoAdb("adb", "disconnect",
                        deviceIp + ":" + ADB_WIFI_PORT);
                System.out.println("[WIFI] Disconnect: " + res);
            }
            adbMode = AdbMode.USB;
            Platform.runLater(() -> setEstado("USB activo", "#89b4fa"));
        });
    }

    // ─────────────────────────────────────────────
    // DETECTAR IP
    // ─────────────────────────────────────────────

  private String detectarIp(String dev) {
    // Intento 1: ip route — solo wlan0
    String route = ejecutarComandoAdb("adb", "-s", dev, "shell", "ip", "route");
    for (String line : route.split("\n")) {
        if (line.contains("wlan0") && line.contains("src")) {
            Matcher m = Pattern.compile("src\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
            if (m.find()) return m.group(1); // Si está en wlan0, es válida
        }
    }
    // Intento 2: ip addr show wlan0 explícitamente
    String addr = ejecutarComandoAdb("adb", "-s", dev, "shell", "ip", "addr", "show", "wlan0");
    for (String line : addr.split("\n")) {
        if (line.trim().startsWith("inet ")) {
            Matcher m = Pattern.compile("inet\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
            if (m.find()) return m.group(1); // Sin filtro de rango
        }
    }
    System.out.println("[WIFI] No se encontró IP wlan0:\n" + addr);
    return "";
}

    private boolean esTethering(String ip) {
        return ip.startsWith("192.168.42.") // Tethering USB estándar
                || ip.startsWith("192.168.113.") // Tethering USB alternativo ← tu caso
                || ip.startsWith("192.168.56.") // VirtualBox / emulador
                || ip.startsWith("10.0.2."); // Emulador Android Studio
    }

    // ─────────────────────────────────────────────
    // POLL TCP
    // ─────────────────────────────────────────────

    private boolean esperarPuertoTcp(String host, int port, int maxMs, int intervalMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), TCP_POLL_TIMEOUT_MS);
                return true;
            } catch (Exception ignored) {
            }
            Thread.sleep(intervalMs);
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // EJECUTAR COMANDO ADB
    // ─────────────────────────────────────────────

    private String ejecutarComandoAdb(String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line).append("\n");
            }
            if (!p.waitFor(12, TimeUnit.SECONDS))
                p.destroyForcibly();
            return sb.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    private void setEstado(String texto, String color) {
        lblEstado.setText(texto);
        lblPunto.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
    }
}