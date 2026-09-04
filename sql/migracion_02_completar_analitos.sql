-- Paso 2: dejamos tu sistema real (analitos / valores_referencia /
-- pedido_analito_resultado) completo para poder probarlo con Hemograma
-- completo y Química Clínica, igual que veníamos probando.
--
-- No toca nada de lo que ya tenías cargado (Hemoglobina, Hematocrito,
-- Glóbulos blancos, Plaquetas de Hemograma; Glucosa, Colesterol, Creatinina,
-- Urea de Química Clínica) — solo agrega lo que falta:
--   - Hemograma completo (id_analisis_tipo=1): Glóbulos Rojos, VSG,
--     VCM, HCM, CHCM, y la fórmula leucocitaria (Neutrófilos, Eosinófilos,
--     Basófilos, Linfocitos, Monocitos).
--   - Amilasa (id_analisis_tipo=9), que todavía no tenía ningún analito.
--
-- También agrega una restricción UNIQUE en pedido_analito_resultado que
-- hace falta para poder guardar sin duplicar filas si se guarda dos veces
-- el mismo examen (la tabla no la tenía).

-- --- Hemograma completo: analitos que faltan ---
INSERT INTO `analitos` (`id_analisis_tipo`, `nombre_analito`, `unidad`, `orden_analito`) VALUES
  (1, 'Glóbulos Rojos', 'mill/mm³', 5),
  (1, 'VSG (Eritrosedimentación)', 'mm/1ra hora', 6),
  (1, 'VCM (Volumen Corpuscular Medio)', 'fL', 7),
  (1, 'HCM (Hemoglobina Corpuscular Media)', 'pg', 8),
  (1, 'CHCM (Conc. de Hemoglobina Corpuscular Media)', 'g/dL', 9),
  (1, 'Neutrófilos', '%', 10),
  (1, 'Eosinófilos', '%', 11),
  (1, 'Basófilos', '%', 12),
  (1, 'Linfocitos', '%', 13),
  (1, 'Monocitos', '%', 14);

-- Valores de referencia de esos analitos nuevos. Los que varían por sexo
-- (Glóbulos Rojos, VSG) van con id_sexo 1/2, como ya tenías con
-- Hemoglobina/Hematocrito; el resto va con id_sexo 3 ("No especificado"
-- = aplica a cualquiera), como ya tenías con Glóbulos Blancos/Plaquetas.
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 1, 4.5, 5.9, '4.5 - 5.9 mill/mm³' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Glóbulos Rojos';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 2, 4.0, 5.2, '4.0 - 5.2 mill/mm³' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Glóbulos Rojos';

INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 1, 0, 15, '0 - 15 mm/1ra hora' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='VSG (Eritrosedimentación)';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 2, 0, 20, '0 - 20 mm/1ra hora' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='VSG (Eritrosedimentación)';

INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 80, 100, '80 - 100 fL' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='VCM (Volumen Corpuscular Medio)';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 27, 32, '27 - 32 pg' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='HCM (Hemoglobina Corpuscular Media)';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 32, 36, '32 - 36 g/dL' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='CHCM (Conc. de Hemoglobina Corpuscular Media)';

INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 40, 70, '40 - 70 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Neutrófilos';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 1, 4, '1 - 4 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Eosinófilos';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 0, 1, '0 - 1 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Basófilos';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 20, 40, '20 - 40 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Linfocitos';
INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 2, 8, '2 - 8 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Monocitos';

-- --- Amilasa: todavía no tenía ningún analito ---
INSERT INTO `analitos` (`id_analisis_tipo`, `nombre_analito`, `unidad`, `orden_analito`) VALUES
  (9, 'Amilasa', 'UA/dl', 1);

INSERT INTO `valores_referencia` (`id_analito`, `id_sexo`, `valor_min`, `valor_max`, `texto_referencia`)
SELECT id_analito, 3, 0, 120, '0 - 120 UA/dl' FROM analitos WHERE id_analisis_tipo=9 AND nombre_analito='Amilasa';

-- --- Restricción que faltaba para poder guardar sin duplicar filas ---
ALTER TABLE `pedido_analito_resultado`
  ADD UNIQUE KEY `uq_par_pedido_analito` (`id_pedido_analisis`, `id_analito`);
