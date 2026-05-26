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


public class ContactosConfigPopup {
        // Elemento para mostrar en listas, muestra modelo+android id pero tiene el
        // valor del num. serie
        public static class DispositivoCombo {
                private final String modelo;
                private final String serial;
                private final String androidId;

                public DispositivoCombo(String modelo, String serial, String androidId) {
                        this.modelo = modelo;
                        this.serial = serial;
                        this.androidId = androidId;
                }

                public String getModelo() {
                        return modelo;
                }

                public String getSerial() {
                        return serial;
                }

                public String getAndroidId() {
                        return androidId;
                }

                @Override
                public String toString() {
                        return modelo + " - " + androidId;
                }
        }

        /**
         * Muestra el popup de configuración del bloque de contactos.
         * Recoge el número de teléfono y el dispositivo receptor de llamadas.
         * Devuelve true si el técnico confirmó, false si canceló.
         */
        public static boolean mostrar(
                        List<DispositivoCombo> otrosDispositivos,
                        String[] resultadoTelefonoDUT, 
                        String[] resultadoTelefono, 
                        String[] resultadoSerialReceptor, 
                        String[] resultadoExchange,
                        Stage owner) {

                boolean[] confirmado = { false };

                Stage popup = new Stage();
                popup.initModality(Modality.APPLICATION_MODAL);
                popup.initStyle(StageStyle.UNDECORATED);
                popup.initOwner(owner);

                VBox root = new VBox(14);
                root.setPadding(new Insets(24));
                root.setPrefWidth(420);
                root.getStyleClass().add("root-popup");

                // ── Título ────────────────────────────────────────────────────────
                Label lblTitulo = new Label("Configuración — Contactos");
                lblTitulo.setFont(Font.font(null, FontWeight.BOLD, 14));
                lblTitulo.setTextFill(Color.web("#cdd6f4"));

                Label lblSubtitulo = new Label(
                                "Introduce el número que se usará para crear contactos y realizar llamadas.");
                lblSubtitulo.setTextFill(Color.web("#a6adc8"));
                lblSubtitulo.setFont(Font.font(11));
                lblSubtitulo.setWrapText(true);
                // ── Número de teléfono (propio) ───────────────────────────────────
                Label lblTelDUT = new Label("Número de teléfono del dispositivo principal (DUT):");
                lblTelDUT.setTextFill(Color.web("#a6adc8"));
                lblTelDUT.setFont(Font.font(11));
                
                TextField txtTelefonoDUT = new TextField(resultadoTelefonoDUT[0]);
                txtTelefonoDUT.setPromptText("Ej: +34698765432");
                // ── Número de teléfono (dispositivo 2) ────────────────────────────
                Label lblTel = new Label("Número de teléfono del segundo dispositivo:");
                lblTel.setTextFill(Color.web("#a6adc8"));
                lblTel.setFont(Font.font(11));

                TextField txtTelefono = new TextField(resultadoTelefono[0]);
                txtTelefono.setPromptText("Ej: +34612345678");

                // ── Selector de dispositivo receptor ──────────────────────────────
                Label lblReceptor = new Label("Dispositivo receptor de llamadas:");
                lblReceptor.setTextFill(Color.web("#a6adc8"));
                lblReceptor.setFont(Font.font(11));

                ComboBox<DispositivoCombo> cmbReceptor = new ComboBox<>(); 
                cmbReceptor.setPrefWidth(370);

                // Label para el aviso de pruebas manuales (definido aquí arriba para poder usarlo)
                Label lblSinDispositivos = new Label("⚠ Las pruebas de recepción de llamada serán manuales.");
                lblSinDispositivos.setTextFill(Color.web("#f9e2af"));
                lblSinDispositivos.setFont(Font.font(10));
                lblSinDispositivos.setVisible(false); 

                if (otrosDispositivos.isEmpty()) {
                        // Caso en el que no hay teléfonos conectados
                        cmbReceptor.getItems()
                                        .add(new DispositivoCombo("Sin otros dispositivos", "N/A", "Desconectado"));
                        cmbReceptor.setDisable(true);
                        lblSinDispositivos.setVisible(true);
                } else {
                        cmbReceptor.getItems().addAll(otrosDispositivos);

                        // Si ya existía un receptor guardado previamente, lo buscamos y
                        // pre-seleccionamos
                        if (resultadoSerialReceptor[0] != null && !resultadoSerialReceptor[0].isEmpty()) {
                                for (DispositivoCombo dc : otrosDispositivos) {
                                        if (dc.getSerial().equals(resultadoSerialReceptor[0])) {
                                                cmbReceptor.getSelectionModel().select(dc);
                                                break;
                                        }
                                }
                        } else {
                                cmbReceptor.getSelectionModel().selectFirst();
                        }
                }

                Label lblExchange = new Label("Cuenta Exchange (Opcional):");
                lblExchange.setTextFill(Color.web("#cdd6f4"));

                TextField txtExchange = new TextField();
                txtExchange.setPromptText("ejemplo@empresa.com");
                if (resultadoExchange[0] != null) {
                        txtExchange.setText(resultadoExchange[0]);
                }

                // ── Error ─────────────────────────────────────────────────────────
                Label lblError = new Label("Introduce un número válido (mínimo 9 dígitos).");
                lblError.setTextFill(Color.web("#f38ba8"));
                lblError.setFont(Font.font(11));
                lblError.setVisible(false);

                // ── Botones ───────────────────────────────────────────────────────
                Button btnContinuar = new Button("Continuar →");
                btnContinuar.getStyleClass().add("btn-continuar"); 

                Button btnCancelar = new Button("Cancelar");
                btnCancelar.getStyleClass().add("btn-cancelar");

                btnCancelar.setOnAction(e -> popup.close());

                btnContinuar.setOnAction(e -> {
                        String telDUT = txtTelefonoDUT.getText().trim();
                        String tel = txtTelefono.getText().trim();

                        if (telDUT.isEmpty() || telDUT.replaceAll("[^0-9]", "").length() < 9) {
                                lblError.setVisible(true);
                                lblError.setText("Introduce un número válido para el Dispositivo principal");
                                return;
                        }
                        if (tel.isEmpty() || tel.replaceAll("[^0-9]", "").length() < 9) {
                                lblError.setVisible(true);
                                lblError.setText("Introduce un número válido para el receptor.");
                                return;
                        }
                        resultadoTelefonoDUT[0] = telDUT;
                        resultadoTelefono[0] = tel;

                        if (otrosDispositivos.isEmpty()) {
                                resultadoSerialReceptor[0] = null;
                        } else {
                                DispositivoCombo seleccionado = cmbReceptor.getSelectionModel().getSelectedItem();
                                resultadoSerialReceptor[0] = (seleccionado != null) ? seleccionado.getSerial() : null;
                        }

                        String exchangeText = txtExchange.getText().trim();
                        resultadoExchange[0] = exchangeText.isEmpty() ? null : exchangeText;

                        confirmado[0] = true;
                        popup.close();
                });

                HBox botones = new HBox(12, btnCancelar, btnContinuar);
                botones.setAlignment(Pos.CENTER_RIGHT);

                // Construcción de la interfaz añadiendo los elementos ordenados
                root.getChildren().addAll(
                                lblTitulo, new Separator(),
                                lblSubtitulo,
                                lblTelDUT,txtTelefonoDUT,
                                lblTel, txtTelefono,
                                lblReceptor, cmbReceptor, lblSinDispositivos, 
                                lblExchange, txtExchange,                                
                                lblError, new Separator(),
                                botones);

                Scene scene = new Scene(root);

                scene.getStylesheets().add(ContactosConfigPopup.class.getResource("/css/style.css").toExternalForm());

                popup.setScene(scene);
                popup.showAndWait();

                return confirmado[0];
        }
}
