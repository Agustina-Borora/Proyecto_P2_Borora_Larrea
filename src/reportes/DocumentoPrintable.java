package reportes;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

/**
 * Adapta un {@link DocumentoMaquetado} a la impresión de Java: cada página que pide la
 * impresora se dibuja con {@link RenderizadorGraphics}, igual que en la vista previa y en el PDF.
 */
public class DocumentoPrintable implements Printable {

    private final DocumentoMaquetado documento;

    public DocumentoPrintable(DocumentoMaquetado documento) {
        this.documento = documento;
    }

    public DocumentoMaquetado getDocumento() {
        return documento;
    }

    @Override
    public int print(Graphics graphics, PageFormat formato, int indicePagina) {
        if (indicePagina < 0 || indicePagina >= documento.cantidadPaginas()) {
            return NO_SUCH_PAGE;
        }
        // El maquetado ya trae sus propios márgenes, así que se dibuja desde la esquina del papel
        // (no desde el área imprimible): así queda en el mismo lugar que en el PDF.
        Graphics2D g2 = (Graphics2D) graphics.create();
        RenderizadorGraphics.dibujar(g2, documento.getPaginas().get(indicePagina));
        g2.dispose();
        return PAGE_EXISTS;
    }
}
