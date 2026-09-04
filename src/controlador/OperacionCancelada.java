package controlador;

/**
 * Señal interna para abortar una operación dentro de
 * {@link ConexionUtil#ejecutar} / {@link ConexionUtil#ejecutarTransaccion}
 * cuando el DAO que falló ya mostró su propio cartel de error (todos los
 * DAO del sistema muestran el suyo antes de devolver null/false). No es un
 * error "nuevo": es solo la forma de decir "cortá acá y hacé rollback",
 * sin que ConexionUtil muestre un segundo cartel encima del que ya se vio.
 */
public class OperacionCancelada extends RuntimeException {
}
