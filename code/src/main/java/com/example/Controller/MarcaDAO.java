package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.Model.DatabaseManager;
import com.example.Model.Marca;

public class MarcaDAO {
    // Insertar una marca nueva, devuelve el ID generado
    public int insertar(Marca marca) throws SQLException {
        // 1. Primero intentamos buscar si la marca ya existe
        String sqlBusqueda = "SELECT id_marca FROM marca WHERE nombre = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement psBusqueda = conn.prepareStatement(sqlBusqueda)) {

            psBusqueda.setString(1, marca.getNombre());
            ResultSet rs = psBusqueda.executeQuery();

            if (rs.next()) {
                // Si existe, retornamos el ID que ya tiene
                return rs.getInt("id_marca");
            }
        }

        // 2. Si no existe, procedemos con el INSERT original
        String sqlInsert = "INSERT INTO marca (nombre, pais_origen) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement psInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {

            psInsert.setString(1, marca.getNombre());
            psInsert.setString(2, marca.getPaisOrigen());
            psInsert.executeUpdate();

            ResultSet keys = psInsert.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("No se pudo obtener el ID de la nueva marca");
        }
    }

    // Obtener todas las marcas (útil para poblar un ComboBox en la UI)
    public List<Marca> obtenerTodas() throws SQLException {
        String sql = "SELECT * FROM marca ORDER BY nombre";
        List<Marca> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Marca buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM marca WHERE id_marca = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
            return null;
        }
    }

    public Marca buscarPorNombre(String nombreMarca) throws SQLException {
        // Convertimos ambos lados a minúsculas en la consulta SQL
        String sql = "SELECT * FROM marca WHERE LOWER(nombre) = LOWER(?)";
        try (Connection conn = DatabaseManager.getInstance().getConexion();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreMarca);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Marca m = new Marca();
                m.setIdMarca(rs.getInt("id_marca"));
                m.setNombre(rs.getString("nombre")); 
                return m;
            }
        }
        return null;
    }

    // Convierte una fila del ResultSet en un objeto Marca
    Marca mapear(ResultSet rs) throws SQLException {
        Marca m = new Marca();
        m.setIdMarca(rs.getInt("id_marca"));
        m.setNombre(rs.getString("nombre"));
        m.setPaisOrigen(rs.getString("pais_origen"));
        return m;
    }

}
