package com.example.View;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

import com.example.Controller.ADBService;

public class AjustesController {

    private String serial;

    // Pestañas
    @FXML
    private ToggleButton tabAjustes;
    @FXML
    private ToggleButton tabGestion;
    @FXML
    private ToggleButton tabRed;
    @FXML
    private ToggleButton tabWifi;

    // Paneles
    @FXML
    private VBox paneAjustes;
    @FXML
    private VBox paneGestion;
    @FXML
    private VBox paneRed;
    @FXML
    private VBox paneWifi;

    // Subcontroladores (JavaFX los inyecta automáticamente por convención
    // {fx:id}Controller)
    @FXML
    private AjustesRapidosController ajustesRapidosController;
    @FXML
    private GestionPantallaController gestionPantallaController;
    @FXML
    private RedImsController redImsController;
    @FXML
    private AdbWifiController adbWifiController;
    ADBService adbService= new ADBService();

    private static final String ESTILO_ACTIVO = "-fx-background-color: #1e1e2e; -fx-text-fill: #89b4fa; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-padding: 10 16; -fx-background-radius: 0; -fx-cursor: hand;";

    private static final String ESTILO_INACTIVO = "-fx-background-color: #181825; -fx-text-fill: #6c7086; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-padding: 10 16; -fx-background-radius: 0; -fx-cursor: hand;";

    // --- Navegación entre pestañas ---

    @FXML
    private void onTabAjustes() {
        mostrarPane(paneAjustes);
        actualizarTabs(tabAjustes);
    }

    @FXML
    private void onTabGestion() {
        mostrarPane(paneGestion);
        actualizarTabs(tabGestion);
    }

    @FXML
    private void onTabRed() {
        mostrarPane(paneRed);
        actualizarTabs(tabRed);
    }

    @FXML
    private void onTabWifi() {
        mostrarPane(paneWifi);
        actualizarTabs(tabWifi);
    }

    private void mostrarPane(VBox paneActivo) {
        for (VBox pane : List.of(paneAjustes, paneGestion, paneRed, paneWifi)) {
            boolean activo = pane == paneActivo;
            pane.setVisible(activo);
            pane.setManaged(activo); // Evita que los paneles ocultos ocupen espacio
        }
    }

    private void actualizarTabs(ToggleButton tabActiva) {
        for (ToggleButton tab : List.of(tabAjustes, tabGestion, tabRed, tabWifi)) {
            tab.setStyle(tab == tabActiva ? ESTILO_ACTIVO : ESTILO_INACTIVO);
            tab.setSelected(tab == tabActiva);
        }
    }

    // --- Serial: se propaga a todos los subcontroladores ---

    public void setSerial(String androidId)  {
        try {
             this.serial = adbService.getSerialActivo(androidId) ;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
       
        ajustesRapidosController.setSerial(serial);
        gestionPantallaController.setSerial(serial);
        redImsController.setSerial(serial);
        adbWifiController.setSerial(serial);
    }

    @FXML
    private void onVolver(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}