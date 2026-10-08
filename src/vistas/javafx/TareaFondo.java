package vistas.javafx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Corre un trabajo lento (consultar la base, armar un PDF, mandar un email) en un hilo aparte, y
 * cuando termina vuelve a la pantalla con el resultado.
 *
 * <p>Por qué existe: la pantalla (el hilo de JavaFX) solo puede hacer una cosa a la vez. Si
 * mientras tanto está esperando a MySQL o a un servidor de email, no puede ni dibujarse ni
 * responder a los clicks -- eso es exactamente el "se tilda / No responde" que pasaba al mandar
 * por WhatsApp o Gmail. Con esto la pantalla queda libre mientras el trabajo corre.</p>
 *
 * <pre>
 *     TareaFondo.ejecutar(
 *         () -> InformesPdfService.guardarInforme(idPedido),   // corre en segundo plano
 *         archivo -> mostrar("Listo: " + archivo),             // vuelve a la pantalla
 *         error -> mostrarError(error.getMessage()));          // vuelve a la pantalla
 * </pre>
 */
public final class TareaFondo {

    private static final ExecutorService HILOS = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tarea-fondo");
        t.setDaemon(true);
        return t;
    });

    private TareaFondo() {
    }

    public interface Trabajo<T> {
        T hacer() throws Exception;
    }

    /**
     * @param alTerminar se ejecuta en el hilo de JavaFX con el resultado.
     * @param alFallar   se ejecuta en el hilo de JavaFX con el error (puede ser null).
     */
    public static <T> void ejecutar(Trabajo<T> trabajo, Consumer<T> alTerminar, Consumer<Exception> alFallar) {
        HILOS.execute(() -> {
            try {
                T resultado = trabajo.hacer();
                if (alTerminar != null) {
                    Platform.runLater(() -> alTerminar.accept(resultado));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (alFallar != null) {
                    Platform.runLater(() -> alFallar.accept(e));
                }
            }
        });
    }

    /** Mensaje entendible de un error (sin el nombre técnico de la excepción). */
    public static String mensaje(Exception e) {
        if (e == null) {
            return "Error desconocido.";
        }
        String m = e.getMessage();
        return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
    }
}
