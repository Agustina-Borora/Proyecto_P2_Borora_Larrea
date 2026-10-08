package utilidades;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import modelo.TramoTexto;

/**
 * Parsea, arma y dibuja el formato de "texto con tramos de colores" que se usa para poder marcar
 * con colores partes de un valor de referencia (por ejemplo separar "M:11-14" de "H:12-15" cada
 * una en un color distinto -- Mes 7: Configuración e Informe de Resultados con colores).
 *
 * <p>En vez de agregar una columna nueva a {@code valores_referencia} o a
 * {@code pedido_analito_resultado} para guardar el color, se guarda todo en el mismo campo de
 * texto de siempre, marcando el arranque de cada tramo coloreado con el caracter de control
 * {@code \u0001} seguido del color en hexadecimal y otro {@code \u0001}. Por ejemplo:</p>
 *
 * <pre>M:11-14\u000116A34A\u0001H:12-15</pre>
 *
 * <p>se lee como "M:11-14" en el color por defecto (negro) y "H:12-15" en verde. Un valor de
 * referencia viejo, sin ningún marcador, se sigue leyendo tal cual, como un único tramo negro --
 * no hace falta ninguna migración de datos para que esto funcione con lo que ya había cargado.
 * {@code \u0001} es un caracter de control que no se puede escribir desde el teclado, así que no
 * hay riesgo de que aparezca por accidente en un valor de referencia real.</p>
 */
public final class TramosTextoUtil {

    private static final char MARCADOR = '\u0001';

    /**
     * Paleta de colores que se ofrece en {@link vistas.javafx.comunes.EditorTramosDialog} --
     * nombre en español (lo que ve la bioquímica) -&gt; color en hexadecimal, o null para "el
     * color por defecto" (negro, sin marcador).
     */
    public static final Map<String, String> PALETA_COLORES = new LinkedHashMap<>();

    static {
        PALETA_COLORES.put("Negro (predeterminado)", null);
        PALETA_COLORES.put("Azul", "2563EB");
        PALETA_COLORES.put("Rosa", "DB2777");
        PALETA_COLORES.put("Verde", "16A34A");
        PALETA_COLORES.put("Rojo", "DC2626");
        PALETA_COLORES.put("Violeta", "7C3AED");
        PALETA_COLORES.put("Naranja", "EA580C");
    }

    private TramosTextoUtil() {
    }

    /**
     * Separa un texto crudo (tal como está guardado en la base) en su lista de tramos. Nunca
     * devuelve null -- un texto vacío o null da una lista vacía.
     */
    public static List<TramoTexto> parsear(String crudo) {
        List<TramoTexto> tramos = new ArrayList<>();
        if (crudo == null || crudo.isEmpty()) {
            return tramos;
        }

        String[] partes = crudo.split(String.valueOf(MARCADOR), -1);
        // Reparación (2026-10-08): en la base hay referencias guardadas como
        // "16A34A\u000136 - 48 %" -- o sea, con el color del primer tramo pero SIN el marcador
        // de adelante. Leído al pie de la letra, eso mostraba "16A34A" como si fuera texto y
        // perdía el color. Si el texto arranca con un color de 6 dígitos hexadecimales seguido de
        // un marcador, se lo toma como el color del primer tramo.
        if (partes.length >= 2 && partes.length % 2 == 0 && partes[0].matches("[0-9A-Fa-f]{6}")) {
            String[] corregido = new String[partes.length + 1];
            corregido[0] = "";
            System.arraycopy(partes, 0, corregido, 1, partes.length);
            partes = corregido;
        }
        if (!partes[0].isEmpty()) {
            tramos.add(new TramoTexto(partes[0], null));
        }
        for (int i = 1; i + 1 < partes.length; i += 2) {
            String color = partes[i];
            String texto = partes[i + 1];
            if (!texto.isEmpty()) {
                tramos.add(new TramoTexto(texto, color.isEmpty() ? null : color));
            }
        }
        return tramos;
    }

    /**
     * Arma el texto crudo (para guardar en la base) a partir de la lista de tramos editada.
     * Tramos con texto vacío se ignoran.
     */
    public static String serializar(List<TramoTexto> tramos) {
        StringBuilder sb = new StringBuilder();
        if (tramos == null) {
            return "";
        }
        for (TramoTexto tramo : tramos) {
            if (tramo.getTexto() == null || tramo.getTexto().isEmpty()) {
                continue;
            }
            if (tramo.getColorHex() == null || tramo.getColorHex().isEmpty()) {
                sb.append(tramo.getTexto());
            } else {
                sb.append(MARCADOR).append(tramo.getColorHex()).append(MARCADOR).append(tramo.getTexto());
            }
        }
        return sb.toString();
    }

    /**
     * Proyección de solo texto (sin colores) de un texto crudo -- lo que hay que usar en
     * cualquier lugar que compare o muestre el valor de referencia como texto plano (por ejemplo
     * el cálculo de Normal/Alto/Bajo por expresión regular, que rompería si el marcador de color
     * quedara en el medio del rango numérico).
     */
    public static String textoPlano(String crudo) {
        if (crudo == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (TramoTexto tramo : conSeparadores(parsear(crudo))) {
            sb.append(tramo.getTexto());
        }
        return sb.toString();
    }

    /**
     * Los mismos tramos, pero con un espacio entre dos tramos seguidos cuando ninguno de los dos
     * lo tiene (2026-10-08). Sin esto, "h 100-150" en azul y "m 50-100" en verde se mostraban
     * pegados ("h 100-150m 50-100") en los informes, salvo que se escribiera el espacio a mano al
     * principio del segundo tramo. Si ya hay un espacio (o un salto de línea), no agrega nada.
     */
    public static List<TramoTexto> conSeparadores(List<TramoTexto> tramos) {
        List<TramoTexto> resultado = new ArrayList<>();
        if (tramos == null) {
            return resultado;
        }
        for (TramoTexto tramo : tramos) {
            String texto = tramo.getTexto() == null ? "" : tramo.getTexto();
            if (!resultado.isEmpty() && !texto.isEmpty()) {
                String anterior = resultado.get(resultado.size() - 1).getTexto();
                boolean anteriorTerminaEnBlanco = anterior.isEmpty()
                        || Character.isWhitespace(anterior.charAt(anterior.length() - 1));
                if (!anteriorTerminaEnBlanco && !Character.isWhitespace(texto.charAt(0))) {
                    texto = " " + texto;
                }
            }
            resultado.add(new TramoTexto(texto, tramo.getColorHex()));
        }
        return resultado;
    }

    /** true si el texto crudo tiene al menos un tramo con color (no solo texto plano). */
    public static boolean tieneColor(String crudo) {
        if (crudo == null) {
            return false;
        }
        for (TramoTexto tramo : parsear(crudo)) {
            if (tramo.getColorHex() != null && !tramo.getColorHex().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static String nombrePorColor(String colorHex) {
        for (Map.Entry<String, String> entrada : PALETA_COLORES.entrySet()) {
            if (colorHex == null ? entrada.getValue() == null : colorHex.equals(entrada.getValue())) {
                return entrada.getKey();
            }
        }
        return "Negro (predeterminado)";
    }

    /**
     * Arma un {@link TextFlow} de JavaFX con los tramos coloreados, para mostrar una vista previa
     * en pantalla (Catálogo de Exámenes, Cargar Resultados, vista previa del informe).
     */
    public static TextFlow aTextFlow(List<TramoTexto> tramos, String tamanoFuenteCss) {
        TextFlow flujo = new TextFlow();
        for (TramoTexto tramo : conSeparadores(tramos)) {
            Text nodo = new Text(tramo.getTexto());
            String color = tramo.getColorHex() == null || tramo.getColorHex().isEmpty() ? "141C19" : tramo.getColorHex();
            nodo.setStyle("-fx-fill: #" + color + "; -fx-font-size: " + tamanoFuenteCss + ";");
            flujo.getChildren().add(nodo);
        }
        return flujo;
    }

    /**
     * Dibuja los tramos coloreados sobre un {@link Graphics2D} (para los reportes impresos /
     * PDF), uno al lado del otro empezando en {@code x}, y devuelve el color al negro al
     * terminar. Devuelve la posición X final (por si hiciera falta seguir dibujando algo más en
     * la misma línea).
     */
    public static int dibujar(Graphics2D g2, List<TramoTexto> tramos, int x, int y) {
        int cursorX = x;
        for (TramoTexto tramo : tramos) {
            g2.setColor(colorAwt(tramo.getColorHex()));
            String texto = tramo.getTexto();
            g2.drawString(texto, cursorX, y);
            cursorX += g2.getFontMetrics().stringWidth(texto);
        }
        g2.setColor(Color.BLACK);
        return cursorX;
    }

    private static Color colorAwt(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.BLACK;
        }
        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}
