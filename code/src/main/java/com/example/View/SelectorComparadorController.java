package com.example.View;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;

public class SelectorComparadorController {
    @FXML
    private ComboBox<String> comboModo;
    @FXML
    private StackPane contenedorDinamico;
    private NavegacionHandler navegacionHandler;

    public void setNavegacionHandler(NavegacionHandler handler) {
        this.navegacionHandler = handler;
    }

    public void initialize() {
        comboModo.getItems().addAll("XML + Excel", "2 XMLs");

        comboModo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #cdd6f4;");
                }
            }
        });

        comboModo.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if ("XML + Excel".equals(nuevo)) {
                cargarSubVista("/fxml/comparador_excel.fxml");
            } else {
                cargarSubVista("/fxml/comparador_xml.fxml");
            }
        });

        // Carga por defecto
        comboModo.getSelectionModel().selectFirst();
    }

    public void cargarSubVista(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Node nodoHijo = loader.load();
            Object controllerHijo = loader.getController();
            if (controllerHijo instanceof ComparadorExcelController) {
                ((ComparadorExcelController) controllerHijo).setSelectorHandler(this);

            }
            contenedorDinamico.getChildren().setAll(nodoHijo);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void cargarSubVista(String fxml,String mensaje) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Node nodoHijo = loader.load();
            Object controllerHijo = loader.getController();
            if (controllerHijo instanceof ComparadorExcelController) {
                ((ComparadorExcelController) controllerHijo).setSelectorHandler(this);
            }
            contenedorDinamico.getChildren().setAll(nodoHijo);
            navegacionHandler.mostrarToast(mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
