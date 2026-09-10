package vistas.panels;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro para campos numéricos decimales (Unidad Bioquímica): solo deja escribir dígitos y un
 * único punto decimal -- no permite un segundo punto, ni que el punto quede como primer
 * caracter del campo.
 */
public class FiltroDecimal extends DocumentFilter {

    /**
     * Inserción de texto nuevo: delega en {@link #filtrar}.
     */
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        String aInsertar = filtrar(fb, offset, 0, string);
        if (aInsertar != null) {
            super.insertString(fb, offset, aInsertar, attr);
        }
    }

    /**
     * Reemplazo de texto (tipeo, pegado, etc.): delega en {@link #filtrar}.
     */
    @Override
    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attrs)
            throws BadLocationException {
        String aInsertar = filtrar(fb, offset, length, string);
        if (aInsertar != null) {
            super.replace(fb, offset, length, aInsertar, attrs);
        }
    }

    /**
     * Deja pasar todos los dígitos de {@code string}, y el punto solo si el campo todavía no
     * tiene uno (contando lo que ya está escrito) y no va a terminar siendo el primer caracter.
     */
    private String filtrar(FilterBypass fb, int offset, int length, String string) throws BadLocationException {
        if (string == null || string.isEmpty()) {
            return string;
        }
        String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
        String restante = actual.substring(0, offset) + actual.substring(offset + length);
        boolean tienePunto = restante.contains(".");

        StringBuilder aceptado = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (Character.isDigit(c)) {
                aceptado.append(c);
            } else if (c == '.' && !tienePunto && (offset > 0 || aceptado.length() > 0)) {
                aceptado.append(c);
                tienePunto = true;
            }
        }
        return aceptado.length() > 0 ? aceptado.toString() : null;
    }
}
