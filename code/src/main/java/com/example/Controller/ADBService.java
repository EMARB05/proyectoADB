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

    // Ejecuta cualquier comando ADB y devuelve la salida línea a línea
    private List<String> ejecutarComando(String... comando) throws IOException {
        List<String> resultado = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true); // mezcla stdout y stderr
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

    // Devuelve los seriales de los dispositivos conectados y autorizados
    public List<String> obtenerDispositivosConectados() throws IOException {
        List<String> seriales = new ArrayList<>();
        List<String> salida = ejecutarComando("adb", "devices");

        for (String linea : salida) {
            // Filtramos la cabecera y las líneas que no sean dispositivos autorizados
            if (linea.isEmpty() || linea.startsWith("List of devices"))
                continue;
            if (linea.contains("unauthorized"))
                continue;
            if (linea.contains("offline"))
                continue;

            // Cada línea válida tiene formato: "SERIAL\tdevice"
            if (linea.contains("\tdevice")) {
                String serial = linea.split("\t")[0].trim();
                seriales.add(serial);
            }
        }

        return seriales;
    }

    // Obtiene el valor de una propiedad concreta del dispositivo
    // Ejemplo: getprop("R5CT103ABCD", "ro.product.model") → "Galaxy S22"
   public String getProp(String serial, String propiedad) {
    try {
        List<String> salida = ejecutarComando("adb", "-s", serial, "shell", "getprop", propiedad);
        if (salida != null && !salida.isEmpty()) {
            String valor = salida.get(0).trim();
            return valor.isEmpty() ? "Desconocido" : valor;
        }
    } catch (IOException e) {
        System.err.println("Error leyendo propiedad " + propiedad + ": " + e.getMessage());
    }
    return "N/A";
}

    // Construye un Dispositivo con los datos leídos directamente por ADB
    // Si el serial no está en la BBDD, este objeto se usa para pre-rellenar el
    // formulario de alta
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
        modelo.setPantallaPulgadas(""); // ADB no expone esto directamente

        return new Dispositivo(modelo, serial);
    }

    // La RAM no es una prop directa, se lee de /proc/meminfo
    private double obtenerRamTotalGb(String serial) throws IOException {
        List<String> salida = ejecutarComando(
                "adb", "-s", serial, "shell", "cat", "/proc/meminfo");

        for (String linea : salida) {
            if (linea.startsWith("MemTotal:")) {
                // Formato: "MemTotal: 7823456 kB"
                String[] partes = linea.split("\\s+");
                if (partes.length >= 2) {
                    long kb = Long.parseLong(partes[1]);
                    return kb / 1024.0 / 1024.0;
                }
            }
        }
        return 0;
    }
}
