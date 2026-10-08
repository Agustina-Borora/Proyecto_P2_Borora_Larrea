package vistas.javafx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Mes 8 (rediseño visual): hasta ahora cada {@code new Alert(...)} de la app se mostraba con el
 * aspecto por defecto de JavaFX -- una ventanita gris con el ícono genérico del sistema
 * operativo, que no tiene nada que ver con el resto del diseño (tarjetas blancas redondeadas,
 * paleta verde, botones con degradé). Era justamente el cartelito "¡Bienvenido!" del login uno
 * de los puntos marcados como "se sigue viendo como Java Swing, no como diseño web".
 *
 * <p>Esta clase le da a cualquier {@link Alert} ya existente el mismo lenguaje visual que ya usa
 * el resto de la app: tarjeta blanca redondeada (reutiliza {@code theme.css}), un ícono de color
 * según el tipo de aviso (verde para info/éxito, ámbar para advertencia, rojo para error, azul
 * para una confirmación) en vez del ícono del sistema operativo, y los botones con el mismo
 * estilo que ya usa el resto de los formularios ({@code boton-primario} para la acción principal,
 * un estilo más discreto para "Cancelar"/"No").</p>
 *
 * <p><b>Uso:</b> no hace falta cambiar cómo se arma cada alerta ni qué hace cada botón -- en
 * todos los lugares de la app que ya tenían un {@code new Alert(...)} alcanza con agregar UNA
 * línea antes de {@code showAndWait()}:</p>
 *
 * <pre>
 *     Alert alerta = new Alert(AlertType.WARNING, "mensaje...");
 *     AlertaUtil.estilizar(alerta);
 *     alerta.showAndWait();
 * </pre>
 */
public final class AlertaUtil {

    private AlertaUtil() {
    }

    public static void estilizar(Alert alerta) {
        if (alerta == null) {
            return;
        }
        DialogPane panel = alerta.getDialogPane();
        if (panel == null) {
            return;
        }
        panel.getStylesheets().add(EstiloApp.hojaDeEstilos());
        panel.getStyleClass().add("alerta-dialogo");

        AlertType tipo = alerta.getAlertType();
        String clave;
        String glifo;
        switch (tipo) {
            case ERROR:
                clave = "rojo";
                glifo = "✕";
                break;
            case WARNING:
                clave = "ambar";
                glifo = "⚠";
                break;
            case CONFIRMATION:
                clave = "azul";
                glifo = "?";
                break;
            default:
                clave = "verde";
                glifo = "✓";
                break;
        }

        Label glifoLbl = new Label(glifo);
        glifoLbl.getStyleClass().addAll("alerta-icono-glifo", "alerta-icono-glifo-" + clave);
        StackPane icono = new StackPane(glifoLbl);
        icono.getStyleClass().addAll("alerta-icono", "alerta-icono-" + clave);
        icono.setPrefSize(44, 44);
        icono.setMinSize(44, 44);
        icono.setMaxSize(44, 44);
        panel.setGraphic(icono);

        // Botón afirmativo (OK en un aviso simple, SI en una confirmación) con el mismo estilo
        // degradado que "boton-primario" en el resto de la app; el resto (Cancelar/No) con un
        // estilo discreto -- misma jerarquía visual "acción principal vs. secundaria" que ya usan
        // los formularios.
        for (ButtonType bt : panel.getButtonTypes()) {
            Object nodo = panel.lookupButton(bt);
            if (!(nodo instanceof Button)) {
                continue;
            }
            Button boton = (Button) nodo;
            boolean esAfirmativo = bt == ButtonType.OK || bt == ButtonType.YES;
            boton.getStyleClass().add(esAfirmativo ? "boton-primario" : "boton-secundario");
        }

        panel.setMinHeight(Region.USE_PREF_SIZE);
        panel.setMinWidth(Region.USE_PREF_SIZE);
        achicarAlHuecoReal(alerta, panel);
        centrarEnPantalla(panel);
    }

    /**
     * Mes 8 -- TERCER intento de arreglo del hueco vacío entre el texto y los botones ("en la
     * alerta no muestra nada abajo" / "sigue igual de muy arriba, no está centrado"). Las dos
     * vueltas anteriores (enganchar {@code Stage#setOnShown} + {@code sizeToScene()}, después
     * forzar el {@code minHeight} del {@link Label} de contenido con {@code Node#lookup}) no
     * tuvieron ningún efecto visible.
     *
     * <p>Esta vuelta ataca la causa raíz por el otro lado: en JavaFX 8 hay un bug conocido donde
     * un {@code Dialog}/{@code Alert} NO resizable (que es el valor por defecto) no vuelve a
     * calcular bien su tamaño una vez que ya se armó una primera vez -- se queda con un alto
     * pensado para el caso general en vez de achicarse al contenido real, sea cual sea el CSS o el
     * {@code minHeight} que se le toque después (de ahí que los dos intentos anteriores, que sí
     * tocaban el contenido, no cambiaran nada: el problema nunca fue el contenido, fue que el
     * diálogo no estaba resizable). Marcándolo {@code setResizable(true)} se lo saca de ese camino
     * con el bug y pasa a recalcular su tamaño como cualquier otra ventana de JavaFX. Se deja
     * puesto el ajuste del {@link Label} de contenido igual (no molesta, y ayuda si el mensaje
     * ocupa más de un renglón).</p>
     */
    private static void achicarAlHuecoReal(Alert alerta, DialogPane panel) {
        alerta.setResizable(true);
        Node contenido = panel.lookup(".content.label");
        if (contenido instanceof Label) {
            ((Label) contenido).setMinHeight(Region.USE_PREF_SIZE);
        }
    }

    /**
     * Mes 8: ningún {@code Alert} de la app le fija un dueño ({@code initOwner}) a su ventana, así
     * que JavaFX la ubica con su posición por defecto en vez de centrada en la pantalla -- "sigue
     * igual de muy arriba, no está centrado". El {@link Stage} real de un Alert recién existe una
     * vez que se le asignó una {@code Scene} (y esa Scene recién tiene {@code Window} una vez
     * mostrada), así que hace falta esperar esos dos pasos con un par de "oyentes" antes de poder
     * engancharse a su {@code setOnShown} real y centrarla ahí -- mismo mecanismo que ya usa
     * {@code VentanaUtil#animarAparicion} con otras ventanas, y que ahí sí funciona.
     */
    private static void centrarEnPantalla(DialogPane panel) {
        panel.sceneProperty().addListener((obsEscena, escenaVieja, escena) -> {
            if (escena == null) {
                return;
            }
            escena.windowProperty().addListener((obsVentana, ventanaVieja, ventana) -> {
                if (ventana instanceof Stage) {
                    ((Stage) ventana).setOnShown(evt -> Platform.runLater(((Stage) ventana)::centerOnScreen));
                }
            });
        });
    }
}
