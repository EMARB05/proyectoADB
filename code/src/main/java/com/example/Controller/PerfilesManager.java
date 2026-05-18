package com.example.Controller;

import com.example.Model.Entradas;
import com.example.Model.PerfilDialer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerfilesManager {

    private static final Map<String, PerfilDialer> PERFILES = new HashMap<>();

    static {
        // COCOM F730 — feature phone con teclas físicas
        // softizq(82) = menú, abajo(20)x2 = navegar hasta Hold, ok(23) = seleccionar
        PERFILES.put("F730", new PerfilDialer(
                "COCOM F730",
                // hold: menú → abajo → abajo → ok (3ª opción = Retener)
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                // retrieve: back → menú → abajo → abajo → ok
                Entradas.secuencia(Entradas.softder(), Entradas.unSegundo(),
                        Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                // mute: menú → abajo → ok (2ª opción = Silenciar)
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                // unmute: igual que mute
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok())));

        // COCOM D17 — feature phone con teclas físicas
        PERFILES.put("D17", new PerfilDialer(
                "COCOM D17",
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softder(), Entradas.unSegundo(),
                        Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok())));

        // COCOM F780 — mismos keycodes que F730
        PERFILES.put("F780", new PerfilDialer(
                "COCOM F780",
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softder(), Entradas.unSegundo(),
                        Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok()),
                Entradas.secuencia(Entradas.softizq(), Entradas.unSegundo(),
                        Entradas.abajo(), Entradas.unSegundo(),
                        Entradas.ok())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PÚBLICOS
    // ─────────────────────────────────────────────────────────────────────────

    public static PerfilDialer obtenerPerfil(String serial) {
        String modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
        System.out.println("[PERFIL] Modelo detectado: " + modelo);

        if (PERFILES.containsKey(modelo)) {
            System.out.println("[PERFIL] Perfil encontrado para: " + modelo);
            return PERFILES.get(modelo);
        }

        // No hay perfil — lanzar calibración automática
        System.out.println("[PERFIL] Sin perfil — iniciando calibración...");
        return calibrarNuevoModelo(serial, modelo);
    }

    public static PerfilDialer calibrarNuevoModelo(String serial, String modelo) {
        System.out.println("[CALIBRAR] Analizando UI de " + modelo + "...");

        String uiDump = ejecutarShellEnSerial(serial,
                "uiautomator dump /sdcard/ui_cal.xml && cat /sdcard/ui_cal.xml");

        int[] coordMute = extraerCoordsDeDesc(uiDump, "Silenciar");
        int[] coordMas = extraerCoordsDeDesc(uiDump, "Mostrar más");
        int[] coordHold = extraerCoordsDeDesc(uiDump, "Retener llamada");
        int[] coordTeclado = extraerCoordsDeDesc(uiDump, "Teclado");

        // Si no se encontraron botones, reintentar un par de veces (render puede tardar)
        int intentos = 0;
        while (intentos < 2 && (coordHold[0] == 0 || coordMute[0] == 0 || coordTeclado[0] == 0)) {
            try {
                Thread.sleep(700);
            } catch (InterruptedException ignored) {
            }
            uiDump = ejecutarShellEnSerial(serial,
                    "uiautomator dump /sdcard/ui_cal_retry.xml && cat /sdcard/ui_cal_retry.xml");
            if (coordMute[0] == 0) coordMute = extraerCoordsDeDesc(uiDump, "Silenciar");
            if (coordMas[0] == 0) coordMas = extraerCoordsDeDesc(uiDump, "Mostrar más");
            if (coordHold[0] == 0) coordHold = extraerCoordsDeDesc(uiDump, "Retener llamada");
            if (coordTeclado[0] == 0) coordTeclado = extraerCoordsDeDesc(uiDump, "Teclado");
            intentos++;
        }

        // Si Mute no se encuentra, intentar variantes
        if (coordMute[0] == 0) {
            System.out.println("[CALIBRAR] Silenciar no encontrado, intentando variantes...");
            coordMute = extraerCoordsDeDesc(uiDump, "Mute");
            if (coordMute[0] == 0) {
                coordMute = extraerCoordsDeDesc(uiDump, "Mudo");
            }
            if (coordMute[0] == 0) {
                coordMute = extraerCoordsDeDesc(uiDump, "Silencio");
            }
        }

        // Si Hold no está visible directamente, intentar abrir menú Más
        if (coordHold[0] == 0 && coordMas[0] > 0) {
            System.out.println("[CALIBRAR] Hold no visible en pantalla principal, abriendo menú Más...");
            ejecutarShellEnSerial(serial, "input tap " + coordMas[0] + " " + coordMas[1]);
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
            }

            uiDump = ejecutarShellEnSerial(serial,
                    "uiautomator dump /sdcard/ui_cal2.xml && cat /sdcard/ui_cal2.xml");
            coordHold = extraerCoordsDeDesc(uiDump, "Retener llamada");

            // Cerrar menú Más
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_BACK");
        }

        // Si Hold aún no se encuentra, intentar con variantes de nombres
        if (coordHold[0] == 0) {
            System.out.println("[CALIBRAR] Hold no encontrado, intentando variantes...");
            // Intenta buscar "Retener" (sin "llamada")
            coordHold = extraerCoordsDeDesc(uiDump, "Retener");
            if (coordHold[0] == 0) {
                // Samsung suele usar "Poner en espera"
                coordHold = extraerCoordsDeDesc(uiDump, "Poner en espera");
            }
            if (coordHold[0] == 0) {
                coordHold = extraerCoordsDeDesc(uiDump, "En espera");
            }
            if (coordHold[0] == 0) {
                // Intenta buscar "Pausa"
                coordHold = extraerCoordsDeDesc(uiDump, "Pausa");
            }
            if (coordHold[0] == 0) {
                // Intenta buscar "Hold"
                coordHold = extraerCoordsDeDesc(uiDump, "Hold");
            }
        }

        System.out.println("[CALIBRAR] Mostrar más: " + coordMas[0] + "," + coordMas[1]);
        System.out.println("[CALIBRAR] Hold: " + coordHold[0] + "," + coordHold[1]);
        System.out.println("[CALIBRAR] Mute: " + coordMute[0] + "," + coordMute[1]);
        System.out.println("[CALIBRAR] Teclado: " + coordTeclado[0] + "," + coordTeclado[1]);

        if (coordHold[0] == 0 || coordMute[0] == 0) {
            guardarDumpCalibracion(modelo, uiDump);
        }

        boolean esTactil = !ejecutarShellEnSerial(serial, "wm size")
                .contains("320x240");

        PerfilDialer perfil = new PerfilDialer(modelo, esTactil,
                coordMas[0], coordMas[1],
                coordHold[0], coordHold[1],
                coordMute[0], coordMute[1],
                coordTeclado[0], coordTeclado[1]);

        PERFILES.put(modelo, perfil);
        return perfil;
    }

    // Permite guardar un perfil manualmente desde la UI
    public static void guardarPerfil(PerfilDialer perfil) {
        if (perfil == null) return;
        PERFILES.put(perfil.getModelo(), perfil);
        System.out.println("[PERFILES] Perfil guardado manualmente para: " + perfil.getModelo());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS - Búsqueda de coordenadas
    // ─────────────────────────────────────────────────────────────────────────

    private static int[] extraerCoordsDeDesc(String uiDump, String desc) {
        int[] coords = extraerCoordsDeAtributo(uiDump, "content-desc", desc);
        if (coords[0] != 0 || coords[1] != 0) {
            return coords;
        }

        coords = extraerCoordsDeAtributo(uiDump, "text", desc);
        if (coords[0] != 0 || coords[1] != 0) {
            return coords;
        }

        return new int[] { 0, 0 };
    }

    private static int[] extraerCoordsDeAtributo(String uiDump, String atributo, String valor) {
        // Permitir coincidencias parciales: el atributo puede contener texto adicional
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                atributo + "=\"[^\"]*" + java.util.regex.Pattern.quote(valor) + "[^\"]*\"[^>]*bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");
        java.util.regex.Matcher m = p.matcher(uiDump);
        if (m.find()) {
            int x = (Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(3))) / 2;
            int y = (Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4))) / 2;
            return new int[] { x, y };
        }
        return new int[] { 0, 0 };
    }

    private static void guardarDumpCalibracion(String modelo, String uiDump) {
        try {
            if (uiDump == null || uiDump.isBlank()) {
                return;
            }

            String modeloSeguro = modelo.replaceAll("[^a-zA-Z0-9._-]", "_");
            java.nio.file.Path dir = java.nio.file.Path.of("tmp", "calibracion");
            Files.createDirectories(dir);

            java.nio.file.Path archivo = dir.resolve("ui_" + modeloSeguro + ".xml");
            Files.writeString(archivo, uiDump);
            System.out.println("[CALIBRAR] Dump guardado en: " + archivo.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("[CALIBRAR] No se pudo guardar dump: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS - Ejecución de comandos ADB
    // ─────────────────────────────────────────────────────────────────────────

    private static String ejecutarShellEnSerial(String serial, String shellCmd) {
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
}
