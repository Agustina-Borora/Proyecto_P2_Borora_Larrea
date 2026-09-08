package vistas.acciones;

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
 * Botón redondo y "plano" (sin relleno propio) usado dentro de {@link PanelAction} para los
 * íconos de ver/editar/eliminar de cada fila.
 */
public class ActionButton extends JButton {

    /**
     * Indica si el botón está siendo presionado, para oscurecer el círculo de fondo mientras dura
     * el clic.
     */
    private boolean mousePress;

    /**
     * Indica si el mouse está sobre el botón: el círculo de fondo solo se dibuja en hover/press,
     * para que el ícono se vea plano (sin fondo) el resto del tiempo.
     */
    private boolean mouseOver;

    /**
     * Quita el relleno/borde estándar de {@link JButton} y registra un listener de mouse que
     * actualiza {@link #mousePress} y {@link #mouseOver}, usados luego por
     * {@link #paintComponent(Graphics)} para decidir si dibuja el círculo y de qué color.
     */
    public ActionButton() {
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(3, 3, 3, 3));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                mousePress = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mousePress = false;
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent me) {
                mouseOver = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent me) {
                mouseOver = false;
                repaint();
            }
        });
    }

    /**
     * Dibuja, antes del ícono del botón, un círculo de fondo centrado que ocupa el lado menor del
     * componente -- solo mientras el mouse está encima o presionado; el resto del tiempo el
     * ícono queda plano, sin círculo.
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        if (mousePress || mouseOver) {
            Graphics2D g2 = (Graphics2D) grphcs.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;
            g2.setColor(mousePress ? new Color(167, 243, 208) : new Color(209, 250, 229)); // verde menta pastel
            g2.fill(new Ellipse2D.Double(x, y, size, size));
            g2.dispose();
        }
        super.paintComponent(grphcs);
    }
}
