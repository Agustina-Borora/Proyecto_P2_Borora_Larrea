package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.Paciente;

/**
 * Controlador para todo lo que las pantallas necesitan de un paciente sin
 * saber que existe una base de datos: buscarlo por DNI (DatosPersonales,
 * RegistrarResultados), listarlos todos y eliminar uno (TablaPacientes).
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
}
