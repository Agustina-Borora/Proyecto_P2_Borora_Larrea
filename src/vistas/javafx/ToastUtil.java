package vistas.javafx;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Cartel chico ("toast") que aparece abajo a la derecha de toda la ventana, se queda un rato
 * visible y se apaga solo con un fade -- pensado para reemplazar los "✓ Guardado" de texto fijo
 * que había repartidos por Configuración y algún diálogo, con un feedback más parecido al de una
 * app web moderna (se nota más, y no hace falta que la bioquímica se fije si el texto de al lado
 * del botón cambió o no).
 *
 * <p>Se ancla sobre el panel de fondo de toda la app ({@code vistas.javafx.shell.AppShell}, que
 * se registra una sola vez con {@link #registrarOverlay} apenas se construye) -- así cualquier
 * pantalla o diálogo puede llamar a {@link #exito}/{@link #error}/{@link #info} sin tener que
 * recibir ni pasar ninguna referencia de por medio.</p>
 */
public final class ToastUtil {

    public enum Tipo {
        EXITO, ERROR, INFO
    }

    private static StackPane overlay;

    private ToastUtil() {
    }

    /** La llama {@code AppShell#construir} una sola vez, apenas arma el panel de fondo de la app. */
    public static void registrarOverlay(StackPane panelFondo) {
        overlay = panelFondo;
    }

    /** Confirmación en verde (misma idea que los "✓ Guardado" que reemplaza). */
    public static void exito(String mensaje) {
        mostrar(mensaje, Tipo.EXITO);
    }

    /** Aviso en rojo para algo que no se pudo hacer. */
    public static void error(String mensaje) {
        mostrar(mensaje, Tipo.ERROR);
    }

    /** Aviso neutro (celeste) para algo informativo que no es ni éxito ni error. */
    public static void info(String mensaje) {
        mostrar(mensaje, Tipo.INFO);
    }

    public static void mostrar(String mensaje, Tipo tipo) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(mensaje, tipo));
            return;
        }
        if (overlay == null) {
            return; // Todavía no se construyó AppShell (no debería pasar en la app real andando).
        }

        String icono = tipo == Tipo.EXITO ? "✓  " : tipo == Tipo.ERROR ? "✕  " : "ℹ  ";
        Label etiqueta = new Label(icono + mensaje);
        etiqueta.getStyleClass().add("toast");
        etiqueta.getStyleClass().add(
                tipo == Tipo.EXITO ? "toast-exito" : tipo == Tipo.ERROR ? "toast-error" : "toast-info");
        etiqueta.setOpacity(0);
        etiqueta.setMouseTransparent(true);
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(380);
        StackPane.setAlignment(etiqueta, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(etiqueta, new Insets(0, 28, 28, 0));

        overlay.getChildren().add(etiqueta);

        FadeTransition entrada = new FadeTransition(Duration.millis(220), etiqueta);
        entrada.setFromValue(0);
        entrada.setToValue(1);

        PauseTransition espera = new PauseTransition(Duration.millis(2600));

        FadeTransition salida = new FadeTransition(Duration.millis(420), etiqueta);
        salida.setFromValue(1);
        salida.setToValue(0);

        SequentialTransition secuencia = new SequentialTransition(entrada, espera, salida);
        secuencia.setOnFinished(evt -> overlay.getChildren().remove(etiqueta));
        secuencia.play();
    }
}
