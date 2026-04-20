package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.example.Model.DatabaseManager;
import com.example.Model.Soc;

public class SocDAO {
    public int insertar(Soc soc) throws SQLException {
        // 1. Buscamos si el modelo de SoC ya existe para no duplicarlo
        String sqlBusqueda = "SELECT id_soc FROM soc WHERE modelo_soc = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement psBusqueda = conn.prepareStatement(sqlBusqueda)) {

            psBusqueda.setString(1, soc.getModeloSoc());
            ResultSet rs = psBusqueda.executeQuery();

            if (rs.next()) {
                // Si ya existe en la base de datos, devolvemos su ID actual
                return rs.getInt("id_soc");
            }
        }

        // 2. Si no existe, lo insertamos normalmente
        String sqlInsert = """
                    INSERT INTO soc (fabricante, modelo_soc, arquitectura, nucleos, frecuencia_mhz)
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement psInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {

            psInsert.setString(1, soc.getFabricante());
            psInsert.setString(2, soc.getModeloSoc());
            psInsert.setString(3, soc.getArquitectura());
            psInsert.setInt(4, soc.getNucleos());
            psInsert.setString(5, soc.getFrecuenciaMhz());

            psInsert.executeUpdate();

            ResultSet keys = psInsert.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("Error al crear el nuevo SoC: no se obtuvo ID");
        }
    }

    public List<Soc> obtenerTodos() throws SQLException {
        String sql = "SELECT * FROM soc ORDER BY fabricante, modelo_soc";
        List<Soc> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public Soc buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM soc WHERE id_soc = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
            return null;
        }
    }

    Soc mapear(ResultSet rs) throws SQLException {
        Soc s = new Soc();
        s.setIdSoc(rs.getInt("id_soc"));
        s.setFabricante(rs.getString("fabricante"));
        s.setModeloSoc(rs.getString("modelo_soc"));
        s.setArquitectura(rs.getString("arquitectura"));
        s.setNucleos(rs.getInt("nucleos"));
        s.setFrecuenciaMhz(rs.getString("frecuencia_mhz"));
        return s;
    }

    public Soc buscarPorModelo(String nombreSoc) throws SQLException {
        // Convertimos ambos lados a minúsculas en la consulta SQL
        String sql = "SELECT * FROM soc WHERE LOWER(modelo_soc) = LOWER(?)";
        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreSoc);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Soc soc = new Soc();
                soc.setIdSoc(rs.getInt("id_soc"));
                soc.setModeloSoc(rs.getString("modelo_soc"));
                soc.setFabricante(rs.getString("fabricante"));
                return soc;
            }
        }
        return null;
    }
}
