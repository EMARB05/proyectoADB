package com.example.Controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class AdbExecutor {
    private static final long DEFAULT_TIMEOUT_SECONDS = 10L;
    private static final ExecutorService ADB_POOL = new ThreadPoolExecutor(
            2,
            4,
            30L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(32),
            runnable -> {
                Thread thread = new Thread(runnable, "adb-worker");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    private AdbExecutor() {
    }

    public static CompletableFuture<ADBService.EjecucionADB> ejecutarAsync(String... comando) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ejecutarConTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS, comando);
            } catch (IOException e) {
                return new ADBService.EjecucionADB(-1, List.of(e.getMessage() == null ? "" : e.getMessage()));
            }
        }, ADB_POOL);
    }

    public static ADBService.EjecucionADB ejecutar(String... comando) throws IOException {
        return ejecutarConTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS, comando);
    }

    public static ADBService.EjecucionADB ejecutarConTimeout(long timeout, TimeUnit unit, String... comando) throws IOException {
        String[] comandoFinal = normalizarAdb(comando);

        ProcessBuilder pb = new ProcessBuilder(comandoFinal);
        pb.redirectErrorStream(true);
        Process proceso = pb.start();

        List<String> lineas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                lineas.add(linea);
            }
        }

        int exitCode;
        try {
            if (!proceso.waitFor(timeout, unit)) {
                proceso.destroyForcibly();
                return new ADBService.EjecucionADB(-1, List.of("ADB timeout after " + timeout + " " + unit.name().toLowerCase()));
            }
            exitCode = proceso.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proceso.destroyForcibly();
            exitCode = -1;
        } finally {
            proceso.destroy();
        }

        return new ADBService.EjecucionADB(exitCode, lineas);
    }

    private static String[] normalizarAdb(String... comando) {
        if (comando.length > 0 && "adb".equals(comando[0])) {
            String adbDir = System.getProperty("aea.adb.path");
            if (adbDir != null && !adbDir.isBlank()) {
                String[] ajustado = comando.clone();
                ajustado[0] = adbDir + File.separator + "adb.exe";
                return ajustado;
            }
        }
        return comando;
    }
}