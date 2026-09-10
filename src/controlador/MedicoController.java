package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

/**
 * Controlador para la tabla de referencia `medicos`.
 */
public final class MedicoController {

    private MedicoController() {
    }

    /**
     * Nombres de los médicos activos, para el combo de "Médico Derivante" en Nuevo Análisis.
     */
    public static List<String> listarNombres(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los médicos",
                con -> dao.MedicoDAO.listarNombres(con),
                Collections.emptyList());
    }
}
