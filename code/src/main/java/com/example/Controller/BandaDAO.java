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
        // 1. Intentamos insertar usando 'OR IGNORE'.
        // Si la banda (tipo + numero) ya existe, no hace nada y no da error.
        String sqlInsert = """
                INSERT OR IGNORE INTO banda (tipo, numero_banda, frecuencia_mhz, tecnologia)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setString(1, banda.getTipo());
            ps.setString(2, banda.getNumeroBanda());
            ps.setString(3, banda.getFrecuenciaMhz());
            ps.setString(4, banda.getTecnologia());
            ps.executeUpdate();
        }

        // 2. Buscamos el ID. Tanto si se acaba de crear como si ya existía,
        // necesitamos el ID para la tabla intermedia 'modelo_banda'.
        String sqlSelect = "SELECT id_banda FROM banda WHERE tipo = ? AND numero_banda = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, banda.getTipo());
            ps.setString(2, banda.getNumeroBanda());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_banda");
                }
            }
        }

        throw new SQLException("Error crítico: No se pudo insertar ni recuperar la banda.");
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
