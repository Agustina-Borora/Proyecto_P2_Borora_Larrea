package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO para la tabla de referencia `roles`. Los roles que ofrece el formulario ("Administrador" /
 * "Tecnico") están fijos en la interfaz, así que por ahora sólo hace falta resolver su id para
 * guardar cambios -- no un listado completo como en {@link SexoDAO}.
 */
public class RolDAO {

    public static Integer buscarIdPorNombre(Connection conexion, String nombreRol) {
        String sql = "SELECT id_rol FROM roles WHERE nombre_rol = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombreRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_rol") : null;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el rol \"" + nombreRol + "\"", e);
            return null;
        }
    }
}
