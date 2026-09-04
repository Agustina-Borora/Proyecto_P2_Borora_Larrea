package interfaz;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPasswordFieldUI;

/**
 * Campo de contraseña personalizado para Java Swing con diseño redondeado,
 * paleta de colores personalizada y soporte para texto placeholder.
 */
public class JPasswordFieldRedondeado extends JPasswordField {

    // Paleta de colores para el estilo gráfico
    private Color colorFondo = new Color(33, 56, 46);       // Verde oscuro
    private Color colorBorde = new Color(55, 82, 70);       // Borde verde medio
    private Color colorTexto = new Color(220, 235, 225);     // Verde muy claro/blanco
    private Color colorPlaceholder = new Color(130, 155, 142); // Verde apagado/grisáceo
    
    /** Texto informativo que se muestra cuando el campo está vacío */
    private String placeholder = "Ingresa tu contraseña";

    /**
     * Constructor por defecto. Configura las propiedades de UI, márgenes, 
     * fuentes y remueve los estilos por defecto del sistema operativo.
     */
    public JPasswordFieldRedondeado() {
        // Elimina el Look & Feel por defecto (evita el rectángulo clásico de Windows)
        setUI(new BasicPasswordFieldUI());
        
        setOpaque(false);                             // Fondo completamente transparente
        setBackground(new Color(0, 0, 0, 0));         // Transparencia alfa
        setForeground(colorTexto);                   // Color del texto escrito
        setCaretColor(Color.WHITE);                   // Color del cursor parpadeante
        setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Define el padding/relleno interno: arriba, izquierda, abajo, derecha
        setBorder(new EmptyBorder(12, 18, 12, 18));
    }

    /**
     * Establece o actualiza el texto del placeholder.
     * 
     * @param placeholder Mensaje informativo para mostrar cuando el campo esté vacío.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint(); // Forzar el redibujado de la interfaz
    }

    /**
     * Sobrescribe el renderizado nativo para pintar el fondo redondeado,
     * el borde y la marca de agua (placeholder).
     * 
     * @param g Objeto Graphics proporcionado por Swing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Activa el suavizado de bordes (Antialiasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Dibuja el fondo verde oscuro redondeado (radio 20px)
        g2.setColor(colorFondo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        // 2. Dibuja el contorno fino del componente
        g2.setColor(colorBorde);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.dispose(); // Libera el contexto de gráficos secundario

        // 3. Pinta el contenido real de la contraseña (máscara de puntos/asteriscos)
        super.paintComponent(g);

        // 4. Pinta el placeholder únicamente cuando el campo está vacío
        if (getPassword().length == 0 && placeholder != null) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            gPlaceholder.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gPlaceholder.setColor(colorPlaceholder);
            gPlaceholder.setFont(getFont());
            
            // Cálculo para centrar el texto verticalmente dentro del componente
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            
            // Dibuja el texto con un margen izquierdo de 18px (coincide con el padding)
            gPlaceholder.drawString(placeholder, 18, y);
            gPlaceholder.dispose();
        }
    }
}