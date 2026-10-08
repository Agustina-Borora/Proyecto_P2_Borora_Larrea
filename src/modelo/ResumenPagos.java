package modelo;

import java.math.BigDecimal;

/**
 * Números que alimentan las 4 tarjetas de la pestaña "Resumen" de Pagos, todos calculados sobre
 * los pedidos del mes en curso (por fecha_pedido).
 */
public class ResumenPagos {

    private BigDecimal totalMes = BigDecimal.ZERO;
    private BigDecimal cobradoParticular = BigDecimal.ZERO;
    private BigDecimal pendienteOs = BigDecimal.ZERO;
    private BigDecimal totalHoy = BigDecimal.ZERO;

    /**
     * Suma de total_pedido de todos los pedidos del mes.
     */
    public BigDecimal getTotalMes() {
        return totalMes;
    }

    public void setTotalMes(BigDecimal totalMes) {
        this.totalMes = totalMes;
    }

    /**
     * Lo que pusieron los pacientes en efectivo este mes (Particular completo + la parte en
     * efectivo de los Mixtos).
     */
    public BigDecimal getCobradoParticular() {
        return cobradoParticular;
    }

    public void setCobradoParticular(BigDecimal cobradoParticular) {
        this.cobradoParticular = cobradoParticular;
    }

    /**
     * Lo que queda por cobrarle a las obras sociales este mes (Obra Social completo + la parte
     * no efectivo de los Mixtos).
     */
    public BigDecimal getPendienteOs() {
        return pendienteOs;
    }

    public void setPendienteOs(BigDecimal pendienteOs) {
        this.pendienteOs = pendienteOs;
    }

    /**
     * Suma de total_pedido de los pedidos de hoy.
     */
    public BigDecimal getTotalHoy() {
        return totalHoy;
    }

    public void setTotalHoy(BigDecimal totalHoy) {
        this.totalHoy = totalHoy;
    }
}
