package com.example.Controller;

import com.example.Model.Dispositivo;
import com.example.Model.Foto;
import com.example.Model.Marca;
import com.example.Model.Soc;
import com.example.Model.Banda;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class DispositivosViewController {
    
    // Declaración de etiquetas de la interfaz
    @FXML private Label lblModelo, lblMarca, lblSoc, lblRam, lblSo, lblBandas;
    @FXML private ImageView imgDispositivo;
    @FXML private Button btnRegistrar;

    private ADBService adbService = new ADBService();
    private DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private String ultimoSerialDetectado;

    public void cargarDatos(String serial) {
        this.ultimoSerialDetectado = serial;
        try {
            // 1. Buscamos en la Base de Datos
            Dispositivo dbDevice = dispositivoDAO.buscarPorSerial(serial);

            if (dbDevice != null) {
                // --- CASO A: EL EQUIPO EXISTE EN BBDD ---
                actualizarInterfaz(dbDevice, false);
                
                // 2. Mostramos la versión del SO (proviene de la BBDD)
                lblSo.setText(dbDevice.getModelo().getSoVersion());

                // 3. Lógica para las BANDAS (ahora en su propio Label)
                if (dbDevice.getModelo().getBandas() != null && !dbDevice.getModelo().getBandas().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Banda b : dbDevice.getModelo().getBandas()) {
                        sb.append("[").append(b.getTipo()).append("-").append(b.getNumeroBanda()).append("] ");
                    }
                    lblBandas.setText(sb.toString());
                } else {
                    lblBandas.setText("Sin bandas registradas");
                }

                // 4. Lógica para la FOTO
                cargarFoto(dbDevice);

                System.out.println("✅ Datos completos cargados de BBDD para: " + serial);

            } else {
                // --- CASO B: EQUIPO NUEVO (Datos de ADB) ---
                Dispositivo adbDevice = adbService.obtenerProps(serial);
                if (adbDevice != null) {
                    actualizarInterfaz(adbDevice, true);
                    lblSo.setText(adbDevice.getModelo().getSoVersion()); // SO desde ADB
                    lblBandas.setText("PENDIENTE DE REGISTRO");
                    imgDispositivo.setImage(null); 
                    btnRegistrar.setVisible(true);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error crítico en la vista de dispositivos: " + e.getMessage());
            e.printStackTrace();
            lblModelo.setText("ERROR AL CARGAR");
        }
    }

    /**
     * Rellena los datos básicos comunes (Marca, Modelo, SoC, RAM)
     */
    private void actualizarInterfaz(Dispositivo d, boolean esNuevo) {
        String status = esNuevo ? " (No registrado)" : "";
        lblModelo.setText(d.getModelo().getNombreModelo() + status);
        lblMarca.setText(d.getModelo().getMarca().getNombre());
        lblSoc.setText(d.getModelo().getSoc().getModeloSoc());
        lblRam.setText(d.getModelo().getRamGb() + " GB");
    }

    /**
     * Intenta cargar la primera foto disponible del modelo
     */
    private void cargarFoto(Dispositivo d) {
        if (d.getModelo().getFotos() != null && !d.getModelo().getFotos().isEmpty()) {
            Foto primeraFoto = d.getModelo().getFotos().get(0);
            String ruta = primeraFoto.getUrl(); 

            File file = new File(ruta);
            if (file.exists()) {
                imgDispositivo.setImage(new Image(file.toURI().toString()));
            } else {
                System.out.println("⚠️ Imagen no encontrada en: " + ruta);
                imgDispositivo.setImage(null);
            }
        } else {
            imgDispositivo.setImage(null);
        }
    }
    // Método para el botón
// DispositivosViewController.java
@FXML
private void handleRegistrarEquipo() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/View/NuevoDispositivo.fxml"));
        VBox root = loader.load();

        NuevoDispositivoController controller = loader.getController();

        // --- AQUÍ ESTÁ EL ARREGLO ---
        // Necesitamos el SERIAL real, no el texto del label del modelo.
        // Lo más seguro es usar una variable que ya tengas o limpiar el texto con cuidado.
        
        // Si tienes el serial guardado en una variable de clase, úsala. 
        // Si no, podemos intentar sacarlo del Label si lo pusiste ahí, 
        // pero lo mejor es pasárselo directamente.
        
        Dispositivo temp = new Dispositivo();
        
        // 1. EL SERIAL: Asegúrate de pasar el serial que detectó ADB originalmente
        // Si no tienes la variable 'serial' a mano, podemos usar el ID del dispositivo 
        // o la lógica que usaste en cargarDatos(String serial)
        temp.setSerialNumber(ultimoSerialDetectado); // <--- Usa la variable que guarda el serial real
        
        com.example.Model.Modelo m = new com.example.Model.Modelo();
        // 2. EL MODELO: Este sí sale de lblModelo
        m.setNombreModelo(lblModelo.getText().replace(" (No registrado)", ""));
        
        m.setMarca(new com.example.Model.Marca(lblMarca.getText()));
        m.setSoc(new com.example.Model.Soc(lblSoc.getText()));
        temp.setModelo(m);

        controller.setDatosIniciales(temp);

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}