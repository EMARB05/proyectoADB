package com.example.View;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

import com.example.Controller.ADBService;
import com.example.Controller.PerfilesManager;
import com.example.Model.BloquePrueba;
import com.example.Model.Dispositivo;
import com.example.Model.LlamadasD17;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;

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

import org.apache.commons.collections4.functors.IfClosure;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class DiagnosticoController extends com.example.Model.AdbCallSupport implements DispositivoAware {

    @FXML
    private ListView<PasoPrueba> listaPasos;
    @FXML
    private FichaTecnicaController fichaTecnicaController;

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

    private final ObservableList<PasoPrueba> pasos = FXCollections.observableArrayList();
    private Dispositivo dispositivoActual;
    // ─── WiFi timeout ─────────────────────────────────────────────────────────
    private static final int WIFI_TIMEOUT_MS = 3 * 60 * 1000;
    private static final int WIFI_POLL_INTERVAL_MS = 3_000;

    // ─── Variables para la configuración del bloque Contactos
    private String contactoTestNombre = "Test_ADB";
    private String contactoTestTelefonoDUT = "";
    private String contactoTestTelefono = "";
    private String contactoTestEmail = "test@adb.com";
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

    private static final String CONTACT_CREATE_SIM = "__CREATE_SIM__";
    private static final String CONTACT_EDIT_SIM = "__EDIT_SIM__";
    private static final String CONTACT_DELETE_SIM = "__DELETE_SIM__";
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

    // ─── Datos extra para los pasos de llamada avanzados ─────────────────────
    // Se rellenan cuando el usuario configura el paso en el popup.
    private String llamadaMasivaNumero = null; // número destino para llamada masiva
    private String llamadaEntreDosSerial1 = null; // serial del teléfono 1
    private String llamadaEntreDosSerial2 = null; // serial del teléfono 2
    private String llamadaEntranteSerial = null;
    private String llamadaEntranteNumero = null;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        listaPasos.setItems(pasos);

        pasos.add(new PasoPrueba("Preparando Dispositivo...",
                "shell input keyevent KEYCODE_WAKEUP && wm dismiss-keyguard"));
        pasos.add(new PasoPrueba("Levantar Interfaz WiFi",
                "shell svc wifi enable && sleep 6"));
        pasos.add(new PasoPrueba("Check Conectividad (Ping)",
                "shell ping -c 4 8.8.8.8"));
        pasos.add(new PasoPrueba("Obtener IP Local",
                "shell ip addr show wlan0"));

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
                new Thread(() -> {
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

        new Thread(() -> {
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
                ejecutarShellEnSerial(serial, "input tap " + prev.getXTeclado() + " " + prev.getYTeclado());
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

                ejecutarShellEnSerial(serial, "input keyevent KEYCODE_BACK");
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
                List<String> seriales = obtenerSerialesADB();
                List<String> opciones = seriales.stream()
                        .map(this::etiquetaDispositivo)
                        .toList();
                Label l1 = crearLabelConfig("Teléfono 1 (llama primero):");
                ComboBox<String> cb1 = crearCombo(opciones);
                Label l2 = crearLabelConfig("Número del Teléfono 1:");
                TextField tf1 = crearTextField("+34612345678");
                Label l3 = crearLabelConfig("Teléfono 2 (recibe primero):");
                ComboBox<String> cb2 = crearCombo(opciones);
                Label l4 = crearLabelConfig("Número del Teléfono 2:");
                TextField tf2 = crearTextField("+34698765432");
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Label ayuda = new Label("Selecciona el modelo del equipo. A la derecha verás su serial o IP.");
                ayuda.setTextFill(Color.web("#6c7086"));
                ayuda.setFont(Font.font(10));
                ayuda.setWrapText(true);

                String serialActual = obtenerSerialADBActual();
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

                Button btn = crearBoton("➕  Añadir al script", "#a6e3a1");
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
                panel.getChildren().addAll(l1, cb1, l2, tf1, l3, cb2, l4, tf2, aviso, btn);
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
                List<String> seriales = obtenerSerialesADB();
                Label l1 = crearLabelConfig("Dispositivo que llama:");
                ComboBox<String> cbLlamante = crearCombo(seriales);
                Label l2 = crearLabelConfig("Número del dispositivo que recibe:");
                TextField tfNumero = crearTextField("+34612345678");
                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));

                String serialActual = obtenerSerialADBActual();
                seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                        .ifPresent(s -> cbLlamante.getSelectionModel().select(s));

                Button btn = crearBoton("➕  Añadir al script", "#94e2d5");
                btn.setOnAction(e -> {
                    String llamante = cbLlamante.getValue();
                    String numero = tfNumero.getText().trim();
                    if (llamante == null) {
                        aviso.setText("Selecciona el dispositivo que llama.");
                        return;
                    }
                    if (numero.isBlank()) {
                        aviso.setText("Introduce el número del receptor.");
                        return;
                    }
                    // Guardamos el serial del que llama y el número en el nombre del paso
                    llamadaEntranteSerial = llamante;
                    llamadaEntranteNumero = numero;
                    pasos.add(new PasoPrueba("Llamada Entrante desde " + llamante, CMD_LLAMADA_ENTRANTE));
                    popup.close();
                });
                panel.getChildren().addAll(l1, cbLlamante, l2, tfNumero, aviso, btn);
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

                List<String> seriales = obtenerSerialesADB();
                List<String> opciones = seriales.stream()
                        .map(this::etiquetaDispositivo)
                        .toList();
                Label lReceptor = crearLabelConfig("Dispositivo que debe contestar:");
                ComboBox<String> cbReceptor = crearCombo(opciones);
                if (!opciones.isEmpty()) {
                    String serialActual = obtenerSerialADBActual();
                    seriales.stream().filter(s -> !s.equals(serialActual)).findFirst()
                            .ifPresent(s -> cbReceptor.getSelectionModel().select(etiquetaDispositivo(s)));
                    if (cbReceptor.getSelectionModel().isEmpty())
                        cbReceptor.getSelectionModel().select(0);
                }

                Label aviso = new Label();
                aviso.setTextFill(Color.web("#f38ba8"));
                aviso.setFont(Font.font(11));
                Button btn = crearBoton("➕  Añadir al script", "#b4befe");
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
                panel.getChildren().addAll(info, tf, lReceptor, cbReceptor, aviso, btn);
            }
        }
        javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
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

        new Thread(() -> {
            ADBService adb = new ADBService();

            String serialActivo;
            try {
                serialActivo = adb.getSerialActivo(dispositivoActual.getAndroid_id());
            } catch (IOException e) {
                serialActivo = dispositivoActual.getSerialNumber();
                System.out.println("[DIAG] Fallback serial: " + serialActivo);
            }
            final String serial = serialActivo;

            for (PasoPrueba paso : pasos) {
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

                if (ref.isManual()) {
                    if (!ref.getComando().isBlank()) {
                        adb.ejecutarAccionHilo(serial, ref.getComando());
                    }
                    Stage owner = (Stage) btnEjecutar.getScene().getWindow();
                    if (paso.getNombre().contains("IMEISV")) {
                        String valor = ConfirmacionManualPopup.mostrarYEsperarConValor(ref.getNombre(), owner);
                        ok = !valor.isEmpty();
                        outputDetalle = ok ? valor : "";
                    } else {
                        ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
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
                }

                else if (TOUCH_PINCH.equals(paso.getComando())) {
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
                    Stage owner = (Stage) btnEjecutar.getScene().getWindow();
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
                    Stage owner = (Stage) btnEjecutar.getScene().getWindow();
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
                    Stage owner = (Stage) btnEjecutar.getScene().getWindow();
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
                } else if (DISPLAY_DISPLAY_SIZE.equals(ref.getComando())) {
                    ok = cambiarYRestaurarDisplaySize(serial, adb);
                } else if (DISPLAY_SCREENSAVER.equals(ref.getComando())) {
                    ok = comprobarScreensaver(serial, adb);
                    adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_WAKEUP");

                } else if (CONTACT_CREATE_SIM.equals(ref.getComando())) {
                    ok = crearContactoSIM(serial, adb);

                } else if (CONTACT_EDIT_SIM.equals(ref.getComando())) {
                    ok = editarContactoSIM(serial, adb);

                } else if (CONTACT_DELETE_SIM.equals(ref.getComando())) {
                    ok = borrarContactoSIM(serial, adb);

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

                } else if (CONTACT_COPY_PHONE_SIM.equals(ref.getComando())) {

                } else if (CONTACT_IMPORT_VCARD.equals(ref.getComando())) {
                    ok = importarVCard(serial, adb);
                } else if (CONTACT_EXPORT_VCARD.equals(ref.getComando())) {
                    ok = exportarVCard(serial, adb);

                } else if (CONTACT_MEMORY_STATUS.equals(ref.getComando())) {
                    ok = comprobarMemoriaContactos(serial, adb);

                } else {
                    ADBService.EjecucionADB r = adb.ejecutarYObtener(serial, ref.getComandos(), ref.isSinOutput());
                    ok = r.exito();
                    outputDetalle = r.outputJunto();
                    boolean exito = adb.ejecutarPasoSync(serial, paso.getComando());

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

            Platform.runLater(() -> {
                btnInforme.setDisable(false);
                btnEjecutar.setDisable(false);
                btnLimpiar.setDisable(false);
                System.out.println("[DIAG] Secuencia completada.");
            });
        }).start();
    }

    private boolean ejecutarEmergencia(String serial, PasoPrueba paso) {
        try {
            System.out.println("[EMERGENCIA] Marcando 112...");
            actualizarEstadoPaso(paso, "Marcando 112...");
            ejecutarShellEnSerial(serial,
                    "am start -a android.intent.action.CALL -d tel:112");

            // Espera 3s — suficiente para ver que marca pero sin que contesten
            Thread.sleep(3_000);

            ejecutarShellEnSerial(serial, "input keyevent KEYCODE_ENDCALL");
            System.out.println("[EMERGENCIA] ✔ Llamada colgada antes de contestar");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
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

            if (!perfil.tieneComandos()) {
                // Dispositivo táctil — abrir teclado y enviar números por TAPs
                if (perfil.getXTeclado() <= 0) {
                    System.out.println("[DTMF] ⚠ No se encontró botón Teclado");
                    return false;
                }

                System.out.println("[DTMF] Abriendo teclado...");
                ejecutarShellEnSerial(serial, "input tap " + perfil.getXTeclado() + " " + perfil.getYTeclado());
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
                    System.out.println("[DTMF] Usando coordenadas detectadas en tiempo real");
                } else if (perfil.getCoordNumeros() != null && perfil.getCoordNumeros().size() >= 10) {
                    usoCoordsManual = true;
                    coordsRun = perfil.getCoordNumeros();
                    System.out.println("[DTMF] Usando coordenadas guardadas de perfil");
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

                        System.out.println("[DTMF] TAP manual " + numero + " -> " + c[0] + "," + c[1]);
                        ejecutarShellEnSerial(serial, "input tap " + c[0] + " " + c[1]);
                        Thread.sleep(600);
                    }
                }

                if (!usoCoordsManual) {
                    String wmSize = ejecutarShellEnSerial(serial, "wm size");
                    int screenWidth = 1080;
                    int screenHeight = 2400;
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)x(\\d+)");
                    java.util.regex.Matcher m = p.matcher(wmSize);
                    if (m.find()) {
                        screenWidth = Integer.parseInt(m.group(1));
                        screenHeight = Integer.parseInt(m.group(2));
                    }

                    int[] coordX = {
                            (int) Math.round(screenWidth * 0.22),
                            (int) Math.round(screenWidth * 0.50),
                            (int) Math.round(screenWidth * 0.78)
                    };
                    int tecladoTop = (int) Math.round(screenHeight * 0.58);
                    int tecladoBottom = (int) Math.round(screenHeight * 0.96);
                    int filaAlto = Math.max(1, (tecladoBottom - tecladoTop) / 4);
                    int[] coordY = {
                            tecladoTop + filaAlto / 2,
                            tecladoTop + filaAlto + filaAlto / 2,
                            tecladoTop + 2 * filaAlto + filaAlto / 2,
                            tecladoTop + 3 * filaAlto + filaAlto / 2
                    };

                    int[] xPos = { 0, 1, 2, 0, 1, 2, 0, 1, 2, 1 };
                    int[] yPos = { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3 };

                    System.out.println("[DTMF] Usando fallback por rejilla");
                    for (int i = 0; i < numeros.length; i++) {
                        if (!llamadaActiva(serial)) {
                            System.out.println("[DTMF] Llamada terminada");
                            break;
                        }
                        int x = coordX[xPos[i]];
                        int y = coordY[yPos[i]];
                        System.out.println("[DTMF] TAP fallback " + numeros[i] + " -> " + x + "," + y);
                        ejecutarShellEnSerial(serial, "input tap " + x + " " + y);
                        Thread.sleep(600);
                    }
                }

                Thread.sleep(800);
                System.out.println("[DTMF] Cerrando teclado...");
                ejecutarShellEnSerial(serial, "input keyevent KEYCODE_BACK");
                Thread.sleep(500);
            } else {
                // Feature phone — usar keycodes directo
                int[] keycodes = { 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
                for (int i = 0; i < keycodes.length; i++) {
                    if (!llamadaActiva(serial)) {
                        System.out.println("[DTMF] Llamada terminada");
                        break;
                    }
                    System.out.println("[DTMF] Enviando KEYCODE_" + i + " (" + keycodes[i] + ")");
                    ejecutarShellEnSerial(serial, "input keyevent " + keycodes[i]);
                    Thread.sleep(600);
                }
            }

            System.out.println("[DTMF] ✔ Tonos enviados");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean ejecutarLlamadaEntrante(PasoPrueba paso) {
        if (llamadaEntranteSerial == null || llamadaEntranteNumero == null) {
            System.out.println("[ENTRANTE] Sin configurar");
            return false;
        }
        try {
            System.out.println("[ENTRANTE] " + llamadaEntranteSerial +
                    " llamando a " + llamadaEntranteNumero);
            actualizarEstadoPaso(paso, "Llamando...");

            // El dispositivo externo llama al receptor
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "am start -a android.intent.action.CALL -d tel:" + llamadaEntranteNumero);

            // Espera a que suene en el receptor
            actualizarEstadoPaso(paso, "Esperando que suene...");
            boolean sono = esperarHastaQueSuene(obtenerSerialADBActual(), 15);

            // Cuelga desde el que llamó — no contestamos
            Thread.sleep(4_000);
            ejecutarShellEnSerial(llamadaEntranteSerial,
                    "input keyevent KEYCODE_ENDCALL");

            System.out.println("[ENTRANTE] Resultado: " + (sono ? "SONÓ ✔" : "NO SONÓ ✖"));
            return sono;
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

        System.out.println("[RED] Red activa detectada: " + red);
        actualizarEstadoPaso(paso, "Red: " + red);

        // PASS si está en alguna red conocida
        return !red.equals("DESCONOCIDA");
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

        System.out.printf("[MASIVA] Llamando a %s desde %d dispositivos%n",
                llamadaMasivaNumero, seriales.size());

        // Lanzar llamada en todos en paralelo
        List<Thread> hilos = new ArrayList<>();
        for (String s : seriales) {
            Thread t = new Thread(() -> {
                ejecutarShellEnSerial(s,
                        "am start -a android.intent.action.CALL -d tel:" + llamadaMasivaNumero);
                System.out.println("[MASIVA] Llamada iniciada en: " + s);
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
        List<Thread> hilosCuelga = new ArrayList<>();
        for (String s : seriales) {
            Thread t = new Thread(() -> {
                ejecutarShellEnSerial(s, "input keyevent KEYCODE_ENDCALL");
                System.out.println("[MASIVA] Llamada colgada en: " + s);
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

        System.out.println("[MASIVA] ✔ Prueba completada");
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

        new Thread(() -> {
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

        System.out.printf("[ENTRE2] Iniciando prueba: %s (%s) ↔ %s (%s)%n",
                s1, numero1, s2, numero2);

        boolean ronda1Ok = false;
        boolean ronda2Ok = false;

        try {
            // ── RONDA 1: Tel.1 → Tel.2 ───────────────────────────────────────
            actualizarEstadoPaso(paso, "Ronda 1: Preparando pantallas...");
            despertarDispositivo(s1);
            despertarDispositivo(s2);
            Thread.sleep(1_500);

            System.out.println("[ENTRE2] Ronda 1: Tel.1 llama a Tel.2...");
            actualizarEstadoPaso(paso, "Ronda 1: Tel.1 → Tel.2");
            ejecutarShellEnSerial(s1, "am start -a android.intent.action.CALL -d tel:" + numero2);

            // Espera inteligente — contesta en cuanto suena, máximo 15s
            actualizarEstadoPaso(paso, "Ronda 1: Esperando que suene...");
            boolean sono1 = esperarHastaQueSuene(s2, 15);

            if (!sono1 || !llamadaActiva(s1)) {
                System.out.println("[ENTRE2] Ronda 1 FAIL — no sonó en Tel.2 o Tel.1 no estableció llamada");
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ronda1Ok = false;
            } else {
                despertarDispositivo(s2);
                Thread.sleep(500);
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_CALL");
                System.out.println("[ENTRE2] Tel.2 contestó");

                actualizarEstadoPaso(paso, "Ronda 1 activa 10s...");
                Thread.sleep(10_000);

                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                System.out.println("[ENTRE2] Ronda 1 finalizada ✔");
                ronda1Ok = true;
            }

            // Pausa entre rondas — siempre se ejecuta aunque Ronda 1 fallara
            actualizarEstadoPaso(paso, "Pausa entre rondas...");
            Thread.sleep(4_000);

            // ── RONDA 2: Tel.2 → Tel.1 ─── siempre se ejecuta ───────────────
            despertarDispositivo(s1);
            despertarDispositivo(s2);
            Thread.sleep(1_500);

            System.out.println("[ENTRE2] Ronda 2: Tel.2 llama a Tel.1...");
            actualizarEstadoPaso(paso, "Ronda 2: Tel.2 → Tel.1");
            ejecutarShellEnSerial(s2, "am start -a android.intent.action.CALL -d tel:" + numero1);

            // Espera inteligente — contesta en cuanto suena, máximo 15s
            actualizarEstadoPaso(paso, "Ronda 2: Esperando que suene...");
            boolean sono2 = esperarHastaQueSuene(s1, 15);

            if (!sono2 || !llamadaActiva(s2)) {
                System.out.println("[ENTRE2] Ronda 2 FAIL — no sonó en Tel.1 o Tel.2 no estableció llamada");
                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                ronda2Ok = false;
            } else {
                despertarDispositivo(s1);
                Thread.sleep(500);
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_CALL");
                System.out.println("[ENTRE2] Tel.1 contestó");

                actualizarEstadoPaso(paso, "Ronda 2 activa 10s...");
                Thread.sleep(10_000);

                ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
                ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
                System.out.println("[ENTRE2] Ronda 2 finalizada ✔");
                ronda2Ok = true;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ENTRE2] Test interrumpido");
        }

        boolean exito = ronda1Ok && ronda2Ok;
        System.out.printf("[ENTRE2] Resultado: Ronda1=%s | Ronda2=%s → %s%n",
                ronda1Ok ? "OK" : "FAIL",
                ronda2Ok ? "OK" : "FAIL",
                exito ? "PASS ✔" : "FAIL ✖");
        return exito;
    }

    // Pruebas de TOUCHSCREEN
    @FXML
    private void addTouchScreenTest() {
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
        System.out.println("[TOUCH] Centro calculado: " + centroX + "x" + centroY);

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

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar(
                "SOFT.004 — Touch Screen",
                bloqueTouchScreen,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
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

        Thread t1 = new Thread(() -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + (fcx - fm) + " " + (fcy - fm) + " " + fcx + " " + fcy + " 600"));
        Thread t2 = new Thread(() -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
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

        Thread t1 = new Thread(() -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                "shell input swipe " + fcx + " " + fcy + " " + (fcx - fm) + " " + (fcy - fm) + " 600"));
        Thread t2 = new Thread(() -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
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
                new BloquePrueba("SOFT.005.001", "Check if network-provided time is shown properly",
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
                new BloquePrueba("SOFT.005.008", "Add a new alarm",
                        "shell am start -a android.intent.action.SET_ALARM --ei android.intent.extra.alarm.HOUR 8 --ei android.intent.extra.alarm.MINUTES 0 --ez android.intent.extra.alarm.SKIP_UI false",
                        true),
                new BloquePrueba("SOFT.005.009", "Edit an alarm",
                        "shell am start -a android.intent.action.SHOW_ALARMS", true),
                new BloquePrueba("SOFT.005.010", "Delete an alarm",
                        "shell am start -a android.intent.action.SHOW_ALARMS", true),
                new BloquePrueba("SOFT.005.011", "Check that timer works properly",
                        "shell am start -a android.intent.action.SET_TIMER --ei android.intent.extra.alarm.LENGTH 30 --ez android.intent.extra.alarm.SKIP_UI false",
                        true),
                new BloquePrueba("SOFT.005.012", "Check that stopwatch works properly",
                        "shell am start -n com.google.android.deskclock/com.android.deskclock.DeskClock", true));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.005 — Clock functions", bloqueReloj, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    // PRUEBAS DE INFO
    @FXML
    public void addInfoTest() {
        List<BloquePrueba> bloqueInfo = List.of(
                new BloquePrueba("SOFT.012.001", "Check device name",
                        "shell getprop ro.product.name"),
                new BloquePrueba("SOFT.012.002", "Change device name",
                        "__CHANGE_NAME__"),
                new BloquePrueba("SOFT.012.003", "Check model name",
                        "shell getprop ro.product.model"),
                new BloquePrueba("SOFT.012.004", "Check serial number",
                        "shell getprop ro.serialno"),
                new BloquePrueba("SOFT.012.005", "Check software version",
                        "shell getprop ro.build.version.release"),
                new BloquePrueba("SOFT.012.006", "Check hardware version",
                        INFO_HW_VERSION),
                new BloquePrueba("SOFT.012.007", "Check IMEI number",
                        List.of("shell service call iphonesubinfo 1 s16 com.android.shell",
                                "shell service call iphonesubinfo 1")),
                new BloquePrueba("SOFT.012.008", "Check IMEISV number",
                        "shell am start -a android.settings.DEVICE_INFO_SETTINGS", true),
                new BloquePrueba("SOFT.012.009", "Check device name when connected to PC",
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

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.012 — Info", bloqueInfo, owner,
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
                new BloquePrueba("SOFT.008.001", "Change brightness level", DISPLAY_BRIGHTNESS_CHANGE),
                new BloquePrueba("SOFT.008.002", "Check default brightness about 75%", DISPLAY_BRIGHTNESS_CHECK),
                new BloquePrueba("SOFT.008.003", "Change wallpaper",
                        DISPLAY_WALLPAPER),
                new BloquePrueba("SOFT.008.004", "Check default time of screen timeout (1 minute)",
                        DISPLAY_TIMEOUT_CHECK),
                new BloquePrueba("SOFT.008.005", "Change the screen timeout and check if it works",
                        DISPLAY_TIMEOUT_CHANGE),
                new BloquePrueba("SOFT.008.006", "Change font size",
                        DISPLAY_FONT_SIZE),
                new BloquePrueba("SOFT.008.007", "Change display size",
                        DISPLAY_DISPLAY_SIZE),
                new BloquePrueba("SOFT.008.008", "Set a screen saver and check if it works",
                        DISPLAY_SCREENSAVER),
                new BloquePrueba("SOFT.008.009", "Check Company colour in UI",
                        "shell am start -a android.settings.DISPLAY_SETTINGS", true));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.008 - Display", bloqueDisplay, owner,
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
        try {
            // Crear imagen y subirla
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.BLUE);
            g.fillRect(0, 0, 100, 100);
            g.dispose();

            File tempFile = File.createTempFile("wallpaper_test", ".jpg");
            ImageIO.write(img, "jpg", tempFile);

            String rutaImagen = "/data/local/tmp/wallpaper_test.jpg";
            new ProcessBuilder("adb", "-s", serial, "push",
                    tempFile.getAbsolutePath(), rutaImagen)
                    .start().waitFor();
            tempFile.delete();

            // Instalar APK
            File apkFile = new File(getClass().getClassLoader()
                    .getResource("apk/WallpaperSetter.apk").toURI());
            new ProcessBuilder("adb", "-s", serial, "install", "-r",
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
                    "shell logcat -d -t 50 -s WallpaperSetter");
            boolean ok = logcat != null
                    && logcat.contains("Wallpaper establecido correctamente");

            // Limpiar
            new ProcessBuilder("adb", "-s", serial, "uninstall",
                    "com.example.wallpapersetter").start().waitFor();
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaImagen);

            return ok;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        boolean confirmado = ContactosConfigPopup.mostrar(otrosDispositivos, telefonoDUT, telefono, serialReceptor,
                owner);

        if (!confirmado) {
            return;
        }
        contactoTestTelefonoDUT = telefonoDUT[0];
        contactoTestTelefono = telefono[0];
        contactoSerialReceptor = (serialReceptor != null && serialReceptor.length > 0 && serialReceptor[0] != null
                && !serialReceptor[0].isEmpty())
                        ? serialReceptor[0]
                        : null;

        mostrarSelectorContactos();
    }

    private void mostrarSelectorContactos() {
        List<BloquePrueba> bloqueContactos = List.of(
                new BloquePrueba("SOFT.006.001", "Create several SIM contacts", CONTACT_CREATE_SIM),
                new BloquePrueba("SOFT.006.002", "Insert SIM card on other device and check SIM contacts created", "",
                        true),
                new BloquePrueba("SOFT.006.003", "Edit SIM card contacts",
                        "shell am start -a android.intent.action.VIEW -t vnd.android.cursor.dir/contact", true),
                new BloquePrueba("SOFR.006.004", "Delete a SIM card contact",
                        "shell am start -a android.intent.action.VIEW -t vnd.android.cursor.dir/contact", true),
                new BloquePrueba("SOFT.006.005", "Make a call to a SIM card contact", CONTACT_CALL_SIM),
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
                new BloquePrueba("SOFT.006.012", "Create several exchange contacts", "", true),
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
                new BloquePrueba("SOFT.006.021", "Copy several SIM contacts to phone", CONTACT_COPY_SIM_PHONE),
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
                new BloquePrueba("SOFT.006.036", "Check Service Dial Numbers (SDN)", "", true),
                new BloquePrueba("SOFT.006.037", "Memory status: Check size for Phone contacts and USIM contacts",
                        CONTACT_MEMORY_STATUS));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.006 — Contacts functions", bloqueContactos, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    public String parsearListaContactos(String rawOutput) {
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

    private boolean editarContactoSIM(String serial, ADBService adb) {
        boolean ok = true;
        for (int i = 1; i <= 3; i++) {
            String nombreOriginal = contactoTestNombre + "_SIM_" + i;
            String nombreEditado = contactoTestNombre + "_SIM_" + i + "_Edited";

            String r = adb.ejecutarComandoSincrono(serial,
                    "shell content update --uri content://icc/adn " +
                            "--bind tag:s:" + nombreEditado + " " +
                            "--where \"tag='" + nombreOriginal + "'\"");
            if (r != null && r.toLowerCase().contains("error")) {
                ok = false;
            }
        }
        return ok;
    }

    private boolean borrarContactoSIM(String serial, ADBService adb) {
        boolean ok = true;
        adb.ejecutarComandoSincrono(serial,
                "shell content delete --uri content://icc/adn " +
                        "--where \"number='" + contactoTestTelefono + "'\"");

        String checkFinal = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://icc/adn " +
                        "--where \"number='" + contactoTestTelefono + "'\"");
        ok = checkFinal == null || checkFinal.contains("No result found") || checkFinal.isBlank();
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
        System.out.println("CONTACTO = " + contactoTestTelefono);
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

    // VCARD
    private boolean importarVCard(String serial, ADBService adb) {
        String nombreTest = "Test_import";
        String telefonoTest = "600112233";
        String rutaVCard = "/sdcard/Download/import_test.vcf";
        String paqueteAPK = "com.example.vcardagent";

        try {
            // Limpiar rutas
            adb.ejecutarComandoSincrono(serial,
                    "shell content delete --uri content://com.android.contacts/raw_contacts " +
                            "--where \"display_name='" + nombreTest + "'\"");
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaVCard);

            // Crear archivo VCard
            String vcardContenido = "BEGIN:VCARD\nVERSION:3.0\nFN:" + nombreTest + "\nTEL;TYPE=CELL:" + telefonoTest
                    + "\nEND:VCARD\n";
            File tempFile = File.createTempFile("import_test", ".vcf");
            java.nio.file.Files.writeString(tempFile.toPath(), vcardContenido);

            new ProcessBuilder("adb", "-s", serial, "push", tempFile.getAbsolutePath(), rutaVCard).start()
                    .waitFor();
            tempFile.delete(); // Limpiar el archivo temporal

            // Extraer e Instalar la apk
            File apkFile = new File(getClass().getClassLoader().getResource("apk/VCardAgent.apk").toURI());
            new ProcessBuilder("adb", "-s", serial, "install", "-r", apkFile.getAbsolutePath()).start().waitFor();
            Thread.sleep(2000);

            // Permisos
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.WRITE_CONTACTS");

            // Limpiar el logcat
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");

            // Broadcast importacion
            adb.ejecutarComandoSincrono(serial, "shell am broadcast -a com.example.vcardagent.ACTION_IMPORT_VCARD " +
                    "-n " + paqueteAPK + "/.VCardReceiver");
            Thread.sleep(4000);

            // Verificaciones
            String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d -t 50 -s VCARD_AGENT");
            boolean apkDiceOk = logcat != null && logcat.contains("Importación verídica completada");

            String queryBaseDatos = adb.ejecutarComandoSincrono(serial,
                    "shell content query --uri content://com.android.contacts/data/phones --projection display_name " +
                            "--where \"display_name='" + nombreTest + "'\"");
            boolean contactoExisteEnAgenda = queryBaseDatos != null && queryBaseDatos.contains(nombreTest);

            new ProcessBuilder("adb", "-s", serial, "uninstall", paqueteAPK).start().waitFor();

            return apkDiceOk && contactoExisteEnAgenda;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean exportarVCard(String serial, ADBService adb) {
        String rutaVCard = "/sdcard/Download/contactos_backup_test.vcf";
        String paqueteAPK = "com.example.vcardagent";

        try {
            // Limpiar
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaVCard);

            // Extraer e Instalar la APK
            File apkFile = new File(getClass().getClassLoader().getResource("apk/VCardAgent.apk").toURI());
            new ProcessBuilder("adb", "-s", serial, "install", "-r", apkFile.getAbsolutePath()).start().waitFor();
            Thread.sleep(2000);

            // Conceder permisos a la apk
            adb.ejecutarComandoSincrono(serial, "shell pm grant " + paqueteAPK + " android.permission.READ_CONTACTS");

            // Limpiar logcat
            adb.ejecutarComandoSincrono(serial, "shell logcat -c");

            // Broadcast Exportar
            adb.ejecutarComandoSincrono(serial, "shell am broadcast -a com.example.vcardagent.ACTION_EXPORT_VCARD " +
                    "-n " + paqueteAPK + "/.VCardReceiver");
            Thread.sleep(4000);

            // Verificar si la apk terminó con éxito
            String logcat = adb.ejecutarComandoSincrono(serial, "shell logcat -d -t 50 -s VCARD_AGENT");
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

            new ProcessBuilder("adb", "-s", serial, "uninstall", paqueteAPK).start().waitFor();

            return apkOk && archivoExiste;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // MEMORIA

    private boolean comprobarMemoriaContactos(String serial, ADBService adb) {
        String phone = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://com.android.contacts/contacts " +
                        "--projection _id");
        String sim = adb.ejecutarComandoSincrono(serial,
                "shell content query --uri content://icc/adn " +
                        "--projection tag");
        return phone != null && sim != null;
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
        System.out.println("[ADB] Dispositivos detectados: " + lista);
        return lista;
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

    // long restanteMs = WIFI_TIMEOUT_MS - (System.currentTimeMillis() - inicio);
    // int min = (int) (restanteMs / 60_000);
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
        System.out.println("[WIFI] Activando interfaz WiFi...");
        adb.ejecutarPasoSync(serial, paso.getComando());

        if (tieneIpWifi(serial)) {
            System.out.println("[WIFI] Conectado automáticamente ✔");

            return true;
        }

        System.out.println("[WIFI] Sin red — abriendo ajustes WiFi...");
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

            // ── Estado mutable de página ──────────────────────────────────────────
            // Usamos array de 1 elemento para poder modificarlos desde lambdas/helpers
            final PDPage[] paginaActual = { new PDPage() };
            doc.addPage(paginaActual[0]);
            final PDPageContentStream[] cs = {
                    new PDPageContentStream(doc, paginaActual[0])
            };
            final int[] y = { 750 };

            // Helper para saltar de página
            // Cierra el stream actual, crea nueva página y abre nuevo stream
            Runnable nuevaPagina = () -> {
                try {
                    cs[0].close();
                    PDPage nuevaPag = new PDPage();
                    doc.addPage(nuevaPag);
                    cs[0] = new PDPageContentStream(doc, nuevaPag);
                    y[0] = 750;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            // Helper para comprobar espacio y saltar si hace falta
            Runnable checkSalto = () -> {
                if (y[0] < 60)
                    nuevaPagina.run();
            };

            // ── TÍTULO ────────────────────────────────────────────────────────────
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
            cs[0].showText("Fecha: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            cs[0].endText();
            y[0] -= 24;

            // ── IDENTIFICACIÓN ────────────────────────────────────────────────────
            y[0] = dibujarSeccionPDF(cs[0], "IDENTIFICACION DEL DISPOSITIVO", y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Modelo", limpiar(dispositivoActual.getModelo().getNombreModelo()), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Marca", limpiar(dispositivoActual.getModelo().getMarca().getNombre()), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "S/N", limpiar(dispositivoActual.getSerialNumber()), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Android ID", limpiar(dispositivoActual.getAndroid_id()), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "IMEI", limpiar(specs.getOrDefault("IMEI", "N/A")), y[0]);

            // ── SOFTWARE ──────────────────────────────────────────────────────────
            y[0] -= 8;
            y[0] = dibujarSeccionPDF(cs[0], "SOFTWARE", y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Version Android", limpiar(specs.getOrDefault("Android", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Parche seguridad", limpiar(specs.getOrDefault("Parche", "N/A")), y[0]);

            // ── HARDWARE ──────────────────────────────────────────────────────────
            y[0] -= 8;
            y[0] = dibujarSeccionPDF(cs[0], "HARDWARE", y[0]);
            y[0] = dibujarFilaPDF(cs[0], "CPU", limpiar(specs.getOrDefault("CPU", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "RAM", limpiar(specs.getOrDefault("RAM", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Almacenamiento", limpiar(specs.getOrDefault("Storage", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Resolucion", limpiar(specs.getOrDefault("Resolucion", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "DPI", limpiar(specs.getOrDefault("DPI", "N/A")), y[0]);

            // ── BATERÍA ───────────────────────────────────────────────────────────
            y[0] -= 8;
            y[0] = dibujarSeccionPDF(cs[0], "BATERIA", y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Nivel", limpiar(specs.getOrDefault("Bateria", "N/A")), y[0]);
            y[0] = dibujarFilaPDF(cs[0], "Estado", limpiar(specs.getOrDefault("EstadoCarga", "N/A")), y[0]);

            // ── RESULTADOS DE PRUEBAS ─────────────────────────────────────────────
            y[0] -= 8;
            checkSalto.run();
            y[0] = dibujarSeccionPDF(cs[0], "RESULTADOS DE PRUEBAS", y[0]);

            for (PasoPrueba paso : pasos) {
                // Salto de página si no hay espacio
                if (y[0] < 60) {
                    nuevaPagina.run();
                    y[0] = dibujarSeccionPDF(cs[0], "RESULTADOS DE PRUEBAS (cont.)", y[0]);
                }

                // Nombre de la prueba
                cs[0].beginText();
                cs[0].setFont(PDType1Font.HELVETICA, 9);
                cs[0].setNonStrokingColor(java.awt.Color.BLACK);
                cs[0].newLineAtOffset(55, y[0]);
                cs[0].showText(limpiar(paso.getNombre()));
                cs[0].endText();

                // PASS / FAIL
                cs[0].beginText();
                cs[0].setFont(PDType1Font.HELVETICA_BOLD, 9);
                cs[0].newLineAtOffset(450, y[0]);
                if ("OK".equals(paso.getEstado())) {
                    cs[0].setNonStrokingColor(new java.awt.Color(34, 139, 34));
                    cs[0].showText("PASS");
                } else {
                    cs[0].setNonStrokingColor(java.awt.Color.RED);
                    cs[0].showText("FAIL");
                }
                cs[0].endText();
                y[0] -= 14;

                // Output detalle
                String detalle = paso.getOutputDetalle();
                if (detalle != null && !detalle.isBlank()) {
                    if (y[0] < 60) {
                        nuevaPagina.run();
                        y[0] = dibujarSeccionPDF(cs[0], "RESULTADOS DE PRUEBAS (cont.)", y[0]);
                    }
                    String detalleLimpio = limpiar(detalle);
                    if (detalleLimpio.length() > 80)
                        detalleLimpio = detalleLimpio.substring(0, 80) + "...";

                    cs[0].beginText();
                    cs[0].setFont(PDType1Font.HELVETICA_OBLIQUE, 7);
                    cs[0].setNonStrokingColor(new java.awt.Color(100, 100, 100));
                    cs[0].newLineAtOffset(65, y[0]);
                    cs[0].showText(detalleLimpio);
                    cs[0].endText();
                    y[0] -= 10;
                }
            }

            // Cerrar el último stream
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

    private int dibujarSeccionPDF(PDPageContentStream cs, String titulo, int y) throws IOException {
        cs.setNonStrokingColor(new java.awt.Color(30, 30, 60));
        cs.addRect(50, y - 4, 500, 16);
        cs.fill();
        cs.beginText();
        cs.setNonStrokingColor(java.awt.Color.WHITE);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(55, y);
        cs.showText(titulo);
        cs.endText();
        cs.setNonStrokingColor(java.awt.Color.BLACK);
        return y - 20;
    }

    private int dibujarFilaPDF(PDPageContentStream cs, String clave, String valor, int y) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.newLineAtOffset(55, y);
        cs.showText(limpiar(clave) + ":");
        cs.endText();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(200, y);
        cs.showText(limpiar(valor));
        cs.endText();
        return y - 14;
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
        return parcelRaw;
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
}