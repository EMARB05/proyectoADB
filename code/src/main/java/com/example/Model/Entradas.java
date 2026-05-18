package com.example.Model;

public class Entradas {
      public static String ok()           { return "input keyevent 23"; }
    public static String softizq()      { return "input keyevent 82"; }
    public static String softder()      { return "input keyevent 4"; }
    public static String verde()        { return "input keyevent 5"; }
    public static String rojo()         { return "input keyevent 26"; }
    public static String arriba()       { return "input keyevent 19"; }
    public static String abajo()        { return "input keyevent 20"; }
    public static String izquierda()    { return "input keyevent 21"; }
    public static String derecha()      { return "input keyevent 22"; }
    public static String conferencia()  { return "input keyevent 135"; }
    public static String transferencia(){ return "input keyevent 136"; }
    public static String asterisco()    { return "input keyevent 17"; }
    public static String almohadilla()  { return "input keyevent 18"; }
    // Memorias
    public static String memouno()      { return "input keyevent 131"; }
    public static String memodos()      { return "input keyevent 132"; }
    public static String memotres()     { return "input keyevent 133"; }
    public static String memocuatro()   { return "input keyevent 134"; }

    // Números
    public static String cero()         { return "input keyevent 7"; }
    public static String uno()          { return "input keyevent 8"; }
    public static String dos()          { return "input keyevent 9"; }
    public static String tres()         { return "input keyevent 10"; }
    public static String cuatro()       { return "input keyevent 11"; }
    public static String cinco()        { return "input keyevent 12"; }
    public static String seis()         { return "input keyevent 13"; }
    public static String siete()        { return "input keyevent 14"; }
    public static String ocho()         { return "input keyevent 15"; }
    public static String nueve()        { return "input keyevent 16"; }

    // Tiempos
    public static String unSegundo()        { return "sleep 1"; }
    public static String cincoSegundos()    { return "sleep 5"; }
    public static String veinteSegundos()   { return "sleep 20"; }
    public static String dosMinutos()       { return "sleep 120"; }

    // Combinar comandos en secuencia
    public static String secuencia(String... cmds) {
        return String.join(" && ", cmds);
    }

}
