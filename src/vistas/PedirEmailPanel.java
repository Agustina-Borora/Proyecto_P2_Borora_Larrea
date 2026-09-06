package vistas;

/**
 * Pantalla "Recuperar contraseña": primer paso del flujo, donde se pide el
 * email de la cuenta antes de mandar el código de verificación.
 *
 * A diferencia de {@link TokenDeConfirmacion} y {@link CambiarContraseña}
 * (que reutilizan el layout de dos mitades — blanca + {@link interfaz.PanelCurvo}
 * — pensado para pantallas grandes tipo Login), esta es una tarjeta chica y
 * autocontenida: un solo campo con ícono, un link para volver y un botón,
 * todo sobre un fondo verde muy oscuro. Por eso {@link RecuperarContrasenaFrame}
 * muestra esta pantalla en una ventana más chica que las otras dos del mismo
 * flujo.
 *
 * Igual que {@link RecuperarContrasenaFrame}, se escribió a mano y no tiene
 * un {@code .form} propio.
 *
 * Quien arme esta pantalla se suscribe a {@link EmailIngresadoListener} para
 * pedirle el código a {@link controlador.PasswordController#solicitarCodigo},
 * y a {@link VolverListener} para volver al login si el usuario se arrepiente.
 */
public class PedirEmailPanel extends javax.swing.JPanel {

    /** Color de fondo de toda la tarjeta: verde muy oscuro, casi negro. */
    private static final java.awt.Color COLOR_FONDO = new java.awt.Color(9, 26, 19);

    /**
     * Se dispara cuando el usuario carga un email y hace click en "ENVIAR".
     */
    public interface EmailIngresadoListener {
        void onEmailIngresado(String email);
    }

    /**
     * Se dispara cuando el usuario hace click en "Regresar al login", sin
     * llegar a pedir ningún código.
     */
    public interface VolverListener {
        void onVolverClicked();
    }

    private final java.util.List<EmailIngresadoListener> listenersEmail = new java.util.ArrayList<>();
    private final java.util.List<VolverListener> listenersVolver = new java.util.ArrayList<>();

    public void addEmailIngresadoListener(EmailIngresadoListener listener) {
        listenersEmail.add(listener);
    }

    public void addVolverListener(VolverListener listener) {
        listenersVolver.add(listener);
    }

    private javax.swing.JLabel jLabelEmail;
    private CampoEmailContorno txtEmail;
    private javax.swing.JLabel jLabelVolver;
    private interfaz.JButtonRedondeado jButtonEnviar;

    public PedirEmailPanel() {
        initComponents();
    }

    private void initComponents() {
        setBackground(COLOR_FONDO);
        setBorder(new javax.swing.border.EmptyBorder(46, 46, 38, 46));
        setLayout(new java.awt.GridBagLayout());

        jLabelEmail = new javax.swing.JLabel("Email:");
        jLabelEmail.setForeground(new java.awt.Color(225, 235, 228));
        jLabelEmail.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 15));

        txtEmail = new CampoEmailContorno();
        txtEmail.setPreferredSize(new java.awt.Dimension(10, 48));
        txtEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarClicked(evt);
            }
        });

        jLabelVolver = new javax.swing.JLabel("←  Regresar al login");
        jLabelVolver.setForeground(new java.awt.Color(90, 200, 140));
        jLabelVolver.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        jLabelVolver.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabelVolver.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                for (VolverListener listener : listenersVolver) {
                    listener.onVolverClicked();
                }
            }
        });

        jButtonEnviar = new interfaz.JButtonRedondeado();
        jButtonEnviar.setText("ENVIAR");
        jButtonEnviar.setPreferredSize(new java.awt.Dimension(150, 46));
        jButtonEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarClicked(evt);
            }
        });

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(0, 0, 8, 0);
        add(jLabelEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(0, 0, 30, 0);
        add(txtEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);
        add(jLabelVolver, gbc);

        gbc.gridx = 1;
        gbc.anchor = java.awt.GridBagConstraints.EAST;
        gbc.weightx = 0;
        add(jButtonEnviar, gbc);
    }

    private void enviarClicked(java.awt.event.ActionEvent evt) {
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Ingresá el email de tu cuenta.",
                    "Falta el email", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (EmailIngresadoListener listener : listenersEmail) {
            listener.onEmailIngresado(email);
        }
    }

    /**
     * Campo de texto para el email de esta tarjeta: contorno redondeado
     * (sin relleno, a diferencia de {@link interfaz.JTextFieldRedondeado})
     * con un sobre dibujado a mano a la izquierda y placeholder propio.
     *
     * Se define acá (y no en el paquete {@code interfaz}) porque este estilo
     * "solo contorno" es específico de esta tarjeta chica; el resto de las
     * pantallas usa el campo con relleno verde de {@code interfaz}.
     */
    private static class CampoEmailContorno extends javax.swing.JTextField {

        private static final String PLACEHOLDER = "Tu email aquí";

        private final java.awt.Color colorBorde = new java.awt.Color(110, 135, 122);
        private final java.awt.Color colorTexto = new java.awt.Color(225, 235, 228);
        private final java.awt.Color colorPlaceholder = new java.awt.Color(140, 160, 150);

        CampoEmailContorno() {
            setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
            setOpaque(false);
            setBackground(new java.awt.Color(0, 0, 0, 0));
            setForeground(colorTexto);
            setCaretColor(java.awt.Color.WHITE);
            setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            // Margen interno: 50px a la izquierda para dejarle lugar al ícono del sobre.
            setBorder(new javax.swing.border.EmptyBorder(14, 50, 14, 18));
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            // Contorno redondeado sin relleno: el campo se ve "hueco" sobre el
            // fondo oscuro de la tarjeta, en vez del relleno verde sólido que
            // usa interfaz.JTextFieldRedondeado en el resto de las pantallas.
            g2.setColor(colorBorde);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

            // Ícono de sobre dibujado a mano (rectángulo + solapa en V), para no
            // depender de ningún archivo de imagen que el proyecto no tiene.
            int iconoX = 16;
            int iconoAncho = 20;
            int iconoAlto = 14;
            int iconoY = (getHeight() - iconoAlto) / 2;
            g2.drawRoundRect(iconoX, iconoY, iconoAncho, iconoAlto, 3, 3);
            g2.drawLine(iconoX, iconoY, iconoX + iconoAncho / 2, iconoY + iconoAlto / 2);
            g2.drawLine(iconoX + iconoAncho, iconoY, iconoX + iconoAncho / 2, iconoY + iconoAlto / 2);

            g2.dispose();

            super.paintComponent(g);

            if (getText().isEmpty()) {
                java.awt.Graphics2D gPlaceholder = (java.awt.Graphics2D) g.create();
                gPlaceholder.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                gPlaceholder.setColor(colorPlaceholder);
                gPlaceholder.setFont(getFont());
                java.awt.FontMetrics fm = gPlaceholder.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                gPlaceholder.drawString(PLACEHOLDER, 50, y);
                gPlaceholder.dispose();
            }
        }
    }
}
