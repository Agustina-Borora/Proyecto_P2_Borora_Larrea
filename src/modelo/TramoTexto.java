package modelo;

/**
 * Un tramo de texto con un color opcional -- por ejemplo, para separar dentro de un mismo valor
 * de referencia la parte que aplica a mujeres de la que aplica a hombres, cada una en un color
 * distinto ("M:11-14" en azul, "H:12-15" en verde). Se arma y edita con
 * {@link vistas.javafx.comunes.EditorTramosDialog}, y varios tramos seguidos forman el texto
 * completo de un campo (ver {@link utilidades.TramosTextoUtil}).
 */
public class TramoTexto {

    private String texto;

    /**
     * Color en hexadecimal sin "#" (ej. "2563EB"), o null/vacío para el color por defecto
     * (negro, el mismo con el que se imprime todo el resto del informe).
     */
    private String colorHex;

    public TramoTexto() {
    }

    public TramoTexto(String texto, String colorHex) {
        this.texto = texto;
        this.colorHex = colorHex;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
