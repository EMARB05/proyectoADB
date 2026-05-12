package com.example.Controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class XmlApnWriter {

    /**
     * Limpia paréntesis informativos (ej: "(IMS)") solo cuando el dato
     * procede del encabezado del bloque de celdas del Excel.
     */
    private static String limpiarSoloSiEsTitulo(String valor) {
        if (valor == null)
            return "";

        if (valor.contains("(") && valor.contains(")")) {
            int pos = valor.indexOf("(");
            if (pos != -1) {
                return valor.substring(0, pos).trim();
            }
        }
        return valor.trim();
    }

    /**
     * Aplica los cambios de la tabla al XML.
     */
    public static String aplicarDiferencia(String linea, ApnComparator.Diferencia d) {
        Pattern pat = Pattern.compile(d.campo + "=\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher(linea);

        if (mat.find()) {
            // Usamos el valor tal cual viene de la tabla (solo con trim)
            String valorParaEscribir = (d.valorExcel != null) ? d.valorExcel.trim() : "";
            return mat.replaceFirst(d.campo + "=\"" + valorParaEscribir + "\"");
        }
        return linea;
    }

    /**
     * Inserta atributos nuevos en un APN existente.
     */
    public static String insertarAtributoEstiloVertical(String lineaCierre, ApnComparator.Diferencia d) {
        if (lineaCierre.contains("/>")) {
            String valorParaEscribir = (d.valorExcel != null) ? d.valorExcel.trim() : "";
            return "        " + d.campo + "=\"" + valorParaEscribir + "\"\n" + lineaCierre;
        }
        return lineaCierre;
    }

    /**
     * Construye el XML para un APN totalmente nuevo detectado en el Excel.
     */
    public static String construirApnXmlVertical(ExcelApnParser.ApnExcel apnExcel, String mcc, String mnc) {
        StringBuilder sb = new StringBuilder("    <apn\n");
 
        String nombreFinal;
        if (apnExcel.campos.containsKey("name")) {
            nombreFinal = apnExcel.campos.get("name").trim();
        } else if (apnExcel.campos.containsKey("carrier")) {
            nombreFinal = apnExcel.campos.get("carrier").trim();
        } else {
            nombreFinal = limpiarSoloSiEsTitulo(apnExcel.nombreApnOriginal);
        }
 
        sb.append("        name=\"").append(nombreFinal).append("\"\n");
 
        if (apnExcel.apn != null && !apnExcel.apn.isEmpty()) {
            sb.append("        apn=\"").append(apnExcel.apn).append("\"\n");
        }
 
        sb.append("        mcc=\"").append(mcc).append("\"\n");
        String mncNorm = (mnc != null && mnc.length() == 1) ? "0" + mnc : mnc;
        sb.append("        mnc=\"").append(mncNorm).append("\"\n");
 
        for (Map.Entry<String, String> e : apnExcel.campos.entrySet()) {
            String clave = e.getKey().toLowerCase();
            if (clave.equals("name") || clave.equals("apn") || clave.equals("mcc") || clave.equals("mnc")) {
                continue;
            }
            String valor = (e.getValue() != null) ? e.getValue().trim() : "";
            sb.append("        ").append(e.getKey()).append("=\"").append(valor).append("\"\n");
        }
 
        sb.append("    />");
        return sb.toString();
    }
    // ───────────────────── COMENTARIOS XML ─────────────────────
 
    /**
     * Construye el comentario-cabecera que se inserta antes del bloque
     * de APNs nuevos al final del archivo.
     */
    public static String construirComentarioBloqueNuevos(int cantidad) {
        return "    <!-- ===== APNs NUEVOS AÑADIDOS ("+cantidad+") ===== -->";
    }
    public static int buscarLineaComentarioNuevos(List<String> lineas) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).contains("<!-- ===== APNs NUEVOS AÑADIDOS (")) {
                return i;
            }
        }
        return -1;
    }
 
    /**
     * Construye el comentario inline que se pone encima de un APN modificado.
     */
    public static String construirComentarioModificado(String nombreApn, String mcc, String mnc, String apn, String type) {
        return "    <!-- MODIFICADO: " + nombreApn + " | "+ mcc+"-"+mnc+"("+apn+" ["+type+"]) actualizado desde Excel -->";
    }
 
    // ───────────────────── UTILIDADES ─────────────────────
    public static int buscarLineaCierre(List<String> lineas) {
        for (int i = lineas.size() - 1; i >= 0; i--) {
            if (lineas.get(i).contains("</apns>"))
                return i;
        }
        return lineas.size() > 0 ? lineas.size() - 1 : 0;
    }

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