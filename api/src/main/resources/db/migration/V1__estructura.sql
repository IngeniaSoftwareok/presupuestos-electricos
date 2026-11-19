CREATE TABLE cliente (
    codigo VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    localidad VARCHAR(80) NOT NULL
);

CREATE TABLE concepto (
    codigo VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    unidad VARCHAR(16) NOT NULL,
    rubro VARCHAR(24) NOT NULL,
    precio DECIMAL(12,2) NOT NULL CHECK (precio >= 0)
);

CREATE TABLE presupuesto (
    secuencia BIGINT AUTO_INCREMENT PRIMARY KEY,
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    espacio BINARY(32) NOT NULL,
    clave CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    huella BINARY(32) NOT NULL,
    cliente_codigo VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    cliente_nombre VARCHAR(100) NOT NULL,
    cliente_localidad VARCHAR(80) NOT NULL,
    fecha TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    descuento_porcentaje DECIMAL(5,2) NOT NULL CHECK (descuento_porcentaje BETWEEN 0 AND 100),
    subtotal DECIMAL(17,2) NOT NULL CHECK (subtotal >= 0),
    descuento DECIMAL(17,2) NOT NULL,
    total DECIMAL(17,2) NOT NULL,
    CONSTRAINT presupuesto_descuento_valido CHECK (descuento BETWEEN 0 AND subtotal),
    CONSTRAINT presupuesto_total_valido CHECK (total = subtotal - descuento),
    UNIQUE KEY presupuesto_id (id),
    UNIQUE KEY presupuesto_reintento (espacio, clave),
    KEY presupuesto_historial (espacio, secuencia)
);

CREATE TABLE presupuesto_renglon (
    presupuesto_id BIGINT NOT NULL,
    posicion SMALLINT NOT NULL CHECK (posicion BETWEEN 0 AND 19),
    concepto_codigo VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    concepto_nombre VARCHAR(100) NOT NULL,
    unidad VARCHAR(16) NOT NULL,
    cantidad DECIMAL(7,3) NOT NULL CHECK (cantidad > 0),
    precio DECIMAL(12,2) NOT NULL CHECK (precio >= 0),
    importe DECIMAL(17,2) NOT NULL CHECK (importe >= 0),
    PRIMARY KEY (presupuesto_id, posicion),
    CONSTRAINT renglon_presupuesto FOREIGN KEY (presupuesto_id)
        REFERENCES presupuesto (secuencia) ON DELETE CASCADE
);
