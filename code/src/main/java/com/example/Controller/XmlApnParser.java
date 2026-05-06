// src/main/java/com/example/Controller/XmlApnParser.java
package com.example.Controller;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class XmlApnParser {

    // Representa un APN del XML con todos sus atributos y número de línea
    public static class ApnEntry {
        public String mcc, mnc, apn;
        public Map<String, String> atributos = new LinkedHashMap<>();
        public int lineaInicio;
        public int lineaFin;

        public String clave() {
            return mcc + "+" + mnc + "+" + apn;
        }
    }

    private final File archivo;
    private final List<ApnEntry> entradas = new ArrayList<>();
    private final List<String> lineas = new ArrayList<>();

    public XmlApnParser(File archivo) throws Exception {
        this.archivo = archivo;
        cargarLineasDesdeDisco();
        parsear();
    }

    /**
     * Carga el archivo físico a la lista de strings en memoria.
     */
    private void cargarLineasDesdeDisco() throws Exception {
        lineas.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas.add(linea);
            }
        }
    }

    public void parsear() throws Exception {
        entradas.clear();

        String contenidoCompleto = String.join("\n", lineas);
        InputStream is = new ByteArrayInputStream(contenidoCompleto.getBytes("UTF-8"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(is);
        NodeList apns = doc.getElementsByTagName("apn");

        // NUEVO: Rastreador para no repetir líneas en APNs duplicados
        int punteroBusqueda = 0;

        for (int i = 0; i < apns.getLength(); i++) {
            Element el = (Element) apns.item(i);
            ApnEntry entry = new ApnEntry();
            entry.mcc = el.getAttribute("mcc");
            entry.mnc = el.getAttribute("mnc");
            entry.apn = el.getAttribute("apn");

            NamedNodeMap attrs = el.getAttributes();
            for (int j = 0; j < attrs.getLength(); j++) {
                Node attr = attrs.item(j);
                entry.atributos.put(attr.getNodeName(), attr.getNodeValue());
            }

            // Usamos la nueva versión que acepta el índice de inicio
            entry.lineaInicio = buscarLineaApn(entry.mcc, entry.mnc, entry.apn, punteroBusqueda);
            entry.lineaFin = buscarLineaFin(entry.lineaInicio);

            // Si encontramos la línea, el siguiente APN debe buscarse después de este
            if (entry.lineaInicio != -1) {
                punteroBusqueda = entry.lineaFin + 1;
            }

            entradas.add(entry);
        }
    }

    // NUEVA VERSIÓN: Copia esto debajo del buscarLineaApn original
    public int buscarLineaApn(String mcc, String mnc, String apn, int desdeLinea) {
        String mncNorm = (mnc != null && mnc.length() == 1) ? "0" + mnc : mnc;
        String apnLow = apn.toLowerCase();

        for (int i = desdeLinea; i < lineas.size(); i++) {
            String l = lineas.get(i).toLowerCase();
            if (l.contains("<apn")) {
                // Aquí declaramos las variables que faltaban
                boolean matchMcc = false;
                boolean matchMnc = false;
                boolean matchApn = false;

                for (int j = i; j < lineas.size(); j++) {
                    String sub = lineas.get(j).toLowerCase();
                    if (sub.contains("mcc=\"" + mcc + "\""))
                        matchMcc = true;
                    if (sub.contains("mnc=\"" + mncNorm + "\"") || sub.contains("mnc=\"" + mnc + "\""))
                        matchMnc = true;
                    if (sub.contains("apn=\"" + apnLow + "\""))
                        matchApn = true;

                    if (sub.contains(">"))
                        break;
                }
                if (matchMcc && matchMnc && matchApn)
                    return i;
            }
        }
        return -1;
    }

    // Método auxiliar para obtener el nombre (puedes ponerlo en ApnEntry)
    public String getNombreIdentificador(ApnEntry entry) {
        String n = entry.atributos.get("carrier");
        if (n == null || n.isEmpty())
            n = entry.atributos.get("name");
        return (n != null) ? n.trim() : "";
    }

    /**
     * Actualiza la memoria con nuevas líneas (por ejemplo, desde el CodeArea)
     * y re-ejecuta el parseo lógico.
     */
    public void setLineas(List<String> nuevasLineas) {
        this.lineas.clear();
        this.lineas.addAll(nuevasLineas);
        try {
            parsear();
        } catch (Exception e) {
            System.err.println("Error al actualizar el parser desde memoria: " + e.getMessage());
        }
    }

    public int buscarLineaApn(String mcc, String mnc, String apn) {
        String mncNorm = (mnc != null && mnc.length() == 1) ? "0" + mnc : mnc;
        String apnLow = apn.toLowerCase();

        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i).toLowerCase();
            if (l.contains("<apn")) {
                boolean matchMcc = false, matchMnc = false, matchApn = false;

                for (int j = i; j < lineas.size(); j++) {
                    String sub = lineas.get(j).toLowerCase();
                    if (sub.contains("mcc=\"" + mcc + "\""))
                        matchMcc = true;
                    if (sub.contains("mnc=\"" + mncNorm + "\"") || sub.contains("mnc=\"" + mnc + "\""))
                        matchMnc = true;
                    if (sub.contains("apn=\"" + apnLow + "\""))
                        matchApn = true;

                    if (sub.contains(">"))
                        break;
                }
                if (matchMcc && matchMnc && matchApn)
                    return i;
            }
        }
        return -1;
    }

    private int buscarLineaFin(int inicio) {
        if (inicio < 0)
            return inicio;
        for (int i = inicio; i < lineas.size(); i++) {
            if (lineas.get(i).contains("/>") || lineas.get(i).contains("</apn>")) {
                return i;
            }
        }
        return inicio;
    }

    public List<ApnEntry> buscarPorOperador(String mcc, String mnc) {
        List<ApnEntry> resultado = new ArrayList<>();
        for (ApnEntry e : entradas) {
            if (e.mcc.equals(mcc) && e.mnc.equals(mnc))
                resultado.add(e);
        }
        return resultado;
    }

    public ApnEntry buscarApn(String mcc, String mnc, String apn) {
        String mncFormateado = mnc;
        try {
            mncFormateado = String.format("%02d", Integer.parseInt(mnc));
        } catch (NumberFormatException e) {
        }

        for (ApnEntry e : entradas) {
            if (e.mcc.equals(mcc) &&
                    e.mnc.equals(mncFormateado) &&
                    e.apn.equalsIgnoreCase(apn)) {
                return e;
            }
        }
        return null;
    }

    public List<String> getLineas() {
        return lineas;
    }

    public List<ApnEntry> getEntradas() {
        return entradas;
    }
}