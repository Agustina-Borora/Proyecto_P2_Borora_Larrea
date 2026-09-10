package vistas.formulariosPrincipales;

/**
 * Pantalla de "Usuarios" del menú lateral (sección Administración).
 */
public class Usuarios extends javax.swing.JPanel {

    /**
     * Construye el panel, inicializa sus componentes gráficos y conecta el botón "Nuevo Usuario".
     * El listado, y el Ver/Editar/Eliminar de cada fila, los maneja tablaUsuarios1 por su cuenta
     * (mismo criterio que vistas.pacientes.TablaPacientes con la pantalla "Pacientes").
     */
    public Usuarios() {
        initComponents();
        jButton1.addActionListener(evt -> abrirNuevoUsuario());
        tablaUsuarios1.setAlCargarUsuarios(this::actualizarTotales);
    }

    /**
     * Refleja en cardUsuarios1 (Total de Usuarios / Administradores / Tecnicos) el listado que
     * acaba de (re)cargar tablaUsuarios1.
     */
    private void actualizarTotales(java.util.List<modelo.Usuario> usuarios) {
        int administradores = 0;
        int tecnicos = 0;
        for (modelo.Usuario usuario : usuarios) {
            if ("Administrador".equalsIgnoreCase(usuario.getRol())) {
                administradores++;
            } else if ("Tecnico".equalsIgnoreCase(usuario.getRol())) {
                tecnicos++;
            }
        }
        cardUsuarios1.setTotales(usuarios.size(), administradores, tecnicos);
    }

    /**
     * Abre "Nuevo Usuario" ({@link vista.usuarios.CrearUsuario}) como ventana flotante (modal),
     * centrada sobre esta pantalla.
     */
    private void abrirNuevoUsuario() {
        vista.usuarios.CrearUsuario panel = new vista.usuarios.CrearUsuario();

        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Nuevo Usuario",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        panel.getBotonCancelar().addActionListener(evt -> dialogo.dispose());

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBorder1 = new vistas.panels.PanelBorder();
        cardUsuarios1 = new vista.usuarios.CardUsuarios();
        jLabel6 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        tablaUsuarios1 = new vista.usuarios.TablaUsuarios();

        setPreferredSize(new java.awt.Dimension(982, 803));

        panelBorder1.setPreferredSize(new java.awt.Dimension(982, 803));

        jLabel6.setFont(new java.awt.Font("SansSerif", 1, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 102, 0));
        jLabel6.setText("Cuentas y Permisos");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 21)); // NOI18N
        jLabel5.setText("Listado");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel5)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton1.setText("Nuevo Usuario");

        javax.swing.GroupLayout panelBorder1Layout = new javax.swing.GroupLayout(panelBorder1);
        panelBorder1.setLayout(panelBorder1Layout);
        panelBorder1Layout.setHorizontalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tablaUsuarios1, javax.swing.GroupLayout.DEFAULT_SIZE, 1012, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cardUsuarios1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelBorder1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)))
                .addContainerGap())
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addComponent(cardUsuarios1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tablaUsuarios1, javax.swing.GroupLayout.DEFAULT_SIZE, 615, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 1036, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 944, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vista.usuarios.CardUsuarios cardUsuarios1;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private vistas.panels.PanelBorder panelBorder1;
    private vista.usuarios.TablaUsuarios tablaUsuarios1;
    // End of variables declaration//GEN-END:variables
}
