package modelo;

/**
 * Clase que gestiona la sesión actual del usuario dentro de la aplicación.
 */
public class Sesion {

    /**
     * Identificador único del usuario en la base de datos.
     */
    public static int idUsuario;

    /**
     * Nombre del usuario autenticado.
     */
    public static String nombre;

    /**
     * Apellido del usuario autenticado.
     */
    public static String apellido;

    /**
     * Rol o nivel de permisos del usuario (ej. Admin, Cliente, Operador).
     */
    public static String rol;
    
    /**
     * Limpia la sesión actual reiniciando todos los datos a sus valores por defecto.
     */
    public static void cerrarSesion() {
        idUsuario = 0;
        nombre = null;
        apellido = null;
        rol = null;
    }
}