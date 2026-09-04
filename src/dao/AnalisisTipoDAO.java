package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import modelo.Prestacion;

/**
 * El nomenclador (catálogo de códigos importado del Excel) y analisis_tipos
 * (lo que realmente usa pedido_analisis) son dos tablas sin relación directa
 * -- ver la revisión de 3FN que hicimos antes. Este DAO es el puente: busca
 * un analisis_tipo por código o por nombre y, si todavía no existe, lo crea
 * a partir de la prestación elegida del nomenclador (precio en 0 y en una
 * categoría "Sin Clasificar" mientras no haya una asignación real de precio
 * ni de categoría -- eso queda para más adelante).
 */
public class AnalisisTipoDAO {

    private static final String CATEGORIA_SIN_CLASIFICAR = "Sin Clasificar (Nomenclador)";

    /** Busca el analisis_tipo que corresponde a esta prestación del nomenclador, creándolo si hace falta. */
    public static Integer obtenerOCrearDesdeNomenclador(Connection con, Prestacion prestacion) {
        Integer idPorCodigo = buscarPorCodigo(con, prestacion.getCodigo());
        if (idPorCodigo != null) {
            return idPorCodigo;
        }
        Integer idPorNombre = buscarPorNombre(con, prestacion.getNombrePrestacion());
        if (idPorNombre != null) {
            return idPorNombre;
        }

        Integer idCategoria = obtenerOCrearCategoriaSinClasificar(con);
        if (idCategoria == null) {
            return null;
        }
        return crear(con, prestacion, idCategoria);
    }

    private static Integer buscarPorCodigo(Connection con, int codigo) {
        String sql = "SELECT id_analisis_tipo FROM analisis_tipos WHERE codigo_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(codigo));
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

    private static Integer crear(Connection con, Prestacion prestacion, int idCategoria) {
        String sql = "INSERT INTO analisis_tipos "
                + "(codigo_analisis, nombre_analisis, id_categoria, precio_base, activo_analisis, created_at) "
                + "VALUES (?, ?, ?, 0, 1, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, String.valueOf(prestacion.getCodigo()));
            ps.setString(2, prestacion.getNombrePrestacion());
            ps.setInt(3, idCategoria);
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

    private static Integer obtenerOCrearCategoriaSinClasificar(Connection con) {
        String sqlBuscar = "SELECT id_categoria FROM categorias_analisis WHERE nombre_categoria = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, CATEGORIA_SIN_CLASIFICAR);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_categoria");
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar la categoría \"Sin Clasificar\"", e);
            return null;
        }

        int siguienteOrden = 1;
        String sqlMax = "SELECT COALESCE(MAX(orden_categoria), 0) + 1 AS siguiente FROM categorias_analisis";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sqlMax)) {
            if (rs.next()) {
                siguienteOrden = rs.getInt("siguiente");
            }
        } catch (SQLException e) {
            // si falla el cálculo del orden seguimos con 1: no es crítico para poder crear la categoría
        }

        String sqlInsert = "INSERT INTO categorias_analisis (nombre_categoria, orden_categoria) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, CATEGORIA_SIN_CLASIFICAR);
            ps.setInt(2, siguienteOrden);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear la categoría \"Sin Clasificar\"", e);
        }
        return null;
    }
}
