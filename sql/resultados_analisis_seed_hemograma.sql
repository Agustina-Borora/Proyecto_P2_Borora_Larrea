-- Paso 2: datos de prueba para "Hemograma completo" (id_analisis_tipo = 1,
-- confirmado en tu base) — para poder probar el flujo de punta a punta en
-- Registrar Resultados. Los valores de referencia son rangos adultos
-- generales; ajustalos cuando tengas los definitivos del laboratorio.
--
-- Sexos confirmados en tu base: 1 = Masculino, 2 = Femenino.
-- Los parámetros sin id_sexo (NULL) aplican para cualquier paciente.

INSERT INTO `analisis_parametros`
  (`id_analisis_tipo`, `nombre_parametro`, `orden_parametro`, `tipo_dato`, `unidad`, `valor_referencia`, `id_sexo`)
VALUES
  (1, 'Hematocrito (Hto)', 1, 'numerico', '%',        '40 - 54', 1),
  (1, 'Hematocrito (Hto)', 1, 'numerico', '%',        '37 - 47', 2),

  (1, 'Hemoglobina',       2, 'numerico', 'g/dL',      '13.5 - 17.5', 1),
  (1, 'Hemoglobina',       2, 'numerico', 'g/dL',      '12.0 - 15.5', 2),

  (1, 'Glóbulos Rojos',    3, 'numerico', 'mill/mm³',  '4.5 - 5.9', 1),
  (1, 'Glóbulos Rojos',    3, 'numerico', 'mill/mm³',  '4.0 - 5.2', 2),

  (1, 'Glóbulos Blancos',  4, 'numerico', '/mm³',      '4500 - 11000', NULL),
  (1, 'Plaquetas',         5, 'numerico', '/mm³',      '150000 - 450000', NULL),

  (1, 'VSG (Eritrosedimentación)', 6, 'numerico', 'mm/1ra hora', '0 - 15', 1),
  (1, 'VSG (Eritrosedimentación)', 6, 'numerico', 'mm/1ra hora', '0 - 20', 2),

  -- Índices Hematimétricos
  (1, 'VCM (Volumen Corpuscular Medio)', 7, 'numerico', 'fL',   '80 - 100', NULL),
  (1, 'HCM (Hemoglobina Corpuscular Media)', 8, 'numerico', 'pg', '27 - 32', NULL),
  (1, 'CHCM (Conc. de Hemoglobina Corpuscular Media)', 9, 'numerico', 'g/dL', '32 - 36', NULL),

  -- Fórmula Leucocitaria
  (1, 'Neutrófilos',  10, 'numerico', '%', '40 - 70', NULL),
  (1, 'Eosinófilos',  11, 'numerico', '%', '1 - 4',   NULL),
  (1, 'Basófilos',    12, 'numerico', '%', '0 - 1',   NULL),
  (1, 'Linfocitos',   13, 'numerico', '%', '20 - 40', NULL),
  (1, 'Monocitos',    14, 'numerico', '%', '2 - 8',   NULL);
