package modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo lo que necesita la pantalla Estadísticas para un período elegido (mes/trimestre/año): las
 * 4 tarjetas de resumen más los datos de los 4 paneles de abajo. Se arma en una sola pasada desde
 * {@link dao.EstadisticasDAO#obtener} para no repetir consultas parecidas.
 */
public class EstadisticasPeriodo {

    private int analisisRealizados;
    private int pacientesUnicos;
    private BigDecimal ingresoTotal = BigDecimal.ZERO;
    private BigDecimal porCobrarOs = BigDecimal.ZERO;

    /** Últimos 6 meses, Particular vs Obra Social -- no cambia con el período elegido, ver el DAO. */
    private final List<PuntoMensual> porMes = new ArrayList<>();

    private final List<ConteoExamen> examenesMasSolicitados = new ArrayList<>();
    private final List<ResumenObraSocial> ordenesPorObraSocial = new ArrayList<>();
    private final List<PacienteFrecuente> pacientesFrecuentes = new ArrayList<>();

    public int getAnalisisRealizados() {
        return analisisRealizados;
    }

    public void setAnalisisRealizados(int analisisRealizados) {
        this.analisisRealizados = analisisRealizados;
    }

    public int getPacientesUnicos() {
        return pacientesUnicos;
    }

    public void setPacientesUnicos(int pacientesUnicos) {
        this.pacientesUnicos = pacientesUnicos;
    }

    public BigDecimal getIngresoTotal() {
        return ingresoTotal;
    }

    public void setIngresoTotal(BigDecimal ingresoTotal) {
        this.ingresoTotal = ingresoTotal;
    }

    public BigDecimal getPorCobrarOs() {
        return porCobrarOs;
    }

    public void setPorCobrarOs(BigDecimal porCobrarOs) {
        this.porCobrarOs = porCobrarOs;
    }

    public List<PuntoMensual> getPorMes() {
        return porMes;
    }

    public List<ConteoExamen> getExamenesMasSolicitados() {
        return examenesMasSolicitados;
    }

    public List<ResumenObraSocial> getOrdenesPorObraSocial() {
        return ordenesPorObraSocial;
    }

    public List<PacienteFrecuente> getPacientesFrecuentes() {
        return pacientesFrecuentes;
    }
}
