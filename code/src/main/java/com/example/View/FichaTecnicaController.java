package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.BandaDAO;
import com.example.Controller.FotoDAO;
import com.example.Controller.LogcatManager;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Foto;

import javafx.application.Platform;
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class FichaTecnicaController implements DispositivoAware {

    @FXML private ImageView imgDispositivo;
    @FXML private Label lblNombreModelo;
    @FXML private Label lblMarca;
    @FXML private Label lblSerial;
    @FXML private Label lblFechaRegistro;
    @FXML private Label lblSoc;
    @FXML private Label lblSocFabricante;
    @FXML private Label lblRam;
    @FXML private Label lblAlmacenamiento;
    @FXML private Label lblAndroid;
    @FXML private Label lblAndroidId;
    @FXML private Label lblPantalla;
    @FXML private Label lblCamara;
    @FXML private Label lblNotas;
    @FXML private FlowPane panelBandas;
    @FXML private StackPane rootPane;

    private final LogcatManager logcatManager = new LogcatManager();
    private Dispositivo dispositivoActual;
    private final BandaDAO bandaDAO = new BandaDAO();
    private final FotoDAO fotoDAO = new FotoDAO();
    private final ADBService adb = new ADBService();

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
        lblPantalla.setText(modelo.getResolucionPantalla() != null ? modelo.getResolucionPantalla() + "\"" : "—");
        lblCamara.setText(modelo.getCamaraMp() != null ? modelo.getCamaraMp() + " MP" : "—");
        lblNotas.setText(dispositivo.getNotas() != null ? dispositivo.getNotas() : "—");
        lblAndroidId.setText("Android ID: " + dispositivo.getAndroid_id());

        new Thread(() -> {
            Platform.runLater(() -> {
                cargarFoto(dispositivo);
            });
        }).start();
        new Thread(() -> {
            Platform.runLater(() -> {
                cargarBandas(modelo.getIdModelo());
            });
        }).start();
        new Thread(() -> {
            Platform.runLater(() -> {
                iniciarLogcat(dispositivo.getAndroid_id());
            });
        }).start();
    }

    // ───────────────────── FOTO ─────────────────────
    private void cargarFoto(Dispositivo dispositivo) {
        try {
            List<Foto> fotos = fotoDAO.obtenerPorModelo(dispositivo.getModelo().getIdModelo());
            if (fotos != null && !fotos.isEmpty()) {
                String ruta = fotos.get(0).getUrlExterna();
                if (ruta != null && !ruta.isEmpty()) {
                    File file = new File(ruta);
                    if (file.exists()) {
                        imgDispositivo.setImage(new Image(file.toURI().toString()));
                        return;
                    }
                }
            }
            imgDispositivo.setImage(new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg")));
        } catch (SQLException e) {
            e.printStackTrace();
            imgDispositivo.setImage(new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg")));
        }
    }

    // ───────────────────── BANDAS ─────────────────────
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
                Label vacio = new Label("Sin bandas registradas");
                vacio.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 12px;");
                panelBandas.getChildren().add(vacio);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ───────────────────── AJUSTES ─────────────────────
    @FXML
    private void onAbrirAjustes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajustes_dispositivos.fxml"));
            Parent root = loader.load();

            AjustesController controller = loader.getController();
            controller.setSerial(dispositivoActual.getAndroid_id());

            Stage stage = new Stage();
            stage.setTitle("Gestión de Dispositivo - " + lblNombreModelo.getText());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(400);
            stage.setMinHeight(300);
            stage.show();

        } catch (IOException e) {
            System.err.println("[FICHA] Error al cargar ajustes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ───────────────────── LABORATORIO ─────────────────────
    @FXML
    private void abrirLaboratorio() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LaboratorioBateria.fxml"));
            Parent root = loader.load();

            // Pasa el android_id para que el laboratorio resuelva el serial activo
            LaboratorioController controller = loader.getController();
            controller.setSerial(dispositivoActual.getAndroid_id());

            Stage stage = new Stage();
            stage.setTitle("Laboratorio de Rendimiento — " + dispositivoActual.getModelo().getNombreModelo());
            stage.setScene(new Scene(root));
            // Para el monitor al cerrar la ventana
            stage.setOnCloseRequest(e -> controller.stop());
            stage.show();

        } catch (IOException e) {
            System.err.println("[FICHA] Error al cargar laboratorio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ───────────────────── LOGCAT ─────────────────────
    private void iniciarLogcat(String androidId) {
        if (logcatManager.isActivo()) return;
        try {
            String serialActivo = adb.getSerialActivo(androidId);
            logcatManager.iniciar(serialActivo);
        } catch (IOException e) {
            System.err.println("[FICHA] Error al iniciar logcat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onAbrirLogcat() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logcat.fxml"));
        Scene scene = new Scene(loader.load(), 700, 480);

        LogcatController controller = loader.getController();
        controller.setLogcatManager(logcatManager, dispositivoActual);

        Stage stage = new Stage();
        stage.setTitle("Logcat — " + dispositivoActual.getAndroid_id());
        stage.setScene(scene);
        stage.initModality(Modality.NONE);
        stage.setOnCloseRequest(null);
        stage.show();
    }
}