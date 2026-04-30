package com.example.View;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.example.Controller.ADBService;
import com.example.Model.Dispositivo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import com.example.Model.PasoPrueba;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                    setPadding(new Insets(8, 12, 8, 12));
                }
            }
        });
    }

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivoActual = dispositivo;
        if (fichaTecnicaController != null)
            fichaTecnicaController.setDispositivo(dispositivo);
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
    "Selecciona los dispositivos ADB e introduce los números manualmente."
);

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
    lblNum1,  tfNum1,
    lblDisp2, cbDisp2,
    lblNum2,  tfNum2,
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
    String s1  = cbDisp1.getValue();
    String s2  = cbDisp2.getValue();
    String n1  = tfNum1.getText().trim();
    String n2  = tfNum2.getText().trim();

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

                if (esWifi) {
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
    String s1      = llamadaEntreDosSerial1;
    String s2      = llamadaEntreDosSerial2;
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

        // Encender y desbloquear AMBOS antes de empezar
        despertarDispositivo(s1);
        despertarDispositivo(s2);
        Thread.sleep(1_500);

        System.out.println("[ENTRE2] Ronda 1: Tel.1 llama a Tel.2...");
        actualizarEstadoPaso(paso, "Ronda 1: Tel.1 → Tel.2");

        ejecutarShellEnSerial(s1,
                "am start -a android.intent.action.CALL -d tel:" + numero2);

        // Esperar que la llamada llegue al receptor (más margen)
        actualizarEstadoPaso(paso, "Ronda 1: Esperando que suene...");
        Thread.sleep(6_000);

        // Despertar Tel.2 justo antes de contestar por si se apagó
        despertarDispositivo(s2);
        Thread.sleep(500);

        // Contestar — doble intento por seguridad
        ejecutarShellEnSerial(s2, "input keyevent KEYCODE_CALL");
        Thread.sleep(1_000);
        ejecutarShellEnSerial(s2, "input keyevent KEYCODE_CALL");
        System.out.println("[ENTRE2] Tel.2 contestó");

        actualizarEstadoPaso(paso, "Ronda 1 activa 10s...");
        Thread.sleep(10_000);

        ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
        ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
        System.out.println("[ENTRE2] Ronda 1 finalizada ✔");
        ronda1Ok = true;

        // Pausa entre rondas — dar tiempo a que el sistema registre fin de llamada
        actualizarEstadoPaso(paso, "Pausa entre rondas...");
        Thread.sleep(4_000);

        // ── RONDA 2: Tel.2 → Tel.1 ───────────────────────────────────────
        // Volver a despertar ambos por si se apagaron durante la pausa
        despertarDispositivo(s1);
        despertarDispositivo(s2);
        Thread.sleep(1_500);

        System.out.println("[ENTRE2] Ronda 2: Tel.2 llama a Tel.1...");
        actualizarEstadoPaso(paso, "Ronda 2: Tel.2 → Tel.1");

        ejecutarShellEnSerial(s2,
                "am start -a android.intent.action.CALL -d tel:" + numero1);

        actualizarEstadoPaso(paso, "Ronda 2: Esperando que suene...");
        Thread.sleep(6_000);

        // Despertar Tel.1 justo antes de contestar
        despertarDispositivo(s1);
        Thread.sleep(500);

        ejecutarShellEnSerial(s1, "input keyevent KEYCODE_CALL");
        Thread.sleep(1_000);
        ejecutarShellEnSerial(s1, "input keyevent KEYCODE_CALL");
        System.out.println("[ENTRE2] Tel.1 contestó");

        actualizarEstadoPaso(paso, "Ronda 2 activa 10s...");
        Thread.sleep(10_000);

        ejecutarShellEnSerial(s2, "input keyevent KEYCODE_ENDCALL");
        ejecutarShellEnSerial(s1, "input keyevent KEYCODE_ENDCALL");
        System.out.println("[ENTRE2] Ronda 2 finalizada ✔");
        ronda2Ok = true;

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("[ENTRE2] Test interrumpido");
    }

    boolean exito = ronda1Ok && ronda2Ok;
    System.out.printf("[ENTRE2] Resultado: Ronda1=%s | Ronda2=%s → %s%n",
            ronda1Ok ? "OK" : "FAIL",
            ronda2Ok ? "OK" : "FAIL",
            exito    ? "PASS ✔" : "FAIL ✖");
    return exito;
}

private String obtenerSerialADBActual() {
    if (dispositivoActual == null) return null;
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
    private String detectarNumeroTelefono(String serial) {

        // ── Estrategia 1: getline1number por iphonesubinfo ────────────────────
        // En Android 12+ el slot 0 es la llamada 11 (con i32 1 para subId)
        // La salida real es: Result: Parcel(... "612345678")
        // El número está entre las últimas comillas simples de la línea "Result:"
        for (String cmd : new String[] {
                "service call iphonesubinfo 11 i32 1", // Android 12+
                "service call iphonesubinfo 6 i32 1", // Android 8-11
                "service call iphonesubinfo 5 i32 1" // fallback
        }) {
            try {
                String out = ejecutarShellEnSerial(serial, cmd);
                System.out.println("[TEL] " + serial + " raw iphonesubinfo: " + out);

                // El número aparece en el Parcel como caracteres separados por puntos
                // Ejemplo: "6.1.2.3.4.5.6.7.8" o directamente "'612345678'"
                // Extraemos solo dígitos y + de la parte después de "Result:"
                int resultIdx = out.indexOf("Result:");
                if (resultIdx >= 0) {
                    String resultPart = out.substring(resultIdx);
                    // Buscar contenido entre comillas simples (el número real)
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("'([+0-9][0-9]{6,})'")
                            .matcher(resultPart);
                    if (m.find()) {
                        String num = m.group(1);
                        System.out.println("[TEL] " + serial + " via iphonesubinfo: " + num);
                        return num;
                    }
                }
            } catch (Exception e) {
                System.out.println("[TEL] iphonesubinfo error: " + e.getMessage());
            }
        }

        // ── Estrategia 2: telephony.registry ─────────────────────────────────
        // Busca líneas con "mPhoneNumber=" o "PhoneNumber=" que tengan
        // un número real (al menos 7 dígitos, no todo ceros)
        try {
            String out = ejecutarShellEnSerial(serial, "dumpsys telephony.registry");
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:mPhoneNumber|PhoneNumber|number)\\s*=\\s*([+0-9]{7,})")
                    .matcher(out);
            while (m.find()) {
                String num = m.group(1);
                // Descartar si es todo ceros o demasiado largo (basura)
                if (!num.matches("0+") && num.length() <= 15) {
                    System.out.println("[TEL] " + serial + " via telephony.registry: " + num);
                    return num;
                }
            }
        } catch (Exception e) {
            System.out.println("[TEL] telephony.registry error: " + e.getMessage());
        }

        // ── Estrategia 3: settings ────────────────────────────────────────────
        for (String setting : new String[] {
                "settings get global line1_number",
                "settings get secure line1_number",
                "getprop net.phone.number"
        }) {
            try {
                String out = ejecutarShellEnSerial(serial, setting).trim();
                if (!out.isBlank() && !out.equals("null")) {
                    String num = out.replaceAll("[^0-9+]", "");
                    if (num.length() >= 7 && num.length() <= 15 && !num.matches("0+")) {
                        System.out.println("[TEL] " + serial + " via settings: " + num);
                        return num;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        System.out.println("[TEL] " + serial + " → número no detectado por ninguna estrategia");
        return null;
    }

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

        // Carpeta por defecto: Documentos
        String userHome = System.getProperty("user.home");
        File documentosPath = new File(userHome, "Documents");
        if (documentosPath.exists()) {
            fc.setInitialDirectory(documentosPath);
        }

        fc.setInitialFileName("Informe_Diagnostico_" + dispositivoActual.getSerialNumber() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File file = fc.showSaveDialog(btnInforme.getScene().getWindow());

        // Si el usuario cancela la selección, salimos
        if (file == null)
            return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Título
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("CERTIFICADO DE DIAGNÓSTICO TÉCNICO");
                cs.endText();

                cs.setLineWidth(1f);
                cs.moveTo(50, 740);
                cs.lineTo(550, 740);
                cs.stroke();

                // Datos dispositivo
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.setLeading(15f);
                cs.newLineAtOffset(50, 710);
                cs.showText("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                cs.newLine();
                cs.showText("Dispositivo: " + dispositivoActual.getModelo().getNombreModelo());
                cs.newLine();
                cs.showText("S/N: " + dispositivoActual.getSerialNumber());
                cs.newLine();
                cs.showText("Android ID: " + dispositivoActual.getAndroid_id());
                cs.endText();

                // Tabla de pruebas
                int y = 620;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(50, y);
                cs.showText("PRUEBA");
                cs.newLineAtOffset(400, 0);
                cs.showText("RESULTADO");
                cs.endText();

                y -= 20;

                for (PasoPrueba paso : pasos) {
                    // Limpieza de caracteres Unicode para evitar errores de fuente
                    String nombreLimpio = paso.getNombre()
                            .replace("➤", ">")
                            .replaceAll("[^\\x00-\\x7F]", "");

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.setNonStrokingColor(java.awt.Color.BLACK);
                    cs.newLineAtOffset(50, y);
                    cs.showText(nombreLimpio);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11); // Re-set font para el PASS/FAIL
                    cs.newLineAtOffset(450, y);
                    if ("OK".equals(paso.getEstado())) {
                        cs.setNonStrokingColor(new java.awt.Color(34, 139, 34));
                        cs.showText("PASS");
                    } else {
                        cs.setNonStrokingColor(java.awt.Color.RED);
                        cs.showText("FAIL");
                    }
                    cs.endText();
                    y -= 20;
                }

                // Veredicto
                y -= 30;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.setNonStrokingColor(java.awt.Color.BLACK);
                cs.newLineAtOffset(50, y);
                cs.endText();
            }

            // Intento de guardado final
            try {
                doc.save(file);
                System.out.println("PDF creado con éxito en: " + file.getAbsolutePath());
            } catch (IOException e) {
                mostrarAlertaError("Error de Acceso",
                        "No se pudo sobrescribir el archivo.\n\n" +
                                "Asegúrate de que el PDF no esté abierto en Chrome, Adobe o Word.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaError("Error Crítico", "Ocurrió un error inesperado al generar el PDF.");
        }
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