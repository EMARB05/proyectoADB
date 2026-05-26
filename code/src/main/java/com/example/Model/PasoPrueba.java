package com.example.Model;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PasoPrueba {
    private final String nombre;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean restablecerPhoneAppAlFinal;
    private final boolean sinOutput;
    private final StringProperty estado;
    private String outputDetalle;
    private final boolean iotExpress;

   
    // Inicializa por completo todas las propiedades del paso de prueba.
    public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos, boolean manual, boolean sinOutput,boolean restablecerPhoneAppAlFinal) {
        this.iotExpress = iotExpress;
        this.nombre = nombre;
        this.comandos = comandos;
        this.manual = manual;
        this.sinOutput = sinOutput;
        this.estado = new SimpleStringProperty("PENDIENTE");
        this.outputDetalle = "";
        this.restablecerPhoneAppAlFinal = restablecerPhoneAppAlFinal;

    }
    public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos) {
    this(iotExpress, nombre, comandos, false, false, false);
}

/**
 * Constructor para pruebas que solo varían en si son manuales o no.
 * Por defecto: MUESTRA output y NO restablece la app de teléfono.
 */
public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos, boolean manual) {
    this(iotExpress, nombre, comandos, manual, false, false);
}

/**
 * Constructor para pruebas automáticas donde quieres controlar si muestran output o no.
 * Por defecto: NO es manual y NO restablece la app de teléfono.
 */
public PasoPrueba(boolean iotExpress, String nombre, List<String> comandos, boolean manual, boolean sinOutput) {
    this(iotExpress, nombre, comandos, manual, sinOutput, false);
}

/**
 * Constructor ultra-reducido (Ideal si el 90% de tus pruebas NO son iotExpress).
 * Por defecto: iotExpress = false, manual = false, sinOutput = false, restablecer = false.
 */
public PasoPrueba(String nombre, List<String> comandos) {
    this(false, nombre, comandos, false, false, false);
}

/**
 * Constructor ultra-reducido especificando si es manual.
 * Por defecto: iotExpress = false, sinOutput = false, restablecer = false.
 */
public PasoPrueba(String nombre, List<String> comandos, boolean manual) {
    this(false, nombre, comandos, manual, false, false);
}

// =========================================================================
// 3. CONSTRUCTORES DE CONVENIENCIA (Para un solo comando String en vez de lista)
// =========================================================================
// Como muchas veces solo le pasas un comando ADB string único, estos constructores
// te permiten pasar un String normal y ellos solitos lo convierten a List.of(comando).

/**
 * Constructor principal adaptado para un único comando en String.
 */
public PasoPrueba(boolean iotExpress, String nombre, String comando, boolean manual, boolean sinOutput, boolean restablecerPhoneAppAlFinal) {
    this(iotExpress, nombre, comando != null ? List.of(comando) : List.of(), manual, sinOutput, restablecerPhoneAppAlFinal);
}

/**
 * Para un comando rápido automático estándar.
 */
public PasoPrueba(boolean iotExpress, String nombre, String comando) {
    this(iotExpress, nombre, comando != null ? List.of(comando) : List.of(), false, false, false);
}

/**
 * Para un comando rápido especificando si es manual (¡Este te va a servir muchísimo para Hot Dial!).
 */
public PasoPrueba(boolean iotExpress, String nombre, String comando, boolean manual) {
    this(iotExpress, nombre, comando != null ? List.of(comando) : List.of(), manual, false, false);
}

/**
 * El más simple de todos: solo el nombre del paso y un comando en String.
 */
public PasoPrueba(String nombre, String comando) {
    this(false, nombre, comando != null ? List.of(comando) : List.of(), false, false, false);
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