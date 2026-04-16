package com.example.View;

import com.example.Controller.ADBService;
import com.example.Model.Dispositivo;
import javafx.fxml.FXML;
import com.example.Model.PasoPrueba;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

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

        // Creamos unos pasos de prueba por defecto
        pasos.add(new PasoPrueba("Levantar Interfaz WiFi", "shell svc wifi enable"));
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
                        setStyle("-fx-text-fill: #cdd6f4; -fx-background-color: transparent;");
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
    private void ejecutarScript() {
        if (dispositivoActual == null || pasos.isEmpty())
            return;
        // Desactivamos el botón para que no pulsen dos veces
        // btnEjecutar.setDisable(true);
        new Thread(() -> {
            ADBService adb = new ADBService(); // O usa una instancia inyectada

            for (PasoPrueba paso : pasos) {
                // 1. Cambiamos estado a "EJECUTANDO" (En el hilo de UI)
                javafx.application.Platform.runLater(() -> {
                    paso.setEstado("EJECUTANDO");
                    listaPasos.refresh();
                });

                // 2. Ejecutamos el comando real (Este hilo se queda esperando aquí)
                boolean exito = adb.ejecutarPasoSync(dispositivoActual.getSerialNumber(), paso.getComando());

                // 3. Esperamos un segundo para que el usuario vea qué pasa (opcional)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                // 4. Actualizamos el resultado final
                javafx.application.Platform.runLater(() -> {
                    paso.setEstado(exito ? "OK" : "ERROR");
                    listaPasos.refresh();
                });
            }

            Platform.runLater(() -> {
                btnInforme.setDisable(false); // ¡Ya podemos guardar los resultados!
                btnEjecutar.setDisable(false);
                System.out.println("Informe listo para generar.");
            });

            // Al finalizar, podrías habilitar el botón de "Generar Informe"
            System.out.println("Secuencia completada.");
        }).start();
    }

    // --- SCRIPTS DE RED ---
    @FXML
    private void addWifiOn() {
        pasos.add(new PasoPrueba("Encender WiFi", "shell svc wifi enable"));
    }

    @FXML
    private void addWifiOff() {
        pasos.add(new PasoPrueba("Apagar WiFi", "shell svc wifi disable"));
    }

    @FXML
    private void addPingStep() {
        pasos.add(new PasoPrueba("Ping Google", "shell ping -c 3 8.8.8.8"));
    }

    // --- PRUEBAS DE HARDWARE ---
    @FXML
    private void addSoundTest() {
        // Esto abre el selector de tonos y reproduce uno para probar altavoz
        pasos.add(new PasoPrueba("Probar Altavoz",
                "shell am start -a android.intent.action.VIEW -d content://settings/system/notification_sound -t audio/mp3"));
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
    private void generarInforme() {
        if (dispositivoActual == null)
            return;

        // 1. Definir el nombre del archivo (usamos el serial y la fecha para que sea
        // único)
        String nombreArchivo = "Reporte_" + dispositivoActual.getSerialNumber() + ".txt";

        // 2. Usar un FileChooser para que el usuario elija dónde guardarlo
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar Informe de Diagnóstico");
        fileChooser.setInitialFileName(nombreArchivo);
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Archivo de Texto", "*.txt"));

        java.io.File file = fileChooser.showSaveDialog(btnInforme.getScene().getWindow());

        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                // Escribir cabecera
                writer.println("==========================================");
                writer.println("       REPORTE TÉCNICO DE DIAGNÓSTICO     ");
                writer.println("==========================================");
                writer.println("Fecha: " + java.time.LocalDateTime.now());
                writer.println("Dispositivo: " + dispositivoActual.getModelo().getNombreModelo());
                writer.println("Marca: " + dispositivoActual.getModelo().getMarca().getNombre());
                writer.println("S/N: " + dispositivoActual.getSerialNumber());
                writer.println("Android ID: " + dispositivoActual.getAndroid_id());
                writer.println("------------------------------------------");
                writer.println("RESULTADOS DE LA SECUENCIA DE PRUEBAS:");
                writer.println("------------------------------------------");

                // Recorrer los pasos de la lista y escribir su estado
                for (PasoPrueba paso : pasos) {
                    String check = paso.getEstado().equals("OK") ? "[ PASS ]" : "[ FAIL ]";
                    writer.printf("%-30s %s%n", paso.getNombre(), check);
                }

                writer.println("------------------------------------------");
                writer.println("ESTADO FINAL: " + (verificarSiTodoOk() ? "APROBADO" : "RECHAZADO"));
                writer.println("==========================================");

                System.out.println("Informe guardado en: " + file.getAbsolutePath());

            } catch (java.io.IOException ex) {
                System.err.println("Error al guardar el informe: " + ex.getMessage());
            }
        }
    }

    // Método auxiliar para el estado final
    private boolean verificarSiTodoOk() {
        return pasos.stream().allMatch(p -> p.getEstado().equals("OK"));
    }
}