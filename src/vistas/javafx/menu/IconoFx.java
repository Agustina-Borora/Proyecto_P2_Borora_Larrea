package vistas.javafx.menu;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Carga los íconos de {@code /icon/*.png} como {@link ImageView} de JavaFX, con la misma técnica
 * de recoloreo píxel por píxel (conservando el canal alpha original) que usa
 * {@link modelo.Model_Menu#toIcon(java.awt.Color)} en la versión Swing del menú -- así el sidebar
 * nuevo, hecho en JavaFX puro, se ve idéntico al viejo.
 */
public final class IconoFx {

    private IconoFx() {
    }

    /**
     * Carga el ícono con sus colores originales (sin recoloreo), escalado a {@code tamano}px.
     */
    public static ImageView vista(String nombreIcono, double tamano) {
        return vista(nombreIcono, tamano, 0);
    }

    /**
     * Carga el ícono y, si {@code colorRgb} no es 0, lo tiñe entero con ese color (formato
     * {@code 0xRRGGBB}), escalado a {@code tamano}px. Si el recurso no existe (id de menú sin
     * ícono asociado, como pasa hoy con "Cerrar Sesion"), devuelve un {@code ImageView} vacío en
     * vez de romper toda la pantalla.
     */
    public static ImageView vista(String nombreIcono, double tamano, int colorRgb) {
        Image img = imagen(nombreIcono, colorRgb);
        ImageView vista = img == null ? new ImageView() : new ImageView(img);
        vista.setFitWidth(tamano);
        vista.setFitHeight(tamano);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        return vista;
    }

    /**
     * Devuelve la imagen del ícono, teñida con {@code colorRgb} (0xRRGGBB) si es distinto de 0,
     * o con sus colores originales si es 0. Devuelve {@code null} (en vez de tirar una excepción)
     * si el recurso {@code /icon/<nombreIcono>.png} no existe.
     */
    public static Image imagen(String nombreIcono, int colorRgb) {
        java.io.InputStream recurso = IconoFx.class.getResourceAsStream("/icon/" + nombreIcono + ".png");
        if (recurso == null) {
            return null;
        }
        Image original = new Image(recurso);
        return colorRgb == 0 ? original : tenir(original, colorRgb);
    }

    private static Image tenir(Image original, int colorRgb) {
        int ancho = (int) original.getWidth();
        int alto = (int) original.getHeight();
        WritableImage resultado = new WritableImage(ancho, alto);
        PixelReader lector = original.getPixelReader();
        PixelWriter escritor = resultado.getPixelWriter();

        int r = (colorRgb >> 16) & 0xFF;
        int g = (colorRgb >> 8) & 0xFF;
        int b = colorRgb & 0xFF;

        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                int argb = lector.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                escritor.setArgb(x, y, alpha == 0 ? 0 : (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return resultado;
    }
}
