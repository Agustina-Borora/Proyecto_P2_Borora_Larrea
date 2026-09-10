/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vista.usuarios;

/**
 *
 * @author agust
 */
public class PermisosExamen extends javax.swing.JPanel {

    /**
     * Id del examen (analisis_tipos.id_analisis_tipo) que representa esta fila.
     */
    private int idAnalisisTipo;

    /**
     * Creates new form PermisosExamen
     */
    /**
     * Ícono circular compartido por los 3 radio buttons: aro gris fino sin seleccionar, círculo
     * verde institucional relleno (con un punto blanco al centro) seleccionado. Reemplaza el
     * ícono nativo del Look & Feel para que se vea igual que en el diseño en cualquier plataforma.
     */
    private static final javax.swing.Icon ICONO_RADIO = new IconoRadioVerde();

    public PermisosExamen() {
        initComponents();
        reorganizarLayout();
        jRadioButton1.setSelected(true);
    }

    /**
     * El GroupLayout que arma el Form Editor fija el nombre del examen en 496px y usa gaps fijos
     * entre los radio buttons (pensado para un contenedor bastante más ancho que los ~844px
     * disponibles en la pestaña "Permisos y Acceso" de CrearUsuario) -- y ese ancho de 496 se
     * termina respetando pase lo que pase, incluso si se intenta achicar el preferredSize del
     * label por código. Se reemplaza el layout entero por un GridBagLayout hecho a mano: columna
     * del nombre con ancho fijo más chico, y los 3 radio buttons repartidos en columnas iguales.
     * {@link vista.usuarios.CrearUsuario#construirEncabezadoPermisos()} arma el encabezado con la
     * misma estructura de columnas para que quede alineado.
     */
    private void reorganizarLayout() {
        removeAll();
        setLayout(new java.awt.GridBagLayout());

        // Alto fijo en vez de tomar jLabel36.getPreferredSize().height: en este punto (dentro del
        // constructor, antes de cargarExamen()) la etiqueta todavía no tiene texto, y un JLabel
        // vacío devuelve preferredSize con height 0 -- ese 0 quedaba fijado para siempre y volvía
        // invisible el nombre del examen aunque el texto se cargara correctamente después.
        jLabel36.setPreferredSize(new java.awt.Dimension(280, 24));
        // Gris oscuro en vez del negro por defecto -- mismo tono que usan las otras tablas de la
        // app (EstiloTablaFlatLaf) para el texto principal.
        jLabel36.setForeground(new java.awt.Color(51, 65, 85));

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridy = 0;
        // Mismos insets que usa CrearUsuario.construirEncabezadoPermisos() para su encabezado --
        // tienen que coincidir para que las columnas queden alineadas.
        gbc.insets = new java.awt.Insets(8, 16, 8, 12);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel36, gbc);

        for (javax.swing.JRadioButton radio : new javax.swing.JRadioButton[]{jRadioButton1, jRadioButton2, jRadioButton3}) {
            radio.setIcon(ICONO_RADIO);
            radio.setSelectedIcon(ICONO_RADIO);
            radio.setContentAreaFilled(false);
            radio.setBorderPainted(false);
            radio.setFocusPainted(false);
        }

        gbc.weightx = 1;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        gbc.gridx = 1;
        add(jRadioButton1, gbc);
        gbc.gridx = 2;
        add(jRadioButton2, gbc);
        gbc.gridx = 3;
        add(jRadioButton3, gbc);
    }

    /**
     * Ícono circular usado por los radio buttons de esta fila (ver {@link #ICONO_RADIO}).
     */
    private static final class IconoRadioVerde implements javax.swing.Icon {

        private static final int DIAMETRO = 18;
        private static final java.awt.Color VERDE = new java.awt.Color(39, 103, 73);
        private static final java.awt.Color GRIS_ARO = new java.awt.Color(200, 206, 202);

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            boolean seleccionado = c instanceof javax.swing.AbstractButton && ((javax.swing.AbstractButton) c).isSelected();
            if (seleccionado) {
                g2.setColor(VERDE);
                g2.fillOval(x, y, DIAMETRO, DIAMETRO);
                int puntoDiametro = DIAMETRO / 2;
                g2.setColor(java.awt.Color.WHITE);
                g2.fillOval(x + (DIAMETRO - puntoDiametro) / 2, y + (DIAMETRO - puntoDiametro) / 2, puntoDiametro, puntoDiametro);
            } else {
                g2.setColor(GRIS_ARO);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(x + 1, y + 1, DIAMETRO - 2, DIAMETRO - 2);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return DIAMETRO;
        }

        @Override
        public int getIconHeight() {
            return DIAMETRO;
        }
    }

    /**
     * Carga el examen que representa esta fila: el nombre en la etiqueta y el id para poder
     * identificarla después (por ejemplo, al guardar los permisos elegidos).
     */
    public void cargarExamen(modelo.AnalisisTipo examen) {
        this.idAnalisisTipo = examen.getIdAnalisisTipo();
        jLabel36.setText(examen.getNombreAnalisis());
    }

    public int getIdAnalisisTipo() {
        return idAnalisisTipo;
    }

    /**
     * Deshabilita los 3 radio buttons (modo Ver de {@link vista.usuarios.CrearUsuario}), dejando
     * a la vista el nivel de acceso actual sin permitir cambiarlo.
     */
    public void setSoloLectura(boolean soloLectura) {
        boolean habilitado = !soloLectura;
        jRadioButton1.setEnabled(habilitado);
        jRadioButton2.setEnabled(habilitado);
        jRadioButton3.setEnabled(habilitado);
    }

    /**
     * Nivel de acceso elegido para este examen ("Sin acceso", "Solo ver" o "Cargar"), en el
     * mismo orden en que aparecen los radio buttons de izquierda a derecha.
     */
    public String getNivelAcceso() {
        if (jRadioButton3.isSelected()) {
            return "Cargar";
        }
        if (jRadioButton2.isSelected()) {
            return "Solo ver";
        }
        return "Sin acceso";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jLabel36 = new javax.swing.JLabel();

        buttonGroup1.add(jRadioButton1);

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton3);

        jLabel36.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 496, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton1)
                .addGap(154, 154, 154)
                .addComponent(jRadioButton2)
                .addGap(106, 106, 106)
                .addComponent(jRadioButton3)
                .addContainerGap(74, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel36, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jRadioButton1)
                            .addComponent(jRadioButton2)
                            .addComponent(jRadioButton3))))
                .addGap(22, 22, 22))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    // End of variables declaration//GEN-END:variables
}
