package vistas.javafx;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Arma la {@link Scene} de una ventana/diálogo con un tamaño "ideal" (el mismo con el que se
 * diseñó, ej. 820x720) pero sin pasarse nunca del área visible de la pantalla real -- antes
 * varios diálogos (Nuevo/Editar Examen, Colores de un valor de referencia) se armaban con
 * {@code new Scene(raiz, ancho, alto)} fijo y {@code setResizable(false)}, así que en una compu
 * con una pantalla más chica que ese diseño (una notebook de 1366x768, o con el zoom de Windows
 * en más de 100%) la ventana terminaba más alta que el espacio disponible y la parte de abajo --
 * típicamente la fila de botones "Guardar"/"Cancelar" -- quedaba tapada por la barra de tareas o
 * directamente fuera de la pantalla, sin forma de hacerle clic.
 *
 * <p>Se usa junto con envolver el contenido (salvo la fila de botones, que se deja fija abajo de
 * todo) en un {@code ScrollPane} con la clase de estilo {@code config-scroll} -- así, si ni
 * reduciendo el tamaño de la ventana entra todo (por ejemplo un examen con muchos analitos
 * cargados), se puede bajar con la rueda del mouse en vez de perder el acceso a los botones.</p>
 */
public final class VentanaUtil {

    /** Margen que se deja libre contra los bordes de la pantalla (barra de tareas, decorado de la ventana, etc.). */
    private static final double MARGEN = 80;

    private VentanaUtil() {
    }

    /**
     * Arma la Scene para {@code raiz} con el ancho/alto ideal pedido, recortado como mucho al
     * área visible de la pantalla principal (la que excluye la barra de tareas de Windows) menos
     * un margen. Además dejar {@code ventana} redimensionable, con un tamaño mínimo razonable, y
     * la centra en la pantalla -- así, si a alguien le queda justo igual, puede agrandarla a mano.
     */
    public static Scene escenaAdaptable(Stage ventana, Parent raiz, double anchoIdeal, double altoIdeal) {
        Rectangle2D visible = Screen.getPrimary().getVisualBounds();
        double ancho = Math.min(anchoIdeal, visible.getWidth() - MARGEN);
        double alto = Math.min(altoIdeal, visible.getHeight() - MARGEN);

        Scene escena = new Scene(raiz, ancho, alto);

        ventana.setResizable(true);
        ventana.setMinWidth(Math.min(420, ancho));
        ventana.setMinHeight(Math.min(320, alto));
        ventana.setScene(escena);
        ventana.centerOnScreen();
        animarAparicion(ventana, raiz);
        return escena;
    }

    /**
     * Fade + un achique/agrande muy sutil (escala 0.97 -&gt; 1) al abrirse la ventana, en vez de
     * aparecer de golpe -- mismo recurso que ya usan la mayoría de las apps de escritorio e
     * interfaces web modernas para que un diálogo nuevo se sienta menos "brusco". Se dispara en
     * {@code ventana.setOnShown} (no antes de mostrar la ventana) para no retrasar la aparición
     * en sí, sólo lo que se ve adentro.
     *
     * <p>{@link #escenaAdaptable} ya la llama sola, así que todo diálogo que arma su Scene con
     * ese método (la mayoría de los nuevos) la tiene gratis. Los diálogos más viejos que todavía
     * arman su propia {@code new Scene(raiz, ...)} a mano (Editar/Enviar/Detalle de Orden, Obra
     * Social, Valor de la UB) la llaman una vez cada uno, justo antes de {@code showAndWait()}.</p>
     */
    public static void animarAparicion(Stage ventana, Parent raiz) {
        raiz.setOpacity(0);

        FadeTransition entrada = new FadeTransition(Duration.millis(190), raiz);
        entrada.setFromValue(0);
        entrada.setToValue(1);

        ScaleTransition escala = new ScaleTransition(Duration.millis(190), raiz);
        escala.setFromX(0.97);
        escala.setFromY(0.97);
        escala.setToX(1);
        escala.setToY(1);

        ParallelTransition aparicion = new ParallelTransition(entrada, escala);
        ventana.setOnShown(evt -> aparicion.play());
    }
}
