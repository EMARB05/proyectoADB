package com.example.Model;

public class Banda {
    private int idBanda;
    private String tipo; // LTE, 5G, GSM...
    private String numeroBanda; // B3, n78...
    private String frecuenciaMhz;
    private String tecnologia; // VoLTE, VoNR...

    public Banda() {
    }

    public Banda(String tipo, String numeroBanda) {
        this.tipo = tipo;
        this.numeroBanda = numeroBanda;
    }

    public int getIdBanda() {
        return idBanda;
    }

    public void setIdBanda(int id) {
        this.idBanda = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String t) {
        this.tipo = t;
    }

    public String getNumeroBanda() {
        return numeroBanda;
    }

    public void setNumeroBanda(String n) {
        this.numeroBanda = n;
    }

    public String getFrecuenciaMhz() {
        return frecuenciaMhz;
    }

    public void setFrecuenciaMhz(String f) {
        this.frecuenciaMhz = f;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String t) {
        this.tecnologia = t;
    }

    @Override
    public String toString() {
        return tipo + " " + numeroBanda;
    }
}
