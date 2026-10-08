package vistas.javafx;

import java.io.File;
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Ventanas de "elegir archivo / carpeta" hechas en JavaFX, para usar desde las pantallas JavaFX.
 *
 * <p>Por qué existe (2026-10-08): antes esas pantallas abrían el selector de Swing
 * ({@code utilidades.SelectorArchivoUtil}, un {@code JFileChooser}) a través de
 * {@link PuenteEDT}. Eso deja el hilo de JavaFX esperando todo el tiempo que el selector esté
 * abierto, y además el {@code JFileChooser} de Windows tarda bastante la primera vez (recorre
 * Escritorio, Documentos, unidades de red...). Resultado: Windows mostraba "Java(TM) Platform SE
 * binary no responde" al tocar "Elegir imagen..." en Configuración. El selector de JavaFX es el
 * nativo de Windows y corre en el mismo hilo de la pantalla, sin bloquear nada.</p>
 */
public final class SelectorArchivoFx {

    private SelectorArchivoFx() {
    }

    /** Ventana dueña del selector (para que aparezca adelante), a partir de cualquier nodo de la pantalla. */
    private static Window duenio(Node nodo) {
        if (nodo == null || nodo.getScene() == null) {
            return null;
        }
        return nodo.getScene().getWindow();
    }

    public static File elegirImagenLogo(Node desde) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir logo del laboratorio");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagen (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        return selector.showOpenDialog(duenio(desde));
    }

    public static File elegirCarpeta(Node desde, String titulo) {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle(titulo);
        return selector.showDialog(duenio(desde));
    }

    /** Destino para guardar un .csv; le agrega ".csv" si no lo tiene. null si se cancela. */
    public static File elegirDestinoCsv(Node desde, String nombreSugerido) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar como");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));
        File archivo = selector.showSaveDialog(duenio(desde));
        if (archivo != null && !archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        return archivo;
    }

    /** Elegir un archivo .sql (una copia de seguridad o un dump de Workbench) para restaurar. */
    public static File elegirArchivoSql(Node desde, File carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir la copia de seguridad (.sql)");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Copia de seguridad (*.sql)", "*.sql"));
        if (carpetaInicial != null && carpetaInicial.isDirectory()) {
            selector.setInitialDirectory(carpetaInicial);
        }
        return selector.showOpenDialog(duenio(desde));
    }

    /** Elegir dónde guardar un PDF; le agrega ".pdf" si no lo tiene. null si se cancela. */
    public static File elegirDestinoPdf(Node desde, String nombreSugerido) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar PDF");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        File archivo = selector.showSaveDialog(duenio(desde));
        if (archivo != null && !archivo.getName().toLowerCase().endsWith(".pdf")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".pdf");
        }
        return archivo;
    }
}
