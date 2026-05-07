package com.example.View;

import com.example.Model.DiferenciaApn;
import com.example.Model.Dispositivo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComparadorXmlController {

    @FXML
    private Label lblEstado;
    @FXML
    private TableView<DiferenciaApn> tblDiferencias;
    @FXML
    private TableColumn<DiferenciaApn, String> colOperadora;
    @FXML
    private TableColumn<DiferenciaApn, String> colNombre;
    @FXML
    private TableColumn<DiferenciaApn, String> colAtributo;
    @FXML
    private TableColumn<DiferenciaApn, String> colValorOriginal;
    @FXML
    private TableColumn<DiferenciaApn, String> colValorCopia;
    @FXML
    private TableColumn<DiferenciaApn, String> colEstado;


    @FXML
    private Button btnCargarOriginal;
    @FXML
    private Button btnCargarCopia;

    private File fileOriginal;
    private File fileCopia;
    Dispositivo dispositivo;
    ComparadorExcelController cExcel = new ComparadorExcelController();
    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivo = dispositivo;
    }

    @FXML
    public void initialize() {
        colOperadora.setCellValueFactory(new PropertyValueFactory<>("operadora"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colAtributo.setCellValueFactory(new PropertyValueFactory<>("atributo"));
        colValorOriginal.setCellValueFactory(new PropertyValueFactory<>("valOriginal"));
        colValorCopia.setCellValueFactory(new PropertyValueFactory<>("valCopia"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colOperadora.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.20));
        colNombre.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.20));
        colAtributo.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.15));
        colValorOriginal.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.15));
        colValorCopia.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.15));
        colEstado.prefWidthProperty().bind(tblDiferencias.widthProperty().multiply(0.15));
    }

   @FXML
private void onCargarOriginal() {
    fileOriginal = seleccionarArchivo("XML Original (Master)");
    if (fileOriginal != null) {
        cExcel.marcarBotonSeleccionado(btnCargarOriginal, fileOriginal.getName());
        ejecutarComparacionSiEsPosible();
    }
}

@FXML
private void onCargarCopia() {
    fileCopia = seleccionarArchivo("XML a comparar");
    if (fileCopia != null) {
        cExcel.marcarBotonSeleccionado(btnCargarCopia, fileCopia.getName());
        ejecutarComparacionSiEsPosible();
    }
}
    private void ejecutarComparacionSiEsPosible() {
        if (fileOriginal != null && fileCopia != null) {
            procesarComparacion();
        }
    }

    private void procesarComparacion() {
        lblEstado.setText("Comparando...");
        lblEstado.setStyle("-fx-background-color: #f9e2af; -fx-padding: 5 12; -fx-background-radius: 15;");
        tblDiferencias.getItems().clear();

        new Thread(() -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document docA = factory.newDocumentBuilder().parse(fileOriginal);
                Document docB = factory.newDocumentBuilder().parse(fileCopia);

                docA.getDocumentElement().normalize();
                docB.getDocumentElement().normalize();

                NodeList listaA = docA.getElementsByTagName("apn");
                NodeList listaB = docB.getElementsByTagName("apn");

                Map<String, Element> mapaOriginal = new HashMap<>();
                for (int i = 0; i < listaA.getLength(); i++) {
                    Element el = (Element) listaA.item(i);
                    mapaOriginal.put(generarIdUnico(el), el);
                }

                ObservableList<DiferenciaApn> resultados = FXCollections.observableArrayList();

                for (int i = 0; i < listaB.getLength(); i++) {
                    Element elB = (Element) listaB.item(i);
                    String idB = generarIdUnico(elB);

                    if (mapaOriginal.containsKey(idB)) {
                        compararAtributos(mapaOriginal.get(idB), elB, resultados);
                    } else {
                        resultados.add(new DiferenciaApn(
                                idB,
                                extraerNombre(elB),
                                "TODO",
                                "FALTA EN XML1",
                                "PRESENTE EN XML2"));
                    }
                }

                System.out.println("[COMPARAR] Diferencias: " + resultados.size());

                Platform.runLater(() -> {
                    tblDiferencias.setItems(resultados);
                    actualizarUIFeedback(resultados.isEmpty());
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblEstado.setText("✗ Error al comparar: " + e.getMessage());
                    lblEstado.setStyle("-fx-background-color: #f38ba8; " +
                            "-fx-padding: 5 12; -fx-background-radius: 15;");
                });
            }
        }).start();
    }

    private String generarIdUnico(Element el) {
        return el.getAttribute("mcc").trim() + "-" +
                el.getAttribute("mnc").trim() + " (" +
                el.getAttribute("apn").trim() + " [" +
                el.getAttribute("type").trim() + "])";
    }

    private String extraerNombre(Element el) {
        return el.getAttribute("name").trim();
    }

    private void compararAtributos(Element elA, Element elB,
            ObservableList<DiferenciaApn> lista) {
        String id = generarIdUnico(elA);
        String nombre = extraerNombre(elA);

        Set<String> todosLosAtributos = new LinkedHashSet<>();

        NamedNodeMap attrsA = elA.getAttributes();
        for (int i = 0; i < attrsA.getLength(); i++)
            todosLosAtributos.add(attrsA.item(i).getNodeName());

        NamedNodeMap attrsB = elB.getAttributes();
        for (int i = 0; i < attrsB.getLength(); i++)
            todosLosAtributos.add(attrsB.item(i).getNodeName());

        todosLosAtributos.remove("mcc");
        todosLosAtributos.remove("mnc");
        todosLosAtributos.remove("name");

        for (String attr : todosLosAtributos) {
            String valA = elA.getAttribute(attr).trim();
            String valB = elB.getAttribute(attr).trim();
            if (!valA.equals(valB)) {
                lista.add(new DiferenciaApn(id, nombre, attr, valA, valB));
            }
        }
    }

    private void actualizarUIFeedback(boolean sinDiferencias) {
        if (sinDiferencias) {
            lblEstado.setText("ARCHIVOS IDÉNTICOS");
            lblEstado.setStyle("-fx-background-color: #a6e3a1; " +
                    "-fx-padding: 5 12; -fx-background-radius: 15;");
        } else {
            lblEstado.setText("DIFERENCIAS ENCONTRADAS");
            lblEstado.setStyle("-fx-background-color: #f38ba8; " +
                    "-fx-padding: 5 12; -fx-background-radius: 15;");
        }
    }

  @FXML
private void onLimpiar() {
    tblDiferencias.getItems().clear();
    fileOriginal = fileCopia = null;

    // Resetea botones
    btnCargarOriginal.setText("Seleccionar XML");
    btnCargarOriginal.setDisable(false);
    btnCargarOriginal.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
            "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");

    btnCargarCopia.setText("Seleccionar XML");
    btnCargarCopia.setDisable(false);
    btnCargarCopia.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
            "-fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand;");

    lblEstado.setText("LISTO");
    lblEstado.setStyle("-fx-background-color: #89b4fa; " +
            "-fx-padding: 5 12; -fx-background-radius: 15;");
}

    @FXML
    private void onAbrirDiff() {
        if (fileOriginal == null || fileCopia == null)
            return;
        try {
            new ProcessBuilder("cmd", "/c", "code", "--diff",
                    fileOriginal.getAbsolutePath(),
                    fileCopia.getAbsolutePath()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Usa lblEstado como ancla para los FileChooser — siempre visible
    private File seleccionarArchivo(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        return fc.showOpenDialog(lblEstado.getScene().getWindow());
    }

    @FXML
    private void onExportar() {
        if (fileOriginal == null || fileCopia == null) {
            lblEstado.setText("Carga ambos archivos primero");
            return;
        }

        String nombreBase = fileOriginal.getName().replace(".xml", "");
        nombreBase = nombreBase.replaceAll("_parcheado_v\\d+$", "");

        File carpeta = fileOriginal.getParentFile();
        int version = 1;
        while (new File(carpeta, nombreBase + "_parcheado_v" + version + ".xml").exists()) {
            version++;
        }
        String nombreSugerido = nombreBase + "_parcheado_v" + version + ".xml";

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar XML parcheado");
        fc.setInitialFileName(nombreSugerido);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        // destino final antes del hilo para que sea accesible en el lambda
        final File destino = fc.showSaveDialog(lblEstado.getScene().getWindow());
        if (destino == null)
            return;

        lblEstado.setText("Procesando...");
        lblEstado.setStyle("-fx-background-color: #f9e2af; " +
                "-fx-padding: 5 12; -fx-background-radius: 15;");

        new Thread(() -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                Document docOriginal = factory.newDocumentBuilder().parse(fileOriginal);
                Document docCopia = factory.newDocumentBuilder().parse(fileCopia);

                docOriginal.getDocumentElement().normalize();
                docCopia.getDocumentElement().normalize();

                NodeList listaOriginal = docOriginal.getElementsByTagName("apn");
                NodeList listaCopia = docCopia.getElementsByTagName("apn");

                Map<String, Element> mapaCopia = new HashMap<>();
                for (int i = 0; i < listaCopia.getLength(); i++) {
                    Element el = (Element) listaCopia.item(i);
                    mapaCopia.put(generarIdUnico(el), el);
                }

                Map<String, Element> mapaOriginal = new HashMap<>();
                for (int i = 0; i < listaOriginal.getLength(); i++) {
                    Element el = (Element) listaOriginal.item(i);
                    mapaOriginal.put(generarIdUnico(el), el);
                }

                System.out.println("[EXPORT] Original: " + mapaOriginal.size() +
                        " APNs | Copia: " + mapaCopia.size() + " APNs");

                int parcheados = 0;
                for (int i = 0; i < listaOriginal.getLength(); i++) {
                    Element elOriginal = (Element) listaOriginal.item(i);
                    String id = generarIdUnico(elOriginal);

                    if (mapaCopia.containsKey(id)) {
                        Element elCopia = mapaCopia.get(id);
                        NamedNodeMap attrsCopia = elCopia.getAttributes();
                        for (int j = 0; j < attrsCopia.getLength(); j++) {
                            Attr attr = (Attr) attrsCopia.item(j);
                            String nombreAttr = attr.getName();
                            String valor = attr.getValue().trim();
                            if (!nombreAttr.equals("mcc") && !nombreAttr.equals("mnc")
                                    && !valor.isEmpty()) {
                                elOriginal.setAttribute(nombreAttr, valor);
                            }
                        }
                        parcheados++;
                    }
                }
                System.out.println("[EXPORT] APNs parcheados: " + parcheados);

                List<Node> apnsNuevos = new ArrayList<>();
                for (int i = 0; i < listaCopia.getLength(); i++) {
                    Element elCopia = (Element) listaCopia.item(i);
                    String id = generarIdUnico(elCopia);
                    if (!mapaOriginal.containsKey(id)) {
                        Node nodoImportado = docOriginal.importNode(elCopia, true);
                        apnsNuevos.add(nodoImportado);
                        System.out.println("[EXPORT] APN nuevo: " + id);
                    }
                }

                if (!apnsNuevos.isEmpty()) {
                    docOriginal.getDocumentElement().appendChild(
                            docOriginal.createComment(
                                    " ===== APNs NUEVOS AÑADIDOS DESDE XML COPIA (" +
                                            apnsNuevos.size() + ") ===== "));

                    for (Node nodo : apnsNuevos) {
                        Element el = (Element) nodo;
                        docOriginal.getDocumentElement().appendChild(
                                docOriginal.createComment(
                                        " NUEVO: " + extraerNombre(el) + " | " +
                                                generarIdUnico(el) + " "));
                        docOriginal.getDocumentElement().appendChild(nodo);
                    }

                    docOriginal.getDocumentElement().appendChild(
                            docOriginal.createComment(" ===== FIN APNs NUEVOS ===== "));

                    System.out.println("[EXPORT] Nuevos añadidos: " + apnsNuevos.size());
                }

                java.io.PrintWriter writer = new java.io.PrintWriter(
                        new java.io.OutputStreamWriter(
                                new java.io.FileOutputStream(destino), "UTF-8"));

                writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                writer.println("<apns>");

                NodeList hijos = docOriginal.getDocumentElement().getChildNodes();
                for (int i = 0; i < hijos.getLength(); i++) {
                    Node nodo = hijos.item(i);

                    if (nodo.getNodeType() == Node.COMMENT_NODE) {
                        writer.println("    <!--" + ((Comment) nodo).getData() + "-->");

                    } else if (nodo.getNodeType() == Node.ELEMENT_NODE
                            && nodo.getNodeName().equals("apn")) {
                        Element el = (Element) nodo;
                        writer.println("    <apn");

                        NamedNodeMap attrs = el.getAttributes();
                        for (int j = 0; j < attrs.getLength(); j++) {
                            Attr attr = (Attr) attrs.item(j);
                            writer.println("        " + attr.getName() +
                                    "=\"" + attr.getValue() + "\"");
                        }
                        writer.println("    />");
                    }
                }

                writer.println("</apns>");
                writer.flush();
                writer.close();

                System.out.println("[EXPORT] Guardado en: " + destino.getAbsolutePath());

                final int totalParcheados = parcheados;
                final int totalNuevos = apnsNuevos.size();

                Platform.runLater(() -> {
                    lblEstado.setText("✓ Exportado: " + totalParcheados +
                            " parcheados, " + totalNuevos + " nuevos → " +
                            destino.getName());
                    lblEstado.setStyle("-fx-background-color: #a6e3a1; " +
                            "-fx-padding: 5 12; -fx-background-radius: 15;");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblEstado.setText("✗ Error: " + e.getMessage());
                    lblEstado.setStyle("-fx-background-color: #f38ba8; " +
                            "-fx-padding: 5 12; -fx-background-radius: 15;");
                });
            }
        }).start();
    }
}