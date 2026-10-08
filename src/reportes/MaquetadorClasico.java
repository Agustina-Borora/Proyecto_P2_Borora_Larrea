package reportes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import modelo.TramoTexto;

/**
 * Estilo "Clásico" del informe de resultados: imita el informe que el laboratorio arma hoy a mano
 * en Word (ver el ejemplo DOC-20260511-WA0065 que pasó la clienta):
 *
 * <ul>
 *   <li>Letra clásica (Times, muy parecida a la Cambria del Word).</li>
 *   <li>Encabezado: nombre del centro, "LABORATORIO DE ANALISIS CLINICOS" (subtítulo),
 *       "BIOQUIMICA: nombre - M.P", y la línea de dirección / teléfono / localidad.</li>
 *   <li>"PACIENTE:", "DR/A:", "D.N.I:", "EDAD:" y "FECHA DE ANALISIS:", y una línea gruesa.</li>
 *   <li>Los estudios en dos columnas (o una, a elección), cada uno con su título subrayado y sus
 *       renglones "Parámetro: valor unidad" con "VR: referencia" alineado a la derecha.</li>
 *   <li>Los resultados de texto largo (por ejemplo un exudado: "Regular células epiteliales")
 *       van con el nombre subrayado y el texto abajo, con asterisco.</li>
 * </ul>
 *
 * <p>Igual que el estilo Moderno: las referencias largas pasan de renglón sin perder sus colores,
 * y el "Alto"/"Bajo" va en rojo SOLO al lado del valor (no se pinta todo el renglón).</p>
 */
class MaquetadorClasico extends MaquetadorBase {

    private static final double SEPARACION_COLUMNAS = 22;
    private static final double ALTO_FIRMA = 50;
    /** Sangría del encabezado, igual que en el Word del laboratorio. */
    private static final double SANGRIA = 18;

    private final ResultadosDatos datos;
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

    private final int cantidadColumnas;
    private final double anchoColumna;
    private int columna;
    private double yInicioColumnas;
    /** Lo más abajo que llegó alguna columna en la hoja actual (para saber dónde entra la firma). */
    private double yMaximo;

    private MaquetadorClasico(DocumentoMaquetado doc, ResultadosDatos datos, DisenoInforme d, ConfigInforme c,
            boolean modoEjemplo) {
        super(doc, d, c, modoEjemplo);
        this.datos = datos;
        this.cantidadColumnas = d.getColumnasClasico();
        this.anchoColumna = (ancho - SEPARACION_COLUMNAS * (cantidadColumnas - 1)) / cantidadColumnas;
    }

    static void agregarA(DocumentoMaquetado doc, ResultadosDatos datos, DisenoInforme d, ConfigInforme c,
            boolean modoEjemplo) {
        new MaquetadorClasico(doc, datos, d, c, modoEjemplo).armarTodo();
    }

    // ------------------------------------------------------------------ armado general

    private void armarTodo() {
        abrirHojaClasica();
        encabezadoClasico();
        bloquePaciente();
        empezarColumnas();

        if (datos.getEstudios().isEmpty()) {
            asegurar(30);
            y += parrafoSerif(xColumna(), "Esta orden todavía no tiene estudios terminados para informar.",
                    anchoColumna, tam(), true, GRIS);
        }
        List<ResultadosDatos.Estudio> estudios = datos.getEstudios();
        for (int i = 0; i < estudios.size(); i++) {
            if (i > 0 && d.isEstudioEnHojaNueva()) {
                hojaSiguiente();
            }
            bloqueEstudio(estudios.get(i));
        }

        if (ConfigInforme.tiene(datos.getNotaFinal())) {
            String nota = datos.getNotaFinal().trim();
            double alto = TextoPdf.envolverTexto(nota, anchoColumna, tam(), false, false, true).size() * interlineado() + 6;
            asegurar(alto);
            y += parrafoSerif(xColumna(), nota, anchoColumna, tam(), false, NEGRO) + 6;
        }

        if (!datos.getEstudiosPendientes().isEmpty()) {
            String texto = "Estudios en proceso (se informarán cuando estén listos): "
                    + String.join(", ", datos.getEstudiosPendientes()) + ".";
            double alto = TextoPdf.envolverTexto(texto, anchoColumna, tam() - 1, false, true, true).size() * interlineado() + 6;
            asegurar(alto);
            y += parrafoSerif(xColumna(), texto, anchoColumna, tam() - 1, true, GRIS) + 6;
        }
        marcarMaximo();

        firma();
        dibujarPies("");
    }

    private double tam() {
        return d.getTamanoLetra();
    }

    private double interlineado() {
        return tam() * 1.3;
    }

    private void abrirHojaClasica() {
        abrirHoja();
        pag.setSerif(true);
    }

    /** Hoja nueva: repite encabezado y datos del paciente (si así está en el diseño), y sigue en columnas. */
    private void hojaSiguiente() {
        abrirHojaClasica();
        if (d.isRepetirDatos()) {
            encabezadoClasico();
            bloquePaciente();
            empezarColumnas();
            return;
        }
        String derecha = "Paciente: " + guion(datos.getNombrePaciente()) + "   ·   Orden N° " + guion(datos.getNumeroOrden());
        encabezadoCorto(TextoPdf.recortarConPuntos(derecha, ancho * 0.62, 8.5, false, true));
        empezarColumnas();
    }

    // ------------------------------------------------------------------ encabezado

    /** El encabezado como en el Word del laboratorio (con logo opcional). */
    private void encabezadoClasico() {
        if (!d.isMostrarEncabezado()) {
            if (modoEjemplo) {
                pag.texto(izq, y - 6, "(Hoja membretada: acá arriba va el membrete impreso del laboratorio)",
                        8, false, true, AVISO);
            }
            return;
        }
        boolean centrado = d.getAlineacionEncabezado() == DisenoInforme.Alineacion.CENTRO;
        double tamano = tam() + 1.5;
        double top = y;

        // Logo (opcional): izquierda, derecha, o centro (arriba, con todo el texto centrado).
        java.awt.image.BufferedImage logo = c.logo();
        double lado = 52;
        double anchoLogo = 0;
        double altoLogo = 0;
        if (logo != null) {
            double escala = Math.min(lado / logo.getWidth(), lado / logo.getHeight());
            anchoLogo = logo.getWidth() * escala;
            altoLogo = logo.getHeight() * escala;
        }
        boolean logoCentro = logo != null && d.getPosicionLogo() == DisenoInforme.Alineacion.CENTRO;
        if (logoCentro) {
            centrado = true;
            pag.imagen(anchoMitad() - anchoLogo / 2, top, anchoLogo, altoLogo, logo);
            y += altoLogo + 4;
            top = y;
        } else if (logo != null) {
            double xLogo = d.getPosicionLogo() == DisenoInforme.Alineacion.DERECHA ? der - anchoLogo : izq;
            pag.imagen(xLogo, top, anchoLogo, altoLogo, logo);
        }
        boolean logoIzquierda = logo != null && !logoCentro && d.getPosicionLogo() == DisenoInforme.Alineacion.IZQUIERDA;
        boolean logoDerecha = logo != null && !logoCentro && d.getPosicionLogo() == DisenoInforme.Alineacion.DERECHA;
        double x = izq + (logoIzquierda ? anchoLogo + 12 : 0);
        double anchoTexto = der - x - (logoDerecha ? anchoLogo + 12 : 0);
        double centro = anchoMitad();

        // El nombre puede ser largo o tener "Enter": se corta en renglones, nunca con "...".
        String nombre = ConfigInforme.tiene(c.nombreLaboratorio) ? c.nombreLaboratorio.trim().toUpperCase() : "LABORATORIO";
        List<String> renglonesNombre = TextoPdf.envolverTexto(nombre, anchoTexto, tamano, false, false, true);
        for (int i = 0; i < renglonesNombre.size(); i++) {
            y += i == 0 ? tamano : tamano * 1.25;
            linea(renglonesNombre.get(i), x, centrado, centro, tamano, false, NEGRO, anchoTexto);
        }
        if (ConfigInforme.tiene(c.subtitulo)) {
            for (String r : TextoPdf.envolverTexto(c.subtitulo.trim().toUpperCase(), anchoTexto - SANGRIA, tamano,
                    false, false, true)) {
                y += tamano * 1.25;
                linea(r, x + (centrado ? 0 : SANGRIA), centrado, centro, tamano, false, NEGRO, anchoTexto - SANGRIA);
            }
        }

        // BIOQUIMICA: (subrayado) NOMBRE - M.P xxx
        y += tamano * 1.9;
        if (ConfigInforme.tiene(c.bioquimica)) {
            String resto = " " + c.bioquimica.trim().toUpperCase()
                    + (ConfigInforme.tiene(c.matricula) ? "  -  M.P " + sinPrefijoMatricula(c.matricula) : "");
            double anchoEtiqueta = TextoPdf.ancho("BIOQUIMICA:", tam(), false, false, true);
            double anchoTotal = anchoEtiqueta + TextoPdf.ancho(resto, tam(), false, false, true);
            double xb = centrado ? centro - anchoTotal / 2 : x + SANGRIA;
            double fin = pag.textoSubrayado(xb, y, "BIOQUIMICA:", tam(), false, NEGRO);
            pag.texto(fin, y, resto, tam(), false, false, NEGRO);
        } else if (modoEjemplo) {
            pag.texto(x + SANGRIA, y, "BIOQUIMICA: (cargala en Configuración > Laboratorio)", tam(), false, true, AVISO);
        } else {
            y -= tamano * 1.9 - tamano * 0.6;
        }

        // Dirección      TELEFONO: xxx      Localidad
        List<String> partes = new ArrayList<String>();
        if (ConfigInforme.tiene(c.direccion)) {
            partes.add(c.direccion.trim().toUpperCase());
        }
        if (ConfigInforme.tiene(c.telefono)) {
            partes.add("TELEFONO: " + c.telefono.trim());
        }
        if (ConfigInforme.tiene(c.localidad)) {
            partes.add(c.localidad.trim().toUpperCase());
        }
        if (!partes.isEmpty()) {
            y += tamano * 1.9;
            if (centrado) {
                linea(String.join("        ", partes), x, true, centro, tam(), false, NEGRO, anchoTexto);
            } else {
                double paso = (anchoTexto - SANGRIA) / partes.size();
                for (int i = 0; i < partes.size(); i++) {
                    pag.texto(x + SANGRIA + paso * i, y,
                            TextoPdf.recortarConPuntos(partes.get(i), paso - 8, tam(), false, true), tam(), false, false, NEGRO);
                }
            }
        } else if (modoEjemplo) {
            y += tamano * 1.9;
            pag.texto(x + SANGRIA, y, "Dirección · Teléfono · Localidad: completalos en Configuración > Laboratorio",
                    tam() - 1, false, true, AVISO);
        }
        String extra = unir("   ·   ", c.email, ConfigInforme.tiene(c.cuit) ? "CUIT " + c.cuit.trim() : null);
        if (!extra.isEmpty()) {
            y += tam() * 1.35;
            linea(extra, x + (centrado ? 0 : SANGRIA), centrado, centro, tam() - 1.5, false, GRIS, anchoTexto);
        }
        y = Math.max(y, top + (logo != null && !logoCentro ? altoLogo : 0)) + tamano * 1.6;
    }

    private void linea(String texto, double x, boolean centrado, double centro, double tamano, boolean negrita,
            int color, double anchoMax) {
        String t = TextoPdf.recortarConPuntos(texto, anchoMax, tamano, negrita, true);
        if (centrado) {
            pag.textoCentrado(centro, y, t, tamano, negrita, false, color);
        } else {
            pag.texto(x, y, t, tamano, negrita, false, color);
        }
    }

    // ------------------------------------------------------------------ paciente

    private void bloquePaciente() {
        double tamano = tam() + 0.5;
        double x = izq + (d.isMostrarEncabezado() ? SANGRIA : 0);
        double anchoDisponible = der - x;

        // PACIENTE: (subrayado, negrita) NOMBRE
        y += tamano;
        double fin = pag.textoSubrayado(x, y, "PACIENTE:", tamano, true, NEGRO);
        pag.texto(fin, y, " " + TextoPdf.recortarConPuntos(guion(datos.getNombrePaciente()).toUpperCase(),
                der - fin - 4, tamano, true, true), tamano, true, false, NEGRO);

        // DR/A: medico                      ORDEN N°: xxx
        y += tamano * 1.25;
        String medico = c.incluirMedico && ConfigInforme.tiene(datos.getMedicoDerivante())
                ? datos.getMedicoDerivante().trim().toUpperCase() : "";
        String orden = "ORDEN N°: " + guion(datos.getNumeroOrden());
        if (c.incluirMedico) {
            pag.texto(x, y, "DR/A:  " + TextoPdf.recortarConPuntos(medico, anchoDisponible * 0.5, tamano, false, true),
                    tamano, false, false, NEGRO);
        }
        pag.texto(x + anchoDisponible * 0.62, y, orden, tamano, false, false, NEGRO);

        // D.N.I: xxx        EDAD: xx                FECHA DE ANALISIS: dd/mm/aaaa
        y += tamano * 1.25;
        pag.texto(x, y, "D.N.I:  " + guion(datos.getDni()), tamano, false, false, NEGRO);
        pag.texto(x + anchoDisponible * 0.32, y, "EDAD:  " + (datos.getEdad() > 0 ? String.valueOf(datos.getEdad()) : "-"),
                tamano, false, false, NEGRO);
        pag.texto(x + anchoDisponible * 0.62, y, "FECHA DE ANALISIS: "
                + (datos.getFecha() != null ? formatoFecha.format(datos.getFecha()) : "-"), tamano, false, false, NEGRO);

        y += tamano * 0.55;
        pag.linea(izq, y, der, y, 1.6, NEGRO);
        y += tamano * 2;
    }

    // ------------------------------------------------------------------ columnas

    private void empezarColumnas() {
        columna = 0;
        yInicioColumnas = y;
        yMaximo = y;
    }

    private double xColumna() {
        return izq + columna * (anchoColumna + SEPARACION_COLUMNAS);
    }

    private void marcarMaximo() {
        yMaximo = Math.max(yMaximo, y);
    }

    /** Si no entran {@code alto} puntos en esta columna, sigue en la de al lado o en una hoja nueva. */
    private void asegurar(double alto) {
        if (y + alto <= limite) {
            return;
        }
        marcarMaximo();
        if (columna + 1 < cantidadColumnas) {
            columna++;
            y = yInicioColumnas;
        } else {
            hojaSiguiente();
        }
    }

    // ------------------------------------------------------------------ estudios

    /** Un renglón del estudio ya cortado en partes, listo para dibujar. */
    private static final class Renglon {
        List<List<TextoPdf.Pieza>> izquierda;
        List<List<TextoPdf.Pieza>> referencia;
        /** Resultado de texto largo: nombre subrayado arriba y el texto abajo con "*". */
        boolean enBloque;
        String nombreBloque;
        List<String> textoBloque;
        double alto;
    }

    private void bloqueEstudio(ResultadosDatos.Estudio estudio) {
        List<Renglon> renglones = new ArrayList<Renglon>();
        for (ResultadosDatos.Fila f : estudio.getFilas()) {
            renglones.add(armarRenglon(f));
        }
        double altoTitulo = interlineado() * 1.5;
        asegurar(altoTitulo + (renglones.isEmpty() ? interlineado() : renglones.get(0).alto));
        tituloEstudio(estudio.getNombre(), false);

        if (renglones.isEmpty()) {
            pag.texto(xColumna(), y + tam(), "Sin parámetros cargados para este estudio.", tam() - 1, false, true, GRIS);
            y += interlineado();
        }
        for (Renglon r : renglones) {
            double columnaAntes = columna;
            int hojasAntes = paginasPropias.size();
            asegurar(r.alto);
            if (columna != columnaAntes || paginasPropias.size() != hojasAntes) {
                tituloEstudio(estudio.getNombre(), true);
            }
            dibujarRenglon(r);
        }

        if (ConfigInforme.tiene(estudio.getObservacion())) {
            String texto = "Obs.: " + estudio.getObservacion().trim();
            double alto = TextoPdf.envolverTexto(texto, anchoColumna, tam() - 1, false, true, true).size() * interlineado();
            asegurar(alto + 2);
            y += 2;
            y += parrafoSerif(xColumna(), texto, anchoColumna, tam() - 1, true, GRIS);
        }
        y += interlineado() * 1.3;
        marcarMaximo();
    }

    private void tituloEstudio(String nombre, boolean continuacion) {
        double tamano = tam() + 0.5;
        String texto = TextoPdf.recortarConPuntos(guion(nombre).toUpperCase() + (continuacion ? " (cont.)" : ""),
                anchoColumna, tamano, true, true);
        y += tamano;
        pag.textoSubrayado(xColumna(), y, texto, tamano, true, NEGRO);
        y += interlineado() * 0.5 + 2;
    }

    private Renglon armarRenglon(ResultadosDatos.Fila f) {
        Renglon r = new Renglon();
        double tamano = tam();
        String unidad = f.getUnidad() == null || f.getUnidad().trim().isEmpty() ? "" : " " + f.getUnidad().trim();
        String valor = f.getValor() == null ? "-" : f.getValor().trim() + unidad;
        boolean fueraDeRango = c.resaltarFueraDeRango && ("Alto".equals(f.getEstado()) || "Bajo".equals(f.getEstado()));
        boolean tieneReferencia = c.incluirReferencia
                && (!f.getTramosReferencia().isEmpty() || ConfigInforme.tiene(f.getReferencia()));
        double rellenoV = d.getEspaciado().relleno * 0.5;

        // Resultado de texto largo sin referencia (ej.: exudado, sedimento): en bloque.
        if (!tieneReferencia && f.getValor() != null
                && (f.getValor().length() > 32 || f.getValor().indexOf('\n') >= 0)) {
            r.enBloque = true;
            r.nombreBloque = guion(f.getNombreParametro());
            r.textoBloque = new ArrayList<String>();
            for (String parte : f.getValor().split("\n")) {
                if (!parte.trim().isEmpty()) {
                    r.textoBloque.addAll(TextoPdf.envolverTexto("*" + parte.trim(), anchoColumna - 16, tamano, false,
                            false, true));
                }
            }
            r.alto = interlineado() * (1 + r.textoBloque.size()) + rellenoV * 2 + 2;
            return r;
        }

        double anchoIzquierda = tieneReferencia ? anchoColumna * 0.56 - 6 : anchoColumna;
        List<TextoPdf.Pieza> izquierda = new ArrayList<TextoPdf.Pieza>();
        izquierda.add(new TextoPdf.Pieza(guion(f.getNombreParametro()) + ": " + valor, null));
        if (fueraDeRango) {
            izquierda.add(new TextoPdf.Pieza("  (" + f.getEstado() + ")", ROJO));
        }
        r.izquierda = TextoPdf.envolver(izquierda, anchoIzquierda, tamano, false, false, true);

        if (tieneReferencia) {
            List<TextoPdf.Pieza> ref = new ArrayList<TextoPdf.Pieza>();
            ref.add(new TextoPdf.Pieza("VR: ", null));
            List<TramoTexto> tramos = utilidades.TramosTextoUtil.conSeparadores(f.getTramosReferencia());
            if (tramos.isEmpty()) {
                ref.add(new TextoPdf.Pieza(guion(f.getReferencia()), null));
            } else {
                for (TramoTexto t : tramos) {
                    ref.add(new TextoPdf.Pieza(t.getTexto(), colorDe(t.getColorHex())));
                }
            }
            r.referencia = TextoPdf.envolver(ref, anchoColumna * 0.44, tamano, false, false, true);
        } else {
            r.referencia = new ArrayList<List<TextoPdf.Pieza>>();
        }
        int lineas = Math.max(r.izquierda.size(), Math.max(1, r.referencia.size()));
        r.alto = lineas * interlineado() + rellenoV * 2;
        return r;
    }

    private void dibujarRenglon(Renglon r) {
        double tamano = tam();
        double rellenoV = d.getEspaciado().relleno * 0.5;
        double x = xColumna();
        double base = y + rellenoV + tamano;

        if (r.enBloque) {
            pag.textoSubrayado(x, base, r.nombreBloque + ":", tamano, false, NEGRO);
            double yb = base + interlineado();
            for (String linea : r.textoBloque) {
                pag.texto(x + 16, yb, linea, tamano, false, false, NEGRO);
                yb += interlineado();
            }
            y += r.alto;
            return;
        }

        double yb = base;
        for (List<TextoPdf.Pieza> renglon : r.izquierda) {
            pag.piezas(x, yb, renglon, tamano, false, NEGRO);
            yb += interlineado();
        }
        yb = base;
        double xRef = x + anchoColumna * 0.56;
        for (List<TextoPdf.Pieza> renglon : r.referencia) {
            pag.piezas(xRef, yb, renglon, tamano, false, NEGRO);
            yb += interlineado();
        }
        y += r.alto;
    }

    // ------------------------------------------------------------------ firma

    private void firma() {
        dibujarFirma(yMaximo, true, this::hojaSiguiente);
    }

    // ------------------------------------------------------------------ utilidades

    private double parrafoSerif(double x, String texto, double anchoMax, double tamano, boolean cursiva, int color) {
        List<String> renglones = TextoPdf.envolverTexto(texto, anchoMax, tamano, false, cursiva, true);
        double yb = y;
        for (String r : renglones) {
            yb += interlineado();
            pag.texto(x, yb, r, tamano, false, cursiva, color);
        }
        return renglones.size() * interlineado();
    }

    /** "M.P 311" -&gt; "311" (para no escribir "M.P M.P 311" si ya se cargó con el prefijo). */
    static String sinPrefijoMatricula(String matricula) {
        return matricula.trim().replaceFirst("(?i)^m\\.?\\s*p\\.?\\s*", "");
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
