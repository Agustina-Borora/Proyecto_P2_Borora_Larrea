-- Paso 5: agrega una columna `tipo_dato` a `analitos`, aditiva, no borra
-- ni cambia nada existente. Hace falta para los casos donde el resultado
-- es un número pero el valor de referencia no es un rango único (ej.
-- Citomegalovirus IgG/IgM: el resultado es un número, pero el VR son 3
-- bandas de interpretación) o directamente no hay VR todavía (RDW-CV) --
-- sin esto, el sistema los mostraba como campo de texto en vez de
-- numérico, solo por no tener un valor_min/valor_max cargado.

ALTER TABLE `analitos`
  ADD COLUMN `tipo_dato` ENUM('numerico','cualitativo','texto') NOT NULL DEFAULT 'numerico'
  AFTER `nombre_analito`;

-- Deja los existentes como venían funcionando: numérico si tienen un VR
-- con rango, texto si el VR es puramente descriptivo (Color, Aspecto).
UPDATE `analitos` a
SET a.tipo_dato = CASE
    WHEN EXISTS (
      SELECT 1 FROM valores_referencia vr
      WHERE vr.id_analito = a.id_analito AND vr.valor_min IS NOT NULL
    ) THEN 'numerico'
    ELSE 'texto'
  END;

-- RDW-CV: el resultado SÍ es un número (13.5%), aunque todavía no tenga
-- VR cargado -- lo marcamos explícito para que no quede como texto.
UPDATE `analitos` SET tipo_dato = 'numerico'
WHERE id_analisis_tipo = 1 AND nombre_analito = 'RDW-CV';
