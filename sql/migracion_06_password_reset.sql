-- migracion_06_password_reset.sql
-- Ejecutar una sola vez sobre la base "laboratorio", después de las
-- migraciones anteriores (migracion_01 a migracion_05).
--
-- Habilita dos cosas nuevas:
--   1) Que la columna password_usuario pueda guardar un hash (bastante más
--      largo que una contraseña en texto plano).
--   2) La tabla de códigos de verificación para "olvidé mi contraseña".

-- 1) El hash que genera controlador.PasswordHasher tiene el formato
--    "iteraciones:saltBase64:hashBase64", que ronda los 100 caracteres.
--    Si la columna ya es lo bastante ancha esto no rompe nada (MODIFY es
--    seguro de re-ejecutar).
ALTER TABLE usuarios MODIFY password_usuario VARCHAR(255) NOT NULL;

-- 2) Tabla para los códigos de 6 dígitos del flujo "olvidé mi contraseña".
--    El código nunca se guarda en texto plano: se guarda hasheado con el
--    mismo PasswordHasher que las contraseñas, así que ni con acceso
--    directo a la base se puede leer el código vigente de nadie.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id_token INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_password_reset_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario)
);

-- Nota: las contraseñas que ya existan en texto plano NO hace falta
-- migrarlas a mano con este script. La próxima vez que cada usuario
-- inicie sesión correctamente, conexiones.Usuario.ingresar() detecta que
-- todavía está en texto plano y la re-hashea sola, de forma transparente.
