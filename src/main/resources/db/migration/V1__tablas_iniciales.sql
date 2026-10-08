-- Catalogo: se puede editar, gana el cambio mas reciente (actualizado_en).
-- servidor_en es la hora del servidor al guardar y sirve de cursor para /api/sync/pull.
CREATE TABLE estanques (
    id             UUID PRIMARY KEY,
    nombre         VARCHAR(80)              NOT NULL,
    tipo           VARCHAR(20)              NOT NULL,
    volumen_m3     DOUBLE PRECISION,
    activo         BOOLEAN                  NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    servidor_en    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_estanques_servidor_en ON estanques (servidor_en);

CREATE TABLE lotes (
    id               UUID PRIMARY KEY,
    estanque_id      UUID                     NOT NULL REFERENCES estanques (id),
    codigo           VARCHAR(40)              NOT NULL,
    fecha_siembra    DATE,
    cantidad_inicial INTEGER,
    peso_inicial_g   DOUBLE PRECISION,
    estado           VARCHAR(20)              NOT NULL,
    actualizado_en   TIMESTAMP WITH TIME ZONE NOT NULL,
    servidor_en      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_lotes_estanque_id ON lotes (estanque_id);
CREATE INDEX ix_lotes_servidor_en ON lotes (servidor_en);

-- Eventos: nunca se editan; el id (UUID) lo genera el celular.
CREATE TABLE lecturas_agua (
    id             UUID PRIMARY KEY,
    estanque_id    UUID                     NOT NULL REFERENCES estanques (id),
    temp_c         DOUBLE PRECISION,
    ph             DOUBLE PRECISION,
    oxigeno_mg_l   DOUBLE PRECISION,
    mv             DOUBLE PRECISION,
    dispositivo_id VARCHAR(64)              NOT NULL,
    origen         VARCHAR(20)              NOT NULL,
    registrado_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    recibido_en    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_lecturas_agua_estanque_id ON lecturas_agua (estanque_id);
CREATE INDEX ix_lecturas_agua_registrado_en ON lecturas_agua (registrado_en);

CREATE TABLE conteos (
    id               UUID PRIMARY KEY,
    lote_id          UUID                     NOT NULL REFERENCES lotes (id),
    total            INTEGER                  NOT NULL,
    cortes_multiples INTEGER,
    dispositivo_id   VARCHAR(64)              NOT NULL,
    origen           VARCHAR(20)              NOT NULL,
    registrado_en    TIMESTAMP WITH TIME ZONE NOT NULL,
    recibido_en      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_conteos_lote_id ON conteos (lote_id);

CREATE TABLE mortalidades (
    id             UUID PRIMARY KEY,
    lote_id        UUID                     NOT NULL REFERENCES lotes (id),
    cantidad       INTEGER                  NOT NULL,
    causa          VARCHAR(120),
    dispositivo_id VARCHAR(64)              NOT NULL,
    origen         VARCHAR(20)              NOT NULL,
    registrado_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    recibido_en    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_mortalidades_lote_id ON mortalidades (lote_id);

CREATE TABLE alimentaciones (
    id             UUID PRIMARY KEY,
    lote_id        UUID                     NOT NULL REFERENCES lotes (id),
    kg             DOUBLE PRECISION         NOT NULL,
    dispositivo_id VARCHAR(64)              NOT NULL,
    origen         VARCHAR(20)              NOT NULL,
    registrado_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    recibido_en    TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_alimentaciones_lote_id ON alimentaciones (lote_id);

CREATE TABLE biometrias (
    id              UUID PRIMARY KEY,
    lote_id         UUID                     NOT NULL REFERENCES lotes (id),
    peso_promedio_g DOUBLE PRECISION         NOT NULL,
    muestra         INTEGER,
    dispositivo_id  VARCHAR(64)              NOT NULL,
    origen          VARCHAR(20)              NOT NULL,
    registrado_en   TIMESTAMP WITH TIME ZONE NOT NULL,
    recibido_en     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_biometrias_lote_id ON biometrias (lote_id);

-- Alertas: las genera el servidor al recibir lecturas fuera de rango.
CREATE TABLE alertas (
    id          UUID PRIMARY KEY,
    estanque_id UUID                     NOT NULL REFERENCES estanques (id),
    lectura_id  UUID                     NOT NULL REFERENCES lecturas_agua (id),
    variable    VARCHAR(20)              NOT NULL,
    valor       DOUBLE PRECISION         NOT NULL,
    nivel       VARCHAR(15)              NOT NULL,
    mensaje     VARCHAR(200)             NOT NULL,
    medido_en   TIMESTAMP WITH TIME ZONE NOT NULL,
    creada_en   TIMESTAMP WITH TIME ZONE NOT NULL,
    atendida    BOOLEAN                  NOT NULL
);
CREATE INDEX ix_alertas_estanque_id ON alertas (estanque_id);
CREATE INDEX ix_alertas_atendida ON alertas (atendida);
