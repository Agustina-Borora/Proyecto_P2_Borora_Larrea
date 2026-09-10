package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

/**
 * Controlador para la tabla de referencia `metodos_pago`.
 */
public final class MetodoPagoController {

    private MetodoPagoController() {
    }

    public static List<String> listarNombres(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los métodos de pago",
                con -> dao.MetodoPagoDAO.listarNombres(con),
                Collections.emptyList());
    }
}
