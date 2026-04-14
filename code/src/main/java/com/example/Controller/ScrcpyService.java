package com.example.Controller;


import java.io.IOException;
import java.net.URISyntaxException;

public class ScrcpyService {

    // Ruta al ejecutable de scrcpy (ajústala a tu proyecto)
    
    private Process scrcpyProcess;

    /**
     * Lanza scrcpy para el dispositivo con el serial indicado.
     * @param serialNumber El serial del dispositivo (obtenido con ADBService)
     */
    public void launch(String serialNumber) {
        // Si ya hay una instancia corriendo, la cerramos primero
        stop();

      try {
        // Obtiene la ruta absoluta real desde el classpath
        String scrcpyPath = getClass()
            .getResource("/scrcpy-win64-v3.3.4/scrcpy.exe")
            .toURI()
            .getPath();

        ProcessBuilder pb = new ProcessBuilder(
            scrcpyPath,
            "-s", serialNumber,
            "--window-title", "Dispositivo: " + serialNumber,
            "--stay-awake"
        );

        pb.redirectErrorStream(true);
        scrcpyProcess = pb.start();

    } catch (IOException | URISyntaxException e) {
        System.err.println("Error al lanzar scrcpy: " + e.getMessage());
    }
    }

    /**
     * Cierra la ventana de scrcpy si está abierta.
     */
    public void stop() {
        if (scrcpyProcess != null && scrcpyProcess.isAlive()) {
            scrcpyProcess.destroy();
            System.out.println("scrcpy cerrado.");
        }
    }

    /**
     * Comprueba si scrcpy está corriendo.
     */
    public boolean isRunning() {
        return scrcpyProcess != null && scrcpyProcess.isAlive();
    }
}