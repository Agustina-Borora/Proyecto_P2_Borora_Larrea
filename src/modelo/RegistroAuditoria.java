package modelo;

import java.util.Date;

/**
 * Modelo (POJO) que representa una fila de auditoría: un movimiento importante que quedó
 * registrado en el sistema (quién lo hizo, qué acción fue, sobre qué entidad, y cuándo). Ver
 * {@code dao.AuditoriaDAO} para la lista de qué queda registrado.
 */
public class RegistroAuditoria {

    private int idAuditoria;
    private Integer idUsuario;

    /**
     * Apellido y nombre de quien hizo la acción, ya resuelto con un JOIN contra `usuarios` (ver
     * {@code dao.AuditoriaDAO#listarRecientes}). Queda null si la acción no tiene un usuario de
     * sesión asociado (ej. un intento de inicio de sesión fallido) o si la cuenta que la hizo ya
     * no existe.
     */
    private String nombreUsuario;

    private String accion;
    private String entidad;
    private Integer idEntidad;
    private String detalle;
    private Date fechaHora;

    public RegistroAuditoria() {
    }

    public int getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(int idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Integer getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Integer idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Date getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Date fechaHora) {
        this.fechaHora = fechaHora;
    }
}
