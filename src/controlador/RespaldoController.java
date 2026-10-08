package controlador;

import java.awt.Component;
import java.io.File;

/**
 * Controlador para el respaldo manual de datos (Configuración &gt; Sistema).
 */
public final class RespaldoController {

    private RespaldoController() {
    }

    public static int exportarTodo(Component padre, File carpeta) {
        return ConexionUtil.ejecutar(padre, "Error al exportar el respaldo",
                con -> dao.RespaldoDAO.exportarTodo(con, carpeta),
                0);
    }
}
