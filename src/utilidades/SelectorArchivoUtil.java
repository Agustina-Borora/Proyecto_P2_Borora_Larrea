package utilidades;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Diálogos compartidos para elegir dónde guardar un archivo o carpeta exportado (CSV, respaldo,
 * etc.). {@link JFileChooser} sólo es seguro de mostrar desde el Event Dispatch Thread de
 * Swing: las pantallas Swing lo pueden llamar directo (ya están en el EDT al reaccionar a un
 * clic), las pantallas JavaFX tienen que pasar por {@link vistas.javafx.PuenteEDT#ejecutar}.
 */
public final class SelectorArchivoUtil {

    private SelectorArchivoUtil() {
    }

    /**
     * Pide un archivo destino para guardar un .csv, con {@code nombreSugerido} precargado. Si el
     * usuario cancela, devuelve null. Si el nombre elegido no termina en ".csv", se lo agrega.
     */
    public static File elegirDestinoCsv(String nombreSugerido) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar como");
        selector.setSelectedFile(new File(nombreSugerido));
        selector.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));

        int resultado = selector.showSaveDialog(null);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        return archivo;
    }

    /**
     * Pide una carpeta destino (para el respaldo, que exporta varios archivos .csv juntos). Si
     * el usuario cancela, devuelve null.
     */
    public static File elegirCarpetaDestino() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elegir carpeta para el respaldo");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int resultado = selector.showDialog(null, "Elegir esta carpeta");
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return selector.getSelectedFile();
    }

    /**
     * Pide una imagen para el logo del encabezado (Config. &gt; Laboratorio). Si el usuario
     * cancela, devuelve null.
     */
    public static File elegirImagenLogo() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elegir logo del laboratorio");
        selector.setFileFilter(new FileNameExtensionFilter("Imagen (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));

        int resultado = selector.showOpenDialog(null);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return selector.getSelectedFile();
    }
}
