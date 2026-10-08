package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import modelo.EvolucionPaciente;

/**
 * Consultas del "Registro evolutivo" de un paciente (Estadísticas &gt; Evolución de un paciente).
 */
public final class EvolucionPacienteDAO {

    private EvolucionPacienteDAO() {
    }

    /** Un paciente encontrado en la búsqueda. */
    public static final class PacienteEncontrado {
        public final int idPaciente;
        public final String nombre;
        public final String dni;
        public final Date fechaNacimiento;
        public final int visitas;

        PacienteEncontrado(int idPaciente, String nombre, String dni, Date fechaNacimiento, int visitas) {
            this.idPaciente = idPaciente;
            this.nombre = nombre;
            this.dni = dni;
            this.fechaNacimiento = fechaNacimiento;
            this.visitas = visitas;
        }

        @Override
        public String toString() {
            int edad = EvolucionPaciente.edadEn(fechaNacimiento, new Date());
            return nombre + "   ·   DNI " + (dni == null ? "-" : dni)
                    + (edad >= 0 ? "   ·   " + EvolucionPaciente.textoEdad(edad) : "")
                    + "   ·   " + visitas + (visitas == 1 ? " visita" : " visitas");
        }
    }

    /**
     * Pacientes cuyo nombre o DNI contiene el texto (vacío = los que más vinieron). Máximo 50.
     */
    public static List<PacienteEncontrado> buscar(Connection con, String texto) throws SQLException {
        String filtro = texto == null ? "" : texto.trim();
        String sql = "SELECT p.id_paciente, p.nya_paciente, p.dni_paciente, p.fecha_nacimiento, "
                + "(SELECT COUNT(*) FROM pedidos pe WHERE pe.id_paciente = p.id_paciente) AS visitas "
                + "FROM pacientes p "
                + (filtro.isEmpty() ? "" : "WHERE p.nya_paciente LIKE ? OR p.dni_paciente LIKE ? ")
                + "ORDER BY visitas DESC, p.nya_paciente LIMIT 50";
        List<PacienteEncontrado> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!filtro.isEmpty()) {
                ps.setString(1, "%" + filtro + "%");
                ps.setString(2, "%" + filtro.replace(".", "") + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PacienteEncontrado(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDate(4),
                            rs.getInt(5)));
                }
            }
        }
        return lista;
    }

    /** Todo el registro evolutivo del paciente. */
    public static EvolucionPaciente cargar(Connection con, int idPaciente) throws SQLException {
        EvolucionPaciente ev = new EvolucionPaciente();
        ev.setIdPaciente(idPaciente);
        try (PreparedStatement ps = con.prepareStatement("SELECT p.nya_paciente, p.dni_paciente, p.fecha_nacimiento, "
                + "s.nombre_sexo FROM pacientes p LEFT JOIN sexos s ON s.id_sexo = p.id_sexo WHERE p.id_paciente = ?")) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No se encontró el paciente.");
                }
                ev.setNombre(rs.getString(1));
                ev.setDni(rs.getString(2));
                ev.setFechaNacimiento(rs.getDate(3));
                ev.setSexo(rs.getString(4));
            }
        } catch (SQLException e) {
            if (e.getErrorCode() != 1146) { // si no existe la tabla "sexos", se sigue sin el sexo
                throw e;
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT nya_paciente, dni_paciente, fecha_nacimiento FROM pacientes WHERE id_paciente = ?")) {
                ps.setInt(1, idPaciente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ev.setNombre(rs.getString(1));
                        ev.setDni(rs.getString(2));
                        ev.setFechaNacimiento(rs.getDate(3));
                    }
                }
            }
        }

        // Visitas (órdenes) con sus estudios.
        String sqlVisitas = "SELECT pe.id_pedido, pe.numero_pedido, pe.fecha_pedido, m.nombre_medico, at.nombre_analisis "
                + "FROM pedidos pe "
                + "LEFT JOIN medicos m ON m.id_medico = pe.id_medico "
                + "LEFT JOIN pedido_analisis pa ON pa.id_pedido = pe.id_pedido "
                + "LEFT JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pe.id_paciente = ? ORDER BY pe.fecha_pedido, pe.id_pedido, pa.id_pedido_analisis";
        Map<Integer, Object[]> visitas = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sqlVisitas)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    Object[] v = visitas.get(id);
                    if (v == null) {
                        v = new Object[]{rs.getString(2), rs.getDate(3), rs.getString(4), new ArrayList<String>()};
                        visitas.put(id, v);
                    }
                    String estudio = rs.getString(5);
                    @SuppressWarnings("unchecked")
                    List<String> estudios = (List<String>) v[3];
                    if (estudio != null && !estudios.contains(estudio)) {
                        estudios.add(estudio);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() != 1054) {
                throw e;
            }
            cargarVisitasSinMedico(con, idPaciente, visitas);
        }
        for (Map.Entry<Integer, Object[]> e : visitas.entrySet()) {
            Object[] v = e.getValue();
            Date fecha = (Date) v[1];
            @SuppressWarnings("unchecked")
            List<String> estudios = (List<String>) v[3];
            ev.getVisitas().add(new EvolucionPaciente.Visita(e.getKey(), (String) v[0], fecha,
                    EvolucionPaciente.edadEn(ev.getFechaNacimiento(), fecha), String.join(", ", estudios), (String) v[2]));
        }

        // Resultados cargados (con la referencia actual del Catálogo para marcar Alto/Bajo).
        String sqlResultados = "SELECT a.id_analito, a.nombre_analito, at.nombre_analisis, a.unidad, pe.fecha_pedido, "
                + "r.valor_resultado, vr.texto_referencia "
                + "FROM pedido_analito_resultado r "
                + "JOIN pedido_analisis pa ON pa.id_pedido_analisis = r.id_pedido_analisis "
                + "JOIN pedidos pe ON pe.id_pedido = pa.id_pedido "
                + "JOIN analitos a ON a.id_analito = r.id_analito "
                + "JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito AND vr.activo_valor_referencia = 1 "
                + "WHERE pe.id_paciente = ? AND r.valor_resultado IS NOT NULL AND r.valor_resultado <> '' "
                + "ORDER BY at.nombre_analisis, a.orden_analito, a.id_analito, pe.fecha_pedido, pe.id_pedido";
        Set<String> vistos = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(sqlResultados)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate(5);
                    String clave = rs.getInt(1) + "|" + fecha + "|" + rs.getString(6);
                    if (!vistos.add(clave)) {
                        continue; // más de una referencia activa: una sola fila
                    }
                    String referencia = utilidades.TramosTextoUtil.textoPlano(rs.getString(7));
                    String valor = rs.getString(6);
                    ev.getMediciones().add(new EvolucionPaciente.Medicion(rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), fecha, EvolucionPaciente.edadEn(ev.getFechaNacimiento(), fecha), valor,
                            referencia, utilidades.CalculoEstadoUtil.calcular(valor, referencia)));
                }
            }
        }
        return ev;
    }

    private static void cargarVisitasSinMedico(Connection con, int idPaciente, Map<Integer, Object[]> visitas)
            throws SQLException {
        String sql = "SELECT pe.id_pedido, pe.numero_pedido, pe.fecha_pedido, at.nombre_analisis FROM pedidos pe "
                + "LEFT JOIN pedido_analisis pa ON pa.id_pedido = pe.id_pedido "
                + "LEFT JOIN analisis_tipos at ON at.id_analisis_tipo = pa.id_analisis_tipo "
                + "WHERE pe.id_paciente = ? ORDER BY pe.fecha_pedido, pe.id_pedido, pa.id_pedido_analisis";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] v = visitas.get(rs.getInt(1));
                    if (v == null) {
                        v = new Object[]{rs.getString(2), rs.getDate(3), null, new ArrayList<String>()};
                        visitas.put(rs.getInt(1), v);
                    }
                    @SuppressWarnings("unchecked")
                    List<String> estudios = (List<String>) v[3];
                    if (rs.getString(4) != null && !estudios.contains(rs.getString(4))) {
                        estudios.add(rs.getString(4));
                    }
                }
            }
        }
    }
}
