package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO para la tabla `medicos`. "Medico Derivante" en Datos del Paciente es
 * un campo de texto libre (no un combo cargado de antemano), así que acá se
 * busca un médico ya cargado con ese nombre y, si no existe, se crea uno
 * nuevo -- mismo patrón de "buscar o crear" que dao.AnalisisTipoDAO usa para
 * el nomenclador.
 */
public class MedicoDAO {

    /**
     * Devuelve el id_medico correspondiente al nombre tipeado: null si el
     * campo vino vacío (es opcional, no todo pedido tiene médico derivante),
     * el id existente si ya hay un médico con ese nombre, o el de uno nuevo
     * recién creado si no existía.
     */
    public static Integer obtenerOCrear(Connection con, String nombreMedico) {
        if (nombreMedico == null || nombreMedico.trim().isEmpty()) {
            return null;
        }
        String nombre = nombreMedico.trim();

        Integer idExistente = buscarPorNombre(con, nombre);
        if (idExistente != null) {
            return idExistente;
        }
        return crear(con, nombre);
    }

    private static Integer buscarPorNombre(Connection con, String nombre) {
        String sql = "SELECT id_medico FROM medicos WHERE nombre_medico = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_medico");
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el médico", e);
        }
        return null;
    }

    private static Integer crear(Connection con, String nombre) {
        String sql = "INSERT INTO medicos (nombre_medico, activo_medico, created_at) VALUES (?, 1, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear el médico \"" + nombre + "\"", e);
        }
        return null;
    }
}
