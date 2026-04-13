package com.example.Model;

public class Foto {
    private int idFoto;
    private int idModelo;
    private String url;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String u) {
        this.url = u;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String d) {
        this.descripcion = d;
    }
}
