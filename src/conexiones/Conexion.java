package conexiones; 

//Connection para poder gestionar la conexión a la base de datos
import java.sql.Connection; 
//DriverManager, que se encarga de conectar la app con el driver de la BD
import java.sql.DriverManager; 
//SQLException para manejar los errores específicos de bases de datos
import java.sql.SQLException;

/**
 * Clase utilitaria encargada de abrir la conexión JDBC contra la base de
 * datos MySQL "laboratorio" (localhost:3306). Concentra en un único lugar
 * la URL, el usuario y la contraseña de acceso, así como el registro del
 * driver JDBC, para que el resto del proyecto (DAOs, controladores) solo
 * necesite llamar a {@link #conectar()} sin repetir esta lógica.
 */
public class Conexion {

    // Ruta de conexión (URL): indica que usa MySQL, en el servidor local (localhost), puerto 3306 y la base de datos "laboratorio"
    private static final String URL = "jdbc:mysql://localhost:3306/laboratorio"; 
    // Define el usuario administrador por defecto de MySQL (root)
    private static final String USER = "root"; 
    // Define la contraseña del usuario (en este caso está vacía)
    private static final String PASSWORD = ""; 

  /**
   * Registra el driver JDBC de MySQL (probando primero el moderno
   * {@code com.mysql.cj.jdbc.Driver} y, si no está disponible, el driver
   * legado {@code com.mysql.jdbc.Driver}) y abre una conexión a la base de
   * datos "laboratorio" usando los parámetros definidos en {@link #URL},
   * {@link #USER} y {@link #PASSWORD}.
   *
   * @return la {@link Connection} abierta, o {@code null} si no se
   *         encontró el driver o falló la conexión (el error se informa
   *         por consola en ambos casos).
   */
  public static Connection conectar() {
        
        try { 
            // 1. REGISTRAMOS EL DRIVER EN MEMORIA EXPLICITAMENTE
            // Intentamos con el driver moderno primero
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                // Si falla el moderno, intentamos con el driver viejo por las dudas
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
