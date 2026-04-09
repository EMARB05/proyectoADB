package com.example.View;

import com.example.Controller.BandaDAO;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import java.sql.SQLException;
import java.util.List;

public class FichaTecnicaController implements DispositivoAware {

    @FXML
    private ImageView imgDispositivo;
    @FXML
    private Label lblNombreModelo;
    @FXML
    private Label lblMarca;
    @FXML
    private Label lblSerial;
    @FXML
    private Label lblFechaRegistro;
    @FXML
    private Label lblSoc;
    @FXML
    private Label lblSocFabricante;
    @FXML
    private Label lblRam;
    @FXML
    private Label lblAlmacenamiento;
    @FXML
    private Label lblAndroid;
    @FXML
    private Label lblPantalla;
    @FXML
    private Label lblCamara;
    @FXML
    private Label lblNotas;
    @FXML
    private FlowPane panelBandas;

    private final BandaDAO bandaDAO = new BandaDAO();

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        var modelo = dispositivo.getModelo();
        var marca = modelo.getMarca();
        var soc = modelo.getSoc();

        lblNombreModelo.setText(modelo.getNombreModelo());
        lblMarca.setText(marca != null ? marca.getNombre() : "—");
        lblSerial.setText("Serial: " + dispositivo.getSerialNumber());
        lblFechaRegistro.setText("Registrado: " +
                (dispositivo.getFechaRegistro() != null ? dispositivo.getFechaRegistro() : "—"));

        lblSoc.setText(soc != null ? soc.getModeloSoc() : "—");
        lblSocFabricante.setText(soc != null ? soc.getFabricante() : "—");
        lblRam.setText(modelo.getRamGb() > 0 ? modelo.getRamGb() + " GB" : "—");
        lblAlmacenamiento.setText(modelo.getAlmacenamientoGb() > 0 ? modelo.getAlmacenamientoGb() + " GB" : "—");
        lblAndroid.setText(modelo.getSoVersion() != null ? modelo.getSoVersion() : "—");
        lblPantalla.setText(modelo.getPantallaPulgadas() != null ? modelo.getPantallaPulgadas() + "\"" : "—");
        lblCamara.setText(modelo.getCamaraMp() != null ? modelo.getCamaraMp() + " MP" : "—");
        lblNotas.setText(dispositivo.getNotas() != null ? dispositivo.getNotas() : "—");

        cargarFoto(dispositivo);
        cargarBandas(modelo.getIdModelo());
    }

    private void cargarFoto(Dispositivo dispositivo) {
        var modelo = dispositivo.getModelo();
        if (modelo.getFotos() != null && !modelo.getFotos().isEmpty()) {
            var foto = modelo.getFotos().get(0);

            if (foto.getUrlExterna() != null) {
                // Foto como ruta de archivo
                imgDispositivo.setImage(
                        new Image("file:" + foto.getUrlExterna()));
            }
        } else {
            // Icono por defecto si no hay foto
            imgDispositivo.setImage(
                    new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg")));
        }
    }

    private void cargarBandas(int idModelo) {
        panelBandas.getChildren().clear();
        try {
            List<Banda> bandas = bandaDAO.obtenerPorModelo(idModelo);
            for (Banda banda : bandas) {
                Label chip = new Label(banda.getTipo() + " " + banda.getNumeroBanda());
                chip.setStyle(
                        "-fx-background-color: #313244; -fx-text-fill: #89b4fa;" +
                                "-fx-padding: 4 10 4 10; -fx-background-radius: 20;" +
                                "-fx-font-size: 12px;");
                panelBandas.getChildren().add(chip);
            }

            if (bandas.isEmpty()) {
                panelBandas.getChildren().add(
                        new Label("Sin bandas registradas") {
                            {
                                setStyle("-fx-text-fill: #6c7086; -fx-font-size: 12px;");
                            }
                        });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}