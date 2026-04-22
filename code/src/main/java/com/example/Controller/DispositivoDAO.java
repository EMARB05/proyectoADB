package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.example.Model.DatabaseManager;
import com.example.Model.Dispositivo;

public class DispositivoDAO {
    private final ModeloDAO modeloDAO = new ModeloDAO();

    // LA consulta clave del Bloque 1: buscar por Serial Number
    // Devuelve null si el dispositivo no está registrado todavía
    public Dispositivo buscarPorSerial(String serial) throws SQLException {
        String sql = "SELECT * FROM dispositivo WHERE serial_number = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, serial);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
            return null; // El dispositivo es nuevo, hay que darlo de alta
        }
    }

    public Dispositivo buscarPorAndroidId(String androidId) throws SQLException {
    String sql = "SELECT * FROM dispositivo WHERE android_id = ?";
    try (Connection conn = DatabaseManager.getInstance().getConexion();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, androidId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapear(rs);
        return null;
    }
}

    public int insertar(Dispositivo dispositivo) throws SQLException {
        String sql = """
                INSERT INTO dispositivo (id_modelo, serial_number, android_id, notas)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, dispositivo.getModelo().getIdModelo());
            ps.setString(2, dispositivo.getSerialNumber());
            ps.setString(3, dispositivo.getAndroid_id());
            ps.setString(4, dispositivo.getNotas());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next())
                return keys.getInt(1);
            throw new SQLException("No se obtuvo ID tras insertar dispositivo");
        }
    }

    public void actualizar(Dispositivo dispositivo) throws SQLException {
        String sql = """
                UPDATE dispositivo
                SET id_modelo = ?, notas = ?
                WHERE serial_number = ?
                """;

        try (Connection conn = DatabaseManager.getInstance().getConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dispositivo.getModelo().getIdModelo());
            ps.setString(2, dispositivo.getNotas());
            ps.setString(3, dispositivo.getSerialNumber());
            ps.executeUpdate();
        }
    }

    Dispositivo mapear(ResultSet rs) throws SQLException {
        Dispositivo d = new Dispositivo();
        d.setIdDispositivo(rs.getInt("id_dispositivo"));
        d.setSerialNumber(rs.getString("serial_number"));
        d.setAndroid_id(rs.getString("android_id"));
        d.setNotas(rs.getString("notas"));
        d.setFechaRegistro(rs.getString("fecha_registro"));

        // Resolvemos la FK al modelo completo (con su marca y SoC incluidos)
        d.setModelo(modeloDAO.buscarPorId(rs.getInt("id_modelo")));
        return d;
    }
}
