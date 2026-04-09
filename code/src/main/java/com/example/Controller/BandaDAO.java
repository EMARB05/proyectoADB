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

public class BandaDAO {
    public int insertar(Banda banda) throws SQLException {
        String sql = """
                INSERT INTO banda (tipo, numero_banda, frecuencia_mhz, tecnologia)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, banda.getTipo());
            ps.setString(2, banda.getNumeroBanda());
            ps.setString(3, banda.getFrecuenciaMhz());
            ps.setString(4, banda.getTecnologia());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("No se obtuvo ID tras insertar banda");
        }
    }

    public List<Banda> obtenerTodas() throws SQLException {
        String sql = "SELECT * FROM banda ORDER BY tipo, numero_banda";
        List<Banda> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    // Obtener todas las bandas asociadas a un modelo concreto
    public List<Banda> obtenerPorModelo(int idModelo) throws SQLException {
        String sql = """
                SELECT b.* FROM banda b
                INNER JOIN modelo_banda mb ON b.id_banda = mb.id_banda
                WHERE mb.id_modelo = ?
                ORDER BY b.tipo, b.numero_banda
                """;
        List<Banda> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idModelo);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    // Asociar una banda a un modelo (tabla intermedia modelo_banda)
    public void asociarAModelo(int idModelo, int idBanda) throws SQLException {
        String sql = "INSERT OR IGNORE INTO modelo_banda (id_modelo, id_banda) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idModelo);
            ps.setInt(2, idBanda);
            ps.executeUpdate();
        }
    }

    // Eliminar todas las bandas asociadas a un modelo (útil al editar)
    public void desasociarDeModelo(int idModelo) throws SQLException {
        String sql = "DELETE FROM modelo_banda WHERE id_modelo = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idModelo);
            ps.executeUpdate();
        }
    }

    Banda mapear(ResultSet rs) throws SQLException {
        Banda b = new Banda();
        b.setIdBanda(rs.getInt("id_banda"));
        b.setTipo(rs.getString("tipo"));
        b.setNumeroBanda(rs.getString("numero_banda"));
        b.setFrecuenciaMhz(rs.getString("frecuencia_mhz"));
        b.setTecnologia(rs.getString("tecnologia"));
        return b;
    }
}
