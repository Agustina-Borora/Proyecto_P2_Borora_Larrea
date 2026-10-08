package controlador;

import java.awt.Component;
import modelo.EstadoNumeracion;

/**
 * Controlador para la pestaña Configuración &gt; Sistema: estado de la numeración automática de
 * órdenes (las 3 preferencias de notificaciones no pasan por acá -- no son datos de la base, ver
 * {@link utilidades.PreferenciasSistema}, a la que la pantalla llama directo).
 */
public final class ConfiguracionController {

    private ConfiguracionController() {
    }

    public static EstadoNumeracion obtenerEstadoNumeracion(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al calcular la numeración de órdenes",
                con -> dao.PedidoDAO.obtenerEstadoNumeracion(con),
                null);
    }
}
