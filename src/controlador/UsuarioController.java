package controlador;

import java.awt.Component;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import modelo.Usuario;

/**
 * Controlador para el listado y mantenimiento básico de usuarios (pantalla "Cuentas y
 * Permisos"). El cambio de contraseña de un usuario ya existente se maneja aparte, en
 * {@link PasswordController}.
 */
public final class UsuarioController {

    /** Mismo mínimo que exige {@link PasswordController} al cambiar una contraseña existente. */
    private static final int LARGO_MINIMO_PASSWORD = 8;

    private UsuarioController() {
    }

    public static List<Usuario> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los usuarios",
                con -> dao.UsuarioDAO.listarTodos(con),
                Collections.emptyList());
    }

    /**
     * Da de alta un usuario nuevo con su contraseña inicial (se hashea acá, nunca se guarda en
     * texto plano). Valida el largo mínimo de la contraseña y que el email no esté ya en uso por
     * otro usuario activo, todo dentro de la misma transacción que resuelve el id de rol e
     * inserta -- si cualquier paso falla, no se crea nada. Devuelve el id del usuario nuevo, o
     * null si no se pudo crear (ya sea por una validación o por un error de base de datos, que
     * {@link ConexionUtil} ya mostró por su cuenta).
     */
    public static Integer crear(Component padre, Usuario usuario, String passwordPlano) {
        if (passwordPlano == null || passwordPlano.length() < LARGO_MINIMO_PASSWORD) {
            JOptionPane.showMessageDialog(padre,
                    "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres.",
                    "Contraseña muy corta", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return ConexionUtil.ejecutarTransaccion(padre, "Error al crear el usuario", con -> {
            Integer idRol = dao.RolDAO.buscarIdPorNombre(con, usuario.getRol());
            if (idRol == null) {
                throw new OperacionCancelada();
            }
            int idExistente;
            try {
                idExistente = dao.Usuario.buscarIdPorEmail(con, usuario.getEmail());
            } catch (SQLException e) {
                throw e;
            }
            if (idExistente != -1) {
                JOptionPane.showMessageDialog(padre, "Ya hay un usuario activo con ese email.",
                        "Email repetido", JOptionPane.WARNING_MESSAGE);
                throw new OperacionCancelada();
            }
            return dao.UsuarioDAO.crear(con, usuario, idRol, PasswordHasher.hash(passwordPlano));
        }, null);
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

    /**
     * Guarda los 11 toggles de "Acceso a Pantalla" de un usuario. Se llama por separado, después
     * de {@link #crear} o {@link #actualizar}, con el mismo criterio que ya usa
     * {@link PasswordController} para el cambio de contraseña (una acción aparte, no mezclada con
     * el guardado de los datos básicos).
     */
    public static boolean guardarPermisosPantalla(Component padre, int idUsuario, Map<String, Boolean> permisos) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los permisos de acceso",
                con -> dao.UsuarioDAO.guardarPermisosPantalla(con, idUsuario, permisos),
                false);
        return ok != null && ok;
    }

    /**
     * Pantallas que un usuario tiene habilitadas, para precargar los toggles al abrir "Ver
     * Usuario" / "Editar Usuario".
     */
    public static Set<String> obtenerPantallasPermitidas(Component padre, int idUsuario) {
        return ConexionUtil.ejecutar(padre, "Error al consultar los permisos de acceso",
                con -> dao.UsuarioDAO.listarPantallasPermitidas(con, idUsuario),
                Collections.emptySet());
    }

    /**
     * Guarda el nivel de acceso (Sin acceso / Solo ver / Cargar) que un usuario tiene para cada
     * examen del catálogo. Misma idea que {@link #guardarPermisosPantalla}: se llama por separado,
     * después de {@link #crear} o {@link #actualizar}.
     */
    public static boolean guardarPermisosExamen(Component padre, int idUsuario, Map<Integer, String> niveles) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los permisos por examen",
                con -> dao.PermisoExamenDAO.guardarPermisosExamen(con, idUsuario, niveles),
                false);
        return ok != null && ok;
    }

    /**
     * Niveles por examen que un usuario tiene guardados, para precargar la grilla al abrir "Ver
     * Usuario" / "Editar Usuario".
     */
    public static Map<Integer, String> obtenerNivelesExamen(Component padre, int idUsuario) {
        return ConexionUtil.ejecutar(padre, "Error al consultar los permisos por examen",
                con -> dao.PermisoExamenDAO.listarNivelesExamen(con, idUsuario),
                Collections.emptyMap());
    }
}
