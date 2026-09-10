package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.Paciente;

/**
 * DAO (Data Access Object) para la tabla `pacientes`.
 */
public class PacienteDAO {

    /**
     * Inserta un nuevo paciente.
     */
    public static Integer insertar(Connection conexion, Paciente paciente) {
        String sql = "INSERT INTO pacientes " +
                "(nya_paciente, dni_paciente, fecha_nacimiento, id_sexo, telefono_paciente, email_paciente) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, paciente.getNyaPaciente());
            ps.setString(2, paciente.getDni());
            ps.setDate(3, paciente.getFechaNacimiento() != null
                    ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
            ps.setInt(4, paciente.getIdSexo());
            ps.setString(5, paciente.getTelefono());
            ps.setString(6, paciente.getEmail());

            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
            return null;

        } catch (SQLException e) {
            Mensajes.error("Error al guardar el paciente", e);
            return null;
        }
    }

    /**
     * Actualiza los datos de un paciente existente, identificado por idPaciente.
     */
    public static boolean actualizar(Connection conexion, Paciente paciente) {
        String sql = "UPDATE pacientes SET nya_paciente = ?, dni_paciente = ?, fecha_nacimiento = ?, " +
                "id_sexo = ?, telefono_paciente = ?, email_paciente = ? " +
                "WHERE id_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, paciente.getNyaPaciente());
            ps.setString(2, paciente.getDni());
            ps.setDate(3, paciente.getFechaNacimiento() != null
                    ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
            ps.setInt(4, paciente.getIdSexo());
            ps.setString(5, paciente.getTelefono());
            ps.setString(6, paciente.getEmail());
            ps.setInt(7, paciente.getIdPaciente());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el paciente", e);
            return false;
        }
    }

    /**
     * Elimina un paciente por id.
     */
    public static boolean eliminar(Connection conexion, int idPaciente) {
        String sql = "DELETE FROM pacientes WHERE id_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            Mensajes.error("Error al eliminar el paciente", e);
            return false;
        }
    }

    /**
     * Busca un paciente por DNI.
     */
    public static Paciente buscarPorDni(Connection conexion, String dni) {
        String sql = "SELECT * FROM pacientes WHERE dni_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPaciente(rs);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el paciente", e);
        }
        return null;
    }

    /**
     * Devuelve todos los pacientes cargados, ordenados por apellido y nombre. La "obra social"
     * mostrada es la lista de todas las que tiene asociadas el paciente (puede tener varias,
     * vía `paciente_obra_social`), separadas por coma; null si no tiene ninguna.
     */
    public static List<Paciente> listarTodos(Connection conexion) {
        List<Paciente> pacientes = new ArrayList<>();
        String sql =
                "SELECT p.*, " +
                "(SELECT GROUP_CONCAT(os.nombre_obra_social SEPARATOR ', ') " +
                "   FROM paciente_obra_social pos " +
                "   JOIN obras_sociales os ON os.id_obra_social = pos.id_obra_social " +
                "   WHERE pos.id_paciente = p.id_paciente) AS nombre_obra_social, " +
                "(SELECT MAX(pe.fecha_pedido) FROM pedidos pe WHERE pe.id_paciente = p.id_paciente) AS ultimo_examen " +
                "FROM pacientes p " +
                "ORDER BY p.nya_paciente";

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Paciente p = mapearPaciente(rs);
                p.setNombreObraSocial(rs.getString("nombre_obra_social"));
                p.setUltimoExamen(rs.getDate("ultimo_examen"));
                pacientes.add(p);
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar pacientes", e);
        }
        return pacientes;
    }

    /**
     * Convierte una fila del ResultSet en un objeto Paciente, leyendo solo las columnas propias de
     * la tabla `pacientes`.
     */
    private static Paciente mapearPaciente(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        p.setIdPaciente(rs.getInt("id_paciente"));
        p.setNyaPaciente(rs.getString("nya_paciente"));
        p.setDni(rs.getString("dni_paciente"));
        p.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
        p.setIdSexo(rs.getInt("id_sexo"));
        p.setTelefono(rs.getString("telefono_paciente"));
        p.setEmail(rs.getString("email_paciente"));
        return p;
    }
}
