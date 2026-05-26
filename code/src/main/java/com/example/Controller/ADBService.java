package com.example.Controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;

public class ADBService {
    private String rutaRemotaActual;

    // Motor privado que captura exit code + output
    // Solo lo usan los métodos de diganóstico que necesitan saber si el comando
    // realmente funcionó
    private EjecucionADB ejecutarADBConCodigo(String... comando) throws IOException {
        String adbDir = System.getProperty("aea.adb.path");
        if (adbDir != null && comando.length > 0 && comando[0].equals("adb"))
            comando[0] = adbDir + File.separator + "adb.exe";

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);
        Process proceso = pb.start();

        List<String> lineas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null)
                lineas.add(linea);
        }

        int exitCode;
        try {
            exitCode = proceso.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exitCode = -1;
        } finally {
            proceso.destroy();
        }

        return new EjecucionADB(exitCode, lineas);
    }

    // ejecutarADB ahora delega
    private List<String> ejecutarADB(String... comando) throws IOException {
        return ejecutarADBConCodigo(comando).lineas();
    }

    // ADBService — nuevo método que devuelve el resultado completo
    public EjecucionADB ejecutarYObtener(String serial, List<String> comandos, boolean sinOutput) {
        for (String comandoShell : comandos) {
            try {
                List<String> fullCmd = new ArrayList<>(List.of("adb", "-s", serial));
                Matcher m = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)").matcher(comandoShell);
                while (m.find()) {
                    if (m.group(1) != null)
                        fullCmd.add(m.group(1));
                    else if (m.group(2) != null)
                        fullCmd.add(m.group(2));
                    else
                        fullCmd.add(m.group(3));
                }

                EjecucionADB r = ejecutarADBConCodigo(fullCmd.toArray(new String[0]));
                String output = r.outputJunto().toLowerCase();

                if (output.contains("error") || output.contains("not found") || output.contains("permission denied")) {
                    continue;
                }

                if (sinOutput) {
                    // En comandos táctiles, si el código es exitoso (0), ya nos sirve (aunque el
                    // output esté vacío).
                    if (r.exito()) {
                        return r;
                    }
                } else {
                    if (output.isBlank()) {
                        continue;
                    }

                    if (r.exito()) {
                        return r;
                    }
                }

            } catch (IOException e) {
                // prueba el siguiente
            }
        }
        return new EjecucionADB(-1, List.of()); // ninguno funcionó
    }

    public EjecucionADB ejecutarYObtener(String serial, List<String> comandos) {
        return ejecutarYObtener(serial, comandos, false);
    }

    public boolean ejecutarYVerificar(String serial, List<String> comandos) {
        return ejecutarYObtener(serial, comandos, false).exito();
    }

    public boolean ejecutarYVerificar(String serial, String comandoShell) {
        return ejecutarYVerificar(serial, List.of(comandoShell));
    }

    public String exportarApnsXml(String serial) throws IOException {
        // Consulta la base de datos de APNs del sistema via ADB
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell",
                "content", "query",
                "--uri", "content://telephony/carriers",
                "--projection", "name:apn:mcc:mnc:type:protocol:bearer_bitmask:numeric");

        // Construir XML
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<apns>\n");

        for (String linea : salida) {
            if (!linea.startsWith("Row:"))
                continue;

            xml.append("    <apn");

            // Parsear cada campo: "name=Movistar, apn=movistar.es, ..."
            Matcher mat = Pattern.compile("(\\w+)=([^,]+)").matcher(
                    linea.replaceFirst("Row:\\s*\\d+\\s+", ""));

            while (mat.find()) {
                String campo = mat.group(1).trim();
                String valor = mat.group(2).trim();
                valor = valor.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;");
                xml.append("\n        ").append(campo).append("=\"").append(valor).append("\"");
            }
            xml.append("\n    />\n");
        }

        xml.append("</apns>");
        return xml.toString();
    }

    // En ADBService — devuelve el serial activo para un android_id dado
    public String getSerialActivo(String androidId) throws IOException {
        Map<String, String> conectados = obtenerDispositivosConectados();
        String serial = conectados.get(androidId);
        if (serial != null) {
            System.out.println("[ADB] Serial activo para " + androidId + ": " + serial);
            return serial;
        }
        System.out.println("[ADB] No se encontró serial activo para androidId: " + androidId);
        return androidId; // fallback
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
            // String[] partes = comandoShell.split(" ");
            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("adb");
            fullCmd.add("-s");
            fullCmd.add(serial);
            // for (String p : partes)
            // fullCmd.add(p);

            // TESTING
            Matcher m = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)").matcher(comandoShell);
            while (m.find()) {
                if (m.group(1) != null)
                    fullCmd.add(m.group(1));
                else if (m.group(2) != null)
                    fullCmd.add(m.group(2));
                else
                    fullCmd.add(m.group(3));
            }
            // TESTING

            List<String> salida = ejecutarADB(fullCmd.toArray(new String[0]));
            // System.out.println(salida);

            // Retornamos la primera línea de la respuesta (ej: "1") o vacío si no hay nada
            if (salida != null && !salida.isEmpty()) {
                return String.join("\n", salida).trim();
            }
            return "";
        } catch (IOException e) {
            System.err.println("Error ADB Síncrono: " + e.getMessage());
            return null;

        }

    }

    public String ejecutarComandoSincronoArray(String serial, String... partes) {
        try {
            List<String> fullCmd = new ArrayList<>(List.of("adb", "-s", serial));
            fullCmd.addAll(List.of(partes));
            List<String> resultado = ejecutarADB(fullCmd.toArray(new String[0]));
            return resultado != null ? String.join("\n", resultado).trim() : null;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    // MÉTODO PARA DIAGNOSTICO CONTROLLER
    public boolean ejecutarComandoSincronoBoolean(String serial, String comandoShell) {
        String resultado = ejecutarComandoSincrono(serial, comandoShell);
        return resultado != null;
    }

    public Map<String, String> obtenerSpecsHardware(String serial) {
        Map<String, String> specs = new HashMap<>();
        try {
            // RAM en GBs
            String ramRaw = ejecutarComandoSincrono(serial, "shell cat /proc/meminfo | grep MemTotal");
            String ramKb = ramRaw.replaceAll("[^0-9]", "").trim();
            if (!ramKb.isEmpty()) {
                long gb = Math.round(Long.parseLong(ramKb) / 1024.0 / 1024.0);
                specs.put("RAM", gb + " GB");
            }

            // Resolución
            String res = ejecutarComandoSincrono(serial, "shell wm size");
            specs.put("Resolucion", res.replace("Physical size: ", "").trim());

            // DPI
            String dpi = ejecutarComandoSincrono(serial, "shell wm density");
            specs.put("DPI", dpi.replace("Physical density: ", "").trim() + " ppi");

            // Storage — solo el tamaño total
            // Storage — múltiples estrategias
            String storage = "N/A";

            // Estrategia 1: df -h /data (Xiaomi y la mayoría)
            String dfData = ejecutarComandoSincrono(serial, "shell df -h /data");
            String lineaData = Arrays.stream(dfData.split("\n"))
                    .filter(l -> l.contains("/data") && !l.startsWith("Filesystem"))
                    .findFirst().orElse("");
            if (!lineaData.isBlank()) {
                String[] cols = lineaData.trim().replaceAll("\\s+", " ").split(" ");
                if (cols.length >= 2)
                    storage = cols[1]; // columna Size
            }

            // Estrategia 2: df -h /storage/emulated (COOCAA y otros)
            if (storage.equals("N/A") || storage.isBlank()) {
                String dfEmulated = ejecutarComandoSincrono(serial, "shell df -h /storage/emulated");
                String lineaEmulated = Arrays.stream(dfEmulated.split("\n"))
                        .filter(l -> !l.startsWith("Filesystem") && !l.isBlank())
                        .findFirst().orElse("");
                if (!lineaEmulated.isBlank()) {
                    String[] cols = lineaEmulated.trim().replaceAll("\\s+", " ").split(" ");
                    if (cols.length >= 2)
                        storage = cols[1];
                }
            }

            // Estrategia 3: getprop del fabricante
            if (storage.equals("N/A") || storage.isBlank()) {
                String prop = ejecutarComandoSincrono(serial, "shell getprop ro.product.storage");
                if (!prop.isBlank() && !prop.equals("null"))
                    storage = prop.trim();
            }

            // Estrategia 4: stat del filesystem
            if (storage.equals("N/A") || storage.isBlank()) {
                String stat = ejecutarComandoSincrono(serial, "shell stat -f /data");
                Matcher mStat = Pattern.compile("Block size:\\s*(\\d+).*Blocks:\\s*Total:\\s*(\\d+)",
                        Pattern.DOTALL).matcher(stat);
                if (mStat.find()) {
                    long bytes = Long.parseLong(mStat.group(1)) * Long.parseLong(mStat.group(2));
                    storage = Math.round(bytes / 1024.0 / 1024.0 / 1024.0) + " GB";
                }
            }

            specs.put("Storage", storage);

            // CPU
            String cpu = ejecutarComandoSincrono(serial, "shell getprop ro.product.board");
            specs.put("CPU", cpu.trim());

            // Android version
            String android = ejecutarComandoSincrono(serial, "shell getprop ro.build.version.release");
            specs.put("Android", "Android " + android.trim());

            // Parche de seguridad
            String patch = ejecutarComandoSincrono(serial, "shell getprop ro.build.version.security_patch");
            specs.put("Parche", patch.trim());

            // Batería — un solo dumpsys para todo
            String batteryDump = ejecutarComandoSincrono(serial, "shell dumpsys battery");
            

            String nivelCarga = "N/A";
            String estadoCarga = "N/A";

            for (String linea : batteryDump.split("\n")) {
                String trim = linea.trim();
                if (trim.startsWith("level:")) {
                    nivelCarga = trim.replace("level:", "").trim() + "%";
                }
                if (trim.startsWith("status:")) {
                    String estadoRaw = trim.replaceAll("status:\\s*", "").trim();
                    estadoCarga = switch (estadoRaw) {
                        case "1" -> "Desconocido";
                        case "2" -> "Cargando";
                        case "3" -> "Cargando";
                        case "4" -> "No cargando";
                        case "5" -> "Cargado";
                        default -> "Estado: " + estadoRaw;
                    };
                }

                // Fuentes de carga — si ninguna está activa, corregimos el estado
                boolean acPowered = batteryDump.contains("AC powered: true");
                boolean usbPowered = batteryDump.contains("USB powered: true");
                boolean wirelessPowered = batteryDump.contains("Wireless powered: true");

                if (!acPowered && !usbPowered && !wirelessPowered) {
                    estadoCarga = "No cargando";
                }
            }

            specs.put("Bateria", nivelCarga);
            specs.put("EstadoCarga", estadoCarga);

            // IMEI — parsear formato Parcel multilínea
            String rawImei = ejecutarComandoSincrono(serial,
                    "shell service call iphonesubinfo 1 s16 com.android.shell");

            String imei = "N/A";
            // Extraer todos los caracteres entre comillas simples del Parcel
            // Formato: '3.5.0.1.' '6.0.6.0.0.0.2.1.' '0.8.1...'
            StringBuilder imeiBuilder = new StringBuilder();
            Matcher mImei = Pattern.compile("'([0-9a-fA-F. ]+)'").matcher(rawImei);
            while (mImei.find()) {
                // Cada grupo tiene dígitos separados por puntos: "3.5.0.1."
                String grupo = mImei.group(1).replace(".", "").replace(" ", "");
                imeiBuilder.append(grupo);
            }
            String candidato = imeiBuilder.toString().replaceAll("[^0-9]", "");
            // El IMEI son 15 dígitos — cogemos los primeros 15
            if (candidato.length() >= 15) {
                imei = candidato.substring(0, 15);
            }

            specs.put("IMEI", imei);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return specs;
    }

    // Métodos lanzados desde el PC
    // public void ejecutarComandoDirecto(String... args) {
    // new Thread(() -> {
    // try {
    // List<String> fullCmd = new ArrayList<>();
    // fullCmd.add("adb");
    // for (String arg : args)
    // fullCmd.add(arg);

    // ejecutarADB(fullCmd.toArray(new String[0]));
    // System.out.println("ADB Directo ejecutado: " + String.join(" ", fullCmd));
    // } catch (IOException e) {
    // System.err.println("Error ADB Directo: " + e.getMessage());
    // }
    // }).start();
    // }

    public String[] getVolteEstado(String serial) throws IOException {
        // [0] = Soporte, [1] = Estado, [2] = Motivo
        String soporte;
        List<String> salidaProp = ejecutarADB("adb", "-s", serial, "shell", "getprop", "persist.vendor.volte_support");
        soporte = (!salidaProp.isEmpty() && "1".equals(salidaProp.get(0).trim())) ? "Soportado" : "No soportado";

        List<String> dumpsys = ejecutarADB("adb", "-s", serial, "shell", "dumpsys", "telephony.registry");

        // Buscamos SOLO la primera línea con mServiceState (estado actual, sin
        // timestamp)
        String lineaEstado = "";
        for (String linea : dumpsys) {
            String trim = linea.trim();
            if (trim.startsWith("mServiceState=") || trim.startsWith("mServiceState={")) {
                lineaEstado = trim;
                break; // La primera es la actual, ignoramos el historial
            }
        }

        if (lineaEstado.isEmpty()) {
            return new String[] { soporte, "Inactivo", "No se pudo leer el estado" };
        }

        // Extraemos mVoiceRegState
        boolean modoAvion = lineaEstado.contains("mVoiceRegState=3(POWER_OFF)");
        boolean sinServicio = lineaEstado.contains("mVoiceRegState=1(OUT_OF_SERVICE)");

        // Extraemos getRilVoiceRadioTechnology
        String radioTech = "";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("getRilVoiceRadioTechnology=\\d+\\((\\w+)\\)")
                .matcher(lineaEstado);
        if (m.find())
            radioTech = m.group(1);

        // Comprobamos VoLTE activo: buscamos el bloque CS+LTE+VOICE en la misma línea
        // (en el estado actual todo viene en una sola línea larga)
        boolean voiceEnLTE = lineaEstado.contains("domain=CS")
                && lineaEstado.contains("accessNetworkTechnology=LTE")
                && lineaEstado.contains("availableServices=[VOICE");

        // Lógica de decisión
        String estado, motivo;

        if (modoAvion) {
            estado = "Inactivo";
            motivo = "Modo avión";
        } else if (sinServicio || radioTech.equals("Unknown") || radioTech.isEmpty()) {
            estado = "Inactivo";
            motivo = "Sin señal";
        } else if (!radioTech.equals("LTE") && !radioTech.equals("NR")) {
            estado = "Inactivo";
            motivo = "Red " + radioTech + " (requiere 4G)";
        } else if (voiceEnLTE) {
            estado = "Activo";
            motivo = "";
        } else {
            estado = "Inactivo";
            motivo = "LTE sin registro IMS";
        }

        return new String[] { soporte, estado, motivo };
    }
    // --- MÉTODOS DE OBTENCIÓN DE DATOS (Sincrónicos) ---

    // Devuelve Map<androidId, serial> para mostrar androidId en lista
    // pero conservar el serial para comandos ADB

    public Map<String, String> obtenerDispositivosConectados() throws IOException {
        Map<String, String> dispositivos = new LinkedHashMap<>();
        List<String> salida = ejecutarADB("adb", "devices");

        for (String linea : salida) {
            if (linea.isEmpty() || linea.startsWith("List of devices"))
                continue;
            if (linea.contains("unauthorized") || linea.contains("offline"))
                continue;
            if (linea.startsWith("adb-"))
                continue;

            if (linea.contains("\tdevice")) {
                String serial = linea.split("\t")[0].trim();
                try {
                    String androidId = getSecureSetting(serial, "android_id");
                    String clave = (androidId == null || androidId.isBlank()) ? serial : androidId;
                    dispositivos.put(clave, serial);
                } catch (IOException e) {
                    // Si falla la lectura del android_id usamos el serial como fallback
                    dispositivos.put(serial, serial);
                    System.out.println("[ADB] No se pudo leer android_id de " + serial + ", usando serial");
                }
            }
        }
        return dispositivos;
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

        String android_id = getSecureSetting(serial, "android_id");

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

    public List<Banda> obtenerBandas(String serial) throws IOException {
        List<Banda> bandasEncontradas = new ArrayList<>();
        List<String> lineas = ejecutarADB("adb", "-s", serial, "shell", "dumpsys telephony.registry");
        String dumpCompleto = String.join(" ", lineas);

        Pattern bloquePattern = Pattern.compile("CellIdentity(Lte|Nr):\\s*\\{(.*?)\\}");
        Matcher matcherBloque = bloquePattern.matcher(dumpCompleto);

        while (matcherBloque.find()) {
            String techType = matcherBloque.group(1).toUpperCase();
            String contenido = matcherBloque.group(2);

            Matcher m = Pattern.compile("mBands=\\[([\\d,\\s]+)\\]").matcher(contenido);
            if (m.find()) {
                for (String num : m.group(1).split(",")) {
                    String numBanda = num.trim();
                    // Ignorar valores nulos de Android (Integer.MAX_VALUE)
                    if (numBanda.isEmpty() || numBanda.equals("2147483647"))
                        continue;

                    String prefijo = techType.equals("NR") ? "n" : "B";
                    String nombreBanda = prefijo + numBanda;

                    if (bandasEncontradas.stream().noneMatch(b -> b.getNumeroBanda().equals(nombreBanda))) {
                        Banda banda = new Banda();
                        banda.setTipo(techType);
                        banda.setNumeroBanda(nombreBanda);
                        banda.setTecnologia(techType.equals("NR") ? "VoNR" : "VoLTE");

                        // Mapeo rápido de frecuencias
                        switch (numBanda) {
                            case "1":
                                banda.setFrecuenciaMhz("2100");
                                break;
                            case "3":
                                banda.setFrecuenciaMhz("1800");
                                break;
                            case "7":
                                banda.setFrecuenciaMhz("2600");
                                break;
                            case "8":
                                banda.setFrecuenciaMhz("900");
                                break;
                            case "20":
                                banda.setFrecuenciaMhz("800");
                                break;
                            case "28":
                                banda.setFrecuenciaMhz("700");
                                break;
                            case "78":
                                banda.setFrecuenciaMhz("3500");
                                break;
                            default:
                                banda.setFrecuenciaMhz("N/A");
                        }
                        bandasEncontradas.add(banda);
                    }
                }
            }
        }
        return bandasEncontradas;
    }

    public boolean isModoAvionActivo(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "settings", "get", "global",
                "airplane_mode_on");
        return !salida.isEmpty() && "1".equals(salida.get(0).trim());
    }

    public boolean ejecutarPasoSync(String serial, String comandoShell) {
        try {
            String comandoLimpio = comandoShell.replace("adb shell ", "").replace("shell ", "");
            String[] partes = comandoLimpio.split(" ");

            List<String> fullCmd = new ArrayList<>();
            fullCmd.add("adb");
            fullCmd.add("-s");
            fullCmd.add(serial);
            fullCmd.add("shell");
            for (String p : partes)
                fullCmd.add(p);

            // AQUÍ USAMOS LA LISTA:
            List<String> salida = ejecutarADB(fullCmd.toArray(new String[0]));

            // --- LÓGICA DE ESPERA INTELIGENTE PARA WIFI ---
            if (comandoShell.contains("wifi enable")) {
                int intentos = 0;
                boolean conectado = false;
                while (intentos < 10 && !conectado) { // Reintenta durante 10 segundos máximo
                    Thread.sleep(1000);
                    // Consultamos si el wifi ya está activo
                    List<String> check = ejecutarADB("adb", "-s", serial, "shell", "settings", "get", "global",
                            "wifi_on");
                    if (!check.isEmpty() && check.get(0).trim().equals("1")) {
                        conectado = true;
                    }
                    intentos++;
                }
                return conectado;
            }

            // --- LÓGICA PARA PING (Validación real de respuesta) ---
            if (comandoShell.contains("ping")) {
                String respuesta = String.join(" ", salida).toLowerCase();
                return respuesta.contains("bytes from") && !respuesta.contains("100% packet loss");
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // App extractor

    // Devuelve la lista de paquetes instalados
    public List<String> listarPaquetes(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "pm", "list", "packages");

        return salida.stream()
                .filter(app -> app.startsWith("package:"))
                .map(app -> app.replace("package:", "").trim())
                .collect(Collectors.toList());
    }

    public String obtenerRutaApk(String serial, String paquete) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "pm", "path", paquete);
        if (!salida.isEmpty()) {
            return salida.get(0).replace("package:", "").trim();
        }
        return null;
    }

    public void descargarApk(String serial, String paquete, String carpetaDestino) throws IOException {
        String rutaRemota = obtenerRutaApk(serial, paquete);
        if (rutaRemota == null)
            throw new IOException("No se encontró el APK de " + paquete);

        String nombreArchivo = paquete + ".apk";
        ejecutarADB("adb", "-s", serial, "pull", rutaRemota, carpetaDestino + "/" + nombreArchivo);
    }

    // Capturas y grabaciones
    public void capturarPantalla(String serial, String carpetaDestino) throws IOException {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rutaTemporal = "/sdcard/screenshot_" + timeStamp + ".png";
        String rutaLocal = carpetaDestino + "/screenshot_" + timeStamp + ".png";

        // hacer la captura en el dispositivo
        ejecutarADB("adb", "-s", serial, "shell", "screencap", "-p", rutaTemporal);
        // descargarla en el PC
        ejecutarADB("adb", "-s", serial, "pull", rutaTemporal, rutaLocal);
        // Borrarla del dispositivo
        ejecutarADB("adb", "-s", serial, "shell", "rm", rutaTemporal);
    }

    public Process iniciarGrabacion(String serial) throws IOException {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        rutaRemotaActual = "/sdcard/video_" + timeStamp + ".mp4";

        ProcessBuilder pb = new ProcessBuilder(
                "adb", "-s", serial, "shell", "screenrecord", "--time-limit", "180", rutaRemotaActual);
        pb.redirectErrorStream(true);

        String adbDir = System.getProperty("aea.adb.path");
        if (adbDir != null) {
            Map<String, String> env = pb.environment();
            env.put("PATH", adbDir + File.pathSeparator + env.getOrDefault("PATH", ""));
        }

        return pb.start();
    }

    public void enviarSenalParada(String serial) {
        ejecutarComandoSincrono(serial, "shell pkill -l SIGINT screenrecord");
    }

    public void descargarYLimpiar(String serial, String carpetaDestino) throws IOException {
        // Esperamos a que el video se cierre bien
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rutaLocal = carpetaDestino + "/video_" + timeStamp + ".mp4";

        // Descargamos y borramos
        ejecutarADB("adb", "-s", serial, "pull", rutaRemotaActual, rutaLocal);
        ejecutarADB("adb", "-s", serial, "shell", "rm", rutaRemotaActual);
        rutaRemotaActual = null;
    }

    // Añadir al final de ADBService.java
    public List<String> obtenerSoloSeriales() throws IOException {
        Map<String, String> dispositivos = obtenerDispositivosConectados();
        // Extraemos solo los valores (los seriales) del mapa
        return new ArrayList<>(dispositivos.values());
    }

    public void ejecutarComandoDirectoSync(String... args) throws Exception {
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add("adb");
        for (String arg : args)
            fullCmd.add(arg);

        System.out.println("[SYNC] Ejecutando: " + String.join(" ", fullCmd)); // ← log

        ProcessBuilder pb = new ProcessBuilder(fullCmd);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(proceso.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ADB] " + line);
            }
        }

        int exitCode = proceso.waitFor();
        System.out.println("[SYNC] Exit code: " + exitCode); // ← log

        if (exitCode != 0) {
            throw new Exception("ADB falló con código: " + exitCode);
        }
    }

    // En ADBService — nuevo método que detecta y maneja ambos casos
    public void instalarAPK(String serial, String pathArchivo) throws Exception {
        if (pathArchivo.endsWith(".apk")) {
            // Instalación simple — ya funciona
            ejecutarComandoDirectoSync("-s", serial, "install", "-r", pathArchivo);

        } else if (pathArchivo.endsWith(".xapk") || pathArchivo.endsWith(".apks") || pathArchivo.endsWith(".apkm")) {
            // Extraer y instalar como splits
            instalarSplitDesdeZip(serial, pathArchivo);
        }
    }

    private void instalarSplitDesdeZip(String serial, String pathZip) throws Exception {
        Path tempDir = Files.createTempDirectory("splits_");

        try {
            String abi = obtenerAbiDispositivo(serial);
            System.out.println("[ZIP] ABI del dispositivo: " + abi);

            // Primera pasada: recoger todos los .apk y detectar qué ABIs hay disponibles
            List<String> todosLosSplits = new ArrayList<>();
            List<String> splitsAbi = new ArrayList<>();

            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(pathZip)) {
                java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".apk"))
                        continue;

                    String nombreEntry = Path.of(entry.getName()).getFileName().toString();
                    Path destino = tempDir.resolve(nombreEntry);

                    try (java.io.InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
                    }

                    todosLosSplits.add(destino.toString());

                    if (esSplitArquitectura(nombreEntry)) {
                        splitsAbi.add(nombreEntry);
                        System.out.println("[ZIP] Split ABI detectado: " + nombreEntry);
                    }
                }
            }

            if (todosLosSplits.isEmpty())
                throw new Exception("No se encontraron APKs dentro del archivo.");

            // Segunda pasada: decidir qué splits usar
            List<String> splitsFinales = new ArrayList<>();

            boolean hayAbiCompatible = splitsAbi.stream().anyMatch(n -> coincideAbi(n, abi));
            System.out.println("[ZIP] ¿Hay ABI compatible (" + abi + ")? " + hayAbiCompatible);

            for (String path : todosLosSplits) {
                String nombre = Path.of(path).getFileName().toString();

                if (esSplitArquitectura(nombre)) {
                    if (hayAbiCompatible && !coincideAbi(nombre, abi)) {
                        // Solo filtramos si HAY un split compatible — si no, incluimos todos
                        System.out.println("[ZIP] Saltando (ABI no compatible): " + nombre);
                        continue;
                    }
                }

                splitsFinales.add(path);
                System.out.println("[ZIP] Incluido: " + nombre);
            }

            System.out.println("[ZIP] Splits finales a instalar: " + splitsFinales.size());

            List<String> cmd = new ArrayList<>();
            cmd.add("-s");
            cmd.add(serial);
            cmd.add("install-multiple");
            cmd.add("-r");
            cmd.addAll(splitsFinales);

            ejecutarComandoDirectoSync(cmd.toArray(new String[0]));

        } finally {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // Detecta el ABI principal del dispositivo
    private String obtenerAbiDispositivo(String serial) throws IOException {
        List<String> salida = ejecutarADB("adb", "-s", serial, "shell", "getprop", "ro.product.cpu.abi");
        return salida.isEmpty() ? "arm64-v8a" : salida.get(0).trim();
    }

    // Comprueba si el split es específico de una arquitectura
    private boolean esSplitArquitectura(String nombre) {
        return nombre.contains("x86_64") || nombre.contains("x86")
                || nombre.contains("arm64") || nombre.contains("armeabi_v7a")
                || nombre.contains("armeabi") || nombre.contains("mips");
    }

    // Comprueba si el split de arquitectura coincide con el ABI del dispositivo
    private boolean coincideAbi(String nombre, String abi) {
        if (abi.contains("arm64"))
            return nombre.contains("arm64");
        if (abi.contains("armeabi-v7a") || abi.contains("armeabi_v7a"))
            return nombre.contains("armeabi_v7a");
        if (abi.contains("x86_64"))
            return nombre.contains("x86_64");
        if (abi.contains("x86"))
            return nombre.contains("x86") && !nombre.contains("x86_64");
        return true; // si no reconocemos el ABI, incluimos todo
    }

    public record EjecucionADB(int exitCode, List<String> lineas) {
        public boolean exito() {
            return exitCode == 0;
        }

        public String outputJunto() {
            return String.join("\n", lineas).trim();
        }
    }
}
