package com.example.Model;

public class Foto {
    private int idFoto;
    private int idModelo;
    private String urlExterna;
    private String descripcion;

    public Foto() {
    }

    public int getIdFoto() {
        return idFoto;
    }

    public void setIdFoto(int id) {
        this.idFoto = id;
    }

    public int getIdModelo() {
        return idModelo;
    }

    public void setIdModelo(int id) {
        this.idModelo = id;
    }

    public String getUrlExterna() {
        return urlExterna;
    }

    public void setUrlExterna(String u) {
        this.urlExterna = u;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String d) {
        this.descripcion = d;
    }
}
