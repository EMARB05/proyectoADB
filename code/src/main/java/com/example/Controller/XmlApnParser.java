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

        public String clave() { return mcc + "+" + mnc + "+" + apn; }
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
            while ((linea = br.readLine()) != null) lineas.add(linea);
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
            entry.lineaFin    = buscarLineaFin(entry.lineaInicio);
            entradas.add(entry);
        }
    }

    private int buscarLineaApn(String mcc, String mnc, String apn) {
        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i);
            if (l.contains("mcc=\"" + mcc + "\"") &&
                l.contains("mnc=\"" + mnc + "\"") &&
                l.contains("apn=\"" + apn + "\"")) return i;
        }
        return -1;
    }

    private int buscarLineaFin(int inicio) {
        if (inicio < 0) return inicio;
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
            if (e.mcc.equals(mcc) && e.mnc.equals(mnc)) resultado.add(e);
        }
        return resultado;
    }

    // Busca un APN concreto por mcc+mnc+apn
    public ApnEntry buscarApn(String mcc, String mnc, String apn) {
        for (ApnEntry e : entradas) {
            if (e.clave().equals(mcc + "+" + mnc + "+" + apn)) return e;
        }
        return null;
    }

    public List<String> getLineas() { return lineas; }
    public List<ApnEntry> getEntradas() { return entradas; }
}