package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
}
