package conexiones;

/**
 * Clase que gestiona la sesión actual del usuario dentro de la aplicación.
 * Almacena la información del usuario autenticado en atributos estáticos
 * para un acceso global a lo largo de todo el ciclo de vida de la ejecución.
 */
public class Sesion {

    /** Identificador único del usuario en la base de datos. */
    public static int idUsuario;

    /** Nombre del usuario autenticado. */
    public static String nombre;

    /** Apellido del usuario autenticado. */
    public static String apellido;

    /** Rol o nivel de permisos del usuario (ej. Admin, Cliente, Operador). */
    public static String rol;
    
    /**
     * Limpia la sesión actual reiniciando todos los datos a sus valores por
     * defecto. Se debe invocar al cerrar sesión (logout) para que ningún
     * dato del usuario anterior quede accesible desde estos campos estáticos.
     */
    public static void cerrarSesion() {
        idUsuario = 0;
        nombre = null;
        apellido = null;
        rol = null;
    }
}