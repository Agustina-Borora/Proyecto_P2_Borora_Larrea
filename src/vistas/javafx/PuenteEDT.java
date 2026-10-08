package vistas.javafx;

import javax.swing.SwingUtilities;

/**
 * Los Controller existentes (a través de {@link controlador.ConexionUtil}) muestran un
 * {@code JOptionPane} si algo falla al hablar con la base de datos -- eso sólo es seguro
 * hacerlo desde el Event Dispatch Thread de Swing, nunca desde el hilo de JavaFX (JavaFX
 * Application Thread). Esta clase ejecuta esas llamadas en el EDT y espera el resultado, para
 * poder seguir usando los mismos Controller desde las pantallas nuevas hechas en JavaFX.
 */
public final class PuenteEDT {

    private PuenteEDT() {
    }

    public interface Operacion<T> {
        T ejecutar();
    }

    public static <T> T ejecutar(Operacion<T> operacion, T valorPorError) {
        final Object[] resultado = new Object[1];
        try {
            SwingUtilities.invokeAndWait(() -> resultado[0] = operacion.ejecutar());
        } catch (Exception e) {
            e.printStackTrace();
            return valorPorError;
        }
        @SuppressWarnings("unchecked")
        T valor = (T) resultado[0];
        return valor;
    }
}
