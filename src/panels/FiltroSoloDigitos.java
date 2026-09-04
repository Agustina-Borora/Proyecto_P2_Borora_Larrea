package panels;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro para campos numéricos con tope de longitud (Celular, DNI): solo
 * deja escribir dígitos, y no deja pasar de largoMaximo caracteres en total
 * (contando lo que ya hay en el campo). Misma lógica que antes tenía el DNI
 * como clase anónima adentro de DatosPersonales; se comparte acá para poder
 * usarla también en Celular.
 */
public class FiltroSoloDigitos extends DocumentFilter {

    private final int largoMaximo;

    public FiltroSoloDigitos(int largoMaximo) {
        this.largoMaximo = largoMaximo;
    }

    /**
     * Inserción de texto nuevo: delega en {@link #reemplazar}.
     */
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        reemplazar(fb, offset, 0, string, attr);
    }

    /**
     * Reemplazo de texto (tipeo, pegado, etc.): delega en {@link #reemplazar}.
     */
    @Override
    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attrs)
            throws BadLocationException {
        reemplazar(fb, offset, length, string, attrs);
    }

    /**
     * Descarta todo lo que no sea dígito de {@code string} y, si el campo ya
     * alcanzó (o superaría) {@code largoMaximo}, recorta lo que se puede
     * insertar para no pasarse del tope; si no queda espacio disponible,
     * simplemente aplica el borrado del tramo seleccionado sin insertar nada.
     */
    private void reemplazar(FilterBypass fb, int offset, int length, String string, AttributeSet attrs)
            throws BadLocationException {
        if (string == null) {
            return;
        }
        StringBuilder soloDigitos = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (Character.isDigit(c)) {
                soloDigitos.append(c);
            }
        }
        int largoSinReemplazar = fb.getDocument().getLength() - length;
        int espacioDisponible = largoMaximo - largoSinReemplazar;
        if (espacioDisponible <= 0) {
            if (length > 0) {
                fb.replace(offset, length, "", attrs);
            }
            return;
        }
        if (soloDigitos.length() > espacioDisponible) {
            soloDigitos.setLength(espacioDisponible);
        }
        fb.replace(offset, length, soloDigitos.toString(), attrs);
    }
}
