package com.example.Controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class XmlApnWriter {

    // Cambiado a PUBLIC y eliminados parámetros unused
    public static String aplicarDiferencia(String linea, ApnComparator.Diferencia d) {
        // Buscamos si el atributo ya existe en esta línea
        Pattern pat = Pattern.compile(d.campo + "=\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher(linea);
        if (mat.find()) {
            return mat.replaceFirst(d.campo + "=\"" + d.valorExcel + "\"");
        }
        return linea;
    }

    // NUEVO MÉTODO para insertar atributos que no existían
    public static String insertarAtributoEstiloVertical(String lineaCierre, ApnComparator.Diferencia d) {
        // Si la línea es el cierre " />", insertamos el atributo antes con formato
        if (lineaCierre.contains("/>")) {
            return "        " + d.campo + "=\"" + d.valorExcel + "\"\n" + lineaCierre;
        }
        return lineaCierre;
    }

    // Cambiado a PUBLIC para que el controlador lo use al insertar nuevos APNs
    // En XmlApnWriter.java

    public static String construirApnXmlVertical(ExcelApnParser.ApnExcel apnExcel, String mcc, String mnc) {
        StringBuilder sb = new StringBuilder("    <apn\n");

        // 1. Atributos prioritarios
        String name = apnExcel.campos.getOrDefault("name", apnExcel.nombreApnOriginal);
        sb.append("        name=\"").append(name).append("\"\n");

        if (apnExcel.apn != null && !apnExcel.apn.isEmpty()) {
            sb.append("        apn=\"").append(apnExcel.apn).append("\"\n");
        }

        // 2. Identificadores de red
        sb.append("        mcc=\"").append(mcc).append("\"\n");
        String mncNorm = (mnc != null && mnc.length() == 1) ? "0" + mnc : mnc;
        sb.append("        mnc=\"").append(mncNorm).append("\"\n");

        // 3. El resto de atributos del mapa
        for (Map.Entry<String, String> e : apnExcel.campos.entrySet()) {
            String clave = e.getKey().toLowerCase();
            // Saltamos los que ya hemos puesto arriba
            if (clave.equals("name") || clave.equals("apn") || clave.equals("mcc") || clave.equals("mnc")) {
                continue;
            }
            sb.append("        ").append(e.getKey()).append("=\"").append(e.getValue()).append("\"\n");
        }

        sb.append("    />");
        return sb.toString();
    }

    // Cambiado a PUBLIC
    public static int buscarLineaCierre(List<String> lineas) {
        // Buscamos el final del documento XML para insertar ahí los nuevos
        for (int i = lineas.size() - 1; i >= 0; i--) {
            if (lineas.get(i).contains("</apns>"))
                return i;
        }
        return lineas.size() > 0 ? lineas.size() - 1 : 0;
    }

    // Nuevo método para el guardado final
    public static File guardarArchivoFinal(File original, List<String> lineas) throws IOException {
        File destino = calcularNombreVersion(original);
        Files.write(destino.toPath(), lineas);
        return destino;
    }

    public static File calcularNombreVersion(File archivo) {
        String nombre = archivo.getName();
        String base = nombre.replaceAll("\\.xml$", "");
        File carpeta = archivo.getParentFile();
        Pattern pat = Pattern.compile("^(.+)_V(\\d+)$");
        Matcher mat = pat.matcher(base);

        String nuevoNombre = mat.matches()
                ? mat.group(1) + "_V" + (Integer.parseInt(mat.group(2)) + 1) + ".xml"
                : base + "_V1.xml";
        return new File(carpeta, nuevoNombre);
    }
}