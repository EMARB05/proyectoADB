package com.example.View;

import com.example.Controller.LogcatManager;
import com.example.Model.Dispositivo;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import java.util.prefs.Preferences;

public class LogcatController {

    @FXML
    private TextField txtFiltro;
    @FXML
    private TextArea areaLogs;
    @FXML
    private Label lblContador;
    @FXML
    private Label lblEstado;
    @FXML
    private CheckBox chkSoloRelevantes;
    @FXML
    private StackPane rootPane;
    @FXML
    private Label lblDirectorio;

    // Directorio de destino — por defecto el escritorio
    private static final String PREF_DIRECTORIO = "logcat_directorio";
    private final Preferences prefs = Preferences.userNodeForPackage(LogcatController.class);
    private Path directorioDestino;

    private LogcatManager logcatManager;
    private Dispositivo dispositivo;
    private Consumer<String> onGuardado;
    private List<String> lineasCongeladas = new ArrayList<>();

    @FXML
    public void initialize() {
        // Carga el directorio guardado, si no hay ninguno usa el escritorio
        String guardado = prefs.get(PREF_DIRECTORIO,
                Paths.get(System.getProperty("user.home"), "Desktop").toString());
        directorioDestino = Paths.get(guardado);
        lblDirectorio.setText("Destino: " + directorioDestino.toString());
    }

    public void setLogcatManager(LogcatManager manager, Dispositivo dispositivo) {
        this.logcatManager = manager;
        this.dispositivo = dispositivo;
        lblEstado.setText("● Capturando logs de " + dispositivo.getSerialNumber());

        // Congela la lista en el momento de abrir la ventana
        congelarYRefrescar();
    }

    private void congelarYRefrescar() {
        // Copia la lista viva en este momento
        lineasCongeladas = new ArrayList<>(logcatManager.getLineasCapturadas());
        // Refresca la vista respetando el estado del checkbox
        onFiltrar();
    }

    public void setOnGuardado(Consumer<String> callback) {
        this.onGuardado = callback;
    }

    @FXML
    private void onFiltrar() {
        String clave = txtFiltro.getText().trim();

        new Thread(() -> {
            // Filtrado sobre la lista CONGELADA (esto mantiene tu funcionalidad intacta)
            List<String> base = chkSoloRelevantes.isSelected()
                    ? lineasCongeladas.stream()
                            .filter(l -> !l.startsWith("- waiting") && !l.startsWith("error:")) // Filtra basura de red
                            .filter(l -> l.contains(" W/") || l.contains(" W ") ||
                                    l.contains(" E/") || l.contains(" E ") ||
                                    l.contains(" F/") || l.contains(" F "))
                            .collect(java.util.stream.Collectors.toList())
                    : lineasCongeladas;

            List<String> lineas = clave.isBlank()
                    ? base
                    : base.stream()
                            .filter(l -> l.toLowerCase().contains(clave.toLowerCase()))
                            .collect(java.util.stream.Collectors.toList());

            String texto = String.join("\n", lineas);
            Platform.runLater(() -> {
                areaLogs.setText(texto);
                areaLogs.setScrollTop(Double.MAX_VALUE);

                lblContador.setText(lineas.size() + " líneas" +
                        (chkSoloRelevantes.isSelected() ? " (solo relevantes)" : "") +
                        (clave.isBlank() ? "" : " con \"" + clave + "\""));
            });
        }).start();
    }

    @FXML
    private void onActualizar() {
        congelarYRefrescar();
    }

    @FXML
    private void onGuardar() {
        if (lineasCongeladas.isEmpty()) {
            lblEstado.setText("⚠ No hay líneas capturadas todavía");
            return;
        }

        try {
            String nombreModelo = dispositivo.getModelo().getNombreModelo();
            String palabraClave = txtFiltro.getText().trim();
            boolean soloWEF = chkSoloRelevantes.isSelected();

            List<String> aGuardar = soloWEF
                    ? lineasCongeladas.stream()
                            .filter(l -> l.contains(" W/") || l.contains(" W ") ||
                                    l.contains(" E/") || l.contains(" E ") ||
                                    l.contains(" F/") || l.contains(" F "))
                            .collect(java.util.stream.Collectors.toList())
                    : lineasCongeladas;

            if (!palabraClave.isBlank()) {
                aGuardar = aGuardar.stream()
                        .filter(l -> l.toLowerCase().contains(palabraClave.toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
            }

            Path archivo = logcatManager.guardar(nombreModelo, aGuardar, soloWEF, palabraClave, directorioDestino);

            // Toast en esta misma ventana en lugar de cerrarla
            mostrarToast("✓ Guardado: " + archivo.getFileName().toString());

            // Notificamos igualmente a la ficha técnica
            if (onGuardado != null) {
                onGuardado.accept(archivo.toAbsolutePath().toString());
            }

        } catch (IOException e) {
            lblEstado.setText("✗ Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void onCambiarDirectorio() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona el directorio de destino");
        chooser.setInitialDirectory(directorioDestino.toFile());

        File seleccionado = chooser.showDialog(areaLogs.getScene().getWindow());
        if (seleccionado != null) {
            directorioDestino = seleccionado.toPath();
            lblDirectorio.setText("Destino: " + directorioDestino.toString());

            // Persiste la elección para la próxima vez
            prefs.put(PREF_DIRECTORIO, directorioDestino.toString());
        }
    }

    private void mostrarToast(String mensaje) {
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
}