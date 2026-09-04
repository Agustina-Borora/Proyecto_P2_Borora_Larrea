package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import modelo.Parametro;

/**
 * Controlador para registrarResultados.cargarReultados: traer los
 * parámetros de un examen (con lo ya guardado, si lo había) y guardar los
 * valores cargados.
 */
public final class ResultadosController {

    private ResultadosController() {
    }

    /** Parámetros de un examen junto con los valores ya guardados (si los había), por id_analito. */
    public static final class DatosExamen {
        private final List<Parametro> parametros;
        private final Map<Integer, String> valoresGuardados;

        public DatosExamen(List<Parametro> parametros, Map<Integer, String> valoresGuardados) {
            this.parametros = parametros;
            this.valoresGuardados = valoresGuardados;
        }

        public List<Parametro> getParametros() {
            return parametros;
        }

        public Map<Integer, String> getValoresGuardados() {
            return valoresGuardados;
        }
    }

    public static DatosExamen cargarExamen(Component padre, int idAnalisisTipo, int idPedidoAnalisis, int idSexoPaciente) {
        return ConexionUtil.ejecutar(padre, "Error al traer los parámetros del examen", con -> {
            List<Parametro> parametros = dao.AnalitoDAO.listarConReferencia(con, idAnalisisTipo, idSexoPaciente);
            Map<Integer, String> valores = dao.PedidoAnalitoResultadoDAO.listarResultados(con, idPedidoAnalisis);
            return new DatosExamen(parametros, valores);
        }, new DatosExamen(Collections.emptyList(), Collections.emptyMap()));
    }

    /**
     * Guarda todos los valores cargados y actualiza el estado del examen
     * (completado si no falta ninguno, en_proceso si falta alguno).
     */
    public static boolean guardarResultados(Component padre, int idPedidoAnalisis,
            Map<Integer, String> valores, boolean faltaAlguno) {
        return ConexionUtil.ejecutar(padre, "Error al guardar los resultados", con -> {
            boolean ok = dao.PedidoAnalitoResultadoDAO.guardarTodos(con, idPedidoAnalisis, valores);
            if (ok) {
                dao.PedidoAnalitoResultadoDAO.actualizarEstado(con, idPedidoAnalisis,
                        faltaAlguno ? "en_proceso" : "completado");
            }
            return ok;
        }, false);
    }
}
