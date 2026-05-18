package com.example.Model;

public class LlamadasD17 extends AdbCallSupport {

    private final String serial;

    public LlamadasD17(String serial) {
        this.serial = serial;
    }

    // ─── HOLD / RETRIEVE ─────────────────────────────────────────────────────
    public boolean ejecutarHold(PerfilDialer perfil) {
        try {
            if (!llamadaActiva(serial)) return false;

            if (perfil.tieneComandos()) {
                ejecutarShell(perfil.getCmdHold());
            } else {
                if (perfil.getXHold() <= 0 || perfil.getYHold() <= 0) {
                    return false;
                }
                // Si el dispositivo tiene botón "Mostrar más" (coordenadas > 0), abrirlo primero
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
                // Para dispositivos táctiles: solo tocar el mismo botón nuevamente para reanudar
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
            if (!llamadaActiva(serial)) return false;

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
        if (numero == null) return false;
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
            if (!sono) return false;
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
        if (numero == null) return false;
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
        if (numero == null) return false;
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

    // Usa this.serial
    private String ejecutarShell(String shellCmd) {
        return ejecutarShellEnSerial(serial, shellCmd);
    }
}