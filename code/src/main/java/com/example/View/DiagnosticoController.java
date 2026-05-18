package com.example.View;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

import com.example.Controller.ADBService;
import com.example.Model.BloquePrueba;
import com.example.Model.Dispositivo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;

import com.example.Model.PasoPrueba;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
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
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class DiagnosticoController implements DispositivoAware {

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
    private Button btnInforme;
    private String llamadaEntreDosNumero1 = null; // número del teléfono 1 (introducido por usuario)
    private String llamadaEntreDosNumero2 = null; // número del teléfono 2 (introducido por usuario)

    private final ObservableList<PasoPrueba> pasos = FXCollections.observableArrayList();
    private Dispositivo dispositivoActual;
    // ─── WiFi timeout ─────────────────────────────────────────────────────────
    private static final int WIFI_TIMEOUT_MS = 3 * 60 * 1000;
    private static final int WIFI_POLL_INTERVAL_MS = 3_000;

    // ─── Marcadores especiales para pasos de llamada avanzados ───────────────
    // El comando del PasoPrueba se usa como identificador interno cuando la
    // lógica de ejecución necesita hacer algo más que un simple shell.
    private static final String CMD_LLAMADA_MASIVA = "__LLAMADA_MASIVA__";
    private static final String CMD_LLAMADA_ENTRE_DOS = "__LLAMADA_ENTRE_DOS__";

    private static final String TOUCH_PINCH = "__PINCH__";
    private static final String TOUCH_SPREAD = "__SPREAD__";

    private static final String INFO_CHANGE_NAME = "__CHANGE_NAME__";
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

    // ─── Datos extra para los pasos de llamada avanzados ─────────────────────
    // Se rellenan cuando el usuario configura el paso en el popup.
    private String llamadaMasivaNumero = null; // número destino para llamada masiva
    private String llamadaEntreDosSerial1 = null; // serial del teléfono 1
    private String llamadaEntreDosSerial2 = null; // serial del teléfono 2
    // Los números se detectan en tiempo de ejecución por ADB

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

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP DE LLAMADA — se abre cuando el usuario pulsa "Añadir prueba llamada"
    // Ofrece dos opciones:
    // A) Llamada masiva → todos los dispositivos ADB llaman a un número manual
    // B) Llamada entre 2 → detecta los seriales ADB conectados, el usuario
    // elige cuál es el teléfono 1 y cuál el 2, y se detectan sus números
    // automáticamente por ADB al ejecutar
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void addCallTest() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("Configurar Prueba de Llamada");

        // ── Contenedor principal ──────────────────────────────────────────────
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle(
                "-fx-background-color: #1e1e2e;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");
        root.setPrefWidth(420);

        // Título
        Label titulo = new Label("Configurar Prueba de Llamada");
        titulo.setFont(Font.font("Poppins", FontWeight.BOLD, 16));
        titulo.setTextFill(Color.web("#cdd6f4"));

        Label subtitulo = new Label("Selecciona el tipo de prueba:");
        subtitulo.setTextFill(Color.web("#a6adc8"));
        subtitulo.setFont(Font.font(13));

        // ── OPCIÓN A: Llamada Masiva ──────────────────────────────────────────
        VBox boxMasiva = crearBoxOpcion(
                "📞  Llamada Masiva",
                "Todos los dispositivos conectados llamarán al número que escribas.");

        TextField tfNumeroMasivo = new TextField();
        tfNumeroMasivo.setPromptText("Número destino (ej: +34612345678)");
        tfNumeroMasivo.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-prompt-text-fill: #6c7086;" +
                        "-fx-border-color: #45475a; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 8;");

        Button btnMasiva = crearBoton("Añadir llamada masiva", "#89b4fa");
        boxMasiva.getChildren().addAll(tfNumeroMasivo, btnMasiva);

        VBox boxEntreDos = crearBoxOpcion(
                "🔄  Llamada entre 2 Dispositivos",
                "Tel. 1 llama a Tel. 2 (10s) → Tel. 2 llama a Tel. 1 (10s).\n" +
                        "Selecciona los dispositivos ADB e introduce los números manualmente.");

        List<String> serialesConectados = obtenerSerialesADB();

        Label lblDisp1 = new Label("Teléfono 1 (el que llama primero):");
        lblDisp1.setTextFill(Color.web("#a6adc8"));
        ComboBox<String> cbDisp1 = crearCombo(serialesConectados);

        Label lblNum1 = new Label("Número del Teléfono 1:");
        lblNum1.setTextFill(Color.web("#a6adc8"));
        TextField tfNum1 = new TextField();
        tfNum1.setPromptText("Ej: +34612345678");
        tfNum1.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-prompt-text-fill: #6c7086;" +
                        "-fx-border-color: #45475a; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 8;");

        Label lblDisp2 = new Label("Teléfono 2 (el que recibe primero):");
        lblDisp2.setTextFill(Color.web("#a6adc8"));
        ComboBox<String> cbDisp2 = crearCombo(serialesConectados);

        Label lblNum2 = new Label("Número del Teléfono 2:");
        lblNum2.setTextFill(Color.web("#a6adc8"));
        TextField tfNum2 = new TextField();
        tfNum2.setPromptText("Ej: +34698765432");
        tfNum2.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-prompt-text-fill: #6c7086;" +
                        "-fx-border-color: #45475a; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 8;");

        // Obtener el serial del dispositivo actualmente abierto
        // Si el dispositivo actual está en ADB, mostramos label fijo en vez de combo
        String serialActual = obtenerSerialADBActual();

        if (serialActual != null && serialesConectados.contains(serialActual)) {
            cbDisp1.getSelectionModel().select(serialActual);
            cbDisp1.setDisable(true);
            cbDisp1.setStyle(cbDisp1.getStyle() + "-fx-opacity: 0.5;"); // visual de "bloqueado"
        } else if (!serialesConectados.isEmpty()) {
            cbDisp1.getSelectionModel().select(0);
        }

        // Tel.2: seleccionar el primero que NO sea el dispositivo actual
        serialesConectados.stream()
                .filter(s -> !s.equals(serialActual))
                .findFirst()
                .ifPresent(s -> cbDisp2.getSelectionModel().select(s));

        Label lblAviso = new Label();
        lblAviso.setTextFill(Color.web("#f38ba8"));
        lblAviso.setFont(Font.font(11));
        lblAviso.setWrapText(true);

        Button btnEntreDos = crearBoton("Añadir llamada entre 2", "#a6e3a1");
        boxEntreDos.getChildren().addAll(
                lblDisp1, cbDisp1,
                lblNum1, tfNum1,
                lblDisp2, cbDisp2,
                lblNum2, tfNum2,
                lblAviso, btnEntreDos);

        // ── Botón cancelar ────────────────────────────────────────────────────
        Button btnCancelar = crearBoton("Cancelar", "#f38ba8");
        btnCancelar.setOnAction(e -> popup.close());

        root.getChildren().addAll(titulo, subtitulo,
                new Separator(), boxMasiva,
                new Separator(), boxEntreDos,
                new Separator(), btnCancelar);

        // ── Acciones ──────────────────────────────────────────────────────────
        btnMasiva.setOnAction(e -> {
            String num = tfNumeroMasivo.getText().trim();
            if (num.isBlank()) {
                tfNumeroMasivo.setStyle(tfNumeroMasivo.getStyle() +
                        "-fx-border-color: #f38ba8;");
                return;
            }
            llamadaMasivaNumero = num;
            pasos.add(new PasoPrueba(
                    "Llamada Masiva → " + num,
                    CMD_LLAMADA_MASIVA));
            popup.close();
        });

        btnEntreDos.setOnAction(e -> {
            String s1 = cbDisp1.getValue();
            String s2 = cbDisp2.getValue();
            String n1 = tfNum1.getText().trim();
            String n2 = tfNum2.getText().trim();

            if (s1 == null || s2 == null) {
                lblAviso.setText("No hay dispositivos ADB disponibles.");
                return;
            }
            if (s1.equals(s2)) {
                lblAviso.setText("Selecciona dos dispositivos distintos.");
                return;
            }
            if (n1.isBlank() || n2.isBlank()) {
                lblAviso.setText("Introduce los números de ambos teléfonos.");
                return;
            }

            llamadaEntreDosSerial1 = s1;
            llamadaEntreDosSerial2 = s2;
            llamadaEntreDosNumero1 = n1;
            llamadaEntreDosNumero2 = n2;

            pasos.add(new PasoPrueba(
                    "Llamada entre 2 (" + s1 + " ↔ " + s2 + ")",
                    CMD_LLAMADA_ENTRE_DOS));
            popup.close();
        });

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EJECUTAR SCRIPT
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ejecutarScript() {
        if (dispositivoActual == null || pasos.isEmpty())
            return;

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
                    ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);

                } else if (esWifi) {
                    ok = ejecutarPasoWifiConEspera(adb, serial, paso);

                } else if (CMD_LLAMADA_MASIVA.equals(paso.getComando())) {
                    ok = ejecutarLlamadaMasiva(paso);

                } else if (CMD_LLAMADA_ENTRE_DOS.equals(paso.getComando())) {
                    ok = ejecutarLlamadaEntreDos(paso);

                } else if (TOUCH_PINCH.equals(paso.getComando())) {
                    ok = ejecutarPinch(serial, adb);

                } else if (TOUCH_SPREAD.equals(paso.getComando())) {
                    ok = ejecutarSpread(serial, adb);

                } else if (INFO_CHANGE_NAME.equals(ref.getComando())) {
                    ok = cambiarYRestaurarNombre(serial, adb);

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
                    outputDetalle = valor != null ? "DEFAULT: " + valor.trim() + "/255" : "";
                } else if (DISPLAY_WALLPAPER.equals(ref.getComando())) {
                    ok = cambiarWallpaper(serial, adb);
                } else if (DISPLAY_TIMEOUT_CHECK.equals(ref.getComando())) {
                    String valor = adb.ejecutarComandoSincrono(serial, "shell settings get system screen_off_timeout");
                    ok = comprobarTimeoutPorDefecto(serial, adb);
                    outputDetalle = valor != null ? "DEFAULT: " + valor.trim() + "ms" : "";
                } else if (DISPLAY_TIMEOUT_CHANGE.equals(ref.getComando())) {
                    ok = cambiarYVerificarTimeout(serial, adb);
                } else if (DISPLAY_FONT_SIZE.equals(ref.getComando())) {
                    ok = cambiarYRestaurarFuente(serial, adb);
                } else if (DISPLAY_DISPLAY_SIZE.equals(ref.getComando())) {
                    ok = cambiarYRestaurarDisplaySize(serial, adb);
                } else if (DISPLAY_SCREENSAVER.equals(ref.getComando())) {
                    ok = comprobarScreensaver(serial, adb);
                    adb.ejecutarComandoSincrono(serial, "shell input keyevent KEYCODE_WAKEUP");
                } else {
                    ADBService.EjecucionADB r = adb.ejecutarYObtener(serial, ref.getComandos());
                    ok = r.exito();
                    outputDetalle = r.outputJunto();
                    if (outputDetalle.contains("Parcel")) {
                        outputDetalle = parsearParcelIMEI(outputDetalle);
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
                System.out.println("[DIAG] Secuencia completada.");
            });
        }).start();
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

    // Espera activamente hasta que el receptor detecte la llamada entrante
    // Comprueba cada segundo hasta maxSegundos — contesta en cuanto suena
    private boolean esperarHastaQueSuene(String serialReceptor, int maxSegundos) {
        System.out.println("[ENTRE2] Esperando que suene en: " + serialReceptor);
        for (int i = 0; i < maxSegundos; i++) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            String out = ejecutarShellEnSerial(serialReceptor, "dumpsys telephony.registry");
            if (out.contains("mCallState=1")) { // RINGING = sonando
                System.out.println("[ENTRE2] ¡Está sonando! (tardó " + (i + 1) + "s)");
                return true;
            }
        }
        System.out.println("[ENTRE2] No sonó en " + maxSegundos + "s — timeout");
        return false;
    }

    // Verifica si el dispositivo tiene una llamada activa o sonando
    private boolean llamadaActiva(String serial) {
        String out = ejecutarShellEnSerial(serial, "dumpsys telephony.registry");
        return out.contains("mCallState=2") || // OFFHOOK = llamada activa
                out.contains("mCallState=1"); // RINGING = sonando
    }

    // Pruebas de TOUCHSCREEN
    @FXML
    private void addTouchScreenTest() {
        List<BloquePrueba> bloqueTouchScreen = List.of(
                new BloquePrueba("SOFT.004.001", "Check single tap on touchscreen",
                        "shell input tap 540 960"),
                new BloquePrueba("SOFT.004.002", "Check double tap on touchscreen",
                        "shell input tap 540 960 && sleep 0.3 && input tap 540 960"),
                new BloquePrueba("SOFT.004.003", "Check long press (press and hold) on touchscreen",
                        "shell input swipe 540 960 540 961 1500"),
                new BloquePrueba("SOFT.004.004", "Check drag on touchscreen",
                        "shell input swipe 200 500 700 500 800"),
                new BloquePrueba("SOFT.004.005", "Check flick movement on touchscreen",
                        "shell input swipe 200 500 700 500 100"),
                new BloquePrueba("SOFT.004.006", "Check pinch on touchscreen",
                        "__PINCH__"),
                new BloquePrueba("SOFT.004.007", "Check spread on touchscreen",
                        "__SPREAD__"));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();

        SelectorPruebasPopup.mostrar(
                "SOFT.004 — Touch Screen",
                bloqueTouchScreen,
                owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    public boolean ejecutarPinch(String serial, ADBService adb) {
        boolean[] resultados = { false, false };
        Thread t1 = new Thread(
                () -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                        "shell input swipe 200 600 540 960 600"));
        Thread t2 = new Thread(
                () -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
                        "shell input swipe 880 1320 540 960 600"));
        t1.start();
        t2.start();
        try {
            t1.join(3_000);
            t2.join(3_000);
        } catch (InterruptedException ignored) {
        }
        return resultados[0] && resultados[1];
    }

    public boolean ejecutarSpread(String serial, ADBService adb) {
        boolean[] resultados = { false, false };
        Thread t1 = new Thread(
                () -> resultados[0] = adb.ejecutarComandoSincronoBoolean(serial,
                        "shell input swipe 540 960 200 600 600"));
        Thread t2 = new Thread(
                () -> resultados[1] = adb.ejecutarComandoSincronoBoolean(serial,
                        "shell input swipe 540 960 880 1320 600"));
        t1.start();
        t2.start();

        try {
            t1.join(3_000);
            t2.join(3_000);
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
                        List.of("shell getprop ro.revision", "shell getprop ro.boot.hardware.revision",
                                "shell getprop ro.hardware.revision", "shell getprop ro.hardware")),
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
            String wallpaperOriginal = adb.ejecutarComandoSincrono(serial,
                    "shell settings get secure wallpaper_component");
            System.out.println("[WALLPAPER] Original: " + wallpaperOriginal);

            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.RED);
            g.fillRect(0, 0, 100, 100);
            g.dispose();

            File tempFile = File.createTempFile("wallpaper_test", ".jpg");
            ImageIO.write(img, "jpg", tempFile);
            System.out.println("[WALLPAPER] Imagen creada en: " + tempFile.getAbsolutePath());

            String rutaImagen = "/data/local/tmp/wallpaper_test.jpg";
            Process pushImg = new ProcessBuilder("adb", "-s", serial, "push",
                    tempFile.getAbsolutePath(), rutaImagen).start();
            pushImg.waitFor();
            
            System.out.println("[WALLPAPER] Push imagen exit code: " + pushImg.exitValue());
            tempFile.delete();

            adb.ejecutarComandoSincrono(serial, "shell chmod 644 "+rutaImagen);

            java.net.URL apkUrl = getClass().getResource("/apk/WallpaperSetter.apk");
            System.out.println("[WALLPAPER] APK URL: " + apkUrl);
            if (apkUrl == null) {
                System.out.println("[WALLPAPER] ERROR — APK no encontrada en resources");
                return false;
            }

            File apkFile = new File(apkUrl.toURI());
            String apkLocal = apkFile.getAbsolutePath();
            System.out.println("[WALLPAPER] APK path corregido: " + apkLocal);

            String rutaApk = "/sdcard/WallpaperSetter.apk";
            Process pushApk = new ProcessBuilder("adb", "-s", serial, "push",
                    apkLocal, rutaApk).start();
            pushApk.waitFor();
            System.out.println("[WALLPAPER] Push APK exit code: " + pushApk.exitValue());

            Process install = new ProcessBuilder("adb", "-s", serial,
                    "install", "-r", apkLocal).start();
            install.waitFor();
            // Leer output de instalación
            String installOutput = new String(install.getInputStream().readAllBytes());
            System.out.println("[WALLPAPER] Install output: " + installOutput);
            System.out.println("[WALLPAPER] Install exit code: " + install.exitValue());
            Thread.sleep(2000);

            adb.ejecutarComandoSincrono(serial, "shell logcat -c");
            adb.ejecutarComandoSincrono(serial,
                    "shell am broadcast -a com.example.wallpapersetter.SET_WALLPAPER " +
                            "--es image_path " + rutaImagen +
                            " -n com.example.wallpapersetter/.WallpaperReceiver");

            Thread.sleep(2000);

            String logcat = adb.ejecutarComandoSincrono(serial,
                    "shell logcat -d -t 50 -s WallpaperSetter");
            System.out.println("[WALLPAPER] Logcat WallpaperSetter: " + logcat);
            boolean cambioOk = logcat != null
                    && logcat.contains("Wallpaper establecido correctamente");
            System.out.println("[WALLPAPER] cambioOk: " + cambioOk);

            if (wallpaperOriginal != null && !wallpaperOriginal.equals("null")
                    && !wallpaperOriginal.isBlank()) {
                String restoreResult = adb.ejecutarComandoSincrono(serial,
                        "shell am broadcast -a com.example.wallpapersetter.SET_WALLPAPER " +
                                "--es image_path " + wallpaperOriginal +
                                " -n com.example.wallpapersetter/.WallpaperReceiver");
                System.out.println("[WALLPAPER] Restore result: " + restoreResult);
            }

            Process uninstall = new ProcessBuilder("adb", "-s", serial,
                    "uninstall", "com.example.wallpapersetter").start();
            uninstall.waitFor();
            System.out.println("[WALLPAPER] Uninstall exit code: " + uninstall.exitValue());

            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaImagen);
            adb.ejecutarComandoSincrono(serial, "shell rm " + rutaApk);

            return cambioOk;

        } catch (Exception e) {
            System.out.println("[WALLPAPER] Excepción: " + e.getMessage());
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
    // DESPERTAR DISPOSITIVO
    // Enciende la pantalla y quita el keyguard para garantizar
    // que los eventos de teclado se procesen correctamente
    // ─────────────────────────────────────────────────────────────────────────
    private void despertarDispositivo(String serial) {
        ejecutarShellEnSerial(serial, "input keyevent KEYCODE_WAKEUP");
        ejecutarShellEnSerial(serial, "wm dismiss-keyguard");
        System.out.println("[WAKE] Pantalla encendida: " + serial);
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
    private String ejecutarShellEnSerial(String serial, String shellCmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "adb", "-s", serial, "shell", shellCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append("\n");
            p.waitFor(10, TimeUnit.SECONDS);
            return sb.toString().trim();
        } catch (Exception e) {
            System.out.println("[ADB] Error en " + serial + ": " + e.getMessage());
            return "";
        }
    }

    /** Actualiza el estado de un paso en el hilo de UI. */
    private void actualizarEstadoPaso(PasoPrueba paso, String estado) {
        Platform.runLater(() -> {
            paso.setEstado(estado);
            listaPasos.refresh();
        });
    }

    /** Crea un VBox con estilo de "tarjeta" para cada opción del popup. */
    private VBox crearBoxOpcion(String titulo, String descripcion) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-border-color: #45475a;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Poppins", FontWeight.BOLD, 15));
        lblTitulo.setTextFill(Color.web("#cdd6f4"));

        Label lblDesc = new Label(descripcion);
        lblDesc.setTextFill(Color.web("#a6adc8"));
        lblDesc.setFont(Font.font("Poppins", FontWeight.NORMAL, 13));
        lblDesc.setWrapText(true);

        box.getChildren().addAll(lblTitulo, lblDesc);
        return box;
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