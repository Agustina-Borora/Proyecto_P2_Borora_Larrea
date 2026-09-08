
package prueba;


import vistas.escritorio.EscritorioDetalledeOrden;
/**
 * Ventana de prueba para ver una pantalla del paquete vistas SOLA, sin conectarla todavía al
 * resto de la app (Login, menú, etc.).
 *
 * @author agust
 */
public class VistaPreview {

    public static void main(String[] args) {
        
        try {
            com.formdev.flatlaf.FlatIntelliJLaf.setup();
        } catch (Exception ex) {
            System.err.println("Fallo al inicializar FlatLaf");
        }

        java.awt.EventQueue.invokeLater(() -> {
            javax.swing.JPanel pantalla = new EscritorioDetalledeOrden();

            javax.swing.JFrame ventana = new javax.swing.JFrame(
                    "Vista previa - " + pantalla.getClass().getSimpleName());
            ventana.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            ventana.getContentPane().add(pantalla);
            ventana.setSize(980, 780); // Pantalla de panel principal, con AbsoluteLayout ancho
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
