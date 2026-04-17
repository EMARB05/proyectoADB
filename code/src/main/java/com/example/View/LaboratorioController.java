package com.example.View;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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
    private final XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramSeries = new XYChart.Series<>();

    // ─────────────────── EXECUTION CORE ───────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> monitorTask;

    // ───────────────────── STATE ─────────────────────────
    private int time = 0;
    private double lastRam = 0;

    // ───────────────────── INIT ──────────────────────────
    @FXML
    public void initialize() {

        batterySeries.setName("Battery");
        cpuSeries.setName("CPU");
        ramSeries.setName("RAM");

        batteryChart.getData().add(batterySeries);
        cpuChart.getData().add(cpuSeries);
        ramChart.getData().add(ramSeries);

        startMonitor();
    }


    @FXML
private void iniciarLlamada() {

    String numero = numeroTelefono.getText();

    if (numero == null || numero.isBlank()) return;

    ejecutarComandoAdb("shell am start -a android.intent.action.CALL -d tel:" + numero);
}

    // ───────────────────── DASHBOARD LOOP ─────────────────
    private void startMonitor() {

        monitorTask = scheduler.scheduleAtFixedRate(() -> {

            try {
                double cpu = obtenerUsoCpuReal();
                int bat = obtenerNivelBateriaReal();

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
                e.printStackTrace();
            }

        }, 0, 5, TimeUnit.SECONDS);
    }

    // ───────────────────── TEST JOB ───────────────────────
    @FXML
    private void iniciarTestConsumo() {

        scheduler.execute(() -> {

            try {
                updateUI("Midiendo reposo...", "-", "-");

                ejecutarComandoAdb("shell dumpsys batterystats --reset");
                ejecutarComandoAdb("shell input keyevent 26"); // pantalla off

                int batStartIdle = obtenerNivelBateriaReal();
                sleep(300000);

                int batEndIdle = obtenerNivelBateriaReal();

                updateUI("Midiendo en llamada...", "-", "-");

                ejecutarComandoAdb("shell input keyevent 26");
                ejecutarComandoAdb("shell am start -a android.intent.action.DIAL");

                int batStartLoad = obtenerNivelBateriaReal();
                sleep(300000);

                int batEndLoad = obtenerNivelBateriaReal();

                double idle = batStartIdle - batEndIdle;
                double load = batStartLoad - batEndLoad;

                Platform.runLater(() -> {
                    labelReposo.setText("Reposo: " + idle + "%");
                    labelLlamada.setText("En llamada: " + load + "%");
                    labelDiferencia.setText("Δ: " + (load - idle) + "%");
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        labelDiferencia.setText("Error: " + e.getMessage()));
            }
        });
    }

    // ───────────────────── HELPERS ────────────────────────
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

    private void sleep(int sec) {
        try {
            Thread.sleep(sec * 1000L);
        } catch (InterruptedException ignored) {}
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

    // ───────────────────── ADB ────────────────────────────
    private String ejecutarComandoAdb(String cmd) {
        try {
            Process p = new ProcessBuilder(("adb " + cmd).split(" "))
                    .redirectErrorStream(true)
                    .start();

            return new String(p.getInputStream().readAllBytes()).trim();

        } catch (Exception e) {
            return "";
        }
    }

    private double obtenerUsoCpuReal() {
        try {
            return Math.min(
                    Double.parseDouble(ejecutarComandoAdb("shell cat /proc/loadavg")
                            .split(" ")[0]) * 10,
                    100
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private int obtenerNivelBateriaReal() {
        String out = ejecutarComandoAdb("shell dumpsys battery");
        for (String l : out.split("\n")) {
            if (l.contains("level")) {
                return Integer.parseInt(l.split(":")[1].trim());
            }
        }
        return 0;
    }

    private double obtenerUsoRamReal() {
        String out = ejecutarComandoAdb("shell cat /proc/meminfo");

        double total = 0, avail = 0;

        for (String l : out.split("\n")) {
            if (l.startsWith("MemTotal")) {
                total = Double.parseDouble(l.replaceAll("[^0-9]", ""));
            }
            if (l.startsWith("MemAvailable")) {
                avail = Double.parseDouble(l.replaceAll("[^0-9]", ""));
            }
        }

        return (total - avail) / 1024;
    }

    // ───────────────────── SHUTDOWN ───────────────────────
    public void stop() {
        if (monitorTask != null) monitorTask.cancel(true);
        scheduler.shutdownNow();
    }
}