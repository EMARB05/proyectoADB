-- schema.sql
-- BBDD: Android Engineering & Automation Suite

PRAGMA foreign_keys = ON;

-- Tabla de marcas (Samsung, Xiaomi, Google...)
CREATE TABLE IF NOT EXISTS marca (
    id_marca    INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre      TEXT NOT NULL UNIQUE,
    pais_origen TEXT
);

-- Tabla de SoC (procesadores: Snapdragon 8 Gen 2, Dimensity 9200...)
CREATE TABLE IF NOT EXISTS soc (
    id_soc          INTEGER PRIMARY KEY AUTOINCREMENT,
    fabricante      TEXT NOT NULL,       -- Qualcomm, MediaTek, Samsung...
    modelo_soc      TEXT NOT NULL UNIQUE,
    arquitectura    TEXT,                -- ARM64, x86...
    nucleos         INTEGER,
    frecuencia_mhz  TEXT                 -- Guardamos como texto: "3200 MHz"
);

-- Tabla de modelos de teléfono (Galaxy S22, Pixel 7...)
CREATE TABLE IF NOT EXISTS modelo (
    id_modelo           INTEGER PRIMARY KEY AUTOINCREMENT,
    id_marca            INTEGER NOT NULL REFERENCES marca(id_marca),
    id_soc              INTEGER REFERENCES soc(id_soc),
    nombre_modelo       TEXT NOT NULL,
    ram_gb              INTEGER,
    almacenamiento_gb   INTEGER,
    so_version          TEXT,            -- "Android 13"
    pantalla_pulgadas   TEXT,
    camara_mp           TEXT
);

-- Tabla de dispositivos físicos (un Samsung S22 concreto con su serial)
CREATE TABLE IF NOT EXISTS dispositivo (
    id_dispositivo  INTEGER PRIMARY KEY AUTOINCREMENT,
    id_modelo       INTEGER NOT NULL REFERENCES modelo(id_modelo),
    serial_number   TEXT NOT NULL UNIQUE,   -- Clave de búsqueda al conectar por ADB
    estado          TEXT DEFAULT 'activo',  -- activo, baja, reparacion...
    notas           TEXT,
    fecha_registro  TEXT DEFAULT (DATE('now'))
);

-- Catálogo de bandas de frecuencia (LTE B3, 5G n78...)
CREATE TABLE IF NOT EXISTS banda (
    id_banda        INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo            TEXT NOT NULL,   -- LTE, 5G, GSM, UMTS
    numero_banda    TEXT NOT NULL,   -- "B3", "n78", "B20"...
    frecuencia_mhz  TEXT,
    tecnologia      TEXT,            -- VoLTE, VoNR...
    UNIQUE(tipo, numero_banda)
);

-- Tabla intermedia: qué bandas soporta cada modelo
CREATE TABLE IF NOT EXISTS modelo_banda (
    id_modelo   INTEGER NOT NULL REFERENCES modelo(id_modelo),
    id_banda    INTEGER NOT NULL REFERENCES banda(id_banda),
    PRIMARY KEY (id_modelo, id_banda)
);

-- Fotos del modelo (almacenadas como BLOB o como ruta/URL)
CREATE TABLE IF NOT EXISTS foto (
    id_foto         INTEGER PRIMARY KEY AUTOINCREMENT,
    id_modelo       INTEGER NOT NULL REFERENCES modelo(id_modelo),
    datos_imagen    BLOB,            -- Imagen en binario (opcional)
    url_externa     TEXT,            -- O ruta relativa: "img/samsung_s22.png"
    descripcion     TEXT             -- "Vista frontal", "Vista trasera"...
);