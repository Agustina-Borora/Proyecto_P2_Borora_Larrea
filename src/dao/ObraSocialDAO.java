package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla `obras_sociales`.
 */
public class ObraSocialDAO {

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
}
