package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.OrdenResumen;

/**
 * DAO para la pantalla Registros: lista TODAS las ordenes de analisis (no solo las ultimas N,
 * a diferencia de EscritorioDAO.listarUltimasOrdenes()).
 */
public class RegistroDAO {

    /**
     * SELECT base compartido por listarTodos(), listarPendientes() y listarPorPaciente(): trae
     * numero de orden, paciente, examen, fecha, cobertura, estado y prioridad del pedido. La
     * cobertura sale de `pagos.id_obra_social` del pedido puntual (no de la lista de obras
     * sociales del paciente), igual que en {@link EscritorioDAO}.
     */
    private static final String SELECT_BASE =
            "SELECT pa.id_pedido_analisis, pa.id_analisis_tipo, pe.numero_pedido, p.dni_paciente, " +
            "p.nya_paciente, at.nombre_analisis, " +
            "pe.fecha_pedido, pa.estado_analisis, pe.prioridad_pedido, " +
            "(SELECT os.nombre_obra_social FROM pagos pg " +
            "   JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social " +
            "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 AND pg.id_obra_social IS NOT NULL " +
            "   LIMIT 1) AS nombre_obra_social, " +
            "(SELECT COUNT(*) FROM envios e WHERE e.id_pedido = pe.id_pedido) AS cant_envios " +
            "FROM pedido_analisis pa " +
            "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido " +
            "JOIN pacientes p ON p.id_paciente = pe.id_paciente " +
            "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo ";

    public static List<OrdenResumen> listarTodos(Connection conexion) {
        return listar(conexion, SELECT_BASE + "ORDER BY pa.created_at DESC");
    }

    /**
     * Para la pantalla Registrar Resultados: solo las ordenes que todavia requieren trabajo
     * (estado_analisis pendiente o en_proceso), sumando tambien cualquier analisis de un pedido
     * marcado como prioridad_pedido = 'urgente' que no este completado ni cancelado, para que un
     * pedido urgente no se pierda de vista aunque su estado puntual sea otro.
     */
    public static List<OrdenResumen> listarPendientes(Connection conexion) {
        String sql = SELECT_BASE +
                "WHERE pa.estado_analisis IN ('pendiente', 'en_proceso', 'urgente') " +
                "OR (pe.prioridad_pedido = 'urgente' AND pa.estado_analisis NOT IN ('completado', 'cancelado')) " +
                "ORDER BY (CASE WHEN pa.estado_analisis = 'urgente' OR pe.prioridad_pedido = 'urgente' " +
                "THEN 0 ELSE 1 END), pa.created_at DESC";
        return listar(conexion, sql);
    }

    /**
     * Para el detalle de un paciente (vistas.pacientes.Vistadetallepaciente): todas sus ordenes,
     * de la mas reciente a la mas vieja.
     */
    public static List<OrdenResumen> listarPorPaciente(Connection conexion, int idPaciente) {
        String sql = SELECT_BASE + "WHERE p.id_paciente = ? ORDER BY pe.fecha_pedido DESC, pa.created_at DESC";
        List<OrdenResumen> ordenes = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ordenes.add(mapearOrden(rs));
                }
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar las ordenes del paciente", e);
        }

        return ordenes;
    }

    private static List<OrdenResumen> listar(Connection conexion, String sql) {
        List<OrdenResumen> ordenes = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ordenes.add(mapearOrden(rs));
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar los registros", e);
        }

        return ordenes;
    }

    /**
     * Arma un OrdenResumen a partir de la fila actual de un ResultSet de SELECT_BASE (usado
     * tanto por listar() como por listarPorPaciente()).
     */
    private static OrdenResumen mapearOrden(ResultSet rs) throws SQLException {
        OrdenResumen orden = new OrdenResumen();
        orden.setIdPedidoAnalisis(rs.getInt("id_pedido_analisis"));
        orden.setIdAnalisisTipo(rs.getInt("id_analisis_tipo"));
        orden.setNumeroOrden(rs.getString("numero_pedido"));
        orden.setDni(rs.getString("dni_paciente"));
        orden.setPaciente(rs.getString("nya_paciente"));
        orden.setExamen(rs.getString("nombre_analisis"));
        orden.setFecha(rs.getDate("fecha_pedido"));
        orden.setCobertura(rs.getString("nombre_obra_social") != null
                ? rs.getString("nombre_obra_social") : "Particular");

        String estadoAnalisis = rs.getString("estado_analisis");
        String prioridad = rs.getString("prioridad_pedido");
        boolean esUrgente = "urgente".equals(estadoAnalisis) || "urgente".equals(prioridad);
        orden.setEstado(EstadoAnalisisUtil.traducir(estadoAnalisis, rs.getInt("cant_envios"), esUrgente));

        return orden;
    }
}
