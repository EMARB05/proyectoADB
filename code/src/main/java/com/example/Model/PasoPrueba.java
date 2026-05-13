package com.example.Model;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PasoPrueba {
    private final String nombre;
    private final List<String> comandos;
    private final boolean manual;
    private final StringProperty estado;

    public PasoPrueba(String nombre, List<String> comandos, boolean manual) {
        this.nombre = nombre;
        this.comandos = comandos;
        this.manual = manual;
        this.estado = new SimpleStringProperty("PENDIENTE");
    }

    // Compatibilidad con pasos existentes de un solo comando
    public PasoPrueba(String nombre, String comando) {
        this(nombre, List.of(comando), false);
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

    public String getEstado() {
        return estado.get();
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public void setEstado(String nuevoEstado) {
        this.estado.set(nuevoEstado);
    }
}