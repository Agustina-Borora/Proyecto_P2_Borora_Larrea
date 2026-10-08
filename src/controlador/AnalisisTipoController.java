package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.Analito;
import modelo.AnalisisTipo;

/**
 * Controlador para el ABM de `analisis_tipos` (Catálogo de Exámenes) y de sus parámetros
 * (`analitos`).
 */
public final class AnalisisTipoController {

    private AnalisisTipoController() {
    }

    public static List<AnalisisTipo> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los exámenes",
                con -> dao.AnalisisTipoDAO.listarTodos(con),
                Collections.emptyList());
    }

    public static List<AnalisisTipo> listarTodosParaAbm(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los exámenes",
                con -> dao.AnalisisTipoDAO.listarTodosParaAbm(con),
                Collections.emptyList());
    }

    public static AnalisisTipo buscarPorId(Component padre, int idAnalisisTipo) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el examen",
                con -> dao.AnalisisTipoDAO.buscarPorIdAbm(con, idAnalisisTipo),
                null);
    }

    public static Integer crear(Component padre, AnalisisTipo tipo) {
        return ConexionUtil.ejecutar(padre, "Error al crear el examen",
                con -> dao.AnalisisTipoDAO.crear(con, tipo),
                null);
    }

    public static boolean actualizar(Component padre, AnalisisTipo tipo) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los cambios",
                con -> dao.AnalisisTipoDAO.actualizar(con, tipo),
                false);
        return ok != null && ok;
    }

    public static boolean cambiarActivo(Component padre, int idAnalisisTipo, boolean activo) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al cambiar el estado del examen",
                con -> dao.AnalisisTipoDAO.cambiarActivo(con, idAnalisisTipo, activo),
                false);
        return ok != null && ok;
    }

    public static List<String> listarCategorias(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar las categorías",
                con -> dao.AnalisisTipoDAO.listarCategorias(con),
                Collections.emptyList());
    }

    public static List<Analito> listarAnalitos(Component padre, int idAnalisisTipo) {
        return ConexionUtil.ejecutar(padre, "Error al listar los parámetros",
                con -> dao.AnalitoDAO.listarParaAbm(con, idAnalisisTipo),
                Collections.emptyList());
    }

    public static Integer crearAnalito(Component padre, Analito analito) {
        return ConexionUtil.ejecutar(padre, "Error al crear el parámetro",
                con -> dao.AnalitoDAO.crear(con, analito),
                null);
    }

    public static boolean actualizarAnalito(Component padre, Analito analito) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar el parámetro",
                con -> dao.AnalitoDAO.actualizar(con, analito),
                false);
        return ok != null && ok;
    }

    public static boolean eliminarAnalito(Component padre, int idAnalito) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al eliminar el parámetro",
                con -> dao.AnalitoDAO.eliminar(con, idAnalito),
                false);
        return ok != null && ok;
    }
}
