package vistas.javafx;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Mes 8 (rediseño visual): las tarjetas de resumen ("Total de Pacientes", "Total del mes", etc.)
 * en Pacientes, Usuarios, Estadísticas y Pagos eran, cada una por su cuenta, una cajita de color
 * liso con su propio método {@code construirTarjeta} duplicado pantalla por pantalla -- el mismo
 * estilo "antes" que tenían las 4 tarjetas del Escritorio hasta la primera vuelta de este
 * rediseño (ver el git log de {@code EscritorioScreen#construirTarjeta}). En vez de copiar y
 * pegar ese mismo cambio 4 veces más, se saca a un solo lugar compartido: tarjeta blanca con
 * sombra, franja de color a la izquierda, insignia circular con un emoji adentro, y el efecto de
 * "se levanta" al pasar el mouse -- exactamente el mismo look que ya aprobó la clienta para el
 * Escritorio, reutilizando las mismas clases CSS ({@code .tarjeta-stat*} en theme.css) para que
 * las tarjetas de TODA la app se vean igual entre sí.
 *
 * <p>{@code claveColor} es uno de "azul"/"verde"/"ambar"/"rojo" (las 4 variantes que ya existen
 * en theme.css). {@code glifo} puede ser un emoji de color (ya probado en el Escritorio: se ve
 * bien en esta app) o un símbolo de texto plano.</p>
 */
public final class TarjetaStatUtil {

    private TarjetaStatUtil() {
    }

    public static VBox construir(String titulo, Label valorLabel, String descripcion, String claveColor, String glifo) {
        return construir(titulo, valorLabel, new Label(descripcion), claveColor, glifo);
    }

    /**
     * Variante para cuando la descripción no es un texto fijo sino un {@link Label} que la
     * pantalla sigue actualizando después (ej. PagosScreen#subtituloTotalHoy, que cambia la fecha
     * del día) -- hace falta quedarse con la MISMA referencia, no copiar su texto a una nueva.
     */
    public static VBox construir(String titulo, Label valorLabel, Label descripcionLabel, String claveColor, String glifo) {
        Label lblGlifo = new Label(glifo);
        lblGlifo.getStyleClass().addAll("tarjeta-stat-icono-glifo", "tarjeta-stat-icono-glifo-" + claveColor);

        StackPane icono = new StackPane(lblGlifo);
        icono.getStyleClass().addAll("tarjeta-stat-icono", "tarjeta-stat-icono-" + claveColor);
        icono.setPrefSize(38, 38);
        icono.setMinSize(38, 38);
        icono.setMaxSize(38, 38);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("tarjeta-stat-etiqueta");
        valorLabel.getStyleClass().add("tarjeta-stat-valor");
        descripcionLabel.getStyleClass().add("tarjeta-stat-sub");
        descripcionLabel.setWrapText(true);

        VBox tarjeta = new VBox(4, icono, valorLabel, lblTitulo, descripcionLabel);
        tarjeta.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(icono, new Insets(0, 0, 10, 0));
        tarjeta.getStyleClass().addAll("tarjeta-stat", "tarjeta-stat-" + claveColor);
        tarjeta.setPadding(new Insets(20));
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        animarHover(tarjeta);
        return tarjeta;
    }

    /** Mismo recurso que {@code EscritorioScreen#animarHoverTarjeta}: JavaFX no anima solo un
     * cambio de clase CSS, así que la "levantada" de la tarjeta se hace a mano con una
     * TranslateTransition disparada por mouse-entered/exited; la sombra más marcada sí es
     * instantánea, vía la clase ".tarjeta-stat-hover" (ver theme.css). */
    private static void animarHover(Region tarjeta) {
        TranslateTransition subir = new TranslateTransition(Duration.millis(140), tarjeta);
        subir.setToY(-4);
        TranslateTransition bajar = new TranslateTransition(Duration.millis(140), tarjeta);
        bajar.setToY(0);
        tarjeta.setOnMouseEntered(evt -> {
            bajar.stop();
            subir.playFromStart();
            tarjeta.getStyleClass().add("tarjeta-stat-hover");
        });
        tarjeta.setOnMouseExited(evt -> {
            subir.stop();
            bajar.playFromStart();
            tarjeta.getStyleClass().remove("tarjeta-stat-hover");
        });
    }
}
