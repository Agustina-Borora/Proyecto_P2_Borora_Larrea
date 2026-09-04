package acciones;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Botón redondo y "plano" (sin relleno propio) usado dentro de {@link PanelAction}
 * para los íconos de ver/editar/eliminar de cada fila. Dibuja un círculo gris
 * claro detrás del ícono al pasar/apretar el mouse, en vez del rectángulo
 * típico de un JButton.
 */
public class ActionButton extends JButton {

    /** Indica si el botón está siendo presionado, para oscurecer el círculo de fondo mientras dura el clic. */
    private boolean mousePress;

    /**
     * Quita el relleno/borde estándar de {@link JButton} y registra un
     * listener de mouse que solo actualiza {@link #mousePress}, usado luego
     * por {@link #paintComponent(Graphics)} para elegir el color del círculo.
     */
    public ActionButton() {
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(3, 3, 3, 3));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                mousePress = true;
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mousePress = false;
            }
        });
    }

    /**
     * Dibuja, antes del ícono del botón, un círculo gris centrado que ocupa
     * el lado menor del componente. El círculo se pinta más oscuro mientras
     * el botón está presionado ({@link #mousePress}), simulando el efecto
     * "pressed" sin usar el relleno rectangular por defecto de JButton.
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        if (mousePress) {
            g2.setColor(new Color(158, 158, 158));
        } else {
            g2.setColor(new Color(199, 199, 199));
        }
        g2.fill(new Ellipse2D.Double(x, y, size, size));
        g2.dispose();
        super.paintComponent(grphcs);
    }
}
