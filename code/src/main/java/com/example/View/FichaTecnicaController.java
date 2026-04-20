package com.example.View;

import com.example.Controller.BandaDAO;
import com.example.Controller.FotoDAO;
import com.example.Controller.LogcatManager;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Foto;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
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
    private Label lblAndroidId;
    @FXML
    private Label lblPantalla;
    @FXML
    private Label lblCamara;
    @FXML
    private Label lblNotas;
    @FXML
    private FlowPane panelBandas;
    @FXML
    private StackPane rootPane;

    private final LogcatManager logcatManager = new LogcatManager();
    private Dispositivo dispositivoActual;
    private final BandaDAO bandaDAO = new BandaDAO();
    private final FotoDAO fotoDAO = new FotoDAO();

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivoActual = dispositivo;

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
        lblAndroidId.setText("Android ID: " + dispositivo.getAndroid_id());

        cargarFoto(dispositivo);
        cargarBandas(modelo.getIdModelo());

        iniciarLogcat(dispositivo.getSerialNumber());
    }

    private void cargarFoto(Dispositivo dispositivo) {
        try {
            // FORZAMOS la carga de la foto desde la DB usando el ID del modelo
            List<Foto> fotos = fotoDAO.obtenerPorModelo(dispositivo.getModelo().getIdModelo());

            if (fotos != null && !fotos.isEmpty()) {
                String ruta = fotos.get(0).getUrlExterna();

                if (ruta != null && !ruta.isEmpty()) {
                    File file = new File(ruta);
                    if (file.exists()) {
                        // Usamos la URI del archivo para evitar problemas de formato
                        imgDispositivo.setImage(new Image(file.toURI().toString()));
                        return; // Si todo sale bien, salimos del método
                    }
                }
            }

            // Si llegamos aquí es porque no hay foto o el archivo no existe
            imgDispositivo.setImage(new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg")));

        } catch (SQLException e) {
            e.printStackTrace();
            // En caso de error de DB, ponemos el placeholder
            imgDispositivo.setImage(new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg")));
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

    @FXML
    private void onAbrirAjustes() {
        try {
            // 1. Cargamos el archivo FXML de la ventana de ajustes
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajustes_dispositivos.fxml"));
            Parent root = loader.load();

            // 2. Obtenemos el controlador de la ventana de ajustes
            AjustesController controller = loader.getController();

            // 3. Le pasamos el SERIAL del dispositivo actual
            // Limpiamos el texto del label por si tiene el prefijo "Serial: "
            // BIEN — pasa el android_id directamente del objeto dispositivo
            controller.setSerial(dispositivoActual.getAndroid_id());

            // 4. Creamos y configuramos la nueva ventana (Stage)
            Stage stage = new Stage();
            stage.setTitle("Gestión de Dispositivo - " + lblNombreModelo.getText());

            // Esto hace que no se pueda interactuar con la ventana principal hasta cerrar
            // esta
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setScene(new Scene(root));
            stage.setMinWidth(400);
            stage.setMinHeight(300);
            stage.show();

        } catch (IOException e) {
            System.err.println("Error al cargar la ventana de ajustes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirLaboratorio() {
        try {
            // Asegúrate de que la ruta empiece por / si está en resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LaboratorioBateria.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Laboratorio de Rendimiento");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniciarLogcat(String serial) {
        // Solo reinicia si el dispositivo cambió o no hay captura activa
        if (logcatManager.isActivo())
            return;

        try {
            logcatManager.iniciar(serial);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAbrirLogcat() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/logcat.fxml"));
        Scene scene = new Scene(loader.load(), 700, 480);

        // Pasamos el logcatManager y el dispositivo al controlador del filtro
        LogcatController controller = loader.getController();
        controller.setLogcatManager(logcatManager, dispositivoActual);

        Stage stage = new Stage();
        stage.setTitle("Logcat — " + dispositivoActual.getSerialNumber());
        stage.setScene(scene);
        stage.initModality(Modality.NONE);
        stage.setOnCloseRequest(null);

        stage.show();
    }
}