package com.example.View;

import com.example.Controller.BandaDAO;
import com.example.Controller.DispositivoDAO;
import com.example.Controller.FotoDAO;
import com.example.Controller.MarcaDAO;
import com.example.Controller.ModeloDAO;
import com.example.Controller.SocDAO;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Foto;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.File;
import java.sql.SQLException;
import java.util.function.Consumer;

public class FormularioAltaController implements DispositivoAware {

    @FXML
    private Label lblSerial;
    @FXML
    private Label lblInfo;
    @FXML
    private VBox vboxMarca;
    @FXML
    private TextField txtMarca;
    @FXML
    private VBox vboxModelo;
    @FXML
    private TextField txtModelo;
    @FXML
    private VBox vboxSoc;
    @FXML
    private TextField txtSoc;
    @FXML
    private VBox vboxRAM;
    @FXML
    private TextField txtRam;
    @FXML
    private VBox vboxAlmacenamiento;
    @FXML
    private TextField txtAlmacenamiento;
    @FXML
    private VBox vboxAndroid;
    @FXML
    private TextField txtAndroid;
    @FXML
    private VBox vboxPantalla;
    @FXML
    private TextField txtPantalla;
    @FXML
    private VBox vboxCamara;
    @FXML
    private TextField txtCamara;
    @FXML
    private TextArea txtNotas;
    @FXML
    private VBox camposFoto;
    @FXML
    private TextField txtRutaFoto;

    @FXML
    private TextField txtAndroidID;

    // Referencia al panel raíz de la app para anclar el Toast
    private StackPane rootPane;

    // Callback que MainController inyecta: recibe el Dispositivo guardado
    // y lo muestra en la ficha técnica
    private Consumer<Dispositivo> onGuardadoExitoso;

    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final SocDAO socDAO = new SocDAO();
    private final ModeloDAO modeloDAO = new ModeloDAO();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private final FotoDAO fotoDAO = new FotoDAO();
    private final BandaDAO bandaDAO = new BandaDAO();

    private String serial;
    private Dispositivo dispositivoDesdeAdb;

    // MainController llama a este método justo después de cargar el FXML
    public void setOnGuardadoExitoso(Consumer<Dispositivo> callback) {
        this.onGuardadoExitoso = callback;
    }

    // MainController llama a este método para que el Toast se ancle
    // en el StackPane raíz de la ventana (por encima de todo)
    public void setRootPane(StackPane rootPane) {
        this.rootPane = rootPane;
    }

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivoDesdeAdb = dispositivo;
        this.serial = dispositivo.getSerialNumber();
        var modelo = dispositivo.getModelo();
        var marca = modelo.getMarca();
        var soc = modelo.getSoc();
        var android_id = dispositivo.getAndroid_id();

        lblSerial.setText(serial);

        if (marca != null)
            txtMarca.setText(marca.getNombre());
        if (modelo != null)
            txtModelo.setText(modelo.getNombreModelo());
        if (soc != null)
            txtSoc.setText(soc.getModeloSoc());
        if (android_id != null)
            txtAndroidID.setText(android_id);

        txtRam.setText(modelo.getRamGb() > 0 ? String.valueOf(modelo.getRamGb()) : "");
        txtAndroid.setText(modelo.getSoVersion() != null ? modelo.getSoVersion() : "");
        txtAlmacenamiento.setText(modelo.getAlmacenamientoGb() > 0 ? String.valueOf(modelo.getAlmacenamientoGb()) : "");
        try {
            Modelo modeloExistente = modeloDAO.buscarPorNombre(modelo.getNombreModelo());
            if (modeloExistente != null) {

                ocultarCampo(vboxSoc);
                ocultarCampo(vboxRAM);
                ocultarCampo(vboxAlmacenamiento);
                ocultarCampo(vboxAndroid);
                ocultarCampo(vboxPantalla);
                ocultarCampo(vboxCamara);
                ocultarCampo(camposFoto);

                lblInfo.setText("Modelo ya registrado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ocultarCampo(VBox campo) {
        campo.setVisible(false);
        campo.setManaged(false);
    }

    @FXML
    private void onGuardar() {
        try {
            // 1. Marca: Buscar por nombre antes de insertar
            String nombreMarca = txtMarca.getText().trim();
            Marca marca = marcaDAO.buscarPorNombre(nombreMarca);
            if (marca == null) {
                marca = new Marca(nombreMarca, "");
                int idMarca = marcaDAO.insertar(marca);
                marca.setIdMarca(idMarca);
            }

            // 2. SoC: Buscar por modelo antes de insertar
            String nombreSoc = txtSoc.getText().trim();
            Soc soc = socDAO.buscarPorModelo(nombreSoc);
            if (soc == null) {
                soc = new Soc();
                soc.setModeloSoc(nombreSoc);
                soc.setFabricante(nombreMarca);
                int idSoc = socDAO.insertar(soc);
                soc.setIdSoc(idSoc);
            }

            // 3. Modelo: Buscar por nombre antes de insertar
            String nombreModelo = txtModelo.getText().trim();
            Modelo modelo = modeloDAO.buscarPorNombre(nombreModelo);

            if (modelo == null) {
                // El modelo es nuevo, lo creamos y le asociamos sus bandas
                modelo = new Modelo(marca, nombreModelo);
                modelo.setSoc(soc);
                modelo.setSoVersion(txtAndroid.getText().trim());
                modelo.setResolucionPantalla(txtPantalla.getText().trim());
                modelo.setCamaraMp(txtCamara.getText().trim());

                if (!txtRam.getText().isBlank())
                    modelo.setRamGb(Double.parseDouble(txtRam.getText().trim()));
                if (!txtAlmacenamiento.getText().isBlank())
                    modelo.setAlmacenamientoGb(Double.parseDouble(txtAlmacenamiento.getText().trim()));

                int idModelo = modeloDAO.insertar(modelo);
                modelo.setIdModelo(idModelo);

                // --- LÓGICA DE BANDAS (Asociadas al Modelo) ---
                // 'dispositivoDesdeAdb' es el objeto que cargaste en 'onSeleccionarDispositivo'
                if (dispositivoDesdeAdb != null && dispositivoDesdeAdb.getBandasTemporales() != null) {
                    for (Banda banda : dispositivoDesdeAdb.getBandasTemporales()) {
                        // El método 'insertar' ahora es robusto (Insert or Ignore + Select ID)
                        int idBanda = bandaDAO.insertar(banda);

                        // Asociamos el modelo nuevo con la banda (tabla modelo_banda)
                        bandaDAO.asociarAModelo(idModelo, idBanda);
                    }
                }

                // Solo insertamos la foto si el MODELO es nuevo
                String ruta = txtRutaFoto.getText();
                if (ruta != null && !ruta.trim().isEmpty()) {
                    Foto foto = new Foto();
                    foto.setIdModelo(idModelo);
                    foto.setUrlExterna(ruta.trim());
                    foto.setDescripcion("Foto principal");
                    fotoDAO.insertar(foto);
                }
            }

            // 4. Dispositivo: Este SIEMPRE se inserta (unidad física única)
            // Usamos el 'modelo' (ya sea el recién creado o el encontrado en DB)
            Dispositivo dispositivo = new Dispositivo(modelo, serial, txtAndroidID.getText());
            dispositivo.setNotas(txtNotas.getText().trim());
            dispositivoDAO.insertar(dispositivo);

            // 5. Feedback y Navegación
            mostrarToast("✓ Dispositivo registrado correctamente");
            PauseTransition espera = new PauseTransition(Duration.millis(400));
            Dispositivo dispositivoFinal = dispositivo;
            espera.setOnFinished(e -> {
                if (onGuardadoExitoso != null) {
                    onGuardadoExitoso.accept(dispositivoFinal);
                }
            });
            espera.play();

        } catch (SQLException e) {
            mostrarToast("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Muestra un Toast flotante en la parte inferior del rootPane.
     * Si rootPane no está disponible, cae de vuelta al label original.
     */
    private void mostrarToast(String mensaje) {
        if (rootPane == null) {
            // Fallback: comportamiento anterior
            lblSerial.setText(mensaje);
            return;
        }

        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24 12 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);

        // Anclamos el Toast en la parte inferior, centrado
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));

        rootPane.getChildren().add(toast);

        // Animación: fade-in → pausa → fade-out → eliminar nodo
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pausa = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));

        new SequentialTransition(fadeIn, pausa, fadeOut).play();
    }

    // NUEVO MÉTODO para el botón de la carpetita en el FXML
    @FXML
    private void onSeleccionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Dispositivo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(lblSerial.getScene().getWindow());
        if (selectedFile != null) {
            txtRutaFoto.setText(selectedFile.getAbsolutePath());
        }
    }

}
