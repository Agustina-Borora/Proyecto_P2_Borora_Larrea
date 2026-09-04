package interfaz;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 * Componente de botón personalizado Swing con bordes redondeados y 
 * cambios de color reactivos al paso y clic del mouse.
 */
public class JButtonRedondeado extends JButton {

    // Definición de paleta de colores para los diferentes estados del botón
    private Color colorNormal = new Color(36, 112, 75);      // Verde base
    private Color colorHover = new Color(45, 135, 90);       // Verde al pasar el mouse
    private Color colorPresionado = new Color(28, 90, 60);   // Verde al hacer clic
    private Color colorActual;                              // Estado de color activo

    /**
     * Constructor por defecto. Configura los estilos base del botón y 
     * asigna los escuchadores de eventos para la interacción visual.
     */
    public JButtonRedondeado() {
        colorActual = colorNormal;
        
        // Estilización inicial del componente Swing
        setText("INICIAR SESIÓN");
        setOpaque(false);                  // Vuelve transparente el fondo rectangular nativo
        setContentAreaFilled(false);       // Desactiva el pintado por defecto del contenido
        setFocusPainted(false);            // Quita el recuadro de enfoque al hacer clic
        setBorderPainted(false);           // Desactiva el borde cuadrado por defecto
        setForeground(Color.WHITE);        // Texto en color blanco
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cambia el cursor al puntero de mano

        // Escuchador de eventos del mouse para la interactividad visual
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                colorActual = colorHover;
                repaint(); // Redibuja el botón con el color Hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                colorActual = colorNormal;
                repaint(); // Vuelve al color base
            }

            @Override
            public void mousePressed(MouseEvent e) {
                colorActual = colorPresionado;
                repaint(); // Cambia al color de clic activo
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Determina si al soltar el clic el cursor sigue dentro del botón
                colorActual = mouseContains(e.getPoint()) ? colorHover : colorNormal;
                repaint();
            }
        });
    }

    /**
     * Comprueba si un punto (coordenadas del mouse) está dentro de los límites del botón.
     * 
     * @param p Coordenadas actuales del cursor.
     * @return true si el punto está dentro del área del botón; false si no.
     */
    private boolean mouseContains(Point p) {
        return getBounds().contains(p);
    }

    /**
     * Sobrescribe el método de renderizado para dibujar la forma redondeada
     * personalizada y aplicar suavizado de bordes (Antialiasing).
     * 
     * @param g Objeto Graphics del sistema.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Activa el suavizado de bordes (antialiasing) para evitar líneas pixeleadas
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibuja el fondo del botón con esquinas redondeadas (radio de 18px)
        g2.setColor(colorActual);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

        g2.dispose(); // Libera los recursos de gráficos secundarios
        
        // Dibuja el texto del botón por encima del fondo personalizado
        super.paintComponent(g);
    }
}