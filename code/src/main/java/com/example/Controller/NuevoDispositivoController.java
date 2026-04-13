package com.example.Controller;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import com.example.Controller.MarcaDAO; // Asegúrate de que estos imports apunten a donde creaste los DAOs
import com.example.Controller.SocDAO;
import com.example.Model.Dispositivo;
import com.example.Model.Foto;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class NuevoDispositivoController {

    @FXML
    private TextField txtSerial, txtModelo;
    @FXML
    private ComboBox<String> cbMarca, cbSoc;
    @FXML
    private TextField txtRutaFoto, txtBandas;

    // --- DECLARACIÓN DE DAOs (Sin las barras // para que funcionen) ---
    private MarcaDAO marcaDAO = new MarcaDAO();
    private SocDAO socDAO = new SocDAO();
    private ModeloDAO modeloDAO= new ModeloDAO();
    private DispositivoDAO dispositivoDAO= new DispositivoDAO();
    private FotoDAO fotoDAO= new FotoDAO();
    private BandaDAO bandaDAO= new BandaDAO();

    public void setDatosIniciales(Dispositivo dispositivo) {
        if (dispositivo == null || dispositivo.getModelo() == null)
            return;

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

            System.out.println(
                    "Combos cargados: " + nombresMarcas.size() + " marcas y " + nombresSocs.size() + " procesadores.");

        } catch (Exception e) { // Cambiado a Exception genérica por si los DAOs no lanzan SQLException
                                // explícita
            System.err.println("Error al cargar los datos en los combos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSeleccionarFoto() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Dispositivo");

        // Filtros para que solo elija imágenes
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(txtSerial.getScene().getWindow());

        if (selectedFile != null) {
            // Guardamos la ruta absoluta para meterla luego en la BBDD
            txtRutaFoto.setText(selectedFile.getAbsolutePath());
        }
    }

   @FXML
private void handleGuardar() {
    // 1. Recoger datos de la interfaz
    String serial = txtSerial.getText();
    String marcaNombre = cbMarca.getValue();
    String modeloNombre = txtModelo.getText();
    String socNombre = cbSoc.getValue();
    String rutaFoto = txtRutaFoto.getText();
    String bandasTexto = txtBandas.getText();

    // 2. Validación básica
    if (marcaNombre == null || modeloNombre.isEmpty() || serial.isEmpty()) {
        mostrarAlerta("Error", "Marca, Modelo y Serial son obligatorios.");
        return;
    }

    try {
        // --- PASO 1: OBTENER OBJETOS DE APOYO ---
        // Necesitamos los IDs reales de la marca y el SoC que seleccionó el usuario
        Marca marca = marcaDAO.buscarPorNombre (marcaNombre);
        Soc soc = socDAO.buscarPorNombre(socNombre);

        // --- PASO 2: CREAR E INSERTAR EL MODELO ---
        Modelo nuevoModelo = new Modelo();
        nuevoModelo.setNombreModelo(modeloNombre);
        nuevoModelo.setMarca(marca);
        nuevoModelo.setSoc(soc);
        // El método insertar debe devolver el ID generado por SQLite
        int idModeloGenerado = modeloDAO.insertar(nuevoModelo); 

        // --- PASO 3: INSERTAR EL DISPOSITIVO ---
        Dispositivo d = new Dispositivo();
        d.setSerialNumber(serial);
        d.setEstado("STOCK"); // Por defecto al registrar
        nuevoModelo.setIdModelo(idModeloGenerado);
        d.setModelo(nuevoModelo);
        
        dispositivoDAO.insertar(d);

        // --- PASO 4: INSERTAR LA FOTO (Si hay ruta) ---
        if (!rutaFoto.isEmpty() && !rutaFoto.equals("No seleccionada...")) {
            Foto f = new Foto();
            f.setUrl(rutaFoto);
            f.setDescripcion("Foto principal");
            fotoDAO.insertar(idModeloGenerado, f); // Método que debes tener en FotoDAO
        }

        // --- PASO 5: INSERTAR BANDAS ---
        if (!bandasTexto.isEmpty()) {
            String[] listaBandas = bandasTexto.split(",");
            for (String bNombre : listaBandas) {
                // Buscamos si la banda existe, si no, la creamos y vinculamos
                int idBanda = bandaDAO.obtenerOCrear(bNombre.trim());
                bandaDAO.vincularModeloBanda(idModeloGenerado, idBanda);
            }
        }

        System.out.println("✅ Dispositivo registrado con éxito en todas las tablas.");
        ((Stage) txtSerial.getScene().getWindow()).close();

    } catch (SQLException e) {
        mostrarAlerta("Error de Base de Datos", e.getMessage());
        e.printStackTrace();
    }
}

private void mostrarAlerta(String titulo, String mensaje) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(titulo);
    alert.setHeaderText(null);
    alert.setContentText(mensaje);
    alert.showAndWait();
}

    @FXML
    private void handleCancelar() {
        ((Stage) txtSerial.getScene().getWindow()).close();
    }
}