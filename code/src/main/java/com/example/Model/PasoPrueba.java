package com.example.Model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PasoPrueba {
    private final String nombre;
    private final String comando;
    private final StringProperty estado; // Usamos Property para que la UI se entere de los cambios

    public PasoPrueba(String nombre, String comando) {
        this.nombre = nombre;
        this.comando = comando;
        this.estado = new SimpleStringProperty("PENDIENTE");
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getComando() { return comando; }
    public String getEstado() { return estado.get(); }
    public StringProperty estadoProperty() { return estado; }
    public void setEstado(String nuevoEstado) { this.estado.set(nuevoEstado); }
}