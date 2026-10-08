package dao;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Centraliza el cartel de error que antes estaba copiado y pegado (con el mismo título
 * "ERROR!!!...") en los 10 DAO del sistema, 25 veces en total.
 */
public final class Mensajes {

    private Mensajes() {
    }

    /**
     * Muestra el cartel de error. Si se llama desde un hilo que no es el de Swing (por ejemplo
     * un trabajo en segundo plano), el cartel se manda al hilo de Swing en vez de abrirse ahí
     * mismo -- abrir un {@code JOptionPane} fuera de ese hilo puede dejar la aplicación tildada.
     */
    public static void error(String contexto, Exception e) {
        Runnable mostrar = () -> JOptionPane.showMessageDialog(null, contexto + ": " + e.getMessage(),
                "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
        if (SwingUtilities.isEventDispatchThread()) {
            mostrar.run();
        } else {
            System.err.println(contexto + ": " + e.getMessage());
            SwingUtilities.invokeLater(mostrar);
        }
    }
}
