package com.example.View;

import java.util.function.Consumer;

import com.example.Model.Dispositivo;

public interface NavegacionHandler {
    void cambiarVistaCentral(String fxmlPath, Dispositivo dispositivo, Consumer<Dispositivo> alFinalizar);
}
