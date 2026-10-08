package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import modelo.ValorUb;

/**
 * DAO para la tabla `valor_ub` (histórico del precio de la Unidad Bioquímica).
 */
public class ValorUbDAO {

    /**
     * Devuelve el valor de la UB vigente a hoy: el más reciente cuya `vigente_desde` ya llegó
     * (no uno cargado a futuro). Null si todavía no hay ningún valor cargado.
     */
    public static BigDecimal obtenerVigente(Connection con) {
        String sql = "SELECT valor FROM valor_ub WHERE vigente_desde <= CURDATE() "
                + "ORDER BY vigente_desde DESC, id_valor_ub DESC LIMIT 1";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBigDecimal("valor");
            }

        } catch (SQLException e) {
            Mensajes.error("Error al obtener el valor vigente de la UB", e);
        }
        return null;
    }

    /**
     * Igual que {@link #obtenerVigente}, pero devuelve la fila completa (para mostrar en
     * pantalla el valor actual y desde cuándo rige).
     */
    public static ValorUb obtenerVigenteCompleto(Connection con) {
        String sql = "SELECT id_valor_ub, valor, vigente_desde FROM valor_ub WHERE vigente_desde <= CURDATE() "
                + "ORDER BY vigente_desde DESC, id_valor_ub DESC LIMIT 1";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                ValorUb v = new ValorUb();
                v.setIdValorUb(rs.getInt("id_valor_ub"));
                v.setValor(rs.getBigDecimal("valor"));
                java.sql.Date fecha = rs.getDate("vigente_desde");
                v.setVigenteDesde(fecha == null ? null : fecha.toLocalDate());
                return v;
            }

        } catch (SQLException e) {
            Mensajes.error("Error al obtener el valor vigente de la UB", e);
        }
        return null;
    }

    /**
     * Carga un nuevo valor de UB, vigente desde hoy. Nunca se edita ni se borra un valor
     * anterior -- queda como histórico y la consulta de "vigente" agarra siempre el último.
     */
    public static Integer crear(Connection con, BigDecimal valor) {
        String sql = "INSERT INTO valor_ub (valor, vigente_desde, created_at) VALUES (?, CURDATE(), NOW())";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, valor);
            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : null;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al cargar el nuevo valor de la UB", e);
            return null;
        }
    }
}
