package com.example.Controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AdbManager {
    public static void inicializar() {
        try {
            // Extrae adb a carpeta temporal
            Path tempDir = Files.createTempDirectory("aea_suite_adb");
            tempDir.toFile().deleteOnExit();

            String[] archivos = {"adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll"};
            for (String archivo : archivos) {
                try (InputStream is = AdbManager.class.getResourceAsStream("/adb/" + archivo)) {
                    if (is != null) {
                        Path destino = tempDir.resolve(archivo);
                        Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
                        destino.toFile().deleteOnExit();
                    }
                }
            }

            // Añade la carpeta temporal al PATH del proceso actual
            // así "adb" se resuelve solo, sin rutas absolutas en ningún sitio
            String pathActual = System.getenv("PATH");
            String nuevoPath = tempDir.toAbsolutePath() + File.pathSeparator + pathActual;

            // Guardamos el PATH modificado para usarlo en ProcessBuilder
            System.setProperty("aea.adb.path", tempDir.toAbsolutePath().toString());
            System.out.println("[ADB] ADB embebido listo en: " + tempDir);

        } catch (IOException e) {
            System.out.println("[ADB] Usando ADB del sistema");
        }
    }
}
