package com.example.View;

import java.io.File;
import java.io.IOException;

import com.example.Controller.ADBService;
import com.example.Controller.ScrcpyService;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

public class GestionPantallaController {

    @FXML
    private ComboBox<String> comboResolucion;

    @FXML
    private StackPane rootPane;

    @FXML
    private Button btnIniciarGrabacion;
    @FXML
    private Button btnDetenerGrabacion;

    private Process grabacionProceso;

    private String serial;
    private final ADBService adbService = new ADBService();
    private final ScrcpyService scrcpyService = new ScrcpyService();

    private int rotacionActual = 0;

    public void setSerial(String serial) {
        this.serial = serial;
        sincronizarEstadoInicial();
    }

    /**
     * Obtiene el estado real del dispositivo al conectar o refrescar.
     */
    public void sincronizarEstadoInicial() {
        new Thread(() -> {
            try {
                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                String rotStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell settings get system user_rotation");

                Platform.runLater(() -> {
                    actualizarUIResolucion(resStatus);
                    try {
                        rotacionActual = Integer.parseInt(rotStatus.trim()) * 90;
                    } catch (NumberFormatException e) {
                        rotacionActual = 0;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void cambiarResolucion(ActionEvent event) {
        String seleccion = comboResolucion.getValue();
        if (seleccion == null || serial == null)
            return;

        String resolucion = seleccion.split(" ")[0];

        new Thread(() -> {
            try {
                // Secuencia limpia: Reset -> Nueva Res -> Nuevo DPI
                adbService.ejecutarComandoSincrono(serial, "shell wm size reset");
                adbService.ejecutarComandoSincrono(serial, "shell wm size " + resolucion);

                int nuevoDpi = calcularDpiProporcional(resolucion);
                adbService.ejecutarComandoSincrono(serial, "shell wm density " + nuevoDpi);

                // Esperamos un momento a que el sistema procese y refrescamos UI
                Thread.sleep(500);
                String nuevoEstado = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                Platform.runLater(() -> actualizarUIResolucion(nuevoEstado));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void resetearResolucion(ActionEvent event) {
        if (serial == null)
            return;

        new Thread(() -> {
            try {
                // Reset de ambos parámetros para evitar interfaces deformes
                adbService.ejecutarComandoSincrono(serial, "shell wm size reset");
                adbService.ejecutarComandoSincrono(serial, "shell wm density reset");

                Thread.sleep(600); // Tiempo para que el WindowManager respire

                String resStatus = adbService.ejecutarComandoSincrono(serial, "shell wm size");
                Platform.runLater(() -> actualizarUIResolucion(resStatus));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Procesa el string de ADB "Physical size: ... Override size: ..."
     * y selecciona el valor correcto en el ComboBox.
     */
    private void actualizarUIResolucion(String rawStatus) {
        try {
            // Buscamos la última línea que contenga la resolución activa
            String linea = rawStatus.trim().lines()
                    .reduce((first, second) -> second)
                    .orElse("");

            // Extraemos solo el formato 000x000
            String resActual = linea.replaceAll(".*: ", "").trim();

            comboResolucion.getItems().stream()
                    .filter(item -> item.contains(resActual))
                    .findFirst()
                    .ifPresent(match -> comboResolucion.setValue(match));
        } catch (Exception e) {
            System.err.println("Error al parsear resolución: " + rawStatus);
        }
    }

    private int calcularDpiProporcional(String resolucion) {
        try {
            int ancho = Integer.parseInt(resolucion.split("x")[0]);
            if (ancho <= 480)
                return 160;
            if (ancho <= 720)
                return 240;
            if (ancho <= 1080)
                return 420;
            return 560;
        } catch (Exception e) {
            return 480;
        }
    }

    @FXML
    private void rotarPantalla90(ActionEvent event) {
        new Thread(() -> {
            adbService.ejecutarComandoSincrono(serial, "shell settings put system accelerometer_rotation 0");
            rotacionActual = (rotacionActual + 90) % 360;
            adbService.ejecutarComandoSincrono(serial,
                    "shell settings put system user_rotation " + (rotacionActual / 90));
        }).start();
    }

    @FXML
    private void flipPantalla(ActionEvent event) {
        new Thread(() -> {
            adbService.ejecutarComandoSincrono(serial, "shell settings put system accelerometer_rotation 0");
            int valorSistema = (rotacionActual / 90 + 2) % 4;
            rotacionActual = valorSistema * 90;
            adbService.ejecutarComandoSincrono(serial, "shell settings put system user_rotation " + valorSistema);
        }).start();
    }

    @FXML
    private void onLanzarScrcpy(ActionEvent event) {
        if (serial != null && !serial.isEmpty()) {
            scrcpyService.launch(serial);
        }
    }

    private File seleccionarCarpeta() {
        DirectoryChooser dChooser = new DirectoryChooser();
        dChooser.setTitle("Seleccionar carpeta para guardar");
        return dChooser.showDialog(btnIniciarGrabacion.getScene().getWindow());
    }

    @FXML
    public void onCapturarPantalla() {
        File carpeta = seleccionarCarpeta();
        if (carpeta != null) {
            new Thread(() -> {
                try {
                    adbService.capturarPantalla(serial, carpeta.getAbsolutePath());
                    Platform.runLater(() -> mostrarToast("Captura guardada con éxito"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    public void onIniciarGrabacion() {
        try {
            grabacionProceso = adbService.iniciarGrabacion(serial);

            btnIniciarGrabacion.setDisable(true);
            btnDetenerGrabacion.setDisable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onDetenerGrabacion() {
        if (grabacionProceso != null) {
            // 1. PARAR EL VIDEO YA (Llamada rápida al service)
            adbService.enviarSenalParada(serial);

            // 2. ELEGIR CARPETA (El video ya no crece en el móvil)
            File carpeta = seleccionarCarpeta();

            if (carpeta != null) {
                new Thread(() -> {
                    try {
                        // 3. DESCARGAR (Llamada al service para el trabajo pesado)
                        adbService.descargarYLimpiar(serial, carpeta.getAbsolutePath());

                        Platform.runLater(() -> {
                            btnIniciarGrabacion.setDisable(false);
                            btnDetenerGrabacion.setDisable(true);
                            mostrarToast("🎥 Vídeo guardado correctamente");
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            grabacionProceso = null;
        }
    }

    /**
     * Muestra un Toast flotante en la parte inferior del rootPane.
     */
    private void mostrarToast(String mensaje) {
        if (rootPane == null)
            return;

        // Crear el Label con tu estilo Mocha
        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);

        // Posicionamiento en el StackPane
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));

        rootPane.getChildren().add(toast);

        // Secuencia de animación
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