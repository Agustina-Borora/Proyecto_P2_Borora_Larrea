package interfaz;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JPanel;

/**
 * Panel contenedor personalizado que dibuja un fondo dividido diagonalmente por una curva de
 * Bézier fluida, combinando blanco y verde oscuro.
 */
public class PanelCurvo extends JPanel {

    /**
     * Constructor por defecto.
     */
    public PanelCurvo() {
        setOpaque(true);
    }

    /**
     * Sobrescribe el proceso de pintado del componente para renderizar los fondos de color y la
     * curva vectorial personalizada.
     *
     * @param g Contexto gráfico proporcionado por el entorno de ejecución Swing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();  // Ancho dinámico del panel
        int h = getHeight(); // Alto dinámico del panel

        // 1. Dibuja el fondo base en blanco
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);

        // 2. Define el color verde oscuro corporativo
        g2d.setColor(new Color(15, 38, 28));

        // Construcción de la figura geométrica curva personalizada
        Path2D.Double path = new Path2D.Double();
        
        path.moveTo(60, 0); 
        
        
        path.curveTo(-60, h * 0.30, 130, h * 0.70, 20, h);
        
        path.lineTo(w, h); // Línea hasta la esquina inferior derecha
        path.lineTo(w, 0); // Línea hasta la esquina superior derecha
        path.closePath();  // Cierra el trazado volviendo automáticamente a (60, 0)

        g2d.fill(path);
    }
}