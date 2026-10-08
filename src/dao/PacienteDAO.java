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
                "(nya_paciente, dni_paciente, fecha_nacimiento, id_sexo, telefono_paciente, email_paciente, " +
                "activo_paciente) VALUES (?, ?, ?, ?, ?, ?, 1)";

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
                    int idGenerado = claves.getInt(1);
                    AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "ALTA", "pacientes", idGenerado,
                            "Paciente creado: " + paciente.getNyaPaciente() + " (DNI " + paciente.getDni() + ")");
                    return idGenerado;
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

            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "EDICION", "pacientes",
                        paciente.getIdPaciente(), "Datos actualizados de " + paciente.getNyaPaciente());
            }
            return ok;

        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el paciente", e);
            return false;
        }
    }

    /**
     * "Elimina" un paciente por id -- borrado lógico (columna `activo_paciente`, migración
     * sql/2026-09-29_pacientes_borrado_logico.sql), no un DELETE real: un laboratorio no puede
     * perder el historial de órdenes/resultados de un paciente sólo porque se lo "elimina" de la
     * pantalla Pacientes, y un DELETE físico corre ese riesgo (falla o borra en cadena, según cómo
     * queden las FK hacia esta tabla el día de mañana). Mismo criterio que ya usan
     * {@code UsuarioDAO#desactivar} y {@code ObraSocialDAO#desactivar}. El paciente deja de
     * aparecer en {@link #listarTodos}, pero sigue siendo encontrable por
     * {@link #buscarPorDni}/{@link #buscarPorId} (a propósito -- ver el javadoc de esos métodos).
     */
    public static boolean eliminar(Connection conexion, int idPaciente) {
        String sql = "UPDATE pacientes SET activo_paciente = 0 WHERE id_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(conexion, modelo.Sesion.idUsuario, "BAJA", "pacientes", idPaciente,
                        "Paciente dado de baja (id " + idPaciente + ")");
            }
            return ok;

        } catch (SQLException e) {
            Mensajes.error("Error al eliminar el paciente", e);
            return false;
        }
    }

    /**
     * Busca un paciente por DNI. No filtra por `activo_paciente` a propósito: lo usan Nuevo Análisis
     * y Registrar Resultados para encontrar/reutilizar al paciente de una orden, y un paciente
     * "eliminado" (borrado lógico, ver {@link #eliminar}) puede seguir teniendo órdenes viejas que
     * hay que poder mostrar igual.
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
     * Busca un paciente por id_paciente -- lo usa "Editar Orden" (Registros) para traer el
     * paciente completo (con fecha_nacimiento e id_sexo, que esa pantalla no edita) antes de
     * pisarle solo nombre/DNI/celular/email y guardarlo con {@link #actualizar}. Tampoco filtra
     * por `activo_paciente` -- mismo motivo que {@link #buscarPorDni}.
     */
    public static Paciente buscarPorId(Connection conexion, int idPaciente) {
        String sql = "SELECT * FROM pacientes WHERE id_paciente = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);

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
     * Devuelve todos los pacientes activos (no "eliminados", ver {@link #eliminar}), ordenados por
     * apellido y nombre. La "obra social" mostrada es la lista de todas las que tiene asociadas el
     * paciente (puede tener varias, vía `paciente_obra_social`), separadas por coma; null si no
     * tiene ninguna.
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
                "WHERE p.activo_paciente = 1 " +
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
        p.setActivo(rs.getInt("activo_paciente") == 1);
        return p;
    }
}
