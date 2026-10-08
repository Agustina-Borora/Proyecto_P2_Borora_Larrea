package reportes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import modelo.Prestacion;

/**
 * Arma el "Comprobante de Orden" (datos del paciente, cobertura, análisis pedidos y total) con el
 * mismo motor y el mismo diseño que el informe de resultados. Se usa solo (al crear una orden en
 * Nuevo Análisis, si así está configurado) o pegado al final del informe de resultados.
 */
public class MaquetadorComprobante extends MaquetadorBase {

    private final ComprobanteOrdenDatos datos;
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private MaquetadorComprobante(DocumentoMaquetado doc, ComprobanteOrdenDatos datos, DisenoInforme d,
            ConfigInforme c, boolean modoEjemplo) {
        super(doc, d, c, modoEjemplo);
        this.datos = datos;
    }

    /** Comprobante solo, en un documento propio. */
    public static DocumentoMaquetado armar(ComprobanteOrdenDatos datos, DisenoInforme diseno, ConfigInforme config) {
        DocumentoMaquetado doc = new DocumentoMaquetado("Comprobante - Orden " + guion(datos.getNumeroOrden()));
        agregarA(doc, datos, diseno, config, false);
        return doc;
    }

    /** Agrega las hojas del comprobante al final de un documento ya armado. */
    public static void agregarA(DocumentoMaquetado doc, ComprobanteOrdenDatos datos, DisenoInforme diseno,
            ConfigInforme config, boolean modoEjemplo) {
        new MaquetadorComprobante(doc, datos, diseno, config, modoEjemplo).armarTodo();
    }

    private void armarTodo() {
        abrirHoja();
        encabezadoCompleto();

        // Título
        pag.rect(izq, y, ancho, 22, acentoSuave(), null, 0);
        pag.texto(izq + 8, y + 15, "COMPROBANTE DE ORDEN", 11, true, false, acento());
        String derecha = "Orden N° " + guion(datos.getNumeroOrden()) + "   ·   "
                + (datos.getFecha() != null ? formatoFecha.format(datos.getFecha()) : "-");
        pag.textoDerecha(der - 8, y + 15, derecha, 9, true, false, acento());
        y += 32;

        // Datos
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Paciente", guion(datos.getNombrePaciente())});
        filas.add(new String[]{"DNI", guion(datos.getDni())});
        String contacto = unir("   ·   ", datos.getTelefono(), datos.getEmail());
        if (!contacto.isEmpty()) {
            filas.add(new String[]{"Contacto", contacto});
        }
        if (ConfigInforme.tiene(datos.getMedicoDerivante())) {
            filas.add(new String[]{"Médico derivante", datos.getMedicoDerivante().trim()});
        }
        boolean esObraSocial = "OBRA_SOCIAL".equals(datos.getCobertura());
        boolean esMixto = "MIXTO".equals(datos.getCobertura());
        String cobertura = textoCobertura(datos.getCobertura());
        if (esObraSocial || esMixto) {
            cobertura += " (" + guion(datos.getObraSocial()) + ")";
        }
        filas.add(new String[]{"Cobertura", cobertura});
        if (esObraSocial || esMixto) {
            filas.add(new String[]{"Plan / Afiliado", guion(datos.getPlan()) + "   ·   N° " + guion(datos.getNroAfiliado())});
        }
        if (!esObraSocial && ConfigInforme.tiene(datos.getMetodoPago())) {
            filas.add(new String[]{"Método de pago", datos.getMetodoPago()});
        }
        if (esMixto && datos.getMontoEfectivo() != null) {
            filas.add(new String[]{"Abona en efectivo", "$ " + plata(datos.getMontoEfectivo())});
        }

        double anchoEtiqueta = 100;
        for (String[] f : filas) {
            List<String> renglones = TextoPdf.envolverTexto(f[1], ancho - anchoEtiqueta, 10, false);
            pag.texto(izq, y + 10, f[0], 8.5, false, false, GRIS);
            for (String r : renglones) {
                pag.texto(izq + anchoEtiqueta, y + 10, r, 10, false, false, NEGRO);
                y += 13.5;
            }
            y += 1.5;
        }
        y += 10;

        // Tabla de análisis
        double wCodigo = 62;
        double wCobertura = 92;
        double xAnalisis = izq + wCodigo;
        double xCob = der - wCobertura;
        double wAnalisis = xCob - xAnalisis;
        String valorCobertura = "PARTICULAR".equals(datos.getCobertura()) || datos.getCobertura() == null
                ? "Particular" : "Obra social";

        cabecera(xAnalisis, xCob);
        List<Prestacion> analisis = datos.getAnalisis() == null ? new ArrayList<Prestacion>() : datos.getAnalisis();
        double tam = Math.max(9, d.getTamanoLetra());
        for (int i = 0; i < analisis.size(); i++) {
            Prestacion p = analisis.get(i);
            List<String> renglones = TextoPdf.envolverTexto(guion(p.getNombrePrestacion()), wAnalisis - 10, tam, false);
            double alto = renglones.size() * tam * 1.25 + 8;
            if (!entra(alto + 30)) {
                abrirHoja();
                encabezadoCorto("Comprobante  ·  Orden N° " + guion(datos.getNumeroOrden()));
                cabecera(xAnalisis, xCob);
            }
            if (d.getEstiloTabla() == DisenoInforme.EstiloTabla.CEBRA && i % 2 == 1) {
                pag.rect(izq, y, ancho, alto, CEBRA, null, 0);
            }
            double yb = y + 4 + tam * 0.86 + 1;
            pag.texto(izq + 5, yb, p.getCodigo() > 0 ? String.valueOf(p.getCodigo()) : "-", tam, false, false, GRIS);
            for (String r : renglones) {
                pag.texto(xAnalisis + 5, yb, r, tam, false, false, NEGRO);
                yb += tam * 1.25;
            }
            pag.texto(xCob + 5, y + 4 + tam * 0.86 + 1, valorCobertura, tam, false, false, GRIS);
            y += alto;
            if (d.getEstiloTabla() == DisenoInforme.EstiloTabla.LINEAS) {
                pag.linea(izq, y, der, y, 0.5, LINEA);
            }
        }

        // Total
        if (!entra(30)) {
            abrirHoja();
            encabezadoCorto("Comprobante  ·  Orden N° " + guion(datos.getNumeroOrden()));
        }
        y += 8;
        String total = "Total:  $ " + (datos.getTotal() != null ? plata(datos.getTotal()) : "-");
        double anchoTotal = TextoPdf.ancho(total, 12.5, true) + 20;
        pag.rect(der - anchoTotal, y, anchoTotal, 22, acentoSuave(), null, 0);
        pag.textoDerecha(der - 10, y + 15.5, total, 12.5, true, false, acento());
        y += 30;

        if (modoEjemplo && !ConfigInforme.tiene(c.leyendaPie)) {
            pag.texto(izq, y + 10, "(Podés poner una leyenda al pie en Configuración > Informes PDF)", 8, false, true, AVISO);
        }

        dibujarPies("Comprobante  ·  ");
    }

    private void cabecera(double xAnalisis, double xCob) {
        pag.texto(izq + 5, y + 10, "CÓDIGO", 7.5, true, false, GRIS);
        pag.texto(xAnalisis + 5, y + 10, "ANÁLISIS", 7.5, true, false, GRIS);
        pag.texto(xCob + 5, y + 10, "COBERTURA", 7.5, true, false, GRIS);
        pag.linea(izq, y + 14, der, y + 14, 0.9, acento());
        y += 17;
    }

    private static String textoCobertura(String clave) {
        if ("OBRA_SOCIAL".equals(clave)) {
            return "Obra Social";
        }
        if ("MIXTO".equals(clave)) {
            return "Mixto (obra social + efectivo)";
        }
        return "Particular";
    }

    private static String plata(BigDecimal monto) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "AR"));
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", simbolos).format(monto);
    }
}
