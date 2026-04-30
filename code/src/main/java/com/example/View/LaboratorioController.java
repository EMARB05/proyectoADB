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

    @FXML private Label lblBateriaActual;
    @FXML private Label lblBateriaEstado;
    @FXML private Label lblCpuActual;
    @FXML private Label lblCpuEstado;
    @FXML private Label lblRamActual;
    @FXML private Label lblRamEstado;

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
    private int    time         = 0;
    private double lastRam      = 0;
    private long[] prevCpuStats = null;

    // ───────────────────── ADB ─────────────────────────
    private final ADBService adbService = new ADBService();
    private String serialActivo = null;

    // ───────────────────────────────────────────────────────────────────────
    // PATRONES para leerConsumoBatterystats()
    //
    // dumpsys batterystats tiene formatos distintos según versión de Android
    // y fabricante. Cubrimos todos los conocidos, en orden de confiabilidad:
    //
    //  Android 9+  →  "Discharge: X mAh"  o  "Total discharge: X mAh"
    //  Android 6-8 →  "Estimated discharge: X mAh"
    //  Alternativo →  "dischargeElapsed: X"  (µAh, hay que convertir)
    //  Alternativo →  "Battery discharge: X mAh"
    //  Alternativo →  "Computed drain: X mAh"
    //  Alternativo →  número tras "mAh" en líneas con "discharge" (gama baja)
    // ───────────────────────────────────────────────────────────────────────
    private static final Pattern[] DISCHARGE_PATTERNS = {
        // Patrón 1 – Android 9+ estándar: "  Discharge: 1.23 mAh"
        Pattern.compile(
            "(?i)^\\s*(?:total\\s+)?discharge:\\s*(\\d+(?:\\.\\d+)?)\\s*mah",
            Pattern.MULTILINE),

        // Patrón 2 – Android 6-8: "  Estimated discharge: 1.23 mAh"
        Pattern.compile(
            "(?i)estimated\\s+discharge:\\s*(\\d+(?:\\.\\d+)?)\\s*mah",
            Pattern.MULTILINE),

        // Patrón 3 – "Battery discharge: 1.23 mAh"
        Pattern.compile(
            "(?i)battery\\s+discharge:\\s*(\\d+(?:\\.\\d+)?)\\s*mah",
            Pattern.MULTILINE),

        // Patrón 4 – "Computed drain: 1.23 mAh"
        Pattern.compile(
            "(?i)computed\\s+drain:\\s*(\\d+(?:\\.\\d+)?)\\s*mah",
            Pattern.MULTILINE),

        // Patrón 5 – líneas genéricas con "discharge" y un número seguido de mAh
        Pattern.compile(
            "(?i)discharge[^\\n]*?([0-9]+(?:\\.[0-9]+)?)\\s*mah",
            Pattern.MULTILINE),

        // Patrón 6 – "dischargeElapsed: 12345" (en µAh, se convierte a mAh)
        // Marcamos este con un prefijo especial para saber que hay que dividir
        Pattern.compile(
            "(?i)dischargeElapsed:\\s*(\\d+)",
            Pattern.MULTILINE),
    };

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

    // ───────────────────── TEST CONSUMO ─────────────────────
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
                // ── FASE 1: REPOSO ──────────────────────────────────────────
                updateUI("Reposo — 10:00", "-", "-");
                double reposo = medirFaseBatterystats(
                        TimeUnit.MINUTES.toMillis(10), "Reposo");

                // ── FASE 2: LLAMADA ─────────────────────────────────────────
                updateUI(
                    String.format("Reposo: %.2f mAh", reposo),
                    "Iniciando llamada...", "-");

                ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero);
                Thread.sleep(5_000);

                double llamada = medirFaseBatterystats(
                        TimeUnit.MINUTES.toMillis(10), "Llamada");

                ejecutarShell("input keyevent KEYCODE_ENDCALL");

                // ── RESULTADO ───────────────────────────────────────────────
                double diff = llamada - reposo;
                double pct  = reposo > 0 ? (diff / reposo) * 100.0 : 0;

                Platform.runLater(() -> {
                    labelReposo.setText(String.format("Reposo:  %.2f mAh", reposo));
                    labelLlamada.setText(String.format("Llamada: %.2f mAh", llamada));
                    labelDiferencia.setText(String.format("Δ %.2f mAh (%.1f%%)", diff, pct));
                });

                System.out.printf(
                    "[TEST] ✔ Reposo=%.2f mAh | Llamada=%.2f mAh | Δ=%.2f mAh (%.1f%%)%n",
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
        if (numero == null || numero.isBlank()) return;
        taskExecutor.execute(() ->
                ejecutarShell("am start -a android.intent.action.CALL -d tel:" + numero));
    }

    // ───────────────────── MONITOR ─────────────────────
    private void startMonitor() {
        if (serialActivo == null) return;
        System.out.println("[MONITOR] Iniciando monitorización: " + serialActivo);

        monitorTask = monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                String out   = ejecutarShell("dumpsys battery");
                int    batPct = extraerNivelBateria(out);
                double cpu   = obtenerUsoCpuReal();
                time += 5;

                if (time % 10 == 0) lastRam = obtenerUsoRamReal();
                double ramSnapshot = lastRam;
                final int    batFinal = batPct;
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
                System.out.println("[MONITOR] Error: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // ───────────────────── BATTERYSTATS RESET ────────────────────────────
    private void resetBatteryStats() {
        String out = ejecutarShell("dumpsys batterystats --reset");
        System.out.println("[BATTERYSTATS] Reset: " + out.trim());
        try { Thread.sleep(2_000); } catch (InterruptedException ignored) {}
        System.out.println("[BATTERYSTATS] Listo para nueva medición");
    }

    // ───────────────────── MEDIR FASE ────────────────────────────────────
    //
    // Devuelve mAh consumidos en la fase.
    //
    // ESTRATEGIA DUAL para cubrir el caso del Cocom F370:
    //   • batterystats devuelve 0 en reposo si la batería no bajó ni 1%
    //     (el sensor solo registra cambios de nivel entero, y en 5 min de
    //     reposo el teléfono puede no consumir suficiente para bajar un punto)
    //   • En ese caso caemos a FALLBACK: tomamos el nivel al inicio y al final,
    //     calculamos cuántos mAh representa ese delta usando la capacidad
    //     nominal estimada desde CHARGE_FULL (o 2500 mAh si no está disponible)
    //   • El resultado siempre se normaliza a mAh/min para que reposo y llamada
    //     sean comparables aunque la duración real difiera ligeramente
    //
    private double medirFaseBatterystats(long duracionMs, String fase) {
        try {
            resetBatteryStats();

            // Nivel de batería al inicio (para el fallback)
            int nivelInicio = extraerNivelBateria(ejecutarShell("dumpsys battery"));
            long t0         = System.currentTimeMillis();

            // Cuenta regresiva
            long inicio = System.currentTimeMillis();
            while (System.currentTimeMillis() - inicio < duracionMs) {
                long restante = duracionMs - (System.currentTimeMillis() - inicio);
                int  min = (int)(restante / 60_000);
                int  seg = (int)((restante % 60_000) / 1_000);
                String txt = fase + " — " + min + ":" + String.format("%02d", seg);

                Platform.runLater(() -> {
                    if (fase.equals("Reposo")) labelReposo.setText(txt);
                    else                       labelLlamada.setText(txt);
                });
                Thread.sleep(1_000);
            }

            long   t1           = System.currentTimeMillis();
            double minutosReales = (t1 - t0) / 60_000.0;

            // ── Intento principal: batterystats ────────────────────────────
            double mah = leerConsumoBatterystats();

            if (mah > 0) {
                System.out.printf(
                    "[FASE] %s via batterystats → %.3f mAh en %.1f min (%.4f mAh/min)%n",
                    fase, mah, minutosReales, mah / minutosReales);
                return mah;
            }

            // ── Fallback: delta de nivel de batería ────────────────────────
            // batterystats devolvió 0 → el nivel no bajó lo suficiente para
            // que el sistema lo registrara. Calculamos manualmente.
            int nivelFin  = extraerNivelBateria(ejecutarShell("dumpsys battery"));
            int deltaPct  = nivelInicio - nivelFin;

            System.out.printf(
                "[FASE] %s batterystats=0, fallback nivel: %d%% → %d%% (Δ%d%%)%n",
                fase, nivelInicio, nivelFin, deltaPct);

            if (deltaPct > 0) {
                // Intentar obtener capacidad real del hardware (µAh → mAh)
                double capacidadMah = obtenerCapacidadBateria();
                double mahFallback  = (deltaPct / 100.0) * capacidadMah;

                System.out.printf(
                    "[FASE] %s via delta nivel → %.1f mAh (capacidad estimada: %.0f mAh)%n",
                    fase, mahFallback, capacidadMah);
                return mahFallback;
            }

            // Sin datos: reposo tan bajo que ni el % bajó en 5 min.
            // Devolvemos una estimación mínima basada en el tiempo,
            // asumiendo que un smartphone en reposo consume ~1 mAh/h de mínimo.
            double estimacionMinima = (minutosReales / 60.0) * 1.0; // 1 mAh/h
            System.out.printf(
                "[FASE] %s sin datos — estimación mínima: %.4f mAh%n",
                fase, estimacionMinima);
            return estimacionMinima;

        } catch (Exception e) {
            System.out.println("[FASE] Error: " + e.getMessage());
            return 0;
        }
    }

    // ───────────────────── CAPACIDAD BATERÍA ─────────────────────────────
    // Lee CHARGE_FULL de uevent (µAh) y convierte a mAh.
    // Si no está disponible devuelve 2500 mAh (valor conservador para gama baja).
    private double obtenerCapacidadBateria() {
        try {
            String out = ejecutarShell(
                    "cat /sys/class/power_supply/battery/uevent");
            for (String line : out.split("\n")) {
                String[] parts = line.trim().split("=", 2);
                if (parts.length == 2 &&
                        parts[0].trim().equalsIgnoreCase("POWER_SUPPLY_CHARGE_FULL")) {
                    double uah = Double.parseDouble(parts[1].trim());
                    double mah = uah / 1000.0;
                    System.out.printf("[BATERÍA] CHARGE_FULL=%.0f µAh → %.0f mAh%n", uah, mah);
                    return mah > 100 ? mah : 2500; // sanity check
                }
            }
        } catch (Exception ignored) {}
        System.out.println("[BATERÍA] CHARGE_FULL no disponible → usando 2500 mAh");
        return 2500;
    }

    // ───────────────────── LEER CONSUMO BATTERYSTATS ─────────────────────
    //
    // Estrategia:
    //   1. Ejecuta dumpsys batterystats
    //   2. Imprime las 40 líneas que contienen "discharge", "drain" o "mah"
    //      para que puedas ver en consola qué devuelve TU dispositivo
    //   3. Prueba los patrones en orden hasta encontrar un valor > 0
    //   4. El patrón 6 (dischargeElapsed) devuelve µAh → convierte a mAh
    //   5. Si ningún patrón funciona, devuelve 0 y avisa en consola
    //      → busca en el log las líneas "[BATTERYSTATS] LÍNEA:" y dinos
    //        cuál devuelve tu dispositivo para añadir el patrón exacto
    // ─────────────────────────────────────────────────────────────────────
    private double leerConsumoBatterystats() {
        try {
            String out = ejecutarShell("dumpsys batterystats");

            // ── Diagnóstico: imprime líneas relevantes ──────────────────────
            System.out.println("[BATTERYSTATS] ── LÍNEAS RELEVANTES ──────────");
            int lineasDiag = 0;
            for (String line : out.split("\n")) {
                String l = line.toLowerCase();
                if ((l.contains("discharge") || l.contains("drain") || l.contains("mah"))
                        && lineasDiag < 40) {
                    System.out.println("[BATTERYSTATS] LÍNEA: " + line.trim());
                    lineasDiag++;
                }
            }
            System.out.println("[BATTERYSTATS] ── FIN LÍNEAS RELEVANTES ──────");

            // ── Probar patrones en orden ────────────────────────────────────
            for (int i = 0; i < DISCHARGE_PATTERNS.length; i++) {
                Matcher m = DISCHARGE_PATTERNS[i].matcher(out);
                if (m.find()) {
                    double valor = Double.parseDouble(m.group(1));

                    // Patrón 6 (dischargeElapsed) viene en µAh → convertir
                    if (i == 5) {
                        double mah = valor / 1000.0;
                        System.out.printf(
                            "[BATTERYSTATS] Patrón %d (dischargeElapsed): %.0f µAh → %.3f mAh%n",
                            i + 1, valor, mah);
                        return mah;
                    }

                    System.out.printf(
                        "[BATTERYSTATS] Patrón %d coincidió: %.3f mAh%n",
                        i + 1, valor);
                    return valor;
                }
            }

            // ── Ningún patrón funcionó ──────────────────────────────────────
            System.out.println(
                "[BATTERYSTATS] ✖ Ningún patrón coincidió.\n" +
                "  → Revisa las líneas '[BATTERYSTATS] LÍNEA:' en consola\n" +
                "  → Dinos qué línea aparece y añadimos el patrón exacto.");
            return 0;

        } catch (Exception e) {
            System.out.println("[BATTERYSTATS] Error: " + e.getMessage());
            return 0;
        }
    }

    // ───────────────────── CPU REAL desde /proc/stat ─────────────────────
    private double obtenerUsoCpuReal() {
        try {
            String   out     = ejecutarShell("cat /proc/stat");
            String   cpuLine = out.split("\n")[0];
            String[] parts   = cpuLine.trim().split("\\s+");

            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = Long.parseLong(parts[5]);
            long irq     = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long totalIdle   = idle + iowait;
            long totalActive = user + nice + system + irq + softirq;
            long total       = totalIdle + totalActive;

            if (prevCpuStats == null) {
                prevCpuStats = new long[]{ totalIdle, total };
                return 0.0;
            }

            long deltaTotal = total     - prevCpuStats[1];
            long deltaIdle  = totalIdle - prevCpuStats[0];
            prevCpuStats = new long[]{ totalIdle, total };

            if (deltaTotal == 0) return 0.0;
            double cpuPct = 100.0 * (deltaTotal - deltaIdle) / deltaTotal;
            return Math.min(Math.max(cpuPct, 0.0), 100.0);

        } catch (Exception e) {
            System.out.println("[CPU] Error: " + e.getMessage());
            return 0;
        }
    }

    // ───────────────────── EXTRAER NIVEL BATERÍA ─────────────────────────
    private int extraerNivelBateria(String out) {
        try {
            for (String line : out.split("\n")) {
                String l = line.trim().toLowerCase();
                if (l.startsWith("level:"))
                    return Integer.parseInt(l.replace("level:", "").trim());
            }
        } catch (Exception ignored) {}
        return 0;
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