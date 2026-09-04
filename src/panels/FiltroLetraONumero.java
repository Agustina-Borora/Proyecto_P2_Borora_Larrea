package panels;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro de entrada reutilizable: rechaza cualquier símbolo, y una vez que
 * el primer caracter del campo queda definido (letra o número) obliga a que
 * todo lo que se siga escribiendo sea del mismo tipo (dentro del modo
 * "letra" también se permite el espacio, para poder buscar "Apellido
 * Nombre" o un nombre de prestación con varias palabras). El modo se
 * recalcula en cada edición a partir de cómo quedaría el texto, así que
 * borrar todo y empezar de nuevo con el otro tipo funciona sin problema.
 *
 * Antes vivía como clase privada adentro de Busqueda; se separó acá porque
 * Solicitud de Análisis (el campo Codigo/Nombre) necesita la misma regla.
 */
public class FiltroLetraONumero extends DocumentFilter {

    /**
     * Inserción de texto nuevo (por ejemplo pegar con el campo vacío):
     * delega en {@link #filtrar} para quedarse solo con lo que respeta el
     * modo letra/número vigente.
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
     * Reemplazo de texto (tipeo normal, autocompletado o pegado sobre una
     * selección): delega en {@link #filtrar} para quedarse solo con lo que
     * respeta el modo letra/número vigente.
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
     * Calcula cómo quedaría el campo con el cambio propuesto para determinar
     * el modo (letras o números) a partir del primer caracter, y devuelve de
     * {@code string} solo los caracteres que respetan ese modo (el espacio
     * siempre se acepta en modo letras). Devuelve {@code null} cuando no
     * queda nada para insertar.
     */
    private String filtrar(FilterBypass fb, int offset, int length, String string) throws BadLocationException {
        if (string == null || string.isEmpty()) {
            return string;
        }

        String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
        String resultante = actual.substring(0, offset) + string + actual.substring(offset + length);

        char primero = resultante.charAt(0);
        boolean modoLetras = Character.isLetter(primero);
        boolean modoNumeros = Character.isDigit(primero);

        StringBuilder aceptado = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (modoLetras && (Character.isLetter(c) || c == ' ')) {
                aceptado.append(c);
            } else if (modoNumeros && Character.isDigit(c)) {
                aceptado.append(c);
            }
            // cualquier otro caracter (símbolos, o el tipo que no corresponde) se descarta
        }

        return aceptado.length() > 0 ? aceptado.toString() : null;
    }
}
