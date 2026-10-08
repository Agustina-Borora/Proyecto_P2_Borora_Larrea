package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.Statement;
import modelo.Analito;
import modelo.AnalitoReferencia;
import modelo.Parametro;

/**
 * DAO para `analitos` + `valores_referencia`: el sistema real de parámetros de examen (no el
 * que armé el 03/09 sin saber que este ya existía — ver analisis_parametros, dado de baja).
 */
public class AnalitoDAO {

    /**
     * Trae los analitos de un examen (con su valor de referencia) para editarlos en el ABM del
     * Catálogo de Exámenes. La tabla real {@code valores_referencia} no distingue por sexo (no
     * tiene columna id_sexo) -- un solo texto_referencia por analito.
     */
    public static List<Analito> listarParaAbm(Connection con, int idAnalisisTipo) {
        List<Analito> lista = new ArrayList<>();
        String sql = "SELECT a.id_analito, a.nombre_analito, a.tipo_dato, a.unidad, a.orden_analito, "
                + "vr.texto_referencia "
                + "FROM analitos a "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito "
                + "AND vr.activo_valor_referencia = 1 "
                + "WHERE a.id_analisis_tipo = ? AND a.activo_analito = 1 "
                + "ORDER BY a.orden_analito, a.id_analito";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAnalisisTipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Analito analito = new Analito();
                    analito.setIdAnalito(rs.getInt("id_analito"));
                    analito.setIdAnalisisTipo(idAnalisisTipo);
                    analito.setNombreAnalito(rs.getString("nombre_analito"));
                    analito.setTipoDato(rs.getString("tipo_dato"));
                    analito.setUnidad(rs.getString("unidad"));
                    analito.setOrdenAnalito(rs.getInt("orden_analito"));
                    analito.setValorReferencia(rs.getString("texto_referencia"));
                    lista.add(analito);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los parámetros del examen", e);
        }
        return lista;
    }

    public static Integer crear(Connection con, Analito analito) {
        String sql = "INSERT INTO analitos (id_analisis_tipo, nombre_analito, tipo_dato, unidad, "
                + "orden_analito, activo_analito) VALUES (?, ?, ?, ?, ?, 1)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, analito.getIdAnalisisTipo());
            ps.setString(2, analito.getNombreAnalito());
            ps.setString(3, analito.getTipoDato());
            ps.setString(4, analito.getUnidad());
            ps.setInt(5, analito.getOrdenAnalito());
            if (ps.executeUpdate() == 0) {
                return null;
            }
            Integer id;
            try (ResultSet claves = ps.getGeneratedKeys()) {
                id = claves.next() ? claves.getInt(1) : null;
            }
            if (id != null) {
                guardarValorReferencia(con, id, analito.getValorReferencia());
            }
            return id;
        } catch (SQLException e) {
            Mensajes.error("Error al crear el parámetro \"" + analito.getNombreAnalito() + "\"", e);
            return null;
        }
    }

    public static boolean actualizar(Connection con, Analito analito) {
        String sql = "UPDATE analitos SET nombre_analito = ?, tipo_dato = ?, unidad = ?, "
                + "orden_analito = ? WHERE id_analito = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, analito.getNombreAnalito());
            ps.setString(2, analito.getTipoDato());
            ps.setString(3, analito.getUnidad());
            ps.setInt(4, analito.getOrdenAnalito());
            ps.setInt(5, analito.getIdAnalito());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                guardarValorReferencia(con, analito.getIdAnalito(), analito.getValorReferencia());
            }
            return ok;
        } catch (SQLException e) {
            Mensajes.error("Error al actualizar el parámetro", e);
            return false;
        }
    }

    /**
     * Baja lógica de un analito -- se conserva el registro por si hay resultados históricos
     * cargados contra él.
     */
    public static boolean eliminar(Connection con, int idAnalito) {
        String sql = "UPDATE analitos SET activo_analito = 0 WHERE id_analito = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAnalito);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Mensajes.error("Error al eliminar el parámetro", e);
            return false;
        }
    }

    /**
     * Reemplaza el valor de referencia de un analito: borra el que hubiera y, si viene un texto
     * no vacío, carga uno nuevo. Evita depender del nombre de la clave primaria de
     * {@code valores_referencia} para hacer un UPDATE puntual. La tabla real no distingue por
     * sexo (no tiene columna id_sexo) -- un solo valor de referencia por analito.
     */
    private static void guardarValorReferencia(Connection con, int idAnalito, String texto) throws SQLException {
        try (PreparedStatement borrar = con.prepareStatement(
                "DELETE FROM valores_referencia WHERE id_analito = ?")) {
            borrar.setInt(1, idAnalito);
            borrar.executeUpdate();
        }
        if (texto == null) {
            return;
        }
        String recortado = recortarEspaciosComunes(texto);
        if (!recortado.isEmpty()) {
            try (PreparedStatement insertar = con.prepareStatement(
                    "INSERT INTO valores_referencia (id_analito, texto_referencia, "
                            + "activo_valor_referencia) VALUES (?, ?, 1)")) {
                insertar.setInt(1, idAnalito);
                insertar.setString(2, recortado);
                insertar.executeUpdate();
            }
        }
    }

    /**
     * Igual que {@code String.trim()} pero solo recorta espacios "de verdad" (espacio, tab, salto
     * de línea) al principio y al final -- a propósito NO recorta cualquier caracter de control,
     * porque {@code String.trim()} sí lo hace (recorta todo caracter &lt;= U+0020) y eso rompía el
     * marcador de color de {@link utilidades.TramosTextoUtil} ({@code \u0001}, U+0001): un valor
     * de referencia cuyo PRIMER tramo tenía color (ej. todo el rango en azul) se guardaba con el
     * marcador inicial cortado, así que al releerlo quedaba mal separado -- se perdía el color Y
     * el texto del primer tramo pasaba a leerse como si fuera parte del texto plano (rompiendo
     * también el cálculo de Normal/Alto/Bajo, que depende de ese texto plano). Encontrado con
     * {@code prueba.TestFlujoInformes}.
     */
    private static String recortarEspaciosComunes(String texto) {
        int inicio = 0;
        int fin = texto.length();
        while (inicio < fin && esEspacioComun(texto.charAt(inicio))) {
            inicio++;
        }
        while (fin > inicio && esEspacioComun(texto.charAt(fin - 1))) {
            fin--;
        }
        return texto.substring(inicio, fin);
    }

    private static boolean esEspacioComun(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Trae los analitos de un analisis_tipo con su valor de referencia. El parámetro
     * {@code idSexoPaciente} se conserva por compatibilidad de firma con el resto del código,
     * pero no se usa -- la tabla real no distingue por sexo (no tiene columna id_sexo).
     */
    public static List<Parametro> listarConReferencia(Connection con, int idAnalisisTipo, int idSexoPaciente) {
        List<Parametro> parametros = new ArrayList<>();

        String sql = "SELECT a.id_analito, a.nombre_analito, a.tipo_dato, a.orden_analito, a.unidad, "
                + "vr.texto_referencia "
                + "FROM analitos a "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito "
                + "AND vr.activo_valor_referencia = 1 "
                + "WHERE a.id_analisis_tipo = ? AND a.activo_analito = 1 "
                + "ORDER BY a.orden_analito, a.id_analito";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAnalisisTipo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Parametro parametro = new Parametro();
                    parametro.setIdParametro(rs.getInt("id_analito"));
                    parametro.setIdAnalisisTipo(idAnalisisTipo);
                    parametro.setNombreParametro(rs.getString("nombre_analito"));
                    parametro.setOrdenParametro(rs.getInt("orden_analito"));
                    parametro.setUnidad(rs.getString("unidad"));
                    parametro.setTipoDato(rs.getString("tipo_dato"));

                    // El texto crudo puede traer tramos de colores (ver TramosTextoUtil) -- acá se separan:
                    // valorReferencia queda en texto plano (lo que necesita el cálculo de Normal/Alto/Bajo
                    // por expresión regular) y tramosReferencia con los mismos tramos ya coloreados, para
                    // mostrar/imprimir.
                    String crudo = rs.getString("texto_referencia");
                    parametro.setValorReferencia(utilidades.TramosTextoUtil.textoPlano(crudo));
                    parametro.setTramosReferencia(utilidades.TramosTextoUtil.parsear(crudo));

                    parametros.add(parametro);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los analitos del examen", e);
        }

        return parametros;
    }

    /**
     * Trae los analitos de un analisis_tipo con su valor de referencia, para pantallas de solo
     * lectura (como el detalle de paciente). La tabla real no distingue por sexo (no tiene
     * columna id_sexo), así que el único valor cargado por analito se deja en
     * {@link AnalitoReferencia#getReferenciaGeneral()} -- {@code referenciasPorSexo} queda
     * siempre vacío, se conserva en el modelo por si el día de mañana se separa por sexo.
     */
    public static List<AnalitoReferencia> listarConReferenciaPorSexo(Connection con, int idAnalisisTipo) {
        Map<Integer, AnalitoReferencia> porId = new LinkedHashMap<>();

        String sql = "SELECT a.id_analito, a.nombre_analito, a.tipo_dato, a.orden_analito, a.unidad, "
                + "vr.texto_referencia "
                + "FROM analitos a "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito "
                + "AND vr.activo_valor_referencia = 1 "
                + "WHERE a.id_analisis_tipo = ? AND a.activo_analito = 1 "
                + "ORDER BY a.orden_analito, a.id_analito";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAnalisisTipo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idAnalito = rs.getInt("id_analito");

                    AnalitoReferencia analito = porId.get(idAnalito);
                    if (analito == null) {
                        analito = new AnalitoReferencia();
                        analito.setIdAnalito(idAnalito);
                        analito.setNombreAnalito(rs.getString("nombre_analito"));
                        analito.setUnidad(rs.getString("unidad"));
                        analito.setTipoDato(rs.getString("tipo_dato"));
                        porId.put(idAnalito, analito);
                    }

                    String textoReferencia = rs.getString("texto_referencia");
                    if (textoReferencia != null) {
                        analito.setReferenciaGeneral(textoReferencia);
                    }
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los analitos del examen", e);
        }

        return new ArrayList<>(porId.values());
    }
}
