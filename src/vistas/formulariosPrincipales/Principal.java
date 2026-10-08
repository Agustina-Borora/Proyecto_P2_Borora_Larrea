
package vistas.formulariosPrincipales;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import vistas.javafx.EstiloApp;
import vistas.javafx.shell.AppShell;

/**
 * Punto de entrada de la ventana principal del sistema. Antes era un {@code JFrame} con el
 * sidebar y el contenedor central ubicados a mano en coordenadas de píxel fijas
 * ({@code AbsoluteLayout}) -- ese diseño sólo se veía completo en la pantalla con la que se armó
 * y se cortaba (sidebar tapado por un "rincón gris") en cualquier notebook más chica. Ahora arma
 * un {@link AppShell} de JavaFX puro, con paneles de layout reales (VBox/BorderPane) que se
 * acomodan solos al tamaño de cada pantalla, sea cual sea su resolución.
 *
 * <p>Esta clase deja de extender {@code JFrame}, pero mantiene el mismo nombre y el mismo método
 * {@link #setVisible(boolean)} para no tener que tocar {@link Login}, que sigue haciendo
 * {@code new Principal().setVisible(true)} después de un login exitoso.</p>
 */
public class Principal {

    private Stage stage;

    /**
     * Fuerza la inicialización del toolkit de JavaFX (si todavía no arrancó) sin necesitar
     * {@code Application.launch()}: crear un {@code JFXPanel}, aunque no se lo use, alcanza para
     * que {@link Platform#runLater} ya funcione después de esto. Es el mismo truco que ya usa
     * {@link vistas.javafx.JavaFxHostPanel}.
     */
    public Principal() {
        new JFXPanel();
    }

    /**
     * Muestra (o esconde) la ventana principal. Arma el {@link AppShell} y lo despliega en un
     * {@code Stage} sin decoración del sistema operativo (mismo look que tenía el JFrame
     * {@code undecorated} original) pero maximizado con {@code Stage.setMaximized(true)}, que a
     * diferencia de {@code JFrame.setExtendedState(MAXIMIZED_BOTH)} respeta la barra de tareas de
     * Windows de forma confiable en cualquier resolución.
     */
    public void setVisible(boolean visible) {
        if (!visible) {
            if (stage != null) {
                Platform.runLater(() -> stage.hide());
            }
            return;
        }

        Platform.runLater(() -> {
            AppShell shell = new AppShell();
            shell.setAlCerrarSesion(() -> {
                // Ya estamos en el hilo de JavaFX (esto se dispara desde el Alert de
                // confirmación de AppShell), así que cerrar el Stage es directo; abrir el
                // Login (Swing) sí hay que despacharlo al Event Dispatch Thread.
                stage.close();
                javax.swing.SwingUtilities.invokeLater(() -> new Login().setVisible(true));
            });

            stage = new Stage(StageStyle.UNDECORATED);
            Scene escena = new Scene(shell.construir());
            escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
            stage.setScene(escena);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.show();
        });
    }

    public static void main(String args[]) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Error al inicializar FlatLaf: " + ex.getMessage());
        }

        java.awt.EventQueue.invokeLater(() -> new Principal().setVisible(true));
    }
}
