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
    @FXML
    private LineChart<Number, Number> batteryChart;
    @FXML
    private LineChart<Number, Number> cpuChart;
    @FXML
    private LineChart<Number, Number> ramChart;

    @FXML
    private Label labelReposo;
    @FXML
    private Label labelLlamada;
    @FXML
    private Label labelDiferencia;
    @FXML
    private TextField numeroTelefono;

    @FXML
    private Label lblBateriaActual;
    @FXML
    private Label lblBateriaEstado;
    @FXML
    private Label lblCpuActual;
    @FXML
    private Label lblCpuEstado;
    @FXML
    private Label lblRamActual;
    @FXML
    private Label lblRamEstado;

    // ───────────────────── SERIES ─────────────────────────
    private final XYChart.Series<Number, Number> batterySeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramSeries = new XYChart.Series<>();

    // ─────────────────── EXECUTION CORE ───────────────────
    private final ScheduledExecutorService monitorScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private ScheduledFuture<?> monitorTask;

    // ───────────────────── STATE ─────────────────────────
    private int time = 0;
    private double lastRam = 0;
    private long[] prevCpuStats = null;

    // ───────────────────── ADB ─────────────────────────
    private final ADBService adbService = new ADBService();
    private String serialActivo = null;

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
    }

    // ───────────────────── SET SERIAL ──────────────────────────
    public void setSerial(String androidId) {
        System.out.println("[LAB] Resolviendo serial para androidId: " + androidId);
        new Thread(() -> {
            try {
                String serial = adbService.getSerialActivo(androidId);
                this.serialActivo = serial;
                System.out.println("[LAB] Serial activo resuelto: " + serialActivo);
                Platform.runLater(this::startMonitor);
            } catch (Exception e) {
                this.serialActivo = androidId;
                System.out.println("[LAB] Fallback serial: " + serialActivo);
                Platform.runLater(this::startMonitor);
            }
        }).start();
    }

    @FXML
    private void iniciarTestConsumo() {
        System.out.println("[TEST] Iniciando medición con batterystats...");

        String numero = numeroTelefono.getText();

        if (numero == null || numero.isBlank()) {
            Platform.runLater(() -> labelDiferencia.setText("Introduce un número"));
            return;
        }

        taskExecutor.execute(() -> {
            try {
                // REPOSO
                updateUI("Reposo — 15:00", "-", "-");
                double reposo = medirFaseBatterystats(
                        TimeUnit.MINUTES.toMillis(2),
                        "Reposo");

                // LLAMADA
                updateUI(
                        String.format("Reposo: %.2f mAh", reposo),
                        "Iniciando llamada...",
                        "-");

                ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero);

                Thread.sleep(5000);

                double llamada = medirFaseBatterystats(
                        TimeUnit.MINUTES.toMillis(2),
                        "Llamada");

                ejecutarShell("input keyevent KEYCODE_ENDCALL");

                double diff = llamada - reposo;
                double pct = reposo > 0
                        ? ((diff / reposo) * 100)
                        : 0;

                Platform.runLater(() -> {
                    labelReposo.setText(
                            String.format("Reposo: %.2f mAh", reposo));

                    labelLlamada.setText(
                            String.format("Llamada: %.2f mAh", llamada));

                    labelDiferencia.setText(
                            String.format("Δ %.2f mAh (%.1f%%)", diff, pct));
                });

                System.out.printf(
                        "[TEST] Reposo=%.2f | Llamada=%.2f | Δ=%.2f (%.1f%%)%n",
                        reposo, llamada, diff, pct);

            } catch (Exception e) {
                Platform.runLater(() -> labelDiferencia.setText("Error: " + e.getMessage()));
            }
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
        taskExecutor.execute(() -> ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero));
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
                String out = ejecutarShell("dumpsys battery");
                int batPct = extraerNivelBateria(out);
                double cpu = obtenerUsoCpuReal();
                time += 5;

                if (time % 10 == 0)
                    lastRam = obtenerUsoRamReal();
                double ramSnapshot = lastRam;
                final int batFinal = batPct;
                final double cpuFinal = cpu;

                Platform.runLater(() -> {
                    batterySeries.getData().add(new XYChart.Data<>(time, batFinal));
                    cpuSeries.getData().add(new XYChart.Data<>(time, cpuFinal));
                    ramSeries.getData().add(new XYChart.Data<>(time, ramSnapshot));
                    trimSeries();
                    aplicarColores();

                    lblBateriaActual.setText(batFinal + "%");
                    lblBateriaEstado.setText(batFinal > 20 ? "Normal" : "Bajo");
                    lblCpuActual.setText(String.format("%.1f%%", cpuFinal));
                    lblCpuEstado.setText(cpuFinal > 80 ? "Alta carga" : "Normal");
                    lblRamActual.setText(String.format("%.0f MB", ramSnapshot));
                    lblRamEstado.setText(ramSnapshot > 0 ? "En uso" : "Sin datos");
                });

            } catch (Exception e) {
                System.out.println("[MONITOR] Error en ciclo: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // ───────────────────── CPU REAL desde /proc/stat ─────────────────────
    private double obtenerUsoCpuReal() {
        try {
            String out = ejecutarShell("cat /proc/stat");
            String cpuLine = out.split("\n")[0];
            String[] parts = cpuLine.trim().split("\\s+");

            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iowait = Long.parseLong(parts[5]);
            long irq = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long totalIdle = idle + iowait;
            long totalActive = user + nice + system + irq + softirq;
            long total = totalIdle + totalActive;

            if (prevCpuStats == null) {
                prevCpuStats = new long[] { totalIdle, total };
                return 0.0;
            }

            long deltaTotal = total - prevCpuStats[1];
            long deltaIdle = totalIdle - prevCpuStats[0];
            prevCpuStats = new long[] { totalIdle, total };

            if (deltaTotal == 0)
                return 0.0;
            double cpuPct = 100.0 * (deltaTotal - deltaIdle) / deltaTotal;
            return Math.min(Math.max(cpuPct, 0.0), 100.0);

        } catch (Exception e) {
            System.out.println("[CPU] Error leyendo /proc/stat: " + e.getMessage());
            return 0;
        }
    }

    private int extraerNivelBateria(String out) {
        try {
            for (String line : out.split("\n")) {
                String l = line.trim().toLowerCase();

                if (l.startsWith("level:")) {
                    return Integer.parseInt(
                            l.replace("level:", "").trim());
                }
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private double leerConsumoBatterystats() {
        try {
            String out = ejecutarShell("dumpsys batterystats");

            for (String line : out.split("\n")) {
                String l = line.trim().toLowerCase();

                if (l.startsWith("discharge:")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("(\\d+[.,]?\\d*)")
                            .matcher(l);

                    if (m.find()) {
                        double valor = Double.parseDouble(
                                m.group(1).replace(",", "."));

                        System.out.printf(
                                "[BATTERYSTATS] Discharge real: %.2f mAh%n",
                                valor);

                        return valor;
                    }
                }
            }

            return 0;

        } catch (Exception e) {
            System.out.println("[BATTERYSTATS] Error: " + e.getMessage());
            return 0;
        }
    }

    private void resetBatteryStats() {
        ejecutarShell("cmd batterystats reset");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        System.out.println("[BATTERYSTATS] Reiniciado");
    }

    private double medirFaseBatterystats(long duracionMs, String fase) {
        try {
            resetBatteryStats();

            long inicio = System.currentTimeMillis();

            while (System.currentTimeMillis() - inicio < duracionMs) {
                long restante = duracionMs - (System.currentTimeMillis() - inicio);

                int min = (int) (restante / 60000);
                int seg = (int) ((restante % 60000) / 1000);

                String txt = fase + " — " + min + ":" + String.format("%02d", seg);

                Platform.runLater(() -> {
                    if (fase.equals("Reposo"))
                        labelReposo.setText(txt);
                    else
                        labelLlamada.setText(txt);
                });

                Thread.sleep(1000);
            }

            return leerConsumoBatterystats();

        } catch (Exception e) {
            System.out.println("[FASE] Error: " + e.getMessage());
            return 0;
        }
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
        } catch (Exception ignored) {
        }
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
            while ((line = br.readLine()) != null)
                sb.append(line).append("\n");
            p.waitFor(10, TimeUnit.SECONDS);
            return sb.toString().trim();
        } catch (Exception e) {
            System.out.println("[ADB] Error: " + e.getMessage());
            return "";
        }
    }

    // ───────────────────── RAM ─────────────────────────
    private double obtenerUsoRamReal() {
        String out = ejecutarShell("cat /proc/meminfo");
        double total = 0, avail = 0;
        for (String l : out.split("\n")) {
            try {
                if (l.startsWith("MemTotal"))
                    total = Double.parseDouble(l.replaceAll("[^0-9]", ""));
                if (l.startsWith("MemAvailable"))
                    avail = Double.parseDouble(l.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
            }
        }
        return (total > 0) ? (total - avail) / 1024.0 : 0;
    }

    // ───────────────────── STOP ─────────────────────
    public void stop() {
        System.out.println("[APP] Deteniendo servicios...");
        if (monitorTask != null)
            monitorTask.cancel(true);
        monitorScheduler.shutdownNow();
        taskExecutor.shutdownNow();
        System.out.println("[APP] Servicios detenidos correctamente");
    }
}