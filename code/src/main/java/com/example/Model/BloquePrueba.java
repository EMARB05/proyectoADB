package com.example.Model;

import java.util.List;

public class BloquePrueba {
    private final String id;
    private final String descripcion;
    private final List<String> comandos;
    private final boolean manual;
    private final boolean restablecerPhoneAppAlFinal;
     private final boolean sinOutput;
    private final boolean iotExpress;

  
   

    // Inicializa por completo todas las propiedades del bloque de pruebas.
    public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos, boolean manual,
            boolean sinOutput, boolean restablecerPhoneAppAlFinal) {
        this.iotExpress = iotExpress;
        this.id = id;
        this.descripcion = descripcion;
        this.comandos = comandos;
        this.manual = manual;
        this.restablecerPhoneAppAlFinal = restablecerPhoneAppAlFinal;
        this.sinOutput = sinOutput;
    }
    public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos) {
    this(iotExpress, id, descripcion, comandos, false, false, false);
}
    public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando) {
    this(iotExpress, id, descripcion, List.of(comando), false, false, false);
}
/**
 * Constructor que permite definir si la prueba requiere intervención manual.
 * Por defecto: MUESTRA output y NO restablece la app.
 */
public BloquePrueba(boolean iotExpress, String id, String descripcion, List<String> comandos, boolean manual) {
    this(iotExpress, id, descripcion, comandos, manual, false, false);
}

/**
 * Constructor ultra-reducido (Asume que iotExpress es false).
 * Por defecto: iotExpress = false, manual = false, sinOutput = false, restablecer = false.
 */
public BloquePrueba(String id, String descripcion, List<String> comandos) {
    this(false, id, descripcion, comandos, false, false, false);
}

/**
 * Constructor ultra-reducido especificando si es manual.
 * Por defecto: iotExpress = false, sinOutput = false, restablecer = false.
 */
public BloquePrueba(String id, String descripcion, List<String> comandos, boolean manual) {
    this(false, id, descripcion, comandos, manual, false, false);
}


// =========================================================================
// 3. CONSTRUCTORES DE CONVENIENCIA (Para un solo comando String en vez de lista)
// =========================================================================
// Súper útiles para tus scripts actuales de Radio y Call Timer, ya que la mayoría 
// de tus pruebas usan una sola línea de comandos encadenados con '&&'.

/**
 * Constructor principal adaptado para recibir un comando único en formato String.
 */
public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando, boolean manual,
                    boolean sinOutput, boolean restablecerPhoneAppAlFinal) {
    this(iotExpress, id, descripcion, comando != null ? List.of(comando) : List.of(), manual, sinOutput, restablecerPhoneAppAlFinal);
}

public BloquePrueba(String id, String descripcion, String comando, boolean manual, boolean sinOutput) {
    this(false, id, descripcion, comando != null ? List.of(comando) : List.of(), manual, sinOutput, false);
}

/**
 * El "salvavidas" para tus listas FXML actuales: comando único en String + flag de manual.
 * Por defecto: MUESTRA output y NO restablece la app.
 */
public BloquePrueba(boolean iotExpress, String id, String descripcion, String comando, boolean manual) {
    this(iotExpress, id, descripcion, comando != null ? List.of(comando) : List.of(), manual, false, false);
}

/**
 * El más básico para pruebas rápidas automáticas con un solo comando String.
 * Por defecto: iotExpress = false, manual = false, sinOutput = false, restablecer = false.
 */
public BloquePrueba(String id, String descripcion, String comando) {
    this(false, id, descripcion, comando != null ? List.of(comando) : List.of(), false, false, false);
}

/**
 * Para un comando rápido en String especificando si es manual.
 * Por defecto: iotExpress = false, sinOutput = false, restablecer = false.
 */
public BloquePrueba(String id, String descripcion, String comando, boolean manual) {
    this(false, id, descripcion, comando != null ? List.of(comando) : List.of(), manual, false, false);
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
        return new PasoPrueba( iotExpress, id + "  —  " + descripcion, comandos, manual, sinOutput, restablecerPhoneAppAlFinal);
    }

    public boolean isRestablecerPhoneAppAlFinal() {
        return restablecerPhoneAppAlFinal;
    }
    @Override
    public String toString() {
        return id + "  —  " + descripcion;
    }
}