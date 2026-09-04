package formulariosPrincipales;

import conexiones.Sesion;

/**
 * Ventana de inicio de sesión de la aplicación.
 * Gestiona la representación gráfica del login y el punto de entrada principal.
 */
public class Login extends javax.swing.JFrame {

    /**
     * Constructor principal de la ventana Login.
     * Configura el tamaño fijo, la posición central en pantalla y los componentes.
     */
    public Login() {
        initComponents(); // Inicializa los componentes creados por el diseñador Swing
        
        this.setSize(900, 550);            // Tamaño fijo de la ventana
        this.setLocationRelativeTo(null);   // Centra la ventana en la pantalla
        this.setResizable(false);           // Impide que el usuario cambie el tamaño
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel1 =  new interfaz.PanelCurvo();
        txtPassword = new interfaz.JPasswordFieldRedondeado();
        txtEmail = new interfaz.JTextFieldRedondeado();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new interfaz.JButtonRedondeado();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setSize(new java.awt.Dimension(900, 550));
        getContentPane().setLayout(new java.awt.GridLayout(1, 2));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 348, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel2);

        jPanel1.setBackground(new java.awt.Color(0, 102, 51));

        txtPassword.setBackground(new java.awt.Color(180, 195, 185));
        txtPassword.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        txtEmail.setBackground(new java.awt.Color(180, 195, 185));
        txtEmail.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        jLabel1.setBackground(new java.awt.Color(180, 200, 190));
        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(180, 195, 190));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel1.setText("Email");

        jLabel2.setBackground(new java.awt.Color(180, 200, 190));
        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(180, 195, 190));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Contraseña");

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 22)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Bienvenido");

        jLabel4.setBackground(new java.awt.Color(140, 165, 150));
        jLabel4.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(180, 195, 190));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Inicia Sesion Para entrar en tu cuenta");

        jLabel5.setBackground(new java.awt.Color(140, 165, 150));
        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(180, 195, 190));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("¿Olvidaste tu contraseña?");
        jLabel5.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jButton1.setText("INICIAR SESIÓN");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(135, 135, 135))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel4)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel5)
                                    .addGap(31, 31, 31))
                                .addComponent(txtPassword)
                                .addComponent(txtEmail)
                                .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)))
                        .addGap(95, 95, 95))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(60, 60, 60)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36)
                .addComponent(jLabel5)
                .addGap(68, 68, 68))
        );

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        // 1. Obtención y limpieza de espacios en blanco de los campos de texto
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        // 2. Validación de campos vacíos
        if (email.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Por favor llene todos los campos",
                "Atención",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return; // Interrumpe la ejecución si falta completar algún campo
        }

        // 3. La Vista ya no abre conexiones ni conoce los DAO: le pide el
        // resultado al Controlador (controlador.LoginController), que se
        // encarga de la conexión, la consulta y los carteles de error.
        // Boolean (no boolean): null significa "ni se pudo intentar" (sin
        // conexión o error SQL), y ahí LoginController ya mostró su cartel.
        Boolean loginExitoso = controlador.LoginController.autenticar(this, email, password);

        if (loginExitoso == null) {
            return;
        }

        if (loginExitoso) {
            // Notifica al usuario extrayendo los datos guardados en la clase Sesion
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "¡Bienvenido " + conexiones.Sesion.nombre + " " + conexiones.Sesion.apellido + "!"
            );

            // Instancia y despliega la ventana principal del sistema
            Principal principal = new Principal();
            principal.setVisible(true);

            // Cierra la ventana actual de Login
            this.dispose();
        } else {
            // Las credenciales no coinciden con ningún usuario registrado.
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Correo o contraseña incorrectos",
                "Error de Acceso",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }

    }//GEN-LAST:event_jButton1ActionPerformed

public static void main(String args[]) {
        
        /* Configuración de Look & Feel por defecto de NetBeans (Nimbus) */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Configuración del tema moderno FlatLaf */
        try {
            com.formdev.flatlaf.FlatIntelliJLaf.setup(); // Tema claro FlatLaf
        } catch(Exception ex) {
            System.err.println("Fallo al inicializar FlatLaf");
        }

        /* Despliegue seguro de la interfaz gráfica en el Event Dispatch Thread (EDT) */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtPassword;
    // End of variables declaration//GEN-END:variables
}
