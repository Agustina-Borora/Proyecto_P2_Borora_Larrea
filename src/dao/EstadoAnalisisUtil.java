package dao;

/**
 * Traduce estado_analisis (+ si el pedido tiene envío/es urgente) al texto
 * que se muestra en la columna Estado. Antes este método (traducirEstado)
 * estaba duplicado, casi idéntico, dentro de EscritorioDAO y RegistroDAO;
 * ahora ambos DAO llaman a esta única versión.
 */
public final class EstadoAnalisisUtil {

    private EstadoAnalisisUtil() {
    }

    /** Variante sin marca de urgencia, usada por EscritorioDAO. */
    public static String traducir(String estadoAnalisis, int cantEnvios) {
        return traducir(estadoAnalisis, cantEnvios, false);
    }

    /**
     * Variante completa, usada por RegistroDAO: si esUrgente es true, pisa
     * cualquier otro estado y siempre devuelve "Urgente".
     */
    public static String traducir(String estadoAnalisis, int cantEnvios, boolean esUrgente) {
        if (esUrgente) {
            return "Urgente";
        }
        if (estadoAnalisis == null) {
            return "";
        }
        switch (estadoAnalisis) {
            case "pendiente":
                return "Pendiente";
            case "en_proceso":
                return "Procesando";
            case "completado":
                return cantEnvios > 0 ? "Completado · Enviado" : "Completado · Sin enviar";
            case "cancelado":
                return "Cancelado";
            default:
                return estadoAnalisis;
        }
    }
}
