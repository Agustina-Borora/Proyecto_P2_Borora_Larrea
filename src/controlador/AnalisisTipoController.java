package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.AnalisisTipo;

/**
 * Controlador para la tabla `analisis_tipos`.
 */
public final class AnalisisTipoController {

    private AnalisisTipoController() {
    }

    public static List<AnalisisTipo> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los exámenes",
                con -> dao.AnalisisTipoDAO.listarTodos(con),
                Collections.emptyList());
    }
}
