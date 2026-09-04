
package registros;

import com.toedter.calendar.JTextFieldDateEditor;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;


/**
 * Contenido del diálogo modal chico que arma FiltrosBusqueda para elegir un
 * rango de fechas ("desde" / "hasta") como filtro, mediante dos
 * {@link com.toedter.calendar.JDateChooser}. Al confirmar, valida y formatea
 * el rango y se lo devuelve al panel que lo abrió llamando a
 * {@link FiltrosBusqueda#setFechaFiltro(String)}, y cierra la ventana.
 */
public class SeleccionarFecha extends javax.swing.JPanel {

    private FiltrosBusqueda panelFiltros;


    /**
     * Arma el panel, bloquea la escritura manual en ambos selectores de
     * fecha (solo se puede elegir con el calendario desplegable, para
     * evitar fechas mal tipeadas) y le da a los botones Guardar/Cancelar el
     * estilo verde de FlatLaf. Se usa directamente solo en diseño
     * (NetBeans); en tiempo de ejecución siempre se instancia con
     * {@link #SeleccionarFecha(FiltrosBusqueda)} para poder avisar el
     * resultado.
     */
    public SeleccionarFecha() {
        initComponents();

    // 1. Bloquear escritura manual en ambos JDateChooser
    ((JTextFieldDateEditor) jDateChooser1.getDateEditor()).setEditable(false);
    ((JTextFieldDateEditor) jDateChooser2.getDateEditor()).setEditable(false);

    // 2. Estilo FlatLaf para los botones (Cancelar y Guardar)
    btnCancelar.putClientProperty("FlatLaf.style", "arc: 12");
    btnGuardar.putClientProperty("FlatLaf.style",
        "arc: 12; " +
        "background: #1E513B; " +
        "foreground: #FFFFFF; " +
        "focusedBackground: #163C2C; " +
        "hoverBackground: #27694C"
    );
    }

    /**
     * Constructor real, usado por FiltrosBusqueda al responder al click sobre
     * el rótulo o el mini-panel de "Rango de Fecha". Guarda una referencia
     * al panel que abrió este diálogo para poder avisarle el rango elegido
     * cuando el usuario apreta "Guardar".
     *
     * @param panelFiltros panel FiltrosBusqueda que muestra el diálogo.
     */
    public SeleccionarFecha(FiltrosBusqueda panelFiltros) {
        this(); // Llama al constructor de arriba para cargar componentes y estilos
        this.panelFiltros = panelFiltros;
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jDateChooser2 = new com.toedter.calendar.JDateChooser();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 51));
        jLabel1.setText("Seleccione el Rango de Fecha a Buscar");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel2.setText("Fecha de Inicio");

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("Fecha Final");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(77, 77, 77)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jDateChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(78, 78, 78))
            .addGroup(layout.createSequentialGroup()
                .addGap(124, 124, 124)
                .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(22, 22, 22))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(39, 39, 39)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jDateChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 94, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCancelar, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40))))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Valida que ambas fechas estén completas y que "desde" no sea posterior
     * a "hasta", formatea el rango como texto (dd/MM/yyyy) y se lo pasa a
     * {@link FiltrosBusqueda#setFechaFiltro(String)}, y cierra el modal. Si
     * falta alguna fecha o el rango es inválido, muestra un aviso y no hace
     * nada más.
     */
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        // TODO add your handling code here:
        Date fechaDesde = jDateChooser2.getDate(); // jDateChooser2 suele ser "Desde"
        Date fechaHasta = jDateChooser1.getDate(); // jDateChooser1 suele ser "Hasta"

        // 1. Validar que no estén vacíos
        if (fechaDesde == null || fechaHasta == null) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, seleccione ambas fechas para continuar.", 
                "Fecha Incompleta", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Validar orden de fechas
        if (fechaDesde.after(fechaHasta)) {
            JOptionPane.showMessageDialog(this, 
                "La fecha de  'Inicio' no puede ser posterior a la fecha 'Final'.", 
                "Rango Inválido", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Formatear y enviar a FiltrosBusqueda
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String desdeStr = sdf.format(fechaDesde);
        String hastaStr = sdf.format(fechaHasta);

        if (panelFiltros != null) {
            panelFiltros.setFechaFiltro(desdeStr + " - " + hastaStr);
        }

        // 4. Cerrar el modal
        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (win != null) {
            win.dispose();
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    /** Cierra el modal sin avisar nada a FiltrosBusqueda (descarta la selección). */
    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        // TODO add your handling code here:
        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (win != null) {
            win.dispose();
        }
    }//GEN-LAST:event_btnCancelarActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private com.toedter.calendar.JDateChooser jDateChooser2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}
