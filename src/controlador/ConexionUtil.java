package controlador;

import conexiones.Conexion;
import java.awt.Component;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Utilidad compartida por todos los Controladores para ejecutar una operación contra la base
 * de datos sin repetir en cada pantalla el mismo bloque de "conectar, verificar null,
 * ejecutar, cerrar en finally".
 */
public final class ConexionUtil {

    private ConexionUtil() {
    }

    /**
     * Operación que necesita una Connection abierta y puede fallar con SQLException.
     */
    public interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    /**
     * Abre una conexión, ejecuta la operación y la cierra siempre (haya salido bien o mal).
     */
    public static <T> T ejecutar(Component padre, String tituloError, Operacion<T> operacion, T valorPorError) {
        Connection con = Conexion.conectar();
        if (con == null) {
            mostrarError(padre, "No se pudo conectar a la base de datos.", "Error de Conexión");
            return valorPorError;
        }
        try {
            return operacion.ejecutar(con);
        } catch (OperacionCancelada e) {
            return valorPorError;
        } catch (SQLException e) {
            mostrarError(padre, tituloError + ": " + e.getMessage(), "Error");
            return valorPorError;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Igual que {@link #ejecutar}, pero para operaciones que necesitan transacción (varios
     * INSERT/UPDATE que tienen que aplicarse todos juntos o ninguno): deja autoCommit en false,
     * hace commit si la operación termina bien, y rollback si tira cualquier excepción.
     */
    public static <T> T ejecutarTransaccion(Component padre, String tituloError, Operacion<T> operacion, T valorPorError) {
        Connection con = Conexion.conectar();
        if (con == null) {
            mostrarError(padre, "No se pudo conectar a la base de datos.", "Error de Conexión");
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
            mostrarError(padre, tituloError + ": " + e.getMessage(), "Error");
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

    /**
     * Muestra el cartel de error en el hilo de Swing. Si esta operación se está ejecutando en un
     * hilo en segundo plano, el cartel se manda al hilo de Swing en vez de abrirse ahí mismo
     * (abrir un {@code JOptionPane} desde otro hilo puede dejar la aplicación tildada).
     */
    private static void mostrarError(Component padre, String mensaje, String titulo) {
        Runnable mostrar = () -> JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            mostrar.run();
        } else {
            System.err.println(titulo + ": " + mensaje);
            javax.swing.SwingUtilities.invokeLater(mostrar);
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
