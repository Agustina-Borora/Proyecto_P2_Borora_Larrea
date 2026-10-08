package modelo;

/**
 * Una fila de "Exámenes más solicitados" de Estadísticas: cuántas veces se pidió ese examen en
 * el período elegido.
 */
public class ConteoExamen {

    private String nombreExamen;
    private int cantidad;

    public String getNombreExamen() {
        return nombreExamen;
    }

    public void setNombreExamen(String nombreExamen) {
        this.nombreExamen = nombreExamen;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
