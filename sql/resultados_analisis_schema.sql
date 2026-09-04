-- Tablas nuevas para guardar resultados de análisis.
-- Paso 1 de la guía: crea la estructura. Los datos de prueba (Hemograma
-- completo) van en un segundo script, una vez confirmados los nombres
-- reales de `sexos` y `analisis_tipos` en tu base.

SET FOREIGN_KEY_CHECKS = 0;

-- Define, por cada analisis_tipo, qué renglones de resultado tiene
-- (ej. "Hemograma completo" tiene varios; "Creatinina" tiene uno solo).
CREATE TABLE IF NOT EXISTS `analisis_parametros` (
  `id_parametro` INT NOT NULL AUTO_INCREMENT,
  `id_analisis_tipo` INT NOT NULL,
  `nombre_parametro` VARCHAR(150) NOT NULL,
  `orden_parametro` INT NOT NULL DEFAULT 0,
  `tipo_dato` ENUM('numerico', 'cualitativo', 'texto') NOT NULL DEFAULT 'numerico',
  `unidad` VARCHAR(30) NULL,
  `valor_referencia` VARCHAR(255) NULL,
  `id_sexo` INT NULL,
  `opciones_cualitativo` VARCHAR(255) NULL,
  `activo` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_parametro`),
  KEY `fk_parametro_analisis_tipo` (`id_analisis_tipo`),
  KEY `fk_parametro_sexo` (`id_sexo`),
  CONSTRAINT `fk_parametro_analisis_tipo` FOREIGN KEY (`id_analisis_tipo`)
    REFERENCES `analisis_tipos` (`id_analisis_tipo`),
  CONSTRAINT `fk_parametro_sexo` FOREIGN KEY (`id_sexo`)
    REFERENCES `sexos` (`id_sexo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Guarda el valor cargado para cada renglón, dentro de una orden puntual.
CREATE TABLE IF NOT EXISTS `resultados_analisis` (
  `id_resultado` INT NOT NULL AUTO_INCREMENT,
  `id_pedido_analisis` INT NOT NULL,
  `id_parametro` INT NOT NULL,
  `valor` VARCHAR(255) NULL,
  `observaciones` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_resultado`),
  UNIQUE KEY `uq_resultado_pedido_parametro` (`id_pedido_analisis`, `id_parametro`),
  KEY `fk_resultado_parametro` (`id_parametro`),
  CONSTRAINT `fk_resultado_pedido_analisis` FOREIGN KEY (`id_pedido_analisis`)
    REFERENCES `pedido_analisis` (`id_pedido_analisis`),
  CONSTRAINT `fk_resultado_parametro` FOREIGN KEY (`id_parametro`)
    REFERENCES `analisis_parametros` (`id_parametro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
