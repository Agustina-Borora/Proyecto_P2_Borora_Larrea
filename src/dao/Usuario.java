package dao;

import modelo.Sesion;
import java.sql.*;
import javax.swing.*;

/**
 * Clase encargada de la autenticación de usuarios y el control de accesos/permisos del
 * sistema.
 */
public class Usuario {

    /**
     * Valida las credenciales de un usuario e inicia su sesión global si son correctas.
     *
     * @param conexion Objeto de conexión activo a la base de datos.
     * @param identificador Email o nombre de pila del usuario (ver más abajo).
     * @param password Contraseña del usuario.
     * @return true si el usuario existe, sus credenciales coinciden y está activo; false de lo contrario.
     * @throws SQLException Si ocurre un error de consulta a la base de datos.
     */
    public static boolean ingresar(Connection conexion, String identificador, String password) throws SQLException {
        boolean existe = false;

        // OJO: ya no se filtra por password en el WHERE. La contraseña
        // ahora se guarda hasheada (ver controlador.PasswordHasher), así
        // que no hay forma de compararla dentro del SQL: hay que traer el
        // hash guardado y compararlo en Java con PasswordHasher.verificar().
        //
        // Mes 8: ahora "identificador" puede ser el email de siempre O el nombre de pila (pedido
        // de la clienta -- "cansa que a cada rato tenga que ingresar el gmail"), sin distinguir
        // mayúsculas/minúsculas. El email sigue siendo único por usuario, pero el nombre de pila
        // no (puede haber dos "Zaira" activas) -- por eso, si el nombre ingresado coincide con más
        // de un usuario activo, NO se entra con ninguno de los dos (ver el chequeo de ambigüedad
        // más abajo): equivocarse de cuenta por un nombre repetido sería un problema de seguridad,
        // no sólo de comodidad. En ese caso puntual, esa persona sigue necesitando el email.
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.apellido_usuario, u.password_usuario, r.nombre_rol " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE (u.email_usuario = ? OR LOWER(u.nombre_usuario) = LOWER(?)) AND u.activo_usuario = 1";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, identificador);
            ps.setString(2, identificador);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Auditoría: no se guarda la contraseña tipeada, sólo el identificador, para
                    // poder ver después intentos repetidos de entrar con un usuario que no existe.
                    AuditoriaDAO.registrar(conexion, null, "LOGIN_FALLIDO", "usuarios", null,
                            "Intento de inicio de sesión fallido con \"" + identificador
                                    + "\" (no existe o está inactivo)");
                    return false;
                }

                int idUsuario = rs.getInt("id_usuario");
                String valorGuardado = rs.getString("password_usuario");
                String nombreUsuario = rs.getString("nombre_usuario");
                String apellidoUsuario = rs.getString("apellido_usuario");
                String nombreRol = rs.getString("nombre_rol");

                if (rs.next()) {
                    // Ambiguo: más de un usuario activo coincide con el nombre ingresado.
                    AuditoriaDAO.registrar(conexion, null, "LOGIN_FALLIDO", "usuarios", null,
                            "Intento de inicio de sesión fallido con \"" + identificador
                                    + "\" (nombre ambiguo, coincide con más de un usuario)");
                    return false;
                }

                boolean coincide;
                if (controlador.PasswordHasher.esFormatoValido(valorGuardado)) {
                    coincide = controlador.PasswordHasher.verificar(password, valorGuardado);
                } else {
                    coincide = valorGuardado != null && valorGuardado.equals(password);
                    if (coincide) {
                        actualizarPassword(conexion, idUsuario, controlador.PasswordHasher.hash(password));
                    }
                }

                if (coincide) {
                    Sesion.idUsuario = idUsuario;
                    Sesion.nombre = nombreUsuario;
                    Sesion.apellido = apellidoUsuario;
                    Sesion.rol = nombreRol;

                    existe = true;
                    AuditoriaDAO.registrar(conexion, idUsuario, "LOGIN", "usuarios", idUsuario,
                            "Inicio de sesión: " + nombreUsuario + " " + apellidoUsuario);
                } else {
                    AuditoriaDAO.registrar(conexion, null, "LOGIN_FALLIDO", "usuarios", idUsuario,
                            "Intento de inicio de sesión fallido con \"" + identificador
                                    + "\" (contraseña incorrecta)");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al ingresar: " + e.getMessage(), "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
        }

        return existe;
    }

    /**
     * Sobrescribe la contraseña (ya hasheada, ver {@link controlador.PasswordHasher#hash}) de un
     * usuario.
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
     * Busca el id de un usuario activo a partir de su email, para el flujo de "olvidé mi
     * contraseña" (hace falta saber a qué usuario generarle el código de verificación, sin que
     * haya iniciado sesión todavía).
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