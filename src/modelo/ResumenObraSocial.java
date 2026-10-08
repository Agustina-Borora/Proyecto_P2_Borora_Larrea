package modelo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Una fila de la tabla "Obras Sociales" de la pestaña "Cobros a Obras Sociales" de Pagos: cuánto
 * le queda por cobrar el laboratorio a una obra social puntual, sobre los pedidos del mes en
 * curso.
 */
public class ResumenObraSocial {

    private String nombreObraSocial;
    private int cantidadOrdenes;
    private Date ultimaOrden;
    private BigDecimal totalAdeudado = BigDecimal.ZERO;

    public String getNombreObraSocial() {
        return nombreObraSocial;
    }

    public void setNombreObraSocial(String nombreObraSocial) {
        this.nombreObraSocial = nombreObraSocial;
    }

    public int getCantidadOrdenes() {
        return cantidadOrdenes;
    }

    public void setCantidadOrdenes(int cantidadOrdenes) {
        this.cantidadOrdenes = cantidadOrdenes;
    }

    public Date getUltimaOrden() {
        return ultimaOrden;
    }

    public void setUltimaOrden(Date ultimaOrden) {
        this.ultimaOrden = ultimaOrden;
    }

    public BigDecimal getTotalAdeudado() {
        return totalAdeudado;
    }

    public void setTotalAdeudado(BigDecimal totalAdeudado) {
        this.totalAdeudado = totalAdeudado;
    }
}
