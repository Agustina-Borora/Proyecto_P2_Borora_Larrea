package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelo.Prestacion;
import reportes.ComprobanteOrdenDatos;
import reportes.ResultadosDatos;

/**
 * Consultas para los informes PDF (un informe por ORDEN) y para recordar, en cada orden, dónde
 * quedó guardado su PDF y si tiene un diseño propio.
 *
 * <p>A diferencia de los otros DAO, estos métodos NO muestran carteles: tiran la
 * {@link SQLException} para que la pantalla decida qué mostrar. Es a propósito -- se llaman
 * desde un hilo en segundo plano (para que la pantalla no se tilde), y un cartel de Swing
 * disparado desde ese hilo es justamente una de las cosas que puede colgar la aplicación.</p>
 *
 * <p>Las columnas {@code pedidos.ruta_informe_pdf}, {@code pedidos.diseno_informe},
 * {@code envios.con_pdf} y {@code envios.archivo_pdf} las agrega
 * {@code sql/2026-10-08_informes_pdf.sql}. Si todavía no se corrió ese script, todo sigue
 * funcionando (el PDF se genera y se manda igual); solo no se recuerda la ruta ni el diseño
 * propio de cada orden -- ver {@link #faltaScript(SQLException)}.</p>
 */
public final class InformeDAO {

    /** MySQL: "Unknown column". */
    private static final int ERROR_COLUMNA_DESCONOCIDA = 1054;

    private InformeDAO() {
    }

    /** true si el error es porque todavía no se corrió el script SQL de esta versión. */
    public static boolean faltaScript(SQLException e) {
        return e != null && e.getErrorCode() == ERROR_COLUMNA_DESCONOCIDA;
    }

    /** id_pedido (la orden) al que pertenece un examen puntual (pedido_analisis). */
    public static int idPedidoDe(Connection con, int idPedidoAnalisis) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_pedido FROM pedido_analisis WHERE id_pedido_analisis = ?")) {
            ps.setInt(1, idPedidoAnalisis);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Todo lo de una orden para su informe: paciente, médico, cobertura, y cada estudio
     * TERMINADO con sus resultados y los valores de referencia (con colores) tal como están HOY
     * en el Catálogo. Los estudios no terminados van a la lista de "pendientes"; los cancelados
     * no aparecen. También arma los datos del comprobante, por si se pide agregarlo al final.
     *
     * @return null si la orden no existe.
     */
    public static ResultadosDatos cargarInforme(Connection con, int idPedido) throws SQLException {
        ResultadosDatos datos = new ResultadosDatos();
        BigDecimal total;
        String telefono;
        String email;

        String sqlOrden = "SELECT pe.numero_pedido, pe.fecha_pedido, pe.total_pedido, "
                + "p.nya_paciente, p.dni_paciente, p.fecha_nacimiento, p.telefono_paciente, p.email_paciente, "
                + "m.nombre_medico, "
                + "(SELECT os.nombre_obra_social FROM pagos pg JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social "
                + "  WHERE pg.id_pedido = pe.id_pedido AND pg.anulado_pago = 0 AND pg.id_obra_social IS NOT NULL LIMIT 1) AS obra_social "
                + "FROM pedidos pe "
                + "JOIN pacientes p ON p.id_paciente = pe.id_paciente "
                + "LEFT JOIN medicos m ON m.id_medico = pe.id_medico "
                + "WHERE pe.id_pedido = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlOrden)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                datos.setIdPedido(idPedido);
                datos.setNumeroOrden(rs.getString("numero_pedido"));
                datos.setFecha(rs.getDate("fecha_pedido"));
                datos.setNombrePaciente(rs.getString("nya_paciente"));
                datos.setDni(rs.getString("dni_paciente"));
                datos.setEdad(edad(rs.getDate("fecha_nacimiento")));
                telefono = rs.getString("telefono_paciente");
                email = rs.getString("email_paciente");
                datos.setTelefono(telefono);
                datos.setEmail(email);
                datos.setMedicoDerivante(rs.getString("nombre_medico"));
                String os = rs.getString("obra_social");
                datos.setCobertura(os != null ? os : "Particular");
                total = rs.getBigDecimal("total_pedido");
            }
        }

        List<Prestacion> prestaciones = new ArrayList<>();
        String sqlEstudios = "SELECT pa.id_pedido_analisis, pa.id_analisis_tipo, pa.estado_analisis, pa.observaciones, "
                + "at.nombre_analisis, at.codigo_analisis "
                + "FROM pedido_analisis pa JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pa.id_pedido = ? ORDER BY pa.id_pedido_analisis";
        List<int[]> completados = new ArrayList<>();
        List<String[]> textosCompletados = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sqlEstudios)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado_analisis");
                    String nombre = rs.getString("nombre_analisis");
                    if ("cancelado".equalsIgnoreCase(estado)) {
                        continue;
                    }
                    prestaciones.add(new Prestacion(0, rs.getInt("codigo_analisis"), nombre, null));
                    if ("completado".equalsIgnoreCase(estado)) {
                        completados.add(new int[]{rs.getInt("id_pedido_analisis"), rs.getInt("id_analisis_tipo")});
                        textosCompletados.add(new String[]{nombre, rs.getString("observaciones")});
                    } else {
                        datos.getEstudiosPendientes().add(nombre);
                    }
                }
            }
        }

        for (int i = 0; i < completados.size(); i++) {
            int idPedidoAnalisis = completados.get(i)[0];
            int idAnalisisTipo = completados.get(i)[1];
            datos.getEstudios().add(new ResultadosDatos.Estudio(idPedidoAnalisis, textosCompletados.get(i)[0],
                    textosCompletados.get(i)[1], cargarFilas(con, idPedidoAnalisis, idAnalisisTipo)));
        }

        datos.setComprobante(armarComprobante(con, idPedido, datos, telefono, email, prestaciones, total));
        return datos;
    }

    /**
     * Filas de un estudio. Incluye los analitos activos Y también los que se dieron de baja del
     * Catálogo después de cargar el resultado (si tienen un valor guardado para este examen) --
     * así un informe viejo nunca pierde filas por un cambio posterior en el Catálogo.
     */
    private static List<ResultadosDatos.Fila> cargarFilas(Connection con, int idPedidoAnalisis, int idAnalisisTipo)
            throws SQLException {
        String sql = "SELECT a.id_analito, a.nombre_analito, a.unidad, vr.texto_referencia, r.valor_resultado "
                + "FROM analitos a "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito AND vr.activo_valor_referencia = 1 "
                + "LEFT JOIN pedido_analito_resultado r ON r.id_analito = a.id_analito AND r.id_pedido_analisis = ? "
                + "WHERE a.id_analisis_tipo = ? AND (a.activo_analito = 1 OR r.valor_resultado IS NOT NULL) "
                + "ORDER BY a.orden_analito, a.id_analito, vr.id_valor_referencia";
        List<ResultadosDatos.Fila> filas = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedidoAnalisis);
            ps.setInt(2, idAnalisisTipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!vistos.add(rs.getInt("id_analito"))) {
                        continue; // más de una referencia activa para el mismo analito: se usa la primera
                    }
                    String crudo = rs.getString("texto_referencia");
                    String plano = utilidades.TramosTextoUtil.textoPlano(crudo);
                    String valor = rs.getString("valor_resultado");
                    filas.add(new ResultadosDatos.Fila(rs.getInt("id_analito"), rs.getString("nombre_analito"), valor, rs.getString("unidad"),
                            plano, utilidades.TramosTextoUtil.conSeparadores(utilidades.TramosTextoUtil.parsear(crudo)),
                            valor == null ? null : utilidades.CalculoEstadoUtil.calcular(valor, plano)));
                }
            }
        }
        return filas;
    }

    private static ComprobanteOrdenDatos armarComprobante(Connection con, int idPedido, ResultadosDatos datos,
            String telefono, String email, List<Prestacion> prestaciones, BigDecimal total) throws SQLException {
        String tipo = "PARTICULAR";
        String metodo = null;
        String plan = null;
        String afiliado = null;
        BigDecimal efectivo = null;
        String obraSocial = null;
        String sql = "SELECT pg.tipo_cobertura, pg.metodo_pago, pg.plan_obra_social, pg.nro_afiliado, pg.monto_efectivo, "
                + "os.nombre_obra_social FROM pagos pg LEFT JOIN obras_sociales os ON os.id_obra_social = pg.id_obra_social "
                + "WHERE pg.id_pedido = ? AND pg.anulado_pago = 0 ORDER BY pg.id_pago DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tipo = rs.getString("tipo_cobertura") == null ? "PARTICULAR" : rs.getString("tipo_cobertura");
                    metodo = rs.getString("metodo_pago");
                    plan = rs.getString("plan_obra_social");
                    afiliado = rs.getString("nro_afiliado");
                    efectivo = rs.getBigDecimal("monto_efectivo");
                    obraSocial = rs.getString("nombre_obra_social");
                }
            }
        }
        return new ComprobanteOrdenDatos(datos.getNumeroOrden(), datos.getFecha(), datos.getNombrePaciente(),
                datos.getDni(), telefono, email, datos.getMedicoDerivante(), tipo, obraSocial, plan, afiliado, metodo,
                efectivo, prestaciones, total);
    }

    // ------------------------------------------------------------------ ruta del PDF y diseño propio

    /** Dónde quedó guardado el último PDF de la orden, o null. */
    public static String leerRutaPdf(Connection con, int idPedido) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT ruta_informe_pdf FROM pedidos WHERE id_pedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public static void guardarRutaPdf(Connection con, int idPedido, String ruta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pedidos SET ruta_informe_pdf = ? WHERE id_pedido = ?")) {
            ps.setString(1, ruta);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    /** Diseño propio de la orden (texto de {@link reportes.DisenoInforme#aTexto()}), o null si usa el predeterminado. */
    public static String leerDiseno(Connection con, int idPedido) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT diseno_informe FROM pedidos WHERE id_pedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Guarda (o borra, con null) el diseño propio de la orden. */
    public static void guardarDiseno(Connection con, int idPedido, String diseno) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pedidos SET diseno_informe = ? WHERE id_pedido = ?")) {
            ps.setString(1, diseno);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    /** Cambios de texto hechos a mano en el informe de la orden (ver {@link reportes.AjustesInforme}), o null. */
    public static String leerAjustes(Connection con, int idPedido) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT ajustes_informe FROM pedidos WHERE id_pedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public static void guardarAjustes(Connection con, int idPedido, String ajustes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pedidos SET ajustes_informe = ? WHERE id_pedido = ?")) {
            ps.setString(1, ajustes);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    /**
     * Pasa al Catálogo de Exámenes el nombre, la unidad y/o la referencia (con colores) de un
     * analito -- para cuando el cambio hecho en un informe tiene que valer para todas las órdenes.
     * Los valores null no se tocan.
     */
    public static void guardarEnCatalogo(Connection con, int idAnalito, String nombre, String unidad,
            String referenciaCruda) throws SQLException {
        if (nombre != null && !nombre.trim().isEmpty()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE analitos SET nombre_analito = ? WHERE id_analito = ?")) {
                ps.setString(1, nombre.trim());
                ps.setInt(2, idAnalito);
                ps.executeUpdate();
            }
        }
        if (unidad != null) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE analitos SET unidad = ? WHERE id_analito = ?")) {
                ps.setString(1, unidad.trim().isEmpty() ? null : unidad.trim());
                ps.setInt(2, idAnalito);
                ps.executeUpdate();
            }
        }
        if (referenciaCruda != null) {
            int cambiadas;
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE valores_referencia SET texto_referencia = ? WHERE id_analito = ? AND activo_valor_referencia = 1")) {
                ps.setString(1, referenciaCruda);
                ps.setInt(2, idAnalito);
                cambiadas = ps.executeUpdate();
            }
            if (cambiadas == 0) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO valores_referencia (id_analito, texto_referencia, activo_valor_referencia) VALUES (?, ?, 1)")) {
                    ps.setInt(1, idAnalito);
                    ps.setString(2, referenciaCruda);
                    ps.executeUpdate();
                }
            }
        }
    }

    /** id del examen (analisis_tipos) al que pertenece un analito. */
    public static int examenDeAnalito(Connection con, int idAnalito) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id_analisis_tipo FROM analitos WHERE id_analito = ?")) {
            ps.setInt(1, idAnalito);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /** Órdenes que ya tienen un PDF guardado y que incluyen un examen dado (para regenerarlas). */
    public static List<Integer> pedidosConPdfPorExamen(Connection con, int idAnalisisTipo) throws SQLException {
        String sql = "SELECT DISTINCT pe.id_pedido FROM pedidos pe "
                + "JOIN pedido_analisis pa ON pa.id_pedido = pe.id_pedido "
                + "WHERE pa.id_analisis_tipo = ? AND pe.ruta_informe_pdf IS NOT NULL AND pe.ruta_informe_pdf <> ''";
        return listarIds(con, sql, idAnalisisTipo);
    }

    /** Todas las órdenes que ya tienen un PDF guardado. */
    public static List<Integer> pedidosConPdf(Connection con) throws SQLException {
        return listarIds(con, "SELECT id_pedido FROM pedidos WHERE ruta_informe_pdf IS NOT NULL AND ruta_informe_pdf <> ''",
                null);
    }

    private static List<Integer> listarIds(Connection con, String sql, Integer parametro) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private static int edad(Date nacimiento) {
        if (nacimiento == null) {
            return 0;
        }
        LocalDate fecha = nacimiento.toLocalDate();
        return Math.max(0, Period.between(fecha, LocalDate.now()).getYears());
    }

    // ------------------------------------------------------------------ mover PDF de carpeta

    /** {id_pedido, ruta} de todas las órdenes que tienen un PDF anotado. */
    public static java.util.List<Object[]> rutasPdf(Connection con) throws SQLException {
        java.util.List<Object[]> lista = new java.util.ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_pedido, ruta_informe_pdf FROM pedidos WHERE ruta_informe_pdf IS NOT NULL AND ruta_informe_pdf <> ''");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Object[]{rs.getInt(1), rs.getString(2)});
            }
        }
        return lista;
    }

    /**
     * Cambia la ruta anotada en los envíos ya hechos (columna {@code envios.archivo_pdf}) cuando
     * el archivo se movió de carpeta. Si la columna todavía no existe, no hace nada.
     */
    public static void cambiarRutaEnvios(Connection con, String rutaVieja, String rutaNueva) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE envios SET archivo_pdf = ? WHERE archivo_pdf = ?")) {
            ps.setString(1, rutaNueva);
            ps.setString(2, rutaVieja);
            ps.executeUpdate();
        } catch (SQLException e) {
            // sin el script SQL no hay rutas anotadas en los envíos
        }
    }
}
