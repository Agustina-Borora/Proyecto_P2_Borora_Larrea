package modelo;

/**
 * Modelo (POJO) para la tabla de referencia `sexos` (id_sexo, nombre_sexo).
 * Se usa para poblar el combo "Sexo" de Datos del Paciente con las opciones
 * reales de la base en lugar de los items dummy que traía el editor visual.
 */
public class Sexo {

    private int idSexo;
    private String nombreSexo;

    public Sexo() {
    }

    public Sexo(int idSexo, String nombreSexo) {
        this.idSexo = idSexo;
        this.nombreSexo = nombreSexo;
    }

    public int getIdSexo() {
        return idSexo;
    }

    public void setIdSexo(int idSexo) {
        this.idSexo = idSexo;
    }

    public String getNombreSexo() {
        return nombreSexo;
    }

    public void setNombreSexo(String nombreSexo) {
        this.nombreSexo = nombreSexo;
    }

    @Override
    public String toString() {
        return nombreSexo;
    }
}
