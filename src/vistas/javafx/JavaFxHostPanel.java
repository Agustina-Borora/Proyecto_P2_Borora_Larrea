package vistas.javafx;

import java.awt.BorderLayout;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Panel Swing que aloja una pantalla construida en JavaFX. Existe para que
 * {@link vistas.formulariosPrincipales.Principal#navegar} pueda seguir usando su mismo mecanismo
 * de navegación (que sólo sabe agregar {@code JComponent}s al contenedor central) con pantallas
 * hechas en JavaFX puro (sin FXML), sin tener que tocar esa clase.
 *
 * Las pantallas nuevas de ABM (Obras Sociales, Médicos, Usuarios, Catálogo de Exámenes) se arman
 * en JavaFX y se exponen a través de una subclase de esta, que sólo tiene que implementar
 * {@link #construirRaiz()}.
 */
public abstract class JavaFxHostPanel extends javax.swing.JPanel {

    static {
        // Evita que cerrar un Stage modal (los formularios de alta/edición) apague el hilo de
        // JavaFX -- si eso pasara, la siguiente pantalla JavaFX ya no podría abrir.
        Platform.setImplicitExit(false);
    }

    public JavaFxHostPanel() {
        setLayout(new BorderLayout());
        JFXPanel fxPanel = new JFXPanel();
        add(fxPanel, BorderLayout.CENTER);
        Platform.runLater(() -> {
            Parent raiz = construirRaiz();
            Scene escena = new Scene(raiz);
            escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
            fxPanel.setScene(escena);
        });
    }

    /**
     * Arma el árbol de nodos de esta pantalla. Se ejecuta en el hilo de JavaFX (JavaFX
     * Application Thread), nunca en el Event Dispatch Thread de Swing -- si necesita llamar a un
     * Controller existente, usar {@link PuenteEDT#ejecutar}.
     */
    protected abstract Parent construirRaiz();
}
