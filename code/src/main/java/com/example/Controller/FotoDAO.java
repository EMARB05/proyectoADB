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
public void insertar(int idModelo, Foto foto) throws SQLException {
        // Asumo que tu tabla se llama 'foto' y tiene columnas 'url', 'descripcion' e 'id_modelo'
        String sql = "INSERT INTO foto (url, descripcion, id_modelo) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, foto.getUrl());
            ps.setString(2, foto.getDescripcion());
            ps.setInt(3, idModelo); // Aquí vinculamos la foto con el modelo recién creado

            ps.executeUpdate();
            System.out.println("✅ Foto vinculada al modelo " + idModelo);
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
        f.setUrl(rs.getString("url"));
        f.setDescripcion(rs.getString("descripcion"));
        return f;
    }
}
