package escritorio;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Componente visual reutilizable (JPanel) que representa una "tarjeta" con icono, título,
 * valor destacado y descripción. Se usa en el dashboard ({@code escritorio.Escritorio})
 * para mostrar estadísticas resumidas (ej. total de órdenes del mes).
 * <p>
 * El fondo se dibuja a mano en {@link #paintComponent(Graphics)} como un degradado
 * ({@link GradientPaint}) entre {@link #color1} y {@link #color2}, con esquinas redondeadas,
 * por eso el panel se crea no opaco (ver constructor).
 * </p>
 */
public class Card extends javax.swing.JPanel {

    /** Primer color utilizado para el degradado del fondo. */
    private Color color1;

    /** Segundo color utilizado para el degradado del fondo. */
    private Color color2;

    /**
     * Obtiene el primer color del degradado de fondo.
     *
     * @return El objeto {@link Color} correspondiente al inicio del degradado.
     */
    public Color getColor1() {
        return color1;
    }

    /**
     * Establece el primer color del degradado y redibuja el componente.
     *
     * @param color1 El nuevo {@link Color} para la parte superior del degradado.
     */
    public void setColor1(Color color1) {
        this.color1 = color1;
        repaint(); // Vuelve a pintar cuando cambias el color
    }

    /**
     * Obtiene el segundo color del degradado de fondo.
     *
     * @return El objeto {@link Color} correspondiente al final del degradado.
     */
    public Color getColor2() {
        return color2;
    }

    /**
     * Establece el segundo color del degradado y redibuja el componente.
     *
     * @param color2 El nuevo {@link Color} para la parte inferior del degradado.
     */
    public void setColor2(Color color2) {
        this.color2 = color2;
        repaint(); // Vuelve a pintar cuando cambias el color
    }

    /**
     * Constructor por defecto.
     * Inicializa los componentes de la interfaz, define el panel como transparente
     * para permitir bordes redondeados personalizados y asigna un tono claro por defecto a los colores.
     */
    public Card() {
        initComponents();
        setOpaque(false);
    }

    /**
     * Modifica el color del texto de todas las etiquetas del componente (Título, Valor y Descripción).
     *
     * @param color El objeto {@link Color} que se aplicará a los textos.
     */
    public void setTextColor(Color color) {
        lbTitle.setForeground(color);
        lbValues.setForeground(color);
        lbDescription.setForeground(color);
    }

    /**
     * Establece el título de la tarjeta (ej. "Total del mes").
     */
    public void setTitle(String title) {
        lbTitle.setText(title);
    }

    /**
     * Establece el valor principal/destacado de la tarjeta (ej. "26").
     */
    public void setValues(String values) {
        lbValues.setText(values);
    }

    /**
     * Establece la descripción secundaria de la tarjeta (ej. "Análisis registrados").
     */
    public void setDescription(String description) {
        lbDescription.setText(description);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lbIcon = new javax.swing.JLabel();
        lbTitle = new javax.swing.JLabel();
        lbValues = new javax.swing.JLabel();
        lbDescription = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        //lbIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/raven/icon/stock.png"))); // NOI18N

        lbTitle.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lbTitle.setForeground(new java.awt.Color(255, 255, 255));
        lbTitle.setText("Total");

        lbValues.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        lbValues.setForeground(new java.awt.Color(255, 255, 255));
        lbValues.setText("Values");

        lbDescription.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        lbDescription.setForeground(new java.awt.Color(255, 255, 255));
        lbDescription.setText("Description");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbDescription)
                    .addComponent(lbValues)
                    .addComponent(lbTitle)
                    .addComponent(lbIcon))
                .addContainerGap(283, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(lbIcon)
                .addGap(18, 18, 18)
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbValues)
                .addGap(18, 18, 18)
                .addComponent(lbDescription)
                .addContainerGap(25, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

/**
     * Pinta el fondo de la tarjeta como un degradado (de {@link #color1} a {@link #color2})
     * con bordes redondeados, antes de delegar en {@code super.paintComponent} para que
     * Swing dibuje encima el icono y los textos.
     */
    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibuja el fondo plano o degradado pastel
        GradientPaint g = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
        g2.setPaint(g);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        // Opcional: si quieres un borde sutil alrededor de la tarjeta
        // g2.setColor(color1.darker());
        // g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

        super.paintComponent(grphcs);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbDescription;
    private javax.swing.JLabel lbIcon;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JLabel lbValues;
    // End of variables declaration//GEN-END:variables
}
