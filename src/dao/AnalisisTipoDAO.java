package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import modelo.Prestacion;

/**
 * `prestaciones` (el catálogo importado del Excel) y `analisis_tipos` (lo que realmente usa
 * pedido_analisis) son tablas relacionadas por código: `analisis_tipos.codigo_analisis` es FK
 * directa a `prestaciones.codigo`.
 */
public class AnalisisTipoDAO {

    /**
     * Busca el analisis_tipo que corresponde a esta prestación del nomenclador, creándolo si hace
     * falta.
     */
    public static Integer obtenerOCrearDesdeNomenclador(Connection con, Prestacion prestacion) {
        Integer idPorCodigo = buscarPorCodigo(con, prestacion.getCodigo());
        if (idPorCodigo != null) {
            return idPorCodigo;
        }
        Integer idPorNombre = buscarPorNombre(con, prestacion.getNombrePrestacion());
        if (idPorNombre != null) {
            return idPorNombre;
        }

        return crear(con, prestacion);
    }

    private static Integer buscarPorCodigo(Connection con, int codigo) {
        String sql = "SELECT id_analisis_tipo FROM analisis_tipos WHERE codigo_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_analisis_tipo");
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el análisis por código", e);
        }
        return null;
    }

    private static Integer buscarPorNombre(Connection con, String nombre) {
        String sql = "SELECT id_analisis_tipo FROM analisis_tipos WHERE nombre_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_analisis_tipo");
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el análisis por nombre", e);
        }
        return null;
    }

    private static Integer crear(Connection con, Prestacion prestacion) {
        String sql = "INSERT INTO analisis_tipos (codigo_analisis, nombre_analisis, activo_analisis, created_at) "
                + "VALUES (?, ?, 1, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, prestacion.getCodigo());
            ps.setString(2, prestacion.getNombrePrestacion());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear el análisis \"" + prestacion.getNombrePrestacion() + "\"", e);
        }
        return null;
    }

    /**
     * Devuelve todos los análisis activos, ordenados por nombre -- se usa para listar los
     * exámenes en la tabla de permisos por examen (Nuevo Usuario).
     */
    public static java.util.List<modelo.AnalisisTipo> listarTodos(Connection conexion) {
        java.util.List<modelo.AnalisisTipo> tipos = new java.util.ArrayList<>();
        String sql = "SELECT id_analisis_tipo, nombre_analisis FROM analisis_tipos "
                + "WHERE activo_analisis = 1 ORDER BY nombre_analisis";

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                tipos.add(new modelo.AnalisisTipo(rs.getInt("id_analisis_tipo"), rs.getString("nombre_analisis")));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar los exámenes", e);
        }

        return tipos;
    }
}
