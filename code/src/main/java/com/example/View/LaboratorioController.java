package com.example.View;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaboratorioController {

    // ───────────────────────── UI ─────────────────────────
    @FXML private LineChart<Number, Number> batteryChart;
    @FXML private LineChart<Number, Number> cpuChart;
    @FXML private LineChart<Number, Number> ramChart;

    @FXML private Label labelReposo;
    @FXML private Label labelLlamada;
    @FXML private Label labelDiferencia;
    @FXML private TextField numeroTelefono;

    // ───────────────────── SERIES ─────────────────────────
    private final XYChart.Series<Number, Number> batterySeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> cpuSeries     = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramSeries     = new XYChart.Series<>();

    // ─────────────────── EXECUTION CORE ───────────────────
    private final ScheduledExecutorService monitorScheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService taskExecutor =
            Executors.newCachedThreadPool();
    private ScheduledFuture<?> monitorTask;

    // ───────────────────── STATE ─────────────────────────
    private int    time    = 0;
    private double lastRam = 0;

    // ───────────────────── ADB MODE ─────────────────────
    private enum AdbMode { USB, WIFI }
    private AdbMode adbMode  = AdbMode.USB;
    private String  deviceIp = "10.55.107.115";

    // ───────────────────── INIT ──────────────────────────
    @FXML
    public void initialize() {
        batterySeries.setName("Battery");
        cpuSeries.setName("CPU");
        ramSeries.setName("RAM");

        batteryChart.getData().add(batterySeries);
        cpuChart.getData().add(cpuSeries);
        ramChart.getData().add(ramSeries);

        batteryChart.setAnimated(false);
        cpuChart.setAnimated(false);
        ramChart.setAnimated(false);

        startMonitor();
    }

    // ───────────────────── ADB WIFI ─────────────────────
    @FXML
    private void conectarAdbWifi() {
        System.out.println("[ADB] Iniciando conexión WiFi...");

        taskExecutor.execute(() -> {
            try {
                System.out.println("[ADB] Activando modo TCP en puerto 5555...");
                String tcpResult = ejecutarComandoAdb(new String[]{"adb", "tcpip", "5555"});
                System.out.println("[ADB] tcpip resultado: " + tcpResult);
                Thread.sleep(2000);

                System.out.println("[ADB] Detectando IP del dispositivo...");
                detectarIpDispositivo();
                System.out.println("[ADB] IP detectada: " + deviceIp);

                System.out.println("[ADB] Conectando a " + deviceIp + ":5555 ...");
                String connectResult = ejecutarComandoAdb(
                        new String[]{"adb", "connect", deviceIp + ":5555"});
                System.out.println("[ADB] Resultado conexión: " + connectResult);

                if (connectResult.contains("connected")) {
                    adbMode = AdbMode.WIFI;
                    System.out.println("[ADB] ✔ Conexión WiFi establecida correctamente con " + deviceIp);
                    Platform.runLater(() -> labelDiferencia.setText("ADB WiFi OK — " + deviceIp));
                } else {
                    System.out.println("[ADB] ✖ Falló la conexión WiFi. Respuesta: " + connectResult);
                    Platform.runLater(() -> labelDiferencia.setText("Error WiFi: " + connectResult));
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[ADB] ✖ Conexión interrumpida");
            } catch (Exception e) {
                System.out.println("[ADB] ✖ Excepción al conectar por WiFi: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void conectarAdbUsb() {
        System.out.println("[ADB] Volviendo a modo USB...");

        taskExecutor.execute(() -> {
            String result = ejecutarComandoAdb(
                    new String[]{"adb", "disconnect", deviceIp + ":5555"});
            System.out.println("[ADB] Disconnect resultado: " + result);
            adbMode = AdbMode.USB;
            System.out.println("[ADB] ✔ Modo USB activado correctamente");
            Platform.runLater(() -> labelDiferencia.setText("ADB USB activo"));
        });
    }

    // ───────────────────── LLAMADA ─────────────────────
    @FXML
    private void iniciarLlamada() {
        String numero = numeroTelefono.getText();
        if (numero == null || numero.isBlank()) {
            System.out.println("[LLAMADA] No se introdujo número de teléfono");
            return;
        }
        System.out.println("[LLAMADA] Iniciando llamada a: " + numero);
        taskExecutor.execute(() ->
                ejecutarComandoAdbShell("am start -a android.intent.action.CALL -d tel:" + numero));
    }

    // ───────────────────── MONITOR ─────────────────────
    private void startMonitor() {
        System.out.println("[MONITOR] Iniciando monitorización en tiempo real...");

        monitorTask = monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                double cpu = obtenerUsoCpuReal();
                int    bat = obtenerNivelBateriaReal();
                time += 5;

                if (time % 10 == 0) {
                    lastRam = obtenerUsoRamReal();
                }

                double ramSnapshot = lastRam;

                System.out.printf("[MONITOR] t=%ds | Batería=%d%% | CPU=%.1f%% | RAM=%.0f MB%n",
                        time, bat, cpu, ramSnapshot);

                Platform.runLater(() -> {
                    batterySeries.getData().add(new XYChart.Data<>(time, bat));
                    cpuSeries.getData().add(new XYChart.Data<>(time, cpu));
                    ramSeries.getData().add(new XYChart.Data<>(time, ramSnapshot));
                    trimSeries();
                    if (time == 5) aplicarColores();
                });

            } catch (Exception e) {
                System.out.println("[MONITOR] Error en ciclo: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // ───────────────────── TEST CONSUMO ─────────────────────
    @FXML
    private void iniciarTestConsumo() {
        System.out.println("[TEST] Iniciando test de consumo energético...");

        String numero = numeroTelefono.getText();
        if (numero == null || numero.isBlank()) {
            Platform.runLater(() -> labelDiferencia.setText("Introduce un número primero"));
            System.out.println("[TEST] No hay número de teléfono, abortando");
            return;
        }

        taskExecutor.execute(() -> {
            try {
                // ── FASE 1: REPOSO (5 minutos) ──────────────────
                System.out.println("[TEST] Fase 1 — Reposo durante 5 minutos...");
                updateUI("Reposo: 5 min...", "-", "-");

                Thread.sleep(TimeUnit.MINUTES.toMillis(5));

                double idleCurrent = leerCorrienteUa();
                System.out.println("[TEST] Corriente reposo: " + idleCurrent + " µA");
                updateUI(String.format("Reposo: %.0f µA", idleCurrent), "Iniciando llamada...", "-");

                // ── FASE 2: LLAMADA (5 minutos) ──────────────────
                System.out.println("[TEST] Fase 2 — Iniciando llamada a: " + numero);
                ejecutarComandoAdbShell("am start -a android.intent.action.CALL -d tel:" + numero);

                // Espera 5 segundos a que la llamada conecte antes de empezar a medir
                Thread.sleep(5000);
                System.out.println("[TEST] Llamada activa, midiendo durante 5 minutos...");
                updateUI(String.format("Reposo: %.0f µA", idleCurrent), "Llamada: 5 min...", "-");

                Thread.sleep(TimeUnit.MINUTES.toMillis(5));

                double callCurrent = leerCorrienteUa();
                System.out.println("[TEST] Corriente en llamada: " + callCurrent + " µA");

                // ── CUELGA ───────────────────────────────────────
                ejecutarComandoAdbShell("input keyevent KEYCODE_ENDCALL");
                System.out.println("[TEST] Llamada finalizada");

                // ── RESULTADO ────────────────────────────────────
                double diff = callCurrent - idleCurrent;
                System.out.printf("[TEST] ✔ Reposo=%.0fµA | Llamada=%.0fµA | Δ=%.0fµA%n",
                        idleCurrent, callCurrent, diff);

                Platform.runLater(() -> {
                    labelReposo.setText(String.format("Reposo:  %.0f µA", idleCurrent));
                    labelLlamada.setText(String.format("Llamada: %.0f µA", callCurrent));
                    labelDiferencia.setText(String.format("Δ: %.0f µA", diff));
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[TEST] ✖ Test interrumpido");
                Platform.runLater(() -> labelDiferencia.setText("Test interrumpido"));
            } catch (Exception e) {
                System.out.println("[TEST] ✖ Error: " + e.getMessage());
                Platform.runLater(() -> labelDiferencia.setText("Error: " + e.getMessage()));
            }
        });
    }

    // ───────────────────── HELPERS UI ─────────────────────
    private void updateUI(String r, String l, String d) {
        Platform.runLater(() -> {
            labelReposo.setText(r);
            labelLlamada.setText(l);
            labelDiferencia.setText(d);
        });
    }

    private void trimSeries() {
        if (cpuSeries.getData().size() > 25) {
            cpuSeries.getData().remove(0);
            batterySeries.getData().remove(0);
            ramSeries.getData().remove(0);
        }
    }

    private void aplicarColores() {
        try {
            batterySeries.getNode().lookup(".chart-series-line")
                    .setStyle("-fx-stroke: #2ecc71;");
            cpuSeries.getNode().lookup(".chart-series-line")
                    .setStyle("-fx-stroke: #a29bfe;");
            ramSeries.getNode().lookup(".chart-series-line")
                    .setStyle("-fx-stroke: #fd79a8;");
        } catch (Exception ignored) {}
    }

    // ───────────────────── ADB CORE ─────────────────────
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

    private String ejecutarComandoAdbShell(String shellCmd) {
        if (adbMode == AdbMode.WIFI) {
            return ejecutarComandoAdb(
                    new String[]{"adb", "-s", deviceIp + ":5555", "shell", shellCmd});
        } else {
            return ejecutarComandoAdb(
                    new String[]{"adb", "shell", shellCmd});
        }
    }

    private void detectarIpDispositivo() {
        String out = ejecutarComandoAdb(new String[]{"adb", "shell", "ip", "route"});
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
        System.out.println("[ADB] No se pudo detectar IP wlan0, usando: " + deviceIp);
    }

    // ───────────────────── LECTURAS DISPOSITIVO ─────────────────────
    private double leerCorrienteUa() {
        String out = ejecutarComandoAdbShell("cat /sys/class/power_supply/battery/current_now");
        try {
            double ua = Double.parseDouble(out.trim());
            return Math.abs(ua);
        } catch (NumberFormatException e) {
            System.out.println("[SENSOR] No se pudo leer current_now, valor: " + out);
            return 0;
        }
    }

    private double obtenerUsoCpuReal() {
        String out = ejecutarComandoAdbShell("cat /proc/loadavg");
        try {
            return Math.min(Double.parseDouble(out.split(" ")[0]) * 10, 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private int obtenerNivelBateriaReal() {
        String out = ejecutarComandoAdbShell("dumpsys battery");
        for (String l : out.split("\n")) {
            if (l.trim().startsWith("level")) {
                try {
                    return Integer.parseInt(l.split(":")[1].trim());
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private double obtenerUsoRamReal() {
        String out = ejecutarComandoAdbShell("cat /proc/meminfo");
        double total = 0, avail = 0;

        for (String l : out.split("\n")) {
            try {
                if (l.startsWith("MemTotal"))
                    total = Double.parseDouble(l.replaceAll("[^0-9]", ""));
                if (l.startsWith("MemAvailable"))
                    avail = Double.parseDouble(l.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }

        return (total > 0) ? (total - avail) / 1024.0 : 0;
    }

    // ───────────────────── STOP ─────────────────────
    public void stop() {
        System.out.println("[APP] Deteniendo servicios...");
        if (monitorTask != null) monitorTask.cancel(true);
        monitorScheduler.shutdownNow();
        taskExecutor.shutdownNow();
        System.out.println("[APP] Servicios detenidos correctamente");
    }
}