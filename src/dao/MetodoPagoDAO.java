package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla `metodos_pago`.
 */
public class MetodoPagoDAO {

    /**
     * Devuelve los nombres de los métodos de pago activos, ordenados alfabéticamente, para el
     * combo de "Método de Pago" en Nuevo Análisis.
     */
    public static List<String> listarNombres(Connection con) {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT nombre_metodo FROM metodos_pago WHERE activo = 1 ORDER BY nombre_metodo";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                nombres.add(rs.getString("nombre_metodo"));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar los métodos de pago", e);
        }
        return nombres;
    }
}
