package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.Usuario;

/**
 * DAO para el listado y el mantenimiento básico de los usuarios del sistema (pantalla "Cuentas y
 * Permisos"). La autenticación y el cambio de contraseña siguen viviendo en {@link dao.Usuario}
 * -- esta clase es sólo para listar, ver, editar y dar de baja cuentas.
 */
public class UsuarioDAO {

    private static final String SELECT_BASE
            = "SELECT u.id_usuario, u.nombre_usuario, u.apellido_usuario, u.email_usuario, "
            + "u.activo_usuario, r.nombre_rol "
            + "FROM usuarios u INNER JOIN roles r ON u.id_rol = r.id_rol ";

    public static List<Usuario> listarTodos(Connection conexion) {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY u.apellido_usuario, u.nombre_usuario";

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapear(rs));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar los usuarios", e);
        }

        return usuarios;
    }

    public static Usuario buscarPorId(Connection conexion, int idUsuario) {
        String sql = SELECT_BASE + "WHERE u.id_usuario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el usuario", e);
            return null;
        }
    }

    /**
     * Actualiza los datos básicos de un usuario (nombre, apellido, email, rol y estado). No toca
     * la contraseña -- para eso está {@link dao.Usuario#actualizarPassword}.
     */
    public static boolean actualizar(Connection conexion, Usuario usuario, int idRol) {
        String sql = "UPDATE usuarios SET nombre_usuario = ?, apellido_usuario = ?, "
                + "email_usuario = ?, id_rol = ?, activo_usuario = ? WHERE id_usuario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getEmail());
            ps.setInt(4, idRol);
            ps.setInt(5, usuario.isActivo() ? 1 : 0);
            ps.setInt(6, usuario.getIdUsuario());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el usuario", e);
            return false;
        }
    }

    /**
     * Baja lógica (no se borra el registro), mismo criterio que el resto del sistema (ej.
     * analisis_tipos.activo_analisis).
     */
    public static boolean desactivar(Connection conexion, int idUsuario) {
        String sql = "UPDATE usuarios SET activo_usuario = 0 WHERE id_usuario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al dar de baja el usuario", e);
            return false;
        }
    }

    private static Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("apellido_usuario"),
                rs.getString("email_usuario"),
                rs.getString("nombre_rol"),
                rs.getInt("activo_usuario") == 1);
    }
}
