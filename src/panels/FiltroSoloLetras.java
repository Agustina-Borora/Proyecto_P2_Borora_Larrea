package panels;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro para campos de nombre de persona (Apellido y Nombre, Medico
 * Derivante): solo letras (con acentos), espacios, apóstrofe y guion --
 * para poder escribir "O'Connor" o "Pérez-García". Nada de números ni
 * otros símbolos.
 */
public class FiltroSoloLetras extends DocumentFilter {

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
     * Devuelve {@code string} quedándose solo con letras, espacio, apóstrofe
     * y guion; el resto de los caracteres se descarta.
     */
    private String filtrar(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        StringBuilder aceptado = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (Character.isLetter(c) || c == ' ' || c == '\'' || c == '-') {
                aceptado.append(c);
            }
        }
        return aceptado.length() > 0 ? aceptado.toString() : null;
    }
}
