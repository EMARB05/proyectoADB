package com.example.Model;

import java.util.HashMap;
import java.util.Map;

// En PerfilDialer añadir soporte para comandos de teclas físicas
public class PerfilDialer {
    private final String modelo;
    private final boolean esTactil;

    // Para táctil — coordenadas
    private final int xMostrarMas, yMostrarMas;
    private final int xHold, yHold;
    private final int xMute, yMute;
    private final int xTeclado, yTeclado;
    private final Map<Integer, int[]> coordNumeros; // Coordenadas de los números 0-9 en el teclado

    // Para feature phone — secuencia de keycodes
    private final String cmdHold;    // secuencia ADB para hold
    private final String cmdRetrieve; // secuencia ADB para retrieve
    private final String cmdMute;    // secuencia ADB para mute
    private final String cmdUnmute;  // secuencia ADB para unmute

    // Constructor táctil
    public PerfilDialer(String modelo, boolean esTactil,
                        int xMostrarMas, int yMostrarMas,
                        int xHold, int yHold,
                        int xMute, int yMute) {
        this(modelo, esTactil, xMostrarMas, yMostrarMas, xHold, yHold, xMute, yMute, 0, 0, new HashMap<>());
    }

    // Constructor táctil con Teclado
    public PerfilDialer(String modelo, boolean esTactil,
                        int xMostrarMas, int yMostrarMas,
                        int xHold, int yHold,
                        int xMute, int yMute,
                        int xTeclado, int yTeclado) {
        this(modelo, esTactil, xMostrarMas, yMostrarMas, xHold, yHold, xMute, yMute, xTeclado, yTeclado, new HashMap<>());
    }

    // Constructor táctil con Teclado y coordenadas de números
    public PerfilDialer(String modelo, boolean esTactil,
                        int xMostrarMas, int yMostrarMas,
                        int xHold, int yHold,
                        int xMute, int yMute,
                        int xTeclado, int yTeclado,
                        Map<Integer, int[]> coordNumeros) {
        this.modelo = modelo;
        this.esTactil = esTactil;
        this.xMostrarMas = xMostrarMas;
        this.yMostrarMas = yMostrarMas;
        this.xHold = xHold;
        this.yHold = yHold;
        this.xMute = xMute;
        this.yMute = yMute;
        this.xTeclado = xTeclado;
        this.yTeclado = yTeclado;
        this.coordNumeros = coordNumeros;
        this.cmdHold = null;
        this.cmdRetrieve = null;
        this.cmdMute = null;
        this.cmdUnmute = null;
    }

    // Constructor feature phone — solo keycodes
    public PerfilDialer(String modelo,
                        String cmdHold, String cmdRetrieve,
                        String cmdMute, String cmdUnmute) {
        this.modelo = modelo;
        this.esTactil = false;
        this.xMostrarMas = 0; this.yMostrarMas = 0;
        this.xHold = 0; this.yHold = 0;
        this.xMute = 0; this.yMute = 0;
        this.xTeclado = 0; this.yTeclado = 0;
        this.coordNumeros = new HashMap<>();
        this.cmdHold = cmdHold;
        this.cmdRetrieve = cmdRetrieve;
        this.cmdMute = cmdMute;
        this.cmdUnmute = cmdUnmute;
    }

    public boolean isTactil() { return esTactil; }
    public boolean tieneComandos() { return cmdHold != null; }
    public int getXMostrarMas() { return xMostrarMas; }
    public int getYMostrarMas() { return yMostrarMas; }
    public int getXHold() { return xHold; }
    public int getYHold() { return yHold; }
    public int getXMute() { return xMute; }
    public int getYMute() { return yMute; }
    public int getXTeclado() { return xTeclado; }
    public int getYTeclado() { return yTeclado; }
    public Map<Integer, int[]> getCoordNumeros() { return coordNumeros; }
    public int[] getCoordNumero(int numero) { 
        return coordNumeros.getOrDefault(numero, new int[]{0, 0}); 
    }
    public String getCmdHold() { return cmdHold; }
    public String getCmdRetrieve() { return cmdRetrieve; }
    public String getCmdMute() { return cmdMute; }
    public String getCmdUnmute() { return cmdUnmute; }
    public String getModelo() { return modelo; }
}