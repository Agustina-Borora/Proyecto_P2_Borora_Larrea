package modelo;

import java.math.BigDecimal;

/**
 * Modelo (POJO) para un tipo de análisis (tabla `analisis_tipos`). Se usa tanto para listados
 * simples (por ejemplo, la tabla de permisos por examen de Nuevo Usuario, con el constructor de
 * dos parámetros) como para el ABM completo del Catálogo de Exámenes (con código, estado y el
 * precio estimado, que no es una columna propia sino que se calcula a partir de
 * {@code prestaciones.unidades_bioquimicas × valor_ub} vigente -- mismo criterio de precios que
 * usa Nuevo Análisis).
 */
public class AnalisisTipo {

    private int idAnalisisTipo;
    private Integer codigoAnalisis;
    private String nombreAnalisis;
    private String categoria;
    private boolean activo;

    /** Sólo para mostrar en el ABM -- no son columnas propias de analisis_tipos. */
    private BigDecimal unidadesBioquimicas;
    private BigDecimal precioEstimado;

    public AnalisisTipo() {
    }

    public AnalisisTipo(int idAnalisisTipo, String nombreAnalisis) {
        this.idAnalisisTipo = idAnalisisTipo;
        this.nombreAnalisis = nombreAnalisis;
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    public Integer getCodigoAnalisis() {
        return codigoAnalisis;
    }

    public void setCodigoAnalisis(Integer codigoAnalisis) {
        this.codigoAnalisis = codigoAnalisis;
    }

    public String getNombreAnalisis() {
        return nombreAnalisis;
    }

    public void setNombreAnalisis(String nombreAnalisis) {
        this.nombreAnalisis = nombreAnalisis;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getUnidadesBioquimicas() {
        return unidadesBioquimicas;
    }

    public void setUnidadesBioquimicas(BigDecimal unidadesBioquimicas) {
        this.unidadesBioquimicas = unidadesBioquimicas;
    }

    public BigDecimal getPrecioEstimado() {
        return precioEstimado;
    }

    public void setPrecioEstimado(BigDecimal precioEstimado) {
        this.precioEstimado = precioEstimado;
    }

    @Override
    public String toString() {
        return nombreAnalisis;
    }
}
