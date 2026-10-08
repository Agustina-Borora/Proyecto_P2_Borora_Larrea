package modelo;

import java.math.BigDecimal;

/**
 * Cobertura elegida en "Nuevo Análisis" al generar una orden: qué tipo es (Particular / Obra
 * Social / Mixto), con qué obra social y método de pago, los datos de afiliación cuando
 * corresponde, y el total calculado -- todo junto para poder guardarlo en la tabla `pagos` en
 * la misma transacción que crea el pedido (ver {@code dao.PagoDAO} y
 * {@code controlador.NuevoAnalisisController#generarOrden}).
 */
public class DatosCobertura {

    private final String tipoCobertura;
    private final ObraSocial obraSocial;
    private final String plan;
    private final String nroAfiliado;
    private final String metodoPago;
    private final BigDecimal montoEfectivo;
    private final BigDecimal total;

    public DatosCobertura(String tipoCobertura, ObraSocial obraSocial, String plan, String nroAfiliado,
            String metodoPago, BigDecimal montoEfectivo, BigDecimal total) {
        this.tipoCobertura = tipoCobertura;
        this.obraSocial = obraSocial;
        this.plan = plan;
        this.nroAfiliado = nroAfiliado;
        this.metodoPago = metodoPago;
        this.montoEfectivo = montoEfectivo;
        this.total = total;
    }

    /**
     * "PARTICULAR", "OBRA_SOCIAL" o "MIXTO" (ver {@code vistas.javafx.nuevoAnalisis.TipoCoberturaPanel}).
     */
    public String getTipoCobertura() {
        return tipoCobertura;
    }

    /**
     * Obra social elegida, o null si es Particular (o si todavía no se cargó ninguna en el ABM).
     */
    public ObraSocial getObraSocial() {
        return obraSocial;
    }

    public String getPlan() {
        return plan;
    }

    public String getNroAfiliado() {
        return nroAfiliado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    /**
     * Monto que abona el paciente en efectivo -- solo tiene sentido con cobertura Mixta; null en
     * cualquier otro caso.
     */
    public BigDecimal getMontoEfectivo() {
        return montoEfectivo;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
