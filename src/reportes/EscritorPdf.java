package reportes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Escribe un {@link DocumentoMaquetado} como archivo PDF de verdad, sin ninguna librería externa
 * (ni .jar que agregar al proyecto) y sin pasar por el cartel de impresión de Windows.
 *
 * <p>Usa las letras estándar que trae cualquier lector de PDF (Helvetica, en normal, negrita y
 * cursiva), así el archivo queda liviano (unos pocos KB, ideal para mandar por email o
 * WhatsApp) y el texto se puede seleccionar y buscar. El logo, si hay, se guarda una sola vez
 * aunque aparezca en varias hojas.</p>
 */
public final class EscritorPdf {

    private EscritorPdf() {
    }

    /**
     * Guarda el documento en {@code destino}. Primero lo escribe en un archivo temporal en la
     * misma carpeta y después lo reemplaza de una -- así nunca queda un PDF a medio escribir.
     *
     * @throws IOException con un mensaje claro si el archivo está abierto en otro programa (por
     *                     ejemplo el lector de PDF) y Windows no deja reemplazarlo.
     */
    public static void guardar(DocumentoMaquetado documento, File destino) throws IOException {
        File carpeta = destino.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
        byte[] bytes = generar(documento);
        File temporal = new File(carpeta, "." + destino.getName() + ".tmp");
        try (OutputStream out = new FileOutputStream(temporal)) {
            out.write(bytes);
        }
        try {
            try {
                Files.move(temporal.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (AccessDeniedException e) {
            temporal.delete();
            throw new IOException("El archivo \"" + destino.getName() + "\" está abierto en otro programa "
                    + "(por ejemplo el lector de PDF). Cerralo y probá de nuevo.", e);
        } catch (FileSystemException e) {
            temporal.delete();
            throw new IOException("No se pudo guardar \"" + destino.getName() + "\" -- puede que esté abierto "
                    + "en otro programa. Cerralo y probá de nuevo.", e);
        }
    }

    /** Arma el PDF completo en memoria. */
    public static byte[] generar(DocumentoMaquetado documento) throws IOException {
        Escritor w = new Escritor();

        // Números de objeto fijos: 1 catálogo, 2 árbol de páginas, 3..10 letras, 11 info.
        int objCatalogo = 1;
        int objPaginas = 2;
        int objFuenteBase = 3;
        int objInfo = 11;
        int siguiente = 12;

        // Imágenes (cada una una sola vez, aunque se repita en varias hojas).
        Map<BufferedImage, Integer> objImagen = new IdentityHashMap<>();
        Map<BufferedImage, String> nombreImagen = new IdentityHashMap<>();
        List<BufferedImage> imagenes = new ArrayList<>();
        for (DocumentoMaquetado.Pagina p : documento.getPaginas()) {
            for (DocumentoMaquetado.Operacion op : p.getOperaciones()) {
                if (op instanceof DocumentoMaquetado.OpImagen) {
                    BufferedImage img = ((DocumentoMaquetado.OpImagen) op).imagen;
                    if (!objImagen.containsKey(img)) {
                        objImagen.put(img, siguiente);
                        siguiente += 2; // imagen + su máscara de transparencia
                        nombreImagen.put(img, "Im" + (imagenes.size() + 1));
                        imagenes.add(img);
                    }
                }
            }
        }

        List<DocumentoMaquetado.Pagina> paginas = documento.getPaginas();
        int[] objPagina = new int[paginas.size()];
        int[] objContenido = new int[paginas.size()];
        for (int i = 0; i < paginas.size(); i++) {
            objPagina[i] = siguiente++;
            objContenido[i] = siguiente++;
        }
        int totalObjetos = siguiente - 1;
        long[] posiciones = new long[totalObjetos + 1];

        w.ascii("%PDF-1.4\n");
        w.bytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        posiciones[objCatalogo] = w.posicion();
        w.ascii(objCatalogo + " 0 obj\n<< /Type /Catalog /Pages " + objPaginas + " 0 R >>\nendobj\n");

        posiciones[objPaginas] = w.posicion();
        StringBuilder kids = new StringBuilder();
        for (int n : objPagina) {
            kids.append(n).append(" 0 R ");
        }
        w.ascii(objPaginas + " 0 obj\n<< /Type /Pages /Kids [ " + kids + "] /Count " + paginas.size() + " >>\nendobj\n");

        String[] fuentes = {"Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
            "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic"};
        for (int i = 0; i < fuentes.length; i++) {
            int n = objFuenteBase + i;
            posiciones[n] = w.posicion();
            w.ascii(n + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + fuentes[i]
                    + " /Encoding /WinAnsiEncoding >>\nendobj\n");
        }

        posiciones[objInfo] = w.posicion();
        String fecha = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        w.ascii(objInfo + " 0 obj\n<< /Title " + textoUnicode(documento.getTitulo())
                + " /Producer (Sistema Laboratorio - informes PDF) /CreationDate (D:" + fecha + ") >>\nendobj\n");

        for (BufferedImage img : imagenes) {
            escribirImagen(w, img, objImagen.get(img), posiciones);
        }

        for (int i = 0; i < paginas.size(); i++) {
            DocumentoMaquetado.Pagina pagina = paginas.get(i);
            byte[] contenido = comprimir(armarContenido(pagina, nombreImagen));

            StringBuilder recursosImagen = new StringBuilder();
            for (BufferedImage img : imagenes) {
                recursosImagen.append('/').append(nombreImagen.get(img)).append(' ').append(objImagen.get(img)).append(" 0 R ");
            }

            posiciones[objPagina[i]] = w.posicion();
            w.ascii(objPagina[i] + " 0 obj\n<< /Type /Page /Parent " + objPaginas + " 0 R"
                    + " /MediaBox [0 0 " + num(pagina.getAncho()) + " " + num(pagina.getAlto()) + "]"
                    + " /Resources << /Font << " + recursosFuentes(objFuenteBase) + ">>"
                    + (imagenes.isEmpty() ? "" : " /XObject << " + recursosImagen + ">>")
                    + " >> /Contents " + objContenido[i] + " 0 R >>\nendobj\n");

            posiciones[objContenido[i]] = w.posicion();
            w.ascii(objContenido[i] + " 0 obj\n<< /Length " + contenido.length + " /Filter /FlateDecode >>\nstream\n");
            w.bytes(contenido);
            w.ascii("\nendstream\nendobj\n");
        }

        long inicioXref = w.posicion();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(totalObjetos + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int n = 1; n <= totalObjetos; n++) {
            xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", posiciones[n]));
        }
        w.ascii(xref.toString());
        w.ascii("trailer\n<< /Size " + (totalObjetos + 1) + " /Root " + objCatalogo + " 0 R /Info " + objInfo
                + " 0 R >>\nstartxref\n" + inicioXref + "\n%%EOF\n");
        return w.toByteArray();
    }

    // ------------------------------------------------------------------ contenido de una hoja

    private static String recursosFuentes(int objFuenteBase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append("/F").append(i + 1).append(' ').append(objFuenteBase + i).append(" 0 R ");
        }
        return sb.toString();
    }

    private static byte[] armarContenido(DocumentoMaquetado.Pagina pagina, Map<BufferedImage, String> nombreImagen) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double alto = pagina.getAlto();
        for (DocumentoMaquetado.Operacion op : pagina.getOperaciones()) {
            StringBuilder sb = new StringBuilder();
            if (op instanceof DocumentoMaquetado.OpRect) {
                DocumentoMaquetado.OpRect r = (DocumentoMaquetado.OpRect) op;
                double yPdf = alto - r.y - r.alto;
                sb.append("q ");
                if (r.relleno != null) {
                    sb.append(colorRelleno(r.relleno)).append(' ');
                }
                if (r.borde != null) {
                    sb.append(colorTrazo(r.borde)).append(' ').append(num(r.grosorBorde)).append(" w ");
                }
                sb.append(num(r.x)).append(' ').append(num(yPdf)).append(' ').append(num(r.ancho)).append(' ')
                        .append(num(r.alto)).append(" re ");
                sb.append(r.relleno != null && r.borde != null ? "B" : (r.relleno != null ? "f" : "S"));
                sb.append(" Q\n");
                ascii(out, sb.toString());
            } else if (op instanceof DocumentoMaquetado.OpLinea) {
                DocumentoMaquetado.OpLinea l = (DocumentoMaquetado.OpLinea) op;
                sb.append("q ").append(colorTrazo(l.color)).append(' ').append(num(l.grosor)).append(" w 0 J ")
                        .append(num(l.x1)).append(' ').append(num(alto - l.y1)).append(" m ")
                        .append(num(l.x2)).append(' ').append(num(alto - l.y2)).append(" l S Q\n");
                ascii(out, sb.toString());
            } else if (op instanceof DocumentoMaquetado.OpTexto) {
                DocumentoMaquetado.OpTexto t = (DocumentoMaquetado.OpTexto) op;
                int numeroFuente = (t.negrita ? (t.cursiva ? 4 : 2) : (t.cursiva ? 3 : 1)) + (t.serif ? 4 : 0);
                String fuente = "/F" + numeroFuente;
                ascii(out, "BT " + fuente + " " + num(t.tamano) + " Tf " + colorRelleno(t.color) + " 1 0 0 1 "
                        + num(t.x) + " " + num(alto - t.y) + " Tm (");
                textoWinAnsi(out, t.texto);
                ascii(out, ") Tj ET\n");
            } else if (op instanceof DocumentoMaquetado.OpImagen) {
                DocumentoMaquetado.OpImagen im = (DocumentoMaquetado.OpImagen) op;
                sb.append("q ").append(num(im.ancho)).append(" 0 0 ").append(num(im.alto)).append(' ')
                        .append(num(im.x)).append(' ').append(num(alto - im.y - im.alto)).append(" cm /")
                        .append(nombreImagen.get(im.imagen)).append(" Do Q\n");
                ascii(out, sb.toString());
            }
        }
        return out.toByteArray();
    }

    private static void escribirImagen(Escritor w, BufferedImage img, int numero, long[] posiciones) throws IOException {
        int ancho = img.getWidth();
        int alto = img.getHeight();
        ByteArrayOutputStream rgb = new ByteArrayOutputStream(ancho * alto * 3);
        ByteArrayOutputStream alfa = new ByteArrayOutputStream(ancho * alto);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int argb = img.getRGB(x, y);
                rgb.write((argb >> 16) & 0xFF);
                rgb.write((argb >> 8) & 0xFF);
                rgb.write(argb & 0xFF);
                alfa.write((argb >>> 24) & 0xFF);
            }
        }
        byte[] datosRgb = comprimir(rgb.toByteArray());
        byte[] datosAlfa = comprimir(alfa.toByteArray());
        int numeroMascara = numero + 1;

        posiciones[numero] = w.posicion();
        w.ascii(numero + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + ancho + " /Height " + alto
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /SMask " + numeroMascara
                + " 0 R /Length " + datosRgb.length + " >>\nstream\n");
        w.bytes(datosRgb);
        w.ascii("\nendstream\nendobj\n");

        posiciones[numeroMascara] = w.posicion();
        w.ascii(numeroMascara + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + ancho + " /Height " + alto
                + " /ColorSpace /DeviceGray /BitsPerComponent 8 /Filter /FlateDecode /Length " + datosAlfa.length
                + " >>\nstream\n");
        w.bytes(datosAlfa);
        w.ascii("\nendstream\nendobj\n");
    }

    // ------------------------------------------------------------------ utilidades

    private static void textoWinAnsi(ByteArrayOutputStream out, String texto) {
        for (int i = 0; i < texto.length(); i++) {
            int b = TextoPdf.codigoWinAnsi(texto.charAt(i));
            if (b == '(' || b == ')' || b == '\\') {
                out.write('\\');
                out.write(b);
            } else if (b < 32 || b > 126) {
                ascii(out, String.format(Locale.ROOT, "\\%03o", b));
            } else {
                out.write(b);
            }
        }
    }

    /** Texto en UTF-16 (para el título del archivo, que admite cualquier caracter). */
    private static String textoUnicode(String texto) {
        StringBuilder sb = new StringBuilder("<FEFF");
        String t = texto == null ? "" : texto;
        for (int i = 0; i < t.length(); i++) {
            sb.append(String.format(Locale.ROOT, "%04X", (int) t.charAt(i)));
        }
        return sb.append('>').toString();
    }

    private static String colorRelleno(int rgb) {
        return componentes(rgb) + " rg";
    }

    private static String colorTrazo(int rgb) {
        return componentes(rgb) + " RG";
    }

    private static String componentes(int rgb) {
        return num(((rgb >> 16) & 0xFF) / 255.0) + " " + num(((rgb >> 8) & 0xFF) / 255.0) + " " + num((rgb & 0xFF) / 255.0);
    }

    private static String num(double valor) {
        String s = String.format(Locale.ROOT, "%.3f", valor);
        // Sacar ceros de más ("12.500" -> "12.5", "3.000" -> "3").
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return "-0".equals(s) ? "0" : s;
    }

    private static byte[] comprimir(byte[] datos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream d = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION))) {
            d.write(datos);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream out, String texto) {
        byte[] b = texto.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    /** Escritor que lleva la cuenta de en qué byte va (el PDF necesita la posición de cada objeto). */
    private static final class Escritor {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        long posicion() {
            return out.size();
        }

        void ascii(String texto) {
            byte[] b = texto.getBytes(StandardCharsets.ISO_8859_1);
            out.write(b, 0, b.length);
        }

        void bytes(byte[] b) {
            out.write(b, 0, b.length);
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }
    }
}
