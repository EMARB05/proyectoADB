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
        List<XmlApnParser.ApnEntry> candidatosXml = xmlParser.buscarPorOperador(mcc, mnc);
        Set<XmlApnParser.ApnEntry> asignados = new HashSet<>();

        System.out.println("\n==== INICIO COMPARACIÓN OPERADOR: MCC=" + mcc + ", MNC=" + mnc + " ====");
        System.out.println("Candidatos XML encontrados: " + candidatosXml.size());
        for (XmlApnParser.ApnEntry xml : candidatosXml) {
            System.out.println("  -> XML: name=" + xml.atributos.get("name") + ", carrier=" + xml.atributos.get("carrier") + ", apn='" + xml.apn + "'");
        }

        for (ExcelApnParser.ApnExcel excel : apnsExcel) {
            System.out.println("\nAnalizando Excel: nombreApnOriginal='" + excel.nombreApnOriginal + "', apn='" + excel.apn + "'");
            System.out.println("  Títulos candidatos Excel: " + excel.titulosCandidatos);
            
            XmlApnParser.ApnEntry mejorMatch = null;

            if (excel.apn != null) {
                // 1. INTENTO POR NOMBRE
                for (XmlApnParser.ApnEntry xml : candidatosXml) {
                    if (asignados.contains(xml)) continue;

                    // Imprimimos la evaluación del paso 1
                    boolean apnCoincide = xml.apn != null && xml.apn.equalsIgnoreCase(excel.apn);
                    String nombreXml = xml.atributos.getOrDefault("carrier", xml.atributos.getOrDefault("name", "")).trim();
                    boolean nombreCoincide = excel.titulosCandidatos.contains(nombreXml);
                    
                    if (apnCoincide && nombreCoincide) {
                        System.out.println("  [Paso 1 - MATCH EXCELENTE] con XML: name=" + nombreXml);
                        mejorMatch = xml;
                        break;
                    }
                }

                // 2. FALLBACK
                if (mejorMatch == null) {
                    System.out.println("  [Paso 1 falló] Probando Paso 2 (Fallback por APN)...");
                    for (XmlApnParser.ApnEntry xml : candidatosXml) {
                        if (asignados.contains(xml)) continue;

                        System.out.println("    Evaluando contra XML apn='" + xml.apn + "' (nombre=" + xml.atributos.get("name") + ")");
                        if (xml.apn != null && xml.apn.equalsIgnoreCase(excel.apn)) {
                            System.out.println("    [Paso 2 - MATCH FALLBACK] con XML: name=" + xml.atributos.get("name"));
                            mejorMatch = xml;
                            break;
                        }
                    }
                }
            } else {
                System.out.println("  [ALERTA] excel.apn es NULL. Saltando búsquedas.");
            }

            if (mejorMatch != null) {
                asignados.add(mejorMatch);
            } else {
                System.out.println("  [RESULTADO] No se encontró ningún match para este bloque de Excel.");
            }

            ResultadoComparacion res = new ResultadoComparacion(excel, mejorMatch);
            calcularDiferencias(res);
            resultados.add(res);
        }
        System.out.println("==== FIN COMPARACIÓN ====\n");
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