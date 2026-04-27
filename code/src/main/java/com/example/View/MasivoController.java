package com.example.View;

import com.example.Controller.ADBService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MasivoController {

    @FXML
    private VBox dropZone;
    @FXML
    private ListView<String> listaApks;
    @FXML
    private ListView<String> listaDestinos;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label labelEstado;
    @FXML
    private Button btnInstalar;

    private final ADBService adb = new ADBService();

    @FXML
    public void initialize() {

        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                db.getFiles().forEach(file -> {
                    String nombre = file.getName().toLowerCase();
                    // ✅ Ahora acepta los tres formatos
                    if (nombre.endsWith(".apk") || nombre.endsWith(".xapk") || nombre.endsWith(".apks")
                            || nombre.endsWith(".apkm")) {
                        listaApks.getItems().add(file.getAbsolutePath());
                    }
                });
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        refrescarDispositivos();
        listaDestinos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void ejecutarInstalacionMasiva() {
        List<String> apks = listaApks.getItems();
        List<String> seleccionados = listaDestinos.getSelectionModel().getSelectedItems();

        if (apks.isEmpty() || seleccionados.isEmpty()) {
            labelEstado.setText("⚠️ Selecciona APKs y al menos un dispositivo.");
            return;
        }

        int total = apks.size() * seleccionados.size();

        btnInstalar.setDisable(true);
        progressBar.setProgress(0);
        labelEstado.setText("Iniciando instalación...");

        new Thread(() -> {
            try {
                Map<String, String> conectados = adb.obtenerDispositivosConectados();
                int[] completadas = { 0 };

                for (String androidId : seleccionados) {
                    String serial = conectados.get(androidId);

                    if (serial == null) {
                        final String msg = "⚠️ Dispositivo no encontrado: " + androidId;
                        Platform.runLater(() -> labelEstado.setText(msg));
                        continue;
                    }

                    for (String pathApk : apks) {
                        String nombreApk = pathApk.substring(
                                Math.max(pathApk.lastIndexOf("/"), pathApk.lastIndexOf("\\")) + 1);
                        final String msgInstalando = "📦 Instalando " + nombreApk + " en " + androidId + "...";
                        Platform.runLater(() -> labelEstado.setText(msgInstalando));

                        try {
                            adb.instalarAPK(serial, pathApk);
                        } catch (Exception e) {
                            final String error = "❌ Error en " + nombreApk + " → " + androidId + ": " + e.getMessage();
                            // ✅ Añade esto para ver el error completo en consola
                            e.printStackTrace();
                            Platform.runLater(() -> labelEstado.setText(error));
                        }

                        completadas[0]++;
                        final double progreso = (double) completadas[0] / total;
                        Platform.runLater(() -> progressBar.setProgress(progreso));
                    }
                }

                Platform.runLater(() -> {
                    btnInstalar.setDisable(false);
                    labelEstado.setText("✅ Instalación completada (" + total + " operaciones).");
                    progressBar.setProgress(1.0);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnInstalar.setDisable(false);
                    labelEstado.setText("❌ Error al obtener dispositivos: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refrescarDispositivos() {
        new Thread(() -> {
            try {
                Map<String, String> dispositivos = adb.obtenerDispositivosConectados();
                Platform.runLater(() -> {
                    listaDestinos.getItems().clear();
                    listaDestinos.getItems().addAll(dispositivos.keySet());

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void limpiarLista() {
        listaApks.getItems().clear();
    }

    @FXML
    private void seleccionarTodosDispositivos() {
        listaDestinos.getSelectionModel().selectAll();
    }

    @FXML
    private void desmarcarTodosDispositivos() {
        listaDestinos.getSelectionModel().clearSelection();
    }

    @FXML
    private void cargarDispositivos() {
        refrescarDispositivos();
    }
}