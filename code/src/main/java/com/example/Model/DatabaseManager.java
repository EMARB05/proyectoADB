package com.example.Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String APP_FOLDER = "AEASuite";
    private static final String DB_NAME    = "dispositivos.db";

    private static DatabaseManager instancia;
    private final String urlJdbc;

    private DatabaseManager() throws SQLException {
        Path rutaDb = resolverRutaBD();
        this.urlJdbc = "jdbc:sqlite:" + rutaDb.toString();
        inicializarEsquema();
    }

    public static DatabaseManager getInstance() throws SQLException {
        if (instancia == null) {
            instancia = new DatabaseManager();
        }
        return instancia;
    }

    public Connection getConexion() throws SQLException {
        Connection conn = DriverManager.getConnection(urlJdbc);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    // ---------------------------------------------------------------
    // Privados
    // ---------------------------------------------------------------

    private Path resolverRutaBD() throws SQLException {
        String appData = System.getenv("APPDATA");
        // Fallback para Linux
        if (appData == null) {
            appData = System.getProperty("user.home") + "/.config";
        }

        Path carpeta = Paths.get(appData, APP_FOLDER);
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new SQLException("No se pudo crear la carpeta de datos: " + carpeta, e);
        }

        return carpeta.resolve(DB_NAME);
    }

    private void inicializarEsquema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(urlJdbc);
             InputStream is = getClass().getResourceAsStream("/sql/schema.sql")) {

            if (is == null) {
                throw new SQLException("No se encontró schema.sql en resources/sql/");
            }

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            ejecutarScript(conn, sql);

        } catch (IOException e) {
            throw new SQLException("Error leyendo schema.sql", e);
        }
    }

    private void ejecutarScript(Connection conn, String script) throws SQLException {
        String[] sentencias = script.split(";");
        try (Statement st = conn.createStatement()) {
            for (String sentencia : sentencias) {
                String limpia = sentencia.strip();
                if (!limpia.isEmpty()) {
                    st.execute(limpia);
                }
            }
        }
    }
}
