package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import modelo.ConteoExamen;
import modelo.EstadisticasPeriodo;
import modelo.PacienteFrecuente;
import modelo.PuntoMensual;
import modelo.ResumenObraSocial;

/**
 * DAO para la pantalla Estadísticas (Mes 6: Paneles de consulta) -- antes un placeholder vacío
 * ({@code vistas.formulariosPrincipales.Estadisticas}, con un JLabel "Form 10" y nada más). Trae
 * las 4 tarjetas de resumen y los 4 paneles (Análisis por mes, Exámenes más solicitados, Órdenes
 * por Obra Social, Pacientes frecuentes) para el período elegido (Este mes / Trimestre / Este
 * año), siguiendo el diseño de Figma (nodo 246:7222).
 *
 * <p>Reutiliza el mismo criterio de cobertura que {@link PagoDAO#cargarDatosPagos}: un pedido sin
 * fila en {@code pagos} se trata como PARTICULAR, y "Mixto" reparte entre lo que paga el paciente
 * (efectivo) y lo que queda a cargo de la obra social.</p>
 */
public class EstadisticasDAO {

    private static final String[] MESES_CORTOS = {
        "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    /**
     * @param periodo "mes" (mes en curso), "trimestre" (últimos 3 meses) o "anio" (año en curso,
     * cualquier otro valor se trata igual que "anio" por las dudas).
     */
    public static EstadisticasPeriodo obtener(Connection con, String periodo) {
        EstadisticasPeriodo datos = new EstadisticasPeriodo();
        Date desde = calcularDesde(periodo);

        cargarPedidosYCobertura(con, desde, datos);
        datos.setAnalisisRealizados(contarAnalisisRealizados(con, desde));
        cargarExamenesMasSolicitados(con, desde, datos);
        cargarPacientesFrecuentes(con, desde, datos);
        cargarTendenciaMensual(con, datos);

        return datos;
    }

    private static Date calcularDesde(String periodo) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if ("trimestre".equals(periodo)) {
            cal.add(Calendar.MONTH, -3);
        } else if ("anio".equals(periodo)) {
            cal.set(Calendar.DAY_OF_YEAR, 1);
        } else { // "mes" (y cualquier valor inesperado cae acá, el más chico de los tres)
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return new Date(cal.getTimeInMillis());
    }

    /**
     * Tarjetas "Ingreso total" y "Por cobrar a OS", "Pacientes únicos" y el desglose "Órdenes por
     * Obra Social" -- todo sale de una sola pasada por {@code pedidos} (grano: un pedido, no un
     * análisis), igual que {@link PagoDAO#cargarDatosPagos} pero filtrado por el período elegido
     * en vez de fijo al mes en curso.
     */
    private static void cargarPedidosYCobertura(Connection con, Date desde, EstadisticasPeriodo datos) {
        String sql = "SELECT pe.id_paciente, pe.fecha_pedido, pe.total_pedido, "
                + "(SELECT pg.tipo_cobertura FROM pagos pg "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 LIMIT 1) AS tipo_cobertura, "
                + "(SELECT pg.monto_efectivo FROM pagos pg "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 LIMIT 1) AS monto_efectivo, "
                + "(SELECT os.nombre_obra_social FROM pagos pg "
                + "   JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 AND pg.id_obra_social IS NOT NULL "
                + "   LIMIT 1) AS nombre_obra_social "
                + "FROM pedidos pe "
                + "WHERE pe.fecha_pedido >= ?";

        Map<String, ResumenObraSocial> porObraSocial = new LinkedHashMap<>();
        java.util.Set<Integer> pacientesUnicos = new java.util.HashSet<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, desde);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pacientesUnicos.add(rs.getInt("id_paciente"));

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
                    switch (tipoCobertura) {
                        case "OBRA_SOCIAL":
                            montoOs = total;
                            break;
                        case "MIXTO":
                            BigDecimal montoPac = montoEfectivo != null ? montoEfectivo : BigDecimal.ZERO;
                            montoOs = total.subtract(montoPac);
                            break;
                        default: // PARTICULAR
                            montoOs = BigDecimal.ZERO;
                            break;
                    }

                    datos.setIngresoTotal(datos.getIngresoTotal().add(total));
                    datos.setPorCobrarOs(datos.getPorCobrarOs().add(montoOs));

                    if (nombreObraSocial != null) {
                        java.sql.Date fechaPedido = rs.getDate("fecha_pedido");
                        ResumenObraSocial resumenOs = porObraSocial.computeIfAbsent(nombreObraSocial, nombre -> {
                            ResumenObraSocial nuevo = new ResumenObraSocial();
                            nuevo.setNombreObraSocial(nombre);
                            return nuevo;
                        });
                        resumenOs.setCantidadOrdenes(resumenOs.getCantidadOrdenes() + 1);
                        resumenOs.setTotalAdeudado(resumenOs.getTotalAdeudado().add(montoOs));
                        if (resumenOs.getUltimaOrden() == null
                                || (fechaPedido != null && fechaPedido.after(resumenOs.getUltimaOrden()))) {
                            resumenOs.setUltimaOrden(fechaPedido);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al calcular los cobros por período", e);
        }

        datos.setPacientesUnicos(pacientesUnicos.size());
        java.util.List<ResumenObraSocial> lista = new java.util.ArrayList<>(porObraSocial.values());
        lista.sort((a, b) -> b.getTotalAdeudado().compareTo(a.getTotalAdeudado()));
        datos.getOrdenesPorObraSocial().addAll(lista);
    }

    /** Tarjeta "Análisis realizados": cuántos {@code pedido_analisis} se pidieron en el período (sin contar los cancelados). */
    private static int contarAnalisisRealizados(Connection con, Date desde) {
        String sql = "SELECT COUNT(*) AS cantidad FROM pedido_analisis pa "
                + "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido "
                + "WHERE pe.fecha_pedido >= ? AND pa.estado_analisis <> 'cancelado'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cantidad") : 0;
            }
        } catch (SQLException e) {
            Mensajes.error("Error al contar los análisis realizados", e);
            return 0;
        }
    }

    /** Panel "Exámenes más solicitados": el top 6 por cantidad de pedidos en el período. */
    private static void cargarExamenesMasSolicitados(Connection con, Date desde, EstadisticasPeriodo datos) {
        String sql = "SELECT at.nombre_analisis, COUNT(*) AS cantidad "
                + "FROM pedido_analisis pa "
                + "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido "
                + "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pe.fecha_pedido >= ? AND pa.estado_analisis <> 'cancelado' "
                + "GROUP BY at.nombre_analisis "
                + "ORDER BY cantidad DESC "
                + "LIMIT 6";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConteoExamen fila = new ConteoExamen();
                    fila.setNombreExamen(rs.getString("nombre_analisis"));
                    fila.setCantidad(rs.getInt("cantidad"));
                    datos.getExamenesMasSolicitados().add(fila);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los exámenes más solicitados", e);
        }
    }

    /** Panel "Pacientes frecuentes": pacientes con más de una visita en el período, con su último estudio. */
    private static void cargarPacientesFrecuentes(Connection con, Date desde, EstadisticasPeriodo datos) {
        String sql = "SELECT p.nya_paciente, COUNT(DISTINCT pe.id_pedido) AS visitas, MAX(pe.fecha_pedido) AS ultima_fecha, "
                + "(SELECT at2.nombre_analisis FROM pedido_analisis pa2 "
                + "   JOIN pedidos pe2 ON pe2.id_pedido = pa2.id_pedido "
                + "   JOIN analisis_tipos at2 ON at2.id_analisis_tipo = pa2.id_analisis_tipo "
                + "   WHERE pe2.id_paciente = p.id_paciente "
                + "   ORDER BY pe2.fecha_pedido DESC, pa2.created_at DESC LIMIT 1) AS ultimo_examen "
                + "FROM pedidos pe "
                + "JOIN pacientes p ON p.id_paciente = pe.id_paciente "
                + "WHERE pe.fecha_pedido >= ? "
                + "GROUP BY p.id_paciente, p.nya_paciente "
                + "HAVING COUNT(DISTINCT pe.id_pedido) > 1 "
                + "ORDER BY visitas DESC, ultima_fecha DESC "
                + "LIMIT 5";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PacienteFrecuente fila = new PacienteFrecuente();
                    fila.setNombrePaciente(rs.getString("nya_paciente"));
                    fila.setVisitas(rs.getInt("visitas"));
                    fila.setUltimoEstudio(rs.getString("ultimo_examen"));
                    fila.setFechaUltimoEstudio(rs.getDate("ultima_fecha"));
                    datos.getPacientesFrecuentes().add(fila);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los pacientes frecuentes", e);
        }
    }

    /**
     * Gráfico "Análisis por mes": siempre los últimos 6 meses (no cambia con el período elegido
     * arriba, para que la tendencia se pueda comparar mes a mes aunque se esté mirando "Este
     * mes"), separado en Particular vs Obra Social/Mixto.
     */
    private static void cargarTendenciaMensual(Connection con, EstadisticasPeriodo datos) {
        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        inicio.add(Calendar.MONTH, -5);
        Date desde = new Date(inicio.getTimeInMillis());

        Map<String, PuntoMensual> porMes = new LinkedHashMap<>();
        Calendar cursor = (Calendar) inicio.clone();
        for (int i = 0; i < 6; i++) {
            String clave = cursor.get(Calendar.YEAR) + "-" + cursor.get(Calendar.MONTH);
            PuntoMensual punto = new PuntoMensual();
            punto.setMes(MESES_CORTOS[cursor.get(Calendar.MONTH)]);
            porMes.put(clave, punto);
            cursor.add(Calendar.MONTH, 1);
        }

        String sql = "SELECT YEAR(pe.fecha_pedido) AS anio, MONTH(pe.fecha_pedido) AS mes, "
                + "(SELECT pg.tipo_cobertura FROM pagos pg "
                + "   WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 LIMIT 1) AS tipo_cobertura, "
                + "COUNT(*) AS cantidad "
                + "FROM pedido_analisis pa "
                + "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido "
                + "WHERE pe.fecha_pedido >= ? AND pa.estado_analisis <> 'cancelado' "
                + "GROUP BY YEAR(pe.fecha_pedido), MONTH(pe.fecha_pedido), tipo_cobertura";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String clave = rs.getInt("anio") + "-" + (rs.getInt("mes") - 1);
                    PuntoMensual punto = porMes.get(clave);
                    if (punto == null) {
                        continue; // fuera de la ventana de 6 meses (no debería pasar, por las dudas)
                    }
                    String tipoCobertura = rs.getString("tipo_cobertura");
                    int cantidad = rs.getInt("cantidad");
                    if (tipoCobertura == null || "PARTICULAR".equals(tipoCobertura)) {
                        punto.setParticular(punto.getParticular() + cantidad);
                    } else {
                        punto.setObraSocial(punto.getObraSocial() + cantidad);
                    }
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al calcular la tendencia mensual", e);
        }

        datos.getPorMes().addAll(porMes.values());
    }
}
