package panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


/**
 * Panel contenedor de fondo/estilo general usado como base visual de casi
 * todas las pantallas del sistema. No contiene lógica de negocio: solo
 * dibuja su propio fondo con esquinas redondeadas (ver
 * {@link #paintComponent}) para que las pantallas que lo extienden o lo
 * embeben tengan un aspecto consistente en toda la aplicación.
 */
public class PanelBorder extends javax.swing.JPanel {

    /**
     * Arma el panel y lo deja no opaco para que Swing no pinte un fondo
     * rectangular por debajo del redondeado que dibuja {@link #paintComponent}.
     */
    public PanelBorder() {
        initComponents();
        setOpaque(false);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(250, 255, 250));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 361, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 197, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Pinta el fondo del panel como un rectángulo con esquinas redondeadas
     * (radio 15px), usando el color de fondo actual del componente, en lugar
     * del fondo rectangular por defecto de JPanel.
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs;
        // Activa el antialiasing para que las esquinas redondeadas se vean suaves y sin dientes de sierra
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Asigna el color de fondo actual del panel
        g2.setColor(getBackground());
        
        // Dibuja un rectángulo con esquinas redondeadas con un radio de 15x15 píxeles
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        
        super.paintComponent(grphcs);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
