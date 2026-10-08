package utilidades;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calcula el estado (Normal/Alto/Bajo) de un valor numérico cargado contra el valor de
 * referencia de su analito -- compartido por {@code controlador.ResultadosController} (para el
 * informe/PDF) y {@code vistas.javafx.registrarResultados.CargarResultadosScreen} (para
 * mostrarlo en vivo mientras se carga), que antes tenían cada una su propia copia de este mismo
 * cálculo ("duplicado a propósito", según el comentario que tenía) -- se juntan acá para no tener
 * que arreglar el mismo bug dos veces (pasó justo con este cambio: se corrigió acá y listo).
 *
 * <p>Antes de este cambio, el valor de referencia tenía que ser EXACTAMENTE un rango o un límite
 * con símbolo ("70 - 110", "&lt; 40"), sin nada más alrededor -- pero los informes reales (los
 * que mandó Agus de otros laboratorios) casi nunca lo escriben así: usan palabras ("Hasta 1
 * mg/dl", "Deseable: Menor a 130 mg/dl") y casi siempre con la unidad pegada en el mismo texto.
 * Como el cálculo exigía que TODO el texto fuera nada más que el número, en la práctica nunca
 * encontraba nada reconocible y el estado quedaba siempre vacío -- por eso "no aparece el estado
 * como alto, normal o bajo" en el PDF real. Ahora busca el patrón en cualquier parte del texto
 * (no exige que sea todo el texto) y también reconoce esas formas con palabras, además de los
 * símbolos de siempre.</p>
 *
 * <p>Sigue siendo un cálculo "mejor esfuerzo": un valor de referencia con dos partes distintas
 * por sexo en el mismo texto (ej. "Hombres: hasta 38  Mujeres: hasta 32"), con varios tramos
 * (deseable/moderado/elevado) o que no tiene ningún patrón reconocible, sigue devolviendo null
 * -- no hay forma de adivinar cuál de los dos números aplica sin que el sistema sepa el sexo del
 * paciente en esta columna (la tabla real {@code valores_referencia} no lo distingue, ver
 * {@code dao.AnalitoDAO}), así que ahí no se muestra nada en vez de arriesgar un cálculo mal
 * hecho.</p>
 */
public final class CalculoEstadoUtil {

    private static final Pattern PATRON_RANGO =
            Pattern.compile("(-?\\d+(?:[.,]\\d+)?)\\s*-\\s*(-?\\d+(?:[.,]\\d+)?)");

    private static final Pattern PATRON_MAXIMO_SIMBOLO = Pattern.compile("[<≤]\\s*=?\\s*(-?\\d+(?:[.,]\\d+)?)");
    private static final Pattern PATRON_MINIMO_SIMBOLO = Pattern.compile("[>≥]\\s*=?\\s*(-?\\d+(?:[.,]\\d+)?)");

    /** "Hasta 1 mg/dl", "Menor a 130 mg/dl", "Menor de 40", "No mayor a 6", "Inferior a 40". */
    private static final Pattern PATRON_MAXIMO_PALABRA = Pattern.compile(
            "(?:hasta|inferior\\s+a|no\\s+mayor\\s+a|menor\\s+(?:a|que|de|o\\s+igual\\s+a))\\s*:?\\s*(-?\\d+(?:[.,]\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    /** "Mayor a 6", "Mayor de 40", "No menor a 6", "Superior a 6". */
    private static final Pattern PATRON_MINIMO_PALABRA = Pattern.compile(
            "(?:superior\\s+a|no\\s+menor\\s+a|mayor\\s+(?:a|que|de|o\\s+igual\\s+a))\\s*:?\\s*(-?\\d+(?:[.,]\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private CalculoEstadoUtil() {
    }

    /**
     * "Normal", "Alto", "Bajo", o null si el valor no es numérico o no se encontró ningún patrón
     * reconocible dentro del texto de referencia.
     */
    public static String calcular(String valorTexto, String referencia) {
        if (valorTexto == null) {
            return null;
        }
        String valorLimpio = valorTexto.trim();
        if (valorLimpio.isEmpty() || !valorLimpio.matches("-?\\d+([.,]\\d+)?")) {
            return null;
        }
        return calcular(parseDecimal(valorLimpio), referencia);
    }

    /** Igual que {@link #calcular(String, String)} pero con el valor ya parseado a {@code double}. */
    public static String calcular(double valor, String referencia) {
        if (referencia == null) {
            return null;
        }
        String ref = referencia.trim();
        if (ref.isEmpty()) {
            return null;
        }

        Matcher rango = PATRON_RANGO.matcher(ref);
        if (rango.find()) {
            double minimo = parseDecimal(rango.group(1));
            double maximo = parseDecimal(rango.group(2));
            // Si el "rango" quedó al revés (min > max) probablemente no era un rango de verdad
            // sino dos números sueltos que matchearon por casualidad -- se sigue buscando en vez
            // de devolver un resultado que no tiene sentido.
            if (minimo <= maximo) {
                if (valor < minimo) {
                    return "Bajo";
                }
                if (valor > maximo) {
                    return "Alto";
                }
                return "Normal";
            }
        }

        Matcher maximoSimbolo = PATRON_MAXIMO_SIMBOLO.matcher(ref);
        if (maximoSimbolo.find()) {
            return valor > parseDecimal(maximoSimbolo.group(1)) ? "Alto" : "Normal";
        }

        Matcher minimoSimbolo = PATRON_MINIMO_SIMBOLO.matcher(ref);
        if (minimoSimbolo.find()) {
            return valor < parseDecimal(minimoSimbolo.group(1)) ? "Bajo" : "Normal";
        }

        Matcher maximoPalabra = PATRON_MAXIMO_PALABRA.matcher(ref);
        if (maximoPalabra.find()) {
            return valor > parseDecimal(maximoPalabra.group(1)) ? "Alto" : "Normal";
        }

        Matcher minimoPalabra = PATRON_MINIMO_PALABRA.matcher(ref);
        if (minimoPalabra.find()) {
            return valor < parseDecimal(minimoPalabra.group(1)) ? "Bajo" : "Normal";
        }

        return null;
    }

    private static double parseDecimal(String texto) {
        return Double.parseDouble(texto.replace(',', '.'));
    }
}
