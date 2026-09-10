package modelo;

import java.math.BigDecimal;

/**
 * Modelo (POJO) para una fila de la tabla `prestaciones` (el catálogo de prestaciones/códigos
 * que se cargó desde el Excel; antes se llamaba `nomenclador`): código, nombre y unidades
 * bioquímicas.
 */
public class Prestacion {

    private int idPrestacion;
    private int codigo;
    private String nombrePrestacion;
    private BigDecimal unidadesBioquimicas;

    public Prestacion() {
    }

    public Prestacion(int idPrestacion, int codigo, String nombrePrestacion, BigDecimal unidadesBioquimicas) {
        this.idPrestacion = idPrestacion;
        this.codigo = codigo;
        this.nombrePrestacion = nombrePrestacion;
        this.unidadesBioquimicas = unidadesBioquimicas;
    }

    public int getIdPrestacion() {
        return idPrestacion;
    }

    public void setIdPrestacion(int idPrestacion) {
        this.idPrestacion = idPrestacion;
    }

    public int getCodigo() {
        return codigo;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public String getNombrePrestacion() {
        return nombrePrestacion;
    }

    public void setNombrePrestacion(String nombrePrestacion) {
        this.nombrePrestacion = nombrePrestacion;
    }

    public BigDecimal getUnidadesBioquimicas() {
        return unidadesBioquimicas;
    }

    public void setUnidadesBioquimicas(BigDecimal unidadesBioquimicas) {
        this.unidadesBioquimicas = unidadesBioquimicas;
    }
}
