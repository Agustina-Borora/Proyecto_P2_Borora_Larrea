-- Paso 1 de la migración: antes de borrar las tablas que armé yo hoy
-- (analisis_parametros / resultados_analisis), confirmamos que no haya
-- resultados reales cargados ahí que se perderían.
SELECT COUNT(*) AS resultados_a_perder FROM resultados_analisis;
