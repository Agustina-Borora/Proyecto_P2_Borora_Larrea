package dao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import utilidades.CsvUtil;

/**
 * DAO para el respaldo manual de datos (Mes 6: Herramientas extra, pestaña Configuración &gt;
 * Sistema): vuelca las tablas principales del laboratorio a archivos .csv, uno por tabla, en la
 * carpeta que elija el usuario.
 *
 * <p>No es un backup binario de MySQL (eso requeriría {@code mysqldump} instalado y accesible
 * desde la app, algo que no se puede asumir en la máquina de la clienta) -- es una exportación
 * completa de los datos en un formato que se abre con Excel o se puede volver a cargar a mano si
 * hiciera falta. A propósito NO incluye la tabla {@code usuarios} (tiene contraseñas, aunque
 * sea su hash: no tiene sentido que termine en un .csv suelto).</p>
 */
public final class RespaldoDAO {

    private RespaldoDAO() {
    }

    private static final String[] TABLAS = {
        "pacientes", "medicos", "obras_sociales", "pedidos", "pedido_analisis",
        "analisis_tipos", "pagos"
    };

    /**
     * Exporta cada tabla de {@link #TABLAS} a "carpeta/tabla.csv". Si alguna tabla falla (por
     * ejemplo si no existe con ese nombre en esta base), se avisa y se sigue con las demás en vez
     * de cortar todo el respaldo por una sola tabla.
     *
     * @return la cantidad de tablas exportadas con éxito.
     */
    public static int exportarTodo(Connection conexion, File carpeta) {
        int exportadas = 0;
        for (String tabla : TABLAS) {
            try (Statement st = conexion.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                CsvUtil.escribirDesdeResultSet(new File(carpeta, tabla + ".csv"), rs);
                exportadas++;
            } catch (SQLException | IOException e) {
                Mensajes.error("No se pudo exportar la tabla " + tabla, e);
            }
        }
        return exportadas;
    }
}
