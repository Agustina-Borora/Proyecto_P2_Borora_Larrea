package modelo;

/**
 * Modelo (POJO) para una fila de `analitos` (un parámetro dentro de un examen, ej. "Hemoglobina"
 * dentro de "Hemograma completo"), pensado para el ABM del Catálogo de Exámenes -- no confundir
 * con {@link Parametro}, que trae el valor de referencia ya resuelto para un paciente puntual.
 * Acá {@code valorReferencia} es el valor de referencia general (id_sexo = 3, "aplica a
 * cualquier sexo"); separar por sexo queda para una vuelta futura de esta pantalla.
 */
public class Analito {

    private int idAnalito;
    private int idAnalisisTipo;
    private String nombreAnalito;
    private String tipoDato; // "numerico" | "cualitativo" | "texto"
    private String unidad;
    private int ordenAnalito;
    private String valorReferencia;

    public int getIdAnalito() {
        return idAnalito;
    }

    public void setIdAnalito(int idAnalito) {
        this.idAnalito = idAnalito;
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    public String getNombreAnalito() {
        return nombreAnalito;
    }

    public void setNombreAnalito(String nombreAnalito) {
        this.nombreAnalito = nombreAnalito;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public int getOrdenAnalito() {
        return ordenAnalito;
    }

    public void setOrdenAnalito(int ordenAnalito) {
        this.ordenAnalito = ordenAnalito;
    }

    public String getValorReferencia() {
        return valorReferencia;
    }

    public void setValorReferencia(String valorReferencia) {
        this.valorReferencia = valorReferencia;
    }
}
