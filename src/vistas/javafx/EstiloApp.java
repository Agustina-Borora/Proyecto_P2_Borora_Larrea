package vistas.javafx;

/**
 * Paleta y hoja de estilos compartida por todas las pantallas JavaFX de la app -- misma paleta
 * que ya usan las pantallas Swing (ver {@code CatalogoExamenes.VERDE_PRINCIPAL}), para que no
 * se note el salto de framework entre una pantalla y otra.
 */
public final class EstiloApp {

    private EstiloApp() {
    }

    public static final String VERDE_PRINCIPAL = "#1E5C3D";
    public static final String VERDE_FONDO = "#E4F5EA";
    public static final String GRIS_TEXTO_SUAVE = "#6E7874";
    public static final String GRIS_TEXTO_FUERTE = "#141C19";
    public static final String GRIS_BORDE = "#DCE1E6";

    public static String hojaDeEstilos() {
        return EstiloApp.class.getResource("theme.css").toExternalForm();
    }
}
