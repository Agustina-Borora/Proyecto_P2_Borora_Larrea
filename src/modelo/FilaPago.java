package modelo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Una fila de la tabla "Todas las órdenes del mes" de la pantalla Pagos: un pedido completo (no
 * un examen puntual, como {@link OrdenResumen}), con lo que costó y cómo se reparte entre lo que
 * puso el paciente y lo que queda por cobrarle a la obra social.
 *
 * <p>montoOS y montoPac se calculan en {@code dao.PagoDAO} a partir de tipoCobertura:</p>
 * <ul>
 *   <li>PARTICULAR: todo el total lo puso el paciente (montoOS = 0).</li>
 *   <li>OBRA_SOCIAL: todo el total lo cubre la obra social (montoPac = 0).</li>
 *   <li>MIXTO: montoPac es lo que cargó la bioquímica como efectivo del paciente, y montoOS es
 *       el resto del total.</li>
 * </ul>
 */
public class FilaPago {

    private int idPedido;
    private String numeroOrden;
    private String paciente;
    private String examenes;
    private Date fecha;
    private String tipoCobertura;
    private String nombreObraSocial;
    private BigDecimal total;
    private BigDecimal montoOs;
    private BigDecimal montoPac;
    private String estadoCobroOs;
    private Date fechaCobroOs;

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getPaciente() {
        return paciente;
    }

    public void setPaciente(String paciente) {
        this.paciente = paciente;
    }

    public String getExamenes() {
        return examenes;
    }

    public void setExamenes(String examenes) {
        this.examenes = examenes;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    /**
     * "PARTICULAR" | "OBRA_SOCIAL" | "MIXTO" (ver {@link DatosCobertura}).
     */
    public String getTipoCobertura() {
        return tipoCobertura;
    }

    public void setTipoCobertura(String tipoCobertura) {
        this.tipoCobertura = tipoCobertura;
    }

    /**
     * Nombre de la obra social, o null si es Particular.
     */
    public String getNombreObraSocial() {
        return nombreObraSocial;
    }

    public void setNombreObraSocial(String nombreObraSocial) {
        this.nombreObraSocial = nombreObraSocial;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getMontoOs() {
        return montoOs;
    }

    public void setMontoOs(BigDecimal montoOs) {
        this.montoOs = montoOs;
    }

    public BigDecimal getMontoPac() {
        return montoPac;
    }

    public void setMontoPac(BigDecimal montoPac) {
        this.montoPac = montoPac;
    }

    /**
     * "pendiente" | "en_gestion" | "cobrado", o null si esta fila no tiene obra social (Particular)
     * o si viene de una consulta que no carga este dato (ver {@code dao.PagoDAO#cargarDatosPagos}
     * -- sólo {@code dao.PagoDAO#cargarCobrosObraSocial} lo completa).
     */
    public String getEstadoCobroOs() {
        return estadoCobroOs;
    }

    public void setEstadoCobroOs(String estadoCobroOs) {
        this.estadoCobroOs = estadoCobroOs;
    }

    /**
     * Fecha en la que se marcó como cobrado, o null mientras no lo esté.
     */
    public Date getFechaCobroOs() {
        return fechaCobroOs;
    }

    public void setFechaCobroOs(Date fechaCobroOs) {
        this.fechaCobroOs = fechaCobroOs;
    }
}
