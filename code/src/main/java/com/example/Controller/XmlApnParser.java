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
        public int lineaInicio; // Para el highlight en la UI
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
        parsear();
    }

    private void parsear() throws Exception {
        // Guardamos las líneas para el highlight
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null)
                lineas.add(linea);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(archivo);
        NodeList apns = doc.getElementsByTagName("apn");

        for (int i = 0; i < apns.getLength(); i++) {
            Element el = (Element) apns.item(i);
            ApnEntry entry = new ApnEntry();
            entry.mcc = el.getAttribute("mcc");
            entry.mnc = el.getAttribute("mnc");
            entry.apn = el.getAttribute("apn");

            // Guardamos todos los atributos
            NamedNodeMap attrs = el.getAttributes();
            for (int j = 0; j < attrs.getLength(); j++) {
                Node attr = attrs.item(j);
                entry.atributos.put(attr.getNodeName(), attr.getNodeValue());
            }

            // Buscamos la línea en el texto para el highlight
            entry.lineaInicio = buscarLineaApn(entry.mcc, entry.mnc, entry.apn);
            entry.lineaFin = buscarLineaFin(entry.lineaInicio);
            entradas.add(entry);
        }
    }

    private int buscarLineaApn(String mcc, String mnc, String apn) {
        if (apn == null)
            return -1;

        // MAGIA: Forzamos que el MNC tenga siempre 2 dígitos (ej: "9" -> "09")
        // Si ya tiene "09", se queda como "09"
        String mncFormateado = mnc;
        try {
            mncFormateado = String.format("%02d", Integer.parseInt(mnc));
        } catch (NumberFormatException e) {
            // Si no es un número, usamos el valor original
        }

        String apnLower = apn.toLowerCase();

        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i).toLowerCase();

            if (!l.contains("<apn"))
                continue;

            // Buscamos con el formato exacto del XML: mnc="09"
            boolean tieneMcc = l.contains("mcc=\"" + mcc + "\"");
            boolean tieneMnc = l.contains("mnc=\"" + mncFormateado + "\"");
            boolean tieneApn = l.contains("apn=\"" + apnLower + "\"");

            if (tieneMcc && tieneMnc && tieneApn) {
                return i;
            }
        }
        return -1;
    }

    private int buscarLineaFin(int inicio) {
        if (inicio < 0)
            return inicio;
        for (int i = inicio; i < lineas.size(); i++) {
            if (lineas.get(i).contains("/>") || lineas.get(i).contains("</apn>"))
                return i;
        }
        return inicio;
    }

    // Busca todos los APNs de un operador por mcc+mnc
    public List<ApnEntry> buscarPorOperador(String mcc, String mnc) {
        List<ApnEntry> resultado = new ArrayList<>();
        for (ApnEntry e : entradas) {
            if (e.mcc.equals(mcc) && e.mnc.equals(mnc))
                resultado.add(e);
        }
        return resultado;
    }

    // Busca un APN concreto por mcc+mnc+apn
    public ApnEntry buscarApn(String mcc, String mnc, String apn) {
        String mncFormateado = mnc;
        try {
            mncFormateado = String.format("%02d", Integer.parseInt(mnc));
        } catch (NumberFormatException e) {
        }

        for (ApnEntry e : entradas) {
            // Comparamos usando el MNC con el cero a la izquierda
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