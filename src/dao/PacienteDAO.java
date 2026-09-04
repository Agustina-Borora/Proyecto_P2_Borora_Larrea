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
 * Sigue el mismo patrón que ya usás en conexiones.Usuario: métodos estáticos
 * que reciben la Connection ya abierta y devuelven/reciben objetos del modelo.
 *
 * Los nombres de columna acá coinciden con el schema real de laboratorio.sql
 * (nya_paciente, dni_paciente, id_sexo, telefono_paciente, email_paciente,
 * id_plan, nro_afiliado, id_registrado_por).
 */
public class PacienteDAO {

    /**
     * Inserta un nuevo paciente. Devuelve el id_paciente generado, o null si
     * falló (ver el cartel de error que se muestra en ese caso). Antes
     * devolvía boolean; ahora hace falta el id para poder crear el pedido
     * que lo referencia, así que se pide con RETURN_GENERATED_KEYS.
     * idRegistradoPor tiene que ser el id del usuario logueado (conexiones.Sesion.idUsuario):
     * la columna es NOT NULL con FK a usuarios.
     */
    public static Integer insertar(Connection conexion, Paciente paciente) {
        String sql = "INSERT INTO pacientes " +
                "(nya_paciente, dni_paciente, fecha_nacimiento, id_sexo, telefono_paciente, " +
                "email_paciente, id_plan, nro_afiliado, id_registrado_por) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, paciente.getNyaPaciente());
            ps.setString(2, paciente.getDni());
            ps.setDate(3, paciente.getFechaNacimiento() != null
                    ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
            ps.setInt(4, paciente.getIdSexo());
            ps.setString(5, paciente.getTelefono());
            ps.setString(6, paciente.getEmail());
            if (paciente.getIdPlan() != null) {
                ps.setInt(7, paciente.getIdPlan());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setString(8, paciente.getNroAfiliado());
            ps.setInt(9, paciente.getIdRegistradoPor());

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
                "id_sexo = ?, telefono_paciente = ?, email_paciente = ?, id_plan = ?, nro_afiliado = ? " +
                "WHERE id_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, paciente.getNyaPaciente());
            ps.setString(2, paciente.getDni());
            ps.setDate(3, paciente.getFechaNacimiento() != null
                    ? new java.sql.Date(paciente.getFechaNacimiento().getTime()) : null);
            ps.setInt(4, paciente.getIdSexo());
            ps.setString(5, paciente.getTelefono());
            ps.setString(6, paciente.getEmail());
            if (paciente.getIdPlan() != null) {
                ps.setInt(7, paciente.getIdPlan());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setString(8, paciente.getNroAfiliado());
            ps.setInt(9, paciente.getIdPaciente());

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
     * Busca un paciente por DNI. Devuelve null si no existe.
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
     * Devuelve todos los pacientes cargados, ordenados por nombre.
     * Es lo que usa TablaPacientes para llenar la grilla. Trae además, con
     * LEFT JOIN, el nombre de la obra social (a través de planes_obra_social)
     * y la fecha del último pedido (MAX(pedidos.fecha_pedido)) para no dejar
     * esas dos columnas de la tabla vacías.
     */
    public static List<Paciente> listarTodos(Connection conexion) {
        List<Paciente> pacientes = new ArrayList<>();
        String sql =
                "SELECT p.*, os.nombre_obra_social, " +
                "(SELECT MAX(pe.fecha_pedido) FROM pedidos pe WHERE pe.id_paciente = p.id_paciente) AS ultimo_examen " +
                "FROM pacientes p " +
                "LEFT JOIN planes_obra_social plan ON plan.id_plan = p.id_plan " +
                "LEFT JOIN obras_sociales os ON os.id_obra_social = plan.id_obra_social " +
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
     * Convierte una fila del ResultSet en un objeto Paciente, leyendo solo las
     * columnas propias de la tabla `pacientes`. Centralizar esto acá evita
     * repetir el mapeo en cada método.
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
        int idPlan = rs.getInt("id_plan");
        p.setIdPlan(rs.wasNull() ? null : idPlan);
        p.setNroAfiliado(rs.getString("nro_afiliado"));
        p.setIdRegistradoPor(rs.getInt("id_registrado_por"));
        return p;
    }
}
