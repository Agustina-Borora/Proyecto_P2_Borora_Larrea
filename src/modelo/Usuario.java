package modelo;

/**
 * Modelo (POJO) para un usuario del sistema (tabla `usuarios`), usado en el listado de "Cuentas
 * y Permisos" y al ver/editar un usuario desde {@link vista.usuarios.CrearUsuario}. La
 * autenticación y el cambio de contraseña siguen viviendo en {@link dao.Usuario} -- este modelo
 * es sólo para listar/mostrar/editar los datos básicos de la cuenta.
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private boolean activo;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String apellido, String email, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * "Apellido y Nombre" combinados, tal como se muestran en el listado y en el campo único del
     * formulario de Datos Personales.
     */
    public String getApellidoYNombre() {
        String texto = (apellido == null ? "" : apellido) + " " + (nombre == null ? "" : nombre);
        return texto.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
