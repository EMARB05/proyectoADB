package com.example.Model;

public class Marca {
    private int idMarca;
    private String nombre;
    private String paisOrigen;

    public Marca() {
    }

    public Marca(String nombre, String paisOrigen) {
        this.nombre = nombre;
        this.paisOrigen = paisOrigen;
    }

    public int getIdMarca() {
        return idMarca;
    }

    public void setIdMarca(int id) {
        this.idMarca = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String n) {
        this.nombre = n;
    }

    public String getPaisOrigen() {
        return paisOrigen;
    }

    public void setPaisOrigen(String p) {
        this.paisOrigen = p;
    }

    @Override
    public String toString() {
        return nombre; // Útil cuando JavaFX muestre el objeto en un ComboBox
    }
}
