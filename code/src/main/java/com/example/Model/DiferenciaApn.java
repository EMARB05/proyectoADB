package com.example.Model;

// 1. LA CLASE DEBE SER PUBLIC
public class DiferenciaApn {
    
    private final String operadora;
    private final String atributo;
    private final String valOriginal;
    private final String valCopia;
    private final String estado;
    private final String nombre;

    public DiferenciaApn(String operadora,String nombre,  String atributo, String valOriginal, String valCopia) {
        this.operadora = operadora;
        this.nombre=nombre;
        this.atributo = atributo;
        this.valOriginal = valOriginal;
        this.valCopia = valCopia;
        this.estado = valOriginal.equals(valCopia) ? "IGUAL" : "DIFERENTE";
    }

    // 2. LOS GETTERS DEBEN SER PUBLIC (Esto es lo que te está fallando)
    public String getOperadora() { return operadora; }
    public String getAtributo() { return atributo; }
    public String getValOriginal() { return valOriginal; }
    public String getValCopia() { return valCopia; } // JavaFX busca "valCopia" -> getValCopia()
    public String getEstado() { return estado; }

    public String getNombre() {
        return nombre;
    }
    
}