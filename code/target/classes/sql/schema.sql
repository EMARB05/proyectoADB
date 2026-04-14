PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS marca (
    id_marca INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    pais_origen TEXT
);

CREATE TABLE IF NOT EXISTS soc (
    id_soc INTEGER PRIMARY KEY AUTOINCREMENT,
    fabricante TEXT NOT NULL,
    modelo_soc TEXT NOT NULL UNIQUE,
    arquitectura TEXT,
    nucleos INTEGER,
    frecuencia_mhz TEXT
);

CREATE TABLE IF NOT EXISTS modelo (
    id_modelo INTEGER PRIMARY KEY AUTOINCREMENT,
    id_marca INTEGER NOT NULL REFERENCES marca(id_marca),
    id_soc INTEGER REFERENCES soc(id_soc),
    nombre_modelo TEXT NOT NULL,
    ram_gb INTEGER,
    almacenamiento_gb INTEGER,
    so_version TEXT,
    pantalla_pulgadas TEXT,
    camara_mp TEXT
);

CREATE TABLE IF NOT EXISTS dispositivo (
    id_dispositivo INTEGER PRIMARY KEY AUTOINCREMENT,
    id_modelo INTEGER NOT NULL REFERENCES modelo(id_modelo),
    serial_number TEXT NOT NULL UNIQUE,
    estado TEXT DEFAULT 'activo',
    notas TEXT,
    fecha_registro TEXT DEFAULT (DATE('now'))
);

CREATE TABLE IF NOT EXISTS banda (
    id_banda INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,
    numero_banda TEXT NOT NULL,
    frecuencia_mhz TEXT,
    tecnologia TEXT,
    UNIQUE(tipo, numero_banda)
);

CREATE TABLE IF NOT EXISTS modelo_banda (
    id_modelo INTEGER NOT NULL REFERENCES modelo(id_modelo),
    id_banda INTEGER NOT NULL REFERENCES banda(id_banda),
    PRIMARY KEY (id_modelo, id_banda)
);

CREATE TABLE IF NOT EXISTS foto (
    id_foto INTEGER PRIMARY KEY AUTOINCREMENT,
    id_modelo INTEGER NOT NULL REFERENCES modelo(id_modelo),
    url TEXT,
    descripcion TEXT
);