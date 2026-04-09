package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.Model.DatabaseManager;
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

    public Modelo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM modelo WHERE id_modelo = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
            return null;
        }
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
}
