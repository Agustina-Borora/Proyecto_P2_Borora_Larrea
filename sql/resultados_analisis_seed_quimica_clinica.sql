-- Paso 3: datos de prueba para los análisis de "Química Clínica" que ya
-- existen en tu base (cada uno se pide como línea individual, no como un
-- panel combinado). Un solo parámetro numérico por examen.
--
-- Valores y VR tomados del informe de ejemplo que mandaste (no son
-- genéricos de manual como los del Hemograma) — igual conviene que los
-- confirmes contra lo que usa tu laboratorio antes de producción.
--
-- IDs confirmados en tu base: 2=Glucemia, 3=Colesterol total, 7=Creatinina,
-- 8=Uremia, 9=AMILASA - sérica.

INSERT INTO `analisis_parametros`
  (`id_analisis_tipo`, `nombre_parametro`, `orden_parametro`, `tipo_dato`, `unidad`, `valor_referencia`, `id_sexo`)
VALUES
  (2, 'Glucosa',    1, 'numerico', 'mg/dl', '70 - 110',   NULL),
  (7, 'Creatinina', 1, 'numerico', 'mg/dl', '0.7 - 1.30', NULL),
  (8, 'Urea',        1, 'numerico', 'mg/dl', '10 - 50',    NULL),
  (3, 'Colesterol total', 1, 'numerico', 'mg/dl', '0 - 200', NULL),
  (9, 'Amilasa',     1, 'numerico', 'UA/dl', '0 - 120',    NULL);
