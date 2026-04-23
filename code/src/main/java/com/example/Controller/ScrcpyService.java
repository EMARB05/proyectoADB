package com.example.Controller;


import java.io.File;
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
    stop();

    try {
        // 'user.dir' obtiene la carpeta desde donde se ejecuta tu .exe o .jar
        String baseDir = System.getProperty("user.dir");
        
        // Construimos la ruta apuntando a la carpeta externa
        File scrcpyFile = new File(baseDir, "scrcpy-win64-v3.3.4/scrcpy.exe");
        String scrcpyPath = scrcpyFile.getAbsolutePath();

        // Verificación de seguridad (opcional pero muy recomendada)
        if (!scrcpyFile.exists()) {
            System.err.println("ERROR: No se encuentra scrcpy en " + scrcpyPath);
            return; 
        }

        ProcessBuilder pb = new ProcessBuilder(
            scrcpyPath,
            "-s", serialNumber,
            "--window-title", "Dispositivo: " + serialNumber,
            "--stay-awake"
        );

        pb.redirectErrorStream(true);
        scrcpyProcess = pb.start();

    } catch (IOException e) {
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