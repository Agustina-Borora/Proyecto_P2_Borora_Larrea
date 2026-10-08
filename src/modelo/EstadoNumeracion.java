package modelo;

/**
 * Estado actual de la numeración automática de órdenes (Figma: tarjeta "Numeración automática de
 * órdenes" en Configuración &gt; Sistema) -- el número que le tocaría a la próxima orden que se
 * genere, el mes en curso (formato AAAA-MM) y cuántas órdenes ya se generaron en ese mes. Ver
 * {@code dao.PedidoDAO#obtenerEstadoNumeracion}.
 */
public class EstadoNumeracion {

    private String proximoNumero;
    private String mesEnCurso;
    private int ordenesEsteMes;

    public String getProximoNumero() {
        return proximoNumero;
    }

    public void setProximoNumero(String proximoNumero) {
        this.proximoNumero = proximoNumero;
    }

    public String getMesEnCurso() {
        return mesEnCurso;
    }

    public void setMesEnCurso(String mesEnCurso) {
        this.mesEnCurso = mesEnCurso;
    }

    public int getOrdenesEsteMes() {
        return ordenesEsteMes;
    }

    public void setOrdenesEsteMes(int ordenesEsteMes) {
        this.ordenesEsteMes = ordenesEsteMes;
    }
}
