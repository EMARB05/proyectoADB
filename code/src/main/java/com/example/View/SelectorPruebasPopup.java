package com.example.View;

import com.example.Model.BloquePrueba;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;

import java.util.*;
import java.util.function.Consumer;

public class SelectorPruebasPopup {

    /**
     * Muestra el popup genérico de selección de pruebas.
     *
     * @param tituloBloq   Nombre del bloque, ej: "SOFT.004 — Touch Screen"
     * @param pruebas      Lista de pruebas del bloque
     * @param owner        Stage padre (para bloquear la ventana principal)
     * @param onAnadir     Callback que recibe las pruebas seleccionadas al pulsar "Añadir"
     */
    
    public static void mostrar(
            String tituloBloq,
            List<BloquePrueba> pruebas,
            Stage owner,
            Consumer<List<BloquePrueba>> onAnadir) {

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initOwner(owner);

        // ── Contenedor principal ──────────────────────────────────────────
        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(520);
        root.setStyle(
            "-fx-background-color: #1e1e2e;" +
            "-fx-border-color: #45475a;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;");

        // ── Título ────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Seleccionar pruebas");
        lblTitulo.setFont(Font.font("Poppins", FontWeight.BOLD, 16));
        lblTitulo.setTextFill(Color.web("#cdd6f4"));

        Label lblBloque = new Label(tituloBloq);
        lblBloque.setFont(Font.font(12));
        lblBloque.setTextFill(Color.web("#a6adc8"));

        Separator sep = new Separator();

        // ── Checkbox "Seleccionar todo" ───────────────────────────────────
        CheckBox chkTodo = new CheckBox("Seleccionar todas");
        chkTodo.setTextFill(Color.web("#89b4fa"));
        chkTodo.setFont(Font.font(null, FontWeight.BOLD, 12));
        estilizarCheckbox(chkTodo);

        // ── ScrollPane con una fila por prueba ────────────────────────────
        VBox listaBox = new VBox(8);
        listaBox.setPadding(new Insets(4, 0, 4, 0));

        Map<BloquePrueba, CheckBox> checkMap = new LinkedHashMap<>();

        for (BloquePrueba p : pruebas) {
            CheckBox chk = new CheckBox();
            estilizarCheckbox(chk);

            // Fila: [checkbox] [ID en negrita] [descripción]
            Label lblId = new Label(p.getId());
            lblId.setFont(Font.font(null, FontWeight.BOLD, 12));
            lblId.setTextFill(Color.web("#89b4fa"));
            lblId.setMinWidth(110);

            Label lblDesc = new Label(p.getDescripcion());
            lblDesc.setTextFill(Color.web("#cdd6f4"));
            lblDesc.setFont(Font.font(12));
            lblDesc.setWrapText(true);

            HBox fila = new HBox(10, chk, lblId, lblDesc);
            fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            fila.setPadding(new Insets(6, 12, 6, 12));
            fila.setStyle("-fx-background-color: #313244; -fx-background-radius: 6;");

            // Highlight al pasar el ratón
            fila.setOnMouseEntered(e ->
                fila.setStyle("-fx-background-color: #45475a; -fx-background-radius: 6;"));
            fila.setOnMouseExited(e ->
                fila.setStyle("-fx-background-color: #313244; -fx-background-radius: 6;"));

            // Clic en la fila entera activa/desactiva el checkbox
            fila.setOnMouseClicked(e -> chk.setSelected(!chk.isSelected()));

            checkMap.put(p, chk);
            listaBox.getChildren().add(fila);
        }

        // Lógica "Seleccionar todo"
        chkTodo.setOnAction(e -> {
            boolean val = chkTodo.isSelected();
            checkMap.values().forEach(c -> c.setSelected(val));
        });

        // Si algún checkbox se desmarca, desmarca "todo"
        checkMap.values().forEach(c ->
            c.selectedProperty().addListener((obs, old, now) -> {
                if (!now) chkTodo.setSelected(false);
                else if (checkMap.values().stream().allMatch(CheckBox::isSelected))
                    chkTodo.setSelected(true);
            })
        );

        ScrollPane scroll = new ScrollPane(listaBox);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(350);
        scroll.setStyle("-fx-background: #1e1e2e; -fx-background-color: #1e1e2e;");

        // ── Botones ───────────────────────────────────────────────────────
        Button btnCancelar = crearBoton("Cancelar", "#f38ba8");
        Button btnAnadir   = crearBoton("Añadir al script", "#a6e3a1");

        btnCancelar.setOnAction(e -> popup.close());

        btnAnadir.setOnAction(e -> {
            List<BloquePrueba> seleccionadas = new ArrayList<>();
            checkMap.forEach((prueba, chk) -> {
                if (chk.isSelected()) seleccionadas.add(prueba);
            });
            if (!seleccionadas.isEmpty()) {
                onAnadir.accept(seleccionadas);  // ← devuelve al controlador
                popup.close();
            } else {
                lblBloque.setText("⚠ Selecciona al menos una prueba.");
                lblBloque.setTextFill(Color.web("#f38ba8"));
            }
        });

        HBox botones = new HBox(12, btnCancelar, btnAnadir);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            lblTitulo, lblBloque, sep, chkTodo, scroll, new Separator(), botones);

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ── Helpers de estilo (los mismos que ya usas) ────────────────────────
    private static Button crearBoton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 8 20 8 20;");
        btn.setOnMouseEntered(e ->
            btn.setStyle(btn.getStyle() + "-fx-background-color: rgba(255,255,255,0.05);"));
        btn.setOnMouseExited(e ->
            btn.setStyle(btn.getStyle().replace("-fx-background-color: rgba(255,255,255,0.05);", "")));
        return btn;
    }

    private static void estilizarCheckbox(CheckBox chk) {
        chk.setStyle("-fx-text-fill: #cdd6f4;");
    }
}
