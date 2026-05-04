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

import org.fxmisc.richtext.CodeArea;
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
        codeAreaXml.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaXml)); // Números de línea

        // Estilo base oscuro (Catppuccin)
        codeAreaXml.setStyle(
                "-fx-background-color: #11111b; " +
                        "-fx-text-fill: #cdd6f4; " +
                        "-fx-font-family: 'JetBrains Mono', monospace; " +
                        "-fx-font-size: 12px;");

        contenedorXml.getChildren().add(new VirtualizedScrollPane<>(codeAreaXml));
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
        lblOperadorActual.setText(operador.pais + "   " + operador.nombre +
                "   ( mcc=\"" + operador.mcc + "\"  mnc=\"" + operador.mnc + "\" )");

        // Iniciamos el hilo secundario para no congelar la app mientras procesamos
        new Thread(() -> {
            // 1. Procesamiento pesado en segundo plano
            List<ApnExcel> apnsExcel = excelParser.obtenerApnsOperador(operador);

            resultadosActuales.clear();
            for (ApnExcel apnExcel : apnsExcel) {
                ResultadoComparacion r = ApnComparator.comparar(
                        apnExcel, xmlParser, operador.mcc, operador.mnc);
                resultadosActuales.add(r);
            }

            // Unimos las 45.000 líneas en un solo String (fuera del hilo de UI)
            String textoCompleto = String.join("\n", xmlParser.getLineas());

            // 2. Volvemos al hilo de JavaFX para actualizar la vista
            Platform.runLater(() -> {
                // CARGA EL TEXTO
                codeAreaXml.replaceText(textoCompleto);

                // ESPERA DE ASIMILACIÓN (PauseTransition es clave con archivos grandes)
                javafx.animation.PauseTransition delayCarga = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(300)); // Subimos a 300ms por seguridad

                delayCarga.setOnFinished(e -> {
                    // PINTADO Y SCROLL (dentro de renderizarHighlightsXml ya llamas a scrollear)
                    renderizarHighlightsXml();

                    // UI SECUNDARIA
                    renderizarTablaExcel(apnsExcel, operador);
                    panelBotones.setVisible(true);
                    panelBotones.setManaged(true);
                    btnAniadir.setDisable(resultadosActuales.isEmpty());
                });

                delayCarga.play();
            });
        }).start(); // <--- ¡ASEGÚRATE DE QUE ESTE .start() ESTÉ AQUÍ!
    }

    // ───────────────────── RENDER XML ─────────────────────

    private void renderizarHighlightsXml() {
        // 1. LIMPIEZA INICIAL
        // Es fundamental limpiar para que no se acumulen colores de búsquedas
        // anteriores
        for (int i = 0; i < codeAreaXml.getParagraphs().size(); i++) {
            codeAreaXml.setParagraphStyle(i, "");
        }

        // 2. APLICAR COLORES
        boolean huboCoincidencias = false;
        for (ResultadoComparacion r : resultadosActuales) {
            if (r.apnXml != null) {
                huboCoincidencias = true;
                String estilo = "-fx-background-color: #313244; -fx-border-color: #89b4fa; -fx-border-width: 0 0 0 3;";

                for (int i = r.apnXml.lineaInicio; i <= r.apnXml.lineaFin; i++) {
                    if (i >= 0 && i < codeAreaXml.getParagraphs().size()) {
                        codeAreaXml.setParagraphStyle(i, estilo);
                    }
                }
            }
        }

        // 3. SCROLL CON RETRASO (La clave del éxito)
        // Usamos un delay un poco más largo (200ms) porque 45k líneas pesan mucho.
        if (huboCoincidencias) {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(200));
            pause.setOnFinished(e -> scrollear());
            pause.play();
        }
    }

    private void scrollear() {
        resultadosActuales.stream()
                .filter(r -> r.apnXml != null)
                .findFirst()
                .ifPresent(r -> {
                    int linea = r.apnXml.lineaInicio;

                    System.out.println("DEBUG: Intentando scrollear a línea: " + linea);

                    // Forzamos un layout antes de scrollear
                    codeAreaXml.requestLayout();

                    Platform.runLater(() -> {
                        if (linea >= 0 && linea < codeAreaXml.getParagraphs().size()) {
                            // Intento 1: El estándar
                            codeAreaXml.showParagraphAtTop(linea);

                            // Intento 2: Forzar si el primero falla (hacer "scroll" manual al párrafo)
                            codeAreaXml.scrollYToPixel(linea * 15.0); // 15 es el alto estimado de línea
                        }
                    });
                });
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