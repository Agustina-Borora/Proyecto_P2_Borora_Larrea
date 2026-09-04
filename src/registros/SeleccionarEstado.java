package registros;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Contenido del diálogo modal chico que arma FiltrosBusqueda para elegir el
 * estado a filtrar (Pendiente / En Proceso / Completado - Sin enviar /
 * Completado - Enviado, con checkboxes de selección múltiple). Al confirmar,
 * arma el texto del filtro y se lo devuelve al panel que lo abrió llamando a
 * {@link FiltrosBusqueda#setEstadoFiltro(String)}, y cierra la ventana.
 */
public class SeleccionarEstado extends javax.swing.JPanel {

    private FiltrosBusqueda panelFiltros;

    /**
     * Arma el panel y le da a los botones Guardar/Cancelar el estilo verde
     * de FlatLaf. Se usa directamente solo en diseño (NetBeans); en tiempo
     * de ejecución siempre se instancia con {@link #SeleccionarEstado(FiltrosBusqueda)}
     * para poder avisar el resultado.
     */
    public SeleccionarEstado() {
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
     * Constructor real, usado por {@link FiltrosBusqueda#abrirModalEstado()}.
     * Guarda una referencia al panel que abrió este diálogo para poder
     * avisarle el estado elegido cuando el usuario apreta "Guardar".
     *
     * @param panelFiltros panel FiltrosBusqueda que muestra el diálogo.
     */
    public SeleccionarEstado(FiltrosBusqueda panelFiltros) {
        this();
        this.panelFiltros = panelFiltros;
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        completadoen = new javax.swing.JCheckBox();
        pendiente = new javax.swing.JCheckBox();
        enproceso = new javax.swing.JCheckBox();
        completadosin = new javax.swing.JCheckBox();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 51));
        jLabel1.setText("Seleccione el Estado a Buscar");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 40, -1, -1));

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        add(btnGuardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 200, 140, 40));

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        add(btnCancelar, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 200, 150, 40));

        completadoen.setText("Complentado - Enviado");
        add(completadoen, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 130, 210, 39));

        pendiente.setText("Pendiente");
        add(pendiente, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 107, 39));

        enproceso.setText("En Proceso");
        add(enproceso, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, 107, 39));

        completadosin.setText("Completado - Sin enviar");
        add(completadosin, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 130, 200, 39));
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Valida que haya al menos un estado tildado, arma el texto del filtro
     * ("Todos los estados" si están los 4, "N seleccionados" si hay más de
     * uno, o el nombre del único tildado) y se lo pasa a
     * {@link FiltrosBusqueda#setEstadoFiltro(String)}, y cierra el modal. Si
     * no hay ninguno tildado, muestra una advertencia y no hace nada más.
     */
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
List<String> seleccionados = new ArrayList<>();

    // 1. Verificar checkboxes marcados
    if (pendiente.isSelected()) {
        seleccionados.add(pendiente.getText());
    }
    if (enproceso.isSelected()) {
        seleccionados.add(enproceso.getText());
    }
    if (completadosin.isSelected()) {
        seleccionados.add(completadosin.getText());
    }
    if (completadoen.isSelected()) {
        seleccionados.add(completadoen.getText());
    }

    // 2. Validación: Al menos uno debe estar seleccionado
    if (seleccionados.isEmpty()) {
        JOptionPane.showMessageDialog(this, 
            "Por favor, seleccione al menos un estado.", 
            "Selección Incompleta", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }

    // 3. Formatear texto inteligente según la cantidad
    String estadoResultado;
    int total = seleccionados.size();

    if (total == 4) {
        estadoResultado = "Todos los estados";
    } else if (total > 1) {
        estadoResultado = total + " seleccionados";
    } else {
        estadoResultado = seleccionados.get(0);
    }

    // 4. Enviar a FiltrosBusqueda
    if (panelFiltros != null) {
        panelFiltros.setEstadoFiltro(estadoResultado);
    }

    // 5. Cerrar el modal
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
    private javax.swing.JCheckBox completadoen;
    private javax.swing.JCheckBox completadosin;
    private javax.swing.JCheckBox enproceso;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JCheckBox pendiente;
    // End of variables declaration//GEN-END:variables
}
