package modelo;

/**
 * Resultado de crear un pedido nuevo: el id interno (para poder colgarle
 * los pedido_analisis) y el numero_pedido legible que se le arma después
 * (ver dao.PedidoDAO.crearPedido), que es lo que se muestra en el cartel de
 * "Orden generada".
 */
public class PedidoCreado {

    private final int idPedido;
    private final String numeroPedido;

    public PedidoCreado(int idPedido, String numeroPedido) {
        this.idPedido = idPedido;
        this.numeroPedido = numeroPedido;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }
}
