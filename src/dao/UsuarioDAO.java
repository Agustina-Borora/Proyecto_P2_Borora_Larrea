package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Da de alta un usuario nuevo, con la contraseña ya hasheada (ver
     * {@link controlador.PasswordHasher#hash}, nunca texto plano acá). Antes no existía ningún
     * método para crear usuarios -- "+ Crear Usuario" en la pantalla de Swing armaba el diálogo
     * pero nadie le cableaba una acción al botón, así que no guardaba nada (encontrado al portar
     * la pantalla a JavaFX). Devuelve el id generado, o null si falló.
     */
    public static Integer crear(Connection conexion, Usuario usuario, int idRol, String passwordHasheada) {
        String sql = "INSERT INTO usuarios (nombre_usuario, apellido_usuario, email_usuario, "
                + "password_usuario, id_rol, activo_usuario) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, passwordHasheada);
            ps.setInt(5, idRol);
            ps.setInt(6, usuario.isActivo() ? 1 : 0);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    return null;
                }
                int idGenerado = rs.getInt(1);
                AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "ALTA", "usuarios", idGenerado,
                        "Usuario creado: " + usuario.getNombre() + " " + usuario.getApellido()
                                + " (" + usuario.getEmail() + ")");
                return idGenerado;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear el usuario", e);
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
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "EDICION", "usuarios",
                        usuario.getIdUsuario(),
                        "Datos actualizados de " + usuario.getNombre() + " " + usuario.getApellido());
            }
            return ok;
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
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "BAJA", "usuarios", idUsuario,
                        "Usuario dado de baja (id " + idUsuario + ")");
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al dar de baja el usuario", e);
            return false;
        }
    }

    /**
     * Reemplaza todos los permisos de acceso a pantalla de un usuario por los que vienen en
     * {@code permisos} (pantalla -&gt; permitido). Antes no existía este método -- la tabla
     * {@code permisos_pantalla} ya la consultaba {@link dao.Usuario#tienePermiso}, pero nadie le
     * escribía nunca una fila, así que los 11 toggles de "Acceso a Pantalla" del formulario se
     * tildaban sin que quedara guardado. Borra todo lo que tenía el usuario y vuelve a insertar el
     * estado completo, en vez de un UPDATE/INSERT fila por fila, porque el formulario siempre manda
     * el estado de los 11 toggles juntos.
     */
    public static boolean guardarPermisosPantalla(Connection conexion, int idUsuario, Map<String, Boolean> permisos) {
        String sqlBorrar = "DELETE FROM permisos_pantalla WHERE id_usuario = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sqlBorrar)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            Mensajes.error("Error al guardar los permisos de acceso", e);
            return false;
        }

        String sqlInsertar = "INSERT INTO permisos_pantalla (id_usuario, pantalla, permitido) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sqlInsertar)) {
            for (Map.Entry<String, Boolean> permiso : permisos.entrySet()) {
                ps.setInt(1, idUsuario);
                ps.setString(2, permiso.getKey());
                ps.setInt(3, Boolean.TRUE.equals(permiso.getValue()) ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            Mensajes.error("Error al guardar los permisos de acceso", e);
            return false;
        }
    }

    /**
     * Devuelve los nombres de pantalla que un usuario tiene habilitados según
     * {@code permisos_pantalla} (sólo las marcadas como {@code permitido = 1}; una pantalla que no
     * aparece en el resultado se interpreta como no permitida -- mismo criterio que ya usa
     * {@link dao.Usuario#tienePermiso}). Se usa para precargar los 11 toggles al abrir "Ver
     * Usuario" / "Editar Usuario".
     */
    public static Set<String> listarPantallasPermitidas(Connection conexion, int idUsuario) {
        Set<String> permitidas = new HashSet<>();
        String sql = "SELECT pantalla FROM permisos_pantalla WHERE id_usuario = ? AND permitido = 1";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permitidas.add(rs.getString("pantalla"));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al consultar los permisos de acceso", e);
        }

        return permitidas;
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
