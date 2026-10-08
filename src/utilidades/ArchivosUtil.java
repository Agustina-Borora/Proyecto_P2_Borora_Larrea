package utilidades;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Abrir un PDF, o mostrarlo marcado en su carpeta del explorador de Windows. Todo esto lanza
 * programas del sistema operativo, que a veces tardan: llamarlo desde un hilo en segundo plano
 * (ver {@code vistas.javafx.TareaFondo}), nunca desde la pantalla directamente.
 */
public final class ArchivosUtil {

    private ArchivosUtil() {
    }

    /** Abre el archivo con el programa predeterminado (el lector de PDF). */
    public static void abrir(File archivo) throws IOException {
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("No se encontró el archivo.");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("Esta computadora no permite abrir archivos desde el sistema.");
        }
        Desktop.getDesktop().open(archivo);
    }

    /**
     * Abre la carpeta del archivo en el explorador, con el archivo ya marcado (en Windows). Así
     * queda a mano para arrastrarlo a un chat o a donde haga falta.
     */
    public static void mostrarEnCarpeta(File archivo) throws IOException {
        if (archivo == null) {
            return;
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows && archivo.isFile()) {
            new ProcessBuilder("explorer.exe", "/select,", archivo.getAbsolutePath()).start();
            return;
        }
        File carpeta = archivo.isDirectory() ? archivo : archivo.getParentFile();
        if (carpeta == null) {
            return;
        }
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(carpeta);
        }
    }
}
