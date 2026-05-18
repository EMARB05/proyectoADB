package com.example.Model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public abstract class AdbCallSupport {

    protected static String safeFilename(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    protected String ejecutarShellEnSerial(String serial, String shellCmd) {
        try {
            String adb = "adb";
            String adbDir = System.getProperty("aea.adb.path");
            if (adbDir != null && !adbDir.isBlank()) {
                adb = adbDir + java.io.File.separator + "adb.exe";
            }

            ProcessBuilder pb = new ProcessBuilder(adb, "-s", serial, "shell", shellCmd);
            pb.redirectErrorStream(true);
            Process proceso = pb.start();

            StringBuilder salida = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    salida.append(line).append("\n");
                }
            }

            proceso.waitFor(10, TimeUnit.SECONDS);
            return salida.toString().trim();
        } catch (Exception e) {
            System.out.println("[ADB] Error en " + serial + ": " + e.getMessage());
            return "";
        }
    }

    protected void marcarNumero(String serial, String numero) throws InterruptedException {
        for (char c : numero.toCharArray()) {
            String keyevent = switch (c) {
                case '0' -> Entradas.cero();
                case '1' -> Entradas.uno();
                case '2' -> Entradas.dos();
                case '3' -> Entradas.tres();
                case '4' -> Entradas.cuatro();
                case '5' -> Entradas.cinco();
                case '6' -> Entradas.seis();
                case '7' -> Entradas.siete();
                case '8' -> Entradas.ocho();
                case '9' -> Entradas.nueve();
                case '*' -> Entradas.asterisco();
                case '#' -> Entradas.almohadilla();
                default -> null;
            };
            if (keyevent != null) {
                ejecutarShellEnSerial(serial, keyevent);
                Thread.sleep(400);
            }
        }
    }

    protected boolean llamadaActiva(String serial) {
        String out = ejecutarShellEnSerial(serial, "dumpsys telephony.registry");
        return out.contains("mCallState=2") || out.contains("mCallState=1");
    }

    protected boolean esperarHastaQueSuene(String receptorSerial, int maxSegundos) {
        for (int i = 0; i < maxSegundos; i++) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            String out = ejecutarShellEnSerial(receptorSerial, "dumpsys telephony.registry");
            if (out.contains("mCallState=1")) {
                return true;
            }
        }
        return false;
    }

    protected void despertarDispositivo(String serial) {
        ejecutarShellEnSerial(serial, "input keyevent KEYCODE_WAKEUP");
        ejecutarShellEnSerial(serial, "wm dismiss-keyguard");
    }
}