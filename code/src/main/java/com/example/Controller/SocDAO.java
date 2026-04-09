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
        String sql = """
                INSERT INTO soc (fabricante, modelo_soc, arquitectura, nucleos, frecuencia_mhz)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, soc.getFabricante());
            ps.setString(2, soc.getModeloSoc());
            ps.setString(3, soc.getArquitectura());
            ps.setInt(4, soc.getNucleos());
            ps.setString(5, soc.getFrecuenciaMhz());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("No se obtuvo ID tras insertar SoC");
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
}
