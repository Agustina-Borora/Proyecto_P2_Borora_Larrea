package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.Sexo;

/**
 * Controlador para la tabla de referencia `sexos`.
 */
public final class SexoController {

    private SexoController() {
    }

    public static List<Sexo> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los sexos",
                con -> dao.SexoDAO.listarTodos(con),
                Collections.emptyList());
    }
}
