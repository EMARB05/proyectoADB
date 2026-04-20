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
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    private LogcatManager logcatManager;
    private Dispositivo dispositivo;
    private Consumer<String> onGuardado;
    private List<String> lineasCongeladas = new ArrayList<>();

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
            List<String> base = chkSoloRelevantes.isSelected()
                    ? lineasCongeladas.stream()
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

                if (chkSoloRelevantes.isSelected()) {
                        lblContador.setText(lineas.size() + " líneas (solo warnings y errores)"
                            + (clave.isBlank() ? "" : " con \"" + clave + "\""));
                } else {
                    lblContador.setText(lineas.size() + " líneas"
                            + (clave.isBlank() ? "" : " con \"" + clave + "\""));
                }

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

            Path archivo = logcatManager.guardar(nombreModelo, aGuardar, soloWEF, palabraClave);

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