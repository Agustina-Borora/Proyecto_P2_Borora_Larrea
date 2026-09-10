package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.Usuario;

/**
 * Controlador para el listado y mantenimiento básico de usuarios (pantalla "Cuentas y
 * Permisos"). El cambio de contraseña se maneja aparte, en {@link PasswordController}.
 */
public final class UsuarioController {

    private UsuarioController() {
    }

    public static List<Usuario> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los usuarios",
                con -> dao.UsuarioDAO.listarTodos(con),
                Collections.emptyList());
    }

    public static Usuario buscarPorId(Component padre, int idUsuario) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el usuario",
                con -> dao.UsuarioDAO.buscarPorId(con, idUsuario),
                null);
    }

    /**
     * Guarda los cambios de un usuario existente. Resuelve el id de rol a partir del nombre
     * ("Administrador" / "Tecnico") ya que el formulario sólo conoce el nombre.
     */
    public static boolean actualizar(Component padre, Usuario usuario) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los cambios", con -> {
            Integer idRol = dao.RolDAO.buscarIdPorNombre(con, usuario.getRol());
            if (idRol == null) {
                return false;
            }
            return dao.UsuarioDAO.actualizar(con, usuario, idRol);
        }, false);
        return ok != null && ok;
    }

    public static boolean desactivar(Component padre, int idUsuario) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al dar de baja el usuario",
                con -> dao.UsuarioDAO.desactivar(con, idUsuario),
                false);
        return ok != null && ok;
    }
}
