package com.example.View;

import com.example.Controller.ADBService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;

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

    // ───────────────────── ADB ─────────────────────────
    private final ADBService adbService = new ADBService();
    private String serialActivo = null; // se rellena en setSerial()

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

        // NO arrancamos el monitor aquí — esperamos a tener el serial
    }

    // ───────────────────── SET SERIAL ──────────────────────────
    // Recibe el android_id y resuelve el serial activo (IP o serial USB)
    public void setSerial(String androidId) {
        System.out.println("[LAB] Resolviendo serial para androidId: " + androidId);
        new Thread(() -> {
            try {
                String serial = adbService.getSerialActivo(androidId);
                this.serialActivo = serial;
                System.out.println("[LAB] Serial activo resuelto: " + serialActivo);
                Platform.runLater(this::startMonitor);
            } catch (Exception e) {
                // Fallback: usa el android_id directamente
                this.serialActivo = androidId;
                System.out.println("[LAB] Fallback serial: " + serialActivo);
                Platform.runLater(this::startMonitor);
            }
        }).start();
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
                ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero));
    }

    // ───────────────────── MONITOR ─────────────────────
    private void startMonitor() {
        if (serialActivo == null) {
            System.out.println("[MONITOR] Sin serial, no se puede iniciar");
            return;
        }
        System.out.println("[MONITOR] Iniciando monitorización con serial: " + serialActivo);

        monitorTask = monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                double cpu = obtenerUsoCpuReal();
                int    bat = obtenerNivelBateriaReal();
                time += 5;

                if (time % 10 == 0) {
                    lastRam = obtenerUsoRamReal();
                }

                double ramSnapshot = lastRam;


                Platform.runLater(() -> {
                    batterySeries.getData().add(new XYChart.Data<>(time, bat));
                    cpuSeries.getData().add(new XYChart.Data<>(time, cpu));
                    ramSeries.getData().add(new XYChart.Data<>(time, ramSnapshot));
                    trimSeries();
                    if (time == 5) aplicarColores();
                });

            } catch (Exception e) {
                System.out.println("[MONITOR] Error en ciclo: " + e.getMessage());
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

        if (serialActivo == null) {
            Platform.runLater(() -> labelDiferencia.setText("Sin dispositivo conectado"));
            System.out.println("[TEST] Sin serial activo, abortando");
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
                ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero);

                Thread.sleep(5000); // espera a que conecte
                System.out.println("[TEST] Llamada activa, midiendo durante 5 minutos...");
                updateUI(String.format("Reposo: %.0f µA", idleCurrent), "Llamada: 5 min...", "-");

                Thread.sleep(TimeUnit.MINUTES.toMillis(5));

                double callCurrent = leerCorrienteUa();
                System.out.println("[TEST] Corriente en llamada: " + callCurrent + " µA");

                // ── CUELGA ───────────────────────────────────────
                ejecutarShell("input keyevent KEYCODE_ENDCALL");
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
    private String ejecutarShell(String shellCmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "adb", "-s", serialActivo, "shell", shellCmd);
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
            System.out.println("[ADB] Error: " + e.getMessage());
            return "";
        }
    }

    // ───────────────────── LECTURAS DISPOSITIVO ─────────────────────
    private double leerCorrienteUa() {
        String out = ejecutarShell("cat /sys/class/power_supply/battery/current_now");
        try {
            return Math.abs(Double.parseDouble(out.trim()));
        } catch (NumberFormatException e) {
            System.out.println("[SENSOR] No se pudo leer current_now, valor: " + out);
            return 0;
        }
    }

    private double obtenerUsoCpuReal() {
        String out = ejecutarShell("cat /proc/loadavg");
        try {
            return Math.min(Double.parseDouble(out.split(" ")[0]) * 10, 100);
        } catch (Exception e) {
            return 0;
        }
    }

    private int obtenerNivelBateriaReal() {
        String out = ejecutarShell("dumpsys battery");
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
        String out = ejecutarShell("cat /proc/meminfo");
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