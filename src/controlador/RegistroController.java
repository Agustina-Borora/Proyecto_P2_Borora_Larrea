package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.OrdenResumen;

/**
 * Controlador para la pantalla Registros (registros.TablaRegistros), tanto
 * en su modo normal (todas las órdenes) como en el modo que usa Registrar
 * Resultados (solo las pendientes).
 */
public final class RegistroController {

    private RegistroController() {
    }

    public static List<OrdenResumen> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los registros",
                con -> dao.RegistroDAO.listarTodos(con),
                Collections.emptyList());
    }

    public static List<OrdenResumen> listarPendientes(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los registros",
                con -> dao.RegistroDAO.listarPendientes(con),
                Collections.emptyList());
    }
}
