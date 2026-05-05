package com.example.View;

import com.example.Controller.*;
import com.example.Controller.ApnComparator.*;
import com.example.Controller.ExcelApnParser.*;
import com.example.Model.Dispositivo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

public class ComparadorApnController implements DispositivoAware {

    @FXML
    private StackPane rootPane;
    @FXML
    private Button btnXml;
    @FXML
    private Button btnXlsx;
    @FXML
    private Button btnAniadir;
    @FXML
    private Label lblOperadorActual;
    @FXML
    private TextArea areaXml;
    @FXML
    private GridPane gridExcel;
    @FXML
    private HBox panelBotones;
    @FXML
    private ScrollPane scrollExcel;
    @FXML
    private StackPane contenedorXml;
@FXML
private VBox overlayCarga;
    private InlineCssTextArea codeAreaXml;
    private Dispositivo dispositivoActual;
    private BiConsumer<String, Dispositivo> onCargarVista;

    private File archivoXml;
    private File archivoXlsx;
    private XmlApnParser xmlParser;
    private ExcelApnParser excelParser;

    // Lista de operadores del XLSX con coincidencias en el XML
    private List<Operador> operadoresConCoincidencias = new ArrayList<>();
    private int indiceOperadorActual = -1;

    // Resultados de comparación del operador actual
    private List<ResultadoComparacion> resultadosActuales = new ArrayList<>();
    private NavegacionHandler navegacionHandler;

    public void setNavegacionHandler(NavegacionHandler handler) {
        this.navegacionHandler = handler;
    }

    @Override
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivoActual = dispositivo;
    }

    public void setOnCargarVista(BiConsumer<String, Dispositivo> callback) {
        this.onCargarVista = callback;
    }

    @FXML
    public void initialize() {
        codeAreaXml = new InlineCssTextArea();
        codeAreaXml.setEditable(false);
        codeAreaXml.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaXml));

        // ESTILO BASE: Fondo oscuro y forzamos el color de la letra por CSS inline
        codeAreaXml.setStyle(
                "-fx-background-color: #1e1e2e; -fx-fill: #cdd6f4; -fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(codeAreaXml);
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        contenedorXml.getChildren().add(vsPane);
    }

    // ───────────────────── SELECCIÓN DE ARCHIVOS ─────────────────────

    @FXML
    private void onSeleccionarXml() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo XML de APNs");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML", "*.xml"));
        File f = chooser.showOpenDialog(btnXml.getScene().getWindow());
        if (f == null)
            return;

        archivoXml = f;
        marcarBotonSeleccionado(btnXml, f.getName());
        intentarCargar();
    }

    @FXML
    private void onSeleccionarXlsx() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo XLSX de referencia");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = chooser.showOpenDialog(btnXlsx.getScene().getWindow());
        if (f == null)
            return;

        archivoXlsx = f;
        marcarBotonSeleccionado(btnXlsx, f.getName());
        intentarCargar();
    }

    // Se llama cuando ambos archivos están seleccionados
    private void intentarCargar() {
        if (archivoXml == null || archivoXlsx == null)
            return;

        // Desactivamos los botones mientras carga
        btnXml.setDisable(true);
        btnXlsx.setDisable(true);

        new Thread(() -> {
            try {
                xmlParser = new XmlApnParser(archivoXml);
                excelParser = new ExcelApnParser(archivoXlsx);

                // Buscamos operadores del XLSX que tengan coincidencias en el XML
                operadoresConCoincidencias.clear();
                for (Operador op : excelParser.getOperadores()) {
                    List<XmlApnParser.ApnEntry> encontrados = xmlParser.buscarPorOperador(op.mcc, op.mnc);
                    if (!encontrados.isEmpty()) {
                        operadoresConCoincidencias.add(op);
                    }
                }

                Platform.runLater(() -> {
                    if (operadoresConCoincidencias.isEmpty()) {
                        lblOperadorActual.setText("No se encontraron coincidencias");
                        return;
                    }
                    // Cargamos el primer operador
                    indiceOperadorActual = 0;
                    cargarOperador(operadoresConCoincidencias.get(0));
                });

            } catch (Exception e) {
                Platform.runLater(() -> lblOperadorActual.setText("Error al cargar archivos: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── CARGA DE OPERADOR ─────────────────────

private void cargarOperador(Operador operador) {
    // 1. Mostrar feedback visual inmediatamente
    mostrarCargando(true);
    
    lblOperadorActual.setText(operador.pais + "   " + operador.nombre +
            "   ( mcc=\"" + operador.mcc + "\"  mnc=\"" + operador.mnc + "\" )");

    new Thread(() -> {
        try {
            List<ApnExcel> apnsExcel = excelParser.obtenerApnsOperador(operador);
            resultadosActuales.clear();
            for (ApnExcel apnExcel : apnsExcel) {
                resultadosActuales.add(ApnComparator.comparar(apnExcel, xmlParser, operador.mcc, operador.mnc));
            }

            String textoCompleto = String.join("\n", xmlParser.getLineas());

            Platform.runLater(() -> {
                codeAreaXml.clear();
                codeAreaXml.append(textoCompleto, "-fx-fill: #cdd6f4;");

                // Pequeña pausa para que el hilo de renderizado de RichText respire
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(300));
                pause.setOnFinished(e -> {
                    renderizarHighlightsXml();
                    renderizarTablaExcel(apnsExcel, operador);
                    panelBotones.setVisible(true);
                    panelBotones.setManaged(true);
                    btnAniadir.setDisable(resultadosActuales.isEmpty());
                    
                    // 2. Ocultar feedback visual al terminar
                    mostrarCargando(false);
                });
                pause.play();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> mostrarCargando(false));
        }
    }).start();
}

// MÉTODO HELPER PARA LA ANIMACIÓN
private void mostrarCargando(boolean mostrar) {
    if (overlayCarga == null) return;

    if (mostrar) {
        overlayCarga.setVisible(true);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlayCarga);
        fadeIn.setFromValue(overlayCarga.getOpacity());
        fadeIn.setToValue(1.0);
        fadeIn.play();
    } else {
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), overlayCarga);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> overlayCarga.setVisible(false));
        fadeOut.play();
    }
}

    // ───────────────────── RENDER XML ─────────────────────

    private void renderizarHighlightsXml() {
        if (codeAreaXml == null)
            return;
        int total = codeAreaXml.getParagraphs().size();

        // DEFINICIÓN DE ESTILOS: Forzamos el color claro (-fx-fill) en ambos
        String estiloNormal = "-fx-fill: #cdd6f4;";
        String estiloResaltado = "-fx-background-color: #3e4452; -fx-fill: #ffffff; -fx-font-weight: bold; -fx-border-color: #61afef; -fx-border-width: 0 0 0 5;";

        for (int i = 0; i < total; i++) {
            codeAreaXml.setParagraphStyle(i, estiloNormal);
        }

        int lineaMasArriba = Integer.MAX_VALUE;
        for (ResultadoComparacion res : resultadosActuales) {
            if (res.apnXml != null && res.apnXml.lineaInicio >= 0) {
                if (res.apnXml.lineaInicio < lineaMasArriba)
                    lineaMasArriba = res.apnXml.lineaInicio;
                for (int i = res.apnXml.lineaInicio; i <= res.apnXml.lineaFin; i++) {
                    if (i < total)
                        codeAreaXml.setParagraphStyle(i, estiloResaltado);
                }
            }
        }

        if (lineaMasArriba != Integer.MAX_VALUE) {
            final int destino = lineaMasArriba;
            Platform.runLater(() -> codeAreaXml.showParagraphAtTop(destino));
        }
    }
    // ───────────────────── RENDER EXCEL ─────────────────────

    private void renderizarTablaExcel(List<ApnExcel> apnsExcel, Operador operador) {
        gridExcel.getChildren().clear();
        gridExcel.getColumnConstraints().clear();

        ColumnConstraints colCampo = new ColumnConstraints();
        colCampo.setPercentWidth(40);
        ColumnConstraints colValor = new ColumnConstraints();
        colValor.setPercentWidth(60);
        gridExcel.getColumnConstraints().addAll(colCampo, colValor);

        int fila = 0;

        for (ApnExcel apnExcel : apnsExcel) {
            ResultadoComparacion resultado = resultadosActuales.stream()
                    .filter(r -> r.apnExcel == apnExcel)
                    .findFirst().orElse(null);

            // Por defecto amarillo (si no hay match en XML o falla el título)
            String colorTitulo = "#ffff00";

            if (resultado != null && resultado.apnXml != null) {
                String carrierXml = "";
                Map<String, String> attrs = resultado.apnXml.atributos;
                if (attrs.containsKey("carrier")) {
                    carrierXml = attrs.get("carrier").trim();
                }

                boolean coincideExacto = false;
                if (!carrierXml.isEmpty()) {
                    for (String tituloCandidatoExcel : apnExcel.titulosCandidatos) {
                        if (tituloCandidatoExcel.equals(carrierXml)) {
                            coincideExacto = true;
                            break;
                        }
                    }
                }

                if (coincideExacto) {
                    colorTitulo = "#ffffff";
                }
            }

            // Renderizado del Título
            if (apnExcel.nombreApnOriginal != null) {
                Label lblT = crearCeldaTabla(apnExcel.nombreApnOriginal, colorTitulo, true);
                // Añadimos un borde inferior grueso para separar visualmente los bloques
                lblT.setStyle(lblT.getStyle() + "-fx-border-width: 0 0 2 0; -fx-border-color: black;");
                gridExcel.add(lblT, 0, fila++, 2, 1);
            }

            // Renderizado de Atributos
            for (Map.Entry<String, String> entry : apnExcel.campos.entrySet()) {
                String campoExcel = entry.getKey();
                String valorExcel = entry.getValue();

                boolean dif = (resultado != null) && resultado.diferencias.stream()
                        .anyMatch(d -> d.campo.equalsIgnoreCase(campoExcel));

                String bg = dif ? "#ffff00" : "#ffffff";

                gridExcel.add(crearCeldaTabla(campoExcel, bg, false), 0, fila);
                gridExcel.add(crearCeldaTabla(valorExcel, bg, false), 1, fila++);
            }

            // Espacio entre bloques
            Region sep = new Region();
            sep.setPrefHeight(15);
            gridExcel.add(sep, 0, fila++, 2, 1);

        }

        // Mostrar panel de botones al terminar
        panelBotones.setVisible(true);
        panelBotones.setManaged(true);
    }

    private Label crearCeldaTabla(String texto, String bgColor, boolean bold) {
        Label lbl = new Label(texto);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setWrapText(true);
        lbl.setPadding(new Insets(4, 8, 4, 8));
        lbl.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: #1e1e2e;" +
                        "-fx-font-size: 12px;" +
                        (bold ? "-fx-font-weight: bold;" : ""));
        return lbl;
    }

    // ───────────────────── AÑADIR / CANCELAR ─────────────────────

    @FXML
    private void onAniadir() {
        new Thread(() -> {
            try {
                File nuevo = XmlApnWriter.guardarNuevaVersion(
                        archivoXml, resultadosActuales);

                // El XML base para los siguientes operadores es el nuevo
                archivoXml = nuevo;
                xmlParser = new XmlApnParser(nuevo);

                Platform.runLater(() -> {
                    mostrarToast("✓ Guardado: " + nuevo.getName());
                    siguienteOperador();
                });
            } catch (Exception e) {
                Platform.runLater(() -> mostrarToast("✗ Error al guardar: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onCancelar() {
        siguienteOperador();
    }

    private void siguienteOperador() {
        indiceOperadorActual++;
        if (indiceOperadorActual >= operadoresConCoincidencias.size()) {
            lblOperadorActual.setText("✓ Todos los operadores procesados");
            panelBotones.setVisible(false);
            panelBotones.setManaged(false);
            areaXml.clear();
            gridExcel.getChildren().clear();
            return;
        }
        cargarOperador(operadoresConCoincidencias.get(indiceOperadorActual));
    }

    // ───────────────────── VOLVER ─────────────────────

    @FXML
    private void onVolver() {
        if (navegacionHandler != null) {
            navegacionHandler.cambiarVistaCentral("/fxml/vista_diagnostico.fxml", dispositivoActual, null);
        }
    }

    // ───────────────────── HELPERS ─────────────────────

    private void marcarBotonSeleccionado(Button btn, String nombre) {
        btn.setText("Archivo seleccionado ✓");
        btn.setDisable(true);
        btn.setStyle(
                "-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e;" +
                        "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");
    }

    private void mostrarToast(String mensaje) {
        if (rootPane == null)
            return;
        Label toast = new Label(mensaje);
        toast.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12 24; -fx-background-radius: 24; -fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 32, 0));
        rootPane.getChildren().add(toast);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400),
                toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));
        new javafx.animation.SequentialTransition(fadeIn, pausa, fadeOut).play();
    }
}