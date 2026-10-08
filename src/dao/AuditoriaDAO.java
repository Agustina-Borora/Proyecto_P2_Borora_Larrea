package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import modelo.RegistroAuditoria;

/**
 * DAO para `auditoria`: el registro de movimientos importantes del sistema (altas, bajas y
 * cambios de pacientes, usuarios, pagos, obras sociales y catálogo de exámenes, más los inicios
 * de sesión) que pidió la clienta para poder ver "quién hizo qué, y cuándo" sin tener que confiar
 * a ciegas.
 *
 * <p>A propósito, esto NO registra absolutamente todo lo que pasa en el sistema -- eso incluiría,
 * por ejemplo, cada resultado de análisis cargado uno por uno, que son decenas por pedido y
 * convertirían la pantalla de Auditoría en una lista inmanejable de ruido en vez de una que sirva
 * para auditar. Queda registrado:</p>
 * <ul>
 * <li>Pacientes: alta, edición y baja (lógica).</li>
 * <li>Usuarios: alta, edición y baja (lógica).</li>
 * <li>Pagos: anulación de un pago y cambio de estado de cobro a obra social.</li>
 * <li>Obras Sociales: alta, edición y baja (lógica).</li>
 * <li>Catálogo de Exámenes: alta, edición y cambio de estado (activo/inactivo) de un examen --
 * no cada analito/parámetro suyo, mismo criterio que arriba.</li>
 * <li>Inicio de sesión: exitoso y fallido.</li>
 * </ul>
 *
 * <p>{@link #registrar} nunca debe interrumpir la operación real que la originó: si guardar la
 * fila de auditoría falla (por ejemplo, porque todavía no se corrió la migración
 * sql/2026-10-01_auditoria.sql), el error se imprime en la consola y nada más -- ni un
 * {@code throw}, ni un cartel con {@link Mensajes#error} (eso sí cortaría el flujo con un popup
 * confuso en medio de una acción que, para el usuario, salió bien). Mismo criterio que ya usa
 * {@code controlador.ConexionUtil#cerrar} para un error al cerrar la conexión.</p>
 */
public final class AuditoriaDAO {

    /** Cuántas filas trae como máximo {@link #listarRecientes} -- ver su javadoc. */
    private static final int LIMITE_LISTADO = 500;

    private AuditoriaDAO() {
    }

    /**
     * Guarda una fila de auditoría. {@code idUsuario} puede ser null (ej. intento de inicio de
     * sesión fallido, donde no se sabe con certeza quién tipeó mal) y {@code idEntidad} también
     * (una acción que no apunta a un id puntual). Devuelve true/false sólo a fines informativos --
     * ningún llamador debería cortar su propio flujo según este resultado, ver el javadoc de la
     * clase.
     */
    public static boolean registrar(Connection con, Integer idUsuario, String accion, String entidad,
            Integer idEntidad, String detalle) {
        String sql = "INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad, detalle) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario == null || idUsuario <= 0) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, idUsuario);
            }
            ps.setString(2, accion);
            ps.setString(3, entidad);
            if (idEntidad == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, idEntidad);
            }
            ps.setString(5, detalle);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // A propósito sin Mensajes.error acá -- ver el javadoc de la clase.
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Trae las últimas {@value #LIMITE_LISTADO} filas de auditoría (la más reciente primero), con
     * el nombre de quien hizo cada una ya resuelto. El filtrado por texto/acción/entidad que hace
     * la pantalla de Auditoría se resuelve en Java sobre esta lista, mismo criterio que ya usan
     * Usuarios/Pagos/Pacientes -- no hace falta volver a consultar la base en cada tecla.
     */
    public static List<RegistroAuditoria> listarRecientes(Connection con) {
        List<RegistroAuditoria> registros = new ArrayList<>();
        String sql = "SELECT a.id_auditoria, a.id_usuario, u.nombre_usuario, u.apellido_usuario, "
                + "a.accion, a.entidad, a.id_entidad, a.detalle, a.fecha_hora "
                + "FROM auditoria a LEFT JOIN usuarios u ON u.id_usuario = a.id_usuario "
                + "ORDER BY a.fecha_hora DESC, a.id_auditoria DESC LIMIT " + LIMITE_LISTADO;

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                registros.add(mapear(rs));
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar la auditoría", e);
        }

        return registros;
    }

    private static RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setIdAuditoria(rs.getInt("id_auditoria"));

        int idUsuario = rs.getInt("id_usuario");
        registro.setIdUsuario(rs.wasNull() ? null : idUsuario);

        String nombre = rs.getString("nombre_usuario");
        String apellido = rs.getString("apellido_usuario");
        registro.setNombreUsuario(nombre == null && apellido == null ? null
                : ((apellido == null ? "" : apellido + " ") + (nombre == null ? "" : nombre)).trim());

        registro.setAccion(rs.getString("accion"));
        registro.setEntidad(rs.getString("entidad"));

        int idEntidad = rs.getInt("id_entidad");
        registro.setIdEntidad(rs.wasNull() ? null : idEntidad);

        registro.setDetalle(rs.getString("detalle"));

        Timestamp fechaHora = rs.getTimestamp("fecha_hora");
        registro.setFechaHora(fechaHora == null ? null : new java.util.Date(fechaHora.getTime()));

        return registro;
    }
}
