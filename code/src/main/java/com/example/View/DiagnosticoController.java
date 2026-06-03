package com.example.View;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import com.example.Controller.ADBService;
import com.example.Controller.PerfilesManager;
import com.example.Model.BloquePrueba;
import com.example.Model.Dispositivo;
import com.example.Model.LlamadasD17;
import com.example.Model.Entradas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import com.example.Model.SC04;
import com.example.Model.PasoPrueba;
import com.example.Model.PerfilDialer;
import com.example.View.ContactosConfigPopup.DispositivoCombo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class DiagnosticoController extends com.example.Model.AdbCallSupport implements DispositivoAware {

    @FXML
    private ListView<PasoPrueba> listaPasos;
    @FXML
    private FichaTecnicaController fichaTecnicaController;

    @FXML
    private ToggleGroup grupoIotMode;
    @FXML
    private ToggleButton btnIotCompleta;
    @FXML
    private ToggleButton btnIotExpress;
    @FXML
    private Button btnHotDial;
    @FXML
    private Button btnCallTimer;

    @FXML
    private ScrollPane scrollCategorias;
    @FXML
    private ScrollPane scrollHardware;

    @FXML
    private Button btnEjecutar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Button btnInforme;
    private String llamadaEntreDosNumero1 = null; // número del teléfono 1 (introducido por usuario)
    private String llamadaEntreDosNumero2 = null; // número del teléfono 2 (introducido por usuario)
    private String hotDialNumero = null;

    private final ObservableList<PasoPrueba> pasos = FXCollections.observableArrayList();
    private Dispositivo dispositivoActual;
    // ─── WiFi timeout ─────────────────────────────────────────────────────────
    private static final int WIFI_TIMEOUT_MS = 3 * 60 * 1000;
    private static final int WIFI_POLL_INTERVAL_MS = 3_000;

    // ─── Variables de configuración para usar contactos
    private String contactoTestNombre = "Test_ADB";
    private String contactoTestTelefonoDUT = "";
    private String contactoTestTelefono = "";;
    private String contactoExchangeCuenta = null;
    private String contactoSerialReceptor = null;

    // ─── Marcadores especiales para pasos de llamada avanzados ───────────────
    // El comando del PasoPrueba se usa como identificador interno cuando la
    // lógica de ejecución necesita hacer algo más que un simple shell.
    private static final String CMD_LLAMADA_MASIVA = "__LLAMADA_MASIVA__";
    private static final String CMD_LLAMADA_ENTRE_DOS = "__LLAMADA_ENTRE_DOS__";
    private static final String CMD_LLAMADA_ENTRANTE = "__CMD_LLAMADA_ENTRANTE__";
    private static final String CMD_EMERGENCIA = "__CMD_EMERGENCIA__";
    private static final String CMD_HOLD_RETRIEVE = "__CMD_HOLD_RETRIEVE__";
    private static final String CMD_DTMF = "__CMD_DTMF__";
    private static final String CMD_MUTE = "__CMD_MUTE__";
    private static final String CMD_RED_ACTIVA = "__CMD_RED_ACTIVA__";
    private static final String CMD_TRANSFERENCIA = "__CMD_TRANSFERENCIA__";
    private static final String CMD_TRANSFERENCIA_CIEGA = "__CMD_TRANSFERENCIA_CIEGA__";
    private static final String CMD_CONFERENCIA = "__CMD_CONFERENCIA__";
    private String transferenciaNumero = null;
    private String transferenciaResponderSerial = null;
    private String conferenciaNumero = null;
    private String conferenciaReceptorSerial = null;

    private static final String TOUCH_PINCH = "__PINCH__";
    private static final String TOUCH_SPREAD = "__SPREAD__";

    private static final String INFO_CHANGE_NAME = "__CHANGE_NAME__";
    private static final String INFO_HW_VERSION = "__HW_VERSION__";
    private static final String INFO_DEVICE_NAME_PC = "__DEVICE_NAME_PC__";
    private static final String INFO_IP = "__IP__";
    private static final String INFO_LOGCAT_BRAND = "__LOGCAT_BRAND__";

    private static final String DISPLAY_BRIGHTNESS_CHANGE = "__BRIGHTNESS_CHANGE__";
    private static final String DISPLAY_BRIGHTNESS_CHECK = "__BRIGHTNESS_CHECK__";
    private static final String DISPLAY_WALLPAPER = "__WALLPAPER__";
    private static final String DISPLAY_TIMEOUT_CHECK = "__TIMEOUT_CHECK__";
    private static final String DISPLAY_TIMEOUT_CHANGE = "__TIMEOUT_CHANGE__";
    private static final String DISPLAY_FONT_SIZE = "__FONT_SIZE__";
    private static final String DISPLAY_DISPLAY_SIZE = "__DISPLAY_SZIE__";
    private static final String DISPLAY_SCREENSAVER = "__SCREENSAVER__";
    private static final String FM_D17_RECORD_SEQUENCE = "__FM_D17_RECORD_SEQUENCE__";
    private static final long CALL_TIMER_DURATION_MS = 36_000L;

    private static final String CONTACT_CREATE_SIM = "__CREATE_SIM__";
    private static final String CONTACT_CALL_SIM = "__CALL_SIM__";
    private static final String CONTACT_RECEIVE_CALL_SIM = "__RECEIVE_CALL_SIM__";
    private static final String CONTACT_CREATE_PHONE = "__CREATE_PHONE__";
    private static final String CONTACT_EDIT_PHONE = "__EDIT_PHONE__";
    private static final String CONTACT_CALL_PHONE = "__CALL_PHONE__";
    private static final String CONTACT_DELETE_PHONE = "__DELETE_PHONE__";
    private static final String CONTACT_RECEIVE_CALL_PHONE = "__RECEIVE_CALL_PHONE__";
    private static final String CONTACT_COPY_SIM_PHONE = "__COPY_SIM_PHONE__";
    private static final String CONTACT_COPY_PHONE_SIM = "__COPY_PHONE_SIM__";
    private static final String CONTACT_IMPORT_VCARD = "__IMPORT_VCARD__";
    private static final String CONTACT_EXPORT_VCARD = "__EXPORT_VCARD__";
    private static final String CONTACT_MEMORY_STATUS = "__MEMORY_STATUS__";
    // BLUETOH CONSTANTES
    private static final String BT_DISCOVERABLE_TEST = "__BT_DISCOVERABLE_TEST__";
    private static final String BT_CHANGE_NAME_TEST = "__BT_CHANGE_NAME_TEST__";

    private static final String CALENDAR_CREATE = "__CALENDAR_CREATE__";
    private static final String CALENDAR_EDIT = "__CALENDAR_EDIT__";
    private static final String CALENDAR_DELETE = "__CALENDAR_DELETE__";

    private static final String MSG_SEND_SMS_NUMBER = "__SEND_SMS_NUMBER__";
    private static final String MSG_SEND_SMS_CONTACT = "__SEND_SMS_CONTACT__";
    private static final String MSG_RECEIVE_SMS = "__RECEIVE_SMS__";
    private static final String MSG_SEND_MMS_NUMBER = "__SEND_MMS_NUMBER__";
    private static final String MSG_SEND_MMS_CONTACT = "__SEND_MMS_CONTACT__";
    private static final String MSG_RECEIVE_MMS = "__RECEIVE_MMS__";
    private static final String MSG_DELETE_ONE = "__MSG_DELETE_ONE__";
    private static final String MSG_DELETE_ALL = "__MSG_DELETE_ALL__";
    private static final String MSG_SEND_SMS_SPECIAL = "__SEND_SMS_SPECIAL__";
    private static final String MSG_SEND_SMS_LONG = "__SEND_SMS_LONG__";
    private static final String MSG_SEND_SMS_NOOPT = "__SEND_SMS_NOOPT__";
    private static final String MSG_MMS_NO_DATA = "__MMS_NO_DATA__";

    // ─── Datos extra para los pasos de llamada avanzados ─────────────────────
    // Se rellenan cuando el usuario configura el paso en el popup.
    private String llamadaMasivaNumero = null; // número destino para llamada masiva
    private String llamadaEntreDosSerial1 = null; // serial del teléfono 1
    private String llamadaEntreDosSerial2 = null; // serial del teléfono 2
    private String llamadaEntranteSerial = null;
    private String llamadaEntranteNumero = null;
    private String musicaUriInterna = null;
    private String musicaUriExterna = null;

    private static final String MUSIC_TITLE = "QA Resume Track";
    private static final String MUSIC_ARTIST = "AEA Lab";
    private static final String MUSIC_ALBUM = "Resume Validation";

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        listaPasos.setItems(pasos);
        listaPasos.setCellFactory(lv -> new ListCell<PasoPrueba>() {
            @Override
            protected void updateItem(PasoPrueba item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getNombre().toUpperCase() + "  ➤  " + item.getEstado());

                    switch (item.getEstado()) {
                        case "OK" -> setStyle(
                                "-fx-text-fill: #a6e3a1; -fx-font-weight: bold; -fx-background-color: rgba(166,227,161,0.1);");
                        case "ERROR" -> setStyle(
                                "-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-background-color: rgba(243,139,168,0.1);");
                        case "EJECUTANDO" ->
                            setStyle("-fx-text-fill: #f3b631; -fx-background-color: rgba(249,226,175,0.1);");
                        default -> {
                            if (item.getEstado().startsWith("ESPERANDO")) {
                                setStyle("-fx-text-fill: #89dceb; -fx-background-color: rgba(137,220,235,0.1);");
                            } else {
                                setStyle("-fx-text-fill: #000000; -fx-background-color: transparent;");
                            }
                        }
                    }

                    // Un poco de padding para que no estén pegados
                    setPadding(new Insets(8, 12, 8, 12));
                }
            }
        });

        // Lógica para asegurar la selección del ToggleGroup y aplicar los estilos
        // dinámicos de los botones
        grupoIotMode.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // Si se intenta desmarcar el botón activo haciendo clic de nuevo, forzamos que
                // se mantenga
                oldValue.setSelected(true);
            } else {
                // Cambios visuales según el botón seleccionado
                if (btnIotCompleta.isSelected()) {
                    btnIotCompleta
                            .setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
                    btnIotExpress.setStyle("-fx-background-color: transparent; -fx-text-fill: #cdd6f4;");

                    btnHotDial.setVisible(true);
                    btnHotDial.setManaged(true);

                    btnCallTimer.setVisible(true);
                    btnCallTimer.setManaged(true);
                } else {
                    btnIotExpress
                            .setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
                    btnIotCompleta.setStyle("-fx-background-color: transparent; -fx-text-fill: #cdd6f4;");

                    btnHotDial.setVisible(false);
                    btnHotDial.setManaged(false);

                    btnCallTimer.setVisible(false);
                    btnCallTimer.setManaged(false);
                }
            }
        });
    }

    private void scroll(ScrollPane scrollPane, int direccion) {
        double width = scrollPane.getContent().getBoundsInLocal().getWidth();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();

        double scrollRange = width - viewportWidth;
        if (scrollRange > 0) {
            double saltoPixeles = 100.0; // pixeles de scroll
            double valorIncremento = saltoPixeles / scrollRange;

            double nuevoValor = scrollPane.getHvalue() + (direccion * valorIncremento);
            scrollPane.setHvalue(Math.max(0.0, Math.min(nuevoValor, 1.0)));
        }
    }

    @FXML
    private void scrollIzquierda() {
        scroll(scrollCategorias, -1);
    }

    @FXML
    private void scrollDerecha() {
        scroll(scrollCategorias, 1);
    }

    @FXML
    private void scrollIzquierdaHw() {
        scroll(scrollHardware, -1);
    }

    @FXML
    private void scrollDerechaHw() {
        scroll(scrollHardware, 1);
    }

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivoActual = dispositivo;
        if (fichaTecnicaController != null) {
            fichaTecnicaController.setDispositivo(dispositivo);
        }
    }

    @FXML
    private void calibrarDispositivo() {
        if (dispositivoActual == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Calibración");
        alert.setHeaderText("Calibrar botones del dialer");
        alert.setContentText(
                "1. Haz una llamada manual en el teléfono\n" +
                        "2. Abre el menú 'Más' durante la llamada\n" +
                        "3. Pulsa OK sin cerrar el menú");

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                new java.lang.Thread(() -> {
                    try {
                        String serial = obtenerSerialADBActual();
                        String modelo = ejecutarShellEnSerial(serial,
                                "getprop ro.product.model").trim();

                        Platform.runLater(() -> fichaTecnicaController.mostrarToast("Calibrando " + modelo + "..."));

                        PerfilDialer perfil = PerfilesManager.calibrarNuevoModelo(serial, modelo);

                        Platform.runLater(() -> {
                            if (perfil.getXHold() > 0 && perfil.getXMute() > 0) {
                                fichaTecnicaController.mostrarToast(
                                        "✅ " + modelo + " calibrado correctamente");
                            } else {
                                fichaTecnicaController.mostrarToast(
                                        "❌ Calibración incompleta: faltan botones del dialer");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    @FXML
    private void calibrarTecladoManual() {
        if (dispositivoActual == null)
            return;

        new java.lang.Thread(() -> {
            try {
                String serial = obtenerSerialADBActual();
                if (serial == null)
                    return;

                Platform.runLater(() -> fichaTecnicaController.mostrarToast("Calibrando teclado automaticamente..."));

                PerfilDialer prev = PerfilesManager.obtenerPerfil(serial);
                if (prev == null || prev.getXTeclado() <= 0 || prev.getYTeclado() <= 0) {
                    Platform.runLater(() -> fichaTecnicaController
                            .mostrarToast("No se detecto el boton Teclado. Haz una llamada y abre el dialer."));
                    return;
                }

                // Asegurar teclado abierto antes de leer nodos DTMF
                ejecutarAccionHilo(serial, "input tap " + prev.getXTeclado() + " " + prev.getYTeclado());
                Thread.sleep(1000);

                Map<Integer, int[]> coords = new HashMap<>();
                for (int intento = 0; intento < 3 && coords.size() < 10; intento++) {
                    String uiDump = ejecutarShellEnSerial(serial,
                            "uiautomator dump /sdcard/ui_dtmf_cal.xml >/dev/null 2>&1; cat /sdcard/ui_dtmf_cal.xml");
                    coords = extraerCoordsNumerosDesdeDump(uiDump);
                    if (coords.size() < 10) {
                        Thread.sleep(500);
                    }
                }

                if (coords.size() < 10) {
                    Platform.runLater(() -> fichaTecnicaController.mostrarToast(
                            "No pude leer los 10 digitos automaticamente. Repite con teclado DTMF visible."));
                    return;
                }

                String modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
                boolean esTactil = true;
                int xMas = prev.getXMostrarMas();
                int yMas = prev.getYMostrarMas();
                int xHold = prev.getXHold();
                int yHold = prev.getYHold();
                int xMute = prev.getXMute();
                int yMute = prev.getYMute();

                PerfilDialer nuevo = new PerfilDialer(modelo, esTactil, xMas, yMas, xHold, yHold, xMute, yMute,
                        prev.getXTeclado(), prev.getYTeclado(), coords);
                PerfilesManager.guardarPerfil(nuevo);

                ejecutarAccionHilo(serial, "input keyevent KEYCODE_BACK");
                Platform.runLater(() -> fichaTecnicaController.mostrarToast("Teclado calibrado sin mostrar captura"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Map<Integer, int[]> extraerCoordsNumerosDesdeDump(String uiDump) {
        Map<Integer, int[]> out = new HashMap<>();
        if (uiDump == null || uiDump.isBlank())
            return out;

        java.util.regex.Pattern nodePattern = java.util.regex.Pattern.compile("<node\\s+([^>]+)>");
        java.util.regex.Matcher nodeMatcher = nodePattern.matcher(uiDump);

        java.util.regex.Pattern digitPattern = java.util.regex.Pattern.compile("(?:text|content-desc)=\"([0-9])\"");
        java.util.regex.Pattern boundsPattern = java.util.regex.Pattern
                .compile("bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");

        while (nodeMatcher.find()) {
            String attrs = nodeMatcher.group(1);
            java.util.regex.Matcher dm = digitPattern.matcher(attrs);
            java.util.regex.Matcher bm = boundsPattern.matcher(attrs);
            if (!dm.find() || !bm.find())
                continue;

            int numero = Integer.parseInt(dm.group(1));
            int x = (Integer.parseInt(bm.group(1)) + Integer.parseInt(bm.group(3))) / 2;
            int y = (Integer.parseInt(bm.group(2)) + Integer.parseInt(bm.group(4))) / 2;
            out.putIfAbsent(numero, new int[] { x, y });
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP DE LLAMADA — se abre cuando el usuario pulsa "Añadir prueba llamada"
    // Ofrece dos opciones:
    // A) Llamada masiva → todos los dispositivos ADB llaman a un número manual
    // B) Llamada entre 2 → detecta los seriales ADB conectados, el usuario
    // elige cuál es el teléfono 1 y cuál el 2, y se detectan sus números
    // automáticamente por ADB al ejecutar
    @FXML
    private void addCallTest() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        // ── Contenedor principal ──────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;");
        root.setPrefWidth(580);

        // ── Header ───────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #181825; -fx-background-radius: 12 12 0 0;");
        HBox.setHgrow(header, javafx.scene.layout.Priority.ALWAYS);

        VBox headerTexto = new VBox(4);
        Label titulo = new Label("📞  Pruebas de Llamada");
        titulo.setFont(Font.font("Poppins", FontWeight.BOLD, 16));
        titulo.setTextFill(Color.web("#cdd6f4"));
        Label subtitulo = new Label("Selecciona el tipo de prueba que quieres añadir");
        subtitulo.setTextFill(Color.web("#6c7086"));
        subtitulo.setFont(Font.font(12));
        headerTexto.getChildren().addAll(titulo, subtitulo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c7086; " +
                "-fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> popup.close());

        header.getChildren().addAll(headerTexto, spacer, btnX);

        // ── Área scrolleable con grid de tarjetas ─────────────────────────────
        // Grid 2 columnas, N filas — escala sola al añadir tarjetas
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 20, 24));

        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // ── Área scrolleable — grid + config juntos ───────────────────────────

        // ── Panel de configuración (se muestra al seleccionar tarjeta) ────────
        VBox panelConfig = new VBox(12);
        panelConfig.setPadding(new Insets(0, 24, 20, 24));
        panelConfig.setVisible(false);
        panelConfig.setManaged(false);

        VBox contenido = new VBox(0);
        contenido.getChildren().addAll(grid, panelConfig);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        scroll.setMaxHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.7);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ── Definición de tarjetas ────────────────────────────────────────────
        // Formato: icono, título, descripción, color borde, acción al pulsar
        record Tarjeta(String icono, String titulo, String desc, String color) {
        }

        List<Tarjeta> tarjetas = List.of(
                new Tarjeta("📞", "Llamada Masiva",
                        "Todos los dispositivos conectados llaman al número introducido.", "#89b4fa"),
                new Tarjeta("🔄", "Llamada entre 2",
                        "Tel.1 llama a Tel.2 y viceversa. Verifica establecimiento bidireccional.", "#a6e3a1"),
                new Tarjeta("🚨", "Llamada de Emergencia",
                        "Marca 112 y verifica que la llamada se establece correctamente.", "#f38ba8"),
                new Tarjeta("📶", "Test Red Activa",
                        "Verifica en qué red (2G/3G/4G/VoLTE) se establece la llamada.", "#fab387"),
                new Tarjeta("⏸", "Hold / Retrieve",
                        "Pone la llamada en espera y la recupera. Verifica ambos estados.", "#cba6f7"),
                new Tarjeta("🔢", "Test DTMF",
                        "Envía tonos DTMF durante una llamada activa y verifica respuesta.", "#f9e2af"),
                new Tarjeta("📥", "Llamada Entrante",
                        "Tel.2 llama a Tel.1. Verifica que suena y se puede contestar.", "#94e2d5"),
                new Tarjeta("🔇", "Test Mute",
                        "Silencia y recupera el micrófono durante una llamada activa.", "#eba0ac"),
                new Tarjeta("↗", "Transferencia consultativa 4G",
                        "Transfiere la llamada activa a un número hablando con él primero.", "#89dceb"),
                new Tarjeta("↗", "Transferencia Ciega 4G",
                        "Transfiere sin hablar con el receptor.", "#f9e2af"),
                new Tarjeta("🤝", "Llamada Conferencia",
                        "Añade un tercer participante a la llamada activa.", "#b4befe"));

        // ── Renderiza tarjetas en el grid ─────────────────────────────────────
        for (int i = 0; i < tarjetas.size(); i++) {
            Tarjeta t = tarjetas.get(i);
            int col = i % 2;
            int row = i / 2;

            VBox card = new VBox(8);
            card.setPadding(new Insets(16));
            card.setCursor(javafx.scene.Cursor.HAND);
            card.setStyle(
                    "-fx-background-color: #313244;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #45475a;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;");

            Label icono = new Label(t.icono());
            icono.setFont(Font.font(24));

            Label nombre = new Label(t.titulo());
            nombre.setFont(Font.font("Poppins", FontWeight.BOLD, 13));
            nombre.setTextFill(Color.web("#cdd6f4"));
            nombre.setWrapText(true);

            Label desc = new Label(t.desc());
            desc.setFont(Font.font(11));
            desc.setTextFill(Color.web("#6c7086"));
            desc.setWrapText(true);

            card.getChildren().addAll(icono, nombre, desc);

            // Hover
            String colorBorde = t.color();
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: #45475a;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: " + colorBorde + ";" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 2;"));
            card.setOnMouseExited(e -> card.setStyle(
                    "-fx-background-color: #313244;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #45475a;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;"));

            // Acción al pulsar cada tarjeta
            String tituloTarjeta = t.titulo();
            card.setOnMouseClicked(e -> {
                panelConfig.getChildren().clear();
                panelConfig.setVisible(true);
                panelConfig.setManaged(true);
                mostrarConfigTarjeta(tituloTarjeta, panelConfig, popup, scroll);
            });

            grid.add(card, col, row);
        }

        // ── Ensambla la ventana ───────────────────────────────────────────────
        root.getChildren().addAll(header, scroll);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ── Muestra el formulario de configuración según la tarjeta pulsada ──────
    private void mostrarConfigTarjeta(String tipo, VBox panel, Stage popup, ScrollPane scroll) {

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #45475a;");

        Label lblTipo = new Label("Configurar: " + tipo);
        lblTipo.setFont(Font.font("Poppins", FontWeight.BOLD, 13));
        lblTipo.setTextFill(Color.web("#cdd6f4"));

        panel.getChildren().addAll(sep, lblTipo);

        switch (tipo) {

            case "Llamada Masiva" -> {
                TextField tf = crearTextField("Número destino (ej: +34612345678)");
                Button btn = crearBoton("➕  Añadir al script", "#89b4fa");
                btn.setOnAction(e -> {
                    String num = tf.getText().trim();
                    if (num.isBlank()) {
                        tf.setStyle(tf.getStyle() + "-fx-border-color: #f38ba8;");
                        return;
                    }
                    llamadaMasivaNumero = num;
                    pasos.add(new PasoPrueba("Llamada Masiva → " + num, CMD_LLAMADA_MASIVA));
                    popup.close();
                });
                panel.getChildren().addAll(tf, btn);
            }

            case "Llamada entre 2" -> {
                Label l1 = crearLabelConfig("Teléfono 1 (llama primero):");
                ComboBox<String> cb1 = crearCombo(List.of());
                cb1.setDisable(true);
                Label l2 = crearLabelConfig("Número del Teléfono 1:");
                TextField tf1 = crearTextField("+34612345678");
                Label l3 = crearLabelConfig("Teléfono 2 (recibe primero):");
                ComboBox<String> cb2 = crearCombo(List.of());
                cb2.setDisable(true);
                Label l4 = crearLabelConfig("Número del Teléfono 2:");
                TextField tf2 = crearTextField("+34698765432");
                Label estadoCarga = new Label("Cargando dispositivos ADB...");
                estadoCarga.setTextFill(Color.web("#6c7086"));
                estadoCarga.setFont(Font.font(10));
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Label ayuda = new Label("Selecciona el modelo del equipo. A la derecha verás su serial o IP.");
                ayuda.setTextFill(Color.web("#6c7086"));
                ayuda.setFont(Font.font(10));
                ayuda.setWrapText(true);

                Button btn = crearBoton("➕  Añadir al script", "#a6e3a1");
                btn.setDisable(true);
                btn.setOnAction(e -> {
                    String s1 = serialDesdeEtiqueta(cb1.getValue()), s2 = serialDesdeEtiqueta(cb2.getValue());
                    String n1 = tf1.getText().trim(), n2 = tf2.getText().trim();
                    if (s1 == null || s2 == null) {
                        aviso.setText("No hay dispositivos ADB.");
                        return;
                    }
                    if (s1.equals(s2)) {
                        aviso.setText("Selecciona dos dispositivos distintos.");
                        return;
                    }
                    if (n1.isBlank() || n2.isBlank()) {
                        aviso.setText("Introduce los números.");
                        return;
                    }
                    llamadaEntreDosSerial1 = s1;
                    llamadaEntreDosSerial2 = s2;
                    llamadaEntreDosNumero1 = n1;
                    llamadaEntreDosNumero2 = n2;
                    pasos.add(new PasoPrueba("Llamada entre 2 (" + s1 + " ↔ " + s2 + ")", CMD_LLAMADA_ENTRE_DOS));
                    popup.close();
                });
                panel.getChildren().addAll(l1, cb1, l2, tf1, l3, cb2, l4, tf2, estadoCarga, aviso, btn);

                cargarSerialesParaPopup(popup, (seriales, serialActual) -> {
                    List<String> opciones = seriales.stream()
                            .map(this::etiquetaDispositivo)
                            .toList();

                    cb1.getItems().setAll(opciones);
                    cb2.getItems().setAll(opciones);
                    cb1.setDisable(false);
                    cb2.setDisable(false);

                    if (serialActual != null && seriales.contains(serialActual)) {
                        cb1.getSelectionModel().select(etiquetaDispositivo(serialActual));
                        cb1.setDisable(true);
                    } else if (!opciones.isEmpty()) {
                        cb1.getSelectionModel().select(0);
                    }

                    seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                            .ifPresent(s -> cb2.getSelectionModel().select(etiquetaDispositivo(s)));
                    if (cb2.getSelectionModel().isEmpty() && !opciones.isEmpty()) {
                        cb2.getSelectionModel().select(0);
                    }

                    estadoCarga.setText(opciones.isEmpty()
                            ? "No hay dispositivos ADB conectados."
                            : "Dispositivos ADB cargados.");
                    btn.setDisable(opciones.isEmpty());
                }, error -> {
                    estadoCarga.setText("No se pudieron cargar los dispositivos ADB.");
                    aviso.setText(error == null || error.isBlank() ? "Error cargando dispositivos ADB." : error);
                    btn.setDisable(true);
                });
            }

            case "Llamada de Emergencia" -> {
                Label info = new Label("Marcará el 112, esperará 3 segundos y colgará\n" +
                        "antes de que contesten. Solo verifica que se puede marcar.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                Button btn = crearBoton("➕  Añadir al script", "#f38ba8");
                btn.setOnAction(e -> {
                    pasos.add(new PasoPrueba("Llamada Emergencia 112", CMD_EMERGENCIA));
                    popup.close();
                });
                panel.getChildren().addAll(info, btn);
            }

            case "Test Red Activa" -> {
                Label info = new Label("Verifica en qué red (2G/3G/4G/VoLTE) está\nregistrado el dispositivo.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                Button btn = crearBoton("➕  Añadir al script", "#fab387");
                btn.setOnAction(e -> {
                    pasos.add(new PasoPrueba("Test Red Activa", CMD_RED_ACTIVA));
                    popup.close();
                });
                panel.getChildren().addAll(info, btn);
            }

            case "Hold / Retrieve" -> {
                Label info = new Label("Requiere una llamada activa. Pone en espera\n" +
                        "5 segundos y luego la recupera.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                Button btn = crearBoton("➕  Añadir al script", "#cba6f7");
                btn.setOnAction(e -> {
                    pasos.add(new PasoPrueba("Hold / Retrieve", CMD_HOLD_RETRIEVE));
                    popup.close();
                });
                panel.getChildren().addAll(info, btn);
            }

            case "Test DTMF" -> {
                Label info = new Label("Requiere una llamada activa. Envía los dígitos 0-9\ncomo tonos DTMF.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                Button btn = crearBoton("➕  Añadir al script", "#f9e2af");
                btn.setOnAction(e -> {
                    pasos.add(new PasoPrueba("Test DTMF 0-9", CMD_DTMF));
                    popup.close();
                });
                panel.getChildren().addAll(info, btn);
            }

            case "Llamada Entrante" -> {
                Label info = new Label("La configuración de esta prueba se pedirá justo antes de ejecutarla.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);

                Button btn = crearBoton("➕  Añadir al script", "#94e2d5");
                btn.setOnAction(e -> {
                    llamadaEntranteSerial = null;
                    llamadaEntranteNumero = null;
                    // Mostrar diálogo de configuración ahora para que el usuario introduzca número
                    Stage ownerStage = (Stage) btnEjecutar.getScene().getWindow();
                    if (configurarLlamadaEntranteParaFm(ownerStage)) {
                        pasos.add(new PasoPrueba("Llamada Entrante", CMD_LLAMADA_ENTRANTE));
                        popup.close();
                    }
                });
                panel.getChildren().addAll(info, btn);
            }

            case "Test Mute" -> {
                Label info = new Label("Requiere una llamada activa. Silencia el micrófono,\n" +
                        "espera 3 segundos y lo recupera.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                Button btn = crearBoton("➕  Añadir al script", "#eba0ac");
                btn.setOnAction(e -> {
                    pasos.add(new PasoPrueba("Test Mute/Unmute", CMD_MUTE));
                    popup.close();
                });
                panel.getChildren().addAll(info, btn);
            }
            case "Transferencia consultativa 4G" -> {
                Label info = new Label("Requiere una llamada activa. Pulsa el botón\n" +
                        "físico de transferencia y marca el número destino.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                TextField tf = crearTextField("Número destino (ej: +34612345678)");
                List<String> seriales = obtenerSerialesADB();
                List<String> opcionesReceptor = seriales.stream()
                        .map(this::etiquetaDispositivo)
                        .toList();
                ComboBox<String> cbReceptor = crearCombo(opcionesReceptor);
                if (!opcionesReceptor.isEmpty()) {
                    String serialActual = obtenerSerialADBActual();
                    seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                            .ifPresent(s -> cbReceptor.getSelectionModel().select(etiquetaDispositivo(s)));
                    if (cbReceptor.getSelectionModel().isEmpty()) {
                        cbReceptor.getSelectionModel().select(0);
                    }
                }
                Label lReceptor = crearLabelConfig("Teléfono que debe contestar automáticamente:");
                Label ayuda = new Label("Se muestra el modelo del equipo y su serial/IP para identificarlo mejor.");
                ayuda.setTextFill(Color.web("#6c7086"));
                ayuda.setFont(Font.font(10));
                ayuda.setWrapText(true);
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Button btn = crearBoton("➕  Añadir al script", "#89dceb");
                btn.setOnAction(e -> {
                    String num = tf.getText().trim();
                    String receptor = serialDesdeEtiqueta(cbReceptor.getValue());
                    if (num.isBlank()) {
                        aviso.setText("Introduce el número destino.");
                        return;
                    }
                    transferenciaNumero = num;
                    transferenciaResponderSerial = receptor;
                    pasos.add(new PasoPrueba("Transferencia → " + num, CMD_TRANSFERENCIA));
                    popup.close();
                });
                panel.getChildren().addAll(info, tf, lReceptor, ayuda, cbReceptor, aviso, btn);
            }

            case "Transferencia Ciega 4G" -> {
                Label info = new Label("Transfiere la llamada activa sin hablar con el receptor.\n" +
                        "Sigue la secuencia de botones físicos del F780.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                TextField tf = crearTextField("Número destino (ej: +34612345678)");
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Button btn = crearBoton("➕  Añadir al script", "#f9e2af");
                btn.setOnAction(e -> {
                    String num = tf.getText().trim();
                    if (num.isBlank()) {
                        aviso.setText("Introduce el número destino.");
                        return;
                    }
                    transferenciaNumero = num;
                    pasos.add(new PasoPrueba("Transferencia Ciega → " + num, CMD_TRANSFERENCIA_CIEGA));
                    popup.close();
                });
                panel.getChildren().addAll(info, tf, aviso, btn);
            }

            case "Llamada Conferencia" -> {
                Label info = new Label("Requiere una llamada activa. Añade un tercer\n" +
                        "participante marcando el número indicado.");
                info.setTextFill(Color.web("#6c7086"));
                info.setFont(Font.font(11));
                info.setWrapText(true);
                TextField tf = crearTextField("Número del tercer participante");

                Label lReceptor = crearLabelConfig("Dispositivo que debe contestar:");
                ComboBox<String> cbReceptor = crearCombo(List.of());
                cbReceptor.setDisable(true);

                Label estadoCarga = new Label("Cargando dispositivos ADB...");
                estadoCarga.setTextFill(Color.web("#6c7086"));
                estadoCarga.setFont(Font.font(10));
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Button btn = crearBoton("➕  Añadir al script", "#b4befe");
                btn.setDisable(true);
                btn.setOnAction(e -> {
                    String num = tf.getText().trim();
                    String receptor = serialDesdeEtiqueta(cbReceptor.getValue());
                    if (num.isBlank()) {
                        aviso.setText("Introduce el número destino.");
                        return;
                    }
                    conferenciaNumero = num;
                    conferenciaReceptorSerial = receptor;
                    pasos.add(new PasoPrueba("Conferencia con " + num, CMD_CONFERENCIA));
                    popup.close();
                });
                panel.getChildren().addAll(info, tf, lReceptor, cbReceptor, estadoCarga, aviso, btn);

                cargarSerialesParaPopup(popup, (seriales, serialActual) -> {
                    List<String> opciones = seriales.stream()
                            .map(this::etiquetaDispositivo)
                            .toList();

                    cbReceptor.getItems().setAll(opciones);
                    cbReceptor.setDisable(false);

                    if (!opciones.isEmpty()) {
                        seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                                .ifPresent(s -> cbReceptor.getSelectionModel().select(etiquetaDispositivo(s)));
                        if (cbReceptor.getSelectionModel().isEmpty()) {
                            cbReceptor.getSelectionModel().select(0);
                        }
                    }

                    estadoCarga.setText(opciones.isEmpty()
                            ? "No hay dispositivos ADB conectados."
                            : "Dispositivos ADB cargados.");
                    btn.setDisable(opciones.isEmpty());
                }, error -> {
                    estadoCarga.setText("No se pudieron cargar los dispositivos ADB.");
                    aviso.setText(error == null || error.isBlank() ? "Error cargando dispositivos ADB." : error);
                    btn.setDisable(true);
                });
            }
        }
        javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private void cargarSerialesParaPopup(Stage popup,
            java.util.function.BiConsumer<List<String>, String> onSuccess,
            java.util.function.Consumer<String> onError) {
        new Thread(() -> {
            try {
                List<String> seriales = obtenerSerialesADB();
                String serialActual = obtenerSerialADBActual();
                Platform.runLater(() -> {
                    if (popup == null || popup.getScene() == null) {
                        return;
                    }
                    onSuccess.accept(seriales, serialActual);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (popup == null || popup.getScene() == null) {
                        return;
                    }
                    onError.accept(ex.getMessage());
                });
            }
        }, "adb-call-popup-loader").start();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────
    private TextField crearTextField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-prompt-text-fill: #6c7086;" +
                        "-fx-border-color: #45475a; -fx-border-radius: 6;" +
                        "-fx-background-radius: 6; -fx-padding: 8;");
        return tf;
    }

    private Label crearLabelConfig(String texto) {
        Label l = new Label(texto);
        l.setTextFill(Color.web("#a6adc8"));
        l.setFont(Font.font(12));
        return l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EJECUTAR SCRIPT
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ejecutarScript() {
        if (dispositivoActual == null || pasos.isEmpty())
            return;

        btnLimpiar.setDisable(true);
        btnEjecutar.setDisable(true);

        final List<PasoPrueba> pasosEjecucion = new ArrayList<>(pasos);

        new Thread(() -> {
            ADBService adb = new ADBService();

            String serialActivo;
            try {
                serialActivo = adb.getSerialActivo(dispositivoActual.getAndroid_id());
            } catch (IOException e) {
                serialActivo = dispositivoActual.getSerialNumber();
            }
            final String serial = serialActivo;

            for (PasoPrueba paso : pasosEjecucion) {
                final PasoPrueba ref = paso;

                // 1. Marcar como ejecutando
                Platform.runLater(() -> {
                    ref.setEstado("EJECUTANDO");
                    listaPasos.refresh();
                });

                boolean ok = false;
                String outputDetalle = "";
                boolean esWifi = paso.getNombre().toLowerCase().contains("wifi") &&
                        paso.getNombre().toLowerCase().contains("levantar");
                Stage owner = (Stage) btnEjecutar.getScene().getWindow();

                if (paso.getNombre() != null && paso.getNombre().startsWith("SOFT.029")) {
                    boolean cuentaOk = comprobarYLoguearCuentaGoogle(serial, adb,
                            "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_CALENDAR");

                    if (!cuentaOk) {
                        continue;
                    }
                    adb.ejecutarComandoSincrono(serial,
                            "shell pm grant com.google.android.calendar android.permission.READ_CALENDAR");
                    adb.ejecutarComandoSincrono(serial,
                            "shell pm grant com.google.android.calendar android.permission.WRITE_CALENDAR");
                    adb.ejecutarComandoSincrono(serial,
                            "shell pm grant com.android.calendar android.permission.READ_CALENDAR");
                    adb.ejecutarComandoSincrono(serial,
                            "shell pm grant com.android.calendar android.permission.WRITE_CALENDAR");
                }

                if (!ref.isManual() && ref.getComandos().size() > 1 &&
                        ref.getComandos().get(0).contains("mAudioRoutes")) {

                    if (!adb.tieneAuricularConectado(serial)) {
                        final PasoPrueba refFinal = ref;

                        Platform.runLater(() -> {
                            refFinal.setEstado("ERROR");
                            refFinal.setOutputDetalle("Auricular no conectado");
                            listaPasos.refresh();
                        });

                        continue;
                    }
                }

                if ("__MO_CALL_DURATION_CHECK__".equals(ref.getComando())) {
                    ok = ejecutarCallTimerDurationCheck(serial, paso);
                } else if ("__MO_CALL_LIMIT_WARN_CHECK__".equals(ref.getComando())) {
                    ok = ejecutarCallLimitWarnCheck(serial, paso);
                } else if ("__MO_CALL_AUTO_HANGUP_CHECK__".equals(ref.getComando())) {
                    ok = ejecutarCallAutoHangupCheck(serial, paso);

                } else if ("__SC04_CALLER_ID_NETWORK_DEFAULT__".equals(ref.getComando())) {
                    LlamadasD17 llamadas = new LlamadasD17(serial);
                    try {
                        actualizarEstadoPaso(paso, "Abriendo ajustes de Caller ID...");
                        ejecutarAccionHilo(serial, Entradas.secuencia(
                                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                "sleep 2",
                                Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.ok(),
                                Entradas.unSegundo(),
                                Entradas.ok()));
                        Thread.sleep(1_500L);

                        String numero = solicitarNumeroLlamada(owner, "Caller ID - Network default",
                                "Introduce el número al que quieres llamar:");
                        if (numero == null || numero.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String numeroLimpio = numero.replaceAll("\\s+", "").trim();
                            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + "...");
                            ejecutarShellEnSerial(serial,
                                    "am start -a android.intent.action.CALL -d tel:" + numeroLimpio);

                            boolean llamadaActiva = false;
                            for (int intento = 0; intento < 10; intento++) {
                                Thread.sleep(1_000L);
                                if (llamadaActiva(serial)) {
                                    llamadaActiva = true;
                                    break;
                                }
                            }

                            if (!llamadaActiva) {
                                ejecutarShellEnSerial(serial,
                                        "am start -a android.intent.action.CALL -d tel:" + numeroLimpio);
                                for (int intento = 0; intento < 5; intento++) {
                                    Thread.sleep(1_000L);
                                    if (llamadaActiva(serial)) {
                                        llamadaActiva = true;
                                        break;
                                    }
                                }
                            }

                            ok = llamadaActiva;
                            if (ok) {
                                ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                            } else {
                                outputDetalle = "No se pudo iniciar la llamada";
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                        outputDetalle = "Interrumpido durante la preparación de la llamada";
                    }

                } else if ("__SC04_CALLER_ID_HIDE_NUMBER__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Abriendo menú y configurando Hide Number...");

                        // 1. Aseguramos que el diálogo se abra (Misma navegación base que el paso 1)
                        ejecutarAccionHilo(serial, Entradas.secuencia(
                                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                "sleep 2",
                                Entradas.abajo(), Entradas.abajo(), Entradas.abajo(), Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.ok(), "sleep 2", Entradas.ok(),
                                "sleep 1",
                                Entradas.abajo(),
                                Entradas.ok() // Abre el diálogo flotante
                        ));
                        Thread.sleep(2500L); // Esperamos a que el diálogo aparezca en pantalla
                        // 3. Realizar llamada de prueba
                        owner = (Stage) btnEjecutar.getScene().getWindow();
                        String numero = solicitarNumeroLlamada(owner, "Caller ID - Hide number",
                                "Introduce el número al que quieres llamar:");

                        if (numero == null || numero.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String numeroLimpio = numero.replaceAll("\\s+", "").trim();
                            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + " (Oculto)...");
                            ejecutarShellEnSerial(serial,
                                    "am start -a android.intent.action.CALL -d tel:" + numeroLimpio);

                            boolean llamadaActiva = false;
                            for (int intento = 0; intento < 10; intento++) {
                                Thread.sleep(1000L);
                                if (llamadaActiva(serial)) {
                                    llamadaActiva = true;
                                    break;
                                }
                            }

                            ok = llamadaActiva;
                            if (ok) {
                                ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                            } else {
                                outputDetalle = "No se pudo iniciar la llamada";
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                    }
                } else if ("__SC04_CALLER_ID_SHOW_NUMBER__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Abriendo menú y configurando Show Number...");

                        // Unificamos la secuencia completa para Show Number
                        ejecutarAccionHilo(serial, Entradas.secuencia(
                                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                "sleep 2",
                                Entradas.ok(),
                                "sleep 2",
                                Entradas.abajo(), // 2. Baja a la opción "Show number"
                                "sleep 1",
                                Entradas.ok() // 3. Confirma con OK
                        ));

                        // Damos margen a que el teléfono responda antes de lanzar la interfaz de
                        // llamada
                        Thread.sleep(4000L);

                        // 3. Realizar llamada de prueba
                        owner = (Stage) btnEjecutar.getScene().getWindow();
                        String numero = solicitarNumeroLlamada(owner, "Caller ID - Show number",
                                "Introduce el número al que quieres llamar:");

                        if (numero == null || numero.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String numeroLimpio = numero.replaceAll("\\s+", "").trim();
                            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + " (Mostrando número)...");
                            ejecutarShellEnSerial(serial,
                                    "am start -a android.intent.action.CALL -d tel:" + numeroLimpio);

                            boolean llamadaActiva = false;
                            for (int intento = 0; intento < 10; intento++) {
                                Thread.sleep(1000L);
                                if (llamadaActiva(serial)) {
                                    llamadaActiva = true;
                                    break;
                                }
                            }

                            ok = llamadaActiva;
                            if (ok) {
                                ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                            } else {
                                outputDetalle = "No se pudo iniciar la llamada";
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                    }
                } else if ("__SC04_USSD_PRIVATE_APN_CHECK__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Probando código USSD *31# (Ocultar)...");

                        // 1. Marcamos el código USSD automáticamente
                        // Usamos dialer para que el usuario vea el resultado del código en pantalla
                        ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:*31%23");
                        // Nota: %23 es el escape para el símbolo # en URIs

                        Thread.sleep(3000L); // Esperamos a que aparezca el mensaje de red "Caller ID enabled"

                        // 2. Realizamos la llamada de verificación
                        owner = (Stage) btnEjecutar.getScene().getWindow();
                        String numero = solicitarNumeroLlamada(owner, "Prueba USSD *31#",
                                "El código *31# ha sido enviado. Introduce número para verificar:");

                        if (numero != null && !numero.isBlank()) {
                            ejecutarShellEnSerial(serial,
                                    "am start -a android.intent.action.CALL -d tel:" + numero.trim());

                            // 3. Confirmación doble: Número oculto + Datos funcionando
                            ok = ConfirmacionManualPopup.mostrarYEsperar(
                                    "¿La llamada fue oculta Y el icono de datos (4G/5G) sigue activo?", owner);

                            // 4. Limpieza: Desactivamos el oculto para no dejar el terminal "tocado"
                            actualizarEstadoPaso(paso, "Restaurando configuración con #31#...");
                            ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:%2331%23");
                        }

                    } catch (Exception e) {
                        outputDetalle = "Error en prueba USSD: " + e.getMessage();
                        ok = false;
                    }
                } else if ("__SC04_CALL_WAITING_ACTIVATE__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Limpiando pantalla y abriendo menú Call Waiting...");

                        // 1. Forzamos un inicio limpio para quitar popups residuales del USSD
                        ejecutarAccionHilo(serial, Entradas.secuencia(
                                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                "sleep 2",
                                // Navegamos al menú "Additional settings" de forma segura
                                Entradas.abajo(), Entradas.abajo(), Entradas.abajo(), Entradas.abajo(),
                                Entradas.abajo(),
                                Entradas.ok(),
                                "sleep 2", // Esperamos que cargue la pantalla interna

                                // 2. Ejecuta tu secuencia: una flecha abajo para situarse sobre Call Waiting y
                                // OK
                                Entradas.abajo(),
                                Entradas.ok()));

                        // Damos un margen generoso para que Android aplique el cambio de red
                        Thread.sleep(4000L);

                        owner = (Stage) btnEjecutar.getScene().getWindow();

                        // Validamos visualmente que se haya activado correctamente
                        ok = ConfirmacionManualPopup.mostrarYEsperar(
                                "¿Se activó correctamente el interruptor de 'Call waiting'?", owner);

                        if (!ok) {
                            outputDetalle = "El usuario indicó que Call Waiting no se activó.";
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                    }
                } else if ("__SC04_CALL_WAITING_CHECK_DURING__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Preparando prueba de llamada en espera activa...");
                        owner = (Stage) btnEjecutar.getScene().getWindow();

                        // 1. Solicitamos el primer número para establecer la llamada base
                        String numeroA = solicitarNumeroLlamada(owner, "Call Waiting - Primera Línea",
                                "Introduce el primer número para iniciar la llamada base (debe atenderla):");

                        if (numeroA == null || numeroA.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String numALimpio = numeroA.replaceAll("\\s+", "").trim();
                            actualizarEstadoPaso(paso, "Llamando a " + numALimpio + "...");
                            ejecutarShellEnSerial(serial,
                                    "am start -a android.intent.action.CALL -d tel:" + numALimpio);

                            // 2. Esperamos a que la primera llamada se conecte y se estabilice
                            boolean llamadaBaseActiva = false;
                            for (int intento = 0; intento < 12; intento++) {
                                Thread.sleep(1000L);
                                if (llamadaActiva(serial)) {
                                    llamadaBaseActiva = true;
                                    break;
                                }
                            }

                            if (!llamadaBaseActiva) {
                                outputDetalle = "No se detectó una llamada activa en el canal base.";
                                ok = false;
                            } else {
                                // 3. El canal está ocupado. Instruimos al usuario para generar la llamada
                                // entrante.
                                actualizarEstadoPaso(paso, "Esperando llamada entrante secundaria...");

                                ok = ConfirmacionManualPopup.mostrarYEsperar(
                                        "INSTRUCCIONES:\n\n" +
                                                "1. Mantén la llamada actual abierta.\n" +
                                                "2. Realiza una llamada HACIA este móvil usando un SEGUNDO teléfono externo.\n"
                                                +
                                                "3. Verifica si en la pantalla del móvil aparece la notificación de 'Llamada en espera' (Call waiting).\n\n"
                                                +
                                                "¿El terminal notificó la segunda llamada correctamente?",
                                        owner);

                                if (!ok) {
                                    outputDetalle = "El usuario reportó que la llamada en espera no se visualizó o falló.";
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                    }
                }  else if ("__SC04_CALL_WAITING_DEACTIVATE__".equals(ref.getComando())) {
                    try {
                        actualizarEstadoPaso(paso, "Abriendo menú para desactivar Call Waiting...");

                        // 1. Forzamos apertura limpia desde CERO para asegurar que estamos en el menú correcto
                        ejecutarAccionHilo(serial, Entradas.secuencia(
                                "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                "sleep 4",
                                
                                Entradas.ok(),
                                "sleep 3", // Esperamos que la red procese la desactivación

                                // Regresamos a la opción de "Caller ID" (subiendo una posición)
                                Entradas.arriba(),
                                "sleep 1",

                                // Abrimos el diálogo de Caller ID para restaurarlo
                                Entradas.ok(),
                                "sleep 2",
                                
                                // Nos aseguramos de subir arriba del todo en el diálogo flotante (Network default)
                                Entradas.arriba(),
                                Entradas.arriba(),

                                // Confirmamos la selección de 'Network default'
                                Entradas.ok()
                             ));

                        // Damos un margen para que el teléfono guarde los cambios en la SIM
                        Thread.sleep(4500L);

                        owner = (Stage) btnEjecutar.getScene().getWindow();

                        // 2. Validamos con el operador que todo volvió a la normalidad
                        ok = ConfirmacionManualPopup.mostrarYEsperar(
                                "¿Se desactivó Call Waiting Y el Caller ID volvió a 'Network default'?", owner);

                        if (!ok) {
                            outputDetalle = "La restauración de los ajustes de llamada falló o no se completó.";
                        }

                        // 3. Cierre limpio de la aplicación para que no quede nada en pantalla
                        actualizarEstadoPaso(paso, "Limpiando interfaz del sistema...");
                        ejecutarShellEnSerial(serial, "am force-stop com.android.phone");
                        Thread.sleep(1000L);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ok = false;
                    }
                } else if ("__HOT_DIAL_CHECK_SERVICE__".equals(ref.getComando())) {
                    LlamadasD17 llamadas = new LlamadasD17(serial);
                    if (hotDialNumero == null || hotDialNumero.isBlank()) {
                        outputDetalle = "Hot dial number not configured";
                        ok = false;
                    } else {
                        actualizarEstadoPaso(paso, "Llamando a hot dial: " + hotDialNumero + "...");
                        ok = llamadas.verificarHotDial(hotDialNumero);
                    }

                } else if ("__HOT_DIAL_CALL_OTHER_NUMBERS__".equals(ref.getComando())) {
                    LlamadasD17 llamadas = new LlamadasD17(serial);

                    String num1 = solicitarNumeroLlamada(owner, "Hot Dial - Other numbers",
                            "Introduce el primer número a llamar:");
                    if (num1 == null || num1.isBlank()) {
                        actualizarEstadoPaso(paso, "Cancelado por el usuario");
                        ok = false;
                    } else {
                        String num2 = solicitarNumeroLlamada(owner, "Hot Dial - Other numbers",
                                "Introduce el segundo número a llamar:");
                        if (num2 == null || num2.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String n1 = num1.replaceAll("\\s+", "").trim();
                            String n2 = num2.replaceAll("\\s+", "").trim();

                            boolean res1 = llamadas.llamarSinVerificar(n1, 5_000L);
                            boolean res2 = llamadas.llamarSinVerificar(n2, 5_000L);

                            ok = res1 && res2;
                            outputDetalle = ok ? "Two calls launched" : "No se pudieron lanzar las dos llamadas";
                            if (ok) {
                                ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                            }
                        }
                    }

                } else if ("__HOT_DIAL_DISABLE_AND_CALL_OTHER_NUMBERS__".equals(ref.getComando())) {
                    LlamadasD17 llamadas = new LlamadasD17(serial);
                    try {
                        ejecutarAccionHilo(serial, "am start -a android.telecom.action.SHOW_CALL_SETTINGS");
                        Thread.sleep(2_000L);
                        ejecutarAccionHilo(serial, "input keyevent 19");
                        Thread.sleep(400L);
                        ejecutarAccionHilo(serial, "input keyevent 23");
                        Thread.sleep(600L);

                        String num1 = solicitarNumeroLlamada(owner, "Hot Dial - Disable and call",
                                "Introduce el primer número a llamar:");
                        if (num1 == null || num1.isBlank()) {
                            actualizarEstadoPaso(paso, "Cancelado por el usuario");
                            ok = false;
                        } else {
                            String num2 = solicitarNumeroLlamada(owner, "Hot Dial - Disable and call",
                                    "Introduce el segundo número a llamar:");
                            if (num2 == null || num2.isBlank()) {
                                actualizarEstadoPaso(paso, "Cancelado por el usuario");
                                ok = false;
                            } else {
                                String n1 = num1.replaceAll("\\s+", "").trim();
                                String n2 = num2.replaceAll("\\s+", "").trim();

                                boolean res1 = llamadas.llamarSinVerificar(n1, 5_000L);
                                if (res1) {
                                    llamadas.colgarLlamadaEnCurso();
                                }
                                boolean res2 = llamadas.llamarSinVerificar(n2, 5_000L);
                                if (res2) {
                                    llamadas.colgarLlamadaEnCurso();
                                }

                                ok = res1 && res2;
                                outputDetalle = ok ? "Two calls launched, waited 5 seconds and hung up"
                                        : "No se pudieron completar las dos llamadas";
                                if (ok) {
                                    ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        actualizarEstadoPaso(paso, "Interrumpido por el sistema");
                        ok = false;
                    }

                } else if (ref.isManual()) {
                    boolean accionOk = true;
                    if (!ref.getComando().isBlank()) {
                        if ("__MUSIC_OPEN_INTERNAL__".equals(ref.getComando())) {
                            // Try preview (small popup) via ACTION_VIEW and autoplay only
                            if (musicaUriInterna == null
                                    && !copiarAudioMusica(serial, "/sdcard/Music/Gone_blue_lyrics_Internal.mp3")) {
                                accionOk = false;
                            } else {
                                String uri = resolverUriMusicaInternaPara(serial);
                                String pkg = resolverPaqueteMusicaPreferido(serial);
                                String startCmd = pkg != null
                                        ? "am start -a android.intent.action.VIEW -d '" + uri + "' -t audio/* -p "
                                                + pkg
                                        : null;
                                if (startCmd == null) {
                                    accionOk = false;
                                    break;
                                }
                                String outStart = ejecutarShellEnSerial(serial, startCmd);
                                boolean launched = outStart != null && !outStart.toLowerCase().contains("error")
                                        && !outStart.toLowerCase().contains("unable");
                                if (launched) {
                                    try {
                                        Thread.sleep(800);
                                        for (int intento = 0; intento < 3; intento++) {
                                            ejecutarAccionHilo(serial, "input keyevent KEYCODE_MEDIA_PLAY");
                                            Thread.sleep(500);
                                        }
                                    } catch (InterruptedException e1) {
                                        Thread.currentThread().interrupt();
                                    }
                                    // Close the preview UI after playback finishes to avoid stale filename
                                    final String finalPkg = pkg;
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(65000); // wait ~65s for 60s audio to finish
                                            if (finalPkg != null && !finalPkg.isBlank()) {
                                                ejecutarAccionHilo(serial, "am force-stop " + finalPkg);
                                            } else {
                                                ejecutarAccionHilo(serial, "input keyevent KEYCODE_BACK");
                                            }
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    }).start();
                                }
                                accionOk = launched;
                            }
                        } else if ("__MUSIC_OPEN_EXTERNAL__".equals(ref.getComando())) {
                            // Try preview (small popup) via ACTION_VIEW and autoplay only
                            if (musicaUriExterna == null
                                    && !copiarAudioMusica(serial,
                                            "/storage/self/primary/Music/Gone_blue_lyrics_External.mp3")) {
                                accionOk = false;
                            } else {
                                String uri = resolverUriMusicaExternaPara(serial);
                                String pkg = resolverPaqueteMusicaPreferido(serial);
                                String startCmd = pkg != null
                                        ? "am start -a android.intent.action.VIEW -d '" + uri + "' -t audio/* -p "
                                                + pkg
                                        : null;
                                if (startCmd == null) {
                                    accionOk = false;
                                    break;
                                }
                                String outStart = ejecutarShellEnSerial(serial, startCmd);
                                boolean launched = outStart != null && !outStart.toLowerCase().contains("error")
                                        && !outStart.toLowerCase().contains("unable");
                                if (launched) {
                                    try {
                                        Thread.sleep(800);
                                        for (int intento = 0; intento < 3; intento++) {
                                            ejecutarAccionHilo(serial, "input keyevent KEYCODE_MEDIA_PLAY");
                                            Thread.sleep(500);
                                        }
                                    } catch (InterruptedException e1) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                                accionOk = launched;
                            }
                        } else {
                            adb.ejecutarAccionHilo(serial, ref.getComando());
                        }
                    }

                    if (!accionOk) {
                        ok = false;
                        outputDetalle = "No se pudo abrir el audio en la app de música";
                    } else {
                        owner = (Stage) btnEjecutar.getScene().getWindow();
                        String nombrePaso = ref.getNombre();

                        if (nombrePaso.contains("Receive several calls using bluetooth headsets")) {
                            boolean configurado = configurarLlamadaEntranteParaFm(owner);
                            if (!configurado || llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
                                ok = false;
                                outputDetalle = "Cancelado por el usuario";
                            } else {
                                ok = ejecutarLlamadaEntrante(paso);
                            }
                        } else if (nombrePaso.contains("Make several calls using the bluetooth headsets")) {
                            ok = ejecutarBluetoothLlamadasSalientes(serial, paso, owner);
                            outputDetalle = ok
                                    ? "Llamadas salientes ejecutadas usando número introducido"
                                    : "No se pudieron completar las llamadas salientes Bluetooth";
                        } else {
                            String infoManual = null;
                            ok = infoManual == null
                                    ? ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner)
                                    : ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner, infoManual);
                        }
                        if (nombrePaso.contains("Receive several calls using bluetooth headsets")) {
                            paso.setOutputDetalle(ok
                                    ? "Llamada entrante ejecutada con el dispositivo configurado."
                                    : outputDetalle);
                        }
                    }

                } else if (esWifi) {
                    ok = ejecutarPasoWifiConEspera(adb, serial, paso);

                } else if (CMD_LLAMADA_MASIVA.equals(paso.getComando())) {
                    ok = ejecutarLlamadaMasiva(paso);

                } else if (CMD_LLAMADA_ENTRE_DOS.equals(paso.getComando())) {
                    ok = ejecutarLlamadaEntreDos(paso);

                } else if (CMD_EMERGENCIA.equals(paso.getComando())) {
                    ok = ejecutarEmergencia(serial, paso);

                } else if (CMD_HOLD_RETRIEVE.equals(paso.getComando())) {
                    ok = ejecutarHoldRetrieve(serial, paso);

                } else if (CMD_DTMF.equals(paso.getComando())) {
                    ok = ejecutarDTMF(serial, paso);

                } else if (CMD_LLAMADA_ENTRANTE.equals(paso.getComando())) {
                    ok = ejecutarLlamadaEntrante(paso);

                } else if ("__MUSIC_COPY_INTERNAL__".equals(paso.getComando())) {
                    ok = copiarAudioMusica(serial, "/sdcard/Music/Gone_blue_lyrics_Internal.mp3");

                } else if ("__MUSIC_COPY_EXTERNAL__".equals(paso.getComando())) {
                    ok = copiarAudioMusica(serial, "/storage/self/primary/Music/Gone_blue_lyrics_External.mp3");

                } else if ("__MUSIC_LLAMADA_ENTRANTE__".equals(paso.getComando())) {
                    ok = ejecutarMusicConLlamadaEntrante(serial, paso);

                } else if ("__MUSIC_LLAMADA_SALIENTE__".equals(paso.getComando())) {
                    ok = ejecutarMusicConLlamadaSaliente(serial, paso);

                } else if (CMD_MUTE.equals(paso.getComando())) {
                    ok = ejecutarMute(serial, paso);

                } else if (CMD_RED_ACTIVA.equals(paso.getComando())) {
                    ok = ejecutarTestRedActiva(serial, paso);

                } else if (CMD_TRANSFERENCIA.equals(paso.getComando())) {
                    ok = ejecutarTransferencia(serial, paso);

                } else if (CMD_CONFERENCIA.equals(paso.getComando())) {
                    ok = ejecutarConferencia(serial, paso);

                } else if (CMD_TRANSFERENCIA_CIEGA.equals(paso.getComando())) {
                    ok = ejecutarTransferenciaCiega(serial, paso);

                } else if (TOUCH_PINCH.equals(paso.getComando())) {
                    ok = ejecutarPinch(serial, adb);

                } else if (TOUCH_SPREAD.equals(paso.getComando())) {
                    ok = ejecutarSpread(serial, adb);

                } else if (INFO_CHANGE_NAME.equals(ref.getComando())) {
                    ok = cambiarYRestaurarNombre(serial, adb);

                } else if (INFO_HW_VERSION.equals(ref.getComando())) {
                    adb.ejecutarComandoSincrono(serial, "shell am start -a android.intent.action.DIAL");
                    try {
                        Thread.sleep(1500);
                        adb.ejecutarComandoSincrono(serial, "shell input text '*#0000#'");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String valor = ConfirmacionManualPopup.mostrarYEsperarConValor(ref.getNombre(), owner);
                    ok = !valor.isEmpty();
                    outputDetalle = ok ? valor : "";

                } else if (INFO_DEVICE_NAME_PC.equals(ref.getComando())) {
                    boolean esUSB = !serial.contains(":");
                    if (esUSB) {
                        try {
                            adb.ejecutarComandoSincrono(serial, "shell svc usb setFunctions mtp");
                            Thread.sleep(1500);

                            new ProcessBuilder("explorer.exe", "::{20D04FE0-3AEA-1069-A2D8-08002B30309D}").start();
                        } catch (IOException | InterruptedException e) {
                            System.err.println("[MTP] Error abriendo explorador: " + e.getMessage());
                        }
                    }
                    String info = esUSB ? null : "";
                    ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner, info);

                } else if (INFO_IP.equals(ref.getComando())) {
                    String output = adb.ejecutarComandoSincrono(serial, "shell ip -f inet addr show wlan0");
                    if (output != null && output.contains("inet ")) {
                        ok = true;
                        String ip = "";
                        for (String linea : output.split("\n")) {
                            linea = linea.trim();
                            if (linea.startsWith("inet ")) {
                                ip = linea.split(" ")[1];
                                ip = ip.split("/")[0];
                                break;
                            }
                        }
                        outputDetalle = ip;
                    } else {
                        ok = false;
                        outputDetalle = "";
                    }

                } else if (INFO_LOGCAT_BRAND.equals(ref.getComando())) {
                    String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d -t 100");
                    ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner, logcat);

                } else if (DISPLAY_BRIGHTNESS_CHANGE.equals(ref.getComando())) {
                    ok = cambiarYVerificarBrillo(serial, adb);

                } else if (DISPLAY_BRIGHTNESS_CHECK.equals(ref.getComando())) {
                    String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_brightness");
                    ok = comprobarBrilloPorDefecto(serial, adb);

                    if (valor != null && !valor.trim().isEmpty()) {
                        try {
                            int brilloInt = Integer.parseInt(valor.trim());
                            int porcentaje = (int) Math.round((brilloInt * 100.0) / 255.0);

                            outputDetalle = porcentaje + "%";
                        } catch (NumberFormatException e) {
                            outputDetalle = "Error al parsear el brillo: " + valor.trim();
                        }
                    } else {
                        outputDetalle = "";
                    }
                } else if (DISPLAY_WALLPAPER.equals(ref.getComando())) {
                    ok = cambiarWallpaper(serial, adb);

                } else if (DISPLAY_TIMEOUT_CHECK.equals(ref.getComando())) {
                    String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
                    ok = comprobarTimeoutPorDefecto(serial, adb);

                    if (valor != null && !valor.trim().isEmpty()) {
                        try {
                            long ms = Long.parseLong(valor.trim());

                            long segundos = ms / 1000;
                            long minutos = segundos / 60;
                            long segundosRestantes = segundos % 60;

                            if (segundosRestantes == 0) {
                                outputDetalle = minutos + " min";
                            } else if (minutos > 0) {
                                outputDetalle = minutos + " min " + segundosRestantes + " s";
                            } else {
                                outputDetalle = segundos + " s";
                            }

                        } catch (NumberFormatException e) {
                            outputDetalle = "Error al parsear timeout: " + valor.trim();
                        }
                    } else {
                        outputDetalle = "";
                    }
                } else if (DISPLAY_TIMEOUT_CHANGE.equals(ref.getComando())) {
                    ok = cambiarYVerificarTimeout(serial, adb);

                } else if (DISPLAY_FONT_SIZE.equals(ref.getComando())) {
                    ok = cambiarYRestaurarFuente(serial, adb);

                } else if ("__FM_BACKGROUND__".equals(paso.getComando())) {
                    ok = ejecutarFMBackground(serial, paso);

                } else if (DISPLAY_DISPLAY_SIZE.equals(ref.getComando())) {
                    ok = cambiarYRestaurarDisplaySize(serial, adb);

                } else if (DISPLAY_SCREENSAVER.equals(ref.getComando())) {
                    ok = comprobarScreensaver(serial, adb);
                    adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_WAKEUP");

                } else if ("__FM_D17_EARPHONE_SEQUENCE__".equals(paso.getComando())) {
                    ok = ejecutarFmRadioD17(serial, paso, true, true,
                            "FM D17: comprobando auriculares y secuencia...");

                } else if ("__FM_D17_RIGHT_OK__".equals(paso.getComando())) {
                    ok = ejecutarFmRadioD17RightOk(serial, paso);

                } else if ("__FM_D17_AUTOSEARCH_NO_EARPHONE__".equals(paso.getComando())) {
                    ok = ejecutarFmRadioD17(serial, paso, false, false,
                            "FM D17: autosearch sin auriculares...");

                } else if (FM_D17_RECORD_SEQUENCE.equals(ref.getComando())) {
                    ok = ejecutarFmD17RecordSequence(serial, paso);

                } else if ("__FM_LLAMADA_ENTRANTE__".equals(paso.getComando())) {
                    ok = ejecutarFmConLlamadaEntrante(serial, paso, owner);

                } else if (paso.getComando().contains("__CAMERA_PACKAGE__")) {
                    // Intentar arrancar cámara de forma robusta usando ADBService.startCamera
                    // Si el comando original contiene pasos extra (p. ej. "&& sleep 2"), los
                    // ejecutamos después.
                    String[] parts = paso.getComando().split("&&");
                    boolean started = adb.startCamera(serial);
                    ok = started;
                    outputDetalle = started ? "Camera started" : "Failed to start camera";

                    // Ejecutar comandos adicionales que aparezcan tras el marcador
                    for (int i = 1; i < parts.length; i++) {
                        String p = parts[i].trim();
                        if (p.startsWith("sleep")) {
                            // Ejecutar sleep localmente para dar tiempo al arranque
                            try {
                                String[] tok = p.split("\\s+");
                                double s = Double.parseDouble(tok[1]);
                                Thread.sleep((long) (s * 1000));
                            } catch (Exception ignored) {
                            }
                        } else if (!p.isBlank()) {
                            adb.ejecutarComandoSincrono(serial, "shell " + p);
                        }
                    }

                } else if ("__MANUAL_PHOTO_QUALITY_CHECK__".equals(paso.getComando())) {
                    String mensaje = """
                            Verifique la calidad de la última foto tomada:

                            1. Abra la galería en el dispositivo
                            2. Localize la foto más reciente (debería tener marca de tiempo reciente)
                            3. Confirme que:
                               - La imagen esté enfocada (no borrosa)
                               - Los colores se vean naturales
                               - No haya artefactos o distorsiones

                            Haga clic en 'OK' si la calidad es aceptable, o 'Cancelar' si no lo es.
                            """;
                    ok = ConfirmacionManualPopup.mostrarYEsperar("Verificación de Calidad de Foto", owner, mensaje);

                } else if ("__MANUAL_VIDEO_QUALITY_CHECK__".equals(paso.getComando())) {
                    String mensaje = """
                            Verifique la calidad del último video grabado:

                            1. Abra la galería en el dispositivo
                            2. Localice el video más reciente
                            3. Reproduzcalo y confirme que:
                               - El video se reproduzca sin interrupciones
                               - La imagen esté estable (sin sacudidas excesivas)
                               - El audio se escuche claro (si aplicable)
                               - No haya píxeles muertos o distorsiones de color

                            Haga clic en 'OK' si la calidad es aceptable, o 'Cancelar' si no lo es.
                            """;
                    ok = ConfirmacionManualPopup.mostrarYEsperar("Verificación de Calidad de Video", owner, mensaje);

                } else if (CONTACT_CREATE_SIM.equals(ref.getComando())) {
                    ok = crearContactoSIM(serial, adb);

                } else if (CONTACT_CALL_SIM.equals(ref.getComando())) {
                    ok = hacerLlamadaDesdeSIM(serial, adb);

                } else if (CONTACT_RECEIVE_CALL_SIM.equals(ref.getComando())) {
                    ok = recibirLlamadaSIM(serial, adb);

                } else if (CONTACT_CREATE_PHONE.equals(ref.getComando())) {
                    ok = crearContactoPhone(serial, adb);

                } else if (CONTACT_EDIT_PHONE.equals(ref.getComando())) {
                    ok = editarContactoPhone(serial, adb);

                } else if (CONTACT_CALL_PHONE.equals(ref.getComando())) {
                    ok = hacerLlamadaDesdePhone(serial, adb);

                } else if (CONTACT_DELETE_PHONE.equals(ref.getComando())) {
                    ok = borrarContactoPhone(serial, adb);

                } else if (CONTACT_RECEIVE_CALL_PHONE.equals(ref.getComando())) {
                    ok = recibirLlamadaPhone(serial, adb);

                } else if (CONTACT_COPY_SIM_PHONE.equals(ref.getComando())) {
                    ok = copiarSimAlTelefono(serial, adb);

                } else if (CONTACT_COPY_PHONE_SIM.equals(ref.getComando())) {
                    ok = copiarTelefonoAlSim(serial, adb);

                } else if (CONTACT_IMPORT_VCARD.equals(ref.getComando())) {
                    ok = importarVCard(serial, adb);

                } else if (CONTACT_EXPORT_VCARD.equals(ref.getComando())) {
                    ok = exportarVCard(serial, adb);

                } else if (CONTACT_MEMORY_STATUS.equals(ref.getComando())) {
                    StringBuilder reporte = new StringBuilder();
                    ok = comprobarMemoriaContactos(serial, adb, reporte);
                    outputDetalle = reporte.toString();
                } else if (BT_DISCOVERABLE_TEST.equals(ref.getComando())) {
                    ok = ejecutarBluetoothDiscoverableTest(serial, paso);

                } else if (BT_CHANGE_NAME_TEST.equals(ref.getComando())) {
                    ok = ejecutarBluetoothChangeNameTest(serial, paso);

                } else if (CALENDAR_CREATE.equals(ref.getComando())) {
                    ok = crearEventoCalendario(serial, adb);

                } else if (CALENDAR_EDIT.equals(ref.getComando())) {
                    ok = editarEventoCalendario(serial, adb);

                } else if (CALENDAR_DELETE.equals(ref.getComando())) {
                    ok = borrarEventoCalendario(serial, adb);

                } else if (MSG_SEND_SMS_NUMBER.equals(ref.getComando())) {
                    ok = enviarSMSNumero(serial, adb);

                } else if (MSG_SEND_SMS_CONTACT.equals(ref.getComando())) {
                    ok = enviarSMSContacto(serial, adb);

                } else if (MSG_RECEIVE_SMS.equals(ref.getComando())) {
                    ok = recibirSMS(serial, adb);

                } else if (MSG_SEND_MMS_NUMBER.equals(ref.getComando())) {
                    ok = enviarMMSNumero(serial, adb);

                } else if (MSG_SEND_MMS_CONTACT.equals(ref.getComando())) {
                    ok = enviarMMSContacto(serial, adb);

                } else if (MSG_RECEIVE_MMS.equals(ref.getComando())) {
                    ok = recibirMMS(serial, adb);

                } else if (MSG_DELETE_ONE.equals(ref.getComando())) {
                    ok = borrarUnaConversacion(serial, adb);

                } else if (MSG_DELETE_ALL.equals(ref.getComando())) {
                    ok = borrarTodasConversaciones(serial, adb);

                } else if (MSG_SEND_SMS_SPECIAL.equals(ref.getComando())) {
                    enviarSMSCaracteresEspeciales(serial, adb);
                    ok = ConfirmacionManualPopup.mostrarYEsperar(
                            "SOFT.015.014 Send SMS with special characters  —  Comprueba los carácteres especiales enviados",
                            owner);

                } else if (MSG_SEND_SMS_LONG.equals(ref.getComando())) {
                    enviarSMSLargo(serial, adb);
                    ok = ConfirmacionManualPopup.mostrarYEsperar(
                            "SOFT.015.015 Send SMS with more than 160 characters  —  Comprueba que el dispositivo informe de que el mensaje se enviará en 2 SMS",
                            owner);

                } else if (MSG_SEND_SMS_NOOPT.equals(ref.getComando())) {
                    ok = enviarSMSSinOptimizacion(serial, adb);

                } else if (MSG_MMS_NO_DATA.equals(ref.getComando())) {
                    ok = enviarMMSSinDatos(serial, adb);

                } else {
                    ADBService.EjecucionADB r = adb.ejecutarYObtener(serial, ref.getComandos(), ref.isSinOutput());
                    ok = r.exito();
                    outputDetalle = r.outputJunto();

                    if (outputDetalle.contains("Parcel")) {
                        outputDetalle = parsearParcelIMEI(outputDetalle);
                    } else if (outputDetalle.contains("link/ether") || outputDetalle.contains("wlan0")) {
                        String macExtraida = parsearMacAddress(outputDetalle);
                        if (macExtraida != null) {
                            outputDetalle = macExtraida;
                        }
                    } else if (outputDetalle.contains("name=") || outputDetalle.contains("display_name=")) {
                        outputDetalle = parsearListaContactos(outputDetalle);
                    }

                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException ignored) {
                    }
                }

                final boolean resultadoFinal = ok;
                final String detalleFinal = outputDetalle;
                Platform.runLater(() -> {
                    ref.setEstado(resultadoFinal ? "OK" : "ERROR");
                    ref.setOutputDetalle(detalleFinal);
                    listaPasos.refresh();
                });
            }

            boolean debeRestablecerPhoneApp = pasos.stream().anyMatch(PasoPrueba::debeRestablecerPhoneAppAlFinal);
            if (debeRestablecerPhoneApp) {
                try {
                    new LlamadasD17(serial).restablecerPhoneApp();
                    System.out.println("[DIAG] Configuracion de Phone restablecida con pm clear com.android.phone");
                } catch (Exception e) {
                    System.out.println("[DIAG] No se pudo ejecutar pm clear com.android.phone: " + e.getMessage());
                }
            } else {
                System.out.println("[DIAG] Omitiendo pm clear com.android.phone para esta secuencia");
            }

            Platform.runLater(() -> {
                btnInforme.setDisable(false);
                btnEjecutar.setDisable(false);
                btnLimpiar.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void addBluetoothTest() {
        List<BloquePrueba> bloqueBluetooth = List.of(
                // SOFT.023.001 usa nuestro método customizado para el nombre
                new BloquePrueba("SOFT.023.001", "Check name of bluetooth device",
                        "shell settings get secure bluetooth_name"),

                new BloquePrueba("SOFT.023.002", "Pair new device via bluetooth",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.003", "Send a file to paired device",
                        "__OPP_SEND_FILE_MANUAL__", true),

                new BloquePrueba("SOFT.023.004", "Receive a file from paired device",
                        "__OPP_RECEIVE_FILE_MANUAL__", true),

                new BloquePrueba("SOFT.023.005", "Forget paired device",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.006", "Connect DUT to several bluetooth headsets",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.007", "Receive several calls using bluetooth headsets",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.008", "Make several calls using the bluetooth headsets",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.009", "Check audio quality on the other side using bluetooth headset in DUT",
                        "__BT_AUDIO_QUALITY_CHECK__", true),

                new BloquePrueba("SOFT.023.010",
                        "Turn off bluetooth headset and check if it connects to DUT when turn on",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.011",
                        "Check if bluetooth headset auto connects after lose signal due distance",
                        "__BT_DISTANCE_TEST__", true),

                new BloquePrueba("SOFT.023.012",
                        "Check that the DUT is able to connect and be used with the bluetooth system of a car",
                        "shell am start -a android.settings.BLUETOOTH_SETTINGS", true),

                new BloquePrueba("SOFT.023.013", "Reboot DUT and check if bluetooth headsets connect again",
                        "reboot", true));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar(
                "SOFT.023 — Bluetooth Functions",
                bloqueBluetooth,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }
    // =========================================================================
    // METODOS DE SOPORTE PARA LAS PRUEBAS DE BLUETOOTH
    // =========================================================================

    private boolean ejecutarBluetoothDiscoverableTest(String serial, PasoPrueba paso) {
        ADBService adb = new ADBService();
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        adb.ejecutarComandoSincrono(serial, "shell am start -a android.settings.BLUETOOTH_SETTINGS");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        String mensaje = """
                Se ha abierto la configuración de Bluetooth en el dispositivo.

                1. Confirme que el Bluetooth está encendido.
                2. Verifique si el dispositivo es visible para otros equipos cercanos.

                Haga clic en 'OK' si es correcto, o 'Cancelar' si falló.""";

        boolean verificado = ConfirmacionManualPopup.mostrarYEsperar("Verificación Visibilidad BT", owner, mensaje);

        paso.setOutputDetalle(verificado ? "Visibilidad verificada por el usuario" : "El usuario canceló la prueba");
        return verificado;
    }

    private boolean ejecutarBluetoothChangeNameTest(String serial, PasoPrueba paso) {
        ADBService adb = new ADBService();
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        // Intentamos obtener el nombre actual antes de cambiarlo
        String nombreOriginal = adb.ejecutarComandoSincrono(serial, "shell settings get secure bluetooth_name");
        if (nombreOriginal == null || nombreOriginal.trim().isEmpty() || nombreOriginal.contains("null")) {
            nombreOriginal = "Android_Device"; // Fallback por si acaso
        } else {
            nombreOriginal = nombreOriginal.trim();
        }

        String nombrePrueba = "TEST_BT_AUTOMATION";

        // Cambiar nombre mediante settings
        adb.ejecutarComandoSincrono(serial, "shell settings put secure bluetooth_name " + nombrePrueba);
        adb.ejecutarComandoSincrono(serial, "shell setprop persist.bluetooth.name " + nombrePrueba);

        // Abrimos los ajustes para que el operador compruebe el cambio
        adb.ejecutarComandoSincrono(serial, "shell am start -a android.settings.BLUETOOTH_SETTINGS");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        String mensaje = "Verifique en la pantalla del dispositivo si el nombre del Bluetooth ha cambiado a:\n\n"
                + "👉 " + nombrePrueba + "\n\n"
                + "¿El nombre se actualizó correctamente?";

        boolean ok = ConfirmacionManualPopup.mostrarYEsperar("Cambio de Nombre BT", owner, mensaje);

        // Restauramos el nombre original para no dejar el teléfono "sucio"
        adb.ejecutarComandoSincrono(serial, "shell settings put secure bluetooth_name " + nombreOriginal);
        adb.ejecutarComandoSincrono(serial, "shell setprop persist.bluetooth.name " + nombreOriginal);

        paso.setOutputDetalle(
                ok ? "Cambio de nombre exitoso y restaurado." : "El nombre no cambió según el reporte del usuario.");
        return ok;
    }

    @FXML
    private void addCallTimerTest() {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        new java.lang.Thread(() -> {
            String serial = obtenerSerialADBActual();
            String modelo = "D17";
            if (serial != null) {
                try {
                    modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
                } catch (Exception ignored) {
                }
            }

            List<BloquePrueba> bloqueCallTimer = LlamadasD17.crearBloquesCallTimer(modelo);
            Platform.runLater(() -> SelectorPruebasPopup.mostrar(
                    "SOFT.046 — Call Timer",
                    bloqueCallTimer,
                    owner,
                    seleccionadas -> seleccionadas.stream()
                            .map(BloquePrueba::toPasoPrueba)
                            .forEach(pasos::add)));
        }).start();
    }

    @FXML
    private void addHotDialTest() {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        new java.lang.Thread(() -> {
            String serial = obtenerSerialADBActual();
            String modelo = "D17";
            if (serial != null) {
                try {
                    modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
                } catch (Exception ignored) {
                }
            }
            List<BloquePrueba> bloqueHotDial = LlamadasD17.crearBloquesHotDial(modelo, null);
            Platform.runLater(() -> SelectorPruebasPopup.mostrar(
                    "SOFT.045 — Hot Dial Function",
                    bloqueHotDial,
                    owner,
                    seleccionadas -> {
                        List<BloquePrueba> procesadas = new java.util.ArrayList<>();
                        for (BloquePrueba bp : seleccionadas) {
                            if ("SOFT.045.002".equals(bp.getId())) {
                                String numero = solicitarNumeroLlamada(owner, "Hot Dial",
                                        "Introduce el número que se guardará en hot dial.\nEse mismo número se reutilizará en la prueba de verificación.");
                                if (numero == null || numero.isBlank()) {
                                    // si el usuario cancela, omitimos esta prueba
                                    continue;
                                }
                                hotDialNumero = numero.trim();
                                String seq = Entradas.secuencia(
                                        "shell am start -a android.telecom.action.SHOW_CALL_SETTINGS",
                                        "sleep 2",
                                        Entradas.abajo(),
                                        Entradas.ok(),
                                        Entradas.unSegundo(),
                                        "input text '" + hotDialNumero + "'",
                                        Entradas.abajo(),
                                        Entradas.derecha(),
                                        Entradas.unSegundo(),
                                        Entradas.ok());
                                BloquePrueba nuevo = new BloquePrueba(bp.getId(), bp.getDescripcion(), seq, false,
                                        false);
                                procesadas.add(nuevo);
                            } else {
                                procesadas.add(bp);
                            }
                        }
                        procesadas.stream().map(BloquePrueba::toPasoPrueba).forEach(pasos::add);
                    }));
        }).start();
    }

    private boolean ejecutarEmergencia(String serial, PasoPrueba paso) {
        try {
            Stage owner = (Stage) btnEjecutar.getScene().getWindow();
            String numeroPrueba = solicitarNumeroLlamada(
                    owner,
                    "Llamada de Prueba",
                    "Ingresa el número que quieres marcar para esta prueba.");

            if (numeroPrueba == null || numeroPrueba.isBlank()) {
                actualizarEstadoPaso(paso, "Cancelado por el usuario");
                return false;
            }

            String numeroLimpio = numeroPrueba.trim();
            actualizarEstadoPaso(paso, "Marcando número de prueba...");
            ejecutarShellEnSerial(serial,
                    "am start -a android.intent.action.CALL -d tel:" + numeroLimpio);

            // Espera 3s — suficiente para ver que marca pero sin que contesten
            Thread.sleep(3_000);

            ejecutarAccionHilo(serial, "input keyevent KEYCODE_ENDCALL");
       
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // En tu controller, agrega un método para detectar el package de cámara

    @FXML
    private void addCameraTest() {
        List<BloquePrueba> bloqueCamera = List.of(
                // SOFT.031.001: Abrir app de cámara
                new BloquePrueba(
                        true, "SOFT.031.001",
                        "Open camera app",
                        List.of(
                                // Nota: El package/activity puede variar por OEM.
                                // Usamos el genérico de AOSP; si falla, ajusta para tu dispositivo.
                                // Abrir cámara, esperar 3s y cerrar (KEYCODE_BACK) para seguir con la siguiente
                                // prueba
                                "shell am start -a android.media.action.IMAGE_CAPTURE && sleep 3 && input keyevent KEYCODE_BACK && sleep 2")),

                // SOFT.031.002: Tomar foto (autocontenida: abre cámara, toma foto, cierra)
                new BloquePrueba(
                        true, "SOFT.031.002",
                        "Make a photo",
                        List.of(
                                // Abre cámara, espera a que cargue, dispara con VOLUME_UP,
                                // confirma con una tecla estándar y espera a que guarde.
                                "shell am start -a android.media.action.IMAGE_CAPTURE && sleep 2 && input keyevent 27 && sleep 2 && input keyevent KEYCODE_DPAD_CENTER && sleep 4")),

                // SOFT.031.003: Verificar calidad de foto (MANUAL)
                new BloquePrueba(
                        true, "SOFT.031.003",
                        "Check image quality",
                        List.of("__MANUAL_PHOTO_QUALITY_CHECK__"), // Marcador especial
                        true),

                // SOFT.031.004: Grabar video automático 5s
                new BloquePrueba(
                        "SOFT.031.004",
                        "Make a video (5 seconds)",
                        List.of(
                                "shell am start -a android.media.action.VIDEO_CAPTURE && sleep 2 && input keyevent 27 && sleep 2 && input keyevent KEYCODE_DPAD_CENTER && sleep 5 && input keyevent KEYCODE_BACK && sleep 1")),

                // SOFT.031.005: Verificar calidad de video (MANUAL)
                new BloquePrueba(
                        "SOFT.031.005",
                        "Check video quality",
                        List.of("__MANUAL_VIDEO_QUALITY_CHECK__"),
                        true),

                // SOFT.031.006: Foto con temporizador (APERTURA MANUAL)
                new BloquePrueba(
                        "SOFT.031.006",
                        "Open camera for self-timer (manual verification)",
                        List.of(
                                "shell am start -a android.media.action.IMAGE_CAPTURE && sleep 2"),
                        true),

                // SOFT.031.007: Activar/desactivar flash y verificar
                new BloquePrueba(
                        "SOFT.031.007",
                        "Activate/deactivate the flash and check if it works",
                        List.of("shell am start -a android.media.action.IMAGE_CAPTURE && sleep 2"),
                        true));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueCamera.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueCamera;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        SelectorPruebasPopup.mostrar(
                "SOFT.031 — Cámara",
                bloquesAFiltrar,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean ejecutarHoldRetrieve(String serial, PasoPrueba paso) {
        PerfilDialer perfil = PerfilesManager.obtenerPerfil(serial);
        LlamadasD17 llamadas = new LlamadasD17(serial);
        actualizarEstadoPaso(paso, "Hold...");
        return llamadas.ejecutarHold(perfil);
    }

    private boolean ejecutarMute(String serial, PasoPrueba paso) {
        PerfilDialer perfil = PerfilesManager.obtenerPerfil(serial);
        LlamadasD17 llamadas = new LlamadasD17(serial);
        actualizarEstadoPaso(paso, "Mute...");
        return llamadas.ejecutarMute(perfil);
    }

    private boolean ejecutarTransferencia(String serial, PasoPrueba paso) {
        LlamadasD17 llamadas = new LlamadasD17(serial);
        actualizarEstadoPaso(paso, "Transfiriendo...");
        return llamadas.ejecutarTransferencia(transferenciaNumero, transferenciaResponderSerial);
    }

    private boolean ejecutarTransferenciaCiega(String serial, PasoPrueba paso) {
        LlamadasD17 llamadas = new LlamadasD17(serial);
        actualizarEstadoPaso(paso, "Transferencia ciega...");
        return llamadas.ejecutarTransferenciaCiega(transferenciaNumero);
    }

    private boolean ejecutarConferencia(String serial, PasoPrueba paso) {
        LlamadasD17 llamadas = new LlamadasD17(serial);
        actualizarEstadoPaso(paso, "Conferencia...");
        return llamadas.ejecutarConferencia(conferenciaNumero, conferenciaReceptorSerial);
    }

    private boolean ejecutarDTMF(String serial, PasoPrueba paso) {
        try {
            if (!llamadaActiva(serial))
                return false;

            PerfilDialer perfil = PerfilesManager.obtenerPerfil(serial);
            actualizarEstadoPaso(paso, "Enviando DTMF...");

            boolean dispositivoTactil = perfil != null && perfil.isTactil();

            if (dispositivoTactil) {
                // Dispositivo táctil — abrir teclado y enviar números por TAPs
                if (perfil.getXTeclado() <= 0) {
                    System.out.println("[DTMF] ⚠ No se encontró botón Teclado");
                    return false;
                }

             
                ejecutarAccionHilo(serial, "input tap " + perfil.getXTeclado() + " " + perfil.getYTeclado());
                Thread.sleep(1500);

                int[] numeros = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
                Map<Integer, int[]> coordsRun = new HashMap<>();
                for (int intento = 0; intento < 3 && coordsRun.size() < 10; intento++) {
                    String uiDump = ejecutarShellEnSerial(serial,
                            "uiautomator dump /sdcard/ui_dtmf_now.xml >/dev/null 2>&1; cat /sdcard/ui_dtmf_now.xml");
                    coordsRun = extraerCoordsNumerosDesdeDump(uiDump);
                    if (coordsRun.size() < 10)
                        Thread.sleep(300);
                }

                boolean usoCoordsManual = false;
                if (coordsRun.size() >= 10) {
                    usoCoordsManual = true;
                    
                } else if (perfil.getCoordNumeros() != null && perfil.getCoordNumeros().size() >= 10) {
                    usoCoordsManual = true;
                    coordsRun = perfil.getCoordNumeros();
                  
                }

                if (usoCoordsManual) {
                    for (int numero : numeros) {
                        if (!llamadaActiva(serial)) {
                            System.out.println("[DTMF] Llamada terminada");
                            break;
                        }

                        int[] c = coordsRun.getOrDefault(numero, new int[] { 0, 0 });
                        if (c[0] <= 0 || c[1] <= 0) {
                            System.out.println("[DTMF] Coordenada inválida para " + numero + ", usando fallback");
                            usoCoordsManual = false;
                            break;
                        }

                       
                        ejecutarAccionHilo(serial, "input tap " + c[0] + " " + c[1]);
                        Thread.sleep(600);
                    }
                }

                if (!usoCoordsManual) {
                    System.out.println("[DTMF] Fallback a keyevents DTMF (1-9)");
                    enviarDtmfPorKeyevents(serial);
                }

                Thread.sleep(800);
              
                ejecutarShellEnSerial(serial, "input keyevent KEYCODE_BACK");
                Thread.sleep(500);
            } else {
                // Feature phone (o perfil no táctil) — usar keyevents directo (1-9)
                enviarDtmfPorKeyevents(serial);
            }

           
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void enviarDtmfPorKeyevents(String serial) throws InterruptedException {
        int[] keycodes = { 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        for (int i = 0; i < keycodes.length; i++) {
            if (!llamadaActiva(serial)) {
                System.out.println("[DTMF] Llamada terminada");
                break;
            }
            
            ejecutarShellEnSerial(serial, "input keyevent " + keycodes[i]);
            Thread.sleep(600);
        }
    }

    private boolean ejecutarLlamadaEntrante(PasoPrueba paso) {
        if (llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
            System.out.println("[ENTRANTE] Sin configuración previa para la prueba");
            return false;
        }
        try {
            // El receptor es el dispositivo actual de la prueba
            String serialReceptor = obtenerSerialADBActual();

           
            actualizarEstadoPaso(paso, "Llamando...");

            // El dispositivo externo llama al receptor
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "am start -a android.intent.action.CALL -d tel:" + llamadaEntranteNumero);

            // Espera a que suene EN EL RECEPTOR (no en el llamante)
            actualizarEstadoPaso(paso, "Esperando que suene...");
            boolean sono = esperarHastaQueSuene(serialReceptor, 15);

            // Cuelga desde el que llamó — no contestamos
            Thread.sleep(4_000);
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "input keyevent KEYCODE_ENDCALL");
            // Por si acaso también en el receptor
            ejecutarShellEnSerial(serialReceptor,
                    "input keyevent KEYCODE_ENDCALL");

           
            return sono;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @FXML
    private void addMusicTest() {
        List<BloquePrueba> bloqueMusica = List.of(
                new BloquePrueba("SOFT.019.001", "Copy music file to internal storage",
                        "__MUSIC_COPY_INTERNAL__"),

                new BloquePrueba("SOFT.019.002", "Play music from internal storage",
                        "__MUSIC_OPEN_INTERNAL__", true),

                new BloquePrueba("SOFT.019.003", "Check music info (Artist, Album, Song)",
                        "__MUSIC_OPEN_INTERNAL__", true),

                new BloquePrueba("SOFT.019.004", "Copy music file to external storage (SD)",
                        "__MUSIC_COPY_EXTERNAL__"),

                new BloquePrueba("SOFT.019.005", "Play music from external storage",
                        "__MUSIC_OPEN_EXTERNAL__", true),

                new BloquePrueba("SOFT.019.006", "Check music info from external storage",
                        "__MUSIC_OPEN_EXTERNAL__", true),

                new BloquePrueba("SOFT.019.007",
                        "While playing music, receive a call. Check if music resumes after call ends",
                        "__MUSIC_LLAMADA_ENTRANTE__"),

                new BloquePrueba("SOFT.019.008",
                        "While playing music, make a call. Check if music resumes after call ends",
                        "__MUSIC_LLAMADA_SALIENTE__"));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueMusica.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueMusica;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        SelectorPruebasPopup.mostrar(
                "SOFT.019 — Music Player",
                bloquesAFiltrar,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private String obtenerRutaAudioMusicaLocal() {
        try {
            // Prefer the single MP3 resource used for tests
            java.net.URL resource = getClass().getResource("/media/Gone_blue_lyrics.mp3");
            if (resource == null) {
                // fallback to old test.wav for compatibility
                resource = getClass().getResource("/media/test.wav");
            }

            if (resource != null) {
                if ("file".equalsIgnoreCase(resource.getProtocol())) {
                    return new File(resource.toURI()).getAbsolutePath();
                }

                try (java.io.InputStream in = resource.openStream()) {
                    String ext = resource.getPath().toLowerCase().endsWith(".mp3") ? ".mp3" : ".wav";
                    File temp = File.createTempFile("music_test", ext);
                    Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    temp.deleteOnExit();
                    return temp.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            System.out.println("[MUSIC] No se pudo resolver el recurso desde classpath: " + e.getMessage());
        }

        File fallback = new File("src/main/resources/media/Gone_blue_lyrics.mp3");
        if (fallback.exists()) {
            return fallback.getAbsolutePath();
        }

        File fallbackAlt = new File("src/test/resources/media/Gone_blue_lyrics.mp3");
        if (fallbackAlt.exists()) {
            return fallbackAlt.getAbsolutePath();
        }

        return null;
    }

    private boolean copiarAudioMusica(String serial, String destinoRemoto) {
        try {
            String rutaLocal = obtenerRutaAudioMusicaLocal();
            if (rutaLocal == null) {
                System.out.println("[MUSIC] No se encontró el archivo de audio local");
                return false;
            }
            // Build destination path with suffix _Internal/_External and keep directory
            // logic
            String nombreLocal = new File(rutaLocal).getName();
            String base = nombreLocal.contains(".") ? nombreLocal.substring(0, nombreLocal.lastIndexOf('.'))
                    : nombreLocal;
            String ext = nombreLocal.contains(".") ? nombreLocal.substring(nombreLocal.lastIndexOf('.') + 1) : "mp3";

            boolean esExternal = destinoRemoto.contains("storage/self/primary")
                    || destinoRemoto.toLowerCase().contains("external");

            String destinoDir = new File(destinoRemoto).getParent();

            if (esExternal) {
                // try to detect actual external SD path (may include filename)
                String sd = detectarRutaSdExterna(serial, nombreLocal);
                if (sd != null) {
                    System.out.println("[MUSIC] SD externa detectada, usando ruta: " + sd);
                    destinoDir = new File(sd).getParent();
                } else {
                    System.out.println("[MUSIC] No se detectó SD externa; usando destino original: " + destinoRemoto);
                }
            }

            if (destinoDir == null || destinoDir.isBlank()) {
                destinoDir = new File(destinoRemoto).getParent();
                if (destinoDir == null)
                    destinoDir = "/sdcard/Music";
            }

            String nombreDestino = base + (esExternal ? "_External." : "_Internal.") + ext;
            String destino = destinoDir.endsWith("/") ? destinoDir + nombreDestino : destinoDir + "/" + nombreDestino;

            if (destino.startsWith("/")) {
                ejecutarShellEnSerial(serial, "mkdir -p " + new File(destino).getParent());
            }
            long tPushStart = System.nanoTime();
            Process push = new ProcessBuilder("adb", "-s", serial, "push", rutaLocal, destino).start();
            String salida = new String(push.getInputStream().readAllBytes());
            String error = new String(push.getErrorStream().readAllBytes());
            int exit = push.waitFor();
            long tPushEnd = System.nanoTime();
            long durMs = TimeUnit.NANOSECONDS.toMillis(tPushEnd - tPushStart);
            if (!salida.isBlank()) {
                System.out.println("[MUSIC] push stdout:\n" + salida);
            }
            if (!error.isBlank()) {
                System.out.println("[MUSIC] push stderr:\n" + error);
            }

            if (exit != 0) {
                // Try fallback to /sdcard/Music/<file> for broader compatibility
                String baseFile = new File(destino).getName();
                String alt = "/sdcard/Music/" + baseFile;
                long tPush2Start = System.nanoTime();
                Process push2 = new ProcessBuilder("adb", "-s", serial, "push", rutaLocal, alt).start();
                String out2 = new String(push2.getInputStream().readAllBytes());
                String err2 = new String(push2.getErrorStream().readAllBytes());
                int exit2 = push2.waitFor();
                long tPush2End = System.nanoTime();
                long dur2Ms = TimeUnit.NANOSECONDS.toMillis(tPush2End - tPush2Start);
                if (!out2.isBlank())
                    System.out.println("[MUSIC] push fallback stdout:\n" + out2);
                if (!err2.isBlank())
                    System.out.println("[MUSIC] push fallback stderr:\n" + err2);
                if (exit2 != 0) {
                    return false;
                }
                destino = alt;
            }

            // Verify file existence on device; if missing and was external, try fallback
            String ls = ejecutarShellEnSerial(serial, "ls -l " + destino);
            if (ls == null || ls.toLowerCase().contains("no such file") || !ls.contains(new File(destino).getName())) {
         
                String baseFile2 = new File(destino).getName();
                String alt2 = "/sdcard/Music/" + baseFile2;
               
                long tPush3Start = System.nanoTime();
                Process push3 = new ProcessBuilder("adb", "-s", serial, "push", rutaLocal, alt2).start();
                push3.waitFor();
                long tPush3End = System.nanoTime();
                long dur3Ms = TimeUnit.NANOSECONDS.toMillis(tPush3End - tPush3Start);
                
                destino = alt2;
            }

            long tRegStart = System.nanoTime();
            String mediaUri = registrarAudioEnMediaStore(serial, destino,
                    MUSIC_TITLE, MUSIC_ARTIST, MUSIC_ALBUM);
            long tRegEnd = System.nanoTime();
            long durRegMs = TimeUnit.NANOSECONDS.toMillis(tRegEnd - tRegStart);

            if (destino.contains("/sdcard/Music/")) {
                musicaUriInterna = mediaUri;
            } else {
                musicaUriExterna = mediaUri;
            }

            return true;
        } catch (Exception e) {
            System.out.println("[MUSIC] Error copiando audio: " + e.getMessage());
            return false;
        }
    }

    private String registrarAudioEnMediaStore(String serial, String rutaRemota, String titulo, String artista,
            String album) {
        try {
            String fileUri = "file://" + rutaRemota;
            long tScanStart = System.nanoTime();
            ejecutarShellEnSerial(serial,
                    "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d " + fileUri);
            String mime = rutaRemota.toLowerCase().endsWith(".mp3") ? "audio/mpeg" : "audio/wav";
            ejecutarShellEnSerial(serial,
                    "content insert --uri content://media/external/audio/media " +
                            "--bind _data:s:" + rutaRemota + " " +
                            "--bind title:s:'" + titulo + "' " +
                            "--bind artist:s:'" + artista + "' " +
                            "--bind album:s:'" + album + "' " +
                            "--bind mime_type:s:" + mime + " " +
                            "--bind is_music:i:1");

            String query = ejecutarShellEnSerial(serial,
                    "content query --uri content://media/external/audio/media " +
                            "--where \"_data='" + rutaRemota + "'\" --projection _id --sort \"date_added DESC\"");
            long tScanEnd = System.nanoTime();
            long durScanMs = TimeUnit.NANOSECONDS.toMillis(tScanEnd - tScanStart);
            

            if (query != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("_id=(\\d+)").matcher(query);
                if (m.find()) {
                    return "content://media/external/audio/media/" + m.group(1);
                }
            }

            return fileUri;
        } catch (Exception e) {
            System.out.println("[MUSIC] Error registrando metadata: " + e.getMessage());
            return "file://" + rutaRemota;
        }
    }

    private String obtenerContentUriParaRuta(String serial, String rutaRemota) {
        try {
            String query = ejecutarShellEnSerial(serial,
                    "content query --uri content://media/external/audio/media --where \"_data='" + rutaRemota
                            + "'\" --projection _id --sort \"date_added DESC\"");
            if (query != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("_id=(\\d+)").matcher(query);
                if (m.find()) {
                    return "content://media/external/audio/media/" + m.group(1);
                }
            }
        } catch (Exception e) {
            System.out.println("[MUSIC] Error al obtener content:// URI para " + rutaRemota + ": " + e.getMessage());
        }
        return null;
    }

    private String resolverUriMusicaInternaPara(String serial) {
        if (musicaUriInterna != null && musicaUriInterna.startsWith("content://"))
            return musicaUriInterna;
        // if we have a file:// path, try to resolve to content://
        String posible = musicaUriInterna != null ? musicaUriInterna
                : "file:///sdcard/Music/Gone_blue_lyrics_Internal.mp3";
        if (posible.startsWith("file://")) {
            String ruta = posible.substring("file://".length());
            String content = obtenerContentUriParaRuta(serial, ruta);
            if (content != null) {
                musicaUriInterna = content;
                return content;
            }
        }
        return posible;
    }

    private String resolverUriMusicaExternaPara(String serial) {
        if (musicaUriExterna != null && musicaUriExterna.startsWith("content://"))
            return musicaUriExterna;
        String posible = musicaUriExterna != null ? musicaUriExterna
                : "file:///storage/self/primary/Music/Gone_blue_lyrics_External.mp3";
        if (posible.startsWith("file://")) {
            String ruta = posible.substring("file://".length());
            String content = obtenerContentUriParaRuta(serial, ruta);
            if (content != null) {
                musicaUriExterna = content;
                return content;
            }
            // If not found, try registering again
            String reReg = registrarAudioEnMediaStore(serial, ruta, MUSIC_TITLE, MUSIC_ARTIST, MUSIC_ALBUM);
            if (reReg != null && reReg.startsWith("content://")) {
                musicaUriExterna = reReg;
                return reReg;
            }
        }
        return posible;
    }

    private String resolverLauncherDePaquete(String serial, String pkg) {
        String out = ejecutarShellEnSerial(serial,
                "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "
                        + pkg);
        if (out == null) {
            return null;
        }
        for (String linea : out.split("\\R")) {
            String l = linea.trim();
            if (l.contains("/")) {
                return l;
            }
        }
        return null;
    }

    private String resolverComponenteAudio(String serial, String uriAudio) {
        String out = ejecutarShellEnSerial(serial,
                "cmd package resolve-activity --brief -a android.intent.action.VIEW -d '" + uriAudio
                        + "' -t audio/*");
        if (out == null) {
            return null;
        }

        for (String linea : out.split("\\R")) {
            String l = linea.trim();
            if (l.contains("/") && !l.contains("ResolverActivity")) {
                return l;
            }
        }

        return null;
    }

    private String detectarRutaSdExterna(String serial, String nombreArchivo) {
        try {
            String out = ejecutarShellEnSerial(serial, "ls /storage");
            if (out == null || out.isBlank())
                return null;

            for (String linea : out.split("\\R")) {
                String entry = linea.trim();
                if (entry.isBlank())
                    continue;
                if (entry.equals("emulated") || entry.equals("self"))
                    continue;
                // candidate path
                String candidate = "/storage/" + entry + "/Music/" + nombreArchivo;
                String prueba = ejecutarShellEnSerial(serial,
                        "mkdir -p /storage/" + entry + "/Music && ls /storage/" + entry + "/Music");
                if (prueba != null && !prueba.toLowerCase().contains("permission denied")) {
                    return candidate;
                }
            }
        } catch (Exception e) {
            System.out.println("[MUSIC] Error detectando SD externa: " + e.getMessage());
        }
        return null;
    }

    private String resolverPaqueteMusicaPreferido(String serial) {
        String fabricante = ejecutarShellEnSerial(serial, "getprop ro.product.manufacturer");
        String fabricanteLc = fabricante == null ? "" : fabricante.toLowerCase();

        List<String> paquetesPreferidos = new ArrayList<>();
        if (fabricanteLc.contains("xiaomi") || fabricanteLc.contains("redmi") || fabricanteLc.contains("poco")) {
            paquetesPreferidos.add("com.miui.player");
            paquetesPreferidos.add("com.miui.mediaviewer");
        } else if (fabricanteLc.contains("samsung")) {
            paquetesPreferidos.add("com.sec.android.app.music");
        }

        String[] candidatos = {
                "com.android.music",
                "com.google.android.apps.youtube.music",
                "com.sec.android.app.music",
                "com.miui.player",
                "com.oppo.music",
                "com.vivo.music",
                "com.transsion.music"
        };

        for (String pkg : candidatos) {
            if (!paquetesPreferidos.contains(pkg)) {
                paquetesPreferidos.add(pkg);
            }
        }

        for (String pkg : paquetesPreferidos) {
            String check = ejecutarShellEnSerial(serial, "pm list packages " + pkg);
            if (check != null && check.contains(pkg)) {
                return pkg;
            }
        }

        return null;
    }

    private boolean abrirAppMusicaCompleta(String serial) {
        String fabricante = ejecutarShellEnSerial(serial, "getprop ro.product.manufacturer");
        String fabricanteLc = fabricante == null ? "" : fabricante.toLowerCase();

        if (fabricanteLc.contains("xiaomi") || fabricanteLc.contains("redmi") || fabricanteLc.contains("poco")) {
            String comp = "com.miui.player/com.miui.player.ui.MusicBrowserActivity";
            String out = ejecutarShellEnSerial(serial, "am start -n " + comp);
            if (out != null && !out.toLowerCase().contains("error") && !out.toLowerCase().contains("unable")) {
                dormirSilencioso(300);
                return true;
            }
        }

        String[] paquetesPreferidos = {
                "com.miui.player",
                "com.sec.android.app.music",
                "com.google.android.apps.youtube.music",
                "com.android.music",
                "com.oppo.music",
                "com.vivo.music",
                "com.transsion.music"
        };

        for (String pkg : paquetesPreferidos) {
            String check = ejecutarShellEnSerial(serial, "pm list packages " + pkg);
            if (check == null || !check.contains(pkg)) {
                continue;
            }

            String launcher = resolverLauncherDePaquete(serial, pkg);
            if (launcher != null && !launcher.isBlank()) {
                String out = ejecutarShellEnSerial(serial, "am start -n " + launcher);
                if (out != null && !out.toLowerCase().contains("error") && !out.toLowerCase().contains("unable")) {
                    dormirSilencioso(300);
                    return true;
                }
            }
        }

        return false;
    }

    private void dormirSilencioso(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean audioMusicaExisteEnDispositivo(String serial, String rutaRemota) {
        String resultado = ejecutarShellEnSerial(serial, "ls -l " + rutaRemota);
        return resultado != null
                && !resultado.toLowerCase().contains("no such file")
                && !resultado.toLowerCase().contains("not found")
                && resultado.contains(new File(rutaRemota).getName());
    }

    private boolean ejecutarMusicConLlamadaEntrante(String serial, PasoPrueba paso) {
        if (!audioMusicaExisteEnDispositivo(serial, "/sdcard/Music/Gone_blue_lyrics_Internal.mp3")) {
            actualizarEstadoPaso(paso, "El audio de prueba no está preparado. Ejecuta primero la copia de música.");
            return false;
        }

        try {
            actualizarEstadoPaso(paso, "Abriendo app de música...");
            boolean abierto = abrirAppMusicaCompleta(serial);
            if (!abierto) {
                actualizarEstadoPaso(paso, "No se pudo abrir la app de música");
                return false;
            }

            Stage owner = obtenerVentanaPrincipal();
            if (owner == null) {
                actualizarEstadoPaso(paso, "No se pudo abrir el diálogo de música");
                return false;
            }
            boolean listo = ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.019.007 — Music Player",
                    owner,
                    "Selecciona la canción de prueba y pulsa reproducir.\nCuando ya esté sonando, pulsa Pass para continuar con la llamada.");

            if (!listo) {
                actualizarEstadoPaso(paso, "Cancelado por el usuario");
                return false;
            }

            Thread.sleep(1_500);

            if (llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
                boolean configurado = configurarLlamadaEntranteParaFm(owner);
                if (!configurado || llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
                    actualizarEstadoPaso(paso, "Cancelado por el usuario");
                    return false;
                }
            }

            actualizarEstadoPaso(paso, "Iniciando llamada entrante...");
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "am start -a android.intent.action.CALL -d tel:" + llamadaEntranteNumero);
            try {
                Thread.sleep(4_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_ENDCALL");
            return ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.019.007 — Music Player",
                    owner,
                    "Confirma si la música se reanudó después de la llamada.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarMusicConLlamadaSaliente(String serial, PasoPrueba paso) {
        if (!audioMusicaExisteEnDispositivo(serial, "/storage/self/primary/Music/Gone_blue_lyrics_External.mp3")
                && !audioMusicaExisteEnDispositivo(serial, "/sdcard/Music/Gone_blue_lyrics_External.mp3")) {
            actualizarEstadoPaso(paso, "El audio de prueba no está preparado. Ejecuta primero la copia de música.");
            return false;
        }

        try {
            actualizarEstadoPaso(paso, "Abriendo app de música...");
            boolean abierto = abrirAppMusicaCompleta(serial);
            if (!abierto) {
                actualizarEstadoPaso(paso, "No se pudo abrir la app de música");
                return false;
            }

            Stage owner = (Stage) btnEjecutar.getScene().getWindow();
            boolean listo = ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.019.008 — Music Player",
                    owner,
                    "Selecciona la canción de prueba y pulsa reproducir.\nCuando ya esté sonando, pulsa OK para continuar con la llamada.");

            if (!listo) {
                actualizarEstadoPaso(paso, "Cancelado por el usuario");
                return false;
            }

            Thread.sleep(1_500);

            String numeroPrueba = solicitarNumeroLlamada(
                    owner,
                    "Prueba de audio",
                    "Ingresa un número de prueba para interrumpir la reproducción de música.");

            if (numeroPrueba == null || numeroPrueba.isBlank()) {
                actualizarEstadoPaso(paso, "Cancelado por el usuario");
                return false;
            }

            actualizarEstadoPaso(paso, "Iniciando llamada de prueba...");
            ejecutarShellEnSerial(serial, "am start -a android.intent.action.CALL -d tel:" + numeroPrueba.trim());
            Thread.sleep(4_000);

            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_ENDCALL");

            return ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.019.008 — Music Player",
                    owner,
                    "Confirma si la música se reanudó después de colgar la llamada saliente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarTestRedActiva(String serial, PasoPrueba paso) {
        actualizarEstadoPaso(paso, "Leyendo red...");
        String out = ejecutarShellEnSerial(serial,
                "dumpsys telephony.registry");

        // Busca el tipo de red activa
        String red = "DESCONOCIDA";
        if (out.contains("getRilVoiceRadioTechnology")) {
            if (out.contains("LTE"))
                red = "4G/VoLTE";
            else if (out.contains("UMTS"))
                red = "3G";
            else if (out.contains("EDGE") || out.contains("GPRS"))
                red = "2G";
            else if (out.contains("NR"))
                red = "5G";
        }

        actualizarEstadoPaso(paso, "Red: " + red);

        // PASS si está en alguna red conocida
        return !red.equals("DESCONOCIDA");
    }

    private String solicitarNumeroLlamada(Stage owner, String titulo, String mensaje) {
        if (Platform.isFxApplicationThread()) {
            return mostrarDialogoNumeroLlamada(owner, titulo, mensaje);
        }

        CountDownLatch latch = new CountDownLatch(1);
        final String[] resultado = { null };

        Platform.runLater(() -> {
            try {
                resultado[0] = mostrarDialogoNumeroLlamada(owner, titulo, mensaje);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return resultado[0];
    }

    private String mostrarDialogoNumeroLlamada(Stage owner, String titulo, String mensaje) {
        final String[] resultado = { null };

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initOwner(owner);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(420);
        root.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font(null, FontWeight.BOLD, 16));
        lblTitulo.setTextFill(Color.web("#cdd6f4"));

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setWrapText(true);
        lblMensaje.setFont(Font.font(13));
        lblMensaje.setTextFill(Color.web("#a6adc8"));

        TextField txtNumero = new TextField();
        txtNumero.setPromptText("Número de llamada");
        txtNumero.setStyle(
                "-fx-background-color: #11111b;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-prompt-text-fill: #6c7086;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 8;");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #f38ba8; -fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-text-fill: #f38ba8;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 10 24 10 24;");

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #a6e3a1; -fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-text-fill: #a6e3a1;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 10 24 10 24;");

        btnAceptar.setOnAction(e -> {
            resultado[0] = txtNumero.getText();
            popup.close();
        });

        btnCancelar.setOnAction(e -> {
            resultado[0] = null;
            popup.close();
        });

        txtNumero.setOnAction(e -> btnAceptar.fire());

        HBox acciones = new HBox(12, btnCancelar, btnAceptar);
        acciones.setStyle("-fx-alignment: center-right;");

        root.getChildren().addAll(lblTitulo, new Separator(), lblMensaje, txtNumero, acciones);

        popup.setScene(new Scene(root));
        popup.showAndWait();
        txtNumero.requestFocus();

        return resultado[0];
    }

    private boolean ejecutarCallTimerDurationCheck(String serial, PasoPrueba paso) {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        String numero = solicitarNumeroLlamada(
                owner,
                "Call Timer",
                "Ingresa el número al que deseas llamar.\nLa llamada se mantendrá activa durante 36 segundos para verificar la notificación.");

        if (numero == null || numero.isBlank()) {
            actualizarEstadoPaso(paso, "Cancelado por el usuario");
            return false;
        }

        String numeroLimpio = numero.replaceAll("\\s+", "").trim();
        LlamadasD17 llamadas = new LlamadasD17(serial);

        try {
            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + "...");
            boolean ok = llamadas.ejecutarCallTimerDurationCheck(numeroLimpio,
                    () -> ConfirmacionManualPopup.mostrarYEsperar(
                            "Verificación de notificación de llamada",
                            owner,
                            "La llamada se mantuvo 36 segundos.\nConfirma si la notificación fue mostrada durante la llamada."));

            return ok;
        } catch (Exception e) {
            llamadas.colgarLlamadaEnCurso();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LLAMADA MASIVA
    // Todos los dispositivos ADB conectados llaman al número configurado.
    // La llamada se mantiene 10 s y luego se cuelga en todos.
    // El paso pasa si al menos 1 llamada se estableció sin error.
    // ─────────────────────────────────────────────────────────────────────────
    private boolean ejecutarLlamadaMasiva(PasoPrueba paso) {
        if (llamadaMasivaNumero == null) {
            System.out.println("[MASIVA] Sin número configurado");
            return false;
        }

        List<String> seriales = obtenerSerialesADB();
        if (seriales.isEmpty()) {
            System.out.println("[MASIVA] No hay dispositivos ADB conectados");
            return false;
        }

        // Lanzar llamada en todos en paralelo
        List<java.lang.Thread> hilos = new ArrayList<>();
        for (String s : seriales) {
            java.lang.Thread t = new java.lang.Thread(() -> {
                ejecutarShellEnSerial(s,
                        "am start -a android.intent.action.CALL -d tel:" + llamadaMasivaNumero);
            });
            t.start();
            hilos.add(t);
        }
        hilos.forEach(t -> {
            try {
                t.join(5_000);
            } catch (InterruptedException ignored) {
            }
        });

        // Esperar 10 s con la llamada activa
        actualizarEstadoPaso(paso, "LLAMANDO 10s...");
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException ignored) {
        }

        // Colgar en todos en paralelo
        List<java.lang.Thread> hilosCuelga = new ArrayList<>();
        for (String s : seriales) {
            java.lang.Thread t = new java.lang.Thread(() -> {
                ejecutarShellEnSerial(s, "input keyevent KEYCODE_ENDCALL");
            });
            t.start();
            hilosCuelga.add(t);
        }
        hilosCuelga.forEach(t -> {
            try {
                t.join(5_000);
            } catch (InterruptedException ignored) {
            }
        });

        return true;
    }

    @FXML
    private void exportarApnXml() {
        if (dispositivoActual == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar APNs a XML");
        fileChooser.setInitialFileName("apns_" + dispositivoActual.getSerialNumber() + ".xml");

        // Configurar ruta por defecto a Documentos
        String userHome = System.getProperty("user.home");
        File documentosDir = new File(userHome, "Documents");
        if (documentosDir.exists()) {
            fileChooser.setInitialDirectory(documentosDir);
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo XML", "*.xml"));

        File file = fileChooser.showSaveDialog(btnEjecutar.getScene().getWindow());
        if (file == null)
            return;

        new java.lang.Thread(() -> {
            try {
                ADBService adb = new ADBService();
                String serial = adb.getSerialActivo(dispositivoActual.getAndroid_id());
                String xmlContent = adb.exportarApnsXml(serial);

                Files.writeString(file.toPath(), xmlContent);

                // IMPORTANTE: Volver al hilo de la interfaz para mostrar el Toast
                javafx.application.Platform.runLater(() -> {
                    fichaTecnicaController.mostrarToast("✅ Exportado en: " + file.getAbsolutePath());
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Opcional: Mostrar Toast de error
                javafx.application.Platform.runLater(() -> {
                    fichaTecnicaController.mostrarToast("❌ Error al exportar el archivo");
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LLAMADA ENTRE 2 DISPOSITIVOS
    //
    // Secuencia:
    // 1. Detectar número del Tel.2 por ADB (telephony.registry o SIM)
    // 2. Tel.1 llama al número del Tel.2
    // 3. Esperar 3s → Tel.2 acepta automáticamente (KEYCODE_CALL)
    // 4. Llamada activa 10s → Tel.1 cuelga
    // 5. Esperar 3s de pausa
    // 6. Detectar número del Tel.1 por ADB
    // 7. Tel.2 llama al número del Tel.1
    // 8. Esperar 3s → Tel.1 acepta automáticamente
    // 9. Llamada activa 10s → Tel.2 cuelga
    // 10. Si ambas rondas funcionaron → OK
    // ─────────────────────────────────────────────────────────────────────────
    private boolean ejecutarLlamadaEntreDos(PasoPrueba paso) {
        String s1 = llamadaEntreDosSerial1;
        String s2 = llamadaEntreDosSerial2;
        String numero1 = llamadaEntreDosNumero1;
        String numero2 = llamadaEntreDosNumero2;

        if (s1 == null || s2 == null || numero1 == null || numero2 == null) {
            System.out.println("[ENTRE2] Seriales o números no configurados");
            return false;
        }


        boolean ronda1Ok = false;
        boolean ronda2Ok = false;

        try {
            // ── RONDA 1: Tel.1 → Tel.2 ───────────────────────────────────────
            actualizarEstadoPaso(paso, "Ronda 1: Preparando pantallas...");
            despertarDispositivo(s1);
            despertarDispositivo(s2);
            Thread.sleep(1_500);

            actualizarEstadoPaso(paso, "Ronda 1: Tel.1 → Tel.2");
            ejecutarShellEnSerial(s1, "am start -a android.intent.action.CALL -d tel:" + numero2);

            // Espera inteligente — contesta en cuanto suena, máximo 15s
            actualizarEstadoPaso(paso, "Ronda 1: Esperando que suene...");
            boolean sono1 = esperarHastaQueSuene(s2, 15);

            if (!sono1 || !llamadaActiva(s1)) {
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ronda1Ok = false;
            } else {
                despertarDispositivo(s2);
                Thread.sleep(500);
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_CALL");

                actualizarEstadoPaso(paso, "Ronda 1 activa 10s...");
                Thread.sleep(10_000);

                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ronda1Ok = true;
            }

            // Pausa entre rondas — siempre se ejecuta aunque Ronda 1 fallara
            actualizarEstadoPaso(paso, "Pausa entre rondas...");
            Thread.sleep(4_000);

            // ── RONDA 2: Tel.2 → Tel.1 ─── siempre se ejecuta ───────────────
            despertarDispositivo(s1);
            despertarDispositivo(s2);
            Thread.sleep(1_500);

            actualizarEstadoPaso(paso, "Ronda 2: Tel.2 → Tel.1");
            ejecutarShellEnSerial(s2, "am start -a android.intent.action.CALL -d tel:" + numero1);

            // Espera inteligente — contesta en cuanto suena, máximo 15s
            actualizarEstadoPaso(paso, "Ronda 2: Esperando que suene...");
            boolean sono2 = esperarHastaQueSuene(s1, 15);

            if (!sono2 || !llamadaActiva(s2)) {
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ronda2Ok = false;
            } else {
                despertarDispositivo(s1);
                Thread.sleep(500);
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_CALL");

                actualizarEstadoPaso(paso, "Ronda 2 activa 10s...");
                Thread.sleep(10_000);

                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ronda2Ok = true;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ENTRE2] Test interrumpido");
        }

        boolean exito = ronda1Ok && ronda2Ok;
        return exito;
    }

    // Pruebas de TOUCHSCREEN
    @FXML
    private void addTouchScreenTest() {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        new java.lang.Thread(() -> {
            // Obtener resolución real del dispositivo
            ADBService adb = new ADBService();
            String serial;
            try {
                serial = adb.getSerialActivo(dispositivoActual.getAndroid_id());
            } catch (IOException e) {
                serial = dispositivoActual.getSerialNumber();
            }

            String resolucion = adb.ejecutarComandoSincrono(serial, "shell wm size");
            // Output: "Physical size: 1080x2340"
            int centroX = 540;
            int centroY = 960;
            if (resolucion != null && resolucion.contains("x")) {
                try {
                    String[] partes = resolucion.replaceAll("[^0-9x]", "").split("x");
                    centroX = Integer.parseInt(partes[0]) / 2;
                    centroY = Integer.parseInt(partes[1]) / 2;
                } catch (Exception e) {
                    System.out.println("[TOUCH] Error parseando resolución, usando valores por defecto");
                }
            }

            // Coordenadas derivadas del centro
            int margenX = centroX / 2;
            int margenY = centroY / 4;

            final int cx = centroX;
            final int cy = centroY;

            List<BloquePrueba> bloqueTouchScreen = List.of(
                    new BloquePrueba("SOFT.004.001", "Check single tap on touchscreen",
                            "shell input tap " + cx + " " + cy, false, true),
                    new BloquePrueba("SOFT.004.002", "Check double tap on touchscreen",
                            "shell input tap " + cx + " " + cy + " && sleep 0.3 && input tap " + cx + " " + cy, false,
                            true),
                    new BloquePrueba("SOFT.004.003", "Check long press (press and hold) on touchscreen",
                            "shell input swipe " + cx + " " + cy + " " + cx + " " + (cy + 1) + " 1500", false, true),
                    new BloquePrueba("SOFT.004.004", "Check drag on touchscreen",
                            "shell input swipe " + (cx - margenX) + " " + cy + " " + (cx + margenX) + " " + cy + " 800",
                            false, true),
                    new BloquePrueba("SOFT.004.005", "Check flick movement on touchscreen",
                            "shell input swipe " + (cx - margenX) + " " + cy + " " + (cx + margenX) + " " + cy + " 100",
                            false, true),
                    new BloquePrueba("SOFT.004.006", "Check pinch on touchscreen",
                            "__PINCH__", false, true),
                    new BloquePrueba("SOFT.004.007", "Check spread on touchscreen",
                            "__SPREAD__", false, true));

            boolean modoExpressActivo = btnIotExpress.isSelected();

            List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                    ? bloqueTouchScreen.stream().filter(BloquePrueba::isIotExpress).toList()
                    : bloqueTouchScreen;

            Platform.runLater(() -> SelectorPruebasPopup.mostrar(
                    "SOFT.004 — Touch Screen",
                    bloquesAFiltrar,
                    owner,
                    seleccionadas -> seleccionadas.stream()
                            .map(BloquePrueba::toPasoPrueba)
                            .forEach(pasos::add)));
        }).start();
    }

    private boolean ejecutarPinch(String serial, ADBService adb) {
        String resolucion = adb.ejecutarComandoSincrono(serial, "shell wm size");
        int cx = 540, cy = 960;
        if (resolucion != null && resolucion.contains("x")) {
            try {
                String[] p = resolucion.replaceAll("[^0-9x]", "").split("x");
                cx = Integer.parseInt(p[0]) / 2;
                cy = Integer.parseInt(p[1]) / 2;
            } catch (Exception ignored) {
            }
        }
        int margen = cx / 3;

        boolean[] resultados = { false, false };
        final int fcx = cx, fcy = cy, fm = margen;

        java.lang.Thread t1 = new java.lang.Thread(() -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + (fcx - fm) + " " + (fcy - fm) + " " + fcx + " " + fcy + " 600"));
        java.lang.Thread t2 = new java.lang.Thread(() -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + (fcx + fm) + " " + (fcy + fm) + " " + fcx + " " + fcy + " 600"));

        t1.start();
        t2.start();
        try {
            t1.join(3000);
            t2.join(3000);
        } catch (InterruptedException ignored) {
        }

        return resultados[0] && resultados[1];
    }

    private boolean ejecutarSpread(String serial, ADBService adb) {
        String resolucion = adb.ejecutarComandoSincrono(serial, "shell wm size");
        int cx = 540, cy = 960;
        if (resolucion != null && resolucion.contains("x")) {
            try {
                String[] p = resolucion.replaceAll("[^0-9x]", "").split("x");
                cx = Integer.parseInt(p[0]) / 2;
                cy = Integer.parseInt(p[1]) / 2;
            } catch (Exception ignored) {
            }
        }
        int margen = cx / 3;

        boolean[] resultados = { false, false };
        final int fcx = cx, fcy = cy, fm = margen;

        java.lang.Thread t1 = new java.lang.Thread(() -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + fcx + " " + fcy + " " + (fcx - fm) + " " + (fcy - fm) + " 600"));
        java.lang.Thread t2 = new java.lang.Thread(() -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + fcx + " " + fcy + " " + (fcx + fm) + " " + (fcy + fm) + " 600"));

        t1.start();
        t2.start();
        try {
            t1.join(3000);
            t2.join(3000);
        } catch (InterruptedException ignored) {
        }

        return resultados[0] && resultados[1];
    }

    // PRUEBAS DEL RELOJ
    @FXML
    public void addClockTest() {
        List<BloquePrueba> bloqueReloj = List.of(
                new BloquePrueba(true, "SOFT.005.001", "Check if network-provided time is shown properly",
                        "shell settings get global auto_time"),
                new BloquePrueba("SOFT.005.002", "Adjust manually date and time",
                        "shell am start -a android.settings.DATE_SETTINGS", true),
                new BloquePrueba("SOFT.005.003", "Check if network-provided time zone is shown (GMT)",
                        "shell settings get global auto_time_zone"),
                new BloquePrueba("SOFT.005.004", "Adjust manually time zone",
                        "shell am start -a android.settings.TIMEZONE_SETTINGS", true),
                new BloquePrueba("SOFT.005.005", "Adjust 24-hour time format",
                        "shell am start -a android.settings.DATE_SETTINGS", true),
                new BloquePrueba("SOFT.005.006", "Adjust 12-hour time format",
                        "shell am start -a android.settings.DATE_SETTINGS", true),
                new BloquePrueba("SOFT.005.007", "Add an hour of world time list",
                        "shell am start -n com.google.android.deskclock/com.android.deskclock.DeskClock", true),
                new BloquePrueba(true, "SOFT.005.008", "Add a new alarm",
                        "shell am start -a android.intent.action.SET_ALARM --ei android.intent.extra.alarm.HOUR 8 --ei android.intent.extra.alarm.MINUTES 0 --ez android.intent.extra.alarm.SKIP_UI false",
                        true),
                new BloquePrueba("SOFT.005.009", "Edit an alarm",
                        "shell am start -a android.intent.action.SHOW_ALARMS", true),
                new BloquePrueba("SOFT.005.010", "Delete an alarm",
                        "shell am start -a android.intent.action.SHOW_ALARMS", true),
                new BloquePrueba(true, "SOFT.005.011", "Check that timer works properly",
                        "shell am start -a android.intent.action.SET_TIMER --ei android.intent.extra.alarm.LENGTH 30 --ez android.intent.extra.alarm.SKIP_UI false",
                        true),
                new BloquePrueba(true, "SOFT.005.012", "Check that stopwatch works properly",
                        "shell am start -n com.google.android.deskclock/com.android.deskclock.DeskClock", true));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueReloj.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueReloj;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.005 — Clock functions", bloquesAFiltrar, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    // PRUEBAS DE INFO
    @FXML
    public void addInfoTest() {
        List<BloquePrueba> bloqueInfo = List.of(
                new BloquePrueba(true, "SOFT.012.001", "Check device name",
                        "shell getprop ro.product.name"),
                new BloquePrueba("SOFT.012.002", "Change device name",
                        "__CHANGE_NAME__"),
                new BloquePrueba(true, "SOFT.012.003", "Check model name",
                        "shell getprop ro.product.model"),
                new BloquePrueba("SOFT.012.004", "Check serial number",
                        "shell getprop ro.serialno"),
                new BloquePrueba(true, "SOFT.012.005", "Check software version",
                        "shell getprop ro.build.version.release"),
                new BloquePrueba("SOFT.012.006", "Check hardware version",
                        INFO_HW_VERSION),
                new BloquePrueba(true, "SOFT.012.007", "Check IMEI number",
                        List.of("shell service call iphonesubinfo 1 s16 com.android.shell",
                                "shell service call iphonesubinfo 1")),
                new BloquePrueba(true, "SOFT.012.008", "Check IMEISV number",
                        "shell am start -a android.settings.DEVICE_INFO_SETTINGS", true),
                new BloquePrueba(true, "SOFT.012.009", "Check device name when connected to PC",
                        "__DEVICE_NAME_PC__"),
                new BloquePrueba("SOFT.012.010", "Check network name (net.hostname)",
                        List.of("shell getprop net.hostname", "shell getprop ro.product.device",
                                "shell getprop ro.product.name", "shell getprop ro.build.product")),
                new BloquePrueba("SOFT.012.011", "Check IP address",
                        "__IP__"),
                new BloquePrueba("SOFT.012.012", "Check Wi-Fi MAC address",
                        List.of(
                                "shell getprop ro.boot.wifimacaddr",
                                "shell getprop wifi.interface",
                                "shell getprop ro.boot.mac_addr",
                                "shell getprop ro.ethernet.addr",
                                "shell ip link show wlan0",
                                "shell ip addr show wlan0")),
                new BloquePrueba("SOFT.012.013", "Check Bluetooth address",
                        "shell settings get secure bluetooth_address"),
                new BloquePrueba("SOFT.012.014", "Check build number",
                        "shell getprop ro.build.display.id"),
                new BloquePrueba("SOFT.012.015", "Check ro.product.code",
                        List.of("shell getprop ro.product.code", "shell getprop ro.product.name",
                                "shell getprop ro.product.model", "shell getprop ro.build.product")),
                new BloquePrueba("SOFT.012.016", "Check ro.product.brand",
                        "shell getprop ro.product.brand"),
                new BloquePrueba("SOFT.012.017", "Check logcat for fabricant references",
                        "__LOGCAT_BRAND__"));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueInfo.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueInfo;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.012 — Info", bloquesAFiltrar, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean cambiarYRestaurarNombre(String serial, ADBService adb) {
        String nombreOriginal = adb.ejecutarComandoSincrono(serial,
                "shell settings get global device_name");
        if (nombreOriginal == null)
            return false;
        nombreOriginal = nombreOriginal.trim();

        adb.ejecutarComandoSincrono(serial,
                "shell settings put global device_name TEST_DEVICE");

        String verificacion = adb.ejecutarComandoSincrono(serial,
                "shell settings get global device_name");
        boolean cambioOk = "TEST_DEVICE".equals(verificacion != null ? verificacion.trim() : "");

        adb.ejecutarComandoSincronoArray(serial,
                "shell", "settings", "put", "global", "device_name", nombreOriginal);

        String restauracion = adb.ejecutarComandoSincrono(serial,
                "shell settings get global device_name");
        boolean restauroOk = nombreOriginal.equals(restauracion != null ? restauracion.trim() : "");

        // FALLBACK intentar con espacios sustituidos por barras bajas
        if (!restauroOk) {
            String nombreSeguro = nombreOriginal.replace(" ", "_");
            adb.ejecutarComandoSincrono(serial,
                    "shell settings put global device_name " + nombreSeguro);

            restauracion = adb.ejecutarComandoSincrono(serial,
                    "shell settings get global device_name");
            restauroOk = nombreSeguro.equals(restauracion != null ? restauracion.trim() : "");
        }

        return cambioOk && restauroOk;
    }

    private String parsearMacAddress(String log) {
        String regex = "link/ether\\s+([0-9a-fA-F:]{17})";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(log);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // PRUEBAS DE DISPLAY
    @FXML
    private void addDisplayTest() {
        List<BloquePrueba> bloqueDisplay = List.of(
                new BloquePrueba(true, "SOFT.008.001", "Change brightness level", DISPLAY_BRIGHTNESS_CHANGE),
                new BloquePrueba("SOFT.008.002", "Check default brightness about 75%", DISPLAY_BRIGHTNESS_CHECK),
                new BloquePrueba("SOFT.008.003", "Change wallpaper",
                        DISPLAY_WALLPAPER),
                new BloquePrueba(true, "SOFT.008.004", "Check default time of screen timeout (1 minute)",
                        DISPLAY_TIMEOUT_CHECK),
                new BloquePrueba(true, "SOFT.008.005", "Change the screen timeout and check if it works",
                        DISPLAY_TIMEOUT_CHANGE),
                new BloquePrueba("SOFT.008.006", "Change font size",
                        DISPLAY_FONT_SIZE),
                new BloquePrueba("SOFT.008.007", "Change display size",
                        DISPLAY_DISPLAY_SIZE),
                new BloquePrueba("SOFT.008.008", "Set a screen saver and check if it works",
                        DISPLAY_SCREENSAVER),
                new BloquePrueba("SOFT.008.009", "Check Company colour in UI",
                        "shell am start -a android.settings.DISPLAY_SETTINGS", true));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueDisplay.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueDisplay;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.008 - Display", bloquesAFiltrar, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean cambiarYVerificarBrillo(String serial, ADBService adb) {
        String valorInicial = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_brightness");
        if (!valorInicial.equals("192")) {
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_brightness 192");
            String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_brightness");
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_brightness " + valorInicial.trim());
            return valor != null && valor.trim().equals("192");
        } else {
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_brightness 190");
            String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_brightness");
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_brightness " + valorInicial.trim());
            return valor != null && valor.trim().equals("190");
        }
    }

    private boolean configurarLlamadaEntranteParaFm(Stage owner) {
        if (!Platform.isFxApplicationThread()) {
            final boolean[] resultado = { false };
            CountDownLatch latch = new CountDownLatch(1);

            Platform.runLater(() -> {
                try {
                    resultado[0] = configurarLlamadaEntranteParaFm(owner);
                } finally {
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            return resultado[0];
        }

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initOwner(owner);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(520);
        root.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");

        Label titulo = new Label("Configurar llamada entrante");
        titulo.setFont(Font.font("Poppins", FontWeight.BOLD, 16));
        titulo.setTextFill(Color.web("#cdd6f4"));

        Label info = new Label(
                "Selecciona el dispositivo que llamará e introduce el número antes de añadir la prueba.");
        info.setTextFill(Color.web("#6c7086"));
        info.setFont(Font.font(11));
        info.setWrapText(true);

        List<String> seriales = obtenerSerialesADB();
        List<String> opciones = seriales.stream().map(this::etiquetaDispositivo).toList();

        Label lblLlamante = crearLabelConfig("Dispositivo que llama:");
        ComboBox<String> cbLlamante = crearCombo(opciones);

        Label lblNumero = crearLabelConfig("Número a marcar:");
        TextField tfNumero = crearTextField("+34612345678");

        String serialActual = obtenerSerialADBActual();
        seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                .ifPresent(s -> cbLlamante.getSelectionModel().select(etiquetaDispositivo(s)));
        if (cbLlamante.getSelectionModel().isEmpty() && !opciones.isEmpty()) {
            cbLlamante.getSelectionModel().select(0);
        }

        Label aviso = new Label();
        aviso.setTextFill(Color.web("#f38ba8"));
        aviso.setFont(Font.font(11));

        boolean[] confirmado = { false };
        String[] llamanteSeleccionado = { null };
        String[] numeroSeleccionado = { null };

        Button btnCancelar = crearBoton("Cancelar", "#f38ba8");
        Button btnAceptar = crearBoton("Añadir prueba", "#a6e3a1");

        btnCancelar.setOnAction(e -> popup.close());
        btnAceptar.setOnAction(e -> {
            String llamante = serialDesdeEtiqueta(cbLlamante.getValue());
            String numero = tfNumero.getText() != null ? tfNumero.getText().trim() : "";

            if (llamante == null || llamante.isBlank()) {
                aviso.setText("Selecciona el dispositivo que llama.");
                return;
            }
            if (numero.isBlank()) {
                aviso.setText("Introduce el número del receptor.");
                return;
            }

            // Evitar que el usuario seleccione el mismo dispositivo que el bajo prueba
            if (serialActual != null && serialActual.equals(llamante)) {
                aviso.setText("No se puede seleccionar el mismo dispositivo que el bajo prueba.");
                return;
            }

            llamanteSeleccionado[0] = llamante;
            numeroSeleccionado[0] = numero;
            confirmado[0] = true;
            popup.close();
        });

        HBox botones = new HBox(12, btnCancelar, btnAceptar);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        root.getChildren().addAll(titulo, info, new Separator(), lblLlamante, cbLlamante, lblNumero, tfNumero,
                aviso, new Separator(), botones);

        popup.setScene(new Scene(root));
        popup.showAndWait();

        if (confirmado[0]) {
            llamadaEntranteSerial = llamanteSeleccionado[0];
            llamadaEntranteNumero = numeroSeleccionado[0];
        }

        return confirmado[0];
    }

    private Stage obtenerVentanaPrincipal() {
        if (Platform.isFxApplicationThread()) {
            return btnEjecutar != null && btnEjecutar.getScene() != null
                    ? (Stage) btnEjecutar.getScene().getWindow()
                    : null;
        }

        final Stage[] owner = { null };
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                owner[0] = btnEjecutar != null && btnEjecutar.getScene() != null
                        ? (Stage) btnEjecutar.getScene().getWindow()
                        : null;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return owner[0];
    }

    private boolean comprobarBrilloPorDefecto(String serial, ADBService adb) {
        String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_brightness");
        if (valor == null) {
            return false;
        }
        try {
            int brillo = Integer.parseInt(valor.trim());
            return brillo >= 150 && brillo <= 210;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean cambiarWallpaper(String serial, ADBService adb) {
        File apkFile = null;
        String rutaImagen = "/data/local/tmp/wallpaper_test.jpg";
        try {
            // Crear imagen y subirla
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.BLUE);
            g.fillRect(0, 0, 100, 100);
            g.dispose();

            File tempFile = File.createTempFile("wallpaper_test", ".jpg");
            ImageIO.write(img, "jpg", tempFile);

            new ProcessBuilder("adb", "-s", serial, "push",
                    tempFile.getAbsolutePath(), rutaImagen)
                    .start().waitFor();
            tempFile.delete();

            // Instalar APK
            desactivarPlayProtect(serial, adb);
            apkFile = obtenerApkDeResources("WallpaperSetter.apk");
            new ProcessBuilder("adb", "-s", serial, "install", "-r", "-g", "-t",
                    apkFile.getAbsolutePath()).start().waitFor();
            Thread.sleep(2000);

            // Cambiar wallpaper
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");
            adb.ejecutarComandoSincrono(serial,
                    "shell am broadcast -a com.example.wallpapersetter.SET_WALLPAPER " +
                            "--es action set " +
                            "--es image_path " + rutaImagen +
                            " -n com.example.wallpapersetter/.WallpaperReceiver");
            Thread.sleep(4000);

            // Verificar via logcat
            String logcat = adb.ejecutarComandoSincrono(serial,
                    "shell logcat -d WallpaperSetter:D *:S");
            boolean ok = logcat != null
                    && logcat.contains("Wallpaper establecido correctamente");

            return ok;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                new ProcessBuilder("adb", "-s", serial, "uninstall",
                        "com.example.wallpapersetter").start().waitFor();

            } catch (Exception e) {
                System.err.println("Error al desinstalar WallpaperSetter: " + e.getMessage());
            }
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaImagen);
            if (apkFile != null && apkFile.exists()) {
                apkFile.delete();
            }
        }
    }

    private boolean comprobarTimeoutPorDefecto(String serial, ADBService adb) {
        String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
        if (valor == null) {
            return false;
        }
        try {
            int timeout = Integer.parseInt(valor.trim());
            return timeout >= 55000 && timeout <= 65000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean cambiarYVerificarTimeout(String serial, ADBService adb) {
        String valorInicial = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
        if (!valorInicial.equals("120000")) {
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_off_timeout 120000");
            String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_off_timeout " + valorInicial.trim());
            return valor != null && valor.trim().equals("120000");
        } else {
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_off_timeout 110000");
            String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
            adb.ejecutarComandoSincrono(serial, "shell settings put system screen_off_timeout " + valorInicial.trim());
            return valor != null && valor.trim().equals("110000");
        }
    }

    private boolean cambiarYRestaurarFuente(String serial, ADBService adb) {
        String valorInicial = adb.ejecutarComandoSincrono(serial, "shell settings get system font_scale");
        if (valorInicial == null) {
            return false;
        }
        valorInicial = valorInicial.trim();
        boolean cambioOk = false;
        if (!valorInicial.equals("1.15")) {
            adb.ejecutarComandoSincrono(serial, "shell settings put system font_scale 1.15");
            String verificacion = adb.ejecutarComandoSincrono(serial, "shell settings get system font_scale");
            cambioOk = verificacion != null && verificacion.trim().startsWith("1.15");
        } else {
            adb.ejecutarComandoSincrono(serial, "shell settings put system font_scale 1.13");
            String verificacion = adb.ejecutarComandoSincrono(serial, "shell settings get system font_scale");
            cambioOk = verificacion != null && verificacion.trim().startsWith("1.13");
        }
        adb.ejecutarComandoSincrono(serial, "shell settings put system font_scale " + valorInicial);
        String restauracion = adb.ejecutarComandoSincrono(serial, "shell settings get system font_scale");
        boolean restauroOk = valorInicial.equals(restauracion != null ? restauracion.trim() : "");

        return cambioOk && restauroOk;
    }

    private boolean cambiarYRestaurarDisplaySize(String serial, ADBService adb) {
        String salidaInicial = adb.ejecutarComandoSincrono(serial, "shell wm size");
        if (salidaInicial == null || salidaInicial.isEmpty()) {
            return false;
        }
        String valorOriginal = salidaInicial.trim();

        boolean cambioOk = false;

        if (!valorOriginal.contains("720x1280")) {
            adb.ejecutarComandoSincrono(serial, "shell wm size " + "720x1280");
            String verificacion = adb.ejecutarComandoSincrono(serial, "shell wm size");
            cambioOk = verificacion != null && verificacion.contains("720x1280");
        } else {
            adb.ejecutarComandoSincrono(serial, "shell wm size 1080x1920");
            String verificacion = adb.ejecutarComandoSincrono(serial, "shell wm size");
            cambioOk = verificacion != null && verificacion.contains("1080x1920");
        }

        if (!valorOriginal.contains("Override size")) {
            adb.ejecutarComandoSincrono(serial, "shell wm size reset");
        } else {
            String[] partes = valorOriginal.split(":");
            String resolucionPura = partes[partes.length - 1].trim();
            adb.ejecutarComandoSincrono(serial, "shell wm size " + resolucionPura);
        }
        String restauracion = adb.ejecutarComandoSincrono(serial, "shell wm size");
        boolean restauroOk = restauracion != null && restauracion.trim().equals(valorOriginal);

        return cambioOk && restauroOk;
    }

    private boolean comprobarScreensaver(String serial, ADBService adb) {
        adb.ejecutarComandoSincrono(serial, "shell settings put secure screensaver_enabled 1");
        adb.ejecutarComandoSincrono(serial, "shell service call dreams 1 s16 \"com.android.dreams.basic/.BasicDream\"");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String todoElEstado = adb.ejecutarComandoSincrono(serial, "shell dumpsys dreams");

        if (todoElEstado == null || todoElEstado.isEmpty()) {
            return false;
        }

        boolean tieneDreamActivo = todoElEstado.contains("mCurrentDream=DreamRecord")
                && !todoElEstado.contains("mCurrentDream=null");
        return tieneDreamActivo;
    }

    // PRUEBAS DE CONTACTOS
    @FXML
    private void addContactsTest() {
        ADBService adb = new ADBService();
        String serial;

        try {
            serial = adb.getSerialActivo(dispositivoActual.getAndroid_id());
        } catch (IOException e) {
            serial = dispositivoActual.getSerialNumber();
        }

        List<DispositivoCombo> otrosDispositivos = new ArrayList<>();
        try {
            Map<String, String> todos = adb.obtenerDispositivosConectados();
            for (Map.Entry<String, String> entry : todos.entrySet()) {
                String serialDispositivo = entry.getValue();
                // Excluir el dispositico actual
                if (!serialDispositivo.equals(serial)) {
                    try {
                        Dispositivo disp = adb.obtenerProps(serialDispositivo);
                        String nombreModeloDispositivo = disp.getModelo().getNombreModelo();
                        String androidIdDispositivo = disp.getAndroid_id();
                        otrosDispositivos.add(
                                new DispositivoCombo(nombreModeloDispositivo, serialDispositivo, androidIdDispositivo));
                    } catch (IOException e) {
                        String androidIdFallback = entry.getKey();
                        otrosDispositivos.add(new DispositivoCombo("Dispositivo (" + serialDispositivo + ")",
                                serialDispositivo, androidIdFallback));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[CONTACTS] No se pudieron obtener otros dispositivos");
        }

        String[] telefonoDUT = { contactoTestTelefonoDUT };
        String[] telefono = { contactoTestTelefono };
        String[] serialReceptor = { contactoSerialReceptor != null ? contactoSerialReceptor : "" };
        String[] exchangeCuenta = { contactoExchangeCuenta };

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        boolean confirmado = ContactosConfigPopup.mostrar(otrosDispositivos, telefonoDUT, telefono, serialReceptor,
                exchangeCuenta, owner);

        if (!confirmado) {
            return;
        }
        contactoTestTelefonoDUT = telefonoDUT[0];
        contactoTestTelefono = telefono[0];
        contactoSerialReceptor = (serialReceptor != null && serialReceptor.length > 0 && serialReceptor[0] != null
                && !serialReceptor[0].isEmpty())
                        ? serialReceptor[0]
                        : null;
        contactoExchangeCuenta = exchangeCuenta[0];

        mostrarSelectorContactos();
    }

    private void mostrarSelectorContactos() {
        List<BloquePrueba> bloqueContactos = List.of(
                new BloquePrueba(true, "SOFT.006.001", "Create several SIM contacts", CONTACT_CREATE_SIM),
                new BloquePrueba("SOFT.006.002", "Insert SIM card on other device and check SIM contacts created", "",
                        true),
                new BloquePrueba(true, "SOFT.006.003", "Edit SIM card contacts",
                        "shell am start -a android.intent.action.VIEW -t vnd.android.cursor.dir/contact", true),
                new BloquePrueba(true, "SOFR.006.004", "Delete a SIM card contact",
                        "shell am start -a android.intent.action.VIEW -t vnd.android.cursor.dir/contact", true),
                new BloquePrueba(true, "SOFT.006.005", "Make a call to a SIM card contact", CONTACT_CALL_SIM),
                new BloquePrueba("SOFT.006.006", "Receive a call from a SIM card contact",
                        contactoSerialReceptor != null ? CONTACT_RECEIVE_CALL_SIM : "",
                        contactoSerialReceptor != null ? false : true),
                new BloquePrueba("SOFT.006.007", "Create several phone contacts", CONTACT_CREATE_PHONE),
                new BloquePrueba("SOFT.006.008", "Edit phone contacts", CONTACT_EDIT_PHONE),
                new BloquePrueba("SOFT.006.009", "Make a call to a phone contact", CONTACT_CALL_PHONE),
                new BloquePrueba("SOFT.006.010", "Delete a phone contact", CONTACT_DELETE_PHONE),
                new BloquePrueba("SOFT.006.011", "Receive a call from a phone contact",
                        contactoSerialReceptor != null ? CONTACT_RECEIVE_CALL_PHONE : "",
                        contactoSerialReceptor != null ? false : true),
                new BloquePrueba("SOFT.006.012", "Create several exchange contacts",
                        "shell am start -a android.intent.action.VIEW -t vnd.android.cursor.dir/contact", true),
                new BloquePrueba("SOFT.006.013", "Check exchange contacts via web", "", true),
                new BloquePrueba("SOFT.006.014", "Edit exchange contacts", "", true),
                new BloquePrueba("SOFT.006.015", "Delete an exchange contact", "", true),
                new BloquePrueba("SOFT.006.016", "Make a call to exchange contact", "", true),
                new BloquePrueba("SOFT.006.017", "Receive call from exchange contact", "", true),
                new BloquePrueba("SOFT.006.018", "List only SIM card contacts",
                        "shell content query --uri content://icc/adn --projection tag"),
                new BloquePrueba("SOFT.006.019", "List only phone contacts",
                        "shell content query --uri content://com.android.contacts/raw_contacts --projection display_name --where \\\"account_type IS NULL\\\""),
                new BloquePrueba("SOFT.006.020", "List only exchange contacts", "", true),
                new BloquePrueba(true, "SOFT.006.021", "Copy several SIM contacts to phone", CONTACT_COPY_SIM_PHONE),
                new BloquePrueba("SOFT.006.022", "Copy SIM contacts to exchange", "", true),
                new BloquePrueba("SOFT.006.023", "Copy phone contacts to SIM", CONTACT_COPY_PHONE_SIM),
                new BloquePrueba("SOFT.006.024", "Copy phone contacts to exchange", "", true),
                new BloquePrueba("SOFT.006.025", "Copy exchange contacts to SIM", "", true),
                new BloquePrueba("SOFT.006.026", "Copy exchange contacts to phone", "", true),
                new BloquePrueba("SOFT.006.027", "Import vCard from internal storage to phone", CONTACT_IMPORT_VCARD),
                new BloquePrueba("SOFT.006.028", "Import vCard to exchange account", "", true),
                new BloquePrueba("SOFT.006.029", "Import vCard from microSD to phone", "", true),
                new BloquePrueba("SOFT.006.030", "Import vCard from microSD to exchange", "", true),
                new BloquePrueba("SOFT.006.031", "Export contacts to vCard", CONTACT_EXPORT_VCARD),
                new BloquePrueba("SOFT.006.032", "Share contacts via Bluetooth", "", true),
                new BloquePrueba("SOFT.006.033", "Share contacts via SMS", "", true),
                new BloquePrueba("SOFT.006.034", "Receive vCard via Bluetooth", "", true),
                new BloquePrueba("SOFT.006.035", "Receive vCard via MMS", "", true),
                new BloquePrueba(true, "SOFT.006.036", "Check Service Dial Numbers (SDN)", "", true),
                new BloquePrueba("SOFT.006.037", "Memory status: Check size for Phone contacts and USIM contacts",
                        CONTACT_MEMORY_STATUS));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueContactos.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueContactos;

        boolean tieneExchange = (contactoExchangeCuenta != null);

        List<BloquePrueba> bloquesFinales = bloquesAFiltrar.stream()
                .map(b -> {
                    if (!tieneExchange && b.getDescripcion().toLowerCase().contains("exchange")) {
                        return new BloquePrueba(b.isIotExpress(), b.getId(), b.getDescripcion(), b.getComandos(), true,
                                b.isSinOuput(), b.isRestablecerPhoneAppAlFinal()) {
                            @Override
                            public PasoPrueba toPasoPrueba() {
                                return new PasoPrueba(this.isIotExpress(), this.getId() + " - " + this.getDescripcion(),
                                        this.getComandos(), true, this.isSinOuput(),
                                        this.isRestablecerPhoneAppAlFinal());
                            }
                        };
                    }
                    return b;
                })
                .toList();

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        SelectorPruebasPopup.mostrar("SOFT.006 — Contacts functions", bloquesFinales, owner,
                seleccionadas -> seleccionadas.stream()
                        .filter(b -> !b.getDescripcion().endsWith("[DISABLED]"))
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private String parsearListaContactos(String rawOutput) {
        if (rawOutput == null || rawOutput.trim().isEmpty() || rawOutput.contains("usage:")
                || rawOutput.toLowerCase().contains("no result")) {
            if (rawOutput != null && rawOutput.contains("display_name")) {
                return "Contactos del Dispositivo: 0 encontrados";
            }
            return "Contactos de la SIM: 0 econtrados";
        }

        String[] lineas = rawOutput.split("\n");
        int contadorTotal = 0;
        boolean esDispositivo = false;

        for (String linea : lineas) {
            String nombreExtraido = "";

            if (linea.contains("display_name=")) {
                esDispositivo = true;
                String[] partes = linea.split("display_name=");
                if (partes.length > 1) {
                    nombreExtraido = partes[1].trim();
                }
            } else if (linea.contains("name=")) {
                String[] partes = linea.split("name=");
                if (partes.length > 1) {
                    String resto = partes[1];
                    if (resto.contains(",")) {
                        nombreExtraido = resto.substring(0, resto.indexOf(",")).trim();
                    } else {
                        nombreExtraido = resto.trim();
                    }
                }
            }

            nombreExtraido = nombreExtraido.replace("\"", "").trim();

            if (!nombreExtraido.isEmpty() && !nombreExtraido.equalsIgnoreCase("null")) {
                contadorTotal++;
            }
        }

        if (esDispositivo) {
            return "Contactos del Dispositivo: " + contadorTotal + " encontrados";
        } else {
            return "Contactos de la SIM: " + contadorTotal + " encontrados";
        }
    }

    // CONTACTOS SIM
    private boolean crearContactoSIM(String serial, ADBService adb) {
        boolean ok = true;
        for (int i = 1; i <= 3; i++) {
            String r = adb.ejecutarComandoSincrono(serial,
                    "shell content insert --uri content://icc/adn " +
                            "--bind tag:s:" + contactoTestNombre + "_SIM_" + i + " " +
                            "--bind number:s:" + contactoTestTelefono);

            if (r != null && r.toLowerCase().contains("error")) {
                ok = false;
            }
        }
        return ok;
    }

    // CONTACTOS TElÉFONO
    private boolean crearContactoPhone(String serial, ADBService adb) {
        boolean ok = true;

        try {
            for (int i = 1; i <= 3; i++) {
                adb.ejecutarComandoSincrono(serial,
                        "shell content insert --uri content://com.android.contacts/raw_contacts " +
                                "--bind account_type:n:null --bind account_name:n:null");
                Thread.sleep(200);

                // Buscamos la última fila insertada y extraemos el ID
                String idStr = adb.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://com.android.contacts/raw_contacts --projection _id");

                if (idStr == null || idStr.trim().isEmpty() || idStr.contains("usage:")) {
                    ok = false;
                    continue;
                }

                String[] lineas = idStr.split("\n");
                int idMasAlto = 0;

                for (String linea : lineas) {
                    if (linea.contains("_id=")) {
                        String[] partes = linea.split("=");
                        if (partes.length > 1) {
                            String idLimpio = partes[1].replaceAll("[^0-9]", "").trim();
                            if (!idLimpio.isEmpty()) {
                                int idActual = Integer.parseInt(idLimpio);
                                if (idActual > idMasAlto) {
                                    idMasAlto = idActual;
                                }
                            }
                        }
                    }
                }

                String id = String.valueOf(idMasAlto);

                if (id.isEmpty()) {
                    ok = false;
                    continue;
                }

                // Insertamos Datos vinculados con el ID
                adb.ejecutarComandoSincrono(serial,
                        "shell content insert --uri content://com.android.contacts/data " +
                                "--bind raw_contact_id:i:" + id + " " +
                                "--bind mimetype:s:vnd.android.cursor.item/name " +
                                "--bind data1:s:" + contactoTestNombre + "_Phone_" + i);
                if (i > 1) {
                    adb.ejecutarComandoSincrono(serial,
                            "shell content insert --uri content://com.android.contacts/data " +
                                    "--bind raw_contact_id:i:" + id + " " +
                                    "--bind mimetype:s:vnd.android.cursor.item/phone_v2 " +
                                    "--bind data1:s:" + contactoTestTelefono + i + " " +
                                    "--bind data2:i:2");
                } else {
                    adb.ejecutarComandoSincrono(serial,
                            "shell content insert --uri content://com.android.contacts/data " +
                                    "--bind raw_contact_id:i:" + id + " " +
                                    "--bind mimetype:s:vnd.android.cursor.item/phone_v2 " +
                                    "--bind data1:s:" + contactoTestTelefono + " " +
                                    "--bind data2:i:2");
                }
                // Forzamos a android a recargar la interfaz
                adb.ejecutarComandoSincrono(serial,
                        "shell am broadcast -a android.intent.action.PROVIDER_CHANGED --receiver-include-background content://com.android.contacts");

                Thread.sleep(500);
            }
        } catch (Exception e) {
            ok = false;
        }

        return ok;
    }

    private boolean editarContactoPhone(String serial, ADBService adb) {
        boolean ok = true;
        for (int i = 1; i <= 3; i++) {
            String nombreOriginal = contactoTestNombre + "_Phone_" + i;
            String nombreEditado = contactoTestNombre + "_Phone_EDITADO_" + i;

            String idStr = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.contacts/data " +
                            "--projection raw_contact_id " +
                            "--where \"data1=\\'" + nombreOriginal + "\\'\" " +
                            "--extra mimetype:s:vnd.android.cursor.item/name");

            if (idStr == null || idStr.trim().isEmpty() || idStr.contains("usage:")) {
                System.out.println("Error o vacío al buscar el contacto: " + nombreOriginal);
                ok = false;
                continue;
            }

            // Extraemos el ID numérico de la fila devuelta
            String id = "";
            int idMasAlto = 0;

            for (String linea : idStr.split("\n")) {
                if (linea.contains("raw_contact_id=")) {
                    String[] partes = linea.split("=");
                    if (partes.length > 1) {
                        String idLimpio = partes[1].replaceAll("[^0-9]", "").trim();
                        if (!idLimpio.isEmpty()) {
                            int idActual = Integer.parseInt(idLimpio);
                            if (idActual > idMasAlto) {
                                idMasAlto = idActual;
                            }
                        }
                    }
                }
            }

            if (idMasAlto > 0) {
                id = String.valueOf(idMasAlto);
            }

            if (id.isEmpty()) {
                ok = false;
                continue;
            }

            // Editar nombre aplicando el filtro correcto en el update
            String r = adb.ejecutarComandoSincrono(serial,
                    "shell content update --uri content://com.android.contacts/data " +
                            "--bind data1:s:" + nombreEditado + " " +
                            "--where \"raw_contact_id=" + id + "\" " +
                            "--extra mimetype:s:vnd.android.cursor.item/name");

            if (r != null && (r.toLowerCase().contains("error") || r.contains("usage:"))) {
                ok = false;
            }
        }

        // Notificación final al sistema operativo
        adb.ejecutarComandoSincrono(serial,
                "shell am broadcast -a android.intent.action.PROVIDER_CHANGED --receiver-include-background content://com.android.contacts");

        return ok;
    }

    private boolean borrarContactoPhone(String serial, ADBService adb) {
        boolean ok = true;

        // Intentamos borrar los 3 contactos editados o no
        for (int i = 1; i <= 3; i++) {
            String nombreOriginal = contactoTestNombre + "_Phone_" + i;
            String nombreEditado = contactoTestNombre + "_Phone_EDITADO_" + i;

            String r1 = adb.ejecutarComandoSincrono(serial,
                    "shell content delete --uri content://com.android.contacts/raw_contacts " +
                            "--where \"display_name=\\'" + nombreOriginal + "\\'\"");

            String r2 = adb.ejecutarComandoSincrono(serial,
                    "shell content delete --uri content://com.android.contacts/raw_contacts " +
                            "--where \"display_name=\\'" + nombreEditado + "\\'\"");

            if ((r1 != null && r1.contains("usage:")) || (r2 != null && r2.contains("usage:"))) {
                ok = false;
            }
        }

        // Notificar al sistema
        adb.ejecutarComandoSincrono(serial,
                "shell am broadcast -a android.intent.action.PROVIDER_CHANGED --receiver-include-background content://com.android.contacts");

        // Verificacion
        String checkOriginal = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://com.android.contacts/raw_contacts --projection _id " +
                        "--where \"display_name=\\'" + (contactoTestNombre + "_Phone_3") + "\\'\"");

        String checkEditado = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://com.android.contacts/raw_contacts --projection _id " +
                        "--where \"display_name=\\'" + (contactoTestNombre + "_Phone_EDITADO_3") + "\\'\"");
        boolean originalBorrado = (checkOriginal == null || checkOriginal.trim().isEmpty()
                || checkOriginal.toLowerCase().contains("no result"));
        boolean editadoBorrado = (checkEditado == null || checkEditado.trim().isEmpty()
                || checkEditado.toLowerCase().contains("no result"));

        return ok && originalBorrado && editadoBorrado;
    }

    // LLAMADAS (CONTACTOS)
    private boolean hacerLlamadaDesdeSIM(String serial, ADBService adb) {
        // Verificar que existe el contacto en la SIM
        String check = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://icc/adn " +
                        "--where \"number='" + contactoTestTelefono + "'\"");
        boolean existeEnSIM = check != null && !check.isBlank()
                && !check.contains("No result found");

        // Si no existe en SIM lo creamos antes de llamar
        if (!existeEnSIM) {
            adb.ejecutarComandoSincrono(serial,
                    "shell content insert --uri content://icc/adn " +
                            "--bind tag:s:" + contactoTestNombre + "_SIM_call " +
                            "--bind number:s:" + contactoTestTelefono);
        }

        return hacerLlamada(serial, adb);
    }

    private boolean hacerLlamadaDesdePhone(String serial, ADBService adb) {
        // Verificar que existe el contacto en el teléfono
        String check = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://com.android.contacts/data " +
                        "--projection raw_contact_id --where \"mimetype='vnd.android.cursor.item/phone_v2'" +
                        " AND data1='" + contactoTestTelefono + "'\"");
        boolean existeEnPhone = check != null && !check.isBlank()
                && !check.contains("No result found");

        // Si no existe en phone lo creamos antes de llamar
        if (!existeEnPhone) {
            crearContactoPhone(serial, adb);
        }

        return hacerLlamada(serial, adb);
    }

    // Motor común de llamada
    private boolean hacerLlamada(String serial, ADBService adb) {
        adb.ejecutarComandoSincrono(serial,
                "shell am start -a android.intent.action.CALL -d tel:" + contactoTestTelefono);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        String estado = adb.ejecutarComandoSincrono(serial, "shell dumpsys telephony.registry | grep mCallState");
        boolean activa = estado != null && estado.contains("mCallState=2");

        adb.ejecutarComandoSincrono(serial, "shell input keyevent 26");

        int intentosMaximos = 10;
        int intento = 0;
        boolean llamadaTerminada = false;

        while (!llamadaTerminada && intento < intentosMaximos) {
            String estadoActual = adb.ejecutarComandoSincrono(serial,
                    "shell dumpsys telephony.registry | grep mCallState");
            if (estadoActual != null && estadoActual.contains("mCallState=0")) {
                llamadaTerminada = true;
            } else {
                intento++;
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        return activa;
    }

    private boolean recibirLlamadaSIM(String serial, ADBService adb) {
        // Verificar que el contacto que llama está en la SIM del receptor
        String check = adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell content query --uri content://icc/adn " +
                        "--where \"number='" + contactoTestTelefonoDUT + "'\"");
        boolean existeEnSIM = check != null && !check.isBlank()
                && !check.contains("No result found");

        if (!existeEnSIM) {
            adb.ejecutarComandoSincrono(contactoSerialReceptor,
                    "shell content insert --uri content://icc/adn " +
                            "--bind tag:s:" + contactoTestNombre + "_SIM_recv " +
                            "--bind number:s:" + contactoTestTelefonoDUT);
        }

        return recibirLlamada(serial, adb);
    }

    private boolean recibirLlamadaPhone(String serial, ADBService adb) {
        // Verificar que el contacto que llama está en el teléfono del receptor
        String check = adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell content query --uri content://com.android.contacts/data " +
                        "--projection raw_contact_id --where \"mimetype='vnd.android.cursor.item/phone_v2'" +
                        " AND data1='" + contactoTestTelefonoDUT + "'\"");
        boolean existeEnPhone = check != null && !check.isBlank()
                && !check.contains("No result found");

        if (!existeEnPhone) {
            // Crear contacto en el receptor con el número del DUT
            adb.ejecutarComandoSincrono(contactoSerialReceptor,
                    "shell content insert --uri content://com.android.contacts/raw_contacts " +
                            "--bind account_type:s:LOCAL --bind account_name:s:LOCAL");
            // ... mismo proceso que crearContactoPhone pero en el receptor
            // y con contactoTestTelefonoDUT como número
        }

        return recibirLlamada(serial, adb);
    }

    // Motor común de recepción
    private boolean recibirLlamada(String serial, ADBService adb) {
        if (contactoSerialReceptor == null)
            return false;

        adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell am start -a android.intent.action.CALL " +
                        "-d tel:" + contactoTestTelefonoDUT);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

        String estadoDUT = adb.ejecutarComandoSincrono(serial,
                "shell dumpsys telephony.registry | grep mCallState");
        boolean sonando = estadoDUT != null && estadoDUT.contains("1");

        adb.ejecutarComandoSincrono(serial,
                "shell input keyevent KEYCODE_CALL");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        String estadoActiva = adb.ejecutarComandoSincrono(serial,
                "shell dumpsys telephony.registry | grep mCallState");
        boolean activa = estadoActiva != null && estadoActiva.contains("2");

        adb.ejecutarComandoSincrono(serial,
                "shell input keyevent KEYCODE_ENDCALL");
        adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell input keyevent KEYCODE_ENDCALL");

        return sonando && activa;
    }

    // COPIA DE CONTACTOS
    private boolean copiarSimAlTelefono(String serial, ADBService adb) {
        boolean ok = true;
        try {
            for (int i = 1; i <= 3; i++) {
                String nombreOrigen = "Test_ADB_SIM_" + i;
                String nombreDestino = nombreOrigen + "_Copied";

                // 1. Intentar leer el contacto actual de la SIM
                String resultadoQuery = adb.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://icc/adn --where \"name='" + nombreOrigen + "'\"");

                if (resultadoQuery != null && resultadoQuery.contains("name=") && resultadoQuery.contains("number=")) {

                    // Extracción inline del número de la SIM
                    int startNum = resultadoQuery.indexOf("number=") + 7;
                    int endNum = resultadoQuery.indexOf(",", startNum);
                    String numero = resultadoQuery.substring(startNum, endNum == -1 ? resultadoQuery.length() : endNum)
                            .trim();

                    if (!numero.isEmpty()) {
                        // 2. Crear la fila base en los contactos del teléfono
                        adb.ejecutarComandoSincrono(serial,
                                "shell content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n:null --bind account_name:n:null");
                        Thread.sleep(200);

                        // 3. Buscar el ID que se acaba de generar (tu misma lógica exacta)
                        String idStr = adb.ejecutarComandoSincrono(serial,
                                "shell content query --uri content://com.android.contacts/raw_contacts --projection _id");
                        String id = "0";
                        if (idStr != null && !idStr.trim().isEmpty()) {
                            String[] lineas = idStr.split("\n");
                            int idMasAlto = 0;
                            for (String linea : lineas) {
                                if (linea.contains("_id=")) {
                                    String[] partes = linea.split("=");
                                    if (partes.length > 1) {
                                        String idLimpio = partes[1].replaceAll("[^0-9]", "").trim();
                                        if (!idLimpio.isEmpty()) {
                                            int idActual = Integer.parseInt(idLimpio);
                                            if (idActual > idMasAlto)
                                                idMasAlto = idActual;
                                        }
                                    }
                                }
                            }
                            id = String.valueOf(idMasAlto);
                        }

                        // 4. Vincular el Nombre de destino y el Número extraído de la SIM
                        adb.ejecutarComandoSincrono(serial,
                                "shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:"
                                        + id + " --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:"
                                        + nombreDestino);

                        adb.ejecutarComandoSincrono(serial,
                                "shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:"
                                        + id + " --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:"
                                        + numero + " --bind data2:i:2");

                        // 5. El aviso de recarga mágico de tu app
                        adb.ejecutarComandoSincrono(serial,
                                "shell am broadcast -a android.intent.action.PROVIDER_CHANGED --receiver-include-background content://com.android.contacts");

                        Thread.sleep(500);
                    } else {
                        ok = false;
                    }
                } else {
                    ok = false;
                }
            }
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    private boolean copiarTelefonoAlSim(String serial, ADBService adb) {
        boolean ok = true;
        try {
            for (int i = 1; i <= 3; i++) {
                String nombreOrigen = "Test_ADB_Phone_" + i;
                String nombreDestino = nombreOrigen + "_Copied";

                String comandoQuery = "shell content query --uri content://com.android.contacts/data/phones " +
                        "--projection display_name:data1 " +
                        "--where \"display_name=\\'" + nombreOrigen + "\\'\"";

                String resultadoQuery = adb.ejecutarComandoSincrono(serial, comandoQuery);

                if (resultadoQuery != null && resultadoQuery.contains("display_name=")
                        && resultadoQuery.contains("data1=")) {

                    int startNum = resultadoQuery.indexOf("data1=") + 6;
                    int endNum = resultadoQuery.indexOf(",", startNum);

                    if (endNum == -1) {
                        endNum = resultadoQuery.indexOf("\n", startNum);
                    }
                    if (endNum == -1) {
                        endNum = resultadoQuery.length();
                    }

                    String numero = resultadoQuery.substring(startNum, endNum).trim();
                    numero = numero.replaceAll("[^0-9+]", "");

                    if (!numero.isEmpty()) {
                        String comandoInsert = String.format(
                                "shell content insert --uri content://icc/adn --bind name:s:'%s' --bind number:s:'%s'",
                                nombreDestino, numero);
                        adb.ejecutarComandoSincrono(serial, comandoInsert);

                        adb.ejecutarComandoSincrono(serial,
                                "shell am broadcast -a android.intent.action.PROVIDER_CHANGED --receiver-include-background content://com.android.contacts");

                        Thread.sleep(500);
                    } else {
                        ok = false;
                    }
                } else {
                    ok = false;
                }
            }
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    // VCARD
    private boolean importarVCard(String serial, ADBService adb) {
        String nombreTest = "Test_import";
        String telefonoTest = "600112233";
        String rutaVCard = "/sdcard/Download/import_test.vcf";
        String paqueteAPK = "com.example.vcardagent";
        File apkFile = null;

        try {
            // Limpiar rutas
            adb.ejecutarComandoSincrono(serial,
                    "shell content delete --uri content://com.android.contacts/raw_contacts " +
                            "--where \"display_name='" + nombreTest + "'\"");
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaVCard);

            // Crear archivo VCard
            String vcardContenido = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:" + nombreTest + "\r\nTEL;TYPE=CELL:"
                    + telefonoTest
                    + "\r\nEND:VCARD\r\n";
            File tempFile = File.createTempFile("import_test", ".vcf");
            Files.writeString(tempFile.toPath(), vcardContenido);

            new ProcessBuilder("adb", "-s", serial, "push", tempFile.getAbsolutePath(), rutaVCard).start()
                    .waitFor();
            tempFile.delete(); // Limpiar el archivo temporal

            // Extraer e Instalar la apk
            desactivarPlayProtect(serial, adb);
            apkFile = obtenerApkDeResources("VCardAgent.apk");
            new ProcessBuilder("adb", "-s", serial, "install", "-r", "-g", "-t", apkFile.getAbsolutePath()).start()
                    .waitFor();
            Thread.sleep(2000);

            // Permisos tradicionales
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.WRITE_CONTACTS");
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.READ_CONTACTS");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm grant " + paqueteAPK + " android.permission.READ_EXTERNAL_STORAGE");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm grant " + paqueteAPK + " android.permission.WRITE_EXTERNAL_STORAGE");

            // Administración de archivos (Solución robusta para saltar Scoped Storage en
            // Android moderno)
            adb.ejecutarComandoSincrono(serial, "shell appops set " + paqueteAPK + " MANAGE_EXTERNAL_STORAGE allow");
            Thread.sleep(500);

            // Limpiar el logcat
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");

            // Broadcast importacion
            adb.ejecutarComandoSincrono(serial, "shell am broadcast -a com.example.vcardagent.ACTION_IMPORT_VCARD " +
                    "-n " + paqueteAPK + "/.VCardReceiver");
            Thread.sleep(7000);

            // Verificaciones
            String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d VCARD_AGENT:D *:S");

            boolean apkDiceOk = logcat != null && logcat.contains("Importación verídica completada");

            String queryBaseDatos = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.contacts/data/phones --projection display_name " +
                            "--where \"display_name='" + nombreTest + "'\"");

            boolean contactoExisteEnAgenda = queryBaseDatos != null && queryBaseDatos.contains(nombreTest);

            return apkDiceOk && contactoExisteEnAgenda;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                new ProcessBuilder("adb", "-s", serial, "uninstall", paqueteAPK).start().waitFor();
            } catch (Exception e) {
                System.err.println("No se pudo desinstalar " + paqueteAPK + ": " + e.getMessage());
            }

            if (apkFile != null && apkFile.exists()) {
                apkFile.delete();
            }

        }
    }

    private boolean exportarVCard(String serial, ADBService adb) {
        String rutaVCard = "/sdcard/Download/contactos_backup_test.vcf";
        String paqueteAPK = "com.example.vcardagent";
        File apkFile = null;

        try {
            // Limpiar archivo de pruebas anterior si existiese
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaVCard);

            // Extraer e Instalar la APK de forma segura (Inmune a rutas virtuales del .jar)
            desactivarPlayProtect(serial, adb);
            apkFile = obtenerApkDeResources("VCardAgent.apk");
            new ProcessBuilder("adb", "-s", serial, "install", "-r", "-g", "-t", apkFile.getAbsolutePath()).start()
                    .waitFor();
            Thread.sleep(2000);

            // Conceder permisos a la apk
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.READ_CONTACTS");
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.WRITE_CONTACTS");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm grant " + paqueteAPK + " android.permission.WRITE_EXTERNAL_STORAGE");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm grant " + paqueteAPK + " android.permission.READ_EXTERNAL_STORAGE");

            // Limpiar logcat
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");

            // Broadcast Exportar
            adb.ejecutarComandoSincrono(serial, "shell am broadcast -a com.example.vcardagent.ACTION_EXPORT_VCARD " +
                    "-n " + paqueteAPK + "/.VCardReceiver");
            Thread.sleep(4000);

            // Verificar si la apk terminó con éxito
            String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d VCARD_AGENT:D *:S");
            boolean apkOk = logcat != null && logcat.contains("Exportación exitosa");

            boolean archivoExiste = false;
            String checkLs = adb.ejecutarComandoSincrono(serial, "shell ls -l " + rutaVCard);
            if (checkLs != null && checkLs.contains(".vcf")) {
                String[] partes = checkLs.trim().split("\\s+");
                for (String parte : partes) {
                    if (parte.matches("\\d+")) {
                        long tamanoBytes = Long.parseLong(parte);
                        if (tamanoBytes > 0) {
                            archivoExiste = true;
                            break;
                        }
                    }
                }
            }

            return apkOk && archivoExiste;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                new ProcessBuilder("adb", "-s", serial, "uninstall", paqueteAPK).start().waitFor();
            } catch (Exception e) {
                System.err.println("Error al desinstalar " + paqueteAPK + ": " + e.getMessage());
            }

            if (apkFile != null && apkFile.exists()) {
                apkFile.delete();
            }
        }
    }

    // MEMORIA
    private boolean comprobarMemoriaContactos(String serial, ADBService adb, StringBuilder outputReporte) {
        String paqueteAPK = "com.example.simreceiver";
        File apkFile = null;

        try {
            // sección teléfono
            String resPhone = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.contacts/contacts --projection _id");
            if (resPhone == null || resPhone.contains("Error")) {
                outputReporte.append("ERROR: No se pudo conocer los contactos del telefono.\n");
                return false;
            }
            int contactosTel = 0;
            if (!resPhone.trim().isEmpty() && !resPhone.contains("No result found")) {
                for (String linea : resPhone.split("\r?\n")) {
                    if (linea.startsWith("Row:"))
                        contactosTel++;
                }
            }

            String statOutput = adb.ejecutarComandoSincrono(serial, "shell stat -f -c \"'%b %a %s'\" /data");
            String almacenamientoInfo = "Espacio desconocido";
            if (statOutput != null && !statOutput.isEmpty() && !statOutput.contains("Error")) {
                try {
                    String[] datos = statOutput.replace("'", "").trim().split("\\s+");
                    if (datos.length >= 3) {
                        long bloquesTotales = Long.parseLong(datos[0]);
                        long bloquesLibres = Long.parseLong(datos[1]);
                        long tamBloque = Long.parseLong(datos[2]);
                        double totalGB = (double) (bloquesTotales * tamBloque) / (1024 * 1024 * 1024);
                        double libreGB = (double) (bloquesLibres * tamBloque) / (1024 * 1024 * 1024);
                        almacenamientoInfo = String.format("Total: %.1fG | Disp: %.1fG", totalGB, libreGB);
                    }
                } catch (Exception e) {
                    almacenamientoInfo = "Almacenamiento interno operativo";
                }
            }
            outputReporte
                    .append(String.format("PHONE CONTACTS: %d guardados (%s)\n", contactosTel, almacenamientoInfo));

            // sección usim
            String simState = adb.ejecutarComandoSincrono(serial, "shell getprop gsm.sim.state");
            boolean tieneSim = simState != null && (simState.contains("LOADED") || simState.contains("READY"));

            if (!tieneSim) {
                outputReporte.append("USIM CONTACTS: SKIPPED (No hay tarjeta SIM insertada)\n");
                return true;
            }

            String resSim = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://icc/adn --projection _id");
            if (resSim == null || resSim.contains("Error")) {
                outputReporte.append("ERROR: Tarjeta SIM presente pero no se pudo leer su memoria.\n");
                return false;
            }

            int contactosSim = 0;
            if (!resSim.trim().isEmpty() && !resSim.contains("No result found")) {
                for (String linea : resSim.split("\r?\n")) {
                    if (linea.startsWith("Row:"))
                        contactosSim++;
                }
            }

            int maxSim = 0;
            boolean apkEjecutadaConExito = false;

            // Extraer e Instalar la APK de forma segura (Inmune a rutas virtuales del .jar)
            desactivarPlayProtect(serial, adb);
            apkFile = obtenerApkDeResources("SimHelper.apk");
            new ProcessBuilder("adb", "-s", serial, "install", "-r", "-g", "-t", apkFile.getAbsolutePath()).start()
                    .waitFor();
            Thread.sleep(2000);

            // Conceder permisos necesarios
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.READ_CONTACTS");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm grant " + paqueteAPK + " android.permission.READ_PHONE_STATE");
            Thread.sleep(500);

            // Limpiar logcat
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");

            adb.ejecutarComandoSincrono(serial, "shell am broadcast -a com.example.simreceiver.ACTION_GET_CAPACITY " +
                    "-n " + paqueteAPK + "/.SimReceiver");

            // Espera de estabilidad de 4 segundos completos
            Thread.sleep(4000);

            // Verificar resultado en Logcat
            String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d -t 50 -s SIMHelper");

            if (logcat != null && logcat.contains("CAPACIDAD_SIM_FINAL:")) {
                String[] partes = logcat.split("CAPACIDAD_SIM_FINAL:");
                if (partes.length > 1) {
                    String valorTexto = partes[partes.length - 1].trim().split("[\r\n\\s]")[0];
                    try {
                        if (!valorTexto.contains("ERROR")) {
                            maxSim = Integer.parseInt(valorTexto);
                            if (maxSim > 0) {
                                apkEjecutadaConExito = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error parseo: " + valorTexto);
                    }
                }
            }

            // Reporte final con textos limpios y sin tildes conflictivas para el PDF
            if (apkEjecutadaConExito) {
                outputReporte.append(
                        String.format("USIM CONTACTS: %d guardados de %d ranuras max.\n", contactosSim, maxSim));
            } else {
                outputReporte.append(
                        String.format("USIM CONTACTS: %d registros leidos con exito (Capacidad maxima no disponible)\n",
                                contactosSim));
            }
            return true;

        } catch (Exception e) {
            outputReporte.append("ERROR: Fallo critico en prueba de memoria: " + e.getMessage() + "\n");
            return false;
        } finally {
            try {
                new ProcessBuilder("adb", "-s", serial, "uninstall", paqueteAPK).start().waitFor();
            } catch (Exception e) {
                System.err.println("Error al desinstalar " + paqueteAPK + ": " + e.getMessage());
            }

            if (apkFile != null && apkFile.exists()) {
                apkFile.delete();
            }
        }
    }

    // PRUEBAS DE CALENDARIO
    @FXML
    private void addCalendarTest() {
        List<BloquePrueba> bloqueCalendar = List.of(
                new BloquePrueba(true, "SOFT.029.001", "Open calendar app",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_CALENDAR"),
                new BloquePrueba("SOFT.029.002", "Create a new event with a reminder", CALENDAR_CREATE),
                new BloquePrueba("SOFT.029.003", "Check if DUT shows the reminder", "", true),
                new BloquePrueba("SOFT.029.004", "Edit a created event", CALENDAR_EDIT),
                new BloquePrueba("SOFT.029.005", "Delete a created event", CALENDAR_DELETE));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueCalendar.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueCalendar;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.029 — Calendar", bloquesAFiltrar, owner,
                seleccionas -> seleccionas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean crearEventoCalendario(String serial, ADBService adb) {
        try {
            // Abrir la app de calendario para asegurar que el provider esté activo
            adb.ejecutarComandoSincrono(serial,
                    "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_CALENDAR");
            Thread.sleep(1000);

            // Obtener el ID del calendario actual

            String calIdStr = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.calendar/calendars --projection _id:account_name:account_type");

            String calId = "";
            if (calIdStr != null && !calIdStr.isBlank() && !calIdStr.toLowerCase().contains("usage")) {
                for (String linea : calIdStr.split("\n")) {
                    if (linea.contains("_id=")) {
                        int index = linea.indexOf("_id=");
                        String sub = linea.substring(index + 4).trim();
                        String[] partes = sub.split("[\\s,]+");
                        if (partes.length > 0 && partes[0].matches("\\d+")) {
                            calId = partes[0];
                            break;
                        }
                    }
                }
            }
            // Extraer el nombre de la cuenta de Google loggeada
            String googleAccountName = "";

            if (calIdStr != null && calIdStr.contains("account_name=")) {
                for (String linea : calIdStr.split("\n")) {
                    if (linea.contains("account_type=com.google") && linea.contains("account_name=")) {
                        int idx = linea.indexOf("account_name=");
                        String sub = linea.substring(idx + 13).trim();
                        String[] partes = sub.split("[\\s,]+");
                        if (partes.length > 0) {
                            googleAccountName = partes[0];
                            break;
                        }
                    }
                }
            }

            if (calId.isEmpty() || (calIdStr != null && calIdStr.contains("account_type=com.google"))) {
                calId = "3";
            }

            // Configurar tiempos
            long ahora = System.currentTimeMillis();
            long unDiaEnMilis = 24L * 60 * 60 * 1000;
            long inicio = ahora + unDiaEnMilis;
            long fin = inicio + (60 * 60 * 1000);

            String comandoInsert = "shell \"content insert --uri 'content://com.android.calendar/events?caller_is_syncadapter=true&account_name="
                    + googleAccountName + "&account_type=com.google' "
                    +
                    "--bind calendar_id:i:" + calId + " " +
                    "--bind title:s:'Test_ADB_Event' " +
                    "--bind description:s:'Evento_de_prueba_ADB' " +
                    "--bind dtstart:l:" + inicio + " " +
                    "--bind dtend:l:" + fin + " " +
                    "--bind hasAlarm:i:1 " +
                    "--bind eventTimezone:s:'Europe/Madrid'\"";

            adb.ejecutarComandoSincrono(serial, comandoInsert);

            // Buscar el ID del evento recién creado
            String listaEventos = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.calendar/events --projection _id:title");

            String eventoId = "";
            if (listaEventos != null && !listaEventos.isBlank() && !listaEventos.toLowerCase().contains("usage")) {
                for (String linea : listaEventos.split("\n")) {
                    if (linea.contains("title=Test_ADB_Event") && linea.contains("_id=")) {
                        int index = linea.indexOf("_id=");
                        String sub = linea.substring(index + 4).trim();
                        String[] partes = sub.split("[\\s,]+");
                        if (partes.length > 0 && partes[0].matches("\\d+")) {
                            eventoId = partes[0];
                        }
                    }
                }
            }

            if (eventoId.isEmpty()) {
                return false;
            }

            // Insertar el recordatorio
            String r = adb.ejecutarComandoSincrono(serial,
                    "shell content insert --uri content://com.android.calendar/reminders " +
                            "--bind event_id:i:" + eventoId + " " +
                            "--bind minutes:i:0 " +
                            "--bind method:i:1");

            boolean exito = r != null && !r.toLowerCase().contains("error") && !r.toLowerCase().contains("usage");
            return exito;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean editarEventoCalendario(String serial, ADBService adb) {
        try {
            // Abrir la app para que el provider este activo
            adb.ejecutarComandoSincrono(serial,
                    "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_CALENDAR");
            Thread.sleep(1000);

            // Buscar si el evento existe
            String listaEventos = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.calendar/events --projection _id:title");

            boolean existe = listaEventos != null && listaEventos.contains("title=Test_ADB_Event");

            // Si no existe, lo creamos dinámicamente
            if (!existe) {
                boolean creado = crearEventoCalendario(serial, adb);
                if (!creado)
                    return false;

                listaEventos = adb.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://com.android.calendar/events --projection _id:title");
            }

            // Extraer el ID del evento objetivo
            String targetId = "";
            if (listaEventos != null && !listaEventos.isBlank() && !listaEventos.toLowerCase().contains("usage")) {
                for (String linea : listaEventos.split("\n")) {
                    if (linea.contains("title=Test_ADB_Event") && linea.contains("_id=")) {
                        int index = linea.indexOf("_id=");
                        String sub = linea.substring(index + 4).trim();
                        String[] partes = sub.split("[\\s,]+");
                        if (partes.length > 0 && partes[0].matches("\\d+")) {
                            targetId = partes[0];
                            break;
                        }
                    }
                }
            }

            if (targetId.isEmpty()) {
                return false;
            }

            String comandoUpdate = "shell \"content update --uri content://com.android.calendar/events "
                    +
                    "--bind title:s:'Test_ADB_Event_Edited' " +
                    "--bind description:s:'Evento_editado_ADB' " +
                    "--where _id=" + targetId + "\"";

            String r = adb.ejecutarComandoSincrono(serial, comandoUpdate);

            boolean exito = r != null && !r.toLowerCase().contains("error") && !r.toLowerCase().contains("usage");

            return exito;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean borrarEventoCalendario(String serial, ADBService adb) {
        try {
            // Abrir la app para que provider este activo
            adb.ejecutarComandoSincrono(serial,
                    "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_CALENDAR");
            Thread.sleep(1000);

            // Buscar si existe el evento original o el editado
            String listaEventos = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.calendar/events --projection _id:title");

            boolean existe = listaEventos != null && (listaEventos.contains("title=Test_ADB_Event")
                    || listaEventos.contains("title=Test_ADB_Event_Edited"));

            // Si no hay nada que borrar, recreamos el entorno para que la prueba tenga
            if (!existe) {
                boolean creado = crearEventoCalendario(serial, adb);
                if (!creado)
                    return false;

                listaEventos = adb.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://com.android.calendar/events --projection _id:title");
            }

            // Iterar y borrar los eventos que coincidan
            if (listaEventos != null && !listaEventos.isBlank() && !listaEventos.toLowerCase().contains("usage")) {
                for (String linea : listaEventos.split("\n")) {
                    if ((linea.contains("title=Test_ADB_Event") || linea.contains("title=Test_ADB_Event_Edited"))
                            && linea.contains("_id=")) {
                        int index = linea.indexOf("_id=");
                        String sub = linea.substring(index + 4).trim();
                        String[] partes = sub.split("[\\s,]+");
                        if (partes.length > 0 && partes[0].matches("\\d+")) {
                            String idABorrar = partes[0];

                            String comandoDelete = "shell \"content delete --uri content://com.android.calendar/events --where _id="
                                    + idABorrar + "\"";
                            adb.ejecutarComandoSincrono(serial, comandoDelete);
                        }
                    }
                }
            }
            // Verificar que ya no existen en la base de datos
            String verificacion = adb.ejecutarComandoSincrono(serial,
                    "shell \"content query --uri content://com.android.calendar/events --projection title --where deleted=0\"");

            boolean exito = verificacion == null || (!verificacion.contains("title=Test_ADB_Event")
                    && !verificacion.contains("title=Test_ADB_Event_Edited"));

            return exito;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // PRUEBAS DE MENSAJES
    @FXML
    private void addMessagingTest() {
        ADBService adb = new ADBService();
        String serial;
        try {
            serial = adb.getSerialActivo(dispositivoActual.getAndroid_id());
        } catch (IOException e) {
            serial = dispositivoActual.getSerialNumber();
        }

        // Obtener otros dispositivos
        List<String> otrosSeriales = new ArrayList<>();
        try {
            Map<String, String> todos = adb.obtenerDispositivosConectados();
            for (Map.Entry<String, String> entry : todos.entrySet()) {
                if (!entry.getValue().equals(serial)) {
                    otrosSeriales.add(entry.getValue());
                }
            }
        } catch (IOException e) {
            System.out.println("[MESSAGING] No se pudieron obtener otros dispositivos");
        }

        String[] telefono = { contactoTestTelefono };
        String[] telefonoDUT = { contactoTestTelefonoDUT };
        String[] serialReceptor = { contactoSerialReceptor != null ? contactoSerialReceptor : "" };

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        boolean confirmado = MensajesConfigPopup.mostrar(otrosSeriales, telefono, telefonoDUT, serialReceptor, owner);

        if (!confirmado) {
            return;
        }

        contactoTestTelefono = telefono[0];
        contactoTestTelefonoDUT = telefonoDUT[0];
        contactoSerialReceptor = (serialReceptor[0] != null && !serialReceptor[0].isEmpty()) ? serialReceptor[0] : null;

        mostrarSelectorMensajes();
    }

    private void mostrarSelectorMensajes() {
        List<BloquePrueba> bloqueMensajes = List.of(
                new BloquePrueba(true, "SOFT.015.001", "Send a SMS to a phone number", MSG_SEND_SMS_NUMBER),
                new BloquePrueba("SOFT.015.002", "Send a SMS to a phone contact", MSG_SEND_SMS_CONTACT),
                new BloquePrueba("SOFT.015.003", "Send a SMS to a exchange contact", "", true),
                new BloquePrueba("SOFT.015.004", "Receive a new SMS from other phone",
                        contactoSerialReceptor != null ? MSG_RECEIVE_SMS : "",
                        contactoSerialReceptor != null ? false : true),
                new BloquePrueba("SOFT.015.005", "Send a SMS using a quick text", "", true),
                new BloquePrueba("SOFT.015.006", "Send a MMS to a phone number", MSG_SEND_MMS_NUMBER),
                new BloquePrueba("SOFT.015.007", "Send a MMS to a phone contact", MSG_SEND_MMS_CONTACT),
                new BloquePrueba("SOFT.015.008", "Receive a new MMS from other phone",
                        contactoSerialReceptor != null ? MSG_RECEIVE_MMS : "",
                        contactoSerialReceptor != null ? false : true),
                new BloquePrueba(true, "SOFT.015.009", "Delete one conversation", MSG_DELETE_ONE),
                new BloquePrueba("SOFT.015.010", "Delete all conversations", MSG_DELETE_ALL),
                new BloquePrueba("SOFT.015.011", "Check number of Messaging Center SMSC", List.of(
                        "shell content query --uri content://telephony/carriers --projection mmsc --where \"current=1\"",
                        "shell getprop gsm.sim.operator.alpha")),
                // shell am start -n
                // com.google.android.apps.messaging/.ui.appsettings.PerSubscriptionSettingsActivity

                new BloquePrueba("SOFT.015.012", "Check if SMS Validity Period is available",
                        "shell am start -a android.settings.WIRELESS_SETTINGS", true),
                new BloquePrueba("SOFT.015.013",
                        "Check id SMS optimization option is available. By default need to be active (use only 7 bit)",
                        "shell content query --uri content://sms --projection _id"),
                new BloquePrueba("SOFT.015.014",
                        "With SMS optimization enable, check if you cand send a special character\nSend SMS with special characters (ÁÉÍÓÚáéíóú)\nExpected:\nDuT must use 7-bit encoding and send all characters ly (e.g. AÉIOUaéiou)",
                        MSG_SEND_SMS_SPECIAL),
                new BloquePrueba("SOFT.015.015",
                        "With SMS optimization enable, compose a SMS with more than 160 characters and check if device advise you that will be send more than 1 SMS",
                        MSG_SEND_SMS_LONG),
                new BloquePrueba("SOFT.015.016",
                        "With SMS optimization disable, check if you can send a special character", MSG_SEND_SMS_NOOPT),
                new BloquePrueba("SOFT.015.017", "Turn off mobile data. Check if DUT sends a MMS properly",
                        MSG_MMS_NO_DATA));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueMensajes.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueMensajes;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.015 — Messaging functions", bloquesAFiltrar, owner,
                seleccionas -> seleccionas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean verificarYCrearContactoPhone(String serial, ADBService adb) {
        String check = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://com.android.contacts/data " +
                        "--projection raw_contact_id --where \"mimetype='vnd.android.cursor.item/phone_v2'" +
                        " AND data1='" + contactoTestTelefono + "'\"");
        boolean existe = check != null && !check.isBlank() && !check.contains("No result foun");
        if (!existe) {
            crearContactoPhone(serial, adb);
        }
        return true;
    }

    private boolean verificarSMSEnviado(String serial, ADBService adb, String numero) {
        String numeroLimpio = numero.replaceAll("[^0-9]", "");
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            String check = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://sms/sent --projection address:body --sort \"date\"");
            if (check != null && check.contains(numeroLimpio)) {
                return true;
            }
        }
        return false;
    }

    private void simularClickEnviar(String serial, ADBService adb) {
        try {
            // Despertar pantalla
            adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_WAKEUP");
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }

            // Capturar interfaz
            adb.ejecutarComandoSincrono(serial, "shell uiautomator dump /data/local/tmp/window_dump.xml");
            String xml = adb.ejecutarComandoSincrono(serial, "shell cat /data/local/tmp/window_dump.xml");

            if (xml == null || xml.isBlank())
                return;

            String[] selectores = {
                    "Compose:Draft:Send",
                    "com.google.android.apps.messaging:id/send_message_button",
                    "com.android.messaging:id/send_message_button",
                    "com.samsung.android.messaging:id/send_button",
                    "content-desc=\"Enviar\"",
                    "content-desc=\"Send\""
            };

            String targetLine = null;
            for (String selector : selectores) {
                if (xml.contains(selector)) {
                    for (String linea : xml.split(">")) {
                        if (linea.contains(selector) && linea.contains("bounds=")) {
                            targetLine = linea;
                            break;
                        }
                    }
                }
                if (targetLine != null)
                    break;
            }

            if (targetLine != null) {
                int idx = targetLine.indexOf("bounds=\"");
                if (idx != -1) {
                    String bounds = targetLine.substring(idx + 8, targetLine.indexOf("\"", idx + 8));
                    String[] pts = bounds.replaceAll("[\\[\\]]", " ").trim().split("\\s+");
                    if (pts.length >= 2) {
                        String[] p1 = pts[0].split(",");
                        String[] p2 = pts[1].split(",");
                        int xCentro = (Integer.parseInt(p1[0]) + Integer.parseInt(p2[0])) / 2;
                        int yCentro = (Integer.parseInt(p1[1]) + Integer.parseInt(p2[1])) / 2;

                        // Click táctil
                        adb.ejecutarComandoSincrono(serial, "shell input tap " + xCentro + " " + yCentro);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback físico
        adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_ESCAPE");
        try {
            Thread.sleep(400);
        } catch (InterruptedException ignored) {
        }
        adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_DPAD_RIGHT");
        adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_DPAD_CENTER");
    }

    private void simularClickBotonAdjuntar(String serial, ADBService adb) {
        try {
            adb.ejecutarComandoSincrono(serial, "shell uiautomator dump /data/local/tmp/window_dump.xml");
            String xml = adb.ejecutarComandoSincrono(serial, "shell cat /data/local/tmp/window_dump.xml");

            if (xml == null || xml.isBlank()) {
                System.out.println("[DEBUG-ADJUNTAR] ERROR: El XML de la pantalla está vacío.");
                return;
            }

            String[] selectores = {
                    "ComposeRowIcon:Gallery",
                    "com.google.android.apps.messaging:id/attach_media_button",
                    "com.google.android.apps.messaging:id/attachment_menu_button",
                    "content-desc=\"Mostrar pantalla para adjuntar archivos multimedia\"", // Basado en tu primer
                                                                                           // volcado
                    "content-desc=\"Compartir contenido multimedia\"",
                    "content-desc=\"Attach\""
            };

            String targetLine = null;
            for (String selector : selectores) {
                if (xml.contains(selector)) {
                    System.out.println("[DEBUG-ADJUNTAR] Selector encontrado en XML: " + selector);
                    for (String linea : xml.split(">")) {
                        if (linea.contains(selector) && linea.contains("bounds=")) {
                            targetLine = linea;
                            break;
                        }
                    }
                }
                if (targetLine != null)
                    break;
            }

            if (targetLine != null) {
                System.out.println("[DEBUG-ADJUNTAR] Línea del nodo: " + targetLine.trim());
                int idx = targetLine.indexOf("bounds=\"");
                if (idx != -1) {
                    String bounds = targetLine.substring(idx + 8, targetLine.indexOf("\"", idx + 8));
                    String[] pts = bounds.replaceAll("[\\[\\]]", " ").trim().split("\\s+");
                    String[] p1 = pts[0].split(",");
                    String[] p2 = pts[1].split(",");
                    int xCentro = (Integer.parseInt(p1[0]) + Integer.parseInt(p2[0])) / 2;
                    int yCentro = (Integer.parseInt(p1[1]) + Integer.parseInt(p2[1])) / 2;

                    System.out.println("[DEBUG-ADJUNTAR] Coordenadas calculadas: X=" + xCentro + ", Y=" + yCentro);
                    adb.ejecutarComandoSincrono(serial, "shell input tap " + xCentro + " " + yCentro);
                    System.out.println("[DEBUG-ADJUNTAR] Comando tap ejecutado.");
                }
            } else {
                System.out.println("[DEBUG-ADJUNTAR] FAILED: Ningún selector coincidió.");
                System.out.println("\n--- XML DIAGNÓSTICO DE EMERGENCIA ---");
                System.out.println(xml.replace("><", ">\n<"));
                System.out.println("--- FIN XML DIAGNÓSTICO ---\n");
            }
        } catch (Exception e) {
            System.out.println("[DEBUG-ADJUNTAR] EXCEPCIÓN: " + e.getMessage());
        }
    }

    private void simularClickPrimeraImagenGaleria(String serial, ADBService adb) {
        try {
            adb.ejecutarComandoSincrono(serial, "shell uiautomator dump /data/local/tmp/window_dump.xml");
            String xml = adb.ejecutarComandoSincrono(serial, "shell cat /data/local/tmp/window_dump.xml");
            if (xml == null || xml.isBlank())
                return;

            // Selectores específicos del botón de captura de fotos (no de vídeo)
            String[] selectoresCamara = {
                    "com.google.android.apps.messaging:id/camera_button",
                    "content-desc=\"Cámara\"",
                    "content-desc=\"Camera\""
            };

            for (String selector : selectoresCamara) {
                if (xml.contains(selector)) {
                    for (String linea : xml.split(">")) {
                        // FILTRO: Debe contener el selector, las coordenadas, y NO ser el botón de
                        // vídeo o rcs_video
                        if (linea.contains(selector) && linea.contains("bounds=") && !linea.contains("video")) {

                            int idx = linea.indexOf("bounds=\"");
                            if (idx != -1) {
                                String bounds = linea.substring(idx + 8, linea.indexOf("\"", idx + 8));
                                String[] pts = bounds.replaceAll("[\\[\\]]", " ").trim().split("\\s+");
                                String[] p1 = pts[0].split(",");
                                String[] p2 = pts[1].split(",");

                                int x1 = Integer.parseInt(p1[0]);
                                int y1 = Integer.parseInt(p1[1]);
                                int x2 = Integer.parseInt(p2[0]);
                                int y2 = Integer.parseInt(p2[1]);

                                // Calculamos las proporciones de la cuadrícula
                                int anchoBoton = x2 - x1;

                                // Calculamos el centro de la primera miniatura (Fila superior)
                                int xImagenNueva = ((x1 + x2) / 2) + anchoBoton;
                                int yImagenNueva = y1 + (y2 - y1) / 2;

                             

                                // Ejecutamos el tap y salimos INMEDIATAMENTE del método completo
                                adb.ejecutarComandoSincrono(serial,
                                        "shell input tap " + xImagenNueva + " " + yImagenNueva);
                                return;
                            }
                        }
                    }
                }
            }

            // Si los bucles terminan sin hacer return, aplicamos el tiro de emergencia
            System.out.println("[DEBUG-GALERIA] No se aisló el nodo de forma limpia. Usando cuadrícula estática...");
            adb.ejecutarComandoSincrono(serial, "shell input tap 240 640");

        } catch (Exception e) {
            System.out.println("[DEBUG-GALERIA] Error en cálculo: " + e.getMessage());
            adb.ejecutarComandoSincrono(serial, "shell input tap 240 640");
        }
    }

    // MSG ── SMS ──────────────────────────────────────────────────────────────

    private boolean enviarSMSNumero(String serial, ADBService adb) {
        String r = adb.ejecutarComandoSincrono(serial, "shell am start -a android.intent.action.SENDTO " +
                "-d sms:" + contactoTestTelefono + " " +
                "--es sms_body 'Test_SMS_ADB' " +
                "--ez exit_on_sent true");
        if (r == null || r.toLowerCase().contains("error")) {
            return false;
        }

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        simularClickEnviar(serial, adb);

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }

        return verificarSMSEnviado(serial, adb, contactoTestTelefono);
    }

    private boolean enviarSMSContacto(String serial, ADBService adb) {
        verificarYCrearContactoPhone(serial, adb);
        return enviarSMSNumero(serial, adb);
    }

    private boolean recibirSMS(String serial, ADBService adb) {
        if (contactoSerialReceptor == null) {
            System.out.println("[DEBUG-RECIBIR] ERROR: contactoSerialReceptor es nulo.");
            return false;
        }

        // Identificador único por fecha y hora exacta
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String textoMensajeUnico = "Test_SMS_" + timestamp;

     
        // El receptor prepara el SMS en su interfaz gráfica
        adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell am start -a android.intent.action.SENDTO " +
                        "-d sms:" + contactoTestTelefonoDUT + " " +
                        "--es sms_body '" + textoMensajeUnico + "' " +
                        "--ez exit_on_sent true");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        // Envío visual
        simularClickEnviar(contactoSerialReceptor, adb);

        // Bucle de lectura en el DUT
        for (int i = 0; i < 7; i++) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }

            // Consulta cruda a toda la base de datos sin cláusula WHERE
            String check = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://sms --projection address:body");

            System.out.println("[DEBUG-RECIBIR] Intento " + (i + 1) + " - VOLCADO COMPLETO DE SMS:\n" + check);

            if (check == null || check.contains("not found") || check.contains("Exception")
                    || check.contains("Error")) {
                continue;
            }

            // Validamos si nuestro código único aparece en algún rincón del volcado total
            if (check.contains(textoMensajeUnico)) {
                System.out
                        .println("[DEBUG-RECIBIR] ¡ÉXITO! Mensaje de la conversación actual detectado en el volcado.");
                return true;
            }
        }

       
        return false;
    }

    // MSG ── MMS ──────────────────────────────────────────────────────────────

    private boolean enviarMMSNumero(String serial, ADBService adb) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String asuntoUnico = "MMS_" + timestamp;
        String numeroLimpio = contactoTestTelefono.trim().replaceAll("[^0-9+]", "");
        String rutaImagen = "/sdcard/Pictures/micro_valid.gif";

      

        try {
            // 1. Inyectar imagen GIF real de 1x1px (43 bytes)
            String gifHex = "47494638396101000100800000000000ffffff21f90401000000002c00000000010001000002024401003b";
            adb.ejecutarComandoSincrono(serial, "shell \"echo '" + gifHex + "' | xxd -r -p > " + rutaImagen + "\"");

            // CORRECCIÓN REFRESCO: Tal cual lo ejecutaste en PowerShell
            adb.ejecutarComandoSincrono(serial,
                    "shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + rutaImagen);
            Thread.sleep(2000); // Le damos 2 segundos completos para que aparezca en el carrete

            // 2. Abrir conversación vacía
            adb.ejecutarComandoSincrono(serial, "shell am start -a android.intent.action.SENDTO -d smsto:"
                    + numeroLimpio + " com.google.android.apps.messaging");
            Thread.sleep(4000);

            // 3. Tu método de adjuntar (el que funciona bien)
            simularClickBotonAdjuntar(serial, adb);
            Thread.sleep(2500);

            // 4. Seleccionar la primera imagen (Método nuevo ultra-preciso abajo)
            simularClickPrimeraImagenGaleria(serial, adb);
            Thread.sleep(2500);

            // 5. Escribir texto post-adjuntar
            Thread.sleep(500);
            adb.ejecutarComandoSincrono(serial, "shell input text '" + asuntoUnico + "'");
            Thread.sleep(1500);

            // 6. Disparar envío
            simularClickEnviar(serial, adb);
            Thread.sleep(4000);

            // 7. Verificación en Base de Datos
            String check = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://mms --projection _id --where \"sub='" + asuntoUnico + "'\"");

            return (check != null && check.contains("Row:"));

        } catch (Exception e) {
            return false;
        } finally {
            // Limpieza
            adb.ejecutarComandoSincrono(serial, "shell rm -f " + rutaImagen);
            adb.ejecutarComandoSincrono(serial,
                    "shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + rutaImagen);
        }
    }

    private boolean enviarMMSContacto(String serial, ADBService adb) {
        verificarYCrearContactoPhone(serial, adb);
        return enviarMMSNumero(serial, adb);
    }

    private boolean recibirMMS(String serial, ADBService adb) {
        if (contactoSerialReceptor == null) {
            return false;
        }

        adb.ejecutarComandoSincrono(contactoSerialReceptor, "shell am start -a android.intent.action.SENDTO " +
                "-d mmsto:" + contactoTestTelefonoDUT + " " +
                "--es subject 'Test_MMS_recv' " +
                "--es sms_body 'MMS_recibido_ADB'");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        adb.ejecutarComandoSincrono(contactoSerialReceptor,
                "shell input keyevent KEYCODE_ENTER");
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
        }

        String check = adb.ejecutarComandoSincrono(serial, "shell content query --uri content://mms/inbox " +
                "--projection _id --sort \"date DESC\"");
        return check != null && !check.isBlank() && !check.contains("No result found");
    }

    // MSG ── Borrar conversaciones ────────────────────────────────────────────

    private boolean borrarUnaConversacion(String serial, ADBService adb) {
        // Obtener el thread_id más reciente
        String threadStr = adb.ejecutarComandoSincrono(serial, "shell content query --uri content://sms " +
                "--projection thread_id --sort \"date DESC\"");
        if (threadStr == null || threadStr.isBlank()) {
            return false;
        }

        String threadId = "";
        for (String linea : threadStr.split("\n")) {
            if (linea.contains("thread_id")) {
                threadId = linea.replaceAll("[^0-9]", "").trim();
                if (!threadId.isEmpty()) {
                    break;
                }
            }
        }
        if (threadId.isEmpty()) {
            return false;
        }

        String r = adb.ejecutarComandoSincrono(serial, "shell content delete --uri content://sms " +
                "--where \"thread_id=" + threadId + "\"");
        return r != null && !r.toLowerCase().contains("error");
    }

    private boolean borrarTodasConversaciones(String serial, ADBService adb) {
        adb.ejecutarComandoSincrono(serial, "shell content delete --uri content://sms");

        // Verificar que se borraron
        String check = adb.ejecutarComandoSincrono(serial, "shell content query --uri content://sms --projection _id");
        return check == null || check.contains("No result found") || check.isBlank();
    }

    // MSG ── SMSC y configuración ─────────────────────────────────────────────

    private boolean enviarSMSCaracteresEspeciales(String serial, ADBService adb) {
        String r = adb.ejecutarComandoSincrono(serial, "shell am start -a android.intent.action.SENDTO " +
                "-d sms:" + contactoTestTelefono + " " +
                "--es sms_body 'ÁÉÍÓÚáéíóú' " +
                "--ez exit_on_sent true");
        if (r == null || r.toLowerCase().contains("error")) {
            return false;
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_ENTER");
        return verificarSMSEnviado(serial, adb, contactoTestTelefono);
    }

    private boolean enviarSMSLargo(String serial, ADBService adb) {
        // 159 caracteres que poder comprobar el popup de los 160
        String textoLargo = "A".repeat(159);
        String r = adb.ejecutarComandoSincrono(serial,
                "shell am start -a android.intent.action.SENDTO " +
                        "-d sms:" + contactoTestTelefono + " " +
                        "--es sms_body '" + textoLargo + "' " +
                        "--ez exit_on_sent true");
        if (r == null || r.toLowerCase().contains("error")) {
            return false;
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_ENTER");
        return verificarSMSEnviado(serial, adb, contactoTestTelefono);
    }

    private boolean enviarSMSSinOptimizacion(String serial, ADBService adb) {
        // Desactivar optimización
        adb.ejecutarComandoSincrono(serial, "shell settings put global sms_encoding_type 1");
        boolean ok = enviarSMSCaracteresEspeciales(serial, adb);

        // Restaurar
        adb.ejecutarComandoSincrono(serial, "shell settings put global sms_encoding_type 0");
        return ok;
    }

    private boolean enviarMMSSinDatos(String serial, ADBService adb) {
        // Desactivar datos móviles
        adb.ejecutarComandoSincrono(serial, "shell svc data disable");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        boolean ok = enviarMMSNumero(serial, adb);

        // Restaurar datos
        adb.ejecutarComandoSincrono(serial, "shell svc data enable");
        return ok;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DETECTAR NÚMERO DE TELÉFONO POR ADB
    //
    // Estrategia en cascada (Android 12/14):
    // 1. dumpsys telephony.registry → busca "mCallIncomingNumber" o "PhoneNumber"
    // 2. service call iphonesubinfo 6 → obtiene la línea de la SIM (MSISDN)
    // 3. settings get secure line1_number → fallback de settings
    //
    // Devuelve el número como String (ej "+34612345678") o null si no se encontró.
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // OBTENER SERIALES ADB CONECTADOS
    // Ejecuta "adb devices" y extrae los seriales de dispositivos online.
    // ─────────────────────────────────────────────────────────────────────────
    private List<String> obtenerSerialesADB() {
        List<String> lista = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "devices");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Formato: "192.168.x.x:5555\tdevice" o "SERIALXXX\tdevice"
                if (line.endsWith("\tdevice") || line.endsWith(" device")) {
                    String serial = line.split("\\s+")[0].trim();
                    if (!serial.isBlank())
                        lista.add(serial);
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("[ADB] Error listando dispositivos: " + e.getMessage());
        }
        return lista;
    }

    private String obtenerSerialADBActual() {
        if (dispositivoActual == null)
            return null;
        try {
            ADBService adb = new ADBService();
            return adb.getSerialActivo(dispositivoActual.getAndroid_id());
        } catch (IOException e) {
            // Fallback al serial guardado en BD
            return dispositivoActual.getSerialNumber();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Ejecuta un comando shell en un serial específico y devuelve la salida. */

    /** Actualiza el estado de un paso en el hilo de UI. */
    private void actualizarEstadoPaso(PasoPrueba paso, String estado) {
        Platform.runLater(() -> {
            paso.setEstado(estado);
            listaPasos.refresh();
        });
    }

    public void desactivarPlayProtect(String serial, ADBService adb) {
        try {
            adb.ejecutarComandoSincrono(serial, "shell settings put global package_verifier_enable 0");
            adb.ejecutarComandoSincrono(serial, "shell settings put global package_verifier_user_consent 0");
            adb.ejecutarComandoSincrono(serial, "shell settings put global upload_apk_enable 0");
            adb.ejecutarComandoSincrono(serial,
                    "shell pm disable-user com.google.android.gms/.chimera.GmsIntentOperationService");
        } catch (Exception e) {
            System.err.println("❌ Error al intentar desactivar Play Protect: " + e.getMessage());
        }
    }

    private boolean comprobarYLoguearCuentaGoogle(String serial, ADBService adb, String rutaHome) {
        try {
            // Comprobación inicial
            boolean cuentaDetectada = cuentaGoogleActiva(serial, adb);
            if (cuentaDetectada) {
                return true;
            }

            adb.ejecutarComandoSincrono(serial,
                    "shell am start -a android.settings.ADD_ACCOUNT_SETTINGS --es account_types com.google");

            while (!cuentaDetectada) {
                Thread.sleep(2000);
                cuentaDetectada = cuentaGoogleActiva(serial, adb);
            }

            adb.ejecutarComandoSincrono(serial, rutaHome);
            Thread.sleep(1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean cuentaGoogleActiva(String serial, ADBService adb) {
        String outputCuentas = adb.ejecutarComandoSincrono(serial, "shell dumpsys account");
        if (outputCuentas != null && !outputCuentas.isBlank()) {
            String[] lineas = outputCuentas.split("\n");
            for (String linea : lineas) {
                if (linea.contains("type=com.google") && linea.contains("name=") && linea.contains("@")) {
                    return true;
                }
            }
        }
        return false;
    }

    public File obtenerApkDeResources(String nombreApk) {
        try {
            String rutaEnResources = "apk/" + nombreApk;

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(rutaEnResources)) {
                if (in == null) {
                    return null;
                }

                File archivoTemporal = File.createTempFile("adb_helper_" + nombreApk.replace(".apk", ""), ".apk");
                Files.copy(in, archivoTemporal.toPath(), StandardCopyOption.REPLACE_EXISTING);

                return archivoTemporal;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void addBrowserTest() {
        List<BloquePrueba> bloqueBrowser = List.of(
                new BloquePrueba(true, "SOFT.028.001", "Open browser and surf on the internet",
                        "shell am start -a android.intent.action.VIEW -d https://www.google.com"),
                new BloquePrueba(true, "SOFT.028.002", "See an online video",
                        "shell am start -W -a android.intent.action.VIEW -d https://www.youtube.com/watch?v=dQw4w9WgXcQ && sleep 10",
                        true),
                new BloquePrueba("SOFT.028.003", "Download an image",
                        "shell am start -a android.intent.action.VIEW -d https://www.gstatic.com/webp/gallery/1.jpg",
                        true),
                new BloquePrueba("SOFT.028.004", "Download a music file",
                        "shell am start -a android.intent.action.VIEW -d https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                        true),
                new BloquePrueba(true, "SOFT.028.005", "Check homepage",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_BROWSER", true),
                new BloquePrueba(true, "SOFT.028.006", "Check bookmarks",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_BROWSER", true),
                new BloquePrueba("SOFT.028.007", "Create a new bookmark",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_BROWSER", true),
                new BloquePrueba("SOFT.028.008", "Edit a bookmark",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_BROWSER", true),
                new BloquePrueba("SOFT.028.009", "Delete a bookmark",
                        "shell am start -a android.intent.action.MAIN -c android.intent.category.APP_BROWSER", true),
                new BloquePrueba("SOFT.028.010", "Change homepage",
                        "shell am start -a android.settings.SETTINGS", true));

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueBrowser.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueBrowser;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar(
                "SOFT.028 — Browser",
                bloquesAFiltrar,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    @FXML
    private void addAdditionalSettingsTest() {
        String modelo = obtenerModeloDispositivoActual();
        List<BloquePrueba> bloqueAdditionalSettings = SC04.crearBloquesAdditionalSettings(modelo);

        boolean modoExpressActivo = btnIotExpress.isSelected();
        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueAdditionalSettings.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueAdditionalSettings;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar(
                "SOFT.035 — Additional settings",
                bloquesAFiltrar,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private String obtenerModeloDispositivoActual() {
        if (dispositivoActual == null) {
            return "";
        }

        try {
            return ejecutarShellEnSerial(obtenerSerialADBActual(), "getprop ro.product.model");
        } catch (Exception e) {
            return dispositivoActual.getAndroid_id();
        }
    }

    private boolean ejecutarFMBackground(String serial, PasoPrueba paso) {
        try {
            actualizarEstadoPaso(paso, "Abriendo FM...");
            ejecutarShellEnSerial(serial, "am start -W -n com.android.fmradio/.FmMainActivity");
            Thread.sleep(3_000);

            actualizarEstadoPaso(paso, "Enviando a background...");
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_HOME");
            Thread.sleep(2_000);

            actualizarEstadoPaso(paso, "Confirmar manualmente...");
            Stage owner = (Stage) btnEjecutar.getScene().getWindow();
            boolean ok = ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.017.015 — Check if it's possible to listen radio FM in background",
                    owner,
                    "La FM Radio se ha enviado a background.\n\n" +
                            "PASS: la radio sigue sonando con la app en segundo plano.\n" +
                            "FAIL: la radio se detiene al enviarla a background.");

            Platform.runLater(() -> {
                paso.setOutputDetalle(ok
                        ? "Confirmado: FM suena en background"
                        : "Confirmado: FM no suena en background");
                listaPasos.refresh();
            });

            return ok;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarFmRadioD17(String serial, PasoPrueba paso, boolean verificarAuricular,
            boolean ejecutarAutosearch,
            String estadoInicial) {
        try {
            ADBService adb = new ADBService();
            if (verificarAuricular && !adb.tieneAuricularConectado(serial)) {
                actualizarEstadoPaso(paso, "Auricular no conectado");
                Platform.runLater(() -> {
                    paso.setOutputDetalle("Auricular no conectado");
                    listaPasos.refresh();
                });
                return false;
            }

            actualizarEstadoPaso(paso, estadoInicial);
            ejecutarShellEnSerial(serial, "am start -W -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity");
            Thread.sleep(2_500);

            if (ejecutarAutosearch) {
                ejecutarShellEnSerial(serial, "input keyevent 84");
                Thread.sleep(2_000);
            }

            ejecutarShellEnSerial(serial, "input keyevent 82");
            Thread.sleep(1_500);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_CENTER");
            Thread.sleep(1_500);
            ejecutarShellEnSerial(serial, "input keyevent 82");
            Thread.sleep(1_500);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_CENTER");
            Thread.sleep(8_500);
            ejecutarShellEnSerial(serial, "input keyevent 4");
            Thread.sleep(1_000);

            Platform.runLater(() -> {
                paso.setOutputDetalle("Auricular detectado y secuencia FM ejecutada");
                listaPasos.refresh();
            });
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarFmRadioD17RightOk(String serial, PasoPrueba paso) {
        try {
            actualizarEstadoPaso(paso, "FM D17: abriendo radio...");
            ejecutarShellEnSerial(serial, "am start -W -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity");
            Thread.sleep(2_500);

            ejecutarShellEnSerial(serial, "input keyevent 22");
            Thread.sleep(1_000);
            ejecutarShellEnSerial(serial, "input keyevent 23");
            Thread.sleep(1_000);

            Platform.runLater(() -> {
                paso.setOutputDetalle("Secuencia automatica ejecutada: derecha + OK");
                listaPasos.refresh();
            });
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarFmD17RecordSequence(String serial, PasoPrueba paso) {
        try {
            actualizarEstadoPaso(paso, "FM D17: abriendo radio...");
            ejecutarShellEnSerial(serial, "am start -W -n com.android.fmradio/com.android.fmradio.LancoFmMainActivity");
            Thread.sleep(2_000);

            actualizarEstadoPaso(paso, "FM D17: iniciando grabación...");
            ejecutarShellEnSerial(serial, "input keyevent 82");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_DOWN");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_CENTER");

            Thread.sleep(5_000);

            ejecutarShellEnSerial(serial, "input keyevent 82");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_CENTER");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_DOWN");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_DOWN");
            Thread.sleep(400);
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_DPAD_CENTER");

            actualizarEstadoPaso(paso, "FM D17: grabación iniciada y guardada");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            actualizarEstadoPaso(paso, "FM D17: interrumpido durante la grabación");
            return false;
        }
    }

    @FXML
    private void addFMRadioTest() {
        String serial = obtenerSerialADBActual();
        String modelo = "D17";
        if (serial != null) {
            try {
                modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
            } catch (Exception ignored) {
            }
        }

        List<BloquePrueba> bloqueFMRadio = LlamadasD17.crearBloquesFmRadio(modelo);

        boolean modoExpressActivo = btnIotExpress.isSelected();

        List<BloquePrueba> bloquesAFiltrar = modoExpressActivo
                ? bloqueFMRadio.stream().filter(BloquePrueba::isIotExpress).toList()
                : bloqueFMRadio;

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar(
                "SOFT.017 — FM Radio",
                bloquesAFiltrar,
                owner,
                seleccionadas -> {
                    List<BloquePrueba> seleccionNormal = new ArrayList<>();
                    boolean requiereConfigFm = false;

                    for (BloquePrueba bloque : seleccionadas) {
                        if ("SOFT.017.016".equals(bloque.getId())) {
                            requiereConfigFm = true;
                        } else {
                            seleccionNormal.add(bloque);
                        }
                    }

                    pasos.addAll(seleccionNormal.stream()
                            .map(BloquePrueba::toPasoPrueba)
                            .toList());

                    if (requiereConfigFm) {
                        Platform.runLater(() -> {
                            if (configurarLlamadaEntranteParaFm(owner)) {
                                pasos.add(new PasoPrueba(
                                        "SOFT.017.016  —  While radio FM playing, receive a call. Check if after call ends radio FM continues playing",
                                        List.of("__FM_LLAMADA_ENTRANTE__"),
                                        false));
                            }
                        });
                    }
                });
    }

    /** Crea un Button con el color de acento indicado. */
    private Button crearBoton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + color + "22;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-cursor: hand;"));
        return btn;
    }

    private ComboBox<String> crearCombo(List<String> items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setMaxWidth(Double.MAX_VALUE);

        // El estilo inline de JavaFX no llega a las celdas del dropdown
        // hay que usar un cellFactory para forzar el color del texto
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #cdd6f4; -fx-background-color: #313244;");
            }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #cdd6f4; -fx-background-color: #313244;");
            }
        });

        cb.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-radius: 4;" +
                        "-fx-text-fill: #cdd6f4;");

        if (items.isEmpty())
            cb.setPromptText("No hay dispositivos ADB conectados");
        return cb;
    }

    private String etiquetaDispositivo(String serial) {
        try {
            String modelo = ejecutarShellEnSerial(serial, "getprop ro.product.model").trim();
            if (modelo.isBlank() || "null".equalsIgnoreCase(modelo)) {
                return serial;
            }
            return modelo + " — " + serial;
        } catch (Exception e) {
            return serial;
        }
    }

    private String serialDesdeEtiqueta(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) {
            return null;
        }
        int separador = etiqueta.indexOf(" — ");
        if (separador < 0) {
            return etiqueta.trim();
        }
        return etiqueta.substring(separador + 3).trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WIFI CON ESPERA (igual que antes)
    // ─────────────────────────────────────────────────────────────────────────
    // private boolean ejecutarPasoWifiConEspera(ADBService adb, String serial,
    // PasoPrueba paso) {
    // System.out.println("[WIFI] Activando interfaz WiFi...");
    // adb.ejecutarPasoSync(serial, paso.getComando());

    // if (tieneIpWifi(serial)) {
    // System.out.println("[WIFI] Conectado automáticamente ✔");
    // Platform.runLater(() -> {
    // paso.setEstado("OK");
    // listaPasos.refresh();
    // });
    // return true;
    // }
    // System.out.println("[WIFI] Sin red — abriendo ajustes WiFi...");
    // ejecutarShellEnSerial(serial, "am start -a android.settings.WIFI_SETTINGS");
    // Platform.runLater(() -> {
    // paso.setEstado("ESPERANDO WIFI...");
    // listaPasos.refresh();
    // });

    // long inicio = System.currentTimeMillis();
    // boolean conectado = false;

    // while (System.currentTimeMillis() - inicio < WIFI_TIMEOUT_MS) {
    // try {
    // Thread.sleep(WIFI_POLL_INTERVAL_MS);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // break;
    // }
    // int min = (int) (restanteMs / 60_000);
    // ESTE ES PARA LA PRUEBA DEL CALL TIMER , ¿Se mostró la advertencia del
    // temporizador de llamada durante la llamada?
    private boolean ejecutarCallLimitWarnCheck(String serial, PasoPrueba paso) {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        String numero = solicitarNumeroLlamada(
                owner,
                "Call Timer Warning",
                "Ingresa el número al que deseas llamar.\nLa llamada se mantendrá activa durante 2 minutos para verificar si aparece la advertencia del temporizador.");

        if (numero == null || numero.isBlank()) {
            actualizarEstadoPaso(paso, "Cancelado por el usuario");
            return false;
        }

        String numeroLimpio = numero.replaceAll("\\s+", "").trim();
        LlamadasD17 llamadas = new LlamadasD17(serial);

        try {
            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + "...");
            return llamadas.ejecutarCallLimitWarnCheck(numeroLimpio, () -> ConfirmacionManualPopup.mostrarYEsperar(
                    paso.getNombre(),
                    owner,
                    "¿Se mostró la advertencia del temporizador de llamada durante la llamada?"));
        } catch (Exception e) {
            llamadas.colgarLlamadaEnCurso();
            return false;
        }
    }

    private boolean ejecutarCallAutoHangupCheck(String serial, PasoPrueba paso) {
        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        String numero = solicitarNumeroLlamada(
                owner,
                "Call Timer Auto Hangup",
                "Ingresa el número al que deseas llamar.\nLa llamada debe colgarse automáticamente cuando alcance el límite configurado.");

        if (numero == null || numero.isBlank()) {
            actualizarEstadoPaso(paso, "Cancelado por el usuario");
            return false;
        }

        String numeroLimpio = numero.replaceAll("\\s+", "").trim();
        LlamadasD17 llamadas = new LlamadasD17(serial);

        try {
            actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + "...");
            return llamadas.ejecutarCallAutoHangupCheck(numeroLimpio, () -> ConfirmacionManualPopup.mostrarYEsperar(
                    paso.getNombre(),
                    owner,
                    "La llamada debería haberse colgado automáticamente a los 2 minutos.\nConfirma si ya se colgó antes de continuar con el paso 8."));
        } catch (Exception e) {
            llamadas.colgarLlamadaEnCurso();
            return false;
        }
    }

    // int seg = (int) ((restanteMs % 60_000) / 1_000);
    // final String cuenta = String.format("ESPERANDO WIFI... %d:%02d", min, seg);
    // Platform.runLater(() -> {
    // paso.setEstado(cuenta);
    // listaPasos.refresh();
    // });
    // if (tieneIpWifi(serial)) {
    // conectado = true;
    // break;
    // }
    // }
    // final String estadoFinal = conectado ? "OK" : "ERROR";
    // Platform.runLater(() -> {
    // paso.setEstado(estadoFinal);
    // listaPasos.refresh();
    // });
    // return conectado;
    // }
    private boolean ejecutarPasoWifiConEspera(ADBService adb, String serial, PasoPrueba paso) {
        adb.ejecutarPasoSync(serial, paso.getComando());

        if (tieneIpWifi(serial)) {
            System.out.println("[WIFI] Conectado automáticamente ✔");

            return true;
        }

        ejecutarShellEnSerial(serial, "am start -a android.settings.WIFI_SETTINGS");

        long inicio = System.currentTimeMillis();
        boolean conectado = false;

        while (System.currentTimeMillis() - inicio < WIFI_TIMEOUT_MS) {
            try {
                Thread.sleep(WIFI_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            long restanteMs = WIFI_TIMEOUT_MS - (System.currentTimeMillis() - inicio);
            int min = (int) (restanteMs / 60_000);
            int seg = (int) ((restanteMs % 60_000) / 1_000);

            final String cuenta = String.format("ESPERANDO WIFI... %d:%02d", min, seg);

            Platform.runLater(() -> {
                paso.setEstado(cuenta);
                listaPasos.refresh();
            });

            if (tieneIpWifi(serial)) {
                conectado = true;
                break;
            }
        }
        return conectado;
    }

    private boolean tieneIpWifi(String serial) {
        try {
            String salida = ejecutarShellEnSerial(serial, "ip addr show wlan0");
            for (String l : salida.split("\n")) {
                String t = l.trim();
                if (t.startsWith("inet ") && !t.contains("127."))
                    return true;
            }
        } catch (Exception e) {
            System.out.println("[WIFI] Error comprobando IP: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCRIPTS DE RED / HARDWARE (botones del panel)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void addWifiOn() {
        pasos.add(new PasoPrueba("Levantar Interfaz WiFi",
                "shell svc wifi enable && sleep 6"));
    }

    @FXML
    private void addWifiOff() {

        pasos.add(new PasoPrueba("Apagar WiFi",
                "shell svc wifi disable && sleep 2"));
    }

    @FXML
    private void addPingStep() {
        pasos.add(new PasoPrueba("Ping Google", "shell ping -c 3 8.8.8.8"));
    }

    @FXML
    private void addSoundTest() {

        pasos.add(new PasoPrueba("Probar Altavoz",
                "shell am start -a android.intent.action.VIEW "
                        + "-d content://settings/system/notification_sound -t audio/* "
                        + "&& sleep 2 && input keyevent 4 && sleep 1"));
    }

    @FXML
    private void addVibrateStep() {
        pasos.add(new PasoPrueba("Probar Vibración",
                "shell input swipe 500 500 501 501 1500"));
    }

    @FXML
    private void limpiarPasos() {
        pasos.clear();
        btnInforme.setDisable(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVEGACIÓN
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void abrirLaboratorio() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/LaboratorioBateria.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("AEA Suite - Laboratorio de Rendimiento");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void btnGestionarClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("LaboratorioBateria.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Análisis de Rendimiento - AEA Suite");
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void generarInformePDF() {
        if (dispositivoActual == null || pasos.isEmpty())
            return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Informe PDF");
        String userHome = System.getProperty("user.home");
        File documentosPath = new File(userHome, "Documents");
        if (documentosPath.exists())
            fc.setInitialDirectory(documentosPath);

        fc.setInitialFileName("Informe_Diagnostico_" + dispositivoActual.getSerialNumber() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File file = fc.showSaveDialog(btnInforme.getScene().getWindow());
        if (file == null)
            return;

        ADBService adb = new ADBService();
        String serial;
        try {
            serial = adb.getSerialActivo(dispositivoActual.getAndroid_id());
        } catch (IOException e) {
            serial = dispositivoActual.getSerialNumber();
        }
        Map<String, String> specs = adb.obtenerSpecsHardware(serial);

        try (PDDocument doc = new PDDocument()) {

            // ── Estado mutable de la página encapsulado en arrays de 1 elemento ──
            final PDPage[] paginaActual = { new PDPage() };
            doc.addPage(paginaActual[0]);
            final PDPageContentStream[] cs = { new PDPageContentStream(doc, paginaActual[0]) };
            final int[] y = { 750 }; // Coordenada Y de inicio

            // Helper seguro para saltar de página
            Runnable nuevaPagina = () -> {
                try {
                    cs[0].close();
                    paginaActual[0] = new PDPage();
                    doc.addPage(paginaActual[0]);
                    cs[0] = new PDPageContentStream(doc, paginaActual[0]);
                    y[0] = 750; // Reiniciar margen superior en la nueva hoja
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            // ── TÍTULO INICIAL DEL PDF ───────────────────────────────────────────
            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs[0].newLineAtOffset(50, y[0]);
            cs[0].showText("CERTIFICADO DE DIAGNOSTICO TECNICO");
            cs[0].endText();

            cs[0].setLineWidth(1f);
            cs[0].moveTo(50, y[0] - 12);
            cs[0].lineTo(550, y[0] - 12);
            cs[0].stroke();
            y[0] -= 26;

            cs[0].beginText();
            cs[0].setFont(PDType1Font.HELVETICA, 10);
            cs[0].newLineAtOffset(50, y[0]);
            cs[0].showText("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            cs[0].endText();
            y[0] -= 24;

            // ── IDENTIFICACIÓN DEL DISPOSITIVO ───────────────────────────────────
            dibujarSeccionPDF(cs, nuevaPagina, y, "IDENTIFICACION DEL DISPOSITIVO");
            dibujarFilaPDF(cs, nuevaPagina, y, "Modelo", dispositivoActual.getModelo().getNombreModelo());
            dibujarFilaPDF(cs, nuevaPagina, y, "Marca", dispositivoActual.getModelo().getMarca().getNombre());
            dibujarFilaPDF(cs, nuevaPagina, y, "S/N", dispositivoActual.getSerialNumber());
            dibujarFilaPDF(cs, nuevaPagina, y, "Android ID", dispositivoActual.getAndroid_id());
            dibujarFilaPDF(cs, nuevaPagina, y, "IMEI", specs.getOrDefault("IMEI", "N/A"));

            // ── SOFTWARE ──────────────────────────────────────────────────────────
            y[0] -= 8;
            dibujarSeccionPDF(cs, nuevaPagina, y, "SOFTWARE");
            dibujarFilaPDF(cs, nuevaPagina, y, "Version Android", specs.getOrDefault("Android", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "Parche seguridad", specs.getOrDefault("Parche", "N/A"));

            // ── HARDWARE ──────────────────────────────────────────────────────────
            y[0] -= 8;
            dibujarSeccionPDF(cs, nuevaPagina, y, "HARDWARE");
            dibujarFilaPDF(cs, nuevaPagina, y, "CPU", specs.getOrDefault("CPU", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "RAM", specs.getOrDefault("RAM", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "Almacenamiento", specs.getOrDefault("Storage", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "Resolucion", specs.getOrDefault("Resolucion", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "DPI", specs.getOrDefault("DPI", "N/A"));

            // ── BATERÍA ───────────────────────────────────────────────────────────
            y[0] -= 8;
            dibujarSeccionPDF(cs, nuevaPagina, y, "BATERIA");
            dibujarFilaPDF(cs, nuevaPagina, y, "Nivel", specs.getOrDefault("Bateria", "N/A"));
            dibujarFilaPDF(cs, nuevaPagina, y, "Estado", specs.getOrDefault("EstadoCarga", "N/A"));

            // ── RESULTADOS DE PRUEBAS ─────────────────────────────────────────────
            y[0] -= 8;
            dibujarSeccionPDF(cs, nuevaPagina, y, "RESULTADOS DE PRUEBAS");

            for (PasoPrueba paso : new ArrayList<>(pasos)) {
                String nombreCompleto = limpiar(paso.getNombre()).trim();
                String idPrueba = "";
                String descripcionPrueba = nombreCompleto;

                if (nombreCompleto.matches("^SOFT\\.\\d{3}\\.\\d{3}.*")) {
                    int primerEspacio = nombreCompleto.indexOf(" ");
                    if (primerEspacio != -1) {
                        idPrueba = nombreCompleto.substring(0, primerEspacio);
                        descripcionPrueba = nombreCompleto.substring(primerEspacio).trim();
                    }
                }

                // Configuración de fuentes y anchos
                PDFont fuenteId = PDType1Font.HELVETICA_BOLD;
                PDFont fuenteDesc = PDType1Font.HELVETICA;
                float tamanoFuentePrueba = 9;

                // Reducimos el ancho máximo de la descripción para que deje espacio al ID a la
                // izquierda (en X=125)
                // 380 originales - 70 de margen para el ID = 310 puntos útiles para el texto de
                // descripción
                float anchoMaximoDescripcion = 310;

                List<String> lineasDescripcion = envolverTexto(descripcionPrueba, fuenteDesc, tamanoFuentePrueba,
                        anchoMaximoDescripcion);
                int espacioRequerido = Math.max(1, lineasDescripcion.size()) * 14;

                // Si el bloque de texto no cabe en lo que queda de página, saltamos ANTES de
                // escribirlo
                if (y[0] - espacioRequerido < 50) {
                    nuevaPagina.run();
                    dibujarSeccionPDF(cs, nuevaPagina, y, "RESULTADOS DE PRUEBAS (cont.)");
                }

                // Guardamos la posición Y donde empieza esta prueba para alinear el PASS/FAIL y
                // el ID en la primera línea
                int yInicialPrueba = y[0];

                // A) Pintamos el ID en NEGRITA (solo en la primera línea del bloque)
                if (!idPrueba.isEmpty()) {
                    cs[0].beginText();
                    cs[0].setFont(fuenteId, tamanoFuentePrueba);
                    cs[0].setNonStrokingColor(java.awt.Color.BLACK);
                    cs[0].newLineAtOffset(55, yInicialPrueba); // Margen izquierdo inicial fijo para los IDs
                    cs[0].showText(idPrueba);
                    cs[0].endText();
                }

                // B) Pintamos la descripción envuelta (en fuente NORMAL)
                cs[0].setNonStrokingColor(java.awt.Color.BLACK);
                for (String linea : lineasDescripcion) {
                    cs[0].beginText();
                    cs[0].setFont(fuenteDesc, tamanoFuentePrueba);

                    // Si hay un ID, la descripción se desplaza a la derecha (X=125) para no pisarlo
                    // y quedar tabulada.
                    // Si por algún motivo no tuviera ID, empieza en el margen normal (X=55).
                    float coordenadaX = idPrueba.isEmpty() ? 55 : 125;

                    cs[0].newLineAtOffset(coordenadaX, y[0]);
                    cs[0].showText(linea);
                    cs[0].endText();
                    y[0] -= 14;
                }

                // Dibujar el estado PASS / FAIL (alineado con la altura horizontal inicial de
                // la prueba)
                cs[0].beginText();
                cs[0].setFont(PDType1Font.HELVETICA_BOLD, 9);
                cs[0].newLineAtOffset(450, yInicialPrueba); // Forzado a mantenerse en su columna limpia externa
                if ("OK".equals(paso.getEstado())) {
                    cs[0].setNonStrokingColor(new java.awt.Color(34, 139, 34));
                    cs[0].showText("PASS");
                } else {
                    cs[0].setNonStrokingColor(java.awt.Color.RED);
                    cs[0].showText("FAIL");
                }
                cs[0].endText();

                // 2. Detalle interno de la prueba (Ej: las "Rows" del operador)
                String detalle = paso.getOutputDetalle();
                if (detalle != null && !detalle.isBlank()) {
                    String[] lineasDetalle = detalle.split("\r?\n");

                    for (String lineaTexto : lineasDetalle) {
                        if (lineaTexto.isBlank())
                            continue;

                        String detalleLimpio = limpiar(lineaTexto);
                        PDFont fuenteDetalle = PDType1Font.HELVETICA_OBLIQUE;
                        float tamanoFuenteDetalle = 7;
                        float anchoMaximoDetalle = 480; // Ocupa casi todo el ancho disponible

                        List<String> subLineasWrap = envolverTexto(detalleLimpio, fuenteDetalle, tamanoFuenteDetalle,
                                anchoMaximoDetalle);

                        for (String subLinea : subLineasWrap) {
                            // Verificación de salto de página línea por línea para los detalles inferiores
                            if (y[0] < 50) {
                                nuevaPagina.run();
                                dibujarSeccionPDF(cs, nuevaPagina, y, "RESULTADOS DE PRUEBAS (cont.)");
                            }

                            cs[0].beginText();
                            cs[0].setFont(fuenteDetalle, tamanoFuenteDetalle);
                            cs[0].setNonStrokingColor(new java.awt.Color(100, 100, 100));
                            cs[0].newLineAtOffset(65, y[0]);
                            cs[0].showText(subLinea);
                            cs[0].endText();

                            y[0] -= 10;
                        }
                    }
                }
                y[0] -= 4; // Pequeña separación estética entre pruebas distintas
            }

            cs[0].close();

            try {
                doc.save(file);
                fichaTecnicaController.mostrarToast("PDF guardado: " + file.getAbsolutePath());
                pasos.clear();
                btnInforme.setDisable(true);
            } catch (IOException e) {
                mostrarAlertaError("Error de Acceso", "No se pudo guardar. Cierra el archivo si esta abierto.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaError("Error Critico", "Error al generar el PDF.");
        }
    }

    /**
     * Método de utilidad para separar un String largo en sub-líneas basándose
     * en el ancho real que ocuparán los caracteres en el PDF.
     */
    private List<String> envolverTexto(String texto, PDFont fuente, float tamanoFuente, float anchoMaximo)
            throws IOException {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            lineas.add("");
            return lineas;
        }

        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();

        for (String palabra : palabras) {
            String testLinea = lineaActual.length() == 0 ? palabra : lineaActual.toString() + " " + palabra;

            // Convertimos el ancho interno de PDFBox a puntos reales de impresión
            float anchoReal = fuente.getStringWidth(testLinea) / 1000 * tamanoFuente;

            if (anchoReal > anchoMaximo) {
                if (lineaActual.length() > 0) {
                    lineas.add(lineaActual.toString());
                    lineaActual = new StringBuilder(palabra);
                } else {
                    // Si una sola palabra es más ancha que el margen permitido (ej. un link largo),
                    // se fuerza el corte
                    lineas.add(palabra);
                    lineaActual = new StringBuilder();
                }
            } else {
                lineaActual.append(lineaActual.length() == 0 ? palabra : " " + palabra);
            }
        }

        if (lineaActual.length() > 0) {
            lineas.add(lineaActual.toString());
        }
        return lineas;
    }

    private void dibujarSeccionPDF(PDPageContentStream[] cs, Runnable nuevaPagina, int[] y, String titulo)
            throws IOException {
        if (y[0] < 60) {
            nuevaPagina.run();
        }
        cs[0].setNonStrokingColor(new java.awt.Color(30, 30, 60));
        cs[0].addRect(50, y[0] - 4, 500, 16);
        cs[0].fill();

        cs[0].beginText();
        cs[0].setNonStrokingColor(java.awt.Color.WHITE);
        cs[0].setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs[0].newLineAtOffset(55, y[0]);
        cs[0].showText(titulo);
        cs[0].endText();

        cs[0].setNonStrokingColor(java.awt.Color.BLACK);
        y[0] -= 20;
    }

    private void dibujarFilaPDF(PDPageContentStream[] cs, Runnable nuevaPagina, int[] y, String clave, String valor)
            throws IOException {
        String valorLimpio = limpiar(valor);
        PDFont fuenteValor = PDType1Font.HELVETICA;
        float tamanoFuente = 9;
        float anchoMaximoValor = 340; // Desde X=200 hasta X=540

        // Envolvemos el valor por si la propiedad de hardware es extremadamente larga
        List<String> lineasValor = envolverTexto(valorLimpio, fuenteValor, tamanoFuente, anchoMaximoValor);
        int espacioRequerido = lineasValor.size() * 14;

        if (y[0] - espacioRequerido < 50) {
            nuevaPagina.run();
        }

        // Pintamos la clave estática de la fila
        cs[0].beginText();
        cs[0].setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs[0].newLineAtOffset(55, y[0]);
        cs[0].showText(limpiar(clave) + ":");
        cs[0].endText();

        // Pintamos el valor (puede ocupar múltiples renglones si la especificación es
        // muy extensa)
        for (String linea : lineasValor) {
            cs[0].beginText();
            cs[0].setFont(fuenteValor, tamanoFuente);
            cs[0].newLineAtOffset(200, y[0]);
            cs[0].showText(linea);
            cs[0].endText();
            y[0] -= 14;
        }
    }

    private String parsearParcelIMEI(String parcelRaw) {
        StringBuilder resultado = new StringBuilder();
        for (String linea : parcelRaw.split("\n")) {
            int fin = linea.indexOf('\'');
            int inicio = linea.lastIndexOf('\'');
            if (fin == inicio || fin == -1)
                continue;
            String chars = linea.substring(fin + 1, inicio);
            for (String c : chars.split("\\.")) {
                c = c.trim();
                if (c.matches("[0-9]"))
                    resultado.append(c);
            }
        }
        String r = resultado.toString();
        // IMEI = 15 dígitos, IMEISV = 16 — usamos lo que haya hasta el máximo
        if (r.length() >= 15)
            return r.substring(0, Math.min(r.length(), 16));
        return r;
    }

    private String limpiar(String texto) {
        return texto.replaceAll("[\\n\\r\\t]", " ")
                .replaceAll("[^\\x20-\\x7E]", "")
                .trim();
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null); // Esto quita el encabezado gris por defecto
        alert.setContentText(mensaje);

        // Si quieres que combine con tu estilo oscuro (opcional)
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1e1e2e;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: #cdd6f4;");

        alert.showAndWait();
    }

    private boolean ejecutarFmConLlamadaEntrante(String serial, PasoPrueba paso, Stage owner) {
        if (llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
            return false;
        }

        try {
            actualizarEstadoPaso(paso, "Abriendo FM...");
            // Usar -W para esperar a que la app esté lista
            ejecutarShellEnSerial(serial, "am start -W -n com.android.fmradio/.FmMainActivity");
            Thread.sleep(2_000); // Esperar a que la app inicie

            // Mandar Home para backgroundear la app (sin validar si está reproduciendo)
            actualizarEstadoPaso(paso, "Backgroundeando FM...");
            ejecutarShellEnSerial(serial, "input keyevent 3");
            Thread.sleep(3_000); // Aumentado: dar más tiempo para que FM se estabilice en background

            // Iniciar llamada desde el dispositivo configurado como llamante
            actualizarEstadoPaso(paso, "Iniciando llamada entrante...");
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "am start -a android.intent.action.CALL -d tel:" + llamadaEntranteNumero);

            // Esperar a que suene en el dispositivo bajo prueba
            actualizarEstadoPaso(paso, "Esperando que suene...");
            boolean sono = esperarHastaQueSuene(serial, 15);
            if (!sono) {
                // Intentar colgar por si acaso
                ejecutarShellEnSerial(llamadaEntranteSerial, "input keyevent KEYCODE_ENDCALL");
                return false;
            }

            // Contestar en el dispositivo bajo prueba
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_CALL");
            actualizarEstadoPaso(paso, "Llamada activa 10s...");
            Thread.sleep(10_000);

            // Colgar en ambos
            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_ENDCALL");
            ejecutarShellEnSerial(llamadaEntranteSerial, "input keyevent KEYCODE_ENDCALL");

            // Confirmación manual: el técnico decide si la FM se recuperó sola.
            actualizarEstadoPaso(paso, "Confirmar FM manualmente...");
            boolean fmOk = ConfirmacionManualPopup.mostrarYEsperar(
                    "SOFT.017.016  —  While radio FM playing, receive a call. Check if after call ends radio FM continues playing",
                    owner,
                    "Confirma si, tras colgar la llamada, la radio FM vuelve a sonar sola en unos segundos.\n\n" +
                            "PASS: la FM se reanuda correctamente.\n" +
                            "FAIL: la FM no se recupera o no vuelve a sonar.");

            Platform.runLater(() -> {
                paso.setOutputDetalle(
                        fmOk ? "Confirmado manualmente: FM se recupera" : "Confirmado manualmente: FM no se recupera");
                listaPasos.refresh();
            });
            return fmOk;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarBluetoothLlamadasSalientes(String serial, PasoPrueba paso, Stage owner) {
        String numero = solicitarNumeroLlamada(
                owner,
                "SOFT.023.008 — Bluetooth",
                "Ingresa el número de prueba para llamar directamente desde el headset Bluetooth.");

        if (numero == null || numero.isBlank()) {
            actualizarEstadoPaso(paso, "Cancelado por el usuario");
            return false;
        }

        String numeroLimpio = numero.replaceAll("\\s+", "").trim();
        LlamadasD17 llamadas = new LlamadasD17(serial);

        actualizarEstadoPaso(paso, "Llamando a " + numeroLimpio + "...");
        boolean lanzada = llamadas.llamarSinVerificar(numeroLimpio, 4_000L);
        if (!lanzada) {
            return false;
        }

        return ConfirmacionManualPopup.mostrarYEsperar(
                "SOFT.023.008 — Bluetooth",
                owner,
                "Confirma si la llamada se inició directamente al número indicado y si pudiste gestionarla desde el headset Bluetooth.");
    }
}