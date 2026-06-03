package com.example.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;

import java.util.List;

import com.example.View.ContactosConfigPopup.DispositivoCombo;

public class MensajesConfigPopup {

        /**
         * Muestra el popup de configuración del bloque de mensajería.
         * Reutiliza los números de contactos si ya están configurados.
         *
         * @param otrosDispositivos    seriales de otros dispositivos conectados
         * @param resultadoTelefono    array[0] = número del receptor
         * @param resultadoTelefonoDUT array[0] = número del DUT
         * @param resultadoSerial      array[0] = serial del receptor
         * @param owner                stage padre
         * @return true si confirmó, false si canceló
         */
        public static boolean mostrar(
                        List<DispositivoCombo> otrosDispositivos,
                        String[] resultadoTelefono,
                        String[] resultadoTelefonoDUT,
                        String[] resultadoSerial,
                        Stage owner) {

                boolean[] confirmado = { false };

                Stage popup = new Stage();
                popup.initModality(Modality.APPLICATION_MODAL);
                popup.initStyle(StageStyle.UNDECORATED);
                popup.initOwner(owner);

                VBox root = new VBox(14);
                root.setPadding(new Insets(24));
                root.setPrefWidth(440);
                root.setStyle(
                                "-fx-background-color: #1e1e2e;" +
                                                "-fx-border-color: #45475a;" +
                                                "-fx-border-width: 1;" +
                                                "-fx-border-radius: 8;" +
                                                "-fx-background-radius: 8;");

                // ── Título ────────────────────────────────────────────────────────
                Label lblTitulo = new Label("Configuración — Mensajería");
                lblTitulo.setFont(Font.font(null, FontWeight.BOLD, 14));
                lblTitulo.setTextFill(Color.web("#cdd6f4"));

                Label lblSubtitulo = new Label(
                                "Introduce los números que se usarán para enviar y recibir SMS/MMS.");
                lblSubtitulo.setTextFill(Color.web("#a6adc8"));
                lblSubtitulo.setFont(Font.font(11));
                lblSubtitulo.setWrapText(true);

                // ── Aviso si ya están configurados ───────────────────────────────
                boolean yaConfigurados = !resultadoTelefono[0].isBlank()
                                && !resultadoTelefonoDUT[0].isBlank();
                if (yaConfigurados) {
                        Label lblAviso = new Label(
                                        "✓ Números ya configurados en el bloque de Contactos — puedes modificarlos.");
                        lblAviso.setTextFill(Color.web("#a6e3a1"));
                        lblAviso.setFont(Font.font(10));
                        lblAviso.setWrapText(true);
                        root.getChildren().add(lblAviso);
                }

                // ── Número DUT ────────────────────────────────────────────────────
                Label lblTelDUT = new Label("Número del dispositivo principal (DUT):");
                lblTelDUT.setTextFill(Color.web("#a6adc8"));
                lblTelDUT.setFont(Font.font(11));

                TextField txtTelDUT = new TextField(resultadoTelefonoDUT[0]);
                txtTelDUT.setPromptText("Ej: +34698765432");
                txtTelDUT.setStyle(
                                "-fx-background-color: #313244;" +
                                                "-fx-text-fill: #cdd6f4;" +
                                                "-fx-prompt-text-fill: #6c7086;" +
                                                "-fx-border-color: #45475a;" +
                                                "-fx-border-radius: 4;" +
                                                "-fx-background-radius: 4;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-padding: 8;");

                // ── Número receptor ───────────────────────────────────────────────
                Label lblTelReceptor = new Label("Número del dispositivo receptor:");
                lblTelReceptor.setTextFill(Color.web("#a6adc8"));
                lblTelReceptor.setFont(Font.font(11));

                TextField txtTelReceptor = new TextField(resultadoTelefono[0]);
                txtTelReceptor.setPromptText("Ej: +34612345678");

                txtTelReceptor.setStyle(
                                "-fx-background-color: #313244;" +
                                                "-fx-text-fill: #cdd6f4;" +
                                                "-fx-prompt-text-fill: #6c7086;" +
                                                "-fx-border-color: #45475a;" +
                                                "-fx-border-radius: 4;" +
                                                "-fx-background-radius: 4;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-padding: 8;");

                // ── Selector dispositivo receptor ─────────────────────────────────
                Label lblReceptor = new Label("Dispositivo receptor de SMS/MMS:");
                lblReceptor.setTextFill(Color.web("#a6adc8"));
                lblReceptor.setFont(Font.font(11));

                ComboBox<DispositivoCombo> cmbReceptor = new ComboBox<>();
                cmbReceptor.setPrefWidth(390);
                cmbReceptor.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4;");

                if (otrosDispositivos.isEmpty()) {
                        cmbReceptor.getItems().add(new DispositivoCombo("Sin otros dispositivos", "N/A", "Desconectado"));
                        cmbReceptor.setDisable(true);
                } else {
                        cmbReceptor.getItems().addAll(otrosDispositivos);
                        // Preseleccionar el que ya estaba configurado
                        if (resultadoSerial[0] != null && !resultadoSerial[0].isBlank()){
                                for (DispositivoCombo dc : otrosDispositivos) {
                                        if (dc.getSerial().equals(resultadoSerial[0])) {
                                                cmbReceptor.getSelectionModel().select(dc);
                                                break;
                                        }
                                }
                        }
                        else {
                                cmbReceptor.getSelectionModel().selectFirst();
                        }
                }

                // ── Error ─────────────────────────────────────────────────────────
                Label lblError = new Label("");
                lblError.setTextFill(Color.web("#f38ba8"));
                lblError.setFont(Font.font(11));
                lblError.setVisible(false);

                // ── Botones ───────────────────────────────────────────────────────
                Button btnContinuar = new Button("Continuar →");
                btnContinuar.setStyle(
                                "-fx-background-color: transparent;" +
                                                "-fx-border-color: #a6e3a1; -fx-border-radius: 6;" +
                                                "-fx-background-radius: 6;" +
                                                "-fx-text-fill: #a6e3a1;" +
                                                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                                                "-fx-padding: 8 20 8 20;");

                Button btnCancelar = new Button("Cancelar");
                btnCancelar.setStyle(
                                "-fx-background-color: transparent;" +
                                                "-fx-border-color: #f38ba8; -fx-border-radius: 6;" +
                                                "-fx-background-radius: 6;" +
                                                "-fx-text-fill: #f38ba8;" +
                                                "-fx-font-size: 13px;" +
                                                "-fx-padding: 8 20 8 20;");

                btnCancelar.setOnAction(e -> popup.close());

                btnContinuar.setOnAction(e -> {
                        String tel = txtTelReceptor.getText().trim();
                        String telDUT = txtTelDUT.getText().trim();

                        if (tel.isEmpty() || tel.replaceAll("[^0-9]", "").length() < 9) {
                                lblError.setText("Introduce un número válido para el receptor.");
                                lblError.setVisible(true);
                                return;
                        }
                        if (telDUT.isEmpty() || telDUT.replaceAll("[^0-9]", "").length() < 9) {
                                lblError.setText("Introduce un número válido para el DUT.");
                                lblError.setVisible(true);
                                return;
                        }

                        resultadoTelefono[0] = tel;
                        resultadoTelefonoDUT[0] = telDUT;
                        
                        if (otrosDispositivos.isEmpty()) {
                                resultadoSerial[0] = null;
                        } else {
                                DispositivoCombo seleccionado = cmbReceptor.getSelectionModel().getSelectedItem();
                                resultadoSerial[0] = (seleccionado != null) ? seleccionado.getSerial() : null;
                        }

                        confirmado[0] = true;
                        popup.close();
                });

                HBox botones = new HBox(12, btnCancelar, btnContinuar);
                botones.setAlignment(Pos.CENTER_RIGHT);

                root.getChildren().addAll(
                                lblTitulo, new Separator(),
                                lblSubtitulo,
                                lblTelDUT, txtTelDUT,
                                lblTelReceptor, txtTelReceptor,
                                lblReceptor, cmbReceptor,
                                lblError, new Separator(),
                                botones);

                Scene scene = new Scene(root);

                scene.getStylesheets().add(ContactosConfigPopup.class.getResource("/css/style.css").toExternalForm());

                popup.setScene(scene);
                popup.showAndWait();

                return confirmado[0];
        }
}