package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.EstadisticasEscritorio;
import modelo.OrdenResumen;

/**
 * DAO para la pantalla Escritorio: las 4 tarjetas resumen y la tabla
 * "Últimas Órdenes". Mismo patrón que PacienteDAO: métodos estáticos que
 * reciben la Connection ya abierta.
 */
public class EscritorioDAO {

    /**
     * Cuenta los pedido_analisis del mes en curso agrupados por estado, y arma
     * el objeto que alimenta las 4 tarjetas (Total del mes / Emitidas / En
     * proceso / Pendientes). Los cancelados no se cuentan en ninguna tarjeta.
     */
    public static EstadisticasEscritorio obtenerEstadisticasDelMes(Connection conexion) {
        EstadisticasEscritorio stats = new EstadisticasEscritorio();

        String sql = "SELECT estado_analisis, COUNT(*) AS cantidad " +
                "FROM pedido_analisis " +
                "WHERE YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE()) " +
                "GROUP BY estado_analisis";

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String estado = rs.getString("estado_analisis");
                int cantidad = rs.getInt("cantidad");

                if ("completado".equals(estado)) {
                    stats.setEmitidas(cantidad);
                } else if ("en_proceso".equals(estado)) {
                    stats.setEnProceso(cantidad);
                } else if ("pendiente".equals(estado)) {
                    stats.setPendientes(cantidad);
                }
                // "cancelado" no suma a ninguna tarjeta
            }

            stats.setTotalMes(stats.getEmitidas() + stats.getEnProceso() + stats.getPendientes());

        } catch (SQLException e) {
            Mensajes.error("Error al calcular las estadísticas del escritorio", e);
        }

        return stats;
    }

    /**
     * Trae las últimas órdenes (una fila por análisis pedido, no por pedido
     * completo) para la tabla del Escritorio, con el estado ya traducido al
     * texto que se muestra en pantalla.
     */
    public static List<OrdenResumen> listarUltimasOrdenes(Connection conexion, int limite) {
        List<OrdenResumen> ordenes = new ArrayList<>();

        String sql = "SELECT pe.numero_pedido, p.nya_paciente, at.nombre_analisis, pe.fecha_pedido, " +
                "pa.estado_analisis, " +
                "(SELECT COUNT(*) FROM envios e WHERE e.id_pedido = pe.id_pedido) AS cant_envios " +
                "FROM pedido_analisis pa " +
                "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido " +
                "JOIN pacientes p ON p.id_paciente = pe.id_paciente " +
                "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo " +
                "ORDER BY pa.created_at DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrdenResumen orden = new OrdenResumen();
                    orden.setNumeroOrden(rs.getString("numero_pedido"));
                    orden.setPaciente(rs.getString("nya_paciente"));
                    orden.setExamen(rs.getString("nombre_analisis"));
                    orden.setFecha(rs.getDate("fecha_pedido"));
                    orden.setEstado(EstadoAnalisisUtil.traducir(rs.getString("estado_analisis"), rs.getInt("cant_envios")));
                    ordenes.add(orden);
                }
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar las últimas órdenes", e);
        }

        return ordenes;
    }
}
