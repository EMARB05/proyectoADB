package com.example.Model;

public class Soc {
    private int idSoc;
    private String fabricante;
    private String modeloSoc;
    private String arquitectura;
    private int nucleos;
    private String frecuenciaMhz;

    public Soc() {
    }

    public Soc(String fabricante, String modeloSoc) {
        this.fabricante = fabricante;
        this.modeloSoc = modeloSoc;
    }
    public Soc(String modeloSoc) {
    this.modeloSoc = modeloSoc;
}

    public int getIdSoc() {
        return idSoc;
    }

    public void setIdSoc(int id) {
        this.idSoc = id;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String f) {
        this.fabricante = f;
    }

    public String getModeloSoc() {
        return modeloSoc;
    }

    public void setModeloSoc(String m) {
        this.modeloSoc = m;
    }

    public String getArquitectura() {
        return arquitectura;
    }

    public void setArquitectura(String a) {
        this.arquitectura = a;
    }

    public int getNucleos() {
        return nucleos;
    }

    public void setNucleos(int n) {
        this.nucleos = n;
    }

    public String getFrecuenciaMhz() {
        return frecuenciaMhz;
    }

    public void setFrecuenciaMhz(String f) {
        this.frecuenciaMhz = f;
    }

    @Override
    public String toString() {
        return fabricante + " " + modeloSoc;
    }
}
