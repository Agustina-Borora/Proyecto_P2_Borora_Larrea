package menu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Panel que representa gráficamente cada fila dentro de la lista del menú.
 */
public class MenuItem extends javax.swing.JPanel {

    // Paleta de colores institucional / diseño "San Gregorio"
    private static final Color GREEN_DARK = new Color(0x27, 0x67, 0x49);   // Texto e ícono seleccionado
    private static final Color GREEN_BG = new Color(0xE8, 0xF5, 0xE9);     // Fondo del elemento seleccionado
    private static final Color GREEN_TITLE = new Color(0x8F, 0xAE, 0x9C);  // Color para separadores/títulos de sección
    private static final Color TEXT_NORMAL = new Color(0x2B, 0x2B, 0x2B);  // Texto e ícono en estado normal
    private static final Color ICON_NORMAL = new Color(0x6B, 0x7C, 0x74);  // Color predeterminado de ícono
    private static final Color HOVER_BG = new Color(0x00, 0x00, 0x00, 12); // Fondo semitransparente al pasar el mouse
    private static final Color RED = new Color(0xE5, 0x39, 0x35);          // Color especial para "Cerrar Sesion"

    private boolean selected;
    private boolean over;
    private boolean logout;
    private boolean isMenu;
    private Model_Menu data;

    public MenuItem(Model_Menu data) {
        initComponents();
        setOpaque(false); // Fondo transparente para aplicar curvas personalizadas mediante Java2D
        this.data = data;
        logout = "Cerrar Sesion".equalsIgnoreCase(data.getName().trim());

        // Configuración estética según el tipo de menú
        if (data.getType() == Model_Menu.MenuType.MENU) {
            isMenu = true;
            lbName.setText(data.getName());
            lbName.setFont(new Font("sansserif", Font.BOLD, 13));

            if (logout) {
                lbIcon.setIcon(data.toIcon(RED));
                lbName.setForeground(RED);
            } else {
                lbIcon.setIcon(data.toIcon(ICON_NORMAL));
                lbName.setForeground(TEXT_NORMAL);
            }
        } else if (data.getType() == Model_Menu.MenuType.TITLE) {
            lbIcon.setText(data.getName().toUpperCase());
            lbIcon.setFont(new Font("sansserif", Font.BOLD, 11));
            lbIcon.setForeground(GREEN_TITLE);
            lbName.setVisible(false); // Oculta el label de nombre en títulos de sección
        } else {
            lbName.setText(" "); // Espaciador opcional
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        applyState();
        repaint();
    }

    public void setOver(boolean over) {
        this.over = over;
        applyState();
        repaint();
    }

    /**
     * Aplica los cambios de color de texto e ícono de acuerdo con el estado actual del ítem.
     */
    private void applyState() {
        if (!isMenu || logout) {
            return; // No altera el color de "Cerrar Sesión" ni de títulos o separadores
        }
        if (selected) {
            lbName.setForeground(GREEN_DARK);
            lbIcon.setIcon(data.toIcon(GREEN_DARK));
        } else {
            lbName.setForeground(TEXT_NORMAL);
            lbIcon.setIcon(data.toIcon(ICON_NORMAL));
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lbIcon = new javax.swing.JLabel();
        lbName = new javax.swing.JLabel();

        lbIcon.setForeground(new java.awt.Color(255, 255, 255));

        lbName.setForeground(new java.awt.Color(255, 255, 255));
        lbName.setText("Menu Name");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lbIcon)
                .addGap(18, 18, 18)
                .addComponent(lbName)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbIcon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbName, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
/**
     * Dibuja los fondos redondeados de selección o hover de los ítems del menú.
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        if (!logout && (selected || over)) {
            Graphics2D g2 = (Graphics2D) grphcs;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (selected) {
                g2.setColor(GREEN_BG);
            } else {
                g2.setColor(HOVER_BG);
            }
            // Dibuja la cápsula redondeada que resalta la opción activa/hover
            g2.fillRoundRect(10, 0, getWidth() - 20, getHeight() - 4, 12, 12);
        }
        super.paintComponent(grphcs);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbIcon;
    private javax.swing.JLabel lbName;
    // End of variables declaration//GEN-END:variables
}
