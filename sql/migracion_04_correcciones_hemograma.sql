-- Paso 4: corrige los parámetros de Hemograma completo que agregué yo con
-- valores genéricos, contra los datos reales de tu plantilla
-- "RUTINA COMPLETA.docx". No toca Hemoglobina/Hematocrito/Glóbulos
-- blancos/Plaquetas (esos ya estaban cargados por vos desde antes).

-- --- Glóbulos Rojos: en tu plantilla NO se separa por sexo (un solo VR) ---
DELETE vr FROM valores_referencia vr
  JOIN analitos a ON a.id_analito = vr.id_analito
  WHERE a.id_analisis_tipo = 1 AND a.nombre_analito = 'Glóbulos Rojos';

INSERT INTO valores_referencia (id_analito, id_sexo, valor_min, valor_max, texto_referencia)
SELECT id_analito, 3, 4.5, 5.5, '4.5 - 5.5 mill/mm3'
FROM analitos WHERE id_analisis_tipo = 1 AND nombre_analito = 'Glóbulos Rojos';

-- --- HCM: tu plantilla dice 27-34 (yo había puesto 27-32) ---
UPDATE valores_referencia vr
  JOIN analitos a ON a.id_analito = vr.id_analito
SET vr.valor_min = 27, vr.valor_max = 34, vr.texto_referencia = '27 - 34 pg'
WHERE a.id_analisis_tipo = 1 AND a.nombre_analito = 'HCM (Hemoglobina Corpuscular Media)';

-- --- Eosinófilos: tu plantilla dice 2-4% (yo había puesto 1-4%) ---
UPDATE valores_referencia vr
  JOIN analitos a ON a.id_analito = vr.id_analito
SET vr.valor_min = 2, vr.valor_max = 4, vr.texto_referencia = '2 - 4 %'
WHERE a.id_analisis_tipo = 1 AND a.nombre_analito = 'Eosinófilos';

-- --- Monocitos: tu plantilla dice 4-8% (yo había puesto 2-8%) ---
UPDATE valores_referencia vr
  JOIN analitos a ON a.id_analito = vr.id_analito
SET vr.valor_min = 4, vr.valor_max = 8, vr.texto_referencia = '4 - 8 %'
WHERE a.id_analisis_tipo = 1 AND a.nombre_analito = 'Monocitos';

-- --- Neutrófilos: en tu plantilla son DOS parámetros, no uno ---
DELETE vr FROM valores_referencia vr
  JOIN analitos a ON a.id_analito = vr.id_analito
  WHERE a.id_analisis_tipo = 1 AND a.nombre_analito = 'Neutrófilos';
DELETE FROM analitos WHERE id_analisis_tipo = 1 AND nombre_analito = 'Neutrófilos';

INSERT INTO analitos (id_analisis_tipo, nombre_analito, unidad, orden_analito) VALUES
  (1, 'Neutrófilos en cayado', '%', 10),
  (1, 'Neutrófilos segmentados', '%', 11);

INSERT INTO valores_referencia (id_analito, id_sexo, valor_min, valor_max, texto_referencia)
SELECT id_analito, 3, 3, 5, '3 - 5 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Neutrófilos en cayado';
INSERT INTO valores_referencia (id_analito, id_sexo, valor_min, valor_max, texto_referencia)
SELECT id_analito, 3, 50, 70, '50 - 70 %' FROM analitos WHERE id_analisis_tipo=1 AND nombre_analito='Neutrófilos segmentados';

-- Se corren detrás de las de Neutrófilos en el orden de la lista.
UPDATE analitos SET orden_analito = 12 WHERE id_analisis_tipo=1 AND nombre_analito='Eosinófilos';
UPDATE analitos SET orden_analito = 13 WHERE id_analisis_tipo=1 AND nombre_analito='Basófilos';
UPDATE analitos SET orden_analito = 14 WHERE id_analisis_tipo=1 AND nombre_analito='Linfocitos';
UPDATE analitos SET orden_analito = 15 WHERE id_analisis_tipo=1 AND nombre_analito='Monocitos';

-- --- RDW-CV: parámetro de tu plantilla que todavía no estaba cargado.
-- Sin VR (tu plantilla lo deja en blanco) -- se agrega con tipo_dato
-- explícito (ver migracion_05) porque, sin VR numérico, si no se marca
-- iba a terminar mostrado como campo de texto en vez de numérico.
INSERT INTO analitos (id_analisis_tipo, nombre_analito, unidad, orden_analito) VALUES
  (1, 'RDW-CV', '%', 16);
