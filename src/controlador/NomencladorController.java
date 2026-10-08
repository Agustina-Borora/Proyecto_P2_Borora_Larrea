package controlador;

import java.awt.Component;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelo.Prestacion;

/**
 * Controlador para el buscador de Solicitud de Análisis (vistas.javafx.nuevoAnalisis.
 * SolicitudAnalisisPanel): busca por código exacto (si el texto tipeado es numérico) o por
 * nombre (si es texto).
 *
 * <p>Busca primero en el catálogo de exámenes ({@code analisis_tipos}, sólo los activos) -- ahí
 * es donde están el nombre que Agus le puso a cada examen y, si corresponde, los parámetros ya
 * configurados para cargar resultados, y es lo único que respeta "dar de baja" un examen. Sólo
 * si nada del catálogo coincide (porque ese código nunca se usó todavía, o el único que había
 * está dado de baja) se busca en {@code prestaciones} (el nomenclador crudo importado del Excel),
 * para poder dar de alta un examen la primera vez que se pide -- exactamente igual que antes,
 * cuando esto buscaba sólo ahí.</p>
 */
public final class NomencladorController {

    private NomencladorController() {
    }

    public static List<Prestacion> buscar(Component padre, String texto) {
        return ConexionUtil.ejecutar(padre, "Error al buscar el análisis", con -> {
            if (Character.isDigit(texto.charAt(0))) {
                return buscarPorCodigo(con, texto);
            }
            return buscarPorNombre(con, texto);
        }, Collections.emptyList());
    }

    private static List<Prestacion> buscarPorCodigo(Connection con, String texto) {
        List<Prestacion> encontrados = new ArrayList<>();
        try {
            int codigo = Integer.parseInt(texto);
            encontrados.addAll(dao.AnalisisTipoDAO.buscarActivosPorCodigo(con, codigo));
            if (encontrados.isEmpty()) {
                Prestacion porCodigo = dao.NomencladorDAO.buscarPorCodigo(con, codigo);
                if (porCodigo != null) {
                    encontrados.add(porCodigo);
                }
            }
        } catch (NumberFormatException ex) {
            // el texto empieza con un dígito pero no es un número entero válido (por ejemplo
            // "4-12") -- se deja la lista vacía en vez de romper la búsqueda.
        }
        return encontrados;
    }

    /**
     * Junta los del catálogo (activos, con el nombre que Agus cargó) con los del nomenclador que
     * todavía no tengan un examen activo en el catálogo para ese código -- así no se repite el
     * mismo código dos veces (una con el nombre del catálogo y otra con el del nomenclador) en la
     * lista de sugerencias.
     */
    private static List<Prestacion> buscarPorNombre(Connection con, String texto) {
        List<Prestacion> delCatalogo = dao.AnalisisTipoDAO.buscarActivosPorNombre(con, texto);

        Set<Integer> codigosDelCatalogo = new HashSet<>();
        for (Prestacion p : delCatalogo) {
            codigosDelCatalogo.add(p.getCodigo());
        }

        List<Prestacion> encontrados = new ArrayList<>(delCatalogo);
        for (Prestacion p : dao.NomencladorDAO.buscarPorNombre(con, texto)) {
            if (!codigosDelCatalogo.contains(p.getCodigo())) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }
}
