package conexiones;

import java.sql.*;
import javax.swing.*;

/**
 * Clase encargada de la autenticación de usuarios y el control de accesos/permisos del sistema.
 */
public class Usuario {

    /**
     * Valida las credenciales de un usuario e inicia su sesión global si son correctas.
     *
     * @param conexion Objeto de conexión activo a la base de datos.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     * @return true si el usuario existe, sus credenciales coinciden y está activo; false de lo contrario.
     * @throws SQLException Si ocurre un error de consulta a la base de datos.
     */
    public static boolean ingresar(Connection conexion, String email, String password) throws SQLException {
        boolean existe = false;

        // OJO: ya no se filtra por password en el WHERE. La contraseña
        // ahora se guarda hasheada (ver controlador.PasswordHasher), así
        // que no hay forma de compararla dentro del SQL: hay que traer el
        // hash guardado y compararlo en Java con PasswordHasher.verificar().
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.apellido_usuario, u.password_usuario, r.nombre_rol " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.email_usuario = ? AND u.activo_usuario = 1";

        // Try-with-resources garantiza el cierre automático de PreparedStatement y ResultSet
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idUsuario = rs.getInt("id_usuario");
                    String valorGuardado = rs.getString("password_usuario");
                    boolean coincide;

                    if (controlador.PasswordHasher.esFormatoValido(valorGuardado)) {
                        coincide = controlador.PasswordHasher.verificar(password, valorGuardado);
                    } else {
                        // Migración: contraseña vieja, guardada en texto
                        // plano antes de que existiera PasswordHasher. Si
                        // coincide tal cual, la re-hasheamos en este mismo
                        // login para migrarla de forma transparente — el
                        // usuario no nota nada distinto, pero a partir de
                        // ahora esa contraseña ya queda hasheada.
                        coincide = valorGuardado != null && valorGuardado.equals(password);
                        if (coincide) {
                            actualizarPassword(conexion, idUsuario, controlador.PasswordHasher.hash(password));
                        }
                    }

                    if (coincide) {
                        // Poblar la clase Sesion con los datos del usuario autenticado
                        Sesion.idUsuario = idUsuario;
                        Sesion.nombre = rs.getString("nombre_usuario");
                        Sesion.apellido = rs.getString("apellido_usuario");
                        Sesion.rol = rs.getString("nombre_rol");

                        existe = true;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al ingresar: " + e.getMessage(), "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
        }

        return existe;
    }

    /**
     * Sobrescribe la contraseña (ya hasheada, ver {@link controlador.PasswordHasher#hash})
     * de un usuario. La usan tanto la migración automática de contraseñas
     * viejas en texto plano (ver {@link #ingresar}) como el flujo de
     * "cambiar contraseña" ({@link controlador.PasswordController}).
     *
     * @param conexion Objeto de conexión activo a la base de datos.
     * @param idUsuario Identificador del usuario a actualizar.
     * @param nuevoValorHasheado Resultado de {@link controlador.PasswordHasher#hash}, nunca texto plano.
     * @throws SQLException Si ocurre un error en la actualización.
     */
    public static void actualizarPassword(Connection conexion, int idUsuario, String nuevoValorHasheado) throws SQLException {
        String sql = "UPDATE usuarios SET password_usuario = ? WHERE id_usuario = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nuevoValorHasheado);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    /**
     * Busca el id de un usuario activo a partir de su email, para el flujo
     * de "olvidé mi contraseña" (hace falta saber a qué usuario generarle
     * el código de verificación, sin que haya iniciado sesión todavía).
     *
     * @param conexion Objeto de conexión activo a la base de datos.
     * @param email Email a buscar.
     * @return el id de usuario, o -1 si no hay ningún usuario activo con ese email.
     * @throws SQLException Si ocurre un error en la consulta SQL.
     */
    public static int buscarIdPorEmail(Connection conexion, String email) throws SQLException {
        String sql = "SELECT id_usuario FROM usuarios WHERE email_usuario = ? AND activo_usuario = 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_usuario") : -1;
            }
        }
    }

    /**
     * Comprueba si el usuario tiene permiso para acceder a una pantalla específica.
     * Los administradores tienen acceso automático total a cualquier pantalla.
     *
     * @param conexion Objeto de conexión activo a la base de datos.
     * @param idUsuario Identificador único del usuario a consultar.
     * @param nombrePantalla Identificador de la vista/pantalla que intenta abrir.
     * @return true si tiene permiso de acceso; false en caso contrario.
     * @throws SQLException Si ocurre un error en la consulta SQL.
     */
    public static boolean tienePermiso(Connection conexion, int idUsuario, String nombrePantalla) throws SQLException {
        boolean permitido = false;

        // Bypass de seguridad: Si el rol es Admin o Administrador, otorga acceso directo
        if ("Admin".equalsIgnoreCase(Sesion.rol) || "Administrador".equalsIgnoreCase(Sesion.rol)) {
            return true;
        }

        String sql = "SELECT permitido FROM permisos_pantalla WHERE id_usuario = ? AND pantalla = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, nombrePantalla);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    permitido = (rs.getInt("permitido") == 1);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al consultar permiso: " + e.getMessage(), "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
        }

        return permitido;
    }
}