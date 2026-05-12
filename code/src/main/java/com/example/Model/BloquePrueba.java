package com.example.Model;

public class BloquePrueba {
    private final String id;
    private final String descripcion;
    private final String comando;
    private final boolean manual;

    public BloquePrueba(String id, String descripcion, String comando) {
        this.id = id;
        this.descripcion = descripcion;
        this.comando = comando;
        this.manual = false;
    }
    public BloquePrueba(String id, String descripcion, String comando, Boolean manual) {
        this.id = id;
        this.descripcion = descripcion;
        this.comando = comando;
        this.manual = manual;
    }

    public String getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getComando() {
        return comando;
    }

    public PasoPrueba toPasoPrueba() {
        return new PasoPrueba(id + "  —  " + descripcion, comando, manual);
    }

    @Override
    public String toString() {
        return id + "  —  " + descripcion;
    }
}