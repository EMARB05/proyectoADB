package com.example.View;

import java.util.concurrent.CountDownLatch;
import java.io.InputStream;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConfirmacionManualPopup {

    static {
        cargarFuentePoppins("/fonts/Poppins-Regular.ttf");
        cargarFuentePoppins("/fonts/Poppins-Medium.ttf");
        cargarFuentePoppins("/fonts/Poppins-SemiBold.ttf");
        cargarFuentePoppins("/fonts/Poppins-Bold.ttf");
    }

    private static void cargarFuentePoppins(String resourcePath) {
        try (InputStream is = ConfirmacionManualPopup.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Font.loadFont(is, 12);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Se llama desde el hilo de ejecución.
     * BLOQUEA hasta que el técnico pulsa PASS o FAIL.
     *
     * @return true = PASS, false = FAIL
     */
    public static boolean mostrarYEsperar(String nombreCompleto, Stage owner, String info) {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] resultado = { false }; // array para poder escribir desde el lambda

        String[] partes = nombreCompleto.split("  —  ", 2);
        String idPrueba = partes.length > 1 ? partes[0] : nombreCompleto;
        String descripcion = partes.length > 1 ? partes[1] : "";

        Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UNDECORATED);
            popup.initOwner(owner);

            VBox root = new VBox(16);
            root.setPadding(new Insets(28));
            root.setPrefWidth(460);
            root.setStyle(
                    "-fx-background-color: #1e1e2e;" +
                            "-fx-border-color: #45475a;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;");

            // Etiqueta "MANUAL"
            Label lblManual = new Label("● PRUEBA MANUAL");
            lblManual.setFont(Font.font(null, FontWeight.BOLD, 11));
            lblManual.setTextFill(Color.web("#f9e2af"));

            // ID + descripción
            Label lblId = new Label(idPrueba);
            lblId.setFont(Font.font(null, FontWeight.BOLD, 15));
            lblId.setTextFill(Color.web("#cdd6f4"));

            Label lblDesc = new Label(descripcion);
            lblDesc.setTextFill(Color.web("#a6adc8"));
            lblDesc.setFont(Font.font(13));
            lblDesc.setWrapText(true);

            // Instrucción
            Label lblInstruccion = new Label(
                    "Realiza la acción en el dispositivo y confirma el resultado.");
            lblInstruccion.setTextFill(Color.web("#6c7086"));
            lblInstruccion.setFont(Font.font(12));
            lblInstruccion.setWrapText(true);

            // Botones
            Button btnPass = new Button("✔  PASS");
            btnPass.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: #a6e3a1; -fx-border-radius: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-text-fill: #a6e3a1;" +
                            "-fx-font-size: 13px; -fx-font-weight: bold;" +
                            "-fx-padding: 10 28 10 28;");

            Button btnFail = new Button("✘  FAIL");
            btnFail.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: #f38ba8; -fx-border-radius: 6;" +
                            "-fx-background-radius: 6;" +
                            "-fx-text-fill: #f38ba8;" +
                            "-fx-font-size: 13px; -fx-font-weight: bold;" +
                            "-fx-padding: 10 28 10 28;");

            btnPass.setOnAction(e -> {
                resultado[0] = true;
                popup.close();
                latch.countDown(); // ← desbloquea el hilo de ejecución
            });

            btnFail.setOnAction(e -> {
                resultado[0] = false;
                popup.close();
                latch.countDown();
            });

            HBox botones = new HBox(16, btnFail, btnPass);
            botones.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(lblManual, lblId, new Separator(), lblDesc, lblInstruccion);

            if (info != null && !info.isBlank()) {
                Label lblInfoTitulo = new Label("Output:");
                lblInfoTitulo.setFont(Font.font(null, FontWeight.BOLD, 11));
                lblInfoTitulo.setTextFill(Color.web("#a6adc8"));

                TextArea txtInfo = new TextArea(info);
                txtInfo.setEditable(false);
                txtInfo.setWrapText(true);
                txtInfo.setPrefRowCount(14);
                txtInfo.setPrefWidth(520);
                txtInfo.setStyle(
                    "-fx-background-color: #f8f8f8;" +
                        "-fx-control-inner-background: #f8f8f8;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-family: 'Poppins';" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                                "-fx-border-color: #45475a;" +
                                "-fx-border-radius: 4;" +
                                "-fx-background-radius: 4;");
                root.getChildren().addAll(lblInfoTitulo, txtInfo);
                root.setPrefWidth(560);
            }
            root.getChildren().addAll(new Separator(), botones);


            popup.setScene(new Scene(root));
            popup.show();
        });

        // El hilo de ejecución espera aquí — la UI sigue respondiendo
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return resultado[0];
    }

    public static boolean mostrarYEsperar(String nombreCompleto, Stage owner){
        return mostrarYEsperar(nombreCompleto, owner,null);
    }
}
