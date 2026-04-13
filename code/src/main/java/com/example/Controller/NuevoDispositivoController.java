package com.example.Controller;

import java.sql.SQLException;
import java.util.List;

import com.example.Controller.MarcaDAO; // Asegúrate de que estos imports apunten a donde creaste los DAOs
import com.example.Controller.SocDAO;
import com.example.Model.Dispositivo;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class NuevoDispositivoController {

    @FXML
    private TextField txtSerial, txtModelo;
    @FXML
    private ComboBox<String> cbMarca, cbSoc;

    // --- DECLARACIÓN DE DAOs (Sin las barras // para que funcionen) ---
    private MarcaDAO marcaDAO = new MarcaDAO();
    private SocDAO socDAO = new SocDAO();

public void setDatosIniciales(Dispositivo dispositivo) {
    if (dispositivo == null || dispositivo.getModelo() == null) return;

    // 1. Rellenamos el Serial (ya es no editable en el FXML)
    txtSerial.setText(dispositivo.getSerialNumber());

    // 2. Rellenamos el nombre del Modelo
    txtModelo.setText(dispositivo.getModelo().getNombreModelo());

    // 3. Seleccionar Marca en el ComboBox
    if (dispositivo.getModelo().getMarca() != null) {
        String marcaADB = dispositivo.getModelo().getMarca().getNombre();
        // Buscamos si la marca que trajo ADB ya existe en nuestro combo (BBDD)
        for (String item : cbMarca.getItems()) {
            if (item.equalsIgnoreCase(marcaADB)) {
                cbMarca.setValue(item);
                break;
            }
        }
    }

    // 4. Seleccionar Procesador (SoC) en el ComboBox
    if (dispositivo.getModelo().getSoc() != null) {
        String socADB = dispositivo.getModelo().getSoc().getModeloSoc();
        for (String item : cbSoc.getItems()) {
            if (item.equalsIgnoreCase(socADB)) {
                cbSoc.setValue(item);
                break;
            }
        }
    }
}
    @FXML
    public void initialize() {
        try {
            // 1. Cargamos Marcas
            List<String> nombresMarcas = marcaDAO.obtenerNombres();
            cbMarca.getItems().setAll(nombresMarcas);

            // 2. Cargamos SoCs (Añadimos esto para que no se quede vacío el segundo combo)
            List<String> nombresSocs = socDAO.obtenerModelosSocs();
            cbSoc.getItems().setAll(nombresSocs);

            System.out.println("Combos cargados: " + nombresMarcas.size() + " marcas y " + nombresSocs.size() + " procesadores.");

        } catch (Exception e) { // Cambiado a Exception genérica por si los DAOs no lanzan SQLException explícita
            System.err.println("Error al cargar los datos en los combos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuardar() {
        String serial = txtSerial.getText();
        String marcaSeleccionada = cbMarca.getValue();
        String modeloNombre = txtModelo.getText();
        String socSeleccionado = cbSoc.getValue();

        if (marcaSeleccionada == null || modeloNombre.isEmpty()) {
            System.out.println("Error: Rellena los campos obligatorios");
            return;
        }

        // De momento dejamos los prints para probar que captura bien los datos
        System.out.println("Intentando registrar:");
        System.out.println("Serial: " + serial + " | Marca: " + marcaSeleccionada + " | Modelo: " + modeloNombre);

        // Aquí irá la lógica de inserción real cuando tengamos los DAOs de Modelo y Dispositivo
        ((Stage) txtSerial.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancelar() {
        ((Stage) txtSerial.getScene().getWindow()).close();
    }
}