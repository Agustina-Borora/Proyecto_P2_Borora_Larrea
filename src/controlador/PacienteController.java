package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import modelo.AnalitoReferencia;
import modelo.OrdenResumen;
import modelo.Paciente;

/**
 * Controlador para todo lo que las pantallas necesitan de un paciente sin saber que existe una
 * base de datos: buscarlo por DNI (DatosPersonales, RegistrarResultados), listarlos todos y
 * eliminar uno (TablaPacientes).
 */
public final class PacienteController {

    private PacienteController() {
    }

    public static Paciente buscarPorDni(Component padre, String dni) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el paciente",
                con -> dao.PacienteDAO.buscarPorDni(con, dni),
                null);
    }

    public static List<Paciente> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar pacientes",
                con -> dao.PacienteDAO.listarTodos(con),
                Collections.emptyList());
    }

    public static boolean eliminar(Component padre, int idPaciente) {
        return ConexionUtil.ejecutar(padre, "Error al eliminar el paciente",
                con -> dao.PacienteDAO.eliminar(con, idPaciente),
                false);
    }

    /**
     * El examen más reciente del paciente (o null si nunca tuvo ninguno), para la pestaña
     * "Último Examen" del detalle de paciente.
     */
    public static OrdenResumen buscarUltimoExamen(Component padre, int idPaciente) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el último examen del paciente", con -> {
            List<OrdenResumen> ordenes = dao.RegistroDAO.listarPorPaciente(con, idPaciente);
            return ordenes.isEmpty() ? null : ordenes.get(0);
        }, null);
    }

    /**
     * Todas las órdenes del paciente (de la más reciente a la más vieja), para la pestaña
     * "Historial" del detalle de paciente.
     */
    public static List<OrdenResumen> listarHistorial(Component padre, int idPaciente) {
        return ConexionUtil.ejecutar(padre, "Error al listar el historial del paciente",
                con -> dao.RegistroDAO.listarPorPaciente(con, idPaciente),
                Collections.emptyList());
    }

    /**
     * Analitos (con sus valores de referencia por sexo) y resultados ya cargados de un examen
     * puntual, para la tabla de la pestaña "Último Examen" del detalle de paciente.
     */
    public static final class DetalleExamenPaciente {
        private final List<AnalitoReferencia> analitos;
        private final Map<Integer, String> resultados;

        public DetalleExamenPaciente(List<AnalitoReferencia> analitos, Map<Integer, String> resultados) {
            this.analitos = analitos;
            this.resultados = resultados;
        }

        public List<AnalitoReferencia> getAnalitos() {
            return analitos;
        }

        public Map<Integer, String> getResultados() {
            return resultados;
        }
    }

    public static DetalleExamenPaciente cargarDetalleExamen(Component padre, int idAnalisisTipo, int idPedidoAnalisis) {
        return ConexionUtil.ejecutar(padre, "Error al traer el detalle del examen", con -> {
            List<AnalitoReferencia> analitos = dao.AnalitoDAO.listarConReferenciaPorSexo(con, idAnalisisTipo);
            Map<Integer, String> resultados = dao.PedidoAnalitoResultadoDAO.listarResultados(con, idPedidoAnalisis);
            return new DetalleExamenPaciente(analitos, resultados);
        }, new DetalleExamenPaciente(Collections.emptyList(), Collections.emptyMap()));
    }
}
