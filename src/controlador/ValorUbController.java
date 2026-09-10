package controlador;

import java.awt.Component;
import java.math.BigDecimal;

/**
 * Controlador para la tabla de referencia `valor_ub`.
 */
public final class ValorUbController {

    private ValorUbController() {
    }

    /**
     * Valor vigente hoy de la Unidad Bioquímica, o null si no hay ninguno cargado.
     */
    public static BigDecimal obtenerVigente(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al obtener el valor de la UB",
                con -> dao.ValorUbDAO.obtenerVigente(con),
                null);
    }
}
