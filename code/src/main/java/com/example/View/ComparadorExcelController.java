package com.example.View;

import com.example.Controller.*;
import com.example.Controller.ApnComparator.*;
import com.example.Controller.ExcelApnParser.*;
import com.example.Model.Dispositivo;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

public class ComparadorExcelController {

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
    @FXML
    private Label lblMensajeCarga;

    private InlineCssTextArea codeAreaXml;

    private BiConsumer<String, Dispositivo> onCargarVista;

    private File archivoXml;
    private File archivoXlsx;
    private XmlApnParser xmlParser;
    private ExcelApnParser excelParser;
    private List<String> lineasXmlMemoria;
    private String mccActual = "";
    private String mncActual = "";

    // Lista de operadores del XLSX con coincidencias en el XML
    private List<Operador> operadoresConCoincidencias = new ArrayList<>();
    private int indiceOperadorActual = -1;
    // Resultados de comparación del operador actual
    private List<ResultadoComparacion> resultadosActuales = new ArrayList<>();

    private SelectorComparadorController selectorHandler;

    public void setSelectorHandler(SelectorComparadorController handler){
        this.selectorHandler = handler;
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
                lineasXmlMemoria = new ArrayList<>(xmlParser.getLineas());
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
                    cargarOperador(operadoresConCoincidencias.get(0), null);
                });

            } catch (Exception e) {
                Platform.runLater(() -> lblOperadorActual.setText("Error al cargar archivos: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── CARGA DE OPERADOR ─────────────────────

    private void cargarOperador(Operador operador, String mensajeToast) {
        lblOperadorActual.setText(operador.pais + "   " + operador.nombre +
                "   ( mcc=\"" + operador.mcc + "\"  mnc=\"" + operador.mnc + "\" )");

        this.mccActual = operador.mcc;
        this.mncActual = operador.mnc;

        javafx.concurrent.Task<List<ApnExcel>> tareaCarga = new javafx.concurrent.Task<>() {
            @Override
            protected List<ApnExcel> call() throws Exception {
                List<ApnExcel> apnsExcel = excelParser.obtenerApnsOperador(operador);
                resultadosActuales.clear();

                resultadosActuales = ApnComparator.compararTodoElOperador(apnsExcel, xmlParser, operador.mcc,
                        operador.mnc);

                return apnsExcel;
            }
        };

        tareaCarga.setOnSucceeded(e -> {
            List<ApnExcel> apnsExcel = tareaCarga.getValue();

            // Refresco visual de la UI
            String textoCompleto = String.join("\n", xmlParser.getLineas());
            codeAreaXml.clear();
            codeAreaXml.append(textoCompleto, "-fx-fill: #cdd6f4;");
            renderizarHighlightsXml();
            renderizarTablaExcel(apnsExcel, operador);

            btnAniadir.setDisable(resultadosActuales.isEmpty());

            // Quitamos overlay y lanzamos Toast
            mostrarCargando(false, "");
            if (mensajeToast != null && !mensajeToast.isEmpty()) {
                mostrarToast(mensajeToast);
            }
        });

        tareaCarga.setOnFailed(e -> {
            tareaCarga.getException().printStackTrace();
            mostrarCargando(false, "");
        });

        new Thread(tareaCarga).start();
    }

    private void mostrarCargando(boolean mostrar, String mensaje) {
        if (overlayCarga == null)
            return;

        if (mostrar) {
            lblMensajeCarga.setText(mensaje);
            overlayCarga.setVisible(true);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(200), overlayCarga);
            fadeIn.setFromValue(overlayCarga.getOpacity());
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } else {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), overlayCarga);
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
                String identificadorXML = "";
                Map<String, String> attrs = resultado.apnXml.atributos;
                if (attrs.containsKey("carrier")) {
                    identificadorXML = attrs.get("carrier").trim();
                } else if (attrs.containsKey("name")) {
                    identificadorXML = attrs.get("name").trim();
                }

                boolean coincideExacto = false;
                if (!identificadorXML.isEmpty()) {
                    for (String tituloCandidatoExcel : apnExcel.titulosCandidatos) {
                        if (tituloCandidatoExcel.trim().equals(identificadorXML)) {
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
        // Feedback visual
        mostrarCargando(true, "Guardando cambios...");

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        delay.setOnFinished(ev -> {
            new Thread(() -> {
                try {
                    // --- LÓGICA DE PROCESAMIENTO XML ---
                    List<String> nuevasLineas = new ArrayList<>(lineasXmlMemoria);
                    for (ResultadoComparacion r : resultadosActuales) {
                        if (!r.tieneDiferencias())
                            continue;
                        if (!r.existeEnXml) {
                            String bloqueNuevo = XmlApnWriter.construirApnXmlVertical(r.apnExcel, mccActual, mncActual);
                            int puntoIdx = XmlApnWriter.buscarLineaCierre(nuevasLineas);
                            if (puntoIdx != -1)
                                nuevasLineas.add(puntoIdx, bloqueNuevo);
                        } else if (r.apnXml != null && r.apnXml.lineaInicio >= 0) {
                            for (Diferencia d : r.diferencias) {
                                boolean modificado = false;
                                for (int i = r.apnXml.lineaInicio; i <= r.apnXml.lineaFin; i++) {
                                    if (i >= nuevasLineas.size())
                                        break;
                                    String lineaMod = XmlApnWriter.aplicarDiferencia(nuevasLineas.get(i), d);
                                    if (!nuevasLineas.get(i).equals(lineaMod)) {
                                        nuevasLineas.set(i, lineaMod);
                                        modificado = true;
                                        break;
                                    }
                                }
                                if (!modificado && d.esNuevo) {
                                    int fin = r.apnXml.lineaFin;
                                    if (fin < nuevasLineas.size()) {
                                        nuevasLineas.set(fin,
                                                XmlApnWriter.insertarAtributoEstiloVertical(nuevasLineas.get(fin), d));
                                    }
                                }
                            }
                        }
                    }
                    List<String> lineasAplanadas = new ArrayList<>();
                    for (String l : nuevasLineas) {
                        if (l != null && l.contains("\n")) {
                            lineasAplanadas.addAll(Arrays.asList(l.split("\n", -1)));
                        } else {
                            lineasAplanadas.add(l);
                        }
                    }
                    lineasXmlMemoria = lineasAplanadas;

                    Platform.runLater(() -> {
                        xmlParser.setLineas(new ArrayList<>(lineasXmlMemoria));

                        try {
                            xmlParser.parsear();
                        } catch (Exception e) {
                            System.err.println("Error al actualizar el parser: " + e.getMessage());
                        }

                        codeAreaXml.replaceText(String.join("\n", lineasXmlMemoria));

                        recalcularResultadosActuales();
                        renderizarHighlightsXml();

                        // Pasamos al siguiente con mensaje de éxito
                        avanzarAlSiguiente("✓ Cambios guardados");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> mostrarCargando(false, ""));
                }
            }).start();
        });
        delay.play();
    }

    private void recalcularResultadosActuales() {
        if (indiceOperadorActual < 0 || operadoresConCoincidencias.isEmpty())
            return;

        Operador op = operadoresConCoincidencias.get(indiceOperadorActual);
        List<ApnExcel> apnsExcel = excelParser.obtenerApnsOperador(op);

        resultadosActuales = ApnComparator.compararTodoElOperador(apnsExcel, xmlParser, op.mcc, op.mnc);
    }

    @FXML
    private void onCancelar() {
        mostrarCargando(true, "Omitiendo operador...");

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        delay.setOnFinished(ev -> {
            avanzarAlSiguiente(null);
        });
        delay.play();
    }

    private void avanzarAlSiguiente(String mensajeToast) {
        indiceOperadorActual++;
        if (indiceOperadorActual >= operadoresConCoincidencias.size()) {
            finalizarProceso();
            mostrarCargando(false, "");
        } else {
            cargarOperador(operadoresConCoincidencias.get(indiceOperadorActual), mensajeToast);
        }
    }

    @FXML
    private void finalizarProceso() {
        try {
            File archivoFinal = XmlApnWriter.guardarArchivoFinal(archivoXml, lineasXmlMemoria);
            String mensajeExito = "Archivo guardado: " + archivoFinal.getName();

            // Limpieza de UI
            panelBotones.setVisible(false);
            codeAreaXml.setEditable(false);

            mostrarToast("✓ Procesado con éxito");
            PauseTransition espera = new PauseTransition(Duration.seconds(4));
            espera.setOnFinished(e -> {
                if (selectorHandler != null) {
                    try {
                        selectorHandler.cargarSubVista("/fxml/comparador_excel.fxml",mensajeExito);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            espera.play();
        } catch (IOException e) {
            mostrarToast("Error al guardar archivo final");
        }
    }

    // ───────────────────── HELPERS ─────────────────────

    public void marcarBotonSeleccionado(Button btn, String nombre) {
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
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 32, 0));
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
        javafx.animation.PauseTransition delayEntrada = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(150));
        new javafx.animation.SequentialTransition(
                delayEntrada,
                fadeIn,
                pausa,
                fadeOut).play();
    }
}