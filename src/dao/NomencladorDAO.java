package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.Prestacion;

/**
 * DAO para la tabla `nomenclador` (el catálogo cargado desde el Excel que
 * ya se había pasado antes: código, nombre de la prestación y unidades
 * bioquímicas). Se usa desde Solicitud de Análisis para elegir los
 * análisis de una orden por código o por nombre.
 */
public class NomencladorDAO {

    /**
     * Busca una prestación por código exacto. Devuelve null si no existe.
     */
    public static Prestacion buscarPorCodigo(Connection conexion, int codigo) {
        String sql = "SELECT id_nomenclador, codigo, nombre_prestacion, unidades_bioquimicas " +
                "FROM nomenclador WHERE codigo = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar en el nomenclador", e);
        }
        return null;
    }

    /**
     * Busca prestaciones cuyo nombre contenga el texto dado (no distingue
     * mayúsculas/minúsculas por el collation de la base). Puede devolver
     * más de una coincidencia.
     */
    public static List<Prestacion> buscarPorNombre(Connection conexion, String texto) {
        List<Prestacion> resultado = new ArrayList<>();
        String sql = "SELECT id_nomenclador, codigo, nombre_prestacion, unidades_bioquimicas " +
                "FROM nomenclador WHERE nombre_prestacion LIKE ? ORDER BY nombre_prestacion";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar en el nomenclador", e);
        }
        return resultado;
    }

    private static Prestacion mapear(ResultSet rs) throws SQLException {
        Prestacion p = new Prestacion();
        p.setIdNomenclador(rs.getInt("id_nomenclador"));
        p.setCodigo(rs.getInt("codigo"));
        p.setNombrePrestacion(rs.getString("nombre_prestacion"));
        p.setUnidadesBioquimicas(rs.getBigDecimal("unidades_bioquimicas"));
        return p;
    }
}
