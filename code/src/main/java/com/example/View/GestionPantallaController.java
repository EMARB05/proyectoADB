package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class GestionPantallaController {

    @FXML
    private ComboBox<String> comboResolucion;

    private String serial;
    private ADBService adbService = new ADBService();
    private ScrcpyService scrcpyService = new ScrcpyService();

    private int rotacionActual = 0;

    public void setSerial(String serial) {
        this.serial = serial;
        sincronizarEstadoInicial();
    }

    public void sincronizarEstadoInicial() {
        new Thread(()->{
            try {
                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                String rotStatus = adbService.ejecutarComandoSincrono(serial, "shell settings get system user_rotation");

                Platform.runLater(()->{
                    // Estado inicial Resolución
                    try {
                        String linea = resStatus.trim().lines()
                            .reduce((first,second)->second)
                            .orElse("");
                        String resActual = linea.replaceAll("[^0-9x]", "");
                        comboResolucion.getItems().stream()
                            .filter(item -> item.startsWith(resActual))
                            .findFirst()
                            .ifPresent(match -> comboResolucion.setValue(match));
                    } catch (Exception e) {
                        System.err.println("Error al parsear la resolución: "+resStatus);
                    }
                    // Estado inicial Rotación
                    try {
                        rotacionActual = Integer.parseInt(rotStatus.trim())*90;
                    } catch (NumberFormatException e) {
                        rotacionActual = 0;
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();;
    }

    
    @FXML
    private void onLanzarScrcpy(ActionEvent event) {
        if (serial != null && !serial.isEmpty()) {
            System.out.println("Lanzando Scrcpy para: " + serial);
            scrcpyService.launch(serial);
        } else {
            System.err.println("No hay serial seleccionado para lanzar Scrcpy");
        }
    }

    @FXML
    private void cambiarResolucion(ActionEvent event){
        String seleccion = comboResolucion.getValue();
        if (seleccion == null) return;

        String resolucion = seleccion.split(" ")[0];
        adbService.ejecutarAccionHilo(serial, "shell wm size "+resolucion);
    }

    @FXML
    private void resetearResolucion(ActionEvent event){
       String seleccion = comboResolucion.getValue();
       if (seleccion == null) return;

        adbService.ejecutarAccionHilo(serial, "shell wm size reset");
        new Thread(()->{
            try{
                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                Platform.runLater(()->{
                    String linea = resStatus.trim().lines()
                        .reduce((first,second)->second)
                        .orElse("");
                    String resActual = linea.replaceAll("[^0-9x]", "");
                    comboResolucion.getItems().stream()
                        .filter(item -> item.startsWith(resActual))
                        .findFirst()
                        .ifPresent(match -> comboResolucion.setValue(match));
                });
            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();;
    }

    @FXML
    private void rotarPantalla90(ActionEvent event){
        adbService.ejecutarAccionHilo(serial, "shell settings put system accelerometer_rotation 0");
        rotacionActual = (rotacionActual + 90) % 360;
        adbService.ejecutarAccionHilo(serial, "shell settings put system user_rotation " + (rotacionActual / 90));
    }

    @FXML
    private void flipPantalla(ActionEvent event){
        adbService.ejecutarAccionHilo(serial, "shell settings put system accelerometer_rotation 0");

        int valorSistema = (rotacionActual/90+2)%4;
        rotacionActual = valorSistema*90;
        adbService.ejecutarAccionHilo(serial, "shell settings put system user_rotation "+ valorSistema);
    }

}