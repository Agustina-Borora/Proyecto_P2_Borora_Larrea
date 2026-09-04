-- Paso 3 (último): dar de baja las tablas que armé yo hoy sin saber que
-- ya existía tu sistema real. Corré esto recién DESPUÉS de:
--   1) confirmar con migracion_01 que resultados_analisis está vacía
--      (o rescatar a mano lo que tuviera antes de correr esto), y
--   2) correr migracion_02 para completar analitos/valores_referencia.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `resultados_analisis`;
DROP TABLE IF EXISTS `analisis_parametros`;
SET FOREIGN_KEY_CHECKS = 1;
