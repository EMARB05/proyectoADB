package com.example.Model;

public class Foto {
    private int idFoto;
    private String url;
    private String descripcion;

    public Foto() {
    }

    public Foto(String url, String descripcion) {
        this.url = url;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public int getIdFoto() { return idFoto; }
    public void setIdFoto(int id) { this.idFoto = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}