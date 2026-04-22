package com.example.Model;

import java.util.List;

public class Dispositivo {
    private int idDispositivo;
    private Modelo modelo; // Objeto completo con toda la ficha técnica
    private String serialNumber; // Clave de búsqueda al conectar por ADB
    private String android_id;
    private String notas;
    private String fechaRegistro;
    private List<Banda> bandasTemporales;

    public Dispositivo() {
    }

    public Dispositivo(Modelo modelo, String serialNumber, String android_id) {
        this.modelo = modelo;
        this.serialNumber = serialNumber;
        this.android_id = android_id;
    }

    public int getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(int id) {
        this.idDispositivo = id;
    }

    public Modelo getModelo() {
        return modelo;
    }

    public String getAndroid_id() {
        return android_id;
    }

    public void setAndroid_id(String android_id) {
        this.android_id = android_id;
    }

    public void setModelo(Modelo m) {
        this.modelo = m;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String s) {
        this.serialNumber = s;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String n) {
        this.notas = n;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String f) {
        this.fechaRegistro = f;
    }

    public List<Banda> getBandasTemporales() {
        return bandasTemporales;
    }

    public void setBandasTemporales(List<Banda> bandasTemporales) {
        this.bandasTemporales = bandasTemporales;
    }

    @Override
    public String toString() {
        String nombreModelo = (modelo != null) ? modelo.getNombreModelo() : "Modelo no asignado";
        return serialNumber + " [" + nombreModelo + "]";
    }

}
