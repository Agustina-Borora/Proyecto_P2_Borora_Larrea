package reportes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un informe ya "armado": la lista de hojas, y en cada hoja la lista exacta de cosas a dibujar
 * (textos, líneas, rectángulos e imágenes) con su posición en puntos (1 punto = 1/72 de pulgada,
 * origen arriba a la izquierda de la hoja A4).
 *
 * <p>Se arma UNA sola vez (ver {@link MaquetadorInforme} y {@link MaquetadorComprobante}) y
 * después se manda igual a los tres destinos: la vista previa en pantalla y la impresora (ver
 * {@link RenderizadorGraphics}) y el archivo PDF (ver {@link EscritorPdf}). Por eso lo que se ve
 * en la vista previa es exactamente lo que sale en el PDF y en papel.</p>
 */
public class DocumentoMaquetado {

    /** Hoja A4 en puntos. */
    public static final double ANCHO_HOJA = 595.28;
    public static final double ALTO_HOJA = 841.89;
    /** Hoja Carta (Letter), la que usa por defecto Word en muchas computadoras. */
    public static final double ANCHO_CARTA = 612;
    public static final double ALTO_CARTA = 792;

    /** Milímetros a puntos. */
    public static double mm(double milimetros) {
        return milimetros * 72.0 / 25.4;
    }

    private final String titulo;
    private final List<Pagina> paginas = new ArrayList<>();

    public DocumentoMaquetado(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<Pagina> getPaginas() {
        return Collections.unmodifiableList(paginas);
    }

    /** Hoja nueva tamaño A4. */
    public Pagina nuevaPagina() {
        return nuevaPagina(ANCHO_HOJA, ALTO_HOJA);
    }

    /** Hoja nueva del tamaño indicado (en puntos). */
    public Pagina nuevaPagina(double ancho, double alto) {
        Pagina pagina = new Pagina(ancho, alto);
        paginas.add(pagina);
        return pagina;
    }

    public int cantidadPaginas() {
        return paginas.size();
    }

    // ------------------------------------------------------------------ operaciones

    /** Algo para dibujar en una hoja. */
    public abstract static class Operacion {
    }

    /** Texto de una sola línea; {@code y} es la línea de base (donde "apoyan" las letras). */
    public static final class OpTexto extends Operacion {
        public final double x;
        public final double y;
        public final String texto;
        public final double tamano;
        public final boolean negrita;
        public final boolean cursiva;
        public final int color;
        /** true = letra clásica (Times), false = Helvetica/Arial. */
        public final boolean serif;

        OpTexto(double x, double y, String texto, double tamano, boolean negrita, boolean cursiva, int color,
                boolean serif) {
            this.serif = serif;
            this.x = x;
            this.y = y;
            this.texto = texto;
            this.tamano = tamano;
            this.negrita = negrita;
            this.cursiva = cursiva;
            this.color = color;
        }
    }

    public static final class OpLinea extends Operacion {
        public final double x1;
        public final double y1;
        public final double x2;
        public final double y2;
        public final double grosor;
        public final int color;

        OpLinea(double x1, double y1, double x2, double y2, double grosor, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.grosor = grosor;
            this.color = color;
        }
    }

    /** Rectángulo relleno ({@code relleno} != null) y/o con borde ({@code borde} != null). */
    public static final class OpRect extends Operacion {
        public final double x;
        public final double y;
        public final double ancho;
        public final double alto;
        public final Integer relleno;
        public final Integer borde;
        public final double grosorBorde;

        OpRect(double x, double y, double ancho, double alto, Integer relleno, Integer borde, double grosorBorde) {
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.relleno = relleno;
            this.borde = borde;
            this.grosorBorde = grosorBorde;
        }
    }

    public static final class OpImagen extends Operacion {
        public final double x;
        public final double y;
        public final double ancho;
        public final double alto;
        public final BufferedImage imagen;

        OpImagen(double x, double y, double ancho, double alto, BufferedImage imagen) {
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.imagen = imagen;
        }
    }

    // ------------------------------------------------------------------ página

    public static final class Pagina {
        private final List<Operacion> operaciones = new ArrayList<Operacion>();
        private final double ancho;
        private final double alto;
        /** Si es true, los textos de esta hoja van con letra clásica (Times) salvo que se indique otra cosa. */
        private boolean serif;

        Pagina(double ancho, double alto) {
            this.ancho = ancho;
            this.alto = alto;
        }

        public double getAncho() {
            return ancho;
        }

        public double getAlto() {
            return alto;
        }

        public boolean isSerif() {
            return serif;
        }

        /** Cambia la letra con la que se escriben los textos de esta hoja (Times o Helvetica). */
        public void setSerif(boolean serif) {
            this.serif = serif;
        }

        public List<Operacion> getOperaciones() {
            return Collections.unmodifiableList(operaciones);
        }

        /**
         * Agrega un texto de una línea. Los superíndices que la letra del PDF no tiene (⁶, ⁴...)
         * se separan solos en un texto aparte, más chico y levantado. Devuelve la X donde
         * terminó (para seguir escribiendo en la misma línea).
         */
        public double texto(double x, double y, String texto, double tamano, boolean negrita, boolean cursiva, int color) {
            String limpio = TextoPdf.normalizar(texto).replace('\n', ' ');
            double cursor = x;
            StringBuilder normal = new StringBuilder();
            for (int i = 0; i < limpio.length(); i++) {
                char c = limpio.charAt(i);
                if (TextoPdf.esSuperindiceDibujado(c)) {
                    cursor = volcar(normal, cursor, y, tamano, negrita, cursiva, color);
                    double tamSup = tamano * TextoPdf.ESCALA_SUPERINDICE;
                    String base = String.valueOf(TextoPdf.baseSuperindice(c));
                    operaciones.add(new OpTexto(cursor, y - tamano * 0.36, base, tamSup, negrita, cursiva, color, serif));
                    cursor += TextoPdf.anchoCaracter(c, tamano, negrita, cursiva, serif);
                } else {
                    normal.append(c);
                }
            }
            return volcar(normal, cursor, y, tamano, negrita, cursiva, color);
        }

        private double volcar(StringBuilder normal, double x, double y, double tamano, boolean negrita, boolean cursiva, int color) {
            if (normal.length() == 0) {
                return x;
            }
            String t = normal.toString();
            operaciones.add(new OpTexto(x, y, t, tamano, negrita, cursiva, color, serif));
            normal.setLength(0);
            return x + TextoPdf.ancho(t, tamano, negrita, cursiva, serif);
        }

        /** Texto alineado a la derecha: termina justo en {@code xDerecha}. */
        public void textoDerecha(double xDerecha, double y, String texto, double tamano, boolean negrita, boolean cursiva, int color) {
            texto(xDerecha - TextoPdf.ancho(texto, tamano, negrita, cursiva, serif), y, texto, tamano, negrita, cursiva, color);
        }

        /** Texto centrado en {@code xCentro}. */
        public void textoCentrado(double xCentro, double y, String texto, double tamano, boolean negrita, boolean cursiva, int color) {
            texto(xCentro - TextoPdf.ancho(texto, tamano, negrita, cursiva, serif) / 2, y, texto, tamano, negrita, cursiva, color);
        }

        /** Un renglón ya cortado de piezas de color (ver {@link TextoPdf#envolver}). */
        public double piezas(double x, double y, List<TextoPdf.Pieza> renglon, double tamano, boolean negrita, int colorPorDefecto) {
            double cursor = x;
            for (TextoPdf.Pieza p : renglon) {
                cursor = texto(cursor, y, p.texto, tamano, negrita, false, p.color == null ? colorPorDefecto : p.color);
            }
            return cursor;
        }

        /** Texto con una línea abajo (subrayado), como los títulos del informe clásico. */
        public double textoSubrayado(double x, double y, String texto, double tamano, boolean negrita, int color) {
            double fin = texto(x, y, texto, tamano, negrita, false, color);
            linea(x, y + tamano * 0.14, fin, y + tamano * 0.14, Math.max(0.5, tamano * 0.06), color);
            return fin;
        }

        public void linea(double x1, double y1, double x2, double y2, double grosor, int color) {
            operaciones.add(new OpLinea(x1, y1, x2, y2, grosor, color));
        }

        public void rect(double x, double y, double ancho, double alto, Integer relleno, Integer borde, double grosorBorde) {
            operaciones.add(new OpRect(x, y, ancho, alto, relleno, borde, grosorBorde));
        }

        public void imagen(double x, double y, double ancho, double alto, BufferedImage imagen) {
            if (imagen != null) {
                operaciones.add(new OpImagen(x, y, ancho, alto, imagen));
            }
        }
    }
}
