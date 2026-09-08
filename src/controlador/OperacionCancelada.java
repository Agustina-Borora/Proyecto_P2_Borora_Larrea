package controlador;

/**
 * Señal interna para abortar una operación dentro de {@link ConexionUtil#ejecutar} / {@link
 * ConexionUtil#ejecutarTransaccion} cuando el DAO que falló ya mostró su propio cartel de
 * error (todos los DAO del sistema muestran el suyo antes de devolver null/false).
 */
public class OperacionCancelada extends RuntimeException {
}
