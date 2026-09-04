package modelo;

/**
 * POJO con los 4 números que muestran las tarjetas del Escritorio,
 * calculados sobre el mes en curso.
 */
public class EstadisticasEscritorio {

    private int totalMes;
    private int emitidas;
    private int enProceso;
    private int pendientes;

    public int getTotalMes() {
        return totalMes;
    }

    public void setTotalMes(int totalMes) {
        this.totalMes = totalMes;
    }

    public int getEmitidas() {
        return emitidas;
    }

    public void setEmitidas(int emitidas) {
        this.emitidas = emitidas;
    }

    public int getEnProceso() {
        return enProceso;
    }

    public void setEnProceso(int enProceso) {
        this.enProceso = enProceso;
    }

    public int getPendientes() {
        return pendientes;
    }

    public void setPendientes(int pendientes) {
        this.pendientes = pendientes;
    }
}
