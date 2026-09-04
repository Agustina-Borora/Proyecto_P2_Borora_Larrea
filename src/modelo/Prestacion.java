package modelo;

import java.math.BigDecimal;

/**
 * Modelo (POJO) para una fila de la tabla `nomenclador` (el catálogo de
 * prestaciones/códigos que se cargó desde el Excel): código, nombre y
 * unidades bioquímicas. Es lo único que importa de esa tabla para elegir
 * los análisis de una orden (ver dao.NomencladorDAO).
 */
public class Prestacion {

    private int idNomenclador;
    private int codigo;
    private String nombrePrestacion;
    private BigDecimal unidadesBioquimicas;

    public Prestacion() {
    }

    public Prestacion(int idNomenclador, int codigo, String nombrePrestacion, BigDecimal unidadesBioquimicas) {
        this.idNomenclador = idNomenclador;
        this.codigo = codigo;
        this.nombrePrestacion = nombrePrestacion;
        this.unidadesBioquimicas = unidadesBioquimicas;
    }

    public int getIdNomenclador() {
        return idNomenclador;
    }

    public void setIdNomenclador(int idNomenclador) {
        this.idNomenclador = idNomenclador;
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
