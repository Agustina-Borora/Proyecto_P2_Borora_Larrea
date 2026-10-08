package controlador;

import java.awt.Component;
import modelo.EstadisticasPeriodo;

/**
 * Controlador para la pantalla Estadísticas. El parámetro "periodo" acepta "mes", "trimestre" o
 * "anio" -- ver {@link dao.EstadisticasDAO#obtener}.
 */
public final class EstadisticasController {

    private EstadisticasController() {
    }

    public static EstadisticasPeriodo obtener(Component padre, String periodo) {
        return ConexionUtil.ejecutar(padre, "Error al cargar las estadísticas",
                con -> dao.EstadisticasDAO.obtener(con, periodo),
                new EstadisticasPeriodo());
    }
}
