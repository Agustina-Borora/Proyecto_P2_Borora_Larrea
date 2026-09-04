package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO para `pedido_analito_resultado`: el valor cargado de cada analito,
 * dentro de un pedido_analisis puntual (un examen de una orden). Es el
 * sistema real (ya tenía datos desde el 13/08); reemplaza al
 * dao.ResultadoDAO que armé el 03/09 sin saber que este ya existía.
 */
public class PedidoAnalitoResultadoDAO {

    /**
     * Trae los resultados ya guardados de un pedido_analisis, como
     * id_analito -> valor, para precargar la pantalla si se reabre un
     * examen que ya tenía algo cargado.
     */
    public static Map<Integer, String> listarResultados(Connection con, int idPedidoAnalisis) {
        Map<Integer, String> resultados = new HashMap<>();

        String sql = "SELECT id_analito, valor_resultado FROM pedido_analito_resultado WHERE id_pedido_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedidoAnalisis);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.put(rs.getInt("id_analito"), rs.getString("valor_resultado"));
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al leer los resultados guardados", e);
        }

        return resultados;
    }

    /**
     * Guarda (o actualiza si ya existía) el valor de un analito para un
     * pedido_analisis. Necesita la UNIQUE KEY (id_pedido_analisis,
     * id_analito) que se agregó en migracion_02_completar_analitos.sql.
     */
    public static boolean guardarResultado(Connection con, int idPedidoAnalisis, int idAnalito, String valor) {
        String sql = "INSERT INTO pedido_analito_resultado (id_pedido_analisis, id_analito, valor_resultado) "
                + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE valor_resultado = VALUES(valor_resultado)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedidoAnalisis);
            ps.setInt(2, idAnalito);
            ps.setString(3, valor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al guardar el resultado", e);
            return false;
        }
    }

    /** Guarda todos los valores cargados de una sola vez (uno por analito). */
    public static boolean guardarTodos(Connection con, int idPedidoAnalisis, Map<Integer, String> valores) {
        boolean todoOk = true;
        for (Map.Entry<Integer, String> entrada : valores.entrySet()) {
            if (!guardarResultado(con, idPedidoAnalisis, entrada.getKey(), entrada.getValue())) {
                todoOk = false;
            }
        }
        return todoOk;
    }

    /** Cambia el estado del examen (ej. a "completado" cuando ya se cargaron todos los valores). */
    public static boolean actualizarEstado(Connection con, int idPedidoAnalisis, String estado) {
        String sql = "UPDATE pedido_analisis SET estado_analisis = ? WHERE id_pedido_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idPedidoAnalisis);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el estado del examen", e);
            return false;
        }
    }
}
