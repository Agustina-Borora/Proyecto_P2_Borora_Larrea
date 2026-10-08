package controlador;

import java.awt.Component;

/**
 * Controlador de la pantalla Login.
 */
public final class LoginController {

    private LoginController() {
    }

    /**
     * Intenta autenticar al usuario. {@code identificador} puede ser el email o el nombre de pila
     * (ver {@link dao.Usuario#ingresar}).
     */
    public static Boolean autenticar(Component padre, String identificador, String password) {
        return ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> dao.Usuario.ingresar(con, identificador, password),
                null);
    }
}
