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

    private static final String CLOCK_CHECK_TIME = "__CHECK_TIME__";
    private static final String CLOCK_DATE_SETTINGS = "__DATE_SETTINGS__";
    private static final String CLOCK_CHECK_ZONE = "__CHECK_ZONE__";
    private static final String CLOCK_ADJUST_ZONE = "__ADJUST_ZONE__";
    private static final String CLOCK_24H = "__24H__";
    private static final String CLOCK_12H = "__12H__";
    private static final String CLOCK_ADD_WORLD_TIME = "__ADD_WORLD_TIME__";
    private static final String CLOCK_ALARM = "__ALARM__";
    private static final String CLOCK_TIMER = "__TIMER__";
    private static final String CLOCK_STOPWATCH = "__STOPWATCH__";
    private static final Map<String, String> CLOCK_INTENTS = Map.of(
            CLOCK_DATE_SETTINGS, "am start -a android.settings.DATE_SETTINGS",
            CLOCK_ADJUST_ZONE, "am start -a android.settings.TIMEZONE_SETTINGS",
            CLOCK_24H, "am start -a android.settings.DATE_SETTINGS",
            CLOCK_12H, "am start -a android.settings.DATE_SETTINGS",
            CLOCK_ADD_WORLD_TIME, "am start -n com.android.deskclock/.DeskClock",
            CLOCK_ALARM, "am start -a android.intent.action.SET_ALARM",
            CLOCK_TIMER, "am start -a android.intent.action.SET_TIMER",
            CLOCK_STOPWATCH, "am start -n com.android.deskclock/.DeskClock");

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
                Platform.runLater(() -> {
                    ref.setEstado("EJECUTANDO");
                    listaPasos.refresh();
                });

                boolean esWifi = paso.getNombre().toLowerCase().contains("wifi") &&
                        paso.getNombre().toLowerCase().contains("levantar");

                if (ref.isManual()) {
                    String intent = CLOCK_INTENTS.get(ref.getComando());
                    if (intent != null) {
                        adb.ejecutarAccionHilo(serial, intent);
                    }

                    Stage owner = (Stage) btnEjecutar.getScene().getWindow();
                    boolean ok = ConfirmacionManualPopup.mostrarYEsperar(ref.getNombre(), owner);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });

                } else if (esWifi) {
                    ejecutarPasoWifiConEspera(adb, serial, paso);

                } else if (CMD_LLAMADA_MASIVA.equals(paso.getComando())) {
                    boolean ok = ejecutarLlamadaMasiva(paso);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });

                } else if (CMD_LLAMADA_ENTRE_DOS.equals(paso.getComando())) {
                    boolean ok = ejecutarLlamadaEntreDos(paso);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });

                } else if (TOUCH_PINCH.equals(paso.getComando())) {
                    boolean ok = ejecutarPinch(serial, adb);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });

                } else if (TOUCH_SPREAD.equals(paso.getComando())) {
                    boolean ok = ejecutarSpread(serial, adb);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });
                } else if (CLOCK_CHECK_TIME.equals(paso.getComando())) {
                    boolean ok = comprobarHora(serial);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });
                } else if (CLOCK_CHECK_ZONE.equals(paso.getComando())) {
                    boolean ok = comprobarZonaHoraria(serial);
                    Platform.runLater(() -> {
                        ref.setEstado(ok ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });
                } else {
                    boolean exito = adb.ejecutarPasoSync(serial, paso.getComando());
                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException ignored) {
                    }
                    Platform.runLater(() -> {
                        ref.setEstado(exito ? "OK" : "ERROR");
                        listaPasos.refresh();
                    });
                }
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
                        "__CHECK_TIME__"),
                new BloquePrueba("SOFT.005.002", "Adjust manually date and time",
                        "__DATE_SETTINGS__", true),
                new BloquePrueba("SOFT.005.003", "Check if network-provided time zone is shown (GMT)",
                        "__CHECK_ZONE__"),
                new BloquePrueba("SOFT.005.004", "Adjust manually time zone",
                        "__ADJUST_ZONE__", true),
                new BloquePrueba("SOFT.005.005", "Adjust 24-hour time format",
                        "__24H__", true),
                new BloquePrueba("SOFT.005.006", "Adjust 12-hour time format",
                        "__12H__", true),
                new BloquePrueba("SOFT.005.007", "Add an hour of world time list",
                        "__ADD_WORLD_TIME__", true),
                new BloquePrueba("SOFT.005.008", "Add a new alarm",
                        "__ALARM__", true),
                new BloquePrueba("SOFT.005.009", "Edit an alarm",
                        "__ALARM__", true),
                new BloquePrueba("SOFT.005.010", "Delete an alarm",
                        "__ALARM__", true),
                new BloquePrueba("SOFT.005.011", "Check that timer works properly",
                        "__TIMER__", true),
                new BloquePrueba("SOFT.005.012", "Check that stopwatch works properly",
                        "__STOPWATCH__", true));

        Stage owner = (Stage) btnEjecutar.getScene().getWindow();
        SelectorPruebasPopup.mostrar("SOFT.005 — Clock functions", bloqueReloj, owner,
                seleccionadas -> seleccionadas.stream()
                        .map(BloquePrueba::toPasoPrueba)
                        .forEach(pasos::add));
    }

    private boolean comprobarHora(String serial) throws IOException{
            
    }

    private boolean comprobarZonaHoraria(String serial) throws IOException{
        
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
    private boolean ejecutarPasoWifiConEspera(ADBService adb, String serial, PasoPrueba paso) {
        System.out.println("[WIFI] Activando interfaz WiFi...");
        adb.ejecutarPasoSync(serial, paso.getComando());

        if (tieneIpWifi(serial)) {
            System.out.println("[WIFI] Conectado automáticamente ✔");
            Platform.runLater(() -> {
                paso.setEstado("OK");
                listaPasos.refresh();
            });
            return true;
        }
        System.out.println("[WIFI] Sin red — abriendo ajustes WiFi...");
        ejecutarShellEnSerial(serial, "am start -a android.settings.WIFI_SETTINGS");
        Platform.runLater(() -> {
            paso.setEstado("ESPERANDO WIFI...");
            listaPasos.refresh();
        });

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
        final String estadoFinal = conectado ? "OK" : "ERROR";
        Platform.runLater(() -> {
            paso.setEstado(estadoFinal);
            listaPasos.refresh();
        });
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
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── TÍTULO ───────────────────────────────────────────────────────
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("CERTIFICADO DE DIAGN" + limpiar("OSTICO") + " T" + limpiar("ECNICO"));
                cs.endText();

                // Línea separadora
                cs.setLineWidth(1f);
                cs.moveTo(50, 738);
                cs.lineTo(550, 738);
                cs.stroke();

                // Fecha
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(50, 724);
                cs.showText("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                cs.endText();

                // ── IDENTIFICACIÓN ───────────────────────────────────────────────
                int y = 700;
                y = dibujarSeccion(cs, "IDENTIFICACION DEL DISPOSITIVO", y);
                y = dibujarFila(cs, "Modelo", limpiar(dispositivoActual.getModelo().getNombreModelo()), y);
                y = dibujarFila(cs, "Marca", limpiar(dispositivoActual.getModelo().getMarca().getNombre()), y);
                y = dibujarFila(cs, "S/N", limpiar(dispositivoActual.getSerialNumber()), y);
                y = dibujarFila(cs, "Android ID", limpiar(dispositivoActual.getAndroid_id()), y);
                y = dibujarFila(cs, "IMEI", limpiar(specs.getOrDefault("IMEI", "N/A")), y);

                // ── SOFTWARE ─────────────────────────────────────────────────────
                y -= 8;
                y = dibujarSeccion(cs, "SOFTWARE", y);
                y = dibujarFila(cs, "Version Android", limpiar(specs.getOrDefault("Android", "N/A")), y);
                y = dibujarFila(cs, "Parche seguridad", limpiar(specs.getOrDefault("Parche", "N/A")), y);

                // ── HARDWARE ─────────────────────────────────────────────────────
                y -= 8;
                y = dibujarSeccion(cs, "HARDWARE", y);
                y = dibujarFila(cs, "CPU", limpiar(specs.getOrDefault("CPU", "N/A")), y);
                y = dibujarFila(cs, "RAM", limpiar(specs.getOrDefault("RAM", "N/A")), y);
                y = dibujarFila(cs, "Almacenamiento", limpiar(specs.getOrDefault("Storage", "N/A")), y);
                y = dibujarFila(cs, "Resolucion", limpiar(specs.getOrDefault("Resolucion", "N/A")), y);
                y = dibujarFila(cs, "DPI", limpiar(specs.getOrDefault("DPI", "N/A")), y);

                // ── BATERÍA ──────────────────────────────────────────────────────
                y -= 8;
                y = dibujarSeccion(cs, "BATERIA", y);
                y = dibujarFila(cs, "Nivel", limpiar(specs.getOrDefault("Bateria", "N/A")), y);
                y = dibujarFila(cs, "Estado", limpiar(specs.getOrDefault("EstadoCarga", "N/A")), y);

                // ── RESULTADOS DE PRUEBAS ────────────────────────────────────────
                y -= 8;
                y = dibujarSeccion(cs, "RESULTADOS DE PRUEBAS", y);

                for (PasoPrueba paso : pasos) {
                    String nombreLimpio = limpiar(paso.getNombre());

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(java.awt.Color.BLACK);
                    cs.newLineAtOffset(55, y);
                    cs.showText(nombreLimpio);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                    cs.newLineAtOffset(450, y);
                    if ("OK".equals(paso.getEstado())) {
                        cs.setNonStrokingColor(new java.awt.Color(34, 139, 34));
                        cs.showText("PASS");
                    } else {
                        cs.setNonStrokingColor(java.awt.Color.RED);
                        cs.showText("FAIL");
                    }
                    cs.endText();

                    y -= 14;
                }

                // ── VEREDICTO FINAL ──────────────────────────────────────────────
                y -= 15;

                cs.fill();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(java.awt.Color.WHITE);
                cs.newLineAtOffset(55, y + 2);

                cs.endText();

            }

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

    private int dibujarSeccion(PDPageContentStream cs, String titulo, int y) throws IOException {
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

    private int dibujarFila(PDPageContentStream cs, String clave, String valor, int y) throws IOException {
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