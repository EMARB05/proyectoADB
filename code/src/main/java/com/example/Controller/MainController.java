package com.example.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.util.List;

public class MainController {

    // Estos IDs deben coincidir exactamente con los fx:id del FXML
    @FXML private Label lblSerial;
    @FXML private Label lblMarca;
    @FXML private Label lblModelo;
    @FXML private Label lblSoC;
    @FXML private Label lblEstado;
    @FXML private Button btnEscanear;
    @FXML private ImageView imgDispositivo;

    // Instanciamos los servicios que ya tenéis
    private ADBService adbService = new ADBService();
    // private DispositivoDAO dispositivoDAO = new DispositivoDAO(); // Descomentar cuando el DAO esté listo

    @FXML
    private void handleEscanear() {
        System.out.println("Botón pulsado: Iniciando escaneo ADB...");
        
        try {
            List<String> dispositivos = adbService.obtenerDispositivosConectados();
            
            if (!dispositivos.isEmpty()) {
                String serial = dispositivos.get(0);
                lblSerial.setText(serial);
                lblEstado.setText("CONECTADO");
                lblEstado.setStyle("-fx-text-fill: #27ae60;"); // Color verde
                
                // Aquí es donde luego haréis: dispositivoDAO.buscarPorSerial(serial)
            } else {
                lblEstado.setText("MÓVIL NO DETECTADO");
                lblEstado.setStyle("-fx-text-fill: #e74c3c;"); // Color rojo
            }
            
        } catch (Exception e) {
            lblEstado.setText("ERROR ADB");
            e.printStackTrace();
        }
    }
}