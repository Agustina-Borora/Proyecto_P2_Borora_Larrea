package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO para los permisos por examen de cada usuario (pestaña "Permisos y Acceso" -&gt; "Permisos
 * por Examen", en Nuevo/Editar Usuario). Antes no existía tabla ni DAO para esto -- la grilla que
 * tenía la versión Swing ({@code vista.usuarios.PermisosExamen}) se podía tildar pero no guardaba
 * nada en ningún lado (ver el javadoc de {@code vistas.javafx.usuarios.UsuarioFormDialog}).
 *
 * <p>Sólo se guarda una fila cuando el nivel es "Solo ver" o "Cargar" -- un examen ausente en
 * {@code permisos_examen} para un usuario se interpreta como "Sin acceso", mismo criterio que
 * {@code permisos_pantalla} / {@link dao.Usuario#tienePermiso}. Hace falta correr la migración
 * {@code sql/2026-09-29_permisos_examen.sql} para que exista la tabla.</p>
 */
public class PermisoExamenDAO {

    /**
     * Reemplaza todos los permisos por examen de un usuario por los que vienen en
     * {@code nivelesPorExamen} (id_analisis_tipo -&gt; nivel). Borra todo lo que tenía el usuario
     * y vuelve a insertar sólo lo que no sea "Sin acceso", mismo criterio de "borrar e insertar de
     * nuevo" que {@link UsuarioDAO#guardarPermisosPantalla}.
     */
    public static boolean guardarPermisosExamen(Connection conexion, int idUsuario, Map<Integer, String> nivelesPorExamen) {
        String sqlBorrar = "DELETE FROM permisos_examen WHERE id_usuario = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sqlBorrar)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            Mensajes.error("Error al guardar los permisos por examen", e);
            return false;
        }

        String sqlInsertar = "INSERT INTO permisos_examen (id_usuario, id_analisis_tipo, nivel_acceso) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sqlInsertar)) {
            boolean hayAlguno = false;
            for (Map.Entry<Integer, String> entrada : nivelesPorExamen.entrySet()) {
                if ("Sin acceso".equals(entrada.getValue())) {
                    continue;
                }
                ps.setInt(1, idUsuario);
                ps.setInt(2, entrada.getKey());
                ps.setString(3, entrada.getValue());
                ps.addBatch();
                hayAlguno = true;
            }
            if (hayAlguno) {
                ps.executeBatch();
            }
            return true;
        } catch (SQLException e) {
            Mensajes.error("Error al guardar los permisos por examen", e);
            return false;
        }
    }

    /**
     * Niveles guardados para un usuario (sólo trae "Solo ver"/"Cargar" -- un examen que no
     * aparece en el resultado se interpreta como "Sin acceso"). Se usa para precargar la grilla al
     * abrir "Ver Usuario" / "Editar Usuario".
     */
    public static Map<Integer, String> listarNivelesExamen(Connection conexion, int idUsuario) {
        Map<Integer, String> niveles = new HashMap<>();
        String sql = "SELECT id_analisis_tipo, nivel_acceso FROM permisos_examen WHERE id_usuario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    niveles.put(rs.getInt("id_analisis_tipo"), rs.getString("nivel_acceso"));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al consultar los permisos por examen", e);
        }

        return niveles;
    }
}
