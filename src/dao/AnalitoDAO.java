package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.AnalitoReferencia;
import modelo.Parametro;

/**
 * DAO para `analitos` + `valores_referencia`: el sistema real de parámetros de examen (no el
 * que armé el 03/09 sin saber que este ya existía — ver analisis_parametros, dado de baja).
 */
public class AnalitoDAO {

    /**
     * Trae los analitos de un analisis_tipo con su valor de referencia aplicable al sexo del
     * paciente (id_sexo del paciente, o id_sexo = 3 "No especificado" para los que no varían por
     * sexo).
     */
    public static List<Parametro> listarConReferencia(Connection con, int idAnalisisTipo, int idSexoPaciente) {
        List<Parametro> parametros = new ArrayList<>();

        String sql = "SELECT a.id_analito, a.nombre_analito, a.tipo_dato, a.orden_analito, a.unidad, "
                + "vr.texto_referencia "
                + "FROM analitos a "
                + "LEFT JOIN valores_referencia vr ON vr.id_analito = a.id_analito "
                + "AND vr.activo_valor_referencia = 1 AND (vr.id_sexo = ? OR vr.id_sexo = 3) "
                + "WHERE a.id_analisis_tipo = ? AND a.activo_analito = 1 "
                + "ORDER BY a.orden_analito, a.id_analito";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSexoPaciente);
            ps.setInt(2, idAnalisisTipo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Parametro parametro = new Parametro();
                    parametro.setIdParametro(rs.getInt("id_analito"));
                    parametro.setIdAnalisisTipo(idAnalisisTipo);
                    parametro.setNombreParametro(rs.getString("nombre_analito"));
                    parametro.setOrdenParametro(rs.getInt("orden_analito"));
                    parametro.setUnidad(rs.getString("unidad"));
                    parametro.setTipoDato(rs.getString("tipo_dato"));
                    parametro.setValorReferencia(rs.getString("texto_referencia"));

                    parametros.add(parametro);
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los analitos del examen", e);
        }

        return parametros;
    }

    /**
     * Trae los analitos de un analisis_tipo con TODOS sus valores de referencia cargados (uno
     * por cada sexo que tenga, más el general si aplica a cualquier sexo), para pantallas de
     * solo lectura que necesitan mostrar las referencias de varios sexos juntas -a diferencia
     * de {@link #listarConReferencia}, que ya viene filtrado a un único sexo puntual-.
     */
    public static List<AnalitoReferencia> listarConReferenciaPorSexo(Connection con, int idAnalisisTipo) {
        Map<Integer, AnalitoReferencia> porId = new LinkedHashMap<>();

        String sql = "SELECT a.id_analito, a.nombre_analito, a.tipo_dato, a.orden_analito, a.unidad, "
                + "vr.id_sexo, vr.texto_referencia "
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
                    int idSexo = rs.getInt("id_sexo");
                    if (textoReferencia == null || rs.wasNull()) {
                        continue;
                    }
                    if (idSexo == 3) {
                        analito.setReferenciaGeneral(textoReferencia);
                    } else {
                        analito.getReferenciasPorSexo().put(idSexo, textoReferencia);
                    }
                }
            }
        } catch (SQLException e) {
            Mensajes.error("Error al listar los analitos del examen", e);
        }

        return new ArrayList<>(porId.values());
    }
}
