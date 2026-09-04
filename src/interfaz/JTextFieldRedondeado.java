package interfaz;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextFieldUI;

/**
 * Campo de entrada de texto plano (JTextField) personalizado para Swing.
 * Cuenta con esquinas redondeadas, marca de agua (placeholder) y estilos 
 * adaptables al diseño oscuro/verde del sistema.
 */
public class JTextFieldRedondeado extends JTextField {

    // Paleta de colores para el estado visual del campo
    private Color colorFondo = new Color(33, 56, 46);       // Verde oscuro
    private Color colorBorde = new Color(55, 82, 70);       // Borde verde tenue
    private Color colorTexto = new Color(220, 235, 225);     // Texto claro
    private Color colorPlaceholder = new Color(130, 155, 142); // Texto de sugerencia atenuado
    
    /** Texto flotante/instructivo cuando el campo está vacío */
    private String placeholder = "Ingrese su email";

    /**
     * Constructor predeterminado.
     * Anula la apariencia nativa de Windows/Swing mediante BasicTextFieldUI 
     * y aplica márgenes e indicaciones de tipografía personalizadas.
     */
    public JTextFieldRedondeado() {
        // Sustituye el UI por defecto para eliminar el borde cuadrado gris de la SO
        setUI(new BasicTextFieldUI());
        
        setOpaque(false);                             // Permite renderizar formas personalizadas sin recuadros opacos
        setBackground(new Color(0, 0, 0, 0));         // Fondo transparente
        setForeground(colorTexto);                   // Color principal del texto
        setCaretColor(Color.WHITE);                   // Color del cursor de edición
        setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Define el relleno interno (Padding): arriba=12, izquierda=18, abajo=12, derecha=18
        setBorder(new EmptyBorder(12, 18, 12, 18));
    }

    /**
     * Define o modifica el texto de marca de agua (placeholder).
     * 
     * @param placeholder Texto informativo a mostrar cuando el campo permanezca vacío.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint(); // Vuelve a pintar el componente para reflejar el cambio
    }

    /**
     * Renderiza las capas visuales del componente: fondo, bordes, texto y placeholder.
     * 
     * @param g Objeto Graphics sobre el cual se realiza el pintado.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Activa el suavizado para evitar bordes dentados (Antialiasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Dibuja el fondo verde oscuro redondeado (radio 20px)
        g2.setColor(colorFondo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        // 2. Dibuja el contorno con un trazo fino
        g2.setColor(colorBorde);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.dispose(); // Libera la copia del contexto gráfico

        // 3. Renderiza el texto real ingresado por el usuario
        super.paintComponent(g);

        // 4. Renderiza el placeholder si el campo no contiene ningún carácter
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            gPlaceholder.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gPlaceholder.setColor(colorPlaceholder);
            gPlaceholder.setFont(getFont());
            
            // Centrado vertical del texto dentro de la caja de texto
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            
            // Pinta la cadena a 18px desde el margen izquierdo (alineado con el padding)
            gPlaceholder.drawString(placeholder, 18, y);
            gPlaceholder.dispose();
        }
    }
}