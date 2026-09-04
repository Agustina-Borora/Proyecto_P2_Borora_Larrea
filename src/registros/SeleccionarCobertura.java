
package registros;

import com.toedter.calendar.JTextFieldDateEditor;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;


/**
 * Contenido del diálogo modal chico que arma FiltrosBusqueda para elegir la
 * cobertura a filtrar (Particular / Obra Social / Mixto, con checkboxes de
 * selección múltiple). Al confirmar, arma el texto del filtro y se lo
 * devuelve al panel que lo abrió llamando a
 * {@link FiltrosBusqueda#setCoberturaFiltro(String)}, y cierra la ventana.
 */
public class SeleccionarCobertura extends javax.swing.JPanel {

    private FiltrosBusqueda panelFiltros;


    /**
     * Arma el panel y le da a los botones Guardar/Cancelar el estilo verde
     * de FlatLaf. Se usa directamente solo en diseño (NetBeans); en tiempo
     * de ejecución siempre se instancia con {@link #SeleccionarCobertura(FiltrosBusqueda)}
     * para poder avisar el resultado.
     */
    public SeleccionarCobertura() {
        initComponents();
        // Estilo FlatLaf para los botones
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
     * Constructor real, usado por {@link FiltrosBusqueda#abrirModalCobertura()}.
     * Guarda una referencia al panel que abrió este diálogo para poder
     * avisarle la cobertura elegida cuando el usuario apreta "Guardar".
     *
     * @param panelFiltros panel FiltrosBusqueda que muestra el diálogo.
     */
    public SeleccionarCobertura(FiltrosBusqueda panelFiltros) {
        this(); // Llama al constructor de arriba para cargar componentes y estilos
        this.panelFiltros = panelFiltros;
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 51));
        jLabel1.setText("Seleccione la Cobertura a Buscar");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        add(btnGuardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 200, 118, 38));

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 200, 127, 38));

        jCheckBox1.setText("Particular");
        add(jCheckBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(39, 114, 107, 39));

        jCheckBox2.setText("Obra Social");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });
        add(jCheckBox2, new org.netbeans.lib.awtextra.AbsoluteConstraints(202, 114, -1, 39));

        jCheckBox3.setText("Mixto");
        jCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox3ActionPerformed(evt);
            }
        });
        add(jCheckBox3, new org.netbeans.lib.awtextra.AbsoluteConstraints(339, 114, 100, 39));
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Valida que haya al menos una cobertura tildada, arma el texto del
     * filtro ("Todas las coberturas" si están las 3, o las tildadas unidas
     * por coma), se lo pasa a
     * {@link FiltrosBusqueda#setCoberturaFiltro(String)} y cierra el modal.
     * Si no hay ninguna tildada, muestra una advertencia y no hace nada más.
     */
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        java.util.List<String> seleccionados = new ArrayList<>();

                    // 1. Verificar cuáles CheckBox están seleccionados
            if (jCheckBox1.isSelected()) {
                seleccionados.add(jCheckBox1.getText()); // "Particular"
            }
            if (jCheckBox2.isSelected()) {
                seleccionados.add(jCheckBox2.getText()); // "Obra Social"
            }
            if (jCheckBox3.isSelected()) {
                seleccionados.add(jCheckBox3.getText()); // "Mixto"
            }

            // 2. Validar que haya al menos una opción elegida
            if (seleccionados.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Por favor, seleccione al menos una opción de cobertura.", 
                    "Selección Incompleta", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 3. Determinar el texto a mostrar en el filtro
            String coberturaResultado;

            // Si están las 3 marcadas, mostramos "Todas"
            if (jCheckBox1.isSelected() && jCheckBox2.isSelected() && jCheckBox3.isSelected()) {
                coberturaResultado = "Todas las coberturas";
            } else {
                // De lo contrario, unimos las seleccionadas por coma
                coberturaResultado = String.join(", ", seleccionados);
            }

            // 4. Enviar resultado al panel principal
            if (panelFiltros != null) {
                panelFiltros.setCoberturaFiltro(coberturaResultado);
            }

            // 5. Cerrar ventana modal
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

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox3ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
