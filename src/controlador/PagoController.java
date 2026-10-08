package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import dao.PagoDAO.DatosPagos;
import modelo.FilaPago;

/**
 * Controlador para la pantalla Pagos.
 */
public final class PagoController {

    private PagoController() {
    }

    public static DatosPagos cargarDatosPagos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al cargar los datos de pagos",
                con -> dao.PagoDAO.cargarDatosPagos(con),
                new DatosPagos());
    }

    /**
     * Anula el pago/cobertura activo de un pedido (ver {@code dao.PagoDAO#anularPago}).
     */
    public static boolean anularPago(Component padre, int idPedido, String motivo) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al anular el pago",
                con -> dao.PagoDAO.anularPago(con, idPedido, motivo),
                false);
        return ok != null && ok;
    }

    /**
     * Pedidos con cobertura de obra social, para la pestaña "Cobros a Obras Sociales" (ver
     * {@code dao.PagoDAO#cargarCobrosObraSocial}).
     */
    public static List<FilaPago> cargarCobrosObraSocial(Component padre, boolean incluirCobrados) {
        return ConexionUtil.ejecutar(padre, "Error al cargar los cobros a obras sociales",
                con -> dao.PagoDAO.cargarCobrosObraSocial(con, incluirCobrados),
                Collections.emptyList());
    }

    /**
     * Cambia el estado de cobro a obra social de un pedido puntual.
     */
    public static boolean actualizarEstadoCobro(Component padre, int idPedido, String nuevoEstado) {
        Boolean ok = ConexionUtil.ejecutar(padre, "Error al actualizar el estado de cobro",
                con -> dao.PagoDAO.actualizarEstadoCobro(con, idPedido, nuevoEstado),
                false);
        return ok != null && ok;
    }

    /**
     * Cambia el estado de cobro de varios pedidos a la vez ("Marcar seleccionadas como
     * cobradas"). Devuelve cuántos se actualizaron realmente.
     */
    public static int actualizarEstadoCobroLote(Component padre, List<Integer> idsPedido, String nuevoEstado) {
        Integer actualizados = ConexionUtil.ejecutar(padre, "Error al actualizar los estados de cobro",
                con -> dao.PagoDAO.actualizarEstadoCobroLote(con, idsPedido, nuevoEstado),
                0);
        return actualizados != null ? actualizados : 0;
    }
}
