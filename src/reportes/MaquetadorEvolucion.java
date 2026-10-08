package reportes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import modelo.EvolucionPaciente;

/**
 * PDF del "Registro evolutivo" de un paciente: datos del paciente, resumen (visitas, meses
 * seguidos), visitas por mes en barras, un cuadro por cada edad, el detalle de las visitas y la
 * evolución de cada resultado (con un gráfico de línea cuando hay dos o más valores numéricos).
 * Usa el mismo encabezado del laboratorio que el informe de resultados.
 */
public final class MaquetadorEvolucion extends MaquetadorBase {

    private static final int AZUL = 0x2563EB;

    private final EvolucionPaciente ev;
    private final SimpleDateFormat fecha = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat fechaCorta = new SimpleDateFormat("dd/MM/yy");

    private MaquetadorEvolucion(DocumentoMaquetado doc, EvolucionPaciente ev, DisenoInforme d, ConfigInforme c) {
        super(doc, d, c, false);
        this.ev = ev;
    }

    public static DocumentoMaquetado armar(EvolucionPaciente ev, DisenoInforme diseno, ConfigInforme config) {
        DisenoInforme d = diseno.copia();
        d.setEstilo(DisenoInforme.Estilo.MODERNO);
        DocumentoMaquetado doc = new DocumentoMaquetado("Registro evolutivo - " + guion(ev.getNombre()));
        new MaquetadorEvolucion(doc, ev, d, config).armarTodo();
        return doc;
    }

    private void armarTodo() {
        abrirHoja();
        encabezadoCompleto();
        bloquePaciente();
        resumen();
        visitasPorMes();
        porEdad();
        detalleVisitas();
        resultados();
        dibujarPies("Registro evolutivo   ·   ");
    }

    private void hojaSiguiente() {
        abrirHoja();
        encabezadoCorto(TextoPdf.recortarConPuntos("Registro evolutivo   ·   " + guion(ev.getNombre())
                + "   ·   DNI " + guion(ev.getDni()), ancho * 0.62, 8.5, false));
    }

    private void asegurar(double alto) {
        if (!entra(alto)) {
            hojaSiguiente();
        }
    }

    // ------------------------------------------------------------------ paciente y resumen

    private void bloquePaciente() {
        double alto = 58;
        pag.rect(izq, y, ancho, alto, 0xFAFBFA, LINEA, 0.7);
        pag.texto(izq + 9, y + 17, "REGISTRO EVOLUTIVO DEL PACIENTE", 9, true, false, acento());
        pag.textoDerecha(der - 9, y + 17, "Emitido el " + fecha.format(new Date()), 7.5, false, false, GRIS_CLARO);
        double mitad = izq + ancho / 2;
        dato(izq + 9, y + 34, "Paciente", guion(ev.getNombre()), true, ancho / 2 - 20);
        dato(izq + 9, y + 49, "DNI", guion(ev.getDni()), false, ancho / 2 - 20);
        String nac = ev.getFechaNacimiento() == null ? "-" : fecha.format(ev.getFechaNacimiento())
                + "  (" + EvolucionPaciente.textoEdad(ev.edadActual()) + ")";
        dato(mitad, y + 34, "Nacimiento", nac, false, ancho / 2 - 12);
        dato(mitad, y + 49, "Sexo", guion(ev.getSexo()), false, ancho / 2 - 12);
        y += alto + 12;
    }

    private void dato(double x, double yb, String etiqueta, String valor, boolean negrita, double anchoMax) {
        pag.texto(x, yb, etiqueta, 8, false, false, GRIS);
        pag.texto(x + 62, yb, TextoPdf.recortarConPuntos(valor, anchoMax - 62, 9.5, negrita), 9.5, negrita, false, NEGRO);
    }

    private void resumen() {
        String[][] cajas = {
            {String.valueOf(ev.getVisitas().size()), ev.getVisitas().size() == 1 ? "visita" : "visitas en total"},
            {String.valueOf(ev.rachaMayorDeMeses()), ev.rachaMayorDeMeses() == 1 ? "mes seguido (racha mayor)"
                    : "meses seguidos (racha mayor)"},
            {String.valueOf(ev.mesesConVisitas()), "meses distintos con visitas"},
            {ev.primeraVisita() == null ? "-" : fechaCorta.format(ev.primeraVisita()) + " al "
                    + fechaCorta.format(ev.ultimaVisita()), "primera y última visita"},
        };
        double sep = 8;
        double w = (ancho - sep * 3) / 4;
        double alto = 44;
        asegurar(alto + 10);
        for (int i = 0; i < cajas.length; i++) {
            double x = izq + i * (w + sep);
            pag.rect(x, y, w, alto, acentoSuave(), null, 0);
            double tam = cajas[i][0].length() > 8 ? 10.5 : 16;
            pag.texto(x + 8, y + 20, TextoPdf.recortarConPuntos(cajas[i][0], w - 16, tam, true), tam, true, false, acento());
            pag.texto(x + 8, y + 35, TextoPdf.recortarConPuntos(cajas[i][1], w - 16, 7.5, false), 7.5, false, false, GRIS);
        }
        y += alto + 16;
    }

    private void tituloSeccion(String titulo, String bajada) {
        asegurar(60);
        pag.texto(izq, y + 10, titulo, 11, true, false, acento());
        y += 14;
        if (bajada != null) {
            y += parrafo(izq, y, bajada, ancho, 8, false, true, GRIS, 10);
        }
        y += 6;
    }

    // ------------------------------------------------------------------ visitas por mes

    private void visitasPorMes() {
        Map<String, Integer> meses = ev.visitasPorMes();
        if (meses.isEmpty()) {
            return;
        }
        List<String> claves = new ArrayList<>(meses.keySet());
        if (claves.size() > 24) {
            claves = claves.subList(claves.size() - 24, claves.size());
        }
        tituloSeccion("Visitas por mes", "Cada barra es un mes; los meses sin barra no vino. "
                + (meses.size() > 24 ? "Se muestran los últimos 24 meses." : ""));
        double alto = 90;
        asegurar(alto + 30);
        int maximo = 1;
        for (String k : claves) {
            maximo = Math.max(maximo, meses.get(k));
        }
        double base = y + alto;
        pag.linea(izq, base, der, base, 0.6, LINEA);
        double paso = ancho / claves.size();
        double anchoBarra = Math.min(28, paso * 0.6);
        for (int i = 0; i < claves.size(); i++) {
            int n = meses.get(claves.get(i));
            double cx = izq + paso * i + paso / 2;
            if (n > 0) {
                double h = (alto - 14) * n / maximo;
                pag.rect(cx - anchoBarra / 2, base - h, anchoBarra, h, acento(), null, 0);
                pag.textoCentrado(cx, base - h - 3, String.valueOf(n), 7.5, true, false, NEGRO);
            }
            String etiqueta = EvolucionPaciente.nombreMes(claves.get(i));
            if (claves.size() > 12) {
                etiqueta = etiqueta.substring(0, Math.min(3, etiqueta.length())) + " " + claves.get(i).substring(2, 4);
            }
            pag.textoCentrado(cx, base + 10, etiqueta, claves.size() > 12 ? 6 : 7, false, false, GRIS);
        }
        y = base + 24;
    }

    // ------------------------------------------------------------------ por edad / visitas

    private void porEdad() {
        List<EvolucionPaciente.PorEdad> edades = ev.porEdad();
        if (edades.isEmpty()) {
            return;
        }
        tituloSeccion("Por edad", "Cuántas veces vino con cada edad y qué estudios se hizo.");
        List<String[]> filas = new ArrayList<>();
        for (EvolucionPaciente.PorEdad pe : edades) {
            filas.add(new String[]{EvolucionPaciente.textoEdad(pe.edad), String.valueOf(pe.visitas),
                String.join(", ", pe.estudios)});
        }
        tabla(new String[]{"Edad", "Visitas", "Estudios realizados"}, new double[]{0.14, 0.11, 0.75}, filas, null);
    }

    private void detalleVisitas() {
        if (ev.getVisitas().isEmpty()) {
            return;
        }
        tituloSeccion("Detalle de las visitas", null);
        List<String[]> filas = new ArrayList<>();
        for (EvolucionPaciente.Visita v : ev.getVisitas()) {
            filas.add(new String[]{v.fecha == null ? "-" : fecha.format(v.fecha), EvolucionPaciente.textoEdad(v.edad),
                guion(v.numeroOrden), guion(v.estudios)});
        }
        tabla(new String[]{"Fecha", "Edad", "Orden N°", "Estudios"}, new double[]{0.15, 0.12, 0.15, 0.58}, filas, null);
    }

    // ------------------------------------------------------------------ resultados

    private void resultados() {
        List<EvolucionPaciente.Serie> series = ev.series();
        if (series.isEmpty()) {
            return;
        }
        tituloSeccion("Evolución de los resultados", "Valores cargados en cada visita. En rojo, los que quedaron "
                + "fuera del valor de referencia actual del Catálogo.");
        List<String[]> sueltos = new ArrayList<>();
        List<Integer> coloresSueltos = new ArrayList<>();
        for (EvolucionPaciente.Serie s : series) {
            if (s.graficable()) {
                serieConGrafico(s);
            } else {
                for (EvolucionPaciente.Medicion m : s.mediciones) {
                    sueltos.add(new String[]{s.etiqueta(), m.fecha == null ? "-" : fecha.format(m.fecha),
                        EvolucionPaciente.textoEdad(m.edad), m.valor + (m.unidad == null || m.numero == null ? "" : " " + m.unidad)});
                    coloresSueltos.add(fueraDeRango(m) ? ROJO : NEGRO);
                }
            }
        }
        if (!sueltos.isEmpty()) {
            asegurar(50);
            pag.texto(izq, y + 10, "Otros resultados (una sola vez o en texto)", 9.5, true, false, NEGRO);
            y += 16;
            tabla(new String[]{"Parámetro", "Fecha", "Edad", "Resultado"}, new double[]{0.38, 0.14, 0.12, 0.36}, sueltos,
                    coloresSueltos);
        }
    }

    private static boolean fueraDeRango(EvolucionPaciente.Medicion m) {
        return "Alto".equalsIgnoreCase(m.estado) || "Bajo".equalsIgnoreCase(m.estado);
    }

    private void serieConGrafico(EvolucionPaciente.Serie s) {
        List<EvolucionPaciente.Medicion> puntos = s.numericas();
        double altoGrafico = 92;
        double altoTabla = 14 * 2 + 6;
        asegurar(20 + altoGrafico + 30 + altoTabla);
        String titulo = s.etiqueta() + (s.unidad == null || s.unidad.trim().isEmpty() ? "" : "  (" + s.unidad.trim() + ")");
        pag.texto(izq, y + 10, TextoPdf.recortarConPuntos(titulo, ancho * 0.7, 9.5, true), 9.5, true, false, NEGRO);
        String ref = puntos.get(puntos.size() - 1).referencia;
        if (ref != null && !ref.trim().isEmpty()) {
            pag.textoDerecha(der, y + 10, TextoPdf.recortarConPuntos("Ref.: " + ref.trim(), ancho * 0.45, 7.5, false), 7.5,
                    false, true, GRIS);
        }
        y += 18;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (EvolucionPaciente.Medicion m : puntos) {
            min = Math.min(min, m.numero);
            max = Math.max(max, m.numero);
        }
        if (max - min < 1e-9) {
            double margen = Math.abs(max) * 0.1 + 1;
            min -= margen;
            max += margen;
        } else {
            double margen = (max - min) * 0.15;
            min -= margen;
            max += margen;
        }
        double xIni = izq + 46;
        double xFin = der - 14;
        double yTop = y + 6;
        double yBase = y + altoGrafico;
        // Grilla con 4 líneas y sus valores.
        for (int i = 0; i <= 3; i++) {
            double v = min + (max - min) * i / 3;
            double yy = yBase - (yBase - yTop) * i / 3;
            pag.linea(xIni, yy, xFin, yy, 0.4, i == 0 ? GRIS_CLARO : LINEA);
            pag.textoDerecha(xIni - 5, yy + 2.5, numero(v), 6.5, false, false, GRIS);
        }
        double paso = puntos.size() == 1 ? 0 : (xFin - xIni - 20) / (puntos.size() - 1);
        double xAnterior = 0;
        double yAnterior = 0;
        for (int i = 0; i < puntos.size(); i++) {
            EvolucionPaciente.Medicion m = puntos.get(i);
            double x = xIni + 10 + paso * i;
            double yy = yBase - (yBase - yTop) * (m.numero - min) / (max - min);
            if (i > 0) {
                pag.linea(xAnterior, yAnterior, x, yy, 1.4, AZUL);
            }
            xAnterior = x;
            yAnterior = yy;
        }
        for (int i = 0; i < puntos.size(); i++) {
            EvolucionPaciente.Medicion m = puntos.get(i);
            double x = xIni + 10 + paso * i;
            double yy = yBase - (yBase - yTop) * (m.numero - min) / (max - min);
            int color = fueraDeRango(m) ? ROJO : AZUL;
            pag.rect(x - 2.6, yy - 2.6, 5.2, 5.2, color, 0xFFFFFF, 0.8);
            pag.textoCentrado(x, yy - 5, m.valor.trim(), 7, true, false, fueraDeRango(m) ? ROJO : NEGRO);
            boolean mostrarEtiqueta = puntos.size() <= 10 || i % (int) Math.ceil(puntos.size() / 10.0) == 0
                    || i == puntos.size() - 1;
            if (mostrarEtiqueta) {
                pag.textoCentrado(x, yBase + 10, m.fecha == null ? "-" : fechaCorta.format(m.fecha), 6.5, false, false, GRIS);
                if (m.edad >= 0) {
                    pag.textoCentrado(x, yBase + 18, m.edad + " a", 6, false, false, GRIS_CLARO);
                }
            }
        }
        y = yBase + 28;
        double variacion = puntos.get(puntos.size() - 1).numero - puntos.get(0).numero;
        String texto = "Primer valor " + puntos.get(0).valor.trim() + " (" + fechaCorta.format(puntos.get(0).fecha)
                + ")  ·  último " + puntos.get(puntos.size() - 1).valor.trim() + " ("
                + fechaCorta.format(puntos.get(puntos.size() - 1).fecha) + ")  ·  "
                + (Math.abs(variacion) < 1e-9 ? "sin cambios" : (variacion > 0 ? "subió " : "bajó ") + numero(Math.abs(variacion)));
        pag.texto(izq, y, TextoPdf.recortarConPuntos(texto, ancho, 7.5, false), 7.5, false, true, GRIS);
        y += 18;
    }

    private static String numero(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9 && Math.abs(v) < 1e12) {
            return String.valueOf((long) Math.rint(v));
        }
        String t = Math.abs(v) >= 100 ? String.format(Locale.ROOT, "%.0f", v)
                : Math.abs(v) >= 10 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
        return t.contains(".") ? t.replaceAll("0+$", "").replaceAll("\\.$", "") : t;
    }

    // ------------------------------------------------------------------ tabla genérica

    /**
     * Tabla simple con encabezado de color y renglones que se cortan si no entran. Si no entra en
     * la hoja, sigue en la siguiente repitiendo el encabezado.
     */
    private void tabla(String[] titulos, double[] proporciones, List<String[]> filas, List<Integer> colores) {
        double tam = 8.5;
        double[] anchos = new double[titulos.length];
        double[] xs = new double[titulos.length];
        double x = izq;
        for (int i = 0; i < titulos.length; i++) {
            anchos[i] = ancho * proporciones[i];
            xs[i] = x;
            x += anchos[i];
        }
        Runnable encabezado = () -> {
            pag.rect(izq, y, ancho, 16, acentoSuave(), null, 0);
            for (int i = 0; i < titulos.length; i++) {
                pag.texto(xs[i] + 5, y + 11, titulos[i], 8, true, false, acento());
            }
            y += 16;
        };
        asegurar(16 + 16);
        encabezado.run();
        for (int f = 0; f < filas.size(); f++) {
            String[] fila = filas.get(f);
            List<List<String>> renglones = new ArrayList<>();
            int max = 1;
            for (int i = 0; i < fila.length; i++) {
                List<String> r = TextoPdf.envolverTexto(guion(fila[i]), anchos[i] - 10, tam, i == 0);
                renglones.add(r);
                max = Math.max(max, r.size());
            }
            double alto = max * 11 + 6;
            if (!entra(alto)) {
                hojaSiguiente();
                encabezado.run();
            }
            int color = colores == null ? NEGRO : colores.get(f);
            for (int i = 0; i < fila.length; i++) {
                double yb = y + 3;
                for (String r : renglones.get(i)) {
                    yb += 11;
                    boolean esResultado = i == fila.length - 1 && colores != null;
                    pag.texto(xs[i] + 5, yb - 2, r, tam, i == 0, false, esResultado ? color : NEGRO);
                }
            }
            y += alto;
            pag.linea(izq, y, der, y, 0.4, LINEA);
        }
        y += 14;
    }
}
