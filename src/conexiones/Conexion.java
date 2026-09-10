package conexiones; 

import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.SQLException;

/**
 * Clase utilitaria encargada de abrir la conexión JDBC contra la base de datos MySQL
 * "Lab_LB" (localhost:3306).
 */
public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/Lab_LB";
    private static final String USER = "root"; 
    private static final String PASSWORD = ""; 

  /**
   * Registra el driver JDBC de MySQL (probando primero el moderno {@code
   * com.mysql.cj.jdbc.Driver} y, si no está disponible, el driver legado {@code
   * com.mysql.jdbc.Driver}) y abre una conexión a la base de datos "Lab_LB" usando los
   * parámetros definidos en {@link #URL}, {@link #USER} y {@link #PASSWORD}.
   *
   * @return la {@link Connection} abierta, o {@code null} si no se
   */
  public static Connection conectar() {
        
        try { 
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                Class.forName("com.mysql.jdbc.Driver");
            }

            // 2. AGREGAMOS PARAMETROS EXTRA A LA URL PARA EVITAR RECHAZOS DE SEGURIDAD
            String urlConParametros = URL + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            
            // 3. PEDIMOS LA CONEXION
            return DriverManager.getConnection(urlConParametros, USER, PASSWORD); 
            
        } catch (ClassNotFoundException e) {
            System.out.println("Error: No se encontró el conector JAR de MySQL: " + e.getMessage());
            return null;
        } catch (SQLException e) { 
            System.out.println("Error de conexión SQL: " + e.getMessage()); 
            return null; 
        }
    }
} 
