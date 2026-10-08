package reportes;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import modelo.Prestacion;

/**
 * Datos ya armados para imprimir el "Comprobante de Orden" (Mes 6: Reportes impresos), tal como
 * quedaron en pantalla justo después de generar la orden en Nuevo Análisis.
 *
 * <p>Es una foto de ese momento, no una consulta a la base: el formulario se vacía enseguida
 * después de mostrar el cartel de éxito (ver {@code NuevoAnalisisScreen#limpiarPantallaCompleta}),
 * así que estos datos hay que juntarlos ANTES de eso.</p>
 */
public class ComprobanteOrdenDatos {

    private final String numeroOrden;
    private final Date fecha;
    private final String nombrePaciente;
    private final String dni;
    private final String telefono;
    private final String email;
    private final String medicoDerivante;
    private final String cobertura;
    private final String obraSocial;
    private final String plan;
    private final String nroAfiliado;
    private final String metodoPago;
    private final BigDecimal montoEfectivo;
    private final List<Prestacion> analisis;
    private final BigDecimal total;

    public ComprobanteOrdenDatos(String numeroOrden, Date fecha, String nombrePaciente, String dni,
            String telefono, String email, String medicoDerivante, String cobertura, String obraSocial,
            String plan, String nroAfiliado, String metodoPago, BigDecimal montoEfectivo,
            List<Prestacion> analisis, BigDecimal total) {
        this.numeroOrden = numeroOrden;
        this.fecha = fecha;
        this.nombrePaciente = nombrePaciente;
        this.dni = dni;
        this.telefono = telefono;
        this.email = email;
        this.medicoDerivante = medicoDerivante;
        this.cobertura = cobertura;
        this.obraSocial = obraSocial;
        this.plan = plan;
        this.nroAfiliado = nroAfiliado;
        this.metodoPago = metodoPago;
        this.montoEfectivo = montoEfectivo;
        this.analisis = analisis;
        this.total = total;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public Date getFecha() {
        return fecha;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public String getDni() {
        return dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getMedicoDerivante() {
        return medicoDerivante;
    }

    /**
     * "PARTICULAR", "OBRA_SOCIAL" o "MIXTO" (ver {@code TipoCoberturaPanel}).
     */
    public String getCobertura() {
        return cobertura;
    }

    public String getObraSocial() {
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
     * Monto que abona el paciente en efectivo -- solo tiene valor con cobertura Mixta.
     */
    public BigDecimal getMontoEfectivo() {
        return montoEfectivo;
    }

    public List<Prestacion> getAnalisis() {
        return analisis;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
