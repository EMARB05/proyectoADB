package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class GestionPantallaController {

    private String serial;
    private ADBService adbService = new ADBService();
    private ScrcpyService scrcpyService = new ScrcpyService();

    @FXML
    public void initialize() {
        // Inicialización futura
    }

        // 2. Crea el método que el FXML está buscando
    @FXML
    private void onLanzarScrcpy(ActionEvent event) {
        if (serial != null && !serial.isEmpty()) {
            System.out.println("Lanzando Scrcpy para: " + serial);
            scrcpyService.launch(serial);
        } else {
            System.err.println("No hay serial seleccionado para lanzar Scrcpy");
        }
    }

    public void setSerial(String serial) {
        this.serial = serial;
        // Aquí puedes sincronizar estado inicial cuando se asigne el serial
    }
}