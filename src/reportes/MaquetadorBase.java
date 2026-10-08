package reportes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que comparten el informe de resultados y el comprobante: márgenes, encabezado del
 * laboratorio (con logo), encabezado corto de las hojas siguientes, pie con número de página y
 * control de "si no entra, pasar a la hoja siguiente".
 */
abstract class MaquetadorBase {

    static final int NEGRO = 0x141C19;
    static final int GRIS = 0x5F6966;
    static final int GRIS_CLARO = 0x8C9490;
    static final int LINEA = 0xD5DBD8;
    static final int CEBRA = 0xF4F6F5;
    static final int ROJO = 0xB91C1C;
    /** Color de los avisos "completá este dato" -- SOLO en la vista previa de ejemplo, nunca en un PDF real. */
    static final int AVISO = 0xB45309;

    static final double ALTO_PIE = 24;

    protected final DocumentoMaquetado doc;
    protected final DisenoInforme d;
    protected final ConfigInforme c;
    protected final boolean modoEjemplo;

    protected DocumentoMaquetado.Pagina pag;
    protected double y;
    protected final double anchoHoja;
    protected final double altoHoja;
    protected final double izq;
    protected final double der;
    protected final double ancho;
    protected final double limite;
    protected final List<DocumentoMaquetado.Pagina> paginasPropias = new ArrayList<>();

    MaquetadorBase(DocumentoMaquetado doc, DisenoInforme d, ConfigInforme c, boolean modoEjemplo) {
        this.doc = doc;
        this.d = d;
        this.c = c;
        this.modoEjemplo = modoEjemplo;
        this.anchoHoja = d.getHoja().ancho;
        this.altoHoja = d.getHoja().alto;
        this.izq = DocumentoMaquetado.mm(d.getMargenLateralMm());
        this.der = anchoHoja - izq;
        this.ancho = der - izq;
        this.limite = altoHoja - DocumentoMaquetado.mm(d.getMargenInferiorMm()) - ALTO_PIE;
    }

    protected int acento() {
        return d.getColorAcento().fuerte;
    }

    protected int acentoSuave() {
        return d.getColorAcento().suave;
    }

    /** Abre una hoja nueva y deja {@code y} en el margen superior. */
    protected DocumentoMaquetado.Pagina abrirHoja() {
        pag = doc.nuevaPagina(anchoHoja, altoHoja);
        paginasPropias.add(pag);
        y = DocumentoMaquetado.mm(d.getMargenSuperiorMm());
        return pag;
    }

    /** true si todavía entran {@code alto} puntos en la hoja actual. */
    protected boolean entra(double alto) {
        return y + alto <= limite;
    }

    // ------------------------------------------------------------------ encabezado completo

    /**
     * Logo + nombre del laboratorio + subtítulo + contacto, con la línea de color debajo. Respeta
     * "Encabezado: izquierda/centro" y "Logo: izquierda/derecha" del diseño. Si el diseño dice
     * "hoja membretada", no dibuja nada (el margen superior ya deja el espacio libre).
     */
    protected void encabezadoCompleto() {
        if (!d.isMostrarEncabezado()) {
            if (modoEjemplo) {
                pag.texto(izq, y - 6, "(Hoja membretada: acá arriba va el membrete impreso del laboratorio)",
                        7.5, false, true, AVISO);
            }
            return;
        }

        BufferedImage logo = c.logo();
        double ladoLogo = 54;
        double anchoLogo = 0;
        double altoLogo = 0;
        if (logo != null) {
            double escala = Math.min(ladoLogo / logo.getWidth(), ladoLogo / logo.getHeight());
            anchoLogo = logo.getWidth() * escala;
            altoLogo = logo.getHeight() * escala;
        } else if (modoEjemplo && c.mostrarLogo) {
            anchoLogo = ladoLogo;
            altoLogo = ladoLogo;
        }
        boolean logoCentro = d.getPosicionLogo() == DisenoInforme.Alineacion.CENTRO;
        boolean logoDerecha = d.getPosicionLogo() == DisenoInforme.Alineacion.DERECHA;
        boolean centrado = logoCentro || d.getAlineacionEncabezado() == DisenoInforme.Alineacion.CENTRO;

        double top = y;
        // Con el logo al centro, va arriba de todo y el texto debajo, todo centrado.
        if (logoCentro && anchoLogo > 0) {
            dibujarLogo(logo, anchoMitad() - anchoLogo / 2, top, anchoLogo, altoLogo);
            top += altoLogo + 6;
        }

        double xIzqTexto = izq + (!logoCentro && !logoDerecha && anchoLogo > 0 ? anchoLogo + 12 : 0);
        double xDerTexto = der - (!logoCentro && logoDerecha && anchoLogo > 0 ? anchoLogo + 12 : 0);
        double anchoTexto = xDerTexto - xIzqTexto;
        if (centrado && !logoCentro && anchoLogo > 0) {
            // Centrado con el logo a un costado: se deja el mismo margen de los dos lados.
            anchoTexto = ancho - 2 * (anchoLogo + 12);
        }

        // Renglones: el nombre puede ser largo o tener "Enter": se corta en renglones, nunca "...".
        List<String[]> lineas = new ArrayList<>(); // {texto, tipo}
        String nombre = ConfigInforme.tiene(c.nombreLaboratorio) ? c.nombreLaboratorio.trim() : "Laboratorio";
        for (String r : TextoPdf.envolverTexto(nombre, anchoTexto, 15.5, true)) {
            lineas.add(new String[]{r, "nombre"});
        }
        if (ConfigInforme.tiene(c.subtitulo)) {
            for (String r : TextoPdf.envolverTexto(c.subtitulo.trim(), anchoTexto, 9.5, false)) {
                lineas.add(new String[]{r, "sub"});
            }
        }
        if (ConfigInforme.tiene(c.bioquimica)) {
            String bioq = "Bioquímica: " + c.bioquimica.trim()
                    + (ConfigInforme.tiene(c.matricula) ? "  -  M.P. " + matriculaSinPrefijo() : "");
            for (String r : TextoPdf.envolverTexto(bioq, anchoTexto, 9, true)) {
                lineas.add(new String[]{r, "bioq"});
            }
        }
        String contacto = unir("   ·   ", c.direccion, c.telefono, c.localidad);
        String contacto2 = unir("   ·   ", c.email, ConfigInforme.tiene(c.cuit) ? "CUIT " + c.cuit.trim() : null);
        for (String r : TextoPdf.envolverTexto(contacto, anchoTexto, 8.5, false)) {
            if (!r.isEmpty()) {
                lineas.add(new String[]{r, "dato"});
            }
        }
        for (String r : TextoPdf.envolverTexto(contacto2, anchoTexto, 8.5, false)) {
            if (!r.isEmpty()) {
                lineas.add(new String[]{r, "dato"});
            }
        }
        if (modoEjemplo && contacto.isEmpty() && contacto2.isEmpty()) {
            lineas.add(new String[]{"Dirección · Teléfono · Email: completalos en Configuración > Laboratorio", "aviso"});
        }

        double altoTexto = 4;
        for (String[] l : lineas) {
            altoTexto += "nombre".equals(l[1]) ? 18 : 12;
        }
        double altoBloque = logoCentro ? altoTexto : Math.max(altoTexto, altoLogo);

        if (!logoCentro && anchoLogo > 0) {
            double xLogo = logoDerecha ? der - anchoLogo : izq;
            dibujarLogo(logo, xLogo, top + (altoBloque - altoLogo) / 2, anchoLogo, altoLogo);
        }

        double yb = top + (altoBloque - altoTexto) / 2;
        for (int i = 0; i < lineas.size(); i++) {
            String[] l = lineas.get(i);
            boolean esNombre = "nombre".equals(l[1]);
            boolean esBioq = "bioq".equals(l[1]);
            double tam = esNombre ? 15.5 : ("sub".equals(l[1]) ? 9.5 : (esBioq ? 9 : 8.5));
            int color = esNombre ? acento() : ("aviso".equals(l[1]) ? AVISO : (esBioq ? NEGRO : GRIS));
            yb += esNombre ? 15 : 12;
            if (centrado) {
                pag.textoCentrado(anchoMitad(), yb, l[0], tam, esNombre || esBioq, "aviso".equals(l[1]), color);
            } else {
                pag.texto(xIzqTexto, yb, l[0], tam, esNombre || esBioq, "aviso".equals(l[1]), color);
            }
            boolean ultimoNombre = esNombre && (i + 1 >= lineas.size() || !"nombre".equals(lineas.get(i + 1)[1]));
            if (esNombre) {
                yb += ultimoNombre ? 4 : 3;
            }
        }

        y = top + altoBloque + 8;
        pag.linea(izq, y, der, y, 1.4, acento());
        y += 14;
    }

    /** Mitad horizontal de la hoja. */
    protected double anchoMitad() {
        return anchoHoja / 2;
    }

    /** Logo, o el recuadro "LOGO" de ejemplo si todavía no se cargó ninguno (solo en la vista previa de ejemplo). */
    protected void dibujarLogo(BufferedImage logo, double x, double yLogo, double w, double h) {
        if (logo != null) {
            pag.imagen(x, yLogo, w, h, logo);
        } else {
            pag.rect(x, yLogo, w, h, 0xF1F3F2, LINEA, 0.6);
            pag.textoCentrado(x + w / 2, yLogo + h / 2 + 3, "LOGO", 7.5, true, false, GRIS_CLARO);
        }
    }

    /**
     * Firma al pie de la última hoja: la imagen de la firma digitalizada (si hay una cargada y
     * está tildado usarla), la línea, el nombre de la bioquímica y su matrícula.
     *
     * @param mayusculas true en el estilo Clásico (nombre en mayúsculas, como en el Word).
     */
    protected void dibujarFirma(double yMinimo, boolean mayusculas, Runnable hojaNueva) {
        if (!d.isMostrarFirma()) {
            return;
        }
        BufferedImage imagen = c.firma();
        double altoImagen = 0;
        double anchoImagen = 0;
        if (imagen != null) {
            double escala = Math.min(150.0 / imagen.getWidth(), 46.0 / imagen.getHeight());
            anchoImagen = imagen.getWidth() * escala;
            altoImagen = imagen.getHeight() * escala;
        }
        double altoTotal = 50 + altoImagen;
        double separacion = 18;
        if (yMinimo + separacion + altoTotal > limite) {
            hojaNueva.run();
            yMinimo = y;
        }
        // De fábrica la firma va pegada al contenido (si sobra hoja, se corta sin perder la firma).
        double top = d.isFirmaAlPie() ? limite - altoTotal : yMinimo + separacion;
        double anchoLinea = 170;
        double xLinea;
        DisenoInforme.Alineacion posicion = d.getPosicionFirma();
        if (posicion == DisenoInforme.Alineacion.IZQUIERDA) {
            xLinea = izq;
        } else if (posicion == DisenoInforme.Alineacion.CENTRO) {
            xLinea = izq + (ancho - anchoLinea) / 2;
        } else {
            xLinea = der - anchoLinea;
        }
        double centro = xLinea + anchoLinea / 2;
        double yLinea = top + altoImagen + 24;
        if (imagen != null) {
            // La firma "apoyada" sobre la línea, como firmada a mano.
            pag.imagen(centro - anchoImagen / 2, yLinea - altoImagen + 6, anchoImagen, altoImagen, imagen);
        }
        pag.linea(xLinea, yLinea, xLinea + anchoLinea, yLinea, 0.7, mayusculas ? NEGRO : GRIS);
        if (ConfigInforme.tiene(c.bioquimica)) {
            String nombre = mayusculas ? c.bioquimica.trim().toUpperCase() : c.bioquimica.trim();
            pag.textoCentrado(centro, yLinea + 12, TextoPdf.recortarConPuntos(nombre, anchoLinea + 40, 9.5, true,
                    pag.isSerif()), 9.5, true, false, NEGRO);
            String titulo = mayusculas ? "BIOQUIMICA" : "Bioquímico/a";
            String cargo = ConfigInforme.tiene(c.matricula) ? titulo + "  -  M.P. " + matriculaSinPrefijo() : titulo;
            pag.textoCentrado(centro, yLinea + 23, cargo, 8.5, false, false, mayusculas ? NEGRO : GRIS);
        } else if (modoEjemplo) {
            pag.textoCentrado(centro, yLinea + 12, "Firma: cargá la bioquímica y su matrícula", 8.5, false, true, AVISO);
            pag.textoCentrado(centro, yLinea + 23, "en Configuración > Laboratorio", 8.5, false, true, AVISO);
        } else {
            pag.textoCentrado(centro, yLinea + 12, "Firma y sello", 9, false, false, GRIS);
        }
    }

    /** La matrícula sin "M.P." adelante (por si ya la escribieron con el prefijo). */
    protected String matriculaSinPrefijo() {
        return ConfigInforme.tiene(c.matricula)
                ? c.matricula.trim().replaceFirst("(?i)^m\\.?\\s*p\\.?\\s*", "") : "";
    }

    /** Encabezado corto de las hojas 2, 3...: laboratorio a la izquierda, un dato a la derecha. */
    protected void encabezadoCorto(String textoDerecha) {
        String nombre = ConfigInforme.tiene(c.nombreLaboratorio) ? c.nombreLaboratorio.trim() : "Laboratorio";
        double anchoDerecha = TextoPdf.ancho(textoDerecha, 8.5, false);
        pag.texto(izq, y + 9, TextoPdf.recortarConPuntos(nombre, ancho - anchoDerecha - 16, 9, true), 9, true, false, acento());
        pag.textoDerecha(der, y + 9, textoDerecha, 8.5, false, false, GRIS);
        y += 14;
        pag.linea(izq, y, der, y, 0.6, LINEA);
        y += 12;
    }

    // ------------------------------------------------------------------ pie

    /** Línea + leyenda (y habilitación) a la izquierda + "Página X de N" a la derecha, en cada hoja propia. */
    protected void dibujarPies(String etiquetaPagina) {
        int total = paginasPropias.size();
        String izquierda = unir("   ·   ",
                c.mostrarHabilitacion && ConfigInforme.tiene(c.habilitacion) ? "Habilitación N° " + c.habilitacion.trim() : null,
                c.leyendaPie);
        double yLinea = limite + 6;
        for (int i = 0; i < total; i++) {
            DocumentoMaquetado.Pagina p = paginasPropias.get(i);
            p.linea(izq, yLinea, der, yLinea, 0.5, LINEA);
            String derecha = etiquetaPagina + "Página " + (i + 1) + " de " + total;
            double anchoDerecha = TextoPdf.ancho(derecha, 7.5, false);
            if (!izquierda.isEmpty()) {
                p.texto(izq, yLinea + 11, TextoPdf.recortarConPuntos(izquierda, ancho - anchoDerecha - 14, 7.5, false),
                        7.5, false, true, GRIS_CLARO);
            }
            p.textoDerecha(der, yLinea + 11, derecha, 7.5, false, false, GRIS_CLARO);
        }
    }

    // ------------------------------------------------------------------ utilidades

    static String unir(String separador, String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p != null && !p.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(separador);
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    static String guion(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    /** Dibuja un texto cortado en renglones; devuelve la altura usada. */
    protected double parrafo(double x, double yInicio, String texto, double anchoMax, double tam, boolean negrita,
            boolean cursiva, int color, double interlineado) {
        List<String> renglones = TextoPdf.envolverTexto(texto, anchoMax, tam, negrita);
        double yb = yInicio;
        for (String r : renglones) {
            yb += interlineado;
            pag.texto(x, yb, r, tam, negrita, cursiva, color);
        }
        return renglones.size() * interlineado;
    }
}
