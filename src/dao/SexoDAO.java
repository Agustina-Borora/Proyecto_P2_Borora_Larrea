package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.Sexo;

/**
 * DAO para la tabla de referencia `sexos`. Mismo patrón que los demás DAO:
 * métodos estáticos que reciben la Connection ya abierta.
 */
public class SexoDAO {

    /**
     * Devuelve todos los sexos disponibles, ordenados por id_sexo (para que
     * el orden del combo sea siempre el mismo).
     */
    public static List<Sexo> listarTodos(Connection conexion) {
        List<Sexo> sexos = new ArrayList<>();
        String sql = "SELECT id_sexo, nombre_sexo FROM sexos ORDER BY id_sexo";

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                sexos.add(new Sexo(rs.getInt("id_sexo"), rs.getString("nombre_sexo")));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar los sexos", e);
        }

        return sexos;
    }
}
