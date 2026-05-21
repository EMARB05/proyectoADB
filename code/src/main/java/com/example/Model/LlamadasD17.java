package com.example.Model;

import java.util.List;
import java.util.function.Supplier;

public class LlamadasD17 extends AdbCallSupport {

    private static final long CALL_DURATION_MS = 36_000L;
    private static final long CALL_WARNING_MS = 120_000L;
    private static final long AUTO_HANGUP_TIMEOUT_MS = 180_000L;

    private final String serial;

    public LlamadasD17(String serial) {
        this.serial = serial;
    }

    public static List<BloquePrueba> crearBloquesCallTimer(String modelo) {
        String modeloNormalizado = modelo == null ? "" : modelo.trim().toUpperCase();

        // Hoy solo soportamos el menú del D17. Si mañana cambia por modelo,
        // este switch nos deja aislar la variante sin tocar el controller.
        if (modeloNormalizado.contains("D17")) {
            return crearBloquesCallTimerD17();
        }

        // Fallback: usamos la misma secuencia que D17 hasta que haya otra variante.
        return crearBloquesCallTimerD17();
    }

    public static List<BloquePrueba> crearBloquesFmRadio(String modelo) {
        String modeloNormalizado = modelo == null ? "" : modelo.trim().toUpperCase();

        if (modeloNormalizado.contains("D17")) {
            return crearBloquesFmRadioD17();
        }

        return crearBloquesFmRadioD17();
    }

    private static List<BloquePrueba> crearBloquesCallTimerD17() {
        return List.of(
                new BloquePrueba("SOFT.046.001", "Enable call timer",
                        "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS && sleep 2 && input keyevent 20 && input keyevent 20 && input keyevent 20 && input keyevent 20 && input keyevent 20 && input keyevent 20 && input keyevent 20 && input keyevent 23 && sleep 1 && input keyevent 23"),

                new BloquePrueba("SOFT.046.002", "Configure notification time",
                        "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS && sleep 2 && input keyevent 20 && input keyevent 20 && sleep 1 && input keyevent 23 && input keyevent 20 && input keyevent 22 && input keyevent 23"),

                new BloquePrueba("SOFT.046.003", "Check if during a call notification is shown",
                        "__MO_CALL_DURATION_CHECK__"),

                new BloquePrueba("SOFT.046.004", "Configure call timer total duration",
                        "shell input keyevent 20 && input keyevent 20 && input keyevent 23 && sleep 1 && sleep 1 && input keyevent 67 && sleep 1 && input text 2 && sleep 1 && input keyevent 20 && input keyevent 22 && input keyevent 23"),

                new BloquePrueba("SOFT.046.005",
                        "Check if during a call DUT warns when the call timer limit is reached",
                        "__MO_CALL_LIMIT_WARN_CHECK__", true),

                new BloquePrueba("SOFT.046.006", "Enable auto hang up option",
                        "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS && sleep 2 && input keyevent 20 && input keyevent 20 && input keyevent 23"),

                new BloquePrueba("SOFT.046.007",
                        "Check if during a call DUT auto hang up the call when the call timer limit is reached",
                        "__MO_CALL_AUTO_HANGUP_CHECK__", true),

                new BloquePrueba("SOFT.046.008", "Reset consumption time",
                        "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS && sleep 2 && input keyevent 19 && input keyevent 19 && input keyevent 19 && input keyevent 19 && input keyevent 23"));
    }

    private static List<BloquePrueba> crearBloquesFmRadioD17() {
        return List.of(
            new BloquePrueba("SOFT.017.001", "Auto search of radio stations with earphone connected",
                "__FM_D17_EARPHONE_SEQUENCE__"),

            new BloquePrueba("SOFT.017.002", "Check sound quality of the stations found",
                "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

            new BloquePrueba("SOFT.017.003", "Change audio output between earphone/speaker",
                "__FM_D17_EARPHONE_SEQUENCE__"),

            new BloquePrueba("SOFT.017.004", "Search any radio station manually",
                "__FM_D17_RIGHT_OK__"),

            new BloquePrueba("SOFT.017.005", "Auto search of radio stations without earphone connected",
                "__FM_D17_AUTOSEARCH_NO_EARPHONE__"),

        new BloquePrueba("SOFT.017.006", "Check sound quality of the stations found without earphone connected",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.007", "Check if RDS information is shown (name of radio station...)",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.008", "Add a radio station to favourite",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.009", "Change name of favourite radio station",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.010", "Remove favourite radio station",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.011",
            "Check if after a new auto search favourite stations saved are removed",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.012", "Start a radio recording and save it",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

        new BloquePrueba("SOFT.017.013", "Listen the radio recording",
            "shell am start -a android.intent.action.VIEW -d /sdcard/Music/ -t audio/*", true),

        new BloquePrueba("SOFT.017.014", "Delete the radio recording",
            "shell am start -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity", true),

            new BloquePrueba("SOFT.017.015", "Check if it's possible to listen radio FM in background",
                "__FM_BACKGROUND__"),

            new BloquePrueba("SOFT.017.016",
                "While radio FM playing, receive a call. Check if after call ends radio FM continues playing",
                "__FM_LLAMADA_ENTRANTE__"));
    }

    // ─── HOLD / RETRIEVE ─────────────────────────────────────────────────────
    public boolean ejecutarHold(PerfilDialer perfil) {
        try {
            if (!llamadaActiva(serial))
                return false;

            if (perfil.tieneComandos()) {
                ejecutarShell(perfil.getCmdHold());
            } else {
                if (perfil.getXHold() <= 0 || perfil.getYHold() <= 0) {
                    return false;
                }
                // Si el dispositivo tiene botón "Mostrar más" (coordenadas > 0), abrirlo
                // primero
                if (perfil.getXMostrarMas() > 0 && perfil.getYMostrarMas() > 0) {
                    ejecutarShell("input tap " + perfil.getXMostrarMas() + " " + perfil.getYMostrarMas());
                    Thread.sleep(800);
                }
                // Tocar el botón "Retener"
                ejecutarShell("input tap " + perfil.getXHold() + " " + perfil.getYHold());
            }
            Thread.sleep(5_000);

            if (perfil.tieneComandos()) {
                ejecutarShell(perfil.getCmdRetrieve());
            } else {
                // Para dispositivos táctiles: solo tocar el mismo botón nuevamente para
                // reanudar
                // NO reabrimos "Mostrar más" (es un toggle que cierraría el menú)
                // El menú debería estar abierto desde el hold
                ejecutarShell("input tap " + perfil.getXHold() + " " + perfil.getYHold());
            }
            Thread.sleep(1_000);
            return llamadaActiva(serial);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ─── MUTE / UNMUTE ────────────────────────────────────────────────────────
    public boolean ejecutarMute(PerfilDialer perfil) {
        try {
            if (!llamadaActiva(serial))
                return false;

            if (perfil.tieneComandos()) {
                ejecutarShell(perfil.getCmdMute());
            } else {
                if (perfil.getXMute() <= 0 || perfil.getYMute() <= 0) {
                    return false;
                }
                ejecutarShell("input tap " + perfil.getXMute() + " " + perfil.getYMute());
            }
            Thread.sleep(3_000);

            if (perfil.tieneComandos()) {
                ejecutarShell(perfil.getCmdUnmute());
            } else {
                ejecutarShell("input tap " + perfil.getXMute() + " " + perfil.getYMute());
            }
            Thread.sleep(1_000);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ─── CONFERENCIA ─────────────────────────────────────────────────────────
    public boolean ejecutarConferencia(String numero, String receptorSerial) {
        if (numero == null)
            return false;
        try {
            // 1. softizq → abajo 3x → ok
            ejecutarShell(Entradas.softizq());
            Thread.sleep(800);
            for (int i = 0; i < 3; i++) {
                ejecutarShell(Entradas.abajo());
                Thread.sleep(400);
            }
            ejecutarShell(Entradas.ok());
            Thread.sleep(800);

            // 2. Marcar número
            marcarNumero(serial, numero);

            // 3. Llamar con verde
            ejecutarShell(Entradas.verde());
            Thread.sleep(1_000);

            // 4. Esperar que suene y contestar
            boolean sono = esperarHastaQueSuene(receptorSerial, 20);
            if (!sono)
                return false;
            despertarDispositivo(receptorSerial);
            Thread.sleep(500);
            ejecutarShellEnSerial(receptorSerial, "input keyevent KEYCODE_CALL");
            Thread.sleep(2_000);

            // 5. softizq → abajo 2x → ok para fusionar
            ejecutarShell(Entradas.softizq());
            Thread.sleep(800);
            for (int i = 0; i < 2; i++) {
                ejecutarShell(Entradas.abajo());
                Thread.sleep(400);
            }
            ejecutarShell(Entradas.ok());
            Thread.sleep(2_000);

            System.out.println("[CONF] ✔ Conferencia establecida");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ─── TRANSFERENCIA CONSULTIVA ─────────────────────────────────────────────
    public boolean ejecutarTransferencia(String numero, String receptorSerial) {
        if (numero == null)
            return false;
        try {
            ejecutarShell(Entradas.softizq());
            Thread.sleep(600);
            for (int i = 0; i < 3; i++) {
                ejecutarShell(Entradas.abajo());
                Thread.sleep(400);
            }
            ejecutarShell(Entradas.ok());
            Thread.sleep(800);

            marcarNumero(serial, numero);

            ejecutarShell(Entradas.verde());
            Thread.sleep(1_400);

            if (receptorSerial != null && !receptorSerial.isBlank()) {
                boolean sono = esperarHastaQueSuene(receptorSerial, 20);
                if (sono) {
                    despertarDispositivo(receptorSerial);
                    Thread.sleep(500);
                    ejecutarShellEnSerial(receptorSerial, "input keyevent KEYCODE_CALL");
                    Thread.sleep(1_000);
                }
            } else {
                int intentos = 0;
                while (intentos < 20 && !llamadaActiva(serial)) {
                    Thread.sleep(500);
                    intentos++;
                }
                Thread.sleep(600);
            }

            ejecutarShell(Entradas.softizq());
            Thread.sleep(900);
            for (int i = 0; i < 4; i++) {
                ejecutarShell(Entradas.abajo());
                Thread.sleep(800);
            }
            ejecutarShell(Entradas.ok());
            Thread.sleep(3_000);

            System.out.println("[TRANSFER] ✔ Transferencia ejecutada");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ─── TRANSFERENCIA CIEGA ─────────────────────────────────────────────────
    public boolean ejecutarTransferenciaCiega(String numero) {
        if (numero == null)
            return false;
        try {
            ejecutarShell(Entradas.softizq());
            Thread.sleep(800);
            for (int i = 0; i < 4; i++) {
                ejecutarShell(Entradas.abajo());
                Thread.sleep(400);
            }
            ejecutarShell(Entradas.ok());
            Thread.sleep(800);

            marcarNumero(serial, numero);

            ejecutarShell(Entradas.abajo());
            Thread.sleep(400);
            ejecutarShell(Entradas.derecha());
            Thread.sleep(400);
            ejecutarShell(Entradas.ok());
            Thread.sleep(3_000);

            System.out.println("[TRANSFER_CIEGA] ✔ Transferencia ciega ejecutada");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean activarCallTimer() {
        return ejecutarSecuencia(
                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                "sleep 2",
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.ok(),
                "sleep 1",
                Entradas.ok());
    }

    public boolean configurarCallTimerAdvertencia() {
        return ejecutarSecuencia(
                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                "sleep 2",
                Entradas.abajo(),
                Entradas.abajo(),
                "sleep 1",
                Entradas.ok(),
                Entradas.abajo(),
                Entradas.derecha(),
                Entradas.ok());
    }

    public boolean configurarCallTimerDuracion() {
        return ejecutarSecuencia(
                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                "sleep 2",
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.ok(),
                "sleep 1",
                "input keyevent KEYCODE_SOFT_RIGHT",
                "sleep 1",
                "input keyevent 67",
                "sleep 1",
                "input text 2",
                "sleep 1",
                Entradas.abajo(),
                Entradas.derecha(),
                Entradas.ok());
    }

    public boolean habilitarAutoHangup() {
        return ejecutarSecuencia(
                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                "sleep 2",
                Entradas.abajo(),
                Entradas.abajo(),
                Entradas.ok());
    }

    public boolean resetearTiempoConsumo() {
        return ejecutarSecuencia(
                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                "sleep 2",
                Entradas.arriba(),
                Entradas.arriba(),
                Entradas.arriba(),
                Entradas.arriba(),
                Entradas.ok());
    }

    public boolean ejecutarCallTimerDurationCheck(String numero, Supplier<Boolean> confirmacion) {
        if (numero == null || numero.isBlank())
            return false;
        try {
            ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:" + numero);
            Thread.sleep(CALL_DURATION_MS);
            boolean ok = confirmacion != null && Boolean.TRUE.equals(confirmacion.get());
            colgarLlamadaEnCurso();
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            colgarLlamadaEnCurso();
            return false;
        }
    }

    public boolean ejecutarCallLimitWarnCheck(String numero, Supplier<Boolean> confirmacion) {
        if (numero == null || numero.isBlank())
            return false;
        try {
            ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:" + numero);
            Thread.sleep(CALL_WARNING_MS);
            boolean ok = confirmacion != null && Boolean.TRUE.equals(confirmacion.get());
            colgarLlamadaEnCurso();
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            colgarLlamadaEnCurso();
            return false;
        }
    }

    public boolean ejecutarCallAutoHangupCheck(String numero, Supplier<Boolean> confirmacion) {
        if (numero == null || numero.isBlank())
            return false;
        try {
            ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:" + numero);

            long waited = 0L;
            long interval = 2_000L;
            while (waited < AUTO_HANGUP_TIMEOUT_MS) {
                if (!llamadaActiva(serial)) {
                    return confirmacion != null && Boolean.TRUE.equals(confirmacion.get());
                }
                Thread.sleep(interval);
                waited += interval;
            }

            colgarLlamadaEnCurso();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            colgarLlamadaEnCurso();
            return false;
        }
    }

    public void restablecerPhoneApp() {
        ejecutarShellEnSerial(serial, "pm clear com.android.phone");
    }

    public void colgarLlamadaEnCurso() {
        System.out.println("[CALL TIMER] Estado inicial llamadaActiva=" + llamadaActiva(serial));
        for (int intento = 1; intento <= 4; intento++) {
            System.out.println("[CALL TIMER] Intento " + intento + " de colgar");
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_ENDCALL");
            ejecutarShellEnSerial(serial, "input keyevent 6");
            ejecutarShellEnSerial(serial, "input keyevent 79");
            ejecutarShellEnSerial(serial, "input keyevent 26");

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            boolean activo = llamadaActiva(serial);
            System.out.println("[CALL TIMER] Después intento " + intento + " llamadaActiva=" + activo);
            if (!activo) {
                System.out.println("[CALL TIMER] Llamada colgada correctamente en intento " + intento);
                return;
            }
        }

        System.out.println("[CALL TIMER] No se pudo colgar la llamada tras varios intentos.");
    }

    // Usa this.serial
    private String ejecutarShell(String shellCmd) {
        return ejecutarShellEnSerial(serial, shellCmd);
    }

    private boolean ejecutarSecuencia(String... comandos) {
        try {
            for (String comando : comandos) {
                if (comando == null || comando.isBlank()) {
                    continue;
                }
                if (comando.startsWith("sleep ")) {
                    String[] partes = comando.split("\\s+");
                    long segundos = Long.parseLong(partes[1]);
                    Thread.sleep(segundos * 1000L);
                } else {
                    ejecutarShell(comando);
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.out.println("[CALL TIMER] Error en secuencia: " + e.getMessage());
            return false;
        }
    }
}