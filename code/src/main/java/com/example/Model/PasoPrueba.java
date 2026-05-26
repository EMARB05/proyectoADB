package com.example.Model;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PasoPrueba {
    private final String nombre;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean sinOutput;
    private final StringProperty estado;
    private String outputDetalle;
    private final boolean iotExpress;

    // Inicializa por completo todas las propiedades del paso de prueba.
    public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos, boolean manual, boolean sinOutput) {
        this.iotExpress = iotExpress;
        this.nombre = nombre;
        this.comandos = comandos;
        this.manual = manual;
        this.sinOutput = sinOutput;
        this.estado = new SimpleStringProperty("PENDIENTE");
        this.outputDetalle = "";
    }

    // Crea un paso con múltiples comandos indicando si es de ejecución manual.
    public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos, boolean manual) {
        this(iotExpress, nombre, comandos, manual, false);
    }

    // Crea un paso automatizado con una lista de comandos.
    public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos) {
        this(iotExpress, nombre, comandos, false, false);
    }

    // Configura todas las propiedades para un paso de un solo comando.
    public PasoPrueba(boolean iotExpress, String nombre, String comando, boolean manual, boolean sinOutput) {
        this(iotExpress, nombre, List.of(comando), manual, sinOutput);
    }

    // Crea un paso de un solo comando indicando si requiere intervención manual.
    public PasoPrueba(boolean iotExpress, String nombre, String comando, boolean manual) {
        this(iotExpress, nombre, List.of(comando), manual, false);
    }

    // Crea un paso automatizado básico a partir de un solo comando.
    public PasoPrueba(boolean iotExpress, String nombre, String comando) {
        this(iotExpress, nombre, List.of(comando), false, false);
    }

    // Define un paso completo con múltiples comandos descartando el modo express.
    public PasoPrueba(String nombre, List<String> comandos, boolean manual, boolean sinOutput) {
        this(false, nombre, comandos, manual, sinOutput);
    }

    // Configura un paso manual de varios comandos que no pertenece al modo express.
    public PasoPrueba(String nombre, List<String> comandos, boolean manual) {
        this(false, nombre, comandos, manual, false);
    }

    // Crea un paso automatizado estándar con varios comandos sin activar el modo
    // express.
    public PasoPrueba(String nombre, List<String> comandos) {
        this(false, nombre, comandos, false, false);
    }

    // Configura todas las opciones de un único comando descartando el modo express.
    public PasoPrueba(String nombre, String comando, boolean manual, boolean sinOutput) {
        this(false, nombre, List.of(comando), manual, sinOutput);
    }

    // Crea un paso manual de un solo comando fuera del modo express.
    public PasoPrueba(String nombre, String comando, boolean manual) {
        this(false, nombre, List.of(comando), manual, false);
    }

    // Define un paso automatizado básico de un comando sin activar el modo express.
    public PasoPrueba(String nombre, String comando) {
        this(false, nombre, List.of(comando), false, false);
    }

    // Getters
    public boolean isManual() {
        return manual;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getComandos() {
        return comandos;
    }

    public String getComando() {
        return comandos.get(0);
    }

    public boolean isSinOutput() {
        return sinOutput;
    }

    public String getEstado() {
        return estado.get();
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public void setEstado(String nuevoEstado) {
        this.estado.set(nuevoEstado);
    }

    public String getOutputDetalle() {
        return outputDetalle;
    }

    public void setOutputDetalle(String out) {
        this.outputDetalle = out;
    }
}