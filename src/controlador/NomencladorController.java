package controlador;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import modelo.Prestacion;

/**
 * Controlador para el buscador de Solicitud de Análisis
 * (nuevoAnalisis.SolicitudAnalisis): busca en el nomenclador por código
 * exacto (si el texto tipeado es numérico) o por nombre (si es texto).
 */
public final class NomencladorController {

    private NomencladorController() {
    }

    public static List<Prestacion> buscar(Component padre, String texto) {
        return ConexionUtil.ejecutar(padre, "Error al buscar en el nomenclador", con -> {
            List<Prestacion> encontrados = new ArrayList<>();
            if (Character.isDigit(texto.charAt(0))) {
                try {
                    int codigo = Integer.parseInt(texto);
                    Prestacion porCodigo = dao.NomencladorDAO.buscarPorCodigo(con, codigo);
                    if (porCodigo != null) {
                        encontrados.add(porCodigo);
                    }
                } catch (NumberFormatException ex) {
                    // codigo demasiado largo para un int: no hay match posible, encontrados queda vacio
                }
            } else {
                encontrados.addAll(dao.NomencladorDAO.buscarPorNombre(con, texto));
            }
            return encontrados;
        }, Collections.emptyList());
    }
}
