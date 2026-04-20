package com.example.Controller;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LogcatManager {
    private Process procesoLogcat;
    private Thread hiloLectura;
    private boolean activo = false;

    // Lista de líneas capturadas en memoria
    private final List<String> lineasCapturadas = new CopyOnWriteArrayList<>();

    // Callback que se llama cada vez que llega una línea nueva
    // Lo usará la UI para actualizar en tiempo real
    private Consumer<String> onNuevaLinea;

    public void setOnNuevaLinea(Consumer<String> callback) {
        this.onNuevaLinea = callback;
    }

    // Inicia la captura de logcat para un dispositivo
    public void iniciar(String serial) throws IOException {
        if (activo)
            return;
        activo = true;
        lineasCapturadas.clear();

        ProcessBuilder pb;

        pb = new ProcessBuilder(
                "adb", "-s", serial, "logcat",
                "-v", "time", "-T", "0");

        pb.redirectErrorStream(true);
        procesoLogcat = pb.start();

        hiloLectura = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(procesoLogcat.getInputStream()))) {
                String linea;
                while (activo && (linea = reader.readLine()) != null) {
                    lineasCapturadas.add(linea);
                    if (onNuevaLinea != null) {
                        final String lineaFinal = linea;
                        onNuevaLinea.accept(lineaFinal);
                    }
                }
            } catch (IOException e) {
                // Proceso detenido, es normal
            }
        });
        hiloLectura.setDaemon(true);
        hiloLectura.start();
    }

    public void detener() {
        activo = false;
        if (procesoLogcat != null)
            procesoLogcat.destroy();
        if (hiloLectura != null)
            hiloLectura.interrupt();
    }

    // Filtra las líneas capturadas por una palabra clave
    public List<String> filtrar(String palabraClave) {
        String clave = palabraClave.toLowerCase();
        return lineasCapturadas.stream()
                .filter(l -> l.toLowerCase().contains(clave))
                .collect(java.util.stream.Collectors.toList());
    }

    // Filtra por nivel de log: W=warning, E=error, F=fatal
    public List<String> filtrarPorNivel(String... niveles) {
        return lineasCapturadas.stream()
                .filter(linea -> {
                    for (String nivel : niveles) {
                        // Cubre ambos formatos:
                        // "... W/TagName..." → formato con -v time
                        // "... W TagName..." → formato estándar
                        if (linea.contains(" " + nivel + "/") ||
                                linea.contains(" " + nivel + " "))
                            return true;
                    }
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // Guarda las líneas (filtradas o todas) en disco
    // Carpeta: logs/NombreModelo/2024-01-15/
    // public Path guardar(String nombreModelo, String palabraClave) throws IOException {
    //     List<String> lineas = palabraClave.isBlank()
    //             ? lineasCapturadas
    //             : filtrar(palabraClave);

    //     String fecha = LocalDateTime.now().format(
    //             DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    //     String timestamp = LocalDateTime.now().format(
    //             DateTimeFormatter.ofPattern("HHmmss"));

    //     // Carpeta: logs/Samsung_Galaxy_S22/2024-01-15/
    //     String nombreLimpio = nombreModelo.replaceAll("[^a-zA-Z0-9_]", "_");
    //     String escritorio = System.getProperty("user.home") + "/Desktop";
    //     Path carpeta = Paths.get(escritorio, "logs", nombreLimpio, fecha);
    //     Files.createDirectories(carpeta);

    //     String nombreArchivo = (palabraClave.isBlank() ? "logcat" : palabraClave)
    //             + "_" + timestamp + ".txt";
    //     Path archivo = carpeta.resolve(nombreArchivo);

    //     Files.write(archivo, lineas);
    //     return archivo;
    // }

    public Path guardar(String nombreModelo, List<String> lineas,
            boolean soloRelevantes, String palabraClave) throws IOException {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        String nombreLimpio = nombreModelo.replaceAll("[^a-zA-Z0-9_]", "_");
        String escritorio = System.getProperty("user.home") + "/Desktop";
        Path carpeta = Paths.get(escritorio, "logs", nombreLimpio, fecha);
        Files.createDirectories(carpeta);

        // Nombre expresivo según filtros aplicados
        StringBuilder nombre = new StringBuilder("logcat");
        if (soloRelevantes)
            nombre.append("_WEF");
        if (!palabraClave.isBlank())
            nombre.append("_").append(
                    palabraClave.replaceAll("[^a-zA-Z0-9]", "_"));
        nombre.append("_").append(timestamp).append(".txt");

        Path archivo = carpeta.resolve(nombre.toString());
        Files.write(archivo, lineas);
        return archivo;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<String> getLineasCapturadas() {
        return lineasCapturadas;
    }
}
