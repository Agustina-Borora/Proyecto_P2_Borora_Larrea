package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la notificación por email al técnico cuando un análisis queda completado (Figma:
 * tarjeta "Notificaciones del sistema" en Configuración &gt; Sistema, toggle "Notificar al
 * técnico cuando un análisis queda listo") -- ver {@code controlador.ResultadosController}, que
 * es quien dispara el envío.
 */
public class NotificacionDAO {

    private NotificacionDAO() {
    }

    /**
     * Emails de los técnicos activos (rol "Tecnico", activo_usuario = 1) a quienes avisarles. Se
     * dejan afuera los que no tienen un email cargado -- no tiene sentido intentar mandarles nada.
     */
    public static List<String> listarEmailsTecnicos(Connection con) {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT u.email_usuario FROM usuarios u "
                + "INNER JOIN roles r ON u.id_rol = r.id_rol "
                + "WHERE r.nombre_rol = 'Tecnico' AND u.activo_usuario = 1 "
                + "AND u.email_usuario IS NOT NULL AND u.email_usuario <> ''";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(rs.getString("email_usuario"));
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar los emails de los técnicos", e);
        }
        return emails;
    }

    /**
     * Datos de la orden/examen/paciente para armar el cuerpo del email de notificación.
     */
    public static final class InfoParaNotificar {
        private final String numeroOrden;
        private final String paciente;
        private final String examen;

        public InfoParaNotificar(String numeroOrden, String paciente, String examen) {
            this.numeroOrden = numeroOrden;
            this.paciente = paciente;
            this.examen = examen;
        }

        public String getNumeroOrden() {
            return numeroOrden;
        }

        public String getPaciente() {
            return paciente;
        }

        public String getExamen() {
            return examen;
        }
    }

    public static InfoParaNotificar buscarInfoParaNotificar(Connection con, int idPedidoAnalisis) {
        String sql = "SELECT pe.numero_pedido, p.nya_paciente, at.nombre_analisis "
                + "FROM pedido_analisis pa "
                + "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido "
                + "JOIN pacientes p ON p.id_paciente = pe.id_paciente "
                + "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pa.id_pedido_analisis = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedidoAnalisis);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new InfoParaNotificar(
                        rs.getString("numero_pedido"),
                        rs.getString("nya_paciente"),
                        rs.getString("nombre_analisis"));
            }
        } catch (SQLException e) {
            Mensajes.error("Error al buscar los datos para la notificación", e);
            return null;
        }
    }
}
