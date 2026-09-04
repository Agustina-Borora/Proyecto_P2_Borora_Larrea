package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.Sexo;

/**
 * Controlador para la tabla de referencia `sexos`. Antes, tanto
 * nuevoAnalisis.DatosPersonales como registrarResultados.EncabezadoDatosPaciente
 * tenían cada una su propio método cargarSexos() con el mismo código
 * (conectar, llamar a dao.SexoDAO.listarTodos(), cerrar); ahora las dos
 * llaman a este único método.
 */
public final class SexoController {

    private SexoController() {
    }

    public static List<Sexo> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los sexos",
                con -> dao.SexoDAO.listarTodos(con),
                Collections.emptyList());
    }
}
