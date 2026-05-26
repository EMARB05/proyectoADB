package com.example.Model;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PasoPrueba {
    private final String nombre;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean restablecerPhoneAppAlFinal;
    private final StringProperty estado;
    private String outputDetalle;

    public PasoPrueba(String nombre, List<String> comandos, boolean manual) {
        this(nombre, comandos, manual, true);
    }

    public PasoPrueba(String nombre, List<String> comandos, boolean manual, boolean restablecerPhoneAppAlFinal) {
        this.nombre = nombre;
        this.comandos = comandos;
        this.manual = manual;
        this.restablecerPhoneAppAlFinal = restablecerPhoneAppAlFinal;
        this.estado = new SimpleStringProperty("PENDIENTE");
        this.outputDetalle = "";
    }

    // Compatibilidad con pasos existentes de un solo comando
    public PasoPrueba(String nombre, String comando) {
        this(nombre, List.of(comando), false);
    }

    // Getters
    public boolean isManual() {
        return manual;
    }

    public boolean debeRestablecerPhoneAppAlFinal() {
        return restablecerPhoneAppAlFinal;
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