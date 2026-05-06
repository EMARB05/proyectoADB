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

        public ResultadoComparacion(ExcelApnParser.ApnExcel apnExcel, XmlApnParser.ApnEntry apnXml) {
            this.apnExcel = apnExcel;
            this.apnXml = apnXml;
            this.existeEnXml = (apnXml != null);
        }

        public boolean tieneDiferencias() {
            return !diferencias.isEmpty() || !existeEnXml;
        }
    }

    /**
     * Compara todos los APNs de un operador en el Excel contra el XML.
     * Garantiza que cada nodo del XML se use una sola vez (mapeo 1 a 1).
     */
    public static List<ResultadoComparacion> compararTodoElOperador(
            List<ExcelApnParser.ApnExcel> apnsExcel, 
            XmlApnParser xmlParser, 
            String mcc, 
            String mnc) {

        List<ResultadoComparacion> resultados = new ArrayList<>();
        // Candidatos del XML para este MCC/MNC
        List<XmlApnParser.ApnEntry> candidatosXml = xmlParser.buscarPorOperador(mcc, mnc);
        
        // Registro de nodos ya vinculados para evitar duplicidades
        Set<XmlApnParser.ApnEntry> asignados = new HashSet<>();

        for (ExcelApnParser.ApnExcel excel : apnsExcel) {
            XmlApnParser.ApnEntry mejorMatch = null;

            if (excel.apn != null) {
                // 1. INTENTO POR NOMBRE (DNI): Coincide APN y el 'name/carrier' está en los títulos del Excel
                for (XmlApnParser.ApnEntry xml : candidatosXml) {
                    if (asignados.contains(xml)) continue;

                    if (xml.apn.equalsIgnoreCase(excel.apn)) {
                        String nombreXml = xml.atributos.getOrDefault("carrier", 
                                           xml.atributos.getOrDefault("name", "")).trim();
                        
                        if (excel.titulosCandidatos.contains(nombreXml)) {
                            mejorMatch = xml;
                            break;
                        }
                    }
                }

                // 2. FALLBACK: Si no hay match por nombre, el primero libre que coincida en el nombre del APN
                if (mejorMatch == null) {
                    for (XmlApnParser.ApnEntry xml : candidatosXml) {
                        if (!asignados.contains(xml) && xml.apn.equalsIgnoreCase(excel.apn)) {
                            mejorMatch = xml;
                            break;
                        }
                    }
                }
            }

            if (mejorMatch != null) {
                asignados.add(mejorMatch);
            }

            // Crear el resultado y calcular las diferencias campo a campo
            ResultadoComparacion res = new ResultadoComparacion(excel, mejorMatch);
            calcularDiferencias(res);
            resultados.add(res);
        }
        return resultados;
    }

    /**
     * Compara los atributos del APN de Excel con los del XML y rellena la lista de diferencias.
     */
    private static void calcularDiferencias(ResultadoComparacion resultado) {
        ExcelApnParser.ApnExcel excel = resultado.apnExcel;
        XmlApnParser.ApnEntry xml = resultado.apnXml;

        if (xml == null) {
            // Todo es nuevo
            for (Map.Entry<String, String> entry : excel.campos.entrySet()) {
                resultado.diferencias.add(new Diferencia(entry.getKey(), null, entry.getValue(), true));
            }
            return;
        }

        // Comparar campo a campo
        for (Map.Entry<String, String> entry : excel.campos.entrySet()) {
            String campo = entry.getKey().toLowerCase();
            String valorExcel = entry.getValue();
            String valorXml = xml.atributos.get(campo);

            if (valorXml == null) {
                // Campo nuevo en este APN
                resultado.diferencias.add(new Diferencia(campo, null, valorExcel, true));
            } else if (!valorXml.equalsIgnoreCase(valorExcel)) {
                // Valor modificado
                resultado.diferencias.add(new Diferencia(campo, valorXml, valorExcel, false));
            }
        }
    }
}