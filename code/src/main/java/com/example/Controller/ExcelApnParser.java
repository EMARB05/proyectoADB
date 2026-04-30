// src/main/java/com/example/Controller/ExcelApnParser.java
package com.example.Controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelApnParser {

    public static class Operador {
        public String pais;
        public String nombre;
        public String mcc;
        public String mnc;
        public String hojaPais; // Nombre de la hoja donde están sus APNs
    }

    public static class ApnExcel {
        public String nombreApn;      // Nombre del bloque (ej: "TDC Internet")
        public String apn;            // Valor del campo APN
        public Map<String, String> campos = new LinkedHashMap<>();
    }

    private final File archivo;
    private final List<Operador> operadores = new ArrayList<>();
    private Workbook workbook;

    public ExcelApnParser(File archivo) throws Exception {
        this.archivo = archivo;
        workbook = new XSSFWorkbook(new FileInputStream(archivo));
        parsearOperadores();
    }

    private void parsearOperadores() {
        // La primera hoja contiene el listado de operadores
        Sheet hoja = workbook.getSheetAt(0);
        String paisActual = "";

        for (Row fila : hoja) {
            Cell celdaPais     = fila.getCell(0);
            Cell celdaNombre   = fila.getCell(1);
            Cell celdaPlmn     = fila.getCell(2);

            if (celdaNombre == null || celdaPlmn == null) continue;

            String nombre = getCellString(celdaNombre);
            String plmn   = getCellString(celdaPlmn);

            if (nombre.isBlank() || plmn.isBlank()) continue;

            // El país aparece en la primera fila del grupo
            if (celdaPais != null && !getCellString(celdaPais).isBlank()) {
                paisActual = getCellString(celdaPais).trim();
            }

            // PLMN formato "001 01" → mcc="001", mnc="01"
            String[] partes = plmn.trim().split("\\s+");
            if (partes.length < 2) continue;

            // Algunos operadores tienen múltiples PLMN: "034 30 / 33"
            // Creamos una entrada por cada PLMN
            String mcc = partes[0].trim();
            // Procesamos posibles múltiples MNC separados por "/"
            String mncRaw = plmn.replace(mcc, "").trim();
            String[] mncs = mncRaw.split("[/,]");

            for (String mncParte : mncs) {
                String mnc = mncParte.trim();
                if (mnc.isBlank()) continue;

                Operador op = new Operador();
                op.pais    = paisActual;
                op.nombre  = nombre.trim();
                op.mcc     = mcc;
                op.mnc     = mnc;
                op.hojaPais = extraerCodigoPais(paisActual);
                operadores.add(op);
            }
        }
    }

    // "España ES" → busca hoja cuyo nombre contenga "España" o "ES"
    private String extraerCodigoPais(String paisConCodigo) {
        String[] partes = paisConCodigo.trim().split("\\s+");
        String codigo = partes[partes.length - 1]; // Último token = código país

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String nombreHoja = workbook.getSheetName(i);
            if (nombreHoja.equalsIgnoreCase(codigo) ||
                nombreHoja.toLowerCase().contains(codigo.toLowerCase()))
                return nombreHoja;
        }
        // Fallback: buscar por nombre completo
        String nombrePais = paisConCodigo.replaceAll("\\s+[A-Z]{2}$", "").trim();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String nombreHoja = workbook.getSheetName(i);
            if (nombreHoja.toLowerCase().contains(nombrePais.toLowerCase()))
                return nombreHoja;
        }
        return null;
    }

    // Obtiene todos los APNs de un operador en su hoja de país
    public List<ApnExcel> obtenerApnsOperador(Operador operador) {
        List<ApnExcel> resultado = new ArrayList<>();
        if (operador.hojaPais == null) return resultado;

        Sheet hoja = workbook.getSheet(operador.hojaPais);
        if (hoja == null) return resultado;

        // Buscamos la columna del operador en fila 0
        Row filaHeader = hoja.getRow(0);
        if (filaHeader == null) return resultado;

        int colOperador = -1;
        for (Cell celda : filaHeader) {
            if (getCellString(celda).trim().equalsIgnoreCase(operador.nombre.trim())) {
                colOperador = celda.getColumnIndex();
                break;
            }
        }
        if (colOperador < 0) return resultado;

        // Recorremos las filas del operador
        // Los grupos de APNs están separados por filas vacías
        ApnExcel apnActual = null;
        for (int r = 1; r <= hoja.getLastRowNum(); r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) continue;

            Cell celdaCampo = fila.getCell(colOperador);
            Cell celdaValor = fila.getCell(colOperador + 1);

            String campo = celdaCampo != null ? getCellString(celdaCampo).trim() : "";
            String valor = celdaValor != null ? getCellString(celdaValor).trim() : "";

            if (campo.isBlank() && valor.isBlank()) {
                // Fila vacía = separador entre APNs
                apnActual = null;
                continue;
            }

            if (campo.equalsIgnoreCase("APN")) {
                // Nueva entrada APN
                apnActual = new ApnExcel();
                apnActual.apn = valor;
                apnActual.campos.put("apn", valor);
                resultado.add(apnActual);
            } else if (apnActual != null && !campo.isBlank()) {
                // Campo del APN actual
                apnActual.campos.put(campo.toLowerCase(), valor);
            } else if (apnActual == null && !campo.isBlank()) {
                // Nombre del bloque (ej: "TDC Internet")
                apnActual = new ApnExcel();
                apnActual.nombreApn = campo;
                resultado.add(apnActual);
            }
        }
        return resultado;
    }

    public List<Operador> getOperadores() { return operadores; }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}