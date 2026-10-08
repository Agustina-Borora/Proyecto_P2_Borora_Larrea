package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO para la tabla `envios` -- registra cada vez que se manda de verdad un resultado por email o
 * WhatsApp desde "Enviar Resultados" (Registros). Antes de esta funcionalidad ningún DAO
 * escribía en esta tabla (solo se leía con COUNT(*) para el badge "Completado · Enviado"), así
 * que ese estado nunca reflejaba la realidad -- ver sql/2026-09-24_envios_resultados.sql, que
 * agrega las columnas `canal`, `destino` y `fecha_envio` que este método necesita.
 */
public class EnvioDAO {

    /**
     * Registra un envío. Se llama una vez por canal realmente usado (hasta dos veces por click en
     * "Enviar Resultados": una para email, otra para WhatsApp, si el paciente tiene los dos datos
     * cargados) -- nunca antes de intentar el envío real, para no mentir en el historial.
     *
     * @param canal     "email" o "whatsapp".
     * @param destino   el email o el celular usado en ese envío puntual (queda de constancia aunque
     *                  después se edite el dato del paciente).
     * @param idUsuario quién lo mandó ({@link modelo.Sesion#idUsuario}) -- misma columna
     *                  {@code id_usuario} que ya usa {@code pedidos} para "quién generó la orden",
     *                  que en tu base es obligatoria (sin ella, el INSERT choca con "Field
     *                  'id_usuario' doesn't have a default value").
     */
    /**
     * Registra un envío dejando constancia de si fue con el PDF del informe adjunto y cuál
     * archivo ({@code envios.con_pdf} y {@code envios.archivo_pdf}, que agrega
     * {@code sql/2026-10-08_informes_pdf.sql}). Si todavía no se corrió ese script, guarda el envío
     * igual sin esas dos columnas -- nunca se pierde el registro del envío por eso.
     *
     * <p>No muestra carteles (se llama desde un hilo en segundo plano): si falla, tira la
     * excepción.</p>
     */
    public static void registrarConPdf(Connection conexion, int idPedido, String canal, String destino, int idUsuario,
            boolean conPdf, String archivoPdf) throws SQLException {
        String sql = "INSERT INTO envios (id_pedido, canal, destino, fecha_envio, id_usuario, con_pdf, archivo_pdf) "
                + "VALUES (?, ?, ?, NOW(), ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.setString(2, canal);
            ps.setString(3, destino);
            ps.setInt(4, idUsuario);
            ps.setBoolean(5, conPdf);
            ps.setString(6, conPdf ? archivoPdf : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!InformeDAO.faltaScript(e)) {
                throw e;
            }
            String sqlViejo = "INSERT INTO envios (id_pedido, canal, destino, fecha_envio, id_usuario) VALUES (?, ?, ?, NOW(), ?)";
            try (PreparedStatement ps = conexion.prepareStatement(sqlViejo)) {
                ps.setInt(1, idPedido);
                ps.setString(2, canal);
                ps.setString(3, destino);
                ps.setInt(4, idUsuario);
                ps.executeUpdate();
            }
        }
    }

    public static boolean registrar(Connection conexion, int idPedido, String canal, String destino, int idUsuario) {
        String sql = "INSERT INTO envios (id_pedido, canal, destino, fecha_envio, id_usuario) VALUES (?, ?, ?, NOW(), ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.setString(2, canal);
            ps.setString(3, destino);
            ps.setInt(4, idUsuario);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            Mensajes.error("Error al registrar el envío", e);
            return false;
        }
    }
}
