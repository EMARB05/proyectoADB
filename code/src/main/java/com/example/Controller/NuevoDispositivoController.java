package com.example.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class NuevoDispositivoController {

    @FXML private TextField txtSerial, txtModelo;
    @FXML private ComboBox<String> cbMarca, cbSoc;

    // Aquí llamaréis a vuestros DAOs para rellenar los combos
    // MarcaDAO marcaDAO = new MarcaDAO();

    public void setSerial(String serial) {
        txtSerial.setText(serial);
    }

    @FXML
    private void initialize() {
        // Esto se ejecuta al abrir la ventana
        // Aquí deberíais cargar los nombres de las marcas:
        // cbMarca.getItems().addAll(marcaDAO.obtenerTodasLasMarcas());
    }

    @FXML
    private void handleGuardar() {
        // Lógica para crear el objeto Dispositivo y llamar al DAO.insert()
        System.out.println("Guardando: " + txtModelo.getText());
        ((Stage) txtSerial.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancelar() {
        ((Stage) txtSerial.getScene().getWindow()).close();
    }
}