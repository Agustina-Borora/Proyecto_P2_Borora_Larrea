package modelo;

import java.util.Date;

/**
 * Una fila de "Pacientes frecuentes" de Estadísticas: un paciente con más de una visita en el
 * período elegido, cuántas veces vino y cuál fue su último estudio.
 */
public class PacienteFrecuente {

    private String nombrePaciente;
    private int visitas;
    private String ultimoEstudio;
    private Date fechaUltimoEstudio;

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente = nombrePaciente;
    }

    public int getVisitas() {
        return visitas;
    }

    public void setVisitas(int visitas) {
        this.visitas = visitas;
    }

    public String getUltimoEstudio() {
        return ultimoEstudio;
    }

    public void setUltimoEstudio(String ultimoEstudio) {
        this.ultimoEstudio = ultimoEstudio;
    }

    public Date getFechaUltimoEstudio() {
        return fechaUltimoEstudio;
    }

    public void setFechaUltimoEstudio(Date fechaUltimoEstudio) {
        this.fechaUltimoEstudio = fechaUltimoEstudio;
    }
}
