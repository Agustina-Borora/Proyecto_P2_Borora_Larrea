package vistas.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Mes 8 (rediseño visual), CUARTA vuelta en el arreglo del cartel de "Bienvenido" del login: las
 * tres primeras vueltas intentaron arreglar el {@link javafx.scene.control.Alert} nativo desde
 * afuera (reajustar tamaño, forzar el alto del texto, marcarlo resizable) sin ningún efecto. La
 * primera versión de esta clase (armar la ventana a mano con un {@link Stage} normal,
 * {@code DECORATED}) sacó el hueco vacío, pero la clienta mandó una captura mostrando una franja
 * de color vacía arriba del ícono -- eso es el propio marco nativo de la ventana (la barra de
 * título de Windows), que varía de look según el tema/acento del sistema operativo de cada PC y
 * no se puede controlar desde JavaFX.
 *
 * <p>Esta vuelta saca el marco nativo del medio directamente: la ventana es {@code
 * StageStyle.TRANSPARENT} (sin ningún borde ni barra de título del sistema operativo) y toda la
 * tarjeta -- bordes redondeados, sombra, botón de cerrar -- la dibuja JavaFX desde cero, mismo
 * lenguaje visual que ya usa el resto de la app (tarjeta blanca redondeada con sombra, igual que
 * {@code .tarjeta} en {@code theme.css}). Así se ve igual en cualquier PC con cualquier tema de
 * Windows, en vez de depender de qué barra de título decida dibujar el sistema operativo -- y de
 * paso se parece más a un modal "web" que a un cartel de escritorio clásico, que era uno de los
 * pedidos de la clienta ("que no quede como si se notara que fue hecho en Java").</p>
 */
public final class AvisoStage {

    private static final double ANCHO = 440;

    private AvisoStage() {
    }

    public enum Tipo { EXITO, ADVERTENCIA, ERROR }

    /** Muestra el aviso y bloquea hasta que se cierra (igual que {@code Alert#showAndWait()}). */
    public static void mostrar(Tipo tipo, String titulo, String mensaje) {
        String clave;
        String glifo;
        switch (tipo) {
            case ERROR:
                clave = "rojo";
                glifo = "✕";
                break;
            case ADVERTENCIA:
                clave = "ambar";
                glifo = "⚠";
                break;
            default:
                clave = "verde";
                glifo = "✓";
                break;
        }

        Stage ventana = new Stage(StageStyle.TRANSPARENT);

        Label glifoLbl = new Label(glifo);
        glifoLbl.getStyleClass().addAll("alerta-icono-glifo", "alerta-icono-glifo-" + clave);
        StackPane icono = new StackPane(glifoLbl);
        icono.getStyleClass().addAll("alerta-icono", "alerta-icono-" + clave);
        icono.setMinSize(44, 44);
        icono.setPrefSize(44, 44);
        icono.setMaxSize(44, 44);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("aviso-stage-titulo");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonCerrar = new Button("✕");
        botonCerrar.getStyleClass().add("aviso-stage-cerrar");
        botonCerrar.setOnAction(evt -> ventana.close());

        HBox encabezado = new HBox(12, icono, lblTitulo, espaciador, botonCerrar);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        texto.getStyleClass().add("aviso-stage-texto");

        Button botonOk = new Button("Aceptar");
        botonOk.getStyleClass().add("boton-primario");
        botonOk.setMaxWidth(Double.MAX_VALUE);
        botonOk.setDefaultButton(true);
        botonOk.setOnAction(evt -> ventana.close());

        VBox caja = new VBox(18, encabezado, texto, botonOk);
        caja.setPadding(new Insets(22, 26, 26, 26));
        caja.getStyleClass().add("aviso-stage-caja");
        caja.setPrefWidth(ANCHO);
        caja.setMaxWidth(ANCHO);
        // Sin esto, el StackPane de abajo estira la tarjeta para llenar todo el alto de la
        // ventana (260) en vez de dejarla con el alto justo para su contenido.
        caja.setMaxHeight(Region.USE_PREF_SIZE);

        // El fondo de la ventana en sí queda transparente (StageStyle.TRANSPARENT) -- la única
        // forma visible es la tarjeta de #aviso-stage-caja, con sus propios bordes redondeados y
        // sombra (ver theme.css), flotando sobre el escritorio en vez de encerrada en un marco
        // cuadrado nativo.
        StackPane raiz = new StackPane(caja);
        raiz.setStyle("-fx-background-color: transparent;");

        Scene escena = new Scene(raiz, ANCHO + 40, 260);
        escena.setFill(Color.TRANSPARENT);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());

        ventana.setTitle(titulo);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setScene(escena);
        ventana.centerOnScreen();
        VentanaUtil.animarAparicion(ventana, caja);
        ventana.showAndWait();
    }
}
