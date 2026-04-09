package com.example.View;

import com.example.Controller.DispositivoDAO;
import com.example.Controller.MarcaDAO;
import com.example.Controller.ModeloDAO;
import com.example.Controller.SocDAO;
import com.example.Model.Dispositivo;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;


public class FormularioAltaController implements DispositivoAware {

    @FXML private Label     lblSerial;
    @FXML private TextField txtMarca;
    @FXML private TextField txtModelo;
    @FXML private TextField txtSoc;
    @FXML private TextField txtRam;
    @FXML private TextField txtAlmacenamiento;
    @FXML private TextField txtAndroid;
    @FXML private TextField txtPantalla;
    @FXML private TextField txtCamara;
    @FXML private TextArea  txtNotas;

    private final MarcaDAO      marcaDAO      = new MarcaDAO();
    private final SocDAO        socDAO        = new SocDAO();
    private final ModeloDAO     modeloDAO     = new ModeloDAO();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();

    private String serial;

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.serial = dispositivo.getSerialNumber();
        var modelo  = dispositivo.getModelo();
        var marca   = modelo.getMarca();
        var soc     = modelo.getSoc();

        lblSerial.setText(serial);

        // Pre-rellenamos con los datos que ADB nos dio
        if (marca  != null) txtMarca.setText(marca.getNombre());
        if (modelo != null) txtModelo.setText(modelo.getNombreModelo());
        if (soc    != null) txtSoc.setText(soc.getModeloSoc());

        txtRam.setText(modelo.getRamGb() > 0 ? String.valueOf(modelo.getRamGb()) : "");
        txtAndroid.setText(modelo.getSoVersion() != null ? modelo.getSoVersion() : "");
    }

    @FXML
    private void onGuardar() {
        try {
            // 1. Marca
            Marca marca = new Marca(txtMarca.getText().trim(), "");
            int idMarca = marcaDAO.insertar(marca);
            marca.setIdMarca(idMarca);

            // 2. SoC
            Soc soc = new Soc();
            soc.setModeloSoc(txtSoc.getText().trim());
            soc.setFabricante(txtMarca.getText().trim());
            int idSoc = socDAO.insertar(soc);
            soc.setIdSoc(idSoc);

            // 3. Modelo
            Modelo modelo = new Modelo(marca, txtModelo.getText().trim());
            modelo.setSoc(soc);
            modelo.setSoVersion(txtAndroid.getText().trim());
            modelo.setPantallaPulgadas(txtPantalla.getText().trim());
            modelo.setCamaraMp(txtCamara.getText().trim());

            if (!txtRam.getText().isBlank())
                modelo.setRamGb(Integer.parseInt(txtRam.getText().trim()));
            if (!txtAlmacenamiento.getText().isBlank())
                modelo.setAlmacenamientoGb(Integer.parseInt(txtAlmacenamiento.getText().trim()));

            int idModelo = modeloDAO.insertar(modelo);
            modelo.setIdModelo(idModelo);

            // 4. Dispositivo
            Dispositivo dispositivo = new Dispositivo(modelo, serial);
            dispositivo.setNotas(txtNotas.getText().trim());
            dispositivoDAO.insertar(dispositivo);

            mostrarExito();

        } catch (SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarExito() {
        lblSerial.setText("✓ Dispositivo registrado correctamente");
        lblSerial.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 15px;");
    }

    private void mostrarError(String mensaje) {
        lblSerial.setText("✗ Error: " + mensaje);
        lblSerial.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 13px;");
    }
}
