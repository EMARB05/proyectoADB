// src/main/java/com/example/Controller/XmlApnWriter.java
package com.example.Controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class XmlApnWriter {

    // Aplica las diferencias al XML y guarda como nueva versión
    public static File guardarNuevaVersion(
            File archivoOriginal,
            List<ApnComparator.ResultadoComparacion> resultados) throws IOException {

        List<String> lineas = Files.readAllLines(archivoOriginal.toPath());

        for (ApnComparator.ResultadoComparacion r : resultados) {
            if (!r.tieneDiferencias()) continue;

            if (!r.existeEnXml) {
                // APN nuevo — lo añadimos antes del cierre </apns>
                String nuevoApn = construirApnXml(r.apnExcel);
                int lineaCierre = buscarLineaCierre(lineas);
                if (lineaCierre >= 0) lineas.add(lineaCierre, nuevoApn);
                continue;
            }

            // APN existente — modificamos sus atributos
            int lineaApn = r.apnXml.lineaInicio;
            if (lineaApn < 0 || lineaApn >= lineas.size()) continue;

            String lineaActual = lineas.get(lineaApn);
            for (ApnComparator.Diferencia d : r.diferencias) {
                lineaActual = aplicarDiferencia(lineaActual, d, lineaApn, lineas);
            }
            lineas.set(lineaApn, lineaActual);
        }

        File destino = calcularNombreVersion(archivoOriginal);
        Files.write(destino.toPath(), lineas);
        return destino;
    }

    // Aplica una diferencia a una línea del XML
    private static String aplicarDiferencia(String linea,
                                             ApnComparator.Diferencia d,
                                             int lineaIdx,
                                             List<String> lineas) {
        if (d.esNuevo && d.valorXml == null) {
            // Campo nuevo — lo insertamos como nuevo atributo
            // Buscamos el cierre "/> " de ese APN para insertar antes
            if (linea.contains("/>")) {
                return linea.replace("/>",
                    " " + d.campo + "=\"" + d.valorExcel + "\"/>");
            }
        }
        // Campo existente con valor diferente — reemplazamos
        Pattern pat = Pattern.compile(
            d.campo + "=\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher(linea);
        if (mat.find()) {
            return mat.replaceFirst(d.campo + "=\"" + d.valorExcel + "\"");
        }
        return linea;
    }

    private static String construirApnXml(ExcelApnParser.ApnExcel apn) {
        StringBuilder sb = new StringBuilder("    <apn");
        for (Map.Entry<String, String> e : apn.campos.entrySet()) {
            sb.append(" ").append(e.getKey())
              .append("=\"").append(e.getValue()).append("\"");
        }
        sb.append("/>");
        return sb.toString();
    }

    private static int buscarLineaCierre(List<String> lineas) {
        for (int i = lineas.size() - 1; i >= 0; i--) {
            if (lineas.get(i).contains("</apns>")) return i;
        }
        return lineas.size() - 1;
    }

    // Calcula el nombre de la nueva versión
    // archivo.xml        → archivo_V1.xml
    // archivo_V1.xml     → archivo_V2.xml
    // archivo_V10.xml    → archivo_V11.xml
    public static File calcularNombreVersion(File archivo) {
        String nombre = archivo.getName();
        String base   = nombre.replaceAll("\\.xml$", "");
        File carpeta  = archivo.getParentFile();

        Pattern pat = Pattern.compile("^(.+)_V(\\d+)$");
        Matcher mat = pat.matcher(base);

        String nuevoNombre;
        if (mat.matches()) {
            String prefijo  = mat.group(1);
            int version     = Integer.parseInt(mat.group(2));
            nuevoNombre     = prefijo + "_V" + (version + 1) + ".xml";
        } else {
            nuevoNombre = base + "_V1.xml";
        }

        return new File(carpeta, nuevoNombre);
    }
}