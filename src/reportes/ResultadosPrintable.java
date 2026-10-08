package reportes;

/**
 * El "Informe de Resultados" de una orden, listo para mandar a la impresora. Es solo un atajo:
 * arma el documento con {@link MaquetadorInforme} (el mismo que usan la vista previa y el PDF) y
 * lo imprime con {@link DocumentoPrintable}.
 *
 * <p>Antes esta clase dibujaba el informe a mano, de a un examen por vez, con filas de alto fijo
 * que cortaban con "..." las referencias largas. Todo eso ahora lo resuelve
 * {@link MaquetadorInforme}.</p>
 */
public class ResultadosPrintable extends DocumentoPrintable {

    public ResultadosPrintable(ResultadosDatos datos, DisenoInforme diseno) {
        super(MaquetadorInforme.armar(datos, diseno, ConfigInforme.actual(), false));
    }
}
