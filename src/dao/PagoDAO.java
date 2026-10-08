package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.DatosCobertura;
import modelo.FilaPago;
import modelo.ResumenObraSocial;
import modelo.ResumenPagos;

/**
 * DAO para `pagos`: guarda la cobertura elegida en "Nuevo Análisis" (tipo de cobertura, obra
 * social, plan, número de afiliado, método de pago y -- solo para Mixto -- el monto que abona
 * el paciente en efectivo) para el pedido recién creado.
 *
 * <p>Hasta esta migración la tabla existía pero nada le escribía nunca: la cobertura se
 * imprimía en el comprobante y se perdía. Ver {@code sql/2026-09-23_cobertura_pedido.sql}.</p>
 */
public class PagoDAO {

    public static boolean crearPago(Connection con, int idPedido, DatosCobertura cobertura) {
        String sql = "INSERT INTO pagos "
                + "(id_pedido, id_obra_social, tipo_cobertura, metodo_pago, plan_obra_social, "
                + "nro_afiliado, monto_efectivo, monto_pago, anulado_pago, estado_cobro_os) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);

            if (cobertura.getObraSocial() != null) {
                ps.setInt(2, cobertura.getObraSocial().getIdObraSocial());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setString(3, cobertura.getTipoCobertura());
            ps.setString(4, vacioANull(cobertura.getMetodoPago()));
            ps.setString(5, vacioANull(cobertura.getPlan()));
            ps.setString(6, vacioANull(cobertura.getNroAfiliado()));

            if (cobertura.getMontoEfectivo() != null) {
                ps.setBigDecimal(7, cobertura.getMontoEfectivo());
            } else {
                ps.setNull(7, Types.DECIMAL);
            }

            // monto_pago guarda una foto del total del pedido en el momento en que se registró
            // esta cobertura -- a diferencia de pedidos.total_pedido, que puede seguir cambiando
            // después (si se agrega o saca un examen), esto queda fijo para que quede registro de
            // cuánto representaba el pedido cuando se cobró/cubrió.
            ps.setBigDecimal(8, cobertura.getTotal() != null ? cobertura.getTotal() : BigDecimal.ZERO);

            // estado_cobro_os arranca en "pendiente" apenas hay obra social de por medio (Obra
            // Social o Mixto) -- un Particular nunca tiene nada que cobrarle a una obra social, así
            // que queda en null (ver el javadoc de la columna en la migración
            // 2026-10-01_estado_cobro_obra_social).
            ps.setString(9, cobertura.getObraSocial() != null ? "pendiente" : null);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al guardar la cobertura del pedido", e);
            return false;
        }
    }

    private static String vacioANull(String texto) {
        return (texto == null || texto.trim().isEmpty()) ? null : texto.trim();
    }

    /**
     * Anula el pago/cobertura activo de un pedido (borrado lógico, igual que el resto del
     * sistema: nunca se hace DELETE -- se marca {@code anulado_pago = 1} y se guarda el motivo).
     * Una vez anulado, el pedido vuelve a aparecer como "PARTICULAR" sin cobertura en
     * {@link #cargarDatosPagos} y en las demás pantallas que filtran por
     * {@code anulado_pago = 0} ({@code EscritorioDAO}, {@code RegistroDAO}, {@code
     * EstadisticasDAO}), tal cual un pedido que nunca tuvo cobertura cargada.
     *
     * <p>No existe (todavía) una pantalla para volver a cargar una cobertura nueva después de
     * anular la anterior -- esta es la operación de "deshacer una cobertura mal cargada", no un
     * reemplazo en un solo paso.</p>
     *
     * @return {@code true} si había un pago activo y se anuló, {@code false} si no había ninguno
     * para anular (el pedido nunca tuvo cobertura, o ya estaba anulada) o si falló la consulta.
     */
    public static boolean anularPago(Connection con, int idPedido, String motivo) {
        String sql = "UPDATE pagos SET anulado_pago = 1, motivo_anulacion = ? "
                + "WHERE id_pedido = ? AND anulado_pago = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, vacioANull(motivo));
            ps.setInt(2, idPedido);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "ANULACION", "pagos", idPedido,
                        "Pago anulado del pedido #" + idPedido
                                + (motivo != null && !motivo.trim().isEmpty() ? ": " + motivo : ""));
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al anular el pago", e);
            return false;
        }
    }

    /**
     * Todo lo que necesita la pantalla Pagos, en una sola consulta: una fila por pedido del mes
     * en curso (por fecha_pedido), con sus exámenes concatenados y su cobertura -- a partir de
     * ahí se arman en Java las 4 tarjetas del Resumen y el desglose por obra social de "Cobros a
     * Obras Sociales", para no repetir la misma cuenta en SQL tres veces.
     *
     * <p>Un pedido sin fila en `pagos` (por ejemplo, uno cargado antes de que existiera este
     * guardado) se trata como PARTICULAR, mismo criterio que {@code EscritorioDAO}.</p>
     */
    public static class DatosPagos {
        public final ResumenPagos resumen = new ResumenPagos();
        public final List<FilaPago> ordenes = new ArrayList<>();
        public final List<ResumenObraSocial> porObraSocial = new ArrayList<>();
    }

    public static DatosPagos cargarDatosPagos(Connection con) {
        DatosPagos datos = new DatosPagos();

        String sql = "SELECT pe.id_pedido, pe.numero_pedido, p.nya_paciente, pe.fecha_pedido, pe.total_pedido, "
                + "GROUP_CONCAT(DISTINCT at.nombre_analisis ORDER BY at.nombre_analisis SEPARATOR ', ') AS examenes, "
                + "(SELECT pg.tipo_cobertura FROM pagos pg "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 LIMIT 1) AS tipo_cobertura, "
                + "(SELECT pg.monto_efectivo FROM pagos pg "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 LIMIT 1) AS monto_efectivo, "
                + "(SELECT os.nombre_obra_social FROM pagos pg "
                + "   JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 AND pg.id_obra_social IS NOT NULL "
                + "   LIMIT 1) AS nombre_obra_social "
                + "FROM pedidos pe "
                + "JOIN pacientes p ON p.id_paciente = pe.id_paciente "
                + "LEFT JOIN pedido_analisis pa ON pa.id_pedido = pe.id_pedido "
                + "LEFT JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE YEAR(pe.fecha_pedido) = YEAR(CURDATE()) AND MONTH(pe.fecha_pedido) = MONTH(CURDATE()) "
                + "GROUP BY pe.id_pedido, pe.numero_pedido, p.nya_paciente, pe.fecha_pedido, pe.total_pedido "
                + "ORDER BY pe.fecha_pedido DESC, pe.id_pedido DESC";

        SimpleDateFormat formatoDia = new SimpleDateFormat("yyyy-MM-dd");
        String hoy = formatoDia.format(new Date());

        Map<String, ResumenObraSocial> porObraSocial = new LinkedHashMap<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total_pedido");
                if (total == null) {
                    total = BigDecimal.ZERO;
                }

                String tipoCobertura = rs.getString("tipo_cobertura");
                if (tipoCobertura == null || tipoCobertura.trim().isEmpty()) {
                    tipoCobertura = "PARTICULAR";
                }
                BigDecimal montoEfectivo = rs.getBigDecimal("monto_efectivo");
                String nombreObraSocial = rs.getString("nombre_obra_social");

                BigDecimal montoOs;
                BigDecimal montoPac;
                switch (tipoCobertura) {
                    case "OBRA_SOCIAL":
                        montoOs = total;
                        montoPac = BigDecimal.ZERO;
                        break;
                    case "MIXTO":
                        montoPac = montoEfectivo != null ? montoEfectivo : BigDecimal.ZERO;
                        montoOs = total.subtract(montoPac);
                        break;
                    default: // PARTICULAR
                        montoOs = BigDecimal.ZERO;
                        montoPac = total;
                        break;
                }

                java.sql.Date fechaPedido = rs.getDate("fecha_pedido");

                FilaPago fila = new FilaPago();
                fila.setIdPedido(rs.getInt("id_pedido"));
                fila.setNumeroOrden(rs.getString("numero_pedido"));
                fila.setPaciente(rs.getString("nya_paciente"));
                fila.setExamenes(rs.getString("examenes"));
                fila.setFecha(fechaPedido);
                fila.setTipoCobertura(tipoCobertura);
                fila.setNombreObraSocial(nombreObraSocial);
                fila.setTotal(total);
                fila.setMontoOs(montoOs);
                fila.setMontoPac(montoPac);
                datos.ordenes.add(fila);

                datos.resumen.setTotalMes(datos.resumen.getTotalMes().add(total));
                datos.resumen.setCobradoParticular(datos.resumen.getCobradoParticular().add(montoPac));
                datos.resumen.setPendienteOs(datos.resumen.getPendienteOs().add(montoOs));
                if (fechaPedido != null && hoy.equals(formatoDia.format(fechaPedido))) {
                    datos.resumen.setTotalHoy(datos.resumen.getTotalHoy().add(total));
                }

                if (nombreObraSocial != null) {
                    ResumenObraSocial resumenOs = porObraSocial.computeIfAbsent(nombreObraSocial, nombre -> {
                        ResumenObraSocial nuevo = new ResumenObraSocial();
                        nuevo.setNombreObraSocial(nombre);
                        return nuevo;
                    });
                    resumenOs.setCantidadOrdenes(resumenOs.getCantidadOrdenes() + 1);
                    resumenOs.setTotalAdeudado(resumenOs.getTotalAdeudado().add(montoOs));
                    if (resumenOs.getUltimaOrden() == null || (fechaPedido != null && fechaPedido.after(resumenOs.getUltimaOrden()))) {
                        resumenOs.setUltimaOrden(fechaPedido);
                    }
                }
            }

        } catch (SQLException e) {
            Mensajes.error("Error al cargar los datos de pagos", e);
        }

        List<ResumenObraSocial> lista = new ArrayList<>(porObraSocial.values());
        lista.sort((a, b) -> b.getTotalAdeudado().compareTo(a.getTotalAdeudado()));
        datos.porObraSocial.addAll(lista);

        return datos;
    }

    /**
     * "pendiente" | "en_gestion" | "cobrado" -- únicos valores válidos para
     * {@code pagos.estado_cobro_os} (ver la migración 2026-10-01_estado_cobro_obra_social).
     */
    private static final java.util.Set<String> ESTADOS_COBRO_VALIDOS
            = new java.util.HashSet<>(java.util.Arrays.asList("pendiente", "en_gestion", "cobrado"));

    /**
     * Pedidos con cobertura de Obra Social o Mixta, para la pestaña "Cobros a Obras Sociales" de
     * Pagos. A diferencia de {@link #cargarDatosPagos} (que sólo mira el mes en curso), esto no
     * tiene límite de fecha: una deuda de un mes anterior que todavía no se cobró sigue
     * apareciendo acá, en vez de desaparecer de la vista sólo porque cambió el mes.
     *
     * @param incluirCobrados si es {@code false} (el caso normal, para la vista de "lo que falta
     * gestionar"), no trae los pedidos ya marcados "cobrado"; si es {@code true}, trae todo
     * (incluido el historial de cobros ya resueltos).
     */
    public static List<FilaPago> cargarCobrosObraSocial(Connection con, boolean incluirCobrados) {
        List<FilaPago> filas = new ArrayList<>();

        String sql = "SELECT pe.id_pedido, pe.numero_pedido, p.nya_paciente, pe.fecha_pedido, pe.total_pedido, "
                + "GROUP_CONCAT(DISTINCT at.nombre_analisis ORDER BY at.nombre_analisis SEPARATOR ', ') AS examenes, "
                + "pg.tipo_cobertura, pg.monto_efectivo, os.nombre_obra_social, "
                + "pg.estado_cobro_os, pg.fecha_cobro_os "
                + "FROM pagos pg "
                + "JOIN pedidos pe ON pe.id_pedido = pg.id_pedido "
                + "JOIN pacientes p ON p.id_paciente = pe.id_paciente "
                + "JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social "
                + "LEFT JOIN pedido_analisis pa ON pa.id_pedido = pe.id_pedido "
                + "LEFT JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pg.anulado_pago = 0 AND pg.id_obra_social IS NOT NULL "
                + (incluirCobrados ? "" : "AND (pg.estado_cobro_os IS NULL OR pg.estado_cobro_os <> 'cobrado') ")
                + "GROUP BY pe.id_pedido, pe.numero_pedido, p.nya_paciente, pe.fecha_pedido, pe.total_pedido, "
                + "pg.tipo_cobertura, pg.monto_efectivo, os.nombre_obra_social, pg.estado_cobro_os, pg.fecha_cobro_os "
                + "ORDER BY pe.fecha_pedido ASC, pe.id_pedido ASC";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total_pedido");
                if (total == null) {
                    total = BigDecimal.ZERO;
                }
                String tipoCobertura = rs.getString("tipo_cobertura");
                BigDecimal montoEfectivo = rs.getBigDecimal("monto_efectivo");

                BigDecimal montoOs;
                BigDecimal montoPac;
                if ("MIXTO".equals(tipoCobertura)) {
                    montoPac = montoEfectivo != null ? montoEfectivo : BigDecimal.ZERO;
                    montoOs = total.subtract(montoPac);
                } else { // OBRA_SOCIAL (es lo único más que puede haber, por el WHERE id_obra_social IS NOT NULL)
                    montoOs = total;
                    montoPac = BigDecimal.ZERO;
                }

                FilaPago fila = new FilaPago();
                fila.setIdPedido(rs.getInt("id_pedido"));
                fila.setNumeroOrden(rs.getString("numero_pedido"));
                fila.setPaciente(rs.getString("nya_paciente"));
                fila.setExamenes(rs.getString("examenes"));
                fila.setFecha(rs.getDate("fecha_pedido"));
                fila.setTipoCobertura(tipoCobertura);
                fila.setNombreObraSocial(rs.getString("nombre_obra_social"));
                fila.setTotal(total);
                fila.setMontoOs(montoOs);
                fila.setMontoPac(montoPac);
                String estado = rs.getString("estado_cobro_os");
                fila.setEstadoCobroOs(estado != null ? estado : "pendiente");
                fila.setFechaCobroOs(rs.getDate("fecha_cobro_os"));
                filas.add(fila);
            }

        } catch (SQLException e) {
            Mensajes.error("Error al cargar los cobros a obras sociales", e);
        }

        return filas;
    }

    /**
     * Cambia el estado de cobro a obra social de un pedido puntual ("pendiente" -&gt;
     * "en_gestion" -&gt; "cobrado", o para atrás si fue un error de tipeo). Cuando el nuevo
     * estado es "cobrado" guarda la fecha de hoy en {@code fecha_cobro_os}; en cualquier otro
     * estado la deja en null, para que no quede una fecha de cobro mintiendo si se destilda un
     * cobro marcado por error.
     */
    public static boolean actualizarEstadoCobro(Connection con, int idPedido, String nuevoEstado) {
        if (!ESTADOS_COBRO_VALIDOS.contains(nuevoEstado)) {
            return false;
        }
        String sql = "UPDATE pagos SET estado_cobro_os = ?, "
                + "fecha_cobro_os = " + ("cobrado".equals(nuevoEstado) ? "CURDATE()" : "NULL") + " "
                + "WHERE id_pedido = ? AND anulado_pago = 0 AND id_obra_social IS NOT NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idPedido);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                AuditoriaDAO.registrar(con, modelo.Sesion.idUsuario, "CAMBIO_ESTADO", "pagos", idPedido,
                        "Estado de cobro a obra social del pedido #" + idPedido + " -> " + nuevoEstado);
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el estado de cobro", e);
            return false;
        }
    }

    /**
     * Igual que {@link #actualizarEstadoCobro}, pero para varios pedidos a la vez ("Marcar
     * seleccionadas como cobradas" de la pestaña Cobros a Obras Sociales). Devuelve cuántos se
     * actualizaron realmente, por si alguno ya no estaba disponible (anulado entre que se cargó
     * la pantalla y se tildó).
     */
    public static int actualizarEstadoCobroLote(Connection con, List<Integer> idsPedido, String nuevoEstado) {
        int actualizados = 0;
        for (int idPedido : idsPedido) {
            if (actualizarEstadoCobro(con, idPedido, nuevoEstado)) {
                actualizados++;
            }
        }
        return actualizados;
    }
}
