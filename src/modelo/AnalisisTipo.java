package modelo;

/**
 * Modelo (POJO) para un tipo de análisis (tabla `analisis_tipos`), usado para listar los
 * exámenes disponibles -- por ejemplo, en la tabla de permisos por examen de Nuevo Usuario.
 */
public class AnalisisTipo {

    private int idAnalisisTipo;
    private String nombreAnalisis;

    public AnalisisTipo() {
    }

    public AnalisisTipo(int idAnalisisTipo, String nombreAnalisis) {
        this.idAnalisisTipo = idAnalisisTipo;
        this.nombreAnalisis = nombreAnalisis;
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    public void setIdAnalisisTipo(int idAnalisisTipo) {
        this.idAnalisisTipo = idAnalisisTipo;
    }

    public String getNombreAnalisis() {
        return nombreAnalisis;
    }

    public void setNombreAnalisis(String nombreAnalisis) {
        this.nombreAnalisis = nombreAnalisis;
    }

    @Override
    public String toString() {
        return nombreAnalisis;
    }
}
