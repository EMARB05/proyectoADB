/* import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.example.Controller.ADBService;
import com.example.Controller.BandaDAO;
import com.example.Controller.DispositivoDAO;
import com.example.Controller.MarcaDAO;
import com.example.Controller.ModeloDAO;
import com.example.Controller.SocDAO;
import com.example.Model.Banda;
import com.example.Model.Dispositivo;
import com.example.Model.Marca;
import com.example.Model.Modelo;
import com.example.Model.Soc;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseTest {

    private static MarcaDAO      marcaDAO;
    private static ADBService adbService;
    private static SocDAO        socDAO;
    private static ModeloDAO     modeloDAO;
    private static DispositivoDAO dispositivoDAO;
    private static BandaDAO      bandaDAO;

    // IDs generados durante el test, los reutilizamos entre métodos
    private static int idMarca;
    private static int idSoc;
    private static int idModelo;
    private static int idBanda;

    @BeforeAll
    static void setup() throws SQLException {
        marcaDAO       = new MarcaDAO();
        socDAO         = new SocDAO();
        modeloDAO      = new ModeloDAO();
        dispositivoDAO = new DispositivoDAO();
        bandaDAO       = new BandaDAO();
        adbService = new ADBService();
    }

    // ------------------------------------------------------------------
    // 1. Insertar datos base
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void insertarMarca() throws SQLException {
        Marca marca = new Marca("Samsung", "Corea del Sur");
        idMarca = marcaDAO.insertar(marca);

        assertTrue(idMarca > 0, "El ID generado debe ser mayor que 0");
        System.out.println("Marca insertada con ID: " + idMarca);
    }

    @Test
    @Order(2)
    void insertarSoc() throws SQLException {
        Soc soc = new Soc("Qualcomm", "Snapdragon 8 Gen 2");
        soc.setArquitectura("ARM64");
        soc.setNucleos(8);
        soc.setFrecuenciaMhz("3200");

        idSoc = socDAO.insertar(soc);
        assertTrue(idSoc > 0, "El ID generado debe ser mayor que 0");
        System.out.println("SoC insertado con ID: " + idSoc);
    }

    @Test
    @Order(3)
    void insertarModelo() throws SQLException {
        Marca marca = marcaDAO.buscarPorId(idMarca);
        Soc   soc   = socDAO.buscarPorId(idSoc);

        Modelo modelo = new Modelo(marca, "Galaxy S22");
        modelo.setSoc(soc);
        modelo.setRamGb(8);
        modelo.setAlmacenamientoGb(128);
        modelo.setSoVersion("Android 13");
        modelo.setPantallaPulgadas("6.1");
        modelo.setCamaraMp("50");

        idModelo = modeloDAO.insertar(modelo);
        assertTrue(idModelo > 0, "El ID generado debe ser mayor que 0");
        System.out.println("Modelo insertado con ID: " + idModelo);
    }

    @Test
    @Order(4)
    void insertarBandaYAsociar() throws SQLException {
        Banda banda = new Banda("LTE", "B3");
        banda.setFrecuenciaMhz("1800");
        banda.setTecnologia("VoLTE");

        idBanda = bandaDAO.insertar(banda);
        assertTrue(idBanda > 0, "El ID generado debe ser mayor que 0");

        bandaDAO.asociarAModelo(idModelo, idBanda);

        // Verificamos que la asociación se guardó
        List<Banda> bandas = bandaDAO.obtenerPorModelo(idModelo);
        assertFalse(bandas.isEmpty(), "El modelo debe tener al menos una banda");
        assertEquals("B3", bandas.get(0).getNumeroBanda());

        System.out.println("Banda insertada y asociada al modelo");
    }

    // ------------------------------------------------------------------
    // 2. El flujo clave del Bloque 1: buscar por serial number
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    void insertarDispositivo() throws SQLException {
        Modelo modelo = modeloDAO.buscarPorId(idModelo);

        Dispositivo dispositivo = new Dispositivo(modelo, "R5CT103ABCD","1234456");
        dispositivo.setEstado("activo");
        dispositivo.setNotas("Dispositivo de prueba");

        int id = dispositivoDAO.insertar(dispositivo);
        assertTrue(id > 0, "El ID generado debe ser mayor que 0");
        System.out.println("Dispositivo insertado con ID: " + id);
    }

    @Test
    @Order(6)
    void buscarPorSerialExistente() throws SQLException {
        Dispositivo dispositivo = dispositivoDAO.buscarPorSerial("R5CT103ABCD");

        assertNotNull(dispositivo, "Debe encontrar el dispositivo por serial");
        assertEquals("R5CT103ABCD", dispositivo.getSerialNumber());
        assertNotNull(dispositivo.getModelo(), "El modelo no debe ser null");
        assertEquals("Galaxy S22", dispositivo.getModelo().getNombreModelo());
        assertNotNull(dispositivo.getModelo().getMarca(), "La marca no debe ser null");
        assertEquals("Samsung", dispositivo.getModelo().getMarca().getNombre());

        System.out.println("Dispositivo encontrado: " + dispositivo);
        System.out.println("  Modelo : " + dispositivo.getModelo());
        System.out.println("  Marca  : " + dispositivo.getModelo().getMarca());
        System.out.println("  SoC    : " + dispositivo.getModelo().getSoc());
    }

    @Test
    @Order(7)
    void buscarPorSerialInexistente() throws SQLException {
        Dispositivo dispositivo = dispositivoDAO.buscarPorSerial("SERIAL_QUE_NO_EXISTE");

        assertNull(dispositivo, "Debe devolver null si el serial no está registrado");
        System.out.println("✓ Serial desconocido devuelve null correctamente");
    }
    @Test
    @Order(8)
    void obtenerProps() throws IOException {
        List<String> lista= adbService.obtenerDispositivosConectados();
        for (String string : lista) {
            Dispositivo di= adbService.obtenerProps(string);
            System.out.println(di);
        }
    }
}
 */