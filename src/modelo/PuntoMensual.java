package modelo;

/**
 * Un punto del gráfico de barras "Análisis por mes" de Estadísticas: cuántos análisis de ese mes
 * fueron de cobertura Particular y cuántos de Obra Social (Mixto se cuenta como Obra Social, para
 * no agregar una tercera serie al gráfico por un caso que en la práctica es poco frecuente).
 */
public class PuntoMensual {

    private String mes;
    private int particular;
    private int obraSocial;

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public int getParticular() {
        return particular;
    }

    public void setParticular(int particular) {
        this.particular = particular;
    }

    public int getObraSocial() {
        return obraSocial;
    }

    public void setObraSocial(int obraSocial) {
        this.obraSocial = obraSocial;
    }
}
