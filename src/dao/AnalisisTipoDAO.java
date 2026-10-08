package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import modelo.AnalisisTipo;
import modelo.Prestacion;

/**
 * `prestaciones` (el catálogo importado del Excel) y `analisis_tipos` (lo que realmente usa
 * pedido_analisis) son tablas relacionadas por código: `analisis_tipos.codigo_analisis` es FK
 * directa a `prestaciones.codigo`.
 */
public class AnalisisTipoDAO {

    /**
     * Trae también el código y el estado (para el ABM) y, con un LEFT JOIN a `prestaciones`, las
     * unidades bioquímicas del examen -- de ahí se calcula el precio estimado (no es una columna
     * propia de analisis_tipos).
     */
    private static final String SELECT_ABM
            = "SELECT at.id_analisis_tipo, at.codigo_analisis, at.nombre_analisis, at.categoria, "
            + "at.activo_analisis, p.unidades_bioquimicas "
            + "FROM analisis_tipos at LEFT JOIN prestaciones p ON p.codigo = at.codigo_analisis ";

    /**
     * Devuelve todos los exámenes (activos e inactivos), con su precio estimado, para la tabla
     * del ABM del Catálogo de Exámenes.
     */
    public static List<AnalisisTipo> listarTodosParaAbm(Connection con) {
        List<AnalisisTipo> lista = new ArrayList<>();
        BigDecimal valorUb = ValorUbDAO.obtenerVigente(con);
        String sql = SELECT_ABM + "ORDER BY at.nombre_analisis";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapearAbm(rs, valorUb));
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los exámenes", e);
        }
        return lista;
    }

    public static AnalisisTipo buscarPorIdAbm(Connection con, int idAnalisisTipo) {
        String sql = SELECT_ABM + "WHERE at.id_analisis_tipo = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAnalisisTipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAbm(rs, ValorUbDAO.obtenerVigente(con)) : null;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el examen", e);
            return null;
        }
    }

    /**
     * Crea un examen nuevo (código opcional -- puede no tener prestación asociada en el
     * nomenclador) y devuelve su id generado, o null si falló.
     */
    public static Integer crear(Connection con, AnalisisTipo tipo) {
        String sql = "INSERT INTO analisis_tipos (codigo_analisis, nombre_analisis, categoria, "
                + "activo_analisis, created_at) VALUES (?, ?, ?, 1, NOW())";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            aplicarCodigo(ps, 1, tipo.getCodigoAnalisis());
            ps.setString(2, tipo.getNombreAnalisis());
            ps.setString(3, tipo.getCategoria());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    return null;
                }
                int idGenerado = claves.getInt(1);
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "ALTA", "analisis_tipos", idGenerado,
                        "Examen creado: " + tipo.getNombreAnalisis());
                return idGenerado;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear el examen \"" + tipo.getNombreAnalisis() + "\"", e);
            return null;
        }
    }

    public static boolean actualizar(Connection con, AnalisisTipo tipo) {
        String sql = "UPDATE analisis_tipos SET codigo_analisis = ?, nombre_analisis = ?, categoria = ?, "
                + "activo_analisis = ? WHERE id_analisis_tipo = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            aplicarCodigo(ps, 1, tipo.getCodigoAnalisis());
            ps.setString(2, tipo.getNombreAnalisis());
            ps.setString(3, tipo.getCategoria());
            ps.setInt(4, tipo.isActivo() ? 1 : 0);
            ps.setInt(5, tipo.getIdAnalisisTipo());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "EDICION", "analisis_tipos",
                        tipo.getIdAnalisisTipo(), "Datos actualizados de " + tipo.getNombreAnalisis());
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el examen", e);
            return false;
        }
    }

    /**
     * Alta/baja lógica -- mismo criterio que el resto del sistema (no se borra el registro).
     */
    public static boolean cambiarActivo(Connection con, int idAnalisisTipo, boolean activo) {
        String sql = "UPDATE analisis_tipos SET activo_analisis = ? WHERE id_analisis_tipo = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, idAnalisisTipo);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "CAMBIO_ESTADO", "analisis_tipos",
                        idAnalisisTipo, "Examen " + (activo ? "activado" : "desactivado") + " (id " + idAnalisisTipo + ")");
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al cambiar el estado del examen", e);
            return false;
        }
    }

    private static void aplicarCodigo(PreparedStatement ps, int indice, Integer codigo) throws SQLException {
        if (codigo == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, codigo);
        }
    }

    private static AnalisisTipo mapearAbm(ResultSet rs, BigDecimal valorUb) throws SQLException {
        AnalisisTipo tipo = new AnalisisTipo();
        tipo.setIdAnalisisTipo(rs.getInt("id_analisis_tipo"));
        int codigo = rs.getInt("codigo_analisis");
        tipo.setCodigoAnalisis(rs.wasNull() ? null : codigo);
        tipo.setNombreAnalisis(rs.getString("nombre_analisis"));
        tipo.setCategoria(rs.getString("categoria"));
        tipo.setActivo(rs.getInt("activo_analisis") == 1);

        BigDecimal unidades = rs.getBigDecimal("unidades_bioquimicas");
        tipo.setUnidadesBioquimicas(unidades);
        if (unidades != null && valorUb != null) {
            tipo.setPrecioEstimado(unidades.multiply(valorUb));
        }
        return tipo;
    }

    /**
     * Busca el analisis_tipo que corresponde a esta prestación del nomenclador, creándolo si hace
     * falta.
     *
     * <p>Ojo: el nomenclador real factura por "grupo" en algunos casos -- un mismo código cubre
     * varias determinaciones distintas con el mismo arancel (ej. código 1050 "Drogas de Abuso
     * Screening" cubre Anfetaminas, Cannabinoides, Cocaína, etc., todas con el mismo precio). Por
     * eso NO alcanza con reusar el primer analisis_tipo que tenga ese código -- si dos exámenes
     * distintos comparten código, hay que tratarlos como exámenes distintos igual. Por eso acá se
     * exige que coincidan código Y nombre para considerarlos "el mismo" examen; solo el nombre
     * (sin código) sigue sirviendo como respaldo para no duplicar un examen que ya se había
     * cargado a mano.</p>
     *
     * <p>Ninguna de las dos búsquedas de acá abajo considera un examen dado de baja como "el
     * mismo": si el único que coincide está inactivo, se trata como si no existiera y se crea uno
     * nuevo (activo) en su lugar -- así "dar de baja" un examen en Catálogo de Exámenes de verdad
     * saca esa fila de circulación para pedidos nuevos, en vez de que una orden termine
     * reusándola igual sin que se note.</p>
     */
    public static Integer obtenerOCrearDesdeNomenclador(Connection con, Prestacion prestacion) {
        Integer idPorCodigoYNombre = buscarPorCodigoYNombre(con, prestacion.getCodigo(), prestacion.getNombrePrestacion());
        if (idPorCodigoYNombre != null) {
            return idPorCodigoYNombre;
        }
        Integer idPorNombre = buscarPorNombre(con, prestacion.getNombrePrestacion());
        if (idPorNombre != null) {
            return idPorNombre;
        }

        return crear(con, prestacion);
    }

    private static Integer buscarPorCodigoYNombre(Connection con, int codigo, String nombre) {
        String sql = "SELECT id_analisis_tipo FROM analisis_tipos "
                + "WHERE codigo_analisis = ? AND nombre_analisis = ? AND activo_analisis = 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            ps.setString(2, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_analisis_tipo");
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar el análisis por código y nombre", e);
        }
        return null;
    }

    private static Integer buscarPorNombre(Connection con, String nombre) {
        String sql = "SELECT id_analisis_tipo FROM analisis_tipos WHERE nombre_analisis = ? AND activo_analisis = 1";
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
     * Busca, entre los exámenes ACTIVOS del catálogo, el/los que tengan exactamente este código
     * -- se usa para las sugerencias de "Estudios solicitados" en Nuevo Análisis, para que ese
     * buscador ofrezca el examen tal cual está cargado en Catálogo de Exámenes (con el nombre que
     * le puso Agus y ya con sus parámetros configurados, si los tiene) en vez del nombre crudo
     * del nomenclador, y para que uno dado de baja deje de aparecer como opción. Devuelve una
     * lista (no un solo resultado) porque nada impide, si el catálogo tiene códigos repetidos por
     * error, que haya más de un examen activo con el mismo código -- en ese caso se muestran
     * todos y es Agus quien elige cuál.
     */
    public static List<Prestacion> buscarActivosPorCodigo(Connection con, int codigo) {
        String sql = "SELECT at.codigo_analisis, at.nombre_analisis, p.unidades_bioquimicas "
                + "FROM analisis_tipos at LEFT JOIN prestaciones p ON p.codigo = at.codigo_analisis "
                + "WHERE at.activo_analisis = 1 AND at.codigo_analisis = ? "
                + "ORDER BY at.nombre_analisis";
        List<Prestacion> resultado = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearComoPrestacion(rs));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar en el catálogo de exámenes", e);
        }
        return resultado;
    }

    /**
     * Misma idea que {@link #buscarActivosPorCodigo}, pero por nombre (usa el nombre que tiene
     * cargado el examen en el catálogo, no el del nomenclador).
     */
    public static List<Prestacion> buscarActivosPorNombre(Connection con, String texto) {
        String sql = "SELECT at.codigo_analisis, at.nombre_analisis, p.unidades_bioquimicas "
                + "FROM analisis_tipos at LEFT JOIN prestaciones p ON p.codigo = at.codigo_analisis "
                + "WHERE at.activo_analisis = 1 AND at.nombre_analisis LIKE ? "
                + "ORDER BY at.nombre_analisis";
        List<Prestacion> resultado = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearComoPrestacion(rs));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar en el catálogo de exámenes", e);
        }
        return resultado;
    }

    private static Prestacion mapearComoPrestacion(ResultSet rs) throws SQLException {
        Prestacion p = new Prestacion();
        p.setCodigo(rs.getInt("codigo_analisis"));
        p.setNombrePrestacion(rs.getString("nombre_analisis"));
        p.setUnidadesBioquimicas(rs.getBigDecimal("unidades_bioquimicas"));
        return p;
    }

    /**
     * Devuelve las categorías ya usadas (sin repetir, sin nulos/vacías, ordenadas alfabéticamente)
     * -- se usa para ofrecerlas en el desplegable del formulario de examen, así se evita que dos
     * exámenes de la misma categoría queden separados en la tabla por una diferencia de tipeo
     * (ej. "Hematologia" vs "Hematología").
     */
    public static List<String> listarCategorias(Connection con) {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT DISTINCT categoria FROM analisis_tipos "
                + "WHERE categoria IS NOT NULL AND categoria <> '' ORDER BY categoria";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                categorias.add(rs.getString("categoria"));
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar las categorías", e);
        }
        return categorias;
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
