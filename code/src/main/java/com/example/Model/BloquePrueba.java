package com.example.Model;

import java.util.List;

public class BloquePrueba {
    private final String id;
    private final String descripcion;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean sinOutput;
    private final boolean iotExpress;

    // Inicializa por completo todas las propiedades del bloque de pruebas.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos, boolean manual,
            boolean sinOutput) {
        this.iotExpress = iotExpress;
        this.id = id;
        this.descripcion = descripcion;
        this.comandos = comandos;
        this.manual = manual;
        this.sinOutput = sinOutput;
    }

    // Crea una prueba con múltiples comandos indicando si es de ejecución manual.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos, boolean manual) {
        this(iotExpress, id, descripcion, comandos, manual, false);
    }

    // Crea una prueba automatizada con una lista de comandos.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos) {
        this(iotExpress, id, descripcion, comandos, false, false);
    }

    // Configura todas las propiedades para una prueba de un solo comando.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando, boolean manual,
            boolean sinOutput) {
        this(iotExpress, id, descripcion, List.of(comando), manual, sinOutput);
    }

    // Crea una prueba de un solo comando indicando si requiere intervención manual.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando, boolean manual) {
        this(iotExpress, id, descripcion, List.of(comando), manual, false);
    }

    // Crea una prueba automatizada básica a partir de un solo comando.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando) {
        this(iotExpress, id, descripcion, List.of(comando), false, false);
    }

    // Define una prueba completa con múltiples comandos descartando el modo
    // express.
    public BloquePrueba(String id, String descripcion, List<String> comandos, boolean manual, boolean sinOutput) {
        this(false, id, descripcion, comandos, manual, sinOutput);
    }

    // Configura una prueba manual de varios comandos que no pertenece al modo
    // express.
    public BloquePrueba(String id, String descripcion, List<String> comandos, boolean manual) {
        this(false, id, descripcion, comandos, manual, false);
    }

    // Crea una prueba automatizada estándar con varios comandos sin activar el modo
    // express.
    public BloquePrueba(String id, String descripcion, List<String> comandos) {
        this(false, id, descripcion, comandos, false, false);
    }

    // Configura todas las opciones de un único comando descartando el modo express.
    public BloquePrueba(String id, String descripcion, String comando, boolean manual, boolean sinOutput) {
        this(false, id, descripcion, List.of(comando), manual, sinOutput);
    }

    // Crea una prueba manual de un solo comando fuera del modo express.
    public BloquePrueba(String id, String descripcion, String comando, boolean manual) {
        this(false, id, descripcion, List.of(comando), manual, false);
    }

    // Define una prueba automatizada básica de un comando sin activar el modo
    // express.
    public BloquePrueba(String id, String descripcion, String comando) {
        this(false, id, descripcion, List.of(comando), false, false);
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

    public boolean isManual(){
        return manual;
    }

    public boolean isSinOuput() {
        return sinOutput;
    }

    public boolean isIotExpress() {
        return iotExpress;
    }

    public PasoPrueba toPasoPrueba() {
        return new PasoPrueba(this.iotExpress, this.descripcion, this.comandos, this.manual, this.sinOutput);
    }

    @Override
    public String toString() {
        return id + "  —  " + descripcion;
    }
}