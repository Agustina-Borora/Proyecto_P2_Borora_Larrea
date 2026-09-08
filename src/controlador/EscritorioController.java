package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.DetalleOrden;
import modelo.EstadisticasEscritorio;
import modelo.OrdenResumen;

/**
 * Controlador para la pantalla Escritorio: las 4 tarjetas resumen
 * (vistas.formulariosPrincipales.Escritorio) y la tabla de últimas órdenes
 * (vistas.escritorio.TablaEscritorio).
 */
public final class EscritorioController {

    private EscritorioController() {
    }

    public static EstadisticasEscritorio obtenerEstadisticasDelMes(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al calcular las estadísticas del escritorio",
                con -> dao.EscritorioDAO.obtenerEstadisticasDelMes(con),
                new EstadisticasEscritorio());
    }

    public static List<OrdenResumen> listarUltimasOrdenes(Component padre, int limite) {
        return ConexionUtil.ejecutar(padre, "Error al listar las últimas órdenes",
                con -> dao.EscritorioDAO.listarUltimasOrdenes(con, limite),
                Collections.emptyList());
    }

    /**
     * Trae el detalle completo de una orden puntual para la pantalla "Detalle de Orden" del
     * Escritorio. Devuelve null si no se encontró o si hubo un error de conexión.
     */
    public static DetalleOrden buscarDetalleOrden(Component padre, int idPedidoAnalisis) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el detalle de la orden",
                con -> dao.EscritorioDAO.buscarDetalleOrden(con, idPedidoAnalisis),
                null);
    }
}
