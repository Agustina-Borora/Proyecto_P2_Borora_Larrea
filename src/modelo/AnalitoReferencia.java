package modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POJO de solo lectura para un renglón de "analito + su(s) valor(es) de referencia", usado por
 * pantallas de vista (como el detalle de paciente). A diferencia de {@link Parametro} -que trae
 * un único valor de referencia, ya filtrado por el sexo de un paciente puntual, para las
 * pantallas donde se cargan resultados- este trae TODOS los valores de referencia cargados para
 * el analito (por ejemplo, mujer y hombre por separado) para poder mostrarlos juntos.
 */
public class AnalitoReferencia {

    private int idAnalito;
    private String nombreAnalito;
    private String unidad;
    private String tipoDato;

    /**
     * id_sexo -&gt; texto de referencia para ese sexo puntual. Nunca contiene la clave 3
     * ("aplica a cualquier sexo"), que se guarda aparte en {@link #referenciaGeneral}.
     */
    private final Map<Integer, String> referenciasPorSexo = new LinkedHashMap<>();

    /**
     * Valor de referencia único cuando el analito no varía por sexo (id_sexo = 3), o null si
     * tiene valores separados por sexo (o no tiene ninguno cargado).
     */
    private String referenciaGeneral;

    public int getIdAnalito() {
        return idAnalito;
    }

    public void setIdAnalito(int idAnalito) {
        this.idAnalito = idAnalito;
    }

    public String getNombreAnalito() {
        return nombreAnalito;
    }

    public void setNombreAnalito(String nombreAnalito) {
        this.nombreAnalito = nombreAnalito;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }

    public Map<Integer, String> getReferenciasPorSexo() {
        return referenciasPorSexo;
    }

    public String getReferenciaGeneral() {
        return referenciaGeneral;
    }

    public void setReferenciaGeneral(String referenciaGeneral) {
        this.referenciaGeneral = referenciaGeneral;
    }

    /**
     * Valor de referencia aplicable a un paciente de sexo puntual: el de ese sexo si existe, si
     * no el general, si no null (analito sin ningún valor de referencia cargado).
     */
    public String referenciaParaSexo(int idSexoPaciente) {
        String propia = referenciasPorSexo.get(idSexoPaciente);
        return propia != null ? propia : referenciaGeneral;
    }
}
