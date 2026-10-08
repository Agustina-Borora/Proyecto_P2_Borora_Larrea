package modelo;

import java.util.Date;

/**
 * POJO con el detalle completo de una orden (un {@code pedido_analisis} puntual) para la
 * pantalla "Detalle de Orden" del Escritorio: datos del paciente, del médico derivante, del
 * análisis solicitado y de su cobertura. A diferencia de {@link OrdenResumen} -pensado para una
 * fila de tabla, con lo mínimo para listar-, este agrupa todo lo que necesita ese detalle
 * ampliado (igual que un pedido puede tener obra social o no, este detalle nunca expone el plan
 * ni la unidad bioquímica: solo, si corresponde, el nombre de la obra social).
 */
public class DetalleOrden {

    private String numeroOrden;
    private String dni;
    private String paciente;
    private int edad;
    private String telefono;
    private String email;
    private String medicoDerivante;
    private String examen;
    private Date fecha;
    private String observacion;
    private String cobertura;
    private String estado;
    private String estadoAnalisisRaw;
    private int idPedidoAnalisis;
    private int idAnalisisTipo;
    private int idPedido;
    private int idPaciente;
    private Integer idMedico;

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getPaciente() {
        return paciente;
    }

    public void setPaciente(String paciente) {
        this.paciente = paciente;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Nombre del médico derivante, o null si el pedido no tiene uno cargado (es opcional).
     */
    public String getMedicoDerivante() {
        return medicoDerivante;
    }

    public void setMedicoDerivante(String medicoDerivante) {
        this.medicoDerivante = medicoDerivante;
    }

    public String getExamen() {
        return examen;
    }

    public void setExamen(String examen) {
        this.examen = examen;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    /**
     * Observación del análisis (pedido_analisis.observaciones), o null si no se cargó ninguna.
     */
    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    /**
     * Nombre de la obra social del paciente, o "Particular" si no tiene una cargada. Nunca el
     * plan completo ni ningún dato de unidad bioquímica.
     */
    public String getCobertura() {
        return cobertura;
    }

    public void setCobertura(String cobertura) {
        this.cobertura = cobertura;
    }

    /**
     * Texto ya traducido para mostrar en el badge de estado (ver {@link dao.EstadoAnalisisUtil}).
     */
    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Valor crudo de {@code pedido_analisis.estado_analisis} (por ejemplo "pendiente",
     * "completado"), para decisiones de la vista como mostrar u ocultar "Cargar Resultados".
     */
    public String getEstadoAnalisisRaw() {
        return estadoAnalisisRaw;
    }

    public void setEstadoAnalisisRaw(String estadoAnalisisRaw) {
        this.estadoAnalisisRaw = estadoAnalisisRaw;
    }

    public int getIdPedidoAnalisis() {
        return idPedidoAnalisis;
    }

    public void setIdPedidoAnalisis(int idPedidoAnalisis) {
        this.idPedidoAnalisis = idPedidoAnalisis;
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    /**
     * Id del pedido dueño de este análisis (pedidos.id_pedido) -- lo necesitan "Editar Orden" y
     * "Enviar Resultados" para guardar cambios sobre el pedido entero, no solo sobre esta fila de
     * pedido_analisis.
     */
    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    /**
     * Id del paciente dueño de la orden (pacientes.id_paciente) -- lo necesita "Editar Orden" para
     * guardar los cambios de nombre/DNI/celular/email.
     */
    public int getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
    }

    /**
     * Id del médico derivante del pedido (medicos.id_medico), o null si no tiene uno cargado.
     */
    public Integer getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(Integer idMedico) {
        this.idMedico = idMedico;
    }
}
