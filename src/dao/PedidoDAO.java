package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import modelo.PedidoCreado;

/**
 * DAO para `pedidos` y `pedido_analisis`, usado por "Generar Orden" en
 * Nuevo Análisis. Los montos (subtotal/total_*) quedan en 0 por ahora: no
 * hay todavía una fuente de precios por análisis (analisis_tipos.precio_base
 * se crea en 0 para lo que viene del nomenclador, ver AnalisisTipoDAO), así
 * que armar una facturación real es una tarea aparte, pendiente.
 */
public class PedidoDAO {

    /**
     * Crea el pedido y le arma un numero_pedido legible ("PED-000123") a
     * partir del id_pedido generado, en un segundo UPDATE porque el número
     * depende de un id que todavía no existe al momento del INSERT.
     * numero_pedido es NOT NULL sin default, así que el INSERT igual necesita
     * mandarle algo: va un valor temporal único, que el UPDATE de abajo pisa
     * enseguida con el número final.
     */
    public static PedidoCreado crearPedido(Connection con, int idPaciente, Integer idMedico, int idRegistradoPor) {
        String numeroTemporal = "TMP-" + System.currentTimeMillis();
        String sqlInsert = "INSERT INTO pedidos "
                + "(numero_pedido, id_paciente, id_medico, id_registrado_por, fecha_pedido, estado_pedido, prioridad_pedido, "
                + "subtotal, total_cobrado, total_obra_social, total_paciente, created_at) "
                + "VALUES (?, ?, ?, ?, CURDATE(), 'pendiente', 'normal', 0, 0, 0, 0, NOW())";

        int idPedido;
        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numeroTemporal);
            ps.setInt(2, idPaciente);
            if (idMedico != null) {
                ps.setInt(3, idMedico);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setInt(4, idRegistradoPor);

            if (ps.executeUpdate() == 0) {
                return null;
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    return null;
                }
                idPedido = claves.getInt(1);
            }
        } catch (SQLException e) {
            Mensajes.error("Error al crear el pedido", e);
            return null;
        }

        String numeroPedido = "PED-" + String.format("%06d", idPedido);
        String sqlNumero = "UPDATE pedidos SET numero_pedido = ? WHERE id_pedido = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlNumero)) {
            ps.setString(1, numeroPedido);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        } catch (SQLException e) {
            Mensajes.error("Error al numerar el pedido", e);
            return null;
        }

        return new PedidoCreado(idPedido, numeroPedido);
    }

    /** Agrega una fila a pedido_analisis para uno de los análisis elegidos en la orden. */
    public static boolean agregarAnalisis(Connection con, int idPedido, int idAnalisisTipo) {
        String sql = "INSERT INTO pedido_analisis (id_pedido, id_analisis_tipo, precio_aplicado, estado_analisis, created_at) "
                + "VALUES (?, ?, 0, 'pendiente', NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.setInt(2, idAnalisisTipo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al agregar el análisis al pedido", e);
            return false;
        }
    }
}
