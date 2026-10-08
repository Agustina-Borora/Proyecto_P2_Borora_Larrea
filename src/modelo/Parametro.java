package modelo;

import java.util.Collections;
import java.util.List;

/**
 * POJO para una fila de `analisis_parametros`: un renglón de resultado dentro de un
 * analisis_tipo (ej. "Hemoglobina" dentro de "Hemograma completo").
 */
public class Parametro {

    private int idParametro;
    private int idAnalisisTipo;
    private String nombreParametro;
    private int ordenParametro;
    private String tipoDato; // "numerico" | "cualitativo" | "texto"
    private String unidad;
    private String valorReferencia;
    private Integer idSexo; // null = aplica a cualquier sexo
    private String opcionesCualitativo;

    /**
     * Los mismos tramos de {@link #valorReferencia} pero ya separados por color (ver
     * {@link utilidades.TramosTextoUtil#parsear}), para poder mostrarlos/imprimirlos coloreados.
     * {@link #valorReferencia} sigue siendo el texto plano (sin marcadores de color) para no
     * romper el cálculo de Normal/Alto/Bajo por expresión regular.
     */
    private List<TramoTexto> tramosReferencia = Collections.emptyList();

    public int getIdParametro() {
        return idParametro;
    }

    public void setIdParametro(int idParametro) {
        this.idParametro = idParametro;
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    public String getNombreParametro() {
        return nombreParametro;
    }

    public void setNombreParametro(String nombreParametro) {
        this.nombreParametro = nombreParametro;
    }

    public int getOrdenParametro() {
        return ordenParametro;
    }

    public void setOrdenParametro(int ordenParametro) {
        this.ordenParametro = ordenParametro;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getValorReferencia() {
        return valorReferencia;
    }

    public void setValorReferencia(String valorReferencia) {
        this.valorReferencia = valorReferencia;
    }

    public Integer getIdSexo() {
        return idSexo;
    }

    public void setIdSexo(Integer idSexo) {
        this.idSexo = idSexo;
    }

    public String getOpcionesCualitativo() {
        return opcionesCualitativo;
    }

    public void setOpcionesCualitativo(String opcionesCualitativo) {
        this.opcionesCualitativo = opcionesCualitativo;
    }

    public List<TramoTexto> getTramosReferencia() {
        return tramosReferencia;
    }

    public void setTramosReferencia(List<TramoTexto> tramosReferencia) {
        this.tramosReferencia = tramosReferencia == null ? Collections.emptyList() : tramosReferencia;
    }
}
