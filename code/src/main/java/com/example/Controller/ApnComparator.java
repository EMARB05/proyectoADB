// src/main/java/com/example/Controller/ApnComparator.java
package com.example.Controller;

import java.util.*;

public class ApnComparator {

    public static class Diferencia {
        public String campo;
        public String valorXml;   // null si no existe en el XML
        public String valorExcel;
        public boolean esNuevo;   // true si el campo no existe en el XML

        public Diferencia(String campo, String valorXml,
                          String valorExcel, boolean esNuevo) {
            this.campo     = campo;
            this.valorXml  = valorXml;
            this.valorExcel = valorExcel;
            this.esNuevo   = esNuevo;
        }
    }

    public static class ResultadoComparacion {
        public XmlApnParser.ApnEntry apnXml;       // null si no existe en XML
        public ExcelApnParser.ApnExcel apnExcel;
        public List<Diferencia> diferencias = new ArrayList<>();
        public boolean existeEnXml;

        public boolean tieneDiferencias() {
            return !diferencias.isEmpty() || !existeEnXml;
        }
    }

    // Compara un APN del Excel contra el XML
    public static ResultadoComparacion comparar(
            ExcelApnParser.ApnExcel apnExcel,
            XmlApnParser xmlParser,
            String mcc, String mnc) {

        ResultadoComparacion resultado = new ResultadoComparacion();
        resultado.apnExcel = apnExcel;

        if (apnExcel.apn == null) return resultado;

        // Buscamos el APN en el XML por mcc+mnc+apn
        XmlApnParser.ApnEntry apnXml =
            xmlParser.buscarApn(mcc, mnc, apnExcel.apn);

        resultado.apnXml     = apnXml;
        resultado.existeEnXml = apnXml != null;

        if (apnXml == null) {
            // El APN completo no existe en el XML — todo es nuevo
            for (Map.Entry<String, String> entry : apnExcel.campos.entrySet()) {
                resultado.diferencias.add(
                    new Diferencia(entry.getKey(), null, entry.getValue(), true)
                );
            }
            return resultado;
        }

        // Comparamos campo a campo
        for (Map.Entry<String, String> entry : apnExcel.campos.entrySet()) {
            String campo      = entry.getKey().toLowerCase();
            String valorExcel = entry.getValue();
            String valorXml   = apnXml.atributos.get(campo);

            if (valorXml == null) {
                // Campo existe en Excel pero no en XML
                resultado.diferencias.add(
                    new Diferencia(campo, null, valorExcel, true)
                );
            } else if (!valorXml.equalsIgnoreCase(valorExcel)) {
                // Campo existe en ambos pero con distinto valor
                resultado.diferencias.add(
                    new Diferencia(campo, valorXml, valorExcel, false)
                );
            }
        }

        return resultado;
    }
}