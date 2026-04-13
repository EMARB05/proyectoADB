package com.example.Controller;

import com.example.Model.Dispositivo; // Asegúrate de que la ruta de tu objeto sea esta
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label lblSerial;
    @FXML private Label lblMarca;
    @FXML private Label lblModelo;
    @FXML private Label lblSoC;
    @FXML private Label lblEstado;
    @FXML private Button btnEscanear;
    @FXML private ImageView imgDispositivo;

    private ADBService adbService = new ADBService();
    private DispositivoDAO dispositivoDAO = new DispositivoDAO(); 

    @FXML
    private void handleEscanear() {
        System.out.println("Botón pulsado: Iniciando escaneo ADB...");
        
        try {
            List<String> dispositivos = adbService.obtenerDispositivosConectados();
            
            if (!dispositivos.isEmpty()) {
                String serial = dispositivos.get(0);
                lblSerial.setText(serial);
                
                // --- AQUÍ CONECTAMOS CON LA BASE DE DATOS ---
                Dispositivo encontrado = dispositivoDAO.buscarPorSerial(serial);
                
                if (encontrado != null) {
                    // Si existe, rellenamos los labels con los datos del objeto
                    lblMarca.setText(encontrado.getModelo().getMarca().getNombre());
                    lblModelo.setText(encontrado.getModelo().getNombreModelo());
                    lblSoC.setText(encontrado.getModelo().getSoc().getModeloSoc());
                    
                    lblEstado.setText("EQUIPO RECONOCIDO");
                    lblEstado.setStyle("-fx-text-fill: #27ae60;"); // Verde
                } else {
                    // Si NO existe, limpiamos los labels y abrimos el formulario de alta
                    lblMarca.setText("---");
                    lblModelo.setText("---");
                    lblSoC.setText("---");
                    lblEstado.setText("NUEVO EQUIPO DETECTADO");
                    lblEstado.setStyle("-fx-text-fill: #f39c12;"); // Naranja
                    
                    abrirFormularioAlta(serial);
                }
                
            } else {
                lblSerial.setText("---");
                lblEstado.setText("MÓVIL NO DETECTADO");
                lblEstado.setStyle("-fx-text-fill: #e74c3c;"); // Rojo
            }
            
        } catch (Exception e) {
            lblEstado.setText("ERROR ADB / BBDD");
            e.printStackTrace();
        }
    }

    private void abrirFormularioAlta(String serialDetectado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/View/NuevoDispositivo.fxml"));
            Parent root = loader.load();

            // Obtenemos el controlador de la ventana de alta
            NuevoDispositivoController controller = loader.getController();
            
            // Le pasamos el serial para que el TextField de la otra ventana ya esté relleno
            controller.setSerial(serialDetectado);

            Stage stage = new Stage();
            stage.setTitle("Registrar Nuevo Dispositivo");
            stage.initModality(Modality.APPLICATION_MODAL); 
            stage.setScene(new Scene(root));
            stage.showAndWait(); 
            
            // Al cerrar la ventana de alta, volvemos a escanear automáticamente
            // para mostrar los datos que acabamos de guardar.
            handleEscanear(); 

        } catch (IOException e) {
            System.err.println("Error al abrir la ventana de alta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}