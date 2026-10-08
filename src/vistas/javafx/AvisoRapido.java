package vistas.javafx;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Aviso de "listo, ya se hizo" que NO hay que cerrar: aparece con una animación corta (el tilde
 * verde "salta"), se queda un momento para poder leerlo y se va solo. Reemplaza a los carteles de
 * "Guardado correctamente" con botón Aceptar, que obligaban a hacer un clic de más cada vez.
 *
 * <p>Si se hace clic sobre el aviso o se aprieta una tecla, se cierra antes. El código que lo
 * llama sigue recién cuando el aviso se fue (igual que antes con el botón Aceptar), así que el
 * orden de las cosas no cambia: por ejemplo, después del "Bienvenido" entra al sistema solo.</p>
 *
 * <p>Los errores y las advertencias siguen siendo ventanas con botón, porque ahí sí hace falta que
 * se lean con calma.</p>
 */
public final class AvisoRapido {

    private static final double ANCHO = 380;

    private AvisoRapido() {
    }

    /** Muestra el aviso y espera a que se vaya solo (algo más de un segundo, más si el texto es largo). */
    public static void mostrar(String titulo, String mensaje) {
        mostrar(null, titulo, mensaje);
    }

    public static void mostrar(Window duena, String titulo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(duena, titulo, mensaje));
            return;
        }
        Stage ventana = new Stage(StageStyle.TRANSPARENT);

        Label tilde = new Label("✓");
        tilde.getStyleClass().addAll("alerta-icono-glifo", "alerta-icono-glifo-verde");
        tilde.setStyle("-fx-font-size: 30px;");
        StackPane icono = new StackPane(tilde);
        icono.getStyleClass().addAll("alerta-icono", "alerta-icono-verde");
        icono.setMinSize(64, 64);
        icono.setPrefSize(64, 64);
        icono.setMaxSize(64, 64);
        icono.setStyle("-fx-background-radius: 32;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("aviso-stage-titulo");
        lblTitulo.setWrapText(true);
        lblTitulo.setAlignment(Pos.CENTER);

        VBox caja = new VBox(12, icono, lblTitulo);
        if (mensaje != null && !mensaje.trim().isEmpty()) {
            Label texto = new Label(mensaje);
            texto.setWrapText(true);
            texto.getStyleClass().add("aviso-stage-texto");
            texto.setStyle("-fx-text-alignment: center;");
            texto.setAlignment(Pos.CENTER);
            caja.getChildren().add(texto);
        }
        caja.setAlignment(Pos.CENTER);
        caja.setPadding(new Insets(24, 28, 24, 28));
        caja.getStyleClass().add("aviso-stage-caja");
        caja.setPrefWidth(ANCHO);
        caja.setMaxWidth(ANCHO);
        caja.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane raiz = new StackPane(caja);
        raiz.setStyle("-fx-background-color: transparent;");
        raiz.setPadding(new Insets(20));

        Scene escena = new Scene(raiz);
        escena.setFill(Color.TRANSPARENT);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());

        ventana.setTitle(titulo);
        if (duena != null) {
            ventana.initOwner(duena);
        }
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setScene(escena);

        // Entrada: la tarjeta aparece y crece un poquito; el tilde "salta".
        caja.setOpacity(0);
        caja.setScaleX(0.92);
        caja.setScaleY(0.92);
        icono.setScaleX(0.3);
        icono.setScaleY(0.3);
        FadeTransition aparecer = new FadeTransition(Duration.millis(180), caja);
        aparecer.setToValue(1);
        ScaleTransition crecer = new ScaleTransition(Duration.millis(200), caja);
        crecer.setToX(1);
        crecer.setToY(1);
        crecer.setInterpolator(Interpolator.EASE_OUT);
        // El tilde crece un poco de más y vuelve a su tamaño ("salta").
        ScaleTransition saltoArriba = new ScaleTransition(Duration.millis(220), icono);
        saltoArriba.setToX(1.15);
        saltoArriba.setToY(1.15);
        saltoArriba.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition saltoAbajo = new ScaleTransition(Duration.millis(130), icono);
        saltoAbajo.setToX(1);
        saltoAbajo.setToY(1);
        saltoAbajo.setInterpolator(Interpolator.EASE_IN);
        SequentialTransition salto = new SequentialTransition(saltoArriba, saltoAbajo);
        ParallelTransition entrada = new ParallelTransition(aparecer, crecer, salto);

        int largo = (titulo == null ? 0 : titulo.length()) + (mensaje == null ? 0 : mensaje.length());
        double msVisible = Math.min(4000, 1100 + largo * 25);
        PauseTransition espera = new PauseTransition(Duration.millis(msVisible));

        FadeTransition salida = new FadeTransition(Duration.millis(260), caja);
        salida.setToValue(0);

        SequentialTransition todo = new SequentialTransition(entrada, espera, salida);
        todo.setOnFinished(e -> ventana.close());
        Runnable cerrarYa = () -> {
            if (todo.getCurrentTime().lessThan(entrada.getTotalDuration().add(Duration.millis(msVisible)))) {
                todo.jumpTo(entrada.getTotalDuration().add(Duration.millis(msVisible)));
            }
        };
        raiz.setOnMouseClicked(e -> cerrarYa.run());
        escena.setOnKeyPressed(e -> cerrarYa.run());

        ventana.setOnShown(e -> {
            centrar(ventana, duena);
            todo.play();
        });
        ventana.showAndWait();
    }

    private static void centrar(Stage ventana, Window duena) {
        if (duena != null && duena.getWidth() > 0) {
            ventana.setX(duena.getX() + (duena.getWidth() - ventana.getWidth()) / 2);
            ventana.setY(duena.getY() + (duena.getHeight() - ventana.getHeight()) / 2);
        } else {
            ventana.centerOnScreen();
        }
    }
}
