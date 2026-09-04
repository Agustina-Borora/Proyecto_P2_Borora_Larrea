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

        // Consulta SQL parametrizada con INNER JOIN para recuperar el nombre del rol
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.apellido_usuario, r.nombre_rol " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.email_usuario = ? AND u.password_usuario = ? AND u.activo_usuario = 1";

        // Try-with-resources garantiza el cierre automático de PreparedStatement y ResultSet
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Poblar la clase Sesion con los datos del usuario autenticado
                    Sesion.idUsuario = rs.getInt("id_usuario");
                    Sesion.nombre = rs.getString("nombre_usuario");
                    Sesion.apellido = rs.getString("apellido_usuario");
                    Sesion.rol = rs.getString("nombre_rol");
                    
                    existe = true;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al ingresar: " + e.getMessage(), "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
        }

        return existe;
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