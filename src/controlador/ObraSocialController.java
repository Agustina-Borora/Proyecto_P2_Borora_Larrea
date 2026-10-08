package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.ObraSocial;

/**
 * Controlador para el ABM de la tabla `obras_sociales`.
 */
public final class ObraSocialController {

    private ObraSocialController() {
    }

    public static List<String> listarNombres(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar las obras sociales",
                con -> dao.ObraSocialDAO.listarNombres(con),
                Collections.emptyList());
    }

    public static List<ObraSocial> listarTodas(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar las obras sociales",
                con -> dao.ObraSocialDAO.listarTodas(con),
                Collections.emptyList());
    }

    public static ObraSocial buscarPorId(Component padre, int idObraSocial) {
        return ConexionUtil.ejecutar(padre, "Error al buscar la obra social",
                con -> dao.ObraSocialDAO.buscarPorId(con, idObraSocial),
                null);
    }

    public static Integer crear(Component padre, ObraSocial obraSocial) {
        return ConexionUtil.ejecutar(padre, "Error al crear la obra social",
                con -> dao.ObraSocialDAO.crear(con, obraSocial),
                null);
    }

    public static boolean actualizar(Component padre, ObraSocial obraSocial) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los cambios",
                con -> dao.ObraSocialDAO.actualizar(con, obraSocial),
                false);
        return ok != null && ok;
    }

    public static boolean desactivar(Component padre, int idObraSocial) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al dar de baja la obra social",
                con -> dao.ObraSocialDAO.desactivar(con, idObraSocial),
                false);
        return ok != null && ok;
    }
}
