package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class AjustesController {
    
    private String serial; // Se debe asignar al abrir la ventana
    private ADBService adbService = new ADBService();
    private ScrcpyService scrcpyService = new ScrcpyService();

    @FXML private Slider sliderBrillo;
    @FXML private Slider sliderVolumen;
    private double ultimoValorEntero = 0;

   @FXML
public void initialize() {
    // --- BRILLO (Rango 0-255) ---
    sliderBrillo.setMin(0);
    sliderBrillo.setMax(255);
    sliderBrillo.valueProperty().addListener((obs, oldVal, newVal) -> {
        int valor = newVal.intValue();
        adbService.ejecutarAccionHilo(serial, "shell settings put system screen_brightness " + valor);
    });

    // --- VOLUMEN (Rango 0-15) ---
    sliderVolumen.setMin(0);
    sliderVolumen.setMax(15);
    sliderVolumen.setBlockIncrement(1);

    sliderVolumen.valueProperty().addListener((obs, oldVal, newVal) -> {
        int actual = newVal.intValue();
        
        // Solo actuamos si el valor ha cambiado de unidad (evita ráfagas)
        if (actual != ultimoValorEntero) {
            if (actual > ultimoValorEntero) {
                // El usuario subió el slider -> Mandamos tecla de subir
                adbService.ejecutarAccionHilo(serial, "shell input keyevent 24");
            } else {
                // El usuario bajó el slider -> Mandamos tecla de bajar
                adbService.ejecutarAccionHilo(serial, "shell input keyevent 25");
            }
            ultimoValorEntero = actual;
        }
    });
}

    @FXML
    private void toggleBluetooth(ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        String estado = btn.isSelected() ? "enable" : "disable";
        adbService.ejecutarAccionHilo(serial, "shell cmd bluetooth_manager " + estado);
    }

    @FXML
    private void toggleAirplane(ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        int valor = btn.isSelected() ? 1 : 0;
        adbService.ejecutarAccionHilo(serial, "shell settings put global airplane_mode_on " + valor);
        // Comando necesario para refrescar el estado en el móvil
        adbService.ejecutarAccionHilo(serial, "shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + (btn.isSelected() ? "true" : "false"));
    }

    @FXML
    private void toggleGPS(ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        // En Android moderno se usa 'location'
        adbService.ejecutarAccionHilo(serial, "shell settings put secure location_mode " + (btn.isSelected() ? "3" : "0"));
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
    }


    

    @FXML
private void onVolver(ActionEvent event) {
    // Esto cierra la ventana actual de ajustes
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.close();
}
}