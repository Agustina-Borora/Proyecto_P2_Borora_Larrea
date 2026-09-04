package panels;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro para texto libre corto con puntuación básica (Observación): letras,
 * números, espacios y la puntuación habitual de una nota clínica breve
 * (. , ; : ( ) -). Bloquea el resto de los símbolos (@, #, $, etc.), que no
 * tienen sentido en una observación.
 */
public class FiltroTextoObservacion extends DocumentFilter {

    private static final String PUNTUACION_PERMITIDA = ".,;:()-";

    /**
     * Inserción de texto nuevo: delega en {@link #filtrar}.
     */
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        String limpio = filtrar(string);
        if (limpio != null) {
            super.insertString(fb, offset, limpio, attr);
        }
    }

    /**
     * Reemplazo de texto (tipeo, pegado, etc.): delega en {@link #filtrar}.
     */
    @Override
    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attrs)
            throws BadLocationException {
        String limpio = filtrar(string);
        if (limpio != null) {
            super.replace(fb, offset, length, limpio, attrs);
        }
    }

    /**
     * Devuelve {@code string} quedándose con letras, dígitos, espacio y la
     * puntuación de {@link #PUNTUACION_PERMITIDA}; descarta cualquier otro
     * símbolo (arrobas, signos de moneda, etc.). No aplica límite de longitud.
     */
    private String filtrar(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        StringBuilder aceptado = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || PUNTUACION_PERMITIDA.indexOf(c) >= 0) {
                aceptado.append(c);
            }
        }
        return aceptado.length() > 0 ? aceptado.toString() : null;
    }
}
