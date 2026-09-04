package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.EstadisticasEscritorio;
import modelo.OrdenResumen;

/**
 * Controlador para la pantalla Escritorio: las 4 tarjetas resumen
 * (formulariosPrincipales.Escritorio) y la tabla de últimas órdenes
 * (escritorio.TablaEscritorio).
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
}
