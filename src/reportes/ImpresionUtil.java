package reportes;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * Manda un informe ya armado ({@link DocumentoMaquetado}) a la impresora. Ahí se elige la
 * impresora real -- para guardar como PDF ya NO hace falta "Microsoft Print to PDF": el sistema
 * guarda el PDF solo (ver {@link InformesPdfService}).
 *
 * <p>Hay que llamarla desde el Event Dispatch Thread de Swing (por ejemplo envuelta en
 * {@code SwingUtilities.invokeLater} si se llama desde una pantalla JavaFX), porque el diálogo
 * de impresión es de AWT.</p>
 */
public final class ImpresionUtil {

    private ImpresionUtil() {
    }

    public static void imprimir(DocumentoMaquetado documento, String nombreTrabajo) {
        PrinterJob trabajo = PrinterJob.getPrinterJob();
        trabajo.setJobName(nombreTrabajo);
        PageFormat formato = trabajo.validatePage(formatoHoja(trabajo, documento));
        trabajo.setPrintable(new DocumentoPrintable(documento), formato);
        try {
            if (trabajo.printDialog()) {
                trabajo.print();
            }
        } catch (PrinterException e) {
            dao.Mensajes.error("Error al imprimir", e);
        }
    }

    /** Hoja vertical del mismo tamaño (A4 o Carta) con la que se armó el informe y el PDF. */
    private static PageFormat formatoHoja(PrinterJob trabajo, DocumentoMaquetado documento) {
        double ancho = DocumentoMaquetado.ANCHO_HOJA;
        double alto = DocumentoMaquetado.ALTO_HOJA;
        if (documento.cantidadPaginas() > 0) {
            ancho = documento.getPaginas().get(0).getAncho();
            alto = documento.getPaginas().get(0).getAlto();
        }
        PageFormat formato = trabajo.defaultPage();
        Paper papel = new Paper();
        papel.setSize(ancho, alto);
        // Área imprimible casi completa: los márgenes reales ya vienen dentro del informe.
        double borde = DocumentoMaquetado.mm(4);
        papel.setImageableArea(borde, borde, ancho - 2 * borde, alto - 2 * borde);
        formato.setPaper(papel);
        formato.setOrientation(PageFormat.PORTRAIT);
        return formato;
    }
}
