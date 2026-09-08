package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.DetalleOrden;
import modelo.EstadisticasEscritorio;
import modelo.OrdenResumen;

/**
 * DAO para la pantalla Escritorio: las 4 tarjetas resumen y la tabla "Últimas Órdenes".
 */
public class EscritorioDAO {

    /**
     * Cuenta los pedido_analisis del mes en curso agrupados por estado, y arma el objeto que
     * alimenta las 4 tarjetas (Total del mes / Emitidas / En proceso / Pendientes).
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
     * Trae las últimas órdenes (una fila por análisis pedido, no por pedido completo) para la
     * tabla del Escritorio, con el estado ya traducido al texto que se muestra en pantalla.
     * Además de lo que se ve en la fila, trae el DNI, la cobertura (nombre de la obra social o
     * "Particular") y los ids de pedido_analisis/analisis_tipo, para que el botón "Ver" de esa
     * fila pueda pedir después el detalle completo de la orden.
     */
    public static List<OrdenResumen> listarUltimasOrdenes(Connection conexion, int limite) {
        List<OrdenResumen> ordenes = new ArrayList<>();

        String sql = "SELECT pa.id_pedido_analisis, pa.id_analisis_tipo, pe.numero_pedido, " +
                "p.dni_paciente, p.nya_paciente, at.nombre_analisis, pe.fecha_pedido, " +
                "pa.estado_analisis, os.nombre_obra_social, " +
                "(SELECT COUNT(*) FROM envios e WHERE e.id_pedido = pe.id_pedido) AS cant_envios " +
                "FROM pedido_analisis pa " +
                "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido " +
                "JOIN pacientes p ON p.id_paciente = pe.id_paciente " +
                "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo " +
                "LEFT JOIN planes_obra_social plan ON plan.id_plan = p.id_plan " +
                "LEFT JOIN obras_sociales os ON os.id_obra_social = plan.id_obra_social " +
                "ORDER BY pa.created_at DESC " +
                "LIMIT ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    orden.setEstado(EstadoAnalisisUtil.traducir(rs.getString("estado_analisis"), rs.getInt("cant_envios")));
                    ordenes.add(orden);
                }
            }

        } catch (SQLException e) {
            Mensajes.error("Error al listar las últimas órdenes", e);
        }

        return ordenes;
    }

    /**
     * Trae el detalle completo de una orden puntual (un {@code pedido_analisis}) para la
     * pantalla "Detalle de Orden": datos del paciente, médico derivante (si el pedido tiene uno
     * cargado), examen, fecha y cobertura -esta última mostrando solo el nombre de la obra
     * social cuando corresponde, igual que en {@link RegistroDAO}, nunca el plan ni ningún dato
     * de unidad bioquímica-. Devuelve null si no existe una orden con ese id.
     */
    public static DetalleOrden buscarDetalleOrden(Connection conexion, int idPedidoAnalisis) {
        String sql = "SELECT pa.id_pedido_analisis, pa.id_analisis_tipo, pa.estado_analisis, " +
                "pe.numero_pedido, pe.fecha_pedido, " +
                "p.dni_paciente, p.nya_paciente, p.fecha_nacimiento, p.telefono_paciente, p.email_paciente, " +
                "at.nombre_analisis, os.nombre_obra_social, m.nombre_medico, " +
                "(SELECT COUNT(*) FROM envios e WHERE e.id_pedido = pe.id_pedido) AS cant_envios " +
                "FROM pedido_analisis pa " +
                "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido " +
                "JOIN pacientes p ON p.id_paciente = pe.id_paciente " +
                "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo " +
                "LEFT JOIN planes_obra_social plan ON plan.id_plan = p.id_plan " +
                "LEFT JOIN obras_sociales os ON os.id_obra_social = plan.id_obra_social " +
                "LEFT JOIN medicos m ON m.id_medico = pe.id_medico " +
                "WHERE pa.id_pedido_analisis = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedidoAnalisis);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                DetalleOrden detalle = new DetalleOrden();
                detalle.setIdPedidoAnalisis(rs.getInt("id_pedido_analisis"));
                detalle.setIdAnalisisTipo(rs.getInt("id_analisis_tipo"));
                detalle.setNumeroOrden(rs.getString("numero_pedido"));
                detalle.setDni(rs.getString("dni_paciente"));
                detalle.setPaciente(rs.getString("nya_paciente"));
                detalle.setEdad(calcularEdad(rs.getDate("fecha_nacimiento")));
                detalle.setTelefono(rs.getString("telefono_paciente"));
                detalle.setEmail(rs.getString("email_paciente"));
                detalle.setMedicoDerivante(rs.getString("nombre_medico"));
                detalle.setExamen(rs.getString("nombre_analisis"));
                detalle.setFecha(rs.getDate("fecha_pedido"));
                // El esquema actual no tiene una columna de observación en pedido_analisis/pedidos;
                // se deja sin dato en vez de inventar un valor (la vista muestra "-" en ese caso).
                detalle.setObservacion(null);
                detalle.setCobertura(rs.getString("nombre_obra_social") != null
                        ? rs.getString("nombre_obra_social") : "Particular");

                String estadoAnalisis = rs.getString("estado_analisis");
                detalle.setEstadoAnalisisRaw(estadoAnalisis);
                detalle.setEstado(EstadoAnalisisUtil.traducir(estadoAnalisis, rs.getInt("cant_envios")));

                return detalle;
            }

        } catch (SQLException e) {
            Mensajes.error("Error al buscar el detalle de la orden", e);
            return null;
        }
    }

    /**
     * Calcula la edad a partir de la fecha de nacimiento (misma lógica que
     * {@link modelo.Paciente#calcularEdad()}; se repite acá porque este método arma el detalle
     * directo desde el ResultSet, sin pasar por un objeto Paciente completo).
     */
    private static int calcularEdad(java.sql.Date fechaNacimiento) {
        if (fechaNacimiento == null) {
            return 0;
        }
        java.util.Calendar nacimiento = java.util.Calendar.getInstance();
        nacimiento.setTime(fechaNacimiento);
        java.util.Calendar hoy = java.util.Calendar.getInstance();

        int edad = hoy.get(java.util.Calendar.YEAR) - nacimiento.get(java.util.Calendar.YEAR);
        if (hoy.get(java.util.Calendar.DAY_OF_YEAR) < nacimiento.get(java.util.Calendar.DAY_OF_YEAR)) {
            edad--;
        }
        return edad;
    }
}
