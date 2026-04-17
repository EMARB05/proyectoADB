package com.example.View;

import com.example.Controller.ADBService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class RedImsController {

    @FXML
    private TextField txtApn;

    @FXML
    private TextField txtOperador;

    @FXML
    private TextField txtVoLTE;

    private String serial;
    private ADBService adbService = new ADBService();

    public void setSerial(String serial) {
        this.serial = serial;
        sincronizarEstadoInicial();
    }

    private void sincronizarEstadoInicial() {
        new Thread(() -> {
            try {
                String apnStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://telephony/carriers/preferapn --projection name");
                String operadorStatus = adbService.ejecutarComandoSincrono(serial, "shell getprop gsm.operator.alpha");
                // String volteStatus = adbService.ejecutarComandoSincrono(serial,
                //         "shell getprop persist.dbg.volte_avail_ovr");

                String[] volteStatus = adbService.getVolteEstado(serial);        
                Platform.runLater(() -> {
                    // APN
                    // devuelve algo como: "Row: 0 name=Mi APN"
                    try {
                        String apn = apnStatus.contains("name=")
                                ? apnStatus.trim().replaceAll(".*name=([^,\\n]+).*", "$1").trim()
                                : "No disponible";
                        txtApn.setText(apn);
                    } catch (Exception e) {
                        txtApn.setText("Error");
                    }
                    // Operador
                    // Devuelve directamente el nombre, ej: "Movistar" o vacío si no hay señal
                    String operador = operadorStatus.trim().isEmpty() ? "Sin señal" : operadorStatus.trim();
                    txtOperador.setText(operador);
                    // VoLTE
                    // 1 = habilitado, 0 = deshabilitado, vacío = depende del operador (valor por
                    // defecto)
                    // String volte = switch (volteStatus.trim()) {
                    //     case "1" -> "Habilitado";
                    //     case "0" -> "Deshabilitado";
                    //     default -> "Por defecto (operador)";
                    // };
                    // txtVoLTE.setText(volte);
                    String volte = volteStatus[0] + " · " + volteStatus[1];
                    if (!volteStatus[2].isEmpty())
                        volte += " (" + volteStatus[2] + ")";
                    // Ejemplo: "Soportado · Inactivo (Red GSM, requiere 4G)"
                    // "Soportado · Activo"
                    txtVoLTE.setText(volte);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void leerApn(ActionEvent event) {
        new Thread(() -> {
            try {
                // shell content query --uri content://telephony/carriers/preferapn --projection
                // name
                // shell dumpsys telephony.registry | grep -i apn
                // shell settings get global default_apn
                String apnStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell content query --uri content://telephony/carriers/preferapn --projection name");
                Platform.runLater(() -> {
                    String apn = apnStatus.contains("name=")
                            ? apnStatus.trim().replaceAll(".*name=([^,\\n]+).*", "$1").trim()
                            : "No disponible";
                    txtApn.setText(apn);
                });
            } catch (Exception e) {
                Platform.runLater(() -> txtApn.setText("Error"));
            }
        }).start();
    }

    @FXML
    private void leerOperador(ActionEvent event) {
        new Thread(() -> {
            try {
                // shell getprop gsm.operator.alpha
                // shell getprop gsm.operator.alpha.1
                // shell dumpsys telephony.registry | grep mOperatorAlphaLong
                String operadorStatus = adbService.ejecutarComandoSincrono(serial,
                        "shell getprop gsm.operator.alpha");
                Platform.runLater(() -> {
                    String operador = operadorStatus.trim().isEmpty()
                            ? "Sin señal"
                            : operadorStatus.trim();
                    txtOperador.setText(operador);
                });
            } catch (Exception e) {
                Platform.runLater(() -> txtOperador.setText("Error"));
            }
        }).start();
    }

    @FXML
    private void leerVolte(ActionEvent event) {
        new Thread(() -> {
            try {
                String[] volte = adbService.getVolteEstado(serial);
                // volte[0] = "Soportado" / "No soportado"
                // volte[1] = "Activo" / "Inactivo"
                // volte[2] = motivo si inactivo
                Platform.runLater(() -> {
                    String texto = volte[0] + " · " + volte[1];
                    if (!volte[2].isEmpty())
                        texto += " (" + volte[2] + ")";
                    // Ejemplo: "Soportado · Inactivo (Red GSM, requiere 4G)"
                    // "Soportado · Activo"
                    txtVoLTE.setText(texto);
                });
            } catch (Exception e) {
                Platform.runLater(() -> txtVoLTE.setText("Error"));
            }
        }).start();
    }
}
