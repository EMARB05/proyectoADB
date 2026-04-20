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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    private Button btnEjecutar; // Debe coincidir con el fx:id del FXML

    @FXML
    private Button btnInforme; // Debe coincidir con el fx:id del FXML

    private final ObservableList<PasoPrueba> pasos = FXCollections.observableArrayList();
    private Dispositivo dispositivoActual;

    @FXML
    public void initialize() {
        // Vinculamos la lista de la lógica con la lista de la pantalla
        listaPasos.setItems(pasos);
        pasos.add(new PasoPrueba("Preparando Dispositivo...", "shell input keyevent KEYCODE_WAKEUP && wm dismiss-keyguard"));

        // Creamos unos pasos de prueba por defecto
        pasos.add(new PasoPrueba("Levantar Interfaz WiFi", "shell svc wifi enable && sleep 6"));
        pasos.add(new PasoPrueba("Check Conectividad (Ping)", "shell ping -c 4 8.8.8.8"));
        pasos.add(new PasoPrueba("Obtener IP Local", "shell ip addr show wlan0"));
        // Personalizamos cómo se ve cada fila de la lista
        listaPasos.setCellFactory(lv -> new ListCell<PasoPrueba>() {
            @Override
            protected void updateItem(PasoPrueba item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // Mostramos el nombre y el estado
                    setText(item.getNombre().toUpperCase() + "  ➤  " + item.getEstado());

                    // Colores dinámicos según el estado
                    if (item.getEstado().equals("OK")) {
                        setStyle(
                                "-fx-text-fill: #a6e3a1; -fx-font-weight: bold; -fx-background-color: rgba(166, 227, 161, 0.1);");
                    } else if (item.getEstado().equals("ERROR")) {
                        setStyle(
                                "-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-background-color: rgba(243, 139, 168, 0.1);");
                    } else if (item.getEstado().equals("EJECUTANDO")) {
                        setStyle("-fx-text-fill: #f9e2af; -fx-background-color: rgba(249, 226, 175, 0.1);");
                    } else {
                        setStyle("-fx-text-fill: #191a1d; -fx-background-color: transparent;");
                    }

                    // Un poco de padding para que no estén pegados
                    setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
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

    @FXML
private void abrirLaboratorio() {
    try {
        // Ajusta la ruta según tu estructura de paquetes (com/example/View/...)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LaboratorioBateria.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle("AEA Suite - Laboratorio de Rendimiento");
        stage.setScene(new Scene(root));
        
        // Esto hace que la ventana sea "Modal" (opcional: bloquea la principal hasta cerrar esta)
        // stage.initModality(Modality.APPLICATION_MODAL);
        
        stage.show();
    } catch (IOException e) {
        System.err.println("No se pudo encontrar el archivo FXML del laboratorio.");
        e.printStackTrace();
    }
}

   @FXML
private void ejecutarScript() {
    if (dispositivoActual == null || pasos.isEmpty()) return;

    new Thread(() -> {
        ADBService adb = new ADBService();

        // Resuelve el serial activo por android_id — funciona por USB y WiFi
        String serialActivo;
        try {
            serialActivo = adb.getSerialActivo(dispositivoActual.getAndroid_id());
        } catch (IOException e) {
            serialActivo = dispositivoActual.getSerialNumber(); // fallback al de la BD
            System.out.println("[DIAG] No se pudo resolver serial activo, usando BD: " + serialActivo);
        }

        final String serial = serialActivo;
        System.out.println("[DIAG] Ejecutando script con serial: " + serial);

        for (PasoPrueba paso : pasos) {
            javafx.application.Platform.runLater(() -> {
                paso.setEstado("EJECUTANDO");
                listaPasos.refresh();
            });

            boolean exito = adb.ejecutarPasoSync(serial, paso.getComando());

            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            javafx.application.Platform.runLater(() -> {
                paso.setEstado(exito ? "OK" : "ERROR");
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
    // --- SCRIPTS DE RED ---
    @FXML
    private void addWifiOn() {
        pasos.add(new PasoPrueba("Encender WiFi", "shell svc wifi enable && sleep 7"));
    }

    @FXML
    private void addWifiOff() {
        pasos.add(new PasoPrueba("Apagar WiFi", "shell svc wifi disable && sleep 2"));
    }

    @FXML
    private void addPingStep() {
        pasos.add(new PasoPrueba("Ping Google", "shell ping -c 3 8.8.8.8"));
    }

    // --- PRUEBAS DE HARDWARE ---
    @FXML
    private void addSoundTest() {
        // Abrir -> Esperar 3 segundos (para que suene) -> Cerrar -> Esperar 1 segundo
        // (pausa de seguridad)
        pasos.add(new PasoPrueba("Probar Altavoz",
                "shell am start -a android.intent.action.VIEW -d content://settings/system/notification_sound -t audio/* "
                        +
                        "&& sleep 2 && input keyevent 4 && sleep 1"));
    }

    @FXML
    private void addCallTest() {
        // Abre el marcador con un número de test
        pasos.add(new PasoPrueba("Probar Marcador/Llamada",
                "shell am start -a android.intent.action.CALL -d tel:+123456789"));
    }

    @FXML
    private void addVibrateStep() {
        // El truco del swipe largo para forzar la vibración háptica
        pasos.add(new PasoPrueba("Probar Vibración", "shell input swipe 500 500 501 501 1500"));
    }

    @FXML
    private void limpiarPasos() {
        pasos.clear();
        btnInforme.setDisable(true); // Si no hay pasos, no hay informe
    }

    @FXML
private void btnGestionarClick() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LaboratorioBateria.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("Análisis de Rendimiento - AEA Suite");
        stage.setScene(scene);
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    @FXML
    private void generarInformePDF() {
        if (dispositivoActual == null || pasos.isEmpty())
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Informe PDF");
        fileChooser.setInitialFileName("Informe_Diagnostico_" + dispositivoActual.getSerialNumber() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(btnInforme.getScene().getWindow());

        if (file != null) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // --- CABECERA ---
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("CERTIFICADO DE DIAGNÓSTICO TÉCNICO");
                    contentStream.endText();

                    // --- LÍNEA DE SEPARACIÓN ---
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(50, 740);
                    contentStream.lineTo(550, 740);
                    contentStream.stroke();

                    // --- DATOS DEL DISPOSITIVO ---
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.setLeading(15f);
                    contentStream.newLineAtOffset(50, 710);

                    contentStream.showText(
                            "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    contentStream.newLine();
                    contentStream.showText("Dispositivo: " + dispositivoActual.getModelo().getNombreModelo());
                    contentStream.newLine();
                    contentStream.showText("S/N: " + dispositivoActual.getSerialNumber());
                    contentStream.newLine();
                    contentStream.showText("Android ID: " + dispositivoActual.getAndroid_id());
                    contentStream.endText();

                    // --- TABLA DE RESULTADOS ---
                    int yPosition = 620;
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("PRUEBA");
                    contentStream.newLineAtOffset(400, 0);
                    contentStream.showText("RESULTADO");
                    contentStream.endText();

                    yPosition -= 20;

                    for (PasoPrueba paso : pasos) {
                        // 1. Escribir el nombre de la prueba (en negro)
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, 11);
                        contentStream.setNonStrokingColor(java.awt.Color.BLACK); // Método moderno
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText(paso.getNombre());
                        contentStream.endText();

                        // 2. Escribir el resultado (en color)
                        contentStream.beginText();
                        contentStream.newLineAtOffset(450, yPosition);

                        if ("OK".equals(paso.getEstado())) {
                            // Usamos la constante de Color o uno personalizado
                            contentStream.setNonStrokingColor(new java.awt.Color(34, 139, 34)); // Un verde bosque
                            contentStream.showText("PASS");
                        } else {
                            contentStream.setNonStrokingColor(java.awt.Color.RED); // Rojo estándar
                            contentStream.showText("FAIL");
                        }
                        contentStream.endText();

                        yPosition -= 20;
                    }

                    // --- ESTADO FINAL ---
                    yPosition -= 30;
                    boolean todoOk = verificarSiTodoOk();
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Veredicto Final: " + (todoOk ? "APROBADO" : "FALLIDO"));
                    contentStream.endText();
                }

                document.save(file);
                System.out.println("PDF Creado con éxito.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean verificarSiTodoOk() {
        // Si la lista está vacía, no podemos decir que esté OK
        if (pasos.isEmpty())
            return false;

        // Recorre todos los pasos y mira si alguno NO es "OK"
        for (PasoPrueba paso : pasos) {
            if (!"OK".equals(paso.getEstado())) {
                return false;
            }
        }
        return true; // Si llega aquí, es que todos están perfectos
    }

    /*
     * @FXML
     * private void generarInforme() {
     * if (dispositivoActual == null)
     * return;
     * 
     * // 1. Definir el nombre del archivo (usamos el serial y la fecha para que sea
     * // único)
     * String nombreArchivo = "Reporte_" + dispositivoActual.getSerialNumber() +
     * ".txt";
     * 
     * // 2. Usar un FileChooser para que el usuario elija dónde guardarlo
     * javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
     * fileChooser.setTitle("Guardar Informe de Diagnóstico");
     * fileChooser.setInitialFileName(nombreArchivo);
     * fileChooser.getExtensionFilters()
     * .add(new javafx.stage.FileChooser.ExtensionFilter("Archivo de Texto",
     * "*.txt"));
     * 
     * java.io.File file =
     * fileChooser.showSaveDialog(btnInforme.getScene().getWindow());
     * 
     * if (file != null) {
     * try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
     * // Escribir cabecera
     * writer.println("==========================================");
     * writer.println("       REPORTE TÉCNICO DE DIAGNÓSTICO     ");
     * writer.println("==========================================");
     * writer.println("Fecha: " + java.time.LocalDateTime.now());
     * writer.println("Dispositivo: " +
     * dispositivoActual.getModelo().getNombreModelo());
     * writer.println("Marca: " +
     * dispositivoActual.getModelo().getMarca().getNombre());
     * writer.println("S/N: " + dispositivoActual.getSerialNumber());
     * writer.println("Android ID: " + dispositivoActual.getAndroid_id());
     * writer.println("------------------------------------------");
     * writer.println("RESULTADOS DE LA SECUENCIA DE PRUEBAS:");
     * writer.println("------------------------------------------");
     * 
     * // Recorrer los pasos de la lista y escribir su estado
     * for (PasoPrueba paso : pasos) {
     * String check = paso.getEstado().equals("OK") ? "[ PASS ]" : "[ FAIL ]";
     * writer.printf("%-30s %s%n", paso.getNombre(), check);
     * }
     * 
     * writer.println("------------------------------------------");
     * writer.println("ESTADO FINAL: " + (verificarSiTodoOk() ? "APROBADO" :
     * "RECHAZADO"));
     * writer.println("==========================================");
     * 
     * System.out.println("Informe guardado en: " + file.getAbsolutePath());
     * 
     * } catch (java.io.IOException ex) {
     * System.err.println("Error al guardar el informe: " + ex.getMessage());
     * }
     * }
     * }
     * 
     * // Método auxiliar para el estado final
     * private boolean verificarSiTodoOk() {
     * return pasos.stream().allMatch(p -> p.getEstado().equals("OK"));
     * }
     */
}