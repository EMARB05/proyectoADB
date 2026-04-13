package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.Model.Banda;
import com.example.Model.DatabaseManager;
import com.example.Model.Foto;
import com.example.Model.Modelo;

public class ModeloDAO {
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final SocDAO socDAO = new SocDAO();

    public int insertar(Modelo modelo) throws SQLException {
        String sql = """
                INSERT INTO modelo
                    (id_marca, id_soc, nombre_modelo, ram_gb, almacenamiento_gb,
                     so_version, pantalla_pulgadas, camara_mp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, modelo.getMarca().getIdMarca());
            ps.setInt(2, modelo.getSoc() != null ? modelo.getSoc().getIdSoc() : 0);
            ps.setString(3, modelo.getNombreModelo());
            ps.setInt(4, modelo.getRamGb());
            ps.setInt(5, modelo.getAlmacenamientoGb());
            ps.setString(6, modelo.getSoVersion());
            ps.setString(7, modelo.getPantallaPulgadas());
            ps.setString(8, modelo.getCamaraMp());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("No se obtuvo ID tras insertar modelo");
        }
    }

    public List<Modelo> obtenerTodos() throws SQLException {
        String sql = "SELECT * FROM modelo ORDER BY nombre_modelo";
        List<Modelo> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    /*
     * public Modelo buscarPorId(int id) throws SQLException {
     * String sql = "SELECT * FROM modelo WHERE id_modelo = ?";
     * 
     * try (Connection conn = DatabaseManager.getInstance().getConexion();
     * PreparedStatement ps = conn.prepareStatement(sql)) {
     * 
     * ps.setInt(1, id);
     * ResultSet rs = ps.executeQuery();
     * if (rs.next())
     * return mapear(rs);
     * return null;
     * }
     * }
     */

    public Modelo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM modelo WHERE id_modelo = ?";
        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Modelo m = new Modelo();
                m.setIdModelo(rs.getInt("id_modelo"));
                m.setNombreModelo(rs.getString("nombre"));
                // ... rellenar marca, soc, etc ...

                // --- AQUÍ ESTÁ LA MAGIA ---
                m.setBandas(obtenerBandas(id)); // Método que hace SELECT a la tabla bandas
                m.setFotos(obtenerFotos(id)); // Método que hace SELECT a la tabla fotos

                return m;
            }
        }
        return null;
    }

    // Ensambla el objeto completo: consulta marca y soc por sus IDs
    Modelo mapear(ResultSet rs) throws SQLException {
        Modelo m = new Modelo();
        m.setIdModelo(rs.getInt("id_modelo"));
        m.setNombreModelo(rs.getString("nombre_modelo"));
        m.setRamGb(rs.getInt("ram_gb"));
        m.setAlmacenamientoGb(rs.getInt("almacenamiento_gb"));
        m.setSoVersion(rs.getString("so_version"));
        m.setPantallaPulgadas(rs.getString("pantalla_pulgadas"));
        m.setCamaraMp(rs.getString("camara_mp"));

        // Resolvemos las FK a objetos completos
        m.setMarca(marcaDAO.buscarPorId(rs.getInt("id_marca")));
        m.setSoc(socDAO.buscarPorId(rs.getInt("id_soc")));
        return m;
    }

    private List<Banda> obtenerBandas(int idModelo) throws SQLException {
    List<Banda> bandas = new ArrayList<>();
    // Traemos todos los detalles de la banda haciendo el JOIN
    String sql = "SELECT b.* FROM banda b " +
                 "JOIN modelo_banda mb ON b.id_banda = mb.id_banda " +
                 "WHERE mb.id_modelo = ?";

    try (Connection conn = DatabaseManager.getInstance().getConexion();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, idModelo);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Banda b = new Banda();
                b.setIdBanda(rs.getInt("id_banda"));
                b.setTipo(rs.getString("tipo"));               // LTE, 5G...
                b.setNumeroBanda(rs.getString("numero_banda")); // B3, n78...
                b.setFrecuenciaMhz(rs.getString("frecuencia_mhz"));
                b.setTecnologia(rs.getString("tecnologia"));
                
                bandas.add(b);
            }
        }
    }
    return bandas;
}

    private List<Foto> obtenerFotos(int idModelo) throws SQLException {
        List<Foto> fotos = new ArrayList<>();
        String sql = "SELECT * FROM foto WHERE id_modelo = ?";
        // Ejecutas consulta y añades a la lista...
        return fotos;
    }
}
