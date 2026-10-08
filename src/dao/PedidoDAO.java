package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import modelo.EstadoNumeracion;
import modelo.PedidoCreado;

/**
 * DAO para `pedidos` y `pedido_analisis`, usado por "Generar Orden" en Nuevo Análisis.
 */
public class PedidoDAO {

    /**
     * Crea el pedido con numeración automática mensual (Figma: tarjeta "Numeración automática de
     * órdenes" en Configuración &gt; Sistema): formato AAAA-MM-NNN, donde NNN es cuántas órdenes
     * ya se generaron en el mes en curso + 1 -- se reinicia solo cada mes porque el conteo sale de
     * contar sólo los numero_pedido que ya tienen el prefijo del mes actual (ver
     * {@link #contarPedidosDelMes}), así que no hace falta ningún trabajo aparte al cambiar de
     * mes. A diferencia del esquema anterior ("PED-000123", secuencial y sin reinicio, armado a
     * partir del id_pedido) el número ya no depende del id generado, así que el pedido se puede
     * insertar directo con su numero_pedido definitivo, sin el UPDATE aparte que hacía falta
     * antes.
     *
     * <p>Los pedidos creados antes de este cambio quedan con su viejo número "PED-NNNNNN" -- no
     * se renumeran, para no reescribir historial ya impreso o entregado.</p>
     */
    public static PedidoCreado crearPedido(Connection con, int idPaciente, Integer idMedico, int idRegistradoPor) {
        // total_pedido queda en 0 (su DEFAULT) hasta el llamado aparte a actualizarTotal(), ya
        // con el id_pedido generado -- ver NuevoAnalisisController.generarOrden.
        String prefijoMes = prefijoMesActual();
        int siguiente = contarPedidosDelMes(con, prefijoMes) + 1;
        String numeroPedido = prefijoMes + "-" + String.format("%03d", siguiente);

        String sqlInsert = "INSERT INTO pedidos "
                + "(numero_pedido, id_paciente, id_medico, id_registrado_por, fecha_pedido, estado_pedido, prioridad_pedido, created_at) "
                + "VALUES (?, ?, ?, ?, CURDATE(), 'pendiente', 'normal', NOW())";

        int idPedido;
        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numeroPedido);
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

        return new PedidoCreado(idPedido, numeroPedido);
    }

    /**
     * Estado actual de la numeración automática, para la tarjeta de Configuración &gt; Sistema:
     * el número que le tocaría a la próxima orden, el mes en curso y cuántas se generaron ya.
     */
    public static EstadoNumeracion obtenerEstadoNumeracion(Connection con) {
        String prefijoMes = prefijoMesActual();
        int cantidad = contarPedidosDelMes(con, prefijoMes);

        EstadoNumeracion estado = new EstadoNumeracion();
        estado.setMesEnCurso(prefijoMes);
        estado.setOrdenesEsteMes(cantidad);
        estado.setProximoNumero(prefijoMes + "-" + String.format("%03d", cantidad + 1));
        return estado;
    }

    private static String prefijoMesActual() {
        return new SimpleDateFormat("yyyy-MM").format(new Date());
    }

    /**
     * Cuántos pedidos ya tienen un numero_pedido con el prefijo del mes dado (formato "AAAA-MM-")
     * -- los pedidos viejos con el formato "PED-NNNNNN" nunca matchean este LIKE, así que no hace
     * falta distinguirlos aparte.
     */
    private static int contarPedidosDelMes(Connection con, String prefijoMes) {
        String sql = "SELECT COUNT(*) FROM pedidos WHERE numero_pedido LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefijoMes + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al calcular el número de la orden", e);
            return 0;
        }
    }

    /**
     * Guarda el total calculado en pantalla (suma de UB de los análisis elegidos + el acto
     * bioquímico, × el valor de la UB vigente). Antes de esto {@code total_pedido} quedaba
     * siempre en 0 (su DEFAULT) porque nada se lo pasaba nunca -- ver el comentario que había en
     * {@link #crearPedido}.
     */
    public static boolean actualizarTotal(Connection con, int idPedido, java.math.BigDecimal total) {
        String sql = "UPDATE pedidos SET total_pedido = ? WHERE id_pedido = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, idPedido);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al guardar el total del pedido", e);
            return false;
        }
    }

    /**
     * Agrega una fila a pedido_analisis para uno de los análisis elegidos en la orden.
     */
    public static boolean agregarAnalisis(Connection con, int idPedido, int idAnalisisTipo) {
        String sql = "INSERT INTO pedido_analisis (id_pedido, id_analisis_tipo, estado_analisis, created_at) "
                + "VALUES (?, ?, 'pendiente', NOW())";
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
