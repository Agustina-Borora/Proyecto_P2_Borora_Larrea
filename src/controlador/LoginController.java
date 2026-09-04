package controlador;

import java.awt.Component;

/**
 * Controlador de la pantalla Login. Antes, formulariosPrincipales.Login
 * abría la conexión, llamaba a conexiones.Usuario.ingresar() y la cerraba
 * ella misma; ahora Login solo le pasa el email/password a este Controlador
 * y usa el resultado para decidir qué hacer.
 */
public final class LoginController {

    private LoginController() {
    }

    /**
     * Intenta autenticar al usuario. Devuelve:
     * - Boolean.TRUE si las credenciales son correctas (la sesión ya quedó
     *   cargada en conexiones.Sesion, eso lo hace conexiones.Usuario).
     * - Boolean.FALSE si se pudo consultar la base pero el email/password
     *   no coinciden con ningún usuario.
     * - null si no se pudo ni intentar (sin conexión, o error SQL): en ese
     *   caso ConexionUtil ya mostró el cartel correspondiente, así que Login
     *   no debe mostrar además "credenciales incorrectas".
     */
    public static Boolean autenticar(Component padre, String email, String password) {
        return ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> conexiones.Usuario.ingresar(con, email, password),
                null);
    }
}
