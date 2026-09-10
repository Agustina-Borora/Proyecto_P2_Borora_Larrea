package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

/**
 * Controlador para la tabla de referencia `obras_sociales`.
 */
public final class ObraSocialController {

    private ObraSocialController() {
    }

    public static List<String> listarNombres(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar las obras sociales",
                con -> dao.ObraSocialDAO.listarNombres(con),
                Collections.emptyList());
    }
}
