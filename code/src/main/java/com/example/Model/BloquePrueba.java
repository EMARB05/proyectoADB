package com.example.Model;

import java.util.List;

public class BloquePrueba {
    private final String id;
    private final String descripcion;
    private final List<String> comandos;
    private final boolean manual;

    public BloquePrueba(String id, String descripcion, String comando) {
        this(id, descripcion, List.of(comando), false);
    }

    public BloquePrueba(String id, String descripcion, String comando, boolean manual) {
        this(id, descripcion, List.of(comando), manual);
    }

    public BloquePrueba(String id, String descripcion, List<String> comandos) {
        this(id, descripcion, comandos, false);
    }

    public BloquePrueba(String id, String descripcion, List<String> comandos, boolean manual) {
        this.id = id;
        this.descripcion = descripcion;
        this.comandos = comandos;
        this.manual = manual;
    }

    public String getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getComandos() {
        return comandos;
    }

    public String getComando() {
        return comandos.get(0);
    }

    public PasoPrueba toPasoPrueba() {
        return new PasoPrueba(id + "  —  " + descripcion, comandos, manual);
    }

    @Override
    public String toString() {
        return id + "  —  " + descripcion;
    }
}