package reportes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import modelo.TramoTexto;

/**
 * Arma (acomoda en hojas) el "Informe de Resultados" de una orden completa.
 *
 * <p>Cómo queda cada hoja, de arriba a abajo:</p>
 * <ol>
 *   <li>Encabezado del laboratorio (logo, nombre, contacto) -- en la primera hoja; en las
 *       siguientes, un encabezado corto con el paciente y la orden.</li>
 *   <li>Recuadro con los datos del paciente y de la orden.</li>
 *   <li>Un bloque por cada estudio: título, y la tabla Determinación / Resultado / Valores de
 *       referencia / Estado. Los textos largos NO se cortan: pasan al renglón de abajo, y las
 *       referencias conservan sus colores. Si una tabla no entra, sigue en la hoja siguiente con
 *       su título "(continuación)" y los nombres de columna repetidos.</li>
 *   <li>Firma del bioquímico/a al pie de la última hoja, y en todas las hojas el pie con la
 *       leyenda y "Página X de N".</li>
 * </ol>
 *
 * <p>"Estado" va en rojo SOLO en su propia celda (Alto/Bajo), no en toda la fila.</p>
 */
public class MaquetadorInforme extends MaquetadorBase {

    private static final double ALTO_TITULO_ESTUDIO = 22;
    private static final double ALTO_CABECERA = 17;
    private static final double ALTO_FIRMA = 52;
    private static final double RELLENO_CELDA = 5;

    private final ResultadosDatos datos;
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

    // Columnas (en puntos), calculadas a partir de los % del diseño.
    private double xDet;
    private double xRes;
    private double xRef;
    private double xEst;
    private double wDet;
    private double wRes;
    private double wRef;
    private double wEst;

    private MaquetadorInforme(DocumentoMaquetado doc, ResultadosDatos datos, DisenoInforme d, ConfigInforme c,
            boolean modoEjemplo) {
        super(doc, d, c, modoEjemplo);
        this.datos = datos;
        calcularColumnas();
    }

    /**
     * Arma el informe completo (y, si en Configuración está tildado "agregar el comprobante al
     * final" y la orden tiene uno, le suma esa hoja al final).
     *
     * @param modoEjemplo true solo para la vista previa de Configuración: muestra avisos de
     *                    "completá este dato" donde falta algo. Nunca en un PDF real.
     */
    public static DocumentoMaquetado armar(ResultadosDatos datos, DisenoInforme diseno, ConfigInforme config,
            boolean modoEjemplo) {
        DocumentoMaquetado doc = new DocumentoMaquetado("Informe de resultados - Orden " + guion(datos.getNumeroOrden())
                + " - " + guion(datos.getNombrePaciente()));
        if (diseno.getEstilo() == DisenoInforme.Estilo.CLASICO) {
            MaquetadorClasico.agregarA(doc, datos, diseno, config, modoEjemplo);
        } else {
            new MaquetadorInforme(doc, datos, diseno, config, modoEjemplo).armarTodo();
        }
        if (config.incluirComprobante && datos.getComprobante() != null) {
            MaquetadorComprobante.agregarA(doc, datos.getComprobante(), diseno, config, modoEjemplo);
        }
        return doc;
    }

    private void calcularColumnas() {
        int pDet = d.getColDeterminacion();
        int pRes = d.getColResultado();
        int pRef = c.incluirReferencia ? d.getColReferencia() : 0;
        int pEst = d.getColEstado();
        if (!c.incluirReferencia) {
            // Sin la columna de referencia, su lugar se reparte entre Determinación y Resultado.
            int libre = d.getColReferencia();
            pDet += Math.round(libre * 0.6f);
            pRes += libre - Math.round(libre * 0.6f);
        }
        double total = pDet + pRes + pRef + pEst;
        wDet = ancho * pDet / total;
        wRes = ancho * pRes / total;
        wRef = ancho * pRef / total;
        wEst = ancho - wDet - wRes - wRef;
        xDet = izq;
        xRes = xDet + wDet;
        xRef = xRes + wRes;
        xEst = xRef + wRef;
    }

    private void armarTodo() {
        abrirHoja();
        encabezadoCompleto();
        bloquePaciente();

        List<ResultadosDatos.Estudio> estudios = datos.getEstudios();
        if (estudios.isEmpty()) {
            y += parrafo(izq, y, "Esta orden todavía no tiene estudios terminados para informar.", ancho, 10, false,
                    true, GRIS, 14);
            y += 10;
        }
        for (int i = 0; i < estudios.size(); i++) {
            if (i > 0 && d.isEstudioEnHojaNueva()) {
                hojaSiguiente();
            }
            bloqueEstudio(estudios.get(i));
        }

        if (ConfigInforme.tiene(datos.getNotaFinal())) {
            String nota = datos.getNotaFinal().trim();
            double alto = TextoPdf.envolverTexto(nota, ancho, 9.5, false).size() * 13 + 10;
            if (!entra(alto)) {
                hojaSiguiente();
            }
            y += parrafo(izq, y, nota, ancho, 9.5, false, false, NEGRO, 13) + 10;
        }

        if (!datos.getEstudiosPendientes().isEmpty()) {
            String texto = "Estudios de esta orden que todavía están en proceso (se informarán cuando estén listos): "
                    + String.join(", ", datos.getEstudiosPendientes()) + ".";
            double alto = TextoPdf.envolverTexto(texto, ancho, 8.5, false).size() * 11 + 6;
            if (!entra(alto)) {
                hojaSiguiente();
            }
            y += parrafo(izq, y, texto, ancho, 8.5, false, true, GRIS, 11) + 6;
        }

        firma();
        dibujarPies("");
    }

    /**
     * Hoja nueva. Si el diseño dice "repetir los datos en cada hoja" (lo normal), vuelve a poner
     * el encabezado del laboratorio y los datos del paciente: si se imprimen y se separan las
     * hojas, ninguna queda sin saber de quién es. Si no, un encabezado corto.
     */
    private void hojaSiguiente() {
        abrirHoja();
        if (d.isRepetirDatos()) {
            encabezadoCompleto();
            bloquePaciente();
            return;
        }
        String derecha = "Paciente: " + guion(datos.getNombrePaciente()) + "   ·   Orden N° " + guion(datos.getNumeroOrden());
        encabezadoCorto(TextoPdf.recortarConPuntos(derecha, ancho * 0.62, 8.5, false));
    }

    // ------------------------------------------------------------------ paciente

    private void bloquePaciente() {
        double relleno = 9;
        double anchoCol = (ancho - relleno * 3) / 2;
        double anchoEtiquetaIzq = 50;
        double anchoEtiquetaDer = 88;

        List<String[]> izquierda = new ArrayList<>();
        izquierda.add(new String[]{"Paciente", guion(datos.getNombrePaciente()), "b"});
        izquierda.add(new String[]{"DNI", guion(datos.getDni()), ""});
        izquierda.add(new String[]{"Edad", datos.getEdad() > 0 ? datos.getEdad() + " años" : "-", ""});

        List<String[]> derecha = new ArrayList<>();
        derecha.add(new String[]{"Orden N°", guion(datos.getNumeroOrden()), "b"});
        derecha.add(new String[]{"Fecha", datos.getFecha() != null ? formatoFecha.format(datos.getFecha()) : "-", ""});
        if (c.incluirMedico && ConfigInforme.tiene(datos.getMedicoDerivante())) {
            derecha.add(new String[]{"Médico derivante", datos.getMedicoDerivante().trim(), ""});
        }
        derecha.add(new String[]{"Cobertura", guion(datos.getCobertura()), ""});

        double altoTitulo = 18;
        double altoIzq = altoColumna(izquierda, anchoCol - anchoEtiquetaIzq);
        double altoDer = altoColumna(derecha, anchoCol - anchoEtiquetaDer);
        double alto = relleno + altoTitulo + Math.max(altoIzq, altoDer) + relleno - 2;

        pag.rect(izq, y, ancho, alto, 0xFAFBFA, LINEA, 0.7);
        pag.texto(izq + relleno, y + relleno + 8, "INFORME DE RESULTADOS", 9, true, false, acento());
        pag.textoDerecha(der - relleno, y + relleno + 8, "Emitido el " + formatoFecha.format(new Date()), 7.5, false,
                false, GRIS_CLARO);
        double yFilas = y + relleno + altoTitulo;
        columna(izquierda, izq + relleno, anchoEtiquetaIzq, anchoCol - anchoEtiquetaIzq, yFilas);
        columna(derecha, izq + relleno * 2 + anchoCol, anchoEtiquetaDer, anchoCol - anchoEtiquetaDer, yFilas);
        y += alto + 16;
    }

    private double altoColumna(List<String[]> filas, double anchoValor) {
        double alto = 0;
        for (String[] f : filas) {
            alto += TextoPdf.envolverTexto(f[1], anchoValor, 9.5, "b".equals(f[2])).size() * 12 + 2;
        }
        return alto;
    }

    private void columna(List<String[]> filas, double x, double anchoEtiqueta, double anchoValor, double yInicio) {
        double yc = yInicio;
        for (String[] f : filas) {
            pag.texto(x, yc + 9, f[0], 8, false, false, GRIS);
            List<String> renglones = TextoPdf.envolverTexto(f[1], anchoValor, 9.5, "b".equals(f[2]));
            double yv = yc;
            for (String r : renglones) {
                pag.texto(x + anchoEtiqueta, yv + 9, r, 9.5, "b".equals(f[2]), false, NEGRO);
                yv += 12;
            }
            yc += renglones.size() * 12 + 2;
        }
    }

    // ------------------------------------------------------------------ estudios

    /** Una fila ya cortada en renglones, lista para dibujar. */
    private static final class FilaArmada {
        List<String> det;
        List<String> res;
        List<List<TextoPdf.Pieza>> ref;
        String estado;
        boolean fueraDeRango;
        double alto;
    }

    private void bloqueEstudio(ResultadosDatos.Estudio estudio) {
        double tam = d.getTamanoLetra();
        double interlineado = tam * 1.25;
        double rellenoV = d.getEspaciado().relleno;

        List<FilaArmada> filas = new ArrayList<>();
        for (ResultadosDatos.Fila f : estudio.getFilas()) {
            filas.add(armarFila(f, tam, interlineado, rellenoV));
        }

        double primeraFila = filas.isEmpty() ? 16 : filas.get(0).alto;
        if (!entra(ALTO_TITULO_ESTUDIO + ALTO_CABECERA + primeraFila)) {
            hojaSiguiente();
        }
        tituloEstudio(estudio.getNombre(), false);
        cabeceraTabla();

        if (filas.isEmpty()) {
            pag.texto(izq + RELLENO_CELDA, y + 12, "Sin parámetros cargados para este estudio.", 9, false, true, GRIS);
            y += 18;
        }

        for (int i = 0; i < filas.size(); i++) {
            FilaArmada fila = filas.get(i);
            if (!entra(fila.alto)) {
                hojaSiguiente();
                tituloEstudio(estudio.getNombre(), true);
                cabeceraTabla();
            }
            dibujarFila(fila, i, tam, interlineado, rellenoV);
        }

        if (ConfigInforme.tiene(estudio.getObservacion())) {
            String texto = "Observaciones: " + estudio.getObservacion().trim();
            double alto = TextoPdf.envolverTexto(texto, ancho - 2 * RELLENO_CELDA, 8.5, false).size() * 11 + 8;
            if (!entra(alto)) {
                hojaSiguiente();
            }
            y += 2;
            y += parrafo(izq + RELLENO_CELDA, y, texto, ancho - 2 * RELLENO_CELDA, 8.5, false, true, GRIS, 11) + 4;
        }
        y += 14;
    }

    private FilaArmada armarFila(ResultadosDatos.Fila f, double tam, double interlineado, double rellenoV) {
        FilaArmada a = new FilaArmada();
        a.det = TextoPdf.envolverTexto(guion(f.getNombreParametro()), wDet - 2 * RELLENO_CELDA, tam, false);
        String unidad = f.getUnidad() == null || f.getUnidad().trim().isEmpty() ? "" : " " + f.getUnidad().trim();
        a.res = TextoPdf.envolverTexto(guion(f.getValor()) + (f.getValor() == null ? "" : unidad), wRes - 2 * RELLENO_CELDA,
                tam, true);

        if (c.incluirReferencia) {
            List<TextoPdf.Pieza> piezas = new ArrayList<>();
            List<TramoTexto> tramos = utilidades.TramosTextoUtil.conSeparadores(f.getTramosReferencia());
            if (tramos.isEmpty()) {
                piezas.add(new TextoPdf.Pieza(guion(f.getReferencia()), null));
            } else {
                for (TramoTexto t : tramos) {
                    piezas.add(new TextoPdf.Pieza(t.getTexto(), colorDe(t.getColorHex())));
                }
            }
            a.ref = TextoPdf.envolver(piezas, wRef - 2 * RELLENO_CELDA, tam, false);
        } else {
            a.ref = new ArrayList<List<TextoPdf.Pieza>>();
        }

        a.estado = f.getEstado() == null ? "" : f.getEstado();
        a.fueraDeRango = "Alto".equals(a.estado) || "Bajo".equals(a.estado);
        int renglones = Math.max(Math.max(a.det.size(), a.res.size()), Math.max(a.ref.size(), 1));
        a.alto = renglones * interlineado + 2 * rellenoV + 2;
        return a;
    }

    private void tituloEstudio(String nombre, boolean continuacion) {
        double alto = 18;
        pag.rect(izq, y, ancho, alto, acentoSuave(), null, 0);
        String texto = guion(nombre).toUpperCase() + (continuacion ? "  (continuación)" : "");
        pag.texto(izq + 7, y + 12.6, TextoPdf.recortarConPuntos(texto, ancho - 14, 10.5, true), 10.5, true, false, acento());
        y += ALTO_TITULO_ESTUDIO;
    }

    private void cabeceraTabla() {
        double yb = y + 10;
        pag.texto(xDet + RELLENO_CELDA, yb, "DETERMINACIÓN", 7.5, true, false, GRIS);
        pag.texto(xRes + RELLENO_CELDA, yb, "RESULTADO", 7.5, true, false, GRIS);
        if (c.incluirReferencia) {
            pag.texto(xRef + RELLENO_CELDA, yb, "VALORES DE REFERENCIA", 7.5, true, false, GRIS);
        }
        pag.texto(xEst + RELLENO_CELDA, yb, "ESTADO", 7.5, true, false, GRIS);
        pag.linea(izq, y + 14, der, y + 14, 0.9, acento());
        y += ALTO_CABECERA;
    }

    private void dibujarFila(FilaArmada fila, int indice, double tam, double interlineado, double rellenoV) {
        if (d.getEstiloTabla() == DisenoInforme.EstiloTabla.CEBRA && indice % 2 == 1) {
            pag.rect(izq, y, ancho, fila.alto, CEBRA, null, 0);
        }
        double primeraBase = y + rellenoV + tam * 0.86 + 1;

        double yb = primeraBase;
        for (String r : fila.det) {
            pag.texto(xDet + RELLENO_CELDA, yb, r, tam, false, false, NEGRO);
            yb += interlineado;
        }
        yb = primeraBase;
        for (String r : fila.res) {
            pag.texto(xRes + RELLENO_CELDA, yb, r, tam, true, false, NEGRO);
            yb += interlineado;
        }
        yb = primeraBase;
        for (List<TextoPdf.Pieza> renglon : fila.ref) {
            pag.piezas(xRef + RELLENO_CELDA, yb, renglon, tam, false, NEGRO);
            yb += interlineado;
        }

        if (!fila.estado.isEmpty()) {
            boolean rojo = fila.fueraDeRango && c.resaltarFueraDeRango;
            String texto = TextoPdf.recortarConPuntos(fila.estado, wEst - 2 * RELLENO_CELDA, tam, rojo);
            pag.texto(xEst + RELLENO_CELDA, primeraBase, texto, tam, rojo, false, rojo ? ROJO : GRIS);
        }

        y += fila.alto;
        if (d.getEstiloTabla() == DisenoInforme.EstiloTabla.LINEAS) {
            pag.linea(izq, y, der, y, 0.5, LINEA);
        }
    }

    // ------------------------------------------------------------------ firma

    /** Firma del bioquímico/a, abajo de todo en la última hoja (o en una hoja nueva si no entra). */
    private void firma() {
        dibujarFirma(y, false, this::hojaSiguiente);
    }

    private static Integer colorDe(String hex) {
        if (hex == null || hex.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(hex.trim(), 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
