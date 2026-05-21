package com.example.Model;

import java.util.List;

public class BloquePrueba {
    private final String id;
    private final String descripcion;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean sinOutput;

    public BloquePrueba(String id, String descripcion, String comando) {
        this(id, descripcion, List.of(comando), false,false);
    }

    public BloquePrueba(String id, String descripcion, String comando, boolean manual) {
        this(id, descripcion, List.of(comando), manual,false);
    }

    public BloquePrueba(String id, String descripcion, String comando, boolean manual, boolean sinOutput) {
        this(id,descripcion,List.of(comando),manual,sinOutput);
    }

    public BloquePrueba(String id, String descripcion, List<String> comandos) {
        this(id, descripcion, comandos, false,false);
    }

    public BloquePrueba(String id, String descripcion, List<String> comandos,boolean manual) {
        this(id, descripcion, comandos, manual,false);
    }

    public BloquePrueba(String id, String descripcion, List<String> comandos, boolean manual, boolean sinOutput) {
        this.id = id;
        this.descripcion = descripcion;
        this.comandos = comandos;
        this.manual = manual;
        this.sinOutput = sinOutput;
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

    public boolean isSinOuput(){
        return sinOutput;
    }

    public PasoPrueba toPasoPrueba() {
        return new PasoPrueba(id + "  —  " + descripcion, comandos, manual,sinOutput);
    }

    @Override
    public String toString() {
        return id + "  —  " + descripcion;
    }
}