-- Tabla de artículos del corpus.
-- IF NOT EXISTS: se puede ejecutar en cada inicio sin borrar los datos.
CREATE TABLE IF NOT EXISTS articulos (
                                         id        SERIAL PRIMARY KEY,
                                         archivo   VARCHAR(255) NOT NULL UNIQUE,   -- nombre del PDF, evita duplicados
    titulo    TEXT,
    autores   TEXT,
    resumen   TEXT NOT NULL,                  -- el abstract
    manual    BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE si se corrigió a mano
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );