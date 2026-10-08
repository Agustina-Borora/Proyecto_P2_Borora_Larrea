package reportes;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import modelo.TramoTexto;
import utilidades.TramosTextoUtil;

/**
 * Los cambios de TEXTO que la bioquímica le hace a un informe puntual, desde la vista previa,
 * sin tener que ir al Catálogo de Exámenes: cambiar el título de un estudio, el nombre o la unidad
 * de un parámetro, la referencia y sus colores, ocultar una fila o un estudio entero, cambiar el
 * orden de los estudios, la observación de un estudio y agregar una nota al final.
 *
 * <p>Se guardan en la orden ({@code pedidos.ajustes_informe}), así que si el PDF se vuelve a
 * generar (por ejemplo porque cambió un color en el Catálogo), los cambios hechos a mano se
 * mantienen. Lo que no se tocó sigue saliendo del Catálogo.</p>
 *
 * <p>Ojo: los VALORES de los resultados no se cambian acá -- eso se hace en "Registrar
 * Resultados", para que lo que dice el informe sea siempre lo que quedó cargado.</p>
 */
public class AjustesInforme {

    private final Properties datos = new Properties();

    public AjustesInforme() {
    }

    // ------------------------------------------------------------------ claves

    private static String claveEstudio(int idPedidoAnalisis, String campo) {
        return "e." + idPedidoAnalisis + "." + campo;
    }

    private static String claveFila(int idPedidoAnalisis, int idAnalito, String campo) {
        return "f." + idPedidoAnalisis + "." + idAnalito + "." + campo;
    }

    private String leer(String clave) {
        return datos.getProperty(clave);
    }

    private void poner(String clave, String valor) {
        if (valor == null) {
            datos.remove(clave);
        } else {
            datos.setProperty(clave, valor);
        }
    }

    // ------------------------------------------------------------------ estudios

    /** Título cambiado del estudio, o null si se usa el del Catálogo. */
    public String getTituloEstudio(int idPedidoAnalisis) {
        return leer(claveEstudio(idPedidoAnalisis, "titulo"));
    }

    public void setTituloEstudio(int idPedidoAnalisis, String titulo) {
        poner(claveEstudio(idPedidoAnalisis, "titulo"), titulo);
    }

    public boolean isEstudioOculto(int idPedidoAnalisis) {
        return "1".equals(leer(claveEstudio(idPedidoAnalisis, "oculto")));
    }

    public void setEstudioOculto(int idPedidoAnalisis, boolean oculto) {
        poner(claveEstudio(idPedidoAnalisis, "oculto"), oculto ? "1" : null);
    }

    /** Observación cambiada del estudio (solo para el informe), o null si se usa la cargada en la orden. */
    public String getObservacion(int idPedidoAnalisis) {
        return leer(claveEstudio(idPedidoAnalisis, "obs"));
    }

    public void setObservacion(int idPedidoAnalisis, String observacion) {
        poner(claveEstudio(idPedidoAnalisis, "obs"), observacion);
    }

    /** Orden de los estudios (ids de pedido_analisis), o lista vacía si se usa el orden de siempre. */
    public List<Integer> getOrden() {
        List<Integer> orden = new ArrayList<>();
        String texto = leer("orden");
        if (texto == null || texto.trim().isEmpty()) {
            return orden;
        }
        for (String parte : texto.split(",")) {
            try {
                orden.add(Integer.parseInt(parte.trim()));
            } catch (NumberFormatException e) {
                // se ignora
            }
        }
        return orden;
    }

    public void setOrden(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            poner("orden", null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        poner("orden", sb.toString());
    }

    // ------------------------------------------------------------------ filas

    public String getNombreFila(int idPedidoAnalisis, int idAnalito) {
        return leer(claveFila(idPedidoAnalisis, idAnalito, "nombre"));
    }

    public void setNombreFila(int idPedidoAnalisis, int idAnalito, String nombre) {
        poner(claveFila(idPedidoAnalisis, idAnalito, "nombre"), nombre);
    }

    public String getUnidadFila(int idPedidoAnalisis, int idAnalito) {
        return leer(claveFila(idPedidoAnalisis, idAnalito, "unidad"));
    }

    public void setUnidadFila(int idPedidoAnalisis, int idAnalito, String unidad) {
        poner(claveFila(idPedidoAnalisis, idAnalito, "unidad"), unidad);
    }

    /** Referencia cambiada, en el mismo formato con colores del Catálogo (ver {@link TramosTextoUtil}), o null. */
    public String getReferenciaFila(int idPedidoAnalisis, int idAnalito) {
        return leer(claveFila(idPedidoAnalisis, idAnalito, "ref"));
    }

    public void setReferenciaFila(int idPedidoAnalisis, int idAnalito, String referenciaCruda) {
        poner(claveFila(idPedidoAnalisis, idAnalito, "ref"), referenciaCruda);
    }

    public boolean isFilaOculta(int idPedidoAnalisis, int idAnalito) {
        return "1".equals(leer(claveFila(idPedidoAnalisis, idAnalito, "oculta")));
    }

    public void setFilaOculta(int idPedidoAnalisis, int idAnalito, boolean oculta) {
        poner(claveFila(idPedidoAnalisis, idAnalito, "oculta"), oculta ? "1" : null);
    }

    /** Saca todos los cambios de una fila (vuelve a lo del Catálogo). */
    public void limpiarFila(int idPedidoAnalisis, int idAnalito) {
        for (String campo : new String[]{"nombre", "unidad", "ref", "oculta"}) {
            poner(claveFila(idPedidoAnalisis, idAnalito, campo), null);
        }
    }

    // ------------------------------------------------------------------ nota

    /** Nota libre que va al final del informe (antes de la firma), o null. */
    public String getNotaFinal() {
        return leer("nota");
    }

    public void setNotaFinal(String nota) {
        poner("nota", nota == null || nota.trim().isEmpty() ? null : nota);
    }

    public boolean estaVacio() {
        return datos.isEmpty();
    }

    // ------------------------------------------------------------------ guardar / leer

    public String aTexto() {
        if (datos.isEmpty()) {
            return null;
        }
        StringWriter w = new StringWriter();
        try {
            datos.store(w, null);
        } catch (IOException e) {
            return null;
        }
        // Se saca la línea de fecha que agrega Properties.store (empieza con "#").
        StringBuilder sb = new StringBuilder();
        for (String linea : w.toString().split("\n")) {
            if (!linea.startsWith("#")) {
                sb.append(linea).append('\n');
            }
        }
        return sb.toString();
    }

    public static AjustesInforme desdeTexto(String texto) {
        AjustesInforme a = new AjustesInforme();
        if (texto != null && !texto.trim().isEmpty()) {
            try {
                a.datos.load(new StringReader(texto));
            } catch (IOException | IllegalArgumentException e) {
                // Texto dañado: se arranca sin cambios en vez de romper el informe.
            }
        }
        return a;
    }

    public AjustesInforme copia() {
        return desdeTexto(aTexto());
    }

    // ------------------------------------------------------------------ aplicar

    /**
     * Devuelve una COPIA de los datos con los cambios aplicados (los datos originales no se tocan,
     * para poder mostrarlos en el editor como "lo que dice el Catálogo").
     */
    public ResultadosDatos aplicar(ResultadosDatos original) {
        ResultadosDatos r = new ResultadosDatos();
        r.setIdPedido(original.getIdPedido());
        r.setNumeroOrden(original.getNumeroOrden());
        r.setFecha(original.getFecha());
        r.setNombrePaciente(original.getNombrePaciente());
        r.setDni(original.getDni());
        r.setEdad(original.getEdad());
        r.setTelefono(original.getTelefono());
        r.setEmail(original.getEmail());
        r.setMedicoDerivante(original.getMedicoDerivante());
        r.setCobertura(original.getCobertura());
        r.setComprobante(original.getComprobante());
        r.getEstudiosPendientes().addAll(original.getEstudiosPendientes());
        r.setNotaFinal(getNotaFinal());

        // Orden: primero los que figuran en "orden", después el resto en su orden de siempre.
        List<ResultadosDatos.Estudio> resto = new ArrayList<>(original.getEstudios());
        List<ResultadosDatos.Estudio> ordenados = new ArrayList<>();
        for (Integer id : getOrden()) {
            for (int i = 0; i < resto.size(); i++) {
                if (resto.get(i).getIdPedidoAnalisis() == id) {
                    ordenados.add(resto.remove(i));
                    break;
                }
            }
        }
        ordenados.addAll(resto);

        for (ResultadosDatos.Estudio e : ordenados) {
            int idPa = e.getIdPedidoAnalisis();
            if (isEstudioOculto(idPa)) {
                continue;
            }
            List<ResultadosDatos.Fila> filas = new ArrayList<>();
            for (ResultadosDatos.Fila f : e.getFilas()) {
                int idAn = f.getIdAnalito();
                if (isFilaOculta(idPa, idAn)) {
                    continue;
                }
                String nombre = valorO(getNombreFila(idPa, idAn), f.getNombreParametro());
                String unidad = valorO(getUnidadFila(idPa, idAn), f.getUnidad());
                String refCruda = getReferenciaFila(idPa, idAn);
                String plano = f.getReferencia();
                List<TramoTexto> tramos = f.getTramosReferencia();
                String estado = f.getEstado();
                if (refCruda != null) {
                    plano = TramosTextoUtil.textoPlano(refCruda);
                    tramos = TramosTextoUtil.conSeparadores(TramosTextoUtil.parsear(refCruda));
                    estado = f.getValor() == null ? null : utilidades.CalculoEstadoUtil.calcular(f.getValor(), plano);
                }
                filas.add(new ResultadosDatos.Fila(idAn, nombre, f.getValor(), unidad, plano, tramos, estado));
            }
            String titulo = valorO(getTituloEstudio(idPa), e.getNombre());
            String obs = getObservacion(idPa) != null ? getObservacion(idPa) : e.getObservacion();
            r.getEstudios().add(new ResultadosDatos.Estudio(idPa, titulo, obs, filas));
        }
        return r;
    }

    private static String valorO(String cambiado, String original) {
        return cambiado != null ? cambiado : original;
    }
}
