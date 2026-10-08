package reportes;

/**
 * El "Comprobante de Orden" listo para mandar a la impresora. Es solo un atajo: arma el
 * documento con {@link MaquetadorComprobante} (el mismo que usa el PDF del comprobante) y lo
 * imprime con {@link DocumentoPrintable}.
 */
public class ComprobanteOrdenPrintable extends DocumentoPrintable {

    public ComprobanteOrdenPrintable(ComprobanteOrdenDatos datos, DisenoInforme diseno) {
        super(MaquetadorComprobante.armar(datos, diseno, ConfigInforme.actual()));
    }
}
