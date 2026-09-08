package controlador;

import java.awt.Component;

/**
 * Controlador de la pantalla Login.
 */
public final class LoginController {

    private LoginController() {
    }

    /**
     * Intenta autenticar al usuario.
     */
    public static Boolean autenticar(Component padre, String email, String password) {
        return ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> dao.Usuario.ingresar(con, email, password),
                null);
    }
}
