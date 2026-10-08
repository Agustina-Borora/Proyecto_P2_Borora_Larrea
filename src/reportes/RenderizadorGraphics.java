package reportes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja una hoja de un {@link DocumentoMaquetado} sobre un {@link Graphics2D}: lo usan la
 * impresora (ver {@link DocumentoPrintable}) y la vista previa en pantalla (ver
 * {@link #aImagen}). Dibuja exactamente lo mismo que después queda en el PDF.
 */
public final class RenderizadorGraphics {

    private static final Map<String, Font> FUENTES = new HashMap<>();

    private RenderizadorGraphics() {
    }

    /** Dibuja la hoja con el origen (0,0) en la esquina superior izquierda del papel. */
    public static void dibujar(Graphics2D g2, DocumentoMaquetado.Pagina pagina) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        for (DocumentoMaquetado.Operacion op : pagina.getOperaciones()) {
            if (op instanceof DocumentoMaquetado.OpRect) {
                DocumentoMaquetado.OpRect r = (DocumentoMaquetado.OpRect) op;
                Rectangle2D.Double forma = new Rectangle2D.Double(r.x, r.y, r.ancho, r.alto);
                if (r.relleno != null) {
                    g2.setColor(new Color(r.relleno));
                    g2.fill(forma);
                }
                if (r.borde != null) {
                    g2.setColor(new Color(r.borde));
                    g2.setStroke(new BasicStroke((float) r.grosorBorde));
                    g2.draw(forma);
                }
            } else if (op instanceof DocumentoMaquetado.OpLinea) {
                DocumentoMaquetado.OpLinea l = (DocumentoMaquetado.OpLinea) op;
                g2.setColor(new Color(l.color));
                g2.setStroke(new BasicStroke((float) l.grosor, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g2.draw(new Line2D.Double(l.x1, l.y1, l.x2, l.y2));
            } else if (op instanceof DocumentoMaquetado.OpTexto) {
                DocumentoMaquetado.OpTexto t = (DocumentoMaquetado.OpTexto) op;
                g2.setColor(new Color(t.color));
                Font f = fuente(t.tamano, t.negrita, t.cursiva, t.serif);
                g2.setFont(f);
                // Si la letra de esta computadora mide distinto que la del PDF (en Windows con
                // Arial son iguales), se estira o se achica apenas el texto para que ocupe
                // exactamente el mismo lugar -- así nunca se pisa con lo que tiene al lado.
                double esperado = TextoPdf.ancho(t.texto, t.tamano, t.negrita, t.cursiva, t.serif);
                double real = f.getStringBounds(t.texto, g2.getFontRenderContext()).getWidth();
                double escala = real > 0.5 ? esperado / real : 1.0;
                if (Math.abs(escala - 1.0) < 0.01) {
                    g2.drawString(t.texto, (float) t.x, (float) t.y);
                } else {
                    Graphics2D copia = (Graphics2D) g2.create();
                    copia.translate(t.x, t.y);
                    copia.scale(escala, 1.0);
                    copia.drawString(t.texto, 0f, 0f);
                    copia.dispose();
                }
            } else if (op instanceof DocumentoMaquetado.OpImagen) {
                DocumentoMaquetado.OpImagen im = (DocumentoMaquetado.OpImagen) op;
                Graphics2D copia = (Graphics2D) g2.create();
                copia.translate(im.x, im.y);
                copia.scale(im.ancho / im.imagen.getWidth(), im.alto / im.imagen.getHeight());
                copia.drawImage(im.imagen, 0, 0, null);
                copia.dispose();
            }
        }
    }

    /** Pasa una hoja a imagen (para la vista previa). {@code escala} 1 = 72 puntos por pulgada. */
    public static BufferedImage aImagen(DocumentoMaquetado.Pagina pagina, double escala) {
        int ancho = (int) Math.round(pagina.getAncho() * escala);
        int alto = (int) Math.round(pagina.getAlto() * escala);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imagen.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, ancho, alto);
        g2.scale(escala, escala);
        dibujar(g2, pagina);
        g2.dispose();
        return imagen;
    }

    /**
     * "SansSerif" en Windows es Arial, que tiene las mismas medidas que la Helvetica del PDF --
     * así los cortes de línea calculados con {@link TextoPdf} coinciden en pantalla y en papel.
     */
    private static synchronized Font fuente(double tamano, boolean negrita, boolean cursiva, boolean serif) {
        int estilo = (negrita ? Font.BOLD : Font.PLAIN) | (cursiva ? Font.ITALIC : Font.PLAIN);
        String clave = estilo + "|" + tamano + "|" + serif;
        Font f = FUENTES.get(clave);
        if (f == null) {
            String nombre = serif ? "Times New Roman" : "Arial";
            Font preferida = new Font(nombre, estilo, 12);
            Font base = nombre.equalsIgnoreCase(preferida.getFamily()) ? preferida
                    : new Font(serif ? "Serif" : "SansSerif", estilo, 12);
            f = base.deriveFont((float) tamano);
            FUENTES.put(clave, f);
        }
        return f;
    }
}
