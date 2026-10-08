package reportes;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo lo que hace falta para medir y acomodar texto en los informes, sin depender de la
 * pantalla ni de la impresora: anchos reales de la letra Helvetica (la misma que usa el PDF, y
 * que tiene exactamente las mismas medidas que Arial, la que se ve en pantalla en Windows), los
 * caracteres especiales que la letra estándar del PDF no trae, y el corte de líneas largas
 * respetando los colores de cada tramo.
 *
 * <p>Por qué medir "a mano" en vez de preguntarle a Java: así la vista previa, lo impreso y el
 * PDF quedan EXACTAMENTE iguales (mismos cortes de línea, mismas páginas), sin importar qué
 * impresora o qué pantalla haya.</p>
 */
public final class TextoPdf {

    /** Ancho (en milésimas del tamaño de letra) de cada caracter WinAnsi 32..255 -- Helvetica. */
    private static final int[] ANCHOS_REGULAR = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556, 556, 556,
        556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556,
        556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 761,
        556, 0, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0, 0, 222, 222, 333, 333, 350, 556, 1000, 333,
        1000, 500, 333, 944, 0, 500, 667, 278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333, 400,
        584, 333, 333, 333, 556, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611, 667, 667, 667, 667, 667, 667, 1000, 722, 667,
        667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556,
        556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278, 556, 556, 556, 556, 556, 556, 556, 584, 611,
        556, 556, 556, 556, 500, 556, 500
    };

    /** Lo mismo para Helvetica-Bold (negrita). */
    private static final int[] ANCHOS_NEGRITA = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556, 556, 556,
        556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556, 611, 556, 611, 556, 333, 611,
        611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584, 761,
        556, 0, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0, 0, 278, 278, 500, 500, 350, 556, 1000, 333,
        1000, 556, 333, 944, 0, 500, 667, 278, 333, 556, 556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333, 400,
        584, 333, 333, 333, 611, 556, 278, 333, 333, 365, 556, 834, 834, 834, 611, 722, 722, 722, 722, 722, 722, 1000, 722, 667,
        667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556,
        556, 556, 556, 556, 556, 889, 556, 556, 556, 556, 556, 278, 278, 278, 278, 611, 611, 611, 611, 611, 611, 611, 584, 611,
        611, 611, 611, 611, 556, 611, 556
    };


    /** Times (letra "clásica", la del estilo de informe Clásico) -- normal, negrita, cursiva y negrita cursiva. */
    private static final int[] ANCHOS_TIMES = {
        250, 333, 408, 500, 500, 833, 778, 180, 333, 333, 500, 564, 250, 333, 250, 278, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 278, 278, 564, 564, 564, 444, 921, 722, 667, 667, 722, 611, 556, 722, 722, 333, 389, 722, 611, 889, 722, 722,
        556, 722, 667, 556, 611, 722, 722, 944, 722, 722, 611, 333, 278, 333, 469, 500, 333, 444, 500, 444, 500, 444, 333, 500,
        500, 278, 278, 500, 278, 778, 500, 500, 500, 500, 333, 389, 278, 500, 500, 722, 500, 500, 444, 480, 200, 480, 541, 761,
        500, 0, 333, 500, 444, 1000, 500, 500, 333, 1000, 556, 333, 889, 0, 611, 0, 0, 333, 333, 444, 444, 350, 500, 1000,
        333, 980, 389, 333, 722, 0, 444, 722, 250, 333, 500, 500, 500, 500, 200, 500, 333, 760, 276, 500, 564, 333, 760, 333,
        400, 564, 300, 300, 333, 500, 453, 250, 333, 300, 310, 500, 750, 750, 750, 444, 722, 722, 722, 722, 722, 722, 889, 667,
        611, 611, 611, 611, 333, 333, 333, 333, 722, 722, 722, 722, 722, 722, 722, 564, 722, 722, 722, 722, 722, 722, 556, 500,
        444, 444, 444, 444, 444, 444, 667, 444, 444, 444, 444, 444, 278, 278, 278, 278, 500, 500, 500, 500, 500, 500, 500, 564,
        500, 500, 500, 500, 500, 500, 500, 500
    };
    private static final int[] ANCHOS_TIMES_NEGRITA = {
        250, 333, 555, 500, 500, 1000, 833, 278, 333, 333, 500, 570, 250, 333, 250, 278, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 333, 333, 570, 570, 570, 500, 930, 722, 667, 722, 722, 667, 611, 778, 778, 389, 500, 778, 667, 944, 722, 778,
        611, 778, 722, 556, 667, 722, 722, 1000, 722, 722, 667, 333, 278, 333, 581, 500, 333, 500, 556, 444, 556, 444, 333, 500,
        556, 278, 333, 556, 278, 833, 556, 500, 556, 556, 444, 389, 333, 556, 500, 722, 500, 500, 444, 394, 220, 394, 520, 761,
        500, 0, 333, 500, 500, 1000, 500, 500, 333, 1000, 556, 333, 1000, 0, 667, 0, 0, 333, 333, 500, 500, 350, 500, 1000,
        333, 1000, 389, 333, 722, 0, 444, 722, 250, 333, 500, 500, 500, 500, 220, 500, 333, 747, 300, 500, 570, 333, 747, 333,
        400, 570, 300, 300, 333, 556, 540, 250, 333, 300, 330, 500, 750, 750, 750, 500, 722, 722, 722, 722, 722, 722, 1000, 722,
        667, 667, 667, 667, 389, 389, 389, 389, 722, 722, 778, 778, 778, 778, 778, 570, 778, 722, 722, 722, 722, 722, 611, 556,
        500, 500, 500, 500, 500, 500, 722, 444, 444, 444, 444, 444, 278, 278, 278, 278, 500, 556, 500, 500, 500, 500, 500, 570,
        500, 556, 556, 556, 556, 500, 556, 500
    };
    private static final int[] ANCHOS_TIMES_CURSIVA = {
        250, 333, 420, 500, 500, 833, 778, 214, 333, 333, 500, 675, 250, 333, 250, 278, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 333, 333, 675, 675, 675, 500, 920, 611, 611, 667, 722, 611, 611, 722, 722, 333, 444, 667, 556, 833, 667, 722,
        611, 722, 611, 500, 556, 722, 611, 833, 611, 556, 556, 389, 278, 389, 422, 500, 333, 500, 500, 444, 500, 444, 278, 500,
        500, 278, 278, 444, 278, 722, 500, 500, 500, 500, 389, 389, 278, 500, 444, 667, 444, 444, 389, 400, 275, 400, 541, 761,
        500, 0, 333, 500, 556, 889, 500, 500, 333, 1000, 500, 333, 944, 0, 556, 0, 0, 333, 333, 556, 556, 350, 500, 889,
        333, 980, 389, 333, 667, 0, 389, 556, 250, 389, 500, 500, 500, 500, 275, 500, 333, 760, 276, 500, 675, 333, 760, 333,
        400, 675, 300, 300, 333, 500, 523, 250, 333, 300, 310, 500, 750, 750, 750, 500, 611, 611, 611, 611, 611, 611, 889, 667,
        611, 611, 611, 611, 333, 333, 333, 333, 722, 667, 722, 722, 722, 722, 722, 675, 722, 722, 722, 722, 722, 556, 611, 500,
        500, 500, 500, 500, 500, 500, 667, 444, 444, 444, 444, 444, 278, 278, 278, 278, 500, 500, 500, 500, 500, 500, 500, 675,
        500, 500, 500, 500, 500, 444, 500, 444
    };
    private static final int[] ANCHOS_TIMES_NEGRITA_CURSIVA = {
        250, 389, 555, 500, 500, 833, 778, 278, 333, 333, 500, 570, 250, 333, 250, 278, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 333, 333, 570, 570, 570, 500, 832, 667, 667, 667, 722, 667, 667, 722, 778, 389, 500, 667, 611, 889, 722, 722,
        611, 722, 667, 556, 611, 722, 667, 889, 667, 611, 611, 333, 278, 333, 570, 500, 333, 500, 500, 444, 500, 444, 333, 500,
        556, 278, 278, 500, 278, 778, 556, 500, 500, 500, 389, 389, 278, 556, 444, 667, 500, 444, 389, 348, 220, 348, 570, 761,
        500, 0, 333, 500, 500, 1000, 500, 500, 333, 1000, 556, 333, 944, 0, 611, 0, 0, 333, 333, 500, 500, 350, 500, 1000,
        333, 1000, 389, 333, 722, 0, 389, 611, 250, 389, 500, 500, 500, 500, 220, 500, 333, 747, 266, 500, 606, 333, 747, 333,
        400, 570, 300, 300, 333, 576, 500, 250, 333, 300, 300, 500, 750, 750, 750, 500, 667, 667, 667, 667, 667, 667, 944, 667,
        667, 667, 667, 667, 389, 389, 389, 389, 722, 722, 722, 722, 722, 722, 722, 570, 722, 722, 722, 722, 722, 611, 611, 500,
        500, 500, 500, 500, 500, 500, 722, 444, 444, 444, 444, 444, 278, 278, 278, 278, 500, 556, 500, 500, 500, 500, 500, 570,
        500, 556, 556, 556, 556, 444, 500, 444
    };

    /** Achique de los superíndices (⁶, ⁴, etc.) respecto de la letra normal. */
    static final double ESCALA_SUPERINDICE = 0.68;

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private TextoPdf() {
    }

    // ------------------------------------------------------------------ caracteres

    /**
     * Deja el texto listo para el informe: cambia los caracteres que la letra estándar del PDF
     * no tiene por su equivalente más parecido ("≤" por "<=", la μ griega por la µ de micro,
     * etc.). Los superíndices que no existen en esa letra (⁴ ⁵ ⁶ ...) se dejan tal cual -- se
     * dibujan como números chiquitos y levantados, ver {@link #esSuperindiceDibujado}.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        CharsetEncoder codificador = WIN_ANSI.newEncoder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\r' || c == '\u0001') {
                continue;
            }
            if (c == '\t') {
                sb.append(' ');
                continue;
            }
            if (c == '\n' || esSuperindiceDibujado(c)) {
                sb.append(c);
                continue;
            }
            String reemplazo = reemplazo(c);
            if (reemplazo != null) {
                sb.append(reemplazo);
                continue;
            }
            if (c < 32) {
                continue;
            }
            sb.append(codificador.canEncode(c) ? c : '?');
        }
        return sb.toString();
    }

    private static String reemplazo(char c) {
        switch (c) {
            case '≤': return "<=";
            case '≥': return ">=";
            case '−': return "-";
            case '‐': return "-";
            case '‑': return "-";
            case 'μ': return "µ";
            case ' ': return " ";
            case '→': return "->";
            case '←': return "<-";
            case '↑': return "^";
            case '↓': return "v";
            case '≈': return "~";
            case '≠': return "!=";
            case '∞': return "inf.";
            case '✓': return "OK";
            case '✔': return "OK";
            case '⚠': return "!";
            case ' ': return " ";
            case ' ': return " ";
            default: return null;
        }
    }

    /**
     * Superíndices que la letra del PDF no trae (⁰ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻): se dibujan como el número
     * normal, más chico y levantado -- queda igual a la vista. ¹ ² ³ sí existen y se usan tal cual.
     */
    public static boolean esSuperindiceDibujado(char c) {
        return c == '⁰' || (c >= '⁴' && c <= '⁹') || c == '⁺' || c == '⁻';
    }

    /** El número/signo "normal" que corresponde a un superíndice dibujado. */
    public static char baseSuperindice(char c) {
        if (c == '⁰') {
            return '0';
        }
        if (c >= '⁴' && c <= '⁹') {
            return (char) ('4' + (c - '⁴'));
        }
        if (c == '⁺') {
            return '+';
        }
        if (c == '⁻') {
            return '-';
        }
        return c;
    }

    /** Código WinAnsi (0..255) de un caracter ya normalizado, o '?' si no lo tiene. */
    public static int codigoWinAnsi(char c) {
        if (c < 128) {
            return c;
        }
        byte[] bytes = String.valueOf(c).getBytes(WIN_ANSI);
        if (bytes.length == 1 && bytes[0] != '?') {
            return bytes[0] & 0xFF;
        }
        return '?';
    }

    // ------------------------------------------------------------------ medidas

    /** Ancho en puntos de un caracter ya normalizado (letra Helvetica/Arial). */
    public static double anchoCaracter(char c, double tamano, boolean negrita) {
        return anchoCaracter(c, tamano, negrita, false, false);
    }

    /**
     * Ancho en puntos de un caracter ya normalizado.
     *
     * @param serif true = letra clásica (Times), false = Helvetica/Arial.
     */
    public static double anchoCaracter(char c, double tamano, boolean negrita, boolean cursiva, boolean serif) {
        if (c == '\n') {
            return 0;
        }
        if (esSuperindiceDibujado(c)) {
            return anchoCaracter(baseSuperindice(c), tamano * ESCALA_SUPERINDICE, negrita, cursiva, serif);
        }
        int codigo = codigoWinAnsi(c);
        if (codigo < 32 || codigo > 255) {
            codigo = '?';
        }
        int[] tabla;
        if (serif) {
            tabla = negrita ? (cursiva ? ANCHOS_TIMES_NEGRITA_CURSIVA : ANCHOS_TIMES_NEGRITA)
                    : (cursiva ? ANCHOS_TIMES_CURSIVA : ANCHOS_TIMES);
        } else {
            tabla = negrita ? ANCHOS_NEGRITA : ANCHOS_REGULAR;
        }
        int ancho = tabla[codigo - 32];
        if (ancho == 0) {
            ancho = 556;
        }
        return ancho * tamano / 1000.0;
    }

    /** Ancho en puntos de un texto (se normaliza solo). */
    public static double ancho(String texto, double tamano, boolean negrita) {
        return ancho(texto, tamano, negrita, false, false);
    }

    public static double ancho(String texto, double tamano, boolean negrita, boolean cursiva, boolean serif) {
        String limpio = normalizar(texto);
        double total = 0;
        for (int i = 0; i < limpio.length(); i++) {
            total += anchoCaracter(limpio.charAt(i), tamano, negrita, cursiva, serif);
        }
        return total;
    }

    /** Recorta un texto de una sola línea con "..." si no entra en {@code anchoMax}. */
    public static String recortarConPuntos(String texto, double anchoMax, double tamano, boolean negrita) {
        return recortarConPuntos(texto, anchoMax, tamano, negrita, false);
    }

    public static String recortarConPuntos(String texto, double anchoMax, double tamano, boolean negrita, boolean serif) {
        String limpio = normalizar(texto).replace('\n', ' ');
        if (ancho(limpio, tamano, negrita, false, serif) <= anchoMax) {
            return limpio;
        }
        String puntos = "...";
        double anchoPuntos = ancho(puntos, tamano, negrita, false, serif);
        StringBuilder sb = new StringBuilder();
        double total = 0;
        for (int i = 0; i < limpio.length(); i++) {
            double a = anchoCaracter(limpio.charAt(i), tamano, negrita, false, serif);
            if (total + a + anchoPuntos > anchoMax) {
                break;
            }
            sb.append(limpio.charAt(i));
            total += a;
        }
        return sb.toString().trim() + puntos;
    }

    // ------------------------------------------------------------------ corte de líneas

    /** Un pedazo de texto con su color (RGB, o null = color por defecto de la celda). */
    public static final class Pieza {
        public final String texto;
        public final Integer color;

        public Pieza(String texto, Integer color) {
            this.texto = texto;
            this.color = color;
        }
    }

    /**
     * Corta una lista de piezas de color en renglones que entren en {@code anchoMax}, cortando
     * por los espacios (y, si una sola palabra no entra entera, por letra). Respeta los saltos de
     * línea escritos a mano. Cada pieza conserva su color en el renglón donde quede -- así una
     * referencia larga y coloreada nunca se corta con "..." ni pierde sus colores.
     */
    public static List<List<Pieza>> envolver(List<Pieza> piezas, double anchoMax, double tamano, boolean negrita) {
        return envolver(piezas, anchoMax, tamano, negrita, false, false);
    }

    public static List<List<Pieza>> envolver(List<Pieza> piezas, double anchoMax, double tamano, boolean negrita,
            boolean cursiva, boolean serif) {
        // Se pasa a una lista plana de (caracter, color) para poder cortar en cualquier punto.
        StringBuilder chars = new StringBuilder();
        List<Integer> colores = new ArrayList<>();
        for (Pieza p : piezas) {
            String limpio = normalizar(p.texto);
            for (int i = 0; i < limpio.length(); i++) {
                chars.append(limpio.charAt(i));
                colores.add(p.color);
            }
        }

        List<List<Pieza>> renglones = new ArrayList<>();
        int inicio = 0;
        int n = chars.length();
        while (inicio < n) {
            // Saltear espacios al principio del renglón (no los saltos de línea).
            while (inicio < n && chars.charAt(inicio) == ' ') {
                inicio++;
            }
            if (inicio >= n) {
                break;
            }
            double total = 0;
            int ultimoEspacio = -1;
            int i = inicio;
            int fin = n;
            int siguiente = n;
            while (i < n) {
                char c = chars.charAt(i);
                if (c == '\n') {
                    fin = i;
                    siguiente = i + 1;
                    break;
                }
                double a = anchoCaracter(c, tamano, negrita, cursiva, serif);
                if (total + a > anchoMax && i > inicio) {
                    if (c == ' ') {
                        fin = i;
                        siguiente = i + 1;
                    } else if (ultimoEspacio > inicio) {
                        fin = ultimoEspacio;
                        siguiente = ultimoEspacio + 1;
                    } else {
                        fin = i;
                        siguiente = i;
                    }
                    break;
                }
                if (c == ' ') {
                    ultimoEspacio = i;
                }
                total += a;
                i++;
            }
            if (i >= n) {
                fin = n;
                siguiente = n;
            }
            renglones.add(agrupar(chars, colores, inicio, fin));
            inicio = siguiente;
        }
        if (renglones.isEmpty()) {
            List<Pieza> vacio = new ArrayList<>();
            renglones.add(vacio);
        }
        return renglones;
    }

    /** Versión simple de {@link #envolver} para un texto de un solo color. */
    public static List<String> envolverTexto(String texto, double anchoMax, double tamano, boolean negrita) {
        return envolverTexto(texto, anchoMax, tamano, negrita, false, false);
    }

    public static List<String> envolverTexto(String texto, double anchoMax, double tamano, boolean negrita,
            boolean cursiva, boolean serif) {
        List<Pieza> piezas = new ArrayList<Pieza>();
        piezas.add(new Pieza(texto == null ? "" : texto, null));
        List<String> resultado = new ArrayList<String>();
        for (List<Pieza> renglon : envolver(piezas, anchoMax, tamano, negrita, cursiva, serif)) {
            StringBuilder sb = new StringBuilder();
            for (Pieza p : renglon) {
                sb.append(p.texto);
            }
            resultado.add(sb.toString());
        }
        return resultado;
    }

    private static List<Pieza> agrupar(CharSequence chars, List<Integer> colores, int desde, int hasta) {
        // Sacar espacios del final.
        while (hasta > desde && chars.charAt(hasta - 1) == ' ') {
            hasta--;
        }
        List<Pieza> renglon = new ArrayList<>();
        int i = desde;
        while (i < hasta) {
            Integer color = colores.get(i);
            int j = i;
            while (j < hasta && iguales(colores.get(j), color)) {
                j++;
            }
            renglon.add(new Pieza(chars.subSequence(i, j).toString(), color));
            i = j;
        }
        return renglon;
    }

    private static boolean iguales(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }
}
