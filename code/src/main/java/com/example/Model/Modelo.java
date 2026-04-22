package com.example.Model;

import java.util.List;

public class Modelo {
    private int idModelo;
    private Marca marca; // Objeto completo, no solo el ID
    private Soc soc;
    private String nombreModelo;
    private double ramGb;
    private double almacenamientoGb;
    private String soVersion;
    private String ResolucionPantalla;
    private String camaraMp;
    private List<Banda> bandas; // Relación muchos a muchos
    private List<Foto> fotos;

    public Modelo() {
    }

    public Modelo(Marca marca, String nombreModelo) {
        this.marca = marca;
        this.nombreModelo = nombreModelo;
    }

    public int getIdModelo() {
        return idModelo;
    }

    public void setIdModelo(int id) {
        this.idModelo = id;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca m) {
        this.marca = m;
    }

    public Soc getSoc() {
        return soc;
    }

    public void setSoc(Soc s) {
        this.soc = s;
    }

    public String getNombreModelo() {
        return nombreModelo;
    }

    public void setNombreModelo(String n) {
        this.nombreModelo = n;
    }

    public double getRamGb() {
        return ramGb;
    }

    public void setRamGb(double r) {
        this.ramGb = r;
    }

    public double getAlmacenamientoGb() {
        return almacenamientoGb;
    }

    public void setAlmacenamientoGb(double a) {
        this.almacenamientoGb = a;
    }

    public String getSoVersion() {
        return soVersion;
    }

    public void setSoVersion(String s) {
        this.soVersion = s;
    }

    public String getResolucionPantalla() {
        return ResolucionPantalla;
    }

    public void setResolucionPantalla(String p) {
        this.ResolucionPantalla = p;
    }

    public String getCamaraMp() {
        return camaraMp;
    }

    public void setCamaraMp(String c) {
        this.camaraMp = c;
    }

    public List<Banda> getBandas() {
        return bandas;
    }

    public void setBandas(List<Banda> b) {
        this.bandas = b;
    }

    public List<Foto> getFotos() {
        return fotos;
    }

    public void setFotos(List<Foto> f) {
        this.fotos = f;
    }

    @Override
    public String toString() {
        String nombreMarca = (marca != null) ? marca.getNombre() : "Sin marca";
        return nombreMarca + " " + nombreModelo;
    }
}
