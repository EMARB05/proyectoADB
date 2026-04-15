package com.example.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.example.Model.Dispositivo;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;

public class ADBService {

    /**
     * MÉTODO MOTOR (Privado): Es el único que realmente toca el ProcessBuilder.
     * Recibe un array de strings y devuelve la salida del comando.
     */
    private List<String> ejecutarADB(String... comando) throws IOException {
        List<String> resultado = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);
        Process proceso = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                resultado.add(linea);
            }
        }
        return resultado;
    }

    /**
     * MÉTODO PARA EL CONTROLADOR (Público): No bloquea la UI (usa hilos).
     * Se usa así: adbService.ejecutarAccionHilo(serial, "shell settings put...");
     */
    public String ejecutarAccionHilo(String serial, String comandoShell) {
        new Thread(() -> {
            try {
                String[] partes = comandoShell.split(" ");
                List<String> fullCmd = new ArrayList<>();
                fullCmd.add("adb");
                fullCmd.add("-s");
                fullCmd.add(serial);

                for (String p : partes) {
                    fullCmd.add(p);
                }

                // Llamamos al motor
                ejecutarADB(fullCmd.toArray(new String[0]));
                System.out.println("ADB Ejecutado en hilo: " + comandoShell);

            } catch (IOException e) {
                System.err.println("Error en hilo ADB: " + e.getMessage());
            }
        }).start();
        return comandoShell;
    }

    /**
     * MÉTODO SÍNCRONO: Se usa para obtener datos (GET).
     * Bloquea el hilo actual hasta que ADB responde.
     */
    public String ejecutarComandoSincrono(String serial, String comandoShell) {
        try {
            String[] partes = comandoShell.split(" ");
            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("adb");
            fullCmd.add("-s");
            fullCmd.add(serial);
            for (String p : partes)
                fullCmd.add(p);

            List<String> salida = ejecutarADB(fullCmd.toArray(new String[0]));

            // Retornamos la primera línea de la respuesta (ej: "1") o vacío si no hay nada
            if (salida != null && !salida.isEmpty()) {
                return String.join("\n", salida).trim();
            }

        } catch (IOException e) {
            System.err.println("Error ADB Síncrono: " + e.getMessage());

        }
        return "";
    }

    // --- MÉTODOS DE OBTENCIÓN DE DATOS (Sincrónicos) ---

    public List<String> obtenerDispositivosConectados() throws IOException {
        List<String> seriales = new ArrayList<>();
        List<String> salida = ejecutarADB("adb", "devices"); // Usamos el motor

        for (String linea : salida) {
            if (linea.isEmpty() || linea.startsWith("List of devices"))
                continue;
            if (linea.contains("unauthorized") || linea.contains("offline"))
                continue;

            if (linea.contains("\tdevice")) {
                seriales.add(linea.split("\t")[0].trim());
            }
        }
        return seriales;
    }



    public String getProp(String serial, String propiedad) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "getprop", propiedad);
        return salida.isEmpty() ? "" : salida.get(0).trim();
    }

    public String getSecureSetting(String serial, String setting) throws IOException {
    // Nota: Aquí quitamos "getprop" y usamos "settings get secure"
    List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "settings", "get", "secure", setting);
    return salida.isEmpty() ? "" : salida.get(0).trim();
}

    public Dispositivo obtenerProps(String serial) throws IOException {
        Marca marca = new Marca();
        marca.setNombre(getProp(serial, "ro.product.manufacturer"));

        Soc soc = new Soc();
        soc.setModeloSoc(getProp(serial, "ro.board.platform"));
        soc.setFabricante(getProp(serial, "ro.product.manufacturer"));



        Modelo modelo = new Modelo(marca, getProp(serial, "ro.product.model"));
        modelo.setSoc(soc);
        modelo.setSoVersion("Android " + getProp(serial, "ro.build.version.release"));
        modelo.setRamGb((int) Math.round(obtenerRamTotalGb(serial)));
        modelo.setAlmacenamientoGb((int) Math.round(obtenerAlmacenamientoTotalGb(serial)));

        String android_id= getSecureSetting(serial, "android_id");

        return new Dispositivo(modelo, serial, android_id);
    }

    

    private double obtenerRamTotalGb(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "cat", "/proc/meminfo");
        for (String linea : salida) {
            if (linea.startsWith("MemTotal:")) {
                String[] partes = linea.split("\\s+");
                if (partes.length >= 2) {
                    return Long.parseLong(partes[1]) / 1024.0 / 1024.0;
                }
            }
        }
        return 0;
    }

    private int obtenerAlmacenamientoTotalGb(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "df", "-k", "/data");
        for (String linea : salida) {
            String limpia = linea.trim();
            if (limpia.startsWith("Filesystem") || limpia.isEmpty())
                continue;

            String[] partes = limpia.split("\\s+");
            try {
                long kbTotal = (partes.length >= 6) ? Long.parseLong(partes[1]) : Long.parseLong(partes[0]);
                double gbDetectados = kbTotal / 1024.0 / 1024.0;
                // (Normalización)
                int[] capacidadesComerciales = { 8, 16, 32, 64, 128, 256, 512, 1024 };

                for (int capacidad : capacidadesComerciales) {
                    if (gbDetectados > (capacidad * 0.65) && gbDetectados <= capacidad) {
                        return capacidad;
                    }
                }
                return (int) Math.round(gbDetectados);
            } catch (Exception e) {
                continue;
            }
        }
        return 0;
    }

    public boolean isModoAvionActivo(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "settings", "get", "global",
                "airplane_mode_on");
        return !salida.isEmpty() && "1".equals(salida.get(0).trim());
    }
}
