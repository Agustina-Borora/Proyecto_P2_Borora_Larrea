package controlador;

import java.awt.Component;
import java.math.BigDecimal;
import modelo.ValorUb;

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

    /**
     * Igual que {@link #obtenerVigente}, pero con la fila completa (para mostrar el valor
     * actual y desde cuándo rige en la pantalla de Catálogo de Exámenes).
     */
    public static ValorUb obtenerVigenteCompleto(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al obtener el valor de la UB",
                con -> dao.ValorUbDAO.obtenerVigenteCompleto(con),
                null);
    }

    /**
     * Carga un nuevo valor de UB, vigente desde hoy.
     */
    public static Integer crear(Component padre, BigDecimal valor) {
        return ConexionUtil.ejecutar(padre, "Error al cargar el nuevo valor de la UB",
                con -> dao.ValorUbDAO.crear(con, valor),
                null);
    }
}
