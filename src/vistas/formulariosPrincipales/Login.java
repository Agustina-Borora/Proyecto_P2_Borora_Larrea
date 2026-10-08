package vistas.formulariosPrincipales;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vistas.javafx.EstiloApp;
import vistas.javafx.login.LoginScreen;

/**
 * Ventana de inicio de sesión de la aplicación -- es el {@code main.class} del proyecto
 * (ver {@code nbproject/project.properties}), y también la ventana a la que se vuelve después de
 * "Cerrar sesión" desde {@code vistas.javafx.shell.AppShell} (a través de {@code Principal}).
 *
 * <p>Antes era un {@code JFrame} armado con el editor visual de NetBeans ({@code GroupLayout}
 * generado). Ahora arma una {@link LoginScreen} de JavaFX puro, mismo criterio que ya se usó para
 * {@link Principal}: esta clase deja de extender {@code JFrame}, pero mantiene el mismo nombre,
 * el mismo constructor sin argumentos y el mismo método {@link #setVisible(boolean)} para no
 * tener que tocar ni el {@code main.class} de NetBeans ni el único lugar que todavía hace
 * {@code new Login().setVisible(true)} ({@code Principal#setVisible} al cerrar sesión).</p>
 *
 * <p>A diferencia de {@link Principal} (que usa {@code StageStyle.UNDECORATED} porque así era el
 * JFrame original), el JFrame original de Login NO era undecorado -- tenía la barra de título y
 * los botones de la ventana del sistema operativo normales, así que acá se mantiene el
 * {@code Stage} con su decoración por defecto.</p>
 */
public class Login {

    private Stage stage;

    /**
     * Fuerza la inicialización del toolkit de JavaFX (si todavía no arrancó) sin necesitar
     * {@code Application.launch()} -- mismo truco que {@link Principal#Principal()}.
     */
    public Login() {
        new JFXPanel();
    }

    /**
     * Muestra (o esconde) la ventana de Login.
     */
    public void setVisible(boolean visible) {
        if (!visible) {
            if (stage != null) {
                Platform.runLater(() -> stage.hide());
            }
            return;
        }

        Platform.runLater(() -> {
            LoginScreen pantalla = new LoginScreen(this::onLoginExitoso);

            stage = new Stage();
            stage.setTitle(utilidades.PreferenciasSistema.getNombreSistema() + " -- Iniciar sesión");
            Scene escena = new Scene(pantalla.construir());
            escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
            stage.setScene(escena);
            stage.setResizable(false);
            // Mismo criterio que tenía el JFrame original (EXIT_ON_CLOSE): cerrar la ventana de
            // Login sin haber iniciado sesión cierra toda la aplicación.
            stage.setOnCloseRequest(evt -> System.exit(0));
            stage.show();
        });
    }

    /**
     * Callback de {@link LoginScreen.LoginExitosoListener}: cierra esta ventana y abre la
     * principal -- mismo flujo que tenía {@code Login.jButton1ActionPerformed} en la versión
     * Swing.
     */
    private void onLoginExitoso() {
        stage.close();
        Principal principal = new Principal();
        principal.setVisible(true);
    }

    public static void main(String args[]) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Error al inicializar FlatLaf: " + ex.getMessage());
        }

        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
}
