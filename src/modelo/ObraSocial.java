package modelo;

/**
 * Modelo (POJO) para una obra social (tabla `obras_sociales`), usado en el ABM de la pantalla
 * de Configuración.
 */
public class ObraSocial {

    private int idObraSocial;
    private String nombre;
    private String codigoInterno;
    private String cuit;
    private String telefono;
    private String email;
    private String direccion;
    private boolean activa;

    public ObraSocial() {
    }

    public ObraSocial(int idObraSocial, String nombre, String codigoInterno, String cuit,
            String telefono, String email, String direccion, boolean activa) {
        this.idObraSocial = idObraSocial;
        this.nombre = nombre;
        this.codigoInterno = codigoInterno;
        this.cuit = cuit;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.activa = activa;
    }

    public int getIdObraSocial() {
        return idObraSocial;
    }

    public void setIdObraSocial(int idObraSocial) {
        this.idObraSocial = idObraSocial;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
