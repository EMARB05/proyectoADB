package com.example.View;

import com.example.Controller.*;
import com.example.Controller.ApnComparator.*;
import com.example.Controller.ExcelApnParser.*;
import com.example.Model.Dispositivo;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

public class ComparadorApnController implements DispositivoAware {

    @FXML private StackPane rootPane;
    @FXML private Button    btnXml;
    @FXML private Button    btnXlsx;
    @FXML private Label     lblOperadorActual;
    @FXML private TextArea  areaXml;
    @FXML private GridPane  gridExcel;
    @FXML private HBox      panelBotones;
    @FXML private ScrollPane scrollExcel;

    private Dispositivo dispositivoActual;
    private BiConsumer<String, Dispositivo> onCargarVista;

    private File archivoXml;
    private File archivoXlsx;
    private XmlApnParser   xmlParser;
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

    // ───────────────────── SELECCIÓN DE ARCHIVOS ─────────────────────

    @FXML
    private void onSeleccionarXml() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo XML de APNs");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML", "*.xml"));
        File f = chooser.showOpenDialog(btnXml.getScene().getWindow());
        if (f == null) return;

        archivoXml = f;
        marcarBotonSeleccionado(btnXml, f.getName());
        intentarCargar();
    }

    @FXML
    private void onSeleccionarXlsx() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo XLSX de referencia");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = chooser.showOpenDialog(btnXlsx.getScene().getWindow());
        if (f == null) return;

        archivoXlsx = f;
        marcarBotonSeleccionado(btnXlsx, f.getName());
        intentarCargar();
    }

    // Se llama cuando ambos archivos están seleccionados
    private void intentarCargar() {
        if (archivoXml == null || archivoXlsx == null) return;

        // Desactivamos los botones mientras carga
        btnXml.setDisable(true);
        btnXlsx.setDisable(true);

        new Thread(() -> {
            try {
                xmlParser   = new XmlApnParser(archivoXml);
                excelParser = new ExcelApnParser(archivoXlsx);

                // Buscamos operadores del XLSX que tengan coincidencias en el XML
                operadoresConCoincidencias.clear();
                for (Operador op : excelParser.getOperadores()) {
                    List<XmlApnParser.ApnEntry> encontrados =
                        xmlParser.buscarPorOperador(op.mcc, op.mnc);
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
                Platform.runLater(() ->
                    lblOperadorActual.setText("Error al cargar archivos: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    // ───────────────────── CARGA DE OPERADOR ─────────────────────

    private void cargarOperador(Operador operador) {
        // Cabecera
        lblOperadorActual.setText(
            operador.pais + "   " + operador.nombre +
            "  ( mcc=\"" + operador.mcc + "\"  mnc=\"" + operador.mnc + "\" )"
        );

        resultadosActuales.clear();

        new Thread(() -> {
            // Obtenemos APNs del Excel para este operador
            List<ApnExcel> apnsExcel =
                excelParser.obtenerApnsOperador(operador);

            // Comparamos cada APN del Excel contra el XML
            for (ApnExcel apnExcel : apnsExcel) {
                if (apnExcel.apn == null) continue;
                ResultadoComparacion r = ApnComparator.comparar(
                    apnExcel, xmlParser, operador.mcc, operador.mnc
                );
                resultadosActuales.add(r);
            }

            // Construimos el texto del XML con highlights
            String textoXml = construirTextoXmlConHighlight();

            Platform.runLater(() -> {
                // Panel izquierdo: XML
                areaXml.setText(textoXml);
                scrollearACoincidencia();

                // Panel derecho: tabla Excel
                renderizarTablaExcel(apnsExcel, operador);

                // Mostramos botones solo si hay diferencias
                boolean hayDiferencias = resultadosActuales.stream()
                    .anyMatch(ResultadoComparacion::tieneDiferencias);
                panelBotones.setVisible(hayDiferencias);
                panelBotones.setManaged(hayDiferencias);
            });
        }).start();
    }

    // ───────────────────── RENDER XML ─────────────────────

    private String construirTextoXmlConHighlight() {
        List<String> lineas = xmlParser.getLineas();
        // Marcamos las líneas de los APNs encontrados con un prefijo
        // TextArea no soporta colores, así que usamos ">>>" como indicador
        Set<Integer> lineasHighlight = new HashSet<>();
        for (ResultadoComparacion r : resultadosActuales) {
            if (r.apnXml != null) {
                for (int i = r.apnXml.lineaInicio; i <= r.apnXml.lineaFin; i++) {
                    lineasHighlight.add(i);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            if (lineasHighlight.contains(i)) {
                sb.append(">>> ").append(lineas.get(i)).append("\n");
            } else {
                sb.append("    ").append(lineas.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    private void scrollearACoincidencia() {
        // Buscamos la primera línea con highlight
        String texto = areaXml.getText();
        int pos = texto.indexOf(">>>");
        if (pos < 0) return;

        // Calculamos el scroll proporcional
        int totalLineas = texto.split("\n").length;
        int lineaHighlight = texto.substring(0, pos).split("\n").length;
        double ratio = (double) lineaHighlight / totalLineas;

        // Pequeño delay para que el TextArea haya terminado de renderizar
        Animation pausa = new PauseTransition(javafx.util.Duration.millis(100));
        pausa.setOnFinished(e -> areaXml.setScrollTop(
                ratio * areaXml.getHeight() * 10
            ));
        pausa.play();
    }

    // ───────────────────── RENDER EXCEL ─────────────────────

    private void renderizarTablaExcel(List<ApnExcel> apnsExcel, Operador operador) {
        gridExcel.getChildren().clear();
        gridExcel.getColumnConstraints().clear();

        // Dos columnas: campo | valor
        ColumnConstraints colCampo = new ColumnConstraints();
        colCampo.setPercentWidth(40);
        ColumnConstraints colValor = new ColumnConstraints();
        colValor.setPercentWidth(60);
        gridExcel.getColumnConstraints().addAll(colCampo, colValor);

        int fila = 0;
        for (ApnExcel apnExcel : apnsExcel) {
            if (apnExcel.apn == null) continue;

            // Buscamos el resultado de comparación para este APN
            ResultadoComparacion resultado = resultadosActuales.stream()
                .filter(r -> r.apnExcel == apnExcel)
                .findFirst().orElse(null);

            // Cabecera del APN
            if (apnExcel.nombreApn != null) {
                Label lblNombre = new Label(apnExcel.nombreApn);
                lblNombre.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 13px;" +
                    "-fx-text-fill: #1e1e2e; -fx-padding: 8 4 4 4;");
                gridExcel.add(lblNombre, 0, fila, 2, 1);
                fila++;
            }

            // Filas de campos
            for (Map.Entry<String, String> entry : apnExcel.campos.entrySet()) {
                String campo = entry.getKey();
                String valor = entry.getValue();

                boolean esDiferente = resultado != null &&
                    resultado.diferencias.stream()
                        .anyMatch(d -> d.campo.equalsIgnoreCase(campo));

                String bgColor = esDiferente ? "#ffff00" : "#ffffff";

                Label lblCampo = crearCeldaTabla(campo, bgColor, false);
                Label lblValor = crearCeldaTabla(valor, bgColor, false);

                gridExcel.add(lblCampo, 0, fila);
                gridExcel.add(lblValor, 1, fila);
                fila++;
            }

            // Separador entre APNs
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #cccccc;");
            gridExcel.add(sep, 0, fila, 2, 1);
            fila++;
        }
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
            (bold ? "-fx-font-weight: bold;" : "")
        );
        return lbl;
    }

    // ───────────────────── AÑADIR / CANCELAR ─────────────────────

    @FXML
    private void onAniadir() {
        new Thread(() -> {
            try {
                File nuevo = XmlApnWriter.guardarNuevaVersion(
                    archivoXml, resultadosActuales
                );

                // El XML base para los siguientes operadores es el nuevo
                archivoXml = nuevo;
                xmlParser  = new XmlApnParser(nuevo);

                Platform.runLater(() -> {
                    mostrarToast("✓ Guardado: " + nuevo.getName());
                    siguienteOperador();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    mostrarToast("✗ Error al guardar: " + e.getMessage())
                );
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
        if (onCargarVista != null) {
            onCargarVista.accept("/fxml/ficha_tecnica.fxml", dispositivoActual);
        }
    }

    // ───────────────────── HELPERS ─────────────────────

    private void marcarBotonSeleccionado(Button btn, String nombre) {
        btn.setText("Archivo seleccionado ✓");
        btn.setDisable(true);
        btn.setStyle(
            "-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e;" +
            "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;"
        );
    }

    private void mostrarToast(String mensaje) {
        if (rootPane == null) return;
        Label toast = new Label(mensaje);
        toast.setStyle(
            "-fx-background-color: #313244; -fx-text-fill: #cdd6f4;" +
            "-fx-padding: 12 24; -fx-background-radius: 24; -fx-font-size: 13px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 4);");
        toast.setOpacity(0);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 32, 0));
        rootPane.getChildren().add(toast);

        javafx.animation.FadeTransition fadeIn =
            new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        javafx.animation.PauseTransition pausa =
            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        javafx.animation.FadeTransition fadeOut =
            new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));
        new javafx.animation.SequentialTransition(fadeIn, pausa, fadeOut).play();
    }
}