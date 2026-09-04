package controlador;

import conexiones.Conexion;
import java.awt.Component;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Utilidad compartida por todos los Controladores para ejecutar una
 * operación contra la base de datos sin repetir en cada pantalla el mismo
 * bloque de "conectar, verificar null, ejecutar, cerrar en finally".
 *
 * Antes de esta clase, ese bloque estaba copiado y pegado (con variaciones
 * mínimas) en 14 lugares distintos de la Vista: Login, Escritorio,
 * TablaEscritorio, TablaPacientes, TablaRegistros, NuevoAnalisis,
 * DatosPersonales, EncabezadoDatosPaciente, SolicitudAnalisis,
 * cargarReultados y RegistrarResultados. Ahora esas pantallas no abren
 * conexiones ni conocen los DAO: le piden el dato a un Controlador (que usa
 * esta clase) y reciben el resultado ya resuelto.
 */
public final class ConexionUtil {

    private ConexionUtil() {
    }

    /** Operación que necesita una Connection abierta y puede fallar con SQLException. */
    public interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    /**
     * Abre una conexión, ejecuta la operación y la cierra siempre (haya
     * salido bien o mal).
     *
     * - Si no se pudo conectar, muestra el cartel de "Error de Conexión" y
     *   devuelve valorPorError.
     * - Si la operación tira SQLException, muestra tituloError + el mensaje
     *   de la excepción, y devuelve valorPorError.
     * - Si la operación tira OperacionCancelada (el DAO ya mostró su propio
     *   cartel de error), no muestra un segundo cartel: solo devuelve
     *   valorPorError.
     */
    public static <T> T ejecutar(Component padre, String tituloError, Operacion<T> operacion, T valorPorError) {
        Connection con = Conexion.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(padre, "No se pudo conectar a la base de datos.",
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return valorPorError;
        }
        try {
            return operacion.ejecutar(con);
        } catch (OperacionCancelada e) {
            return valorPorError;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(padre, tituloError + ": " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return valorPorError;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Igual que {@link #ejecutar}, pero para operaciones que necesitan
     * transacción (varios INSERT/UPDATE que tienen que aplicarse todos
     * juntos o ninguno): deja autoCommit en false, hace commit si la
     * operación termina bien, y rollback si tira cualquier excepción.
     */
    public static <T> T ejecutarTransaccion(Component padre, String tituloError, Operacion<T> operacion, T valorPorError) {
        Connection con = Conexion.conectar();
        if (con == null) {
            JOptionPane.showMessageDialog(padre, "No se pudo conectar a la base de datos.",
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return valorPorError;
        }
        try {
            con.setAutoCommit(false);
            T resultado = operacion.ejecutar(con);
            con.commit();
            return resultado;
        } catch (OperacionCancelada e) {
            rollback(con);
            return valorPorError;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(padre, tituloError + ": " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return valorPorError;
        } finally {
            try {
                if (!con.isClosed()) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                // nada para hacer si falla al restaurar autoCommit
            }
            cerrar(con);
        }
    }

    private static void rollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            // nada más para hacer si falla el rollback
        }
    }

    private static void cerrar(Connection con) {
        try {
            if (!con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
