package com.example.View;
 
import com.example.Controller.ADBService;
import javafx.fxml.FXML;
 
public class RedImsController {
 
    private String serial;
    private ADBService adbService = new ADBService();
 
    @FXML
    public void initialize() {
        // Inicialización futura
    }
 
    public void setSerial(String serial) {
        this.serial = serial;
    }
}
 