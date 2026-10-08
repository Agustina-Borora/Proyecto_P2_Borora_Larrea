package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.ObraSocial;

/**
 * DAO para la tabla `obras_sociales`.
 */
public class ObraSocialDAO {

    private static final String SELECT_BASE
            = "SELECT id_obra_social, nombre_obra_social, codigo_interno, cuit, telefono, email, "
            + "direccion, activo_obra_social FROM obras_sociales ";

    /**
     * Devuelve los nombres de las obras sociales activas, ordenados alfabéticamente, para el
     * combo de "Obra Social" en Nuevo Análisis.
     */
    public static List<String> listarNombres(Connection con) {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT nombre_obra_social FROM obras_sociales WHERE activo_obra_social = 1 "
                + "ORDER BY nombre_obra_social";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                nombres.add(rs.getString("nombre_obra_social"));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar las obras sociales", e);
        }
        return nombres;
    }

    /**
     * Devuelve todas las obras sociales (activas e inactivas), para la tabla del ABM.
     */
    public static List<ObraSocial> listarTodas(Connection con) {
        List<ObraSocial> obrasSociales = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY nombre_obra_social";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                obrasSociales.add(mapear(rs));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar las obras sociales", e);
        }
        return obrasSociales;
    }

    public static ObraSocial buscarPorId(Connection con, int idObraSocial) {
        String sql = SELECT_BASE + "WHERE id_obra_social = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idObraSocial);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar la obra social", e);
            return null;
        }
    }

    /**
     * Crea una nueva obra social y devuelve su id generado, o null si falló.
     */
    public static Integer crear(Connection con, ObraSocial obraSocial) {
        String sql = "INSERT INTO obras_sociales (nombre_obra_social, codigo_interno, cuit, "
                + "telefono, email, direccion, activo_obra_social) VALUES (?, ?, ?, ?, ?, ?, 1)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            aplicarParametros(ps, obraSocial);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    int idGenerado = claves.getInt(1);
                    AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "ALTA", "obras_sociales", idGenerado,
                            "Obra social creada: " + obraSocial.getNombre());
                    return idGenerado;
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear la obra social \"" + obraSocial.getNombre() + "\"", e);
        }
        return null;
    }

    public static boolean actualizar(Connection con, ObraSocial obraSocial) {
        String sql = "UPDATE obras_sociales SET nombre_obra_social = ?, codigo_interno = ?, "
                + "cuit = ?, telefono = ?, email = ?, direccion = ?, activo_obra_social = ? "
                + "WHERE id_obra_social = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = aplicarParametros(ps, obraSocial);
            ps.setInt(i++, obraSocial.isActiva() ? 1 : 0);
            ps.setInt(i, obraSocial.getIdObraSocial());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "EDICION", "obras_sociales",
                        obraSocial.getIdObraSocial(), "Datos actualizados de " + obraSocial.getNombre());
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar la obra social", e);
            return false;
        }
    }

    /**
     * Baja lógica (no se borra el registro), mismo criterio que el resto del sistema.
     */
    public static boolean desactivar(Connection con, int idObraSocial) {
        String sql = "UPDATE obras_sociales SET activo_obra_social = 0 WHERE id_obra_social = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idObraSocial);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "BAJA", "obras_sociales", idObraSocial,
                        "Obra social dada de baja (id " + idObraSocial + ")");
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al dar de baja la obra social", e);
            return false;
        }
    }

    /**
     * Carga en el PreparedStatement los campos comunes a crear/actualizar (nombre, código
     * interno, CUIT, teléfono, email, dirección) y devuelve el índice del próximo parámetro
     * libre, para que cada método siga completando lo que le falte (el "activo" en crear ya va
     * fijo en el SQL, y en actualizar se agrega después de este llamado).
     */
    private static int aplicarParametros(PreparedStatement ps, ObraSocial os) throws SQLException {
        ps.setString(1, os.getNombre());
        ps.setString(2, os.getCodigoInterno());
        ps.setString(3, os.getCuit());
        ps.setString(4, os.getTelefono());
        ps.setString(5, os.getEmail());
        ps.setString(6, os.getDireccion());
        return 7;
    }

    private static ObraSocial mapear(ResultSet rs) throws SQLException {
        return new ObraSocial(
                rs.getInt("id_obra_social"),
                rs.getString("nombre_obra_social"),
                rs.getString("codigo_interno"),
                rs.getString("cuit"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("direccion"),
                rs.getInt("activo_obra_social") == 1);
    }
}
