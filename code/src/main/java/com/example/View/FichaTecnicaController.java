package com.example.View;

import com.example.Controller.ADBService;
import com.example.Controller.BandaDAO;
import com.example.Controller.DispositivoDAO;
import com.example.Controller.FotoDAO;
import com.example.Controller.LogcatManager;
import com.example.Controller.ModeloDAO;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Foto;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    @FXML
    private TextField txtAndroid;
    @FXML
    private TextField txtCamara;
    @FXML
    private TextField txtPantalla;
    @FXML
    private javafx.scene.control.TextArea txtNotasEdit;
    @FXML
    private Button btnEditarAndroid;
    @FXML
    private Button btnEditarCamara;
    @FXML
    private Button btnEditarPantalla;
    @FXML
    private Button btnEditarNotas;
    @FXML
    private Button btnGuardarNotas;
    // Añade esto a tus variables @FXML
    @FXML
    private VBox hoverOverlay;

    private final ModeloDAO modeloDAO = new ModeloDAO();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
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

        // 1. CARGA INSTANTÁNEA (Datos que ya vienen en el objeto)
        lblNombreModelo.setText(modelo.getNombreModelo());
        lblMarca.setText(marca != null ? marca.getNombre() : "—");
        lblSerial.setText("Serial: " + dispositivo.getSerialNumber());
        lblFechaRegistro.setText(
                "Registrado: " + (dispositivo.getFechaRegistro() != null ? dispositivo.getFechaRegistro() : "—"));
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
            cargarFoto(dispositivo);
            imgDispositivo.getParent().setOnMouseEntered(e -> hoverOverlay.setVisible(true));
            imgDispositivo.getParent().setOnMouseExited(e -> hoverOverlay.setVisible(false));
            Platform.runLater(() -> {
                cargarBandas(modelo.getIdModelo());
                iniciarLogcat(dispositivo.getAndroid_id());
            });
        }).start();
    }

    @FXML
    private void onEditarAndroid() {
        activarEdicion(lblAndroid, txtAndroid, btnEditarAndroid);
    }

    @FXML
    private void onEditarCamara() {
        activarEdicion(lblCamara, txtCamara, btnEditarCamara);
    }

    @FXML
    private void onEditarPantalla() {
        activarEdicion(lblPantalla, txtPantalla, btnEditarPantalla);
    }

    @FXML
    private void onEditarNotas() {
        activarEdicionArea(lblNotas, txtNotasEdit, btnEditarNotas);
    }

    @FXML
    private void onEditarFoto() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Modelo");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File archivoSeleccionado = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (archivoSeleccionado != null) {
            actualizarFotoModelo(archivoSeleccionado);
        }
    }

    private void actualizarFotoModelo(File nuevaFoto) {
        try {
            int idModelo = dispositivoActual.getModelo().getIdModelo();
            String nuevaRuta = nuevaFoto.getAbsolutePath();

            // 1. Obtener la foto actual para saber si insertar o actualizar
            List<Foto> fotosExistentes = fotoDAO.obtenerPorModelo(idModelo);

            if (fotosExistentes.isEmpty()) {
            } else {
                fotoDAO.eliminar(fotosExistentes.get(0).getIdFoto());
            }

            Foto fotoNueva = new Foto();
            fotoNueva.setIdModelo(idModelo);
            fotoNueva.setUrlExterna(nuevaRuta);
            fotoNueva.setDescripcion("Foto de " + dispositivoActual.getModelo().getNombreModelo());
            fotoDAO.insertar(fotoNueva);

            imgDispositivo.setImage(new Image(nuevaFoto.toURI().toString()));
            mostrarToast("✓ Foto del modelo actualizada");

        } catch (Exception e) {
            mostrarToast("✗ Error al actualizar la foto");
            e.printStackTrace();
        }
    }

    private void activarEdicion(Label label, TextField campo, Button btn) {
        // Extraemos el valor actual quitando unidades (GB, MP, ")
        String valorActual = label.getText()
                .replace(" GB", "")
                .replace(" MP", "")
                .replace("\"", "")
                .replace("—", "");

        campo.setText(valorActual);
        label.setVisible(false);
        label.setManaged(false);
        campo.setVisible(true);
        campo.setManaged(true);
        campo.requestFocus();
        btn.setText("✕");
        btn.setOnAction(e -> cancelarEdicion(label, campo, btn));
    }

    private void activarEdicionArea(Label label, TextArea campo, Button btn) {
        campo.setText(label.getText().equals("—") ? "" : label.getText());
        label.setVisible(false);
        label.setManaged(false);
        campo.setVisible(true);
        campo.setManaged(true);
        btnGuardarNotas.setVisible(true);
        btnGuardarNotas.setManaged(true);
        campo.requestFocus();
        btn.setText("✕");
        btn.setOnAction(e -> cancelarEdicion(label, campo, btn));
    }

    private void cancelarEdicion(Label label, Control campo, Button btn) {
        campo.setVisible(false);
        campo.setManaged(false);
        btnGuardarNotas.setVisible(false);
        btnGuardarNotas.setManaged(false);
        label.setVisible(true);
        label.setManaged(true);
        btn.setText("✏");
        btn.setOnAction(e -> {
            if (campo instanceof TextField)
                activarEdicion(label, (TextField) campo, btn);
            else
                activarEdicionArea(label,
                        (javafx.scene.control.TextArea) campo, btn);
        });
    }

    // ───────────────────── FOTO ─────────────────────
    private void cargarFoto(Dispositivo dispositivo) {
        try {
            List<Foto> fotos = fotoDAO.obtenerPorModelo(dispositivo.getModelo().getIdModelo());
            Image imagenFinal;

            if (fotos != null && !fotos.isEmpty()) {
                String ruta = fotos.get(0).getUrlExterna();
                File file = new File(ruta);
                if (file.exists()) {
                    imagenFinal = new Image(file.toURI().toString());
                } else {
                    imagenFinal = new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg"));
                }
            } else {
                imagenFinal = new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg"));
            }

            // USAMOS PLATFORM.RUNLATER PARA ASIGNAR LA IMAGEN
            Platform.runLater(() -> imgDispositivo.setImage(imagenFinal));

        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> imgDispositivo
                    .setImage(new Image(getClass().getResourceAsStream("/img/device_placeholder.jpg"))));
        }
    }

    // ───────────────────── BANDAS ─────────────────────
    private void cargarBandas(int idModelo) {
        try {
            List<Banda> bandas = bandaDAO.obtenerPorModelo(idModelo);

            Platform.runLater(() -> {
                panelBandas.getChildren().clear();

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
            });
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
            stage.setMinWidth(485);
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
        if (logcatManager.isActivo())
            return;
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

    @FXML
    private void onGuardarAndroid() {
        String valor = txtAndroid.getText().trim();
        try {
            modeloDAO.actualizarCampo(dispositivoActual.getModelo().getIdModelo(), "so_version", valor);
            dispositivoActual.getModelo().setSoVersion(valor);
            lblAndroid.setText(valor.isEmpty() ? "—" : valor);
            cancelarEdicion(lblAndroid, txtAndroid, btnEditarAndroid);
            mostrarToast("✓ Versión Android actualizada");
        } catch (SQLException e) {
            mostrarToast("✗ Error al guardar");
            e.printStackTrace();
        }
    }

    @FXML
    private void onGuardarCamara() {
        String valor = txtCamara.getText().trim();
        try {
            modeloDAO.actualizarCampo(dispositivoActual.getModelo().getIdModelo(), "camara_mp", valor);
            dispositivoActual.getModelo().setCamaraMp(valor);
            lblCamara.setText(valor.isEmpty() ? "—" : valor + " MP");
            cancelarEdicion(lblCamara, txtCamara, btnEditarCamara);
            mostrarToast("✓ Cámara actualizada");
        } catch (SQLException e) {
            mostrarToast("✗ Error al guardar");
            e.printStackTrace();
        }
    }

    @FXML
    private void onGuardarPantalla() {
        String valor = txtPantalla.getText().trim();
        try {
            modeloDAO.actualizarCampo(dispositivoActual.getModelo().getIdModelo(), "resolucion_pantalla", valor);
            dispositivoActual.getModelo().setResolucionPantalla(valor);
            lblPantalla.setText(valor.isEmpty() ? "—" : valor + "\"");
            cancelarEdicion(lblPantalla, txtPantalla, btnEditarPantalla);
            mostrarToast("✓ Pantalla actualizada");
        } catch (SQLException e) {
            mostrarToast("✗ Error al guardar");
            e.printStackTrace();
        }
    }

    @FXML
    private void onGuardarNotas() {
        String valor = txtNotasEdit.getText().trim();
        try {
            dispositivoActual.setNotas(valor);
            dispositivoDAO.actualizar(dispositivoActual);
            lblNotas.setText(valor.isEmpty() ? "—" : valor);
            cancelarEdicion(lblNotas, txtNotasEdit, btnEditarNotas);
            mostrarToast("✓ Notas actualizadas");
        } catch (SQLException e) {
            mostrarToast("✗ Error al guardar");
            e.printStackTrace();
        }
    }

    public void mostrarToast(String mensaje) {
        if (rootPane == null)
            return;
        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));
        rootPane.getChildren().add(toast);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400),
                toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));
        new javafx.animation.SequentialTransition(fadeIn, pausa, fadeOut).play();
    }

 


}