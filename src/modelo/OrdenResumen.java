package modelo;

import java.util.Date;

/**
 * POJO para una fila de la tabla "Últimas Órdenes" del Escritorio.
 * No es una tabla real de la base: sale de un JOIN entre pedidos,
 * pacientes, pedido_analisis, analisis_tipos y envios (ver EscritorioDAO).
 */
public class OrdenResumen {

    private String numeroOrden;
    private String dni;
    private String paciente;
    private String examen;
    private Date fecha;
    private String cobertura;
    private String estado;
    private int idPedidoAnalisis;
    private int idAnalisisTipo;

    /**
     * Id real de la fila en pedido_analisis (el examen puntual dentro de
     * la orden). Lo necesita Registrar Resultados para saber sobre qué
     * fila guardar/leer resultados; EscritorioDAO no lo carga (se queda
     * en 0), no lo usa.
     */
    public int getIdPedidoAnalisis() {
        return idPedidoAnalisis;
    }

    public void setIdPedidoAnalisis(int idPedidoAnalisis) {
        this.idPedidoAnalisis = idPedidoAnalisis;
    }

    /** Id del analisis_tipo de este examen (para buscar sus parámetros). */
    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getCobertura() {
        return cobertura;
    }

    public void setCobertura(String cobertura) {
        this.cobertura = cobertura;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
