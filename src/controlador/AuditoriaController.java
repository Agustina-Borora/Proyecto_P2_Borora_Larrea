package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.RegistroAuditoria;

/**
 * Controlador para la pantalla Auditoría. El guardado de cada movimiento (ver
 * {@code dao.AuditoriaDAO#registrar}) no pasa por acá -- lo llama directo cada DAO que lo
 * necesita, reutilizando la misma Connection que ya tiene abierta para su propia operación, igual
 * que cualquier otro INSERT que no necesite mostrarle nada a una pantalla.
 */
public final class AuditoriaController {

    private AuditoriaController() {
    }

    public static List<RegistroAuditoria> listarRecientes(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar la auditoría",
                con -> dao.AuditoriaDAO.listarRecientes(con),
                Collections.emptyList());
    }
}
