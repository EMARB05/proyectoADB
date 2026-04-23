package com.example.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.Model.DatabaseManager;
import com.example.Model.Foto;

public class FotoDAO {
public int insertar(Foto foto) throws SQLException {
        String sql = "INSERT INTO foto (id_modelo, url_externa, descripcion) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, foto.getIdModelo());
            ps.setString(2, foto.getUrlExterna());   
            ps.setString(3, foto.getDescripcion());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            throw new SQLException("No se obtuvo ID tras insertar foto");
        }
    }

    

    // Obtener todas las fotos de un modelo
    public List<Foto> obtenerPorModelo(int idModelo) throws SQLException {
        String sql = "SELECT * FROM foto WHERE id_modelo = ?";
        List<Foto> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idModelo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void eliminar(int idFoto) throws SQLException {
        String sql = "DELETE FROM foto WHERE id_foto = ?";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idFoto);
            ps.executeUpdate();
        }
    }

    Foto mapear(ResultSet rs) throws SQLException {
        Foto f = new Foto();
        f.setIdFoto(rs.getInt("id_foto"));
        f.setIdModelo(rs.getInt("id_modelo"));
        f.setUrlExterna(rs.getString("url_externa"));
        f.setDescripcion(rs.getString("descripcion"));
        return f;
    }
}
