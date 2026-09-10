package modelo;

import java.util.Calendar;
import java.util.Date;

/**
 * Modelo (POJO) que representa un paciente del laboratorio.
 */
public class Paciente {

    private int idPaciente;
    private String nyaPaciente;
    private String dni;
    private Date fechaNacimiento;
    private int idSexo;
    private String telefono;
    private String email;

    private String nombreObraSocial;
    private Date ultimoExamen;

    public Paciente() {
    }

    /**
     * Calcula la edad actual del paciente a partir de la fecha de nacimiento.
     */
    public int calcularEdad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        Calendar nacimiento = Calendar.getInstance();
        nacimiento.setTime(fechaNacimiento);
        Calendar hoy = Calendar.getInstance();

        int edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR);
        if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
            edad--;
        }
        return edad;
    }

    public int getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getNyaPaciente() {
        return nyaPaciente;
    }

    public void setNyaPaciente(String nyaPaciente) {
        this.nyaPaciente = nyaPaciente;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public int getIdSexo() {
        return idSexo;
    }

    public void setIdSexo(int idSexo) {
        this.idSexo = idSexo;
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

    public String getNombreObraSocial() {
        return nombreObraSocial;
    }

    public void setNombreObraSocial(String nombreObraSocial) {
        this.nombreObraSocial = nombreObraSocial;
    }

    public Date getUltimoExamen() {
        return ultimoExamen;
    }

    public void setUltimoExamen(Date ultimoExamen) {
        this.ultimoExamen = ultimoExamen;
    }
}
