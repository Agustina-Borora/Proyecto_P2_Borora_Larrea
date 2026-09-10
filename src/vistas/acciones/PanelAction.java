package vistas.acciones;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel con los botones redondos (ver / editar / compartir / eliminar) que se pinta dentro de la
 * columna "Accion" de una tabla. Cada botón es opcional: el constructor recibe un flag por
 * botón, para que cada pantalla muestre solo los que necesita.
 */
public class PanelAction extends javax.swing.JPanel {

    /**
     * Construye el panel con los 4 botones visibles.
     */
    public PanelAction() {
        this(true, true, true, true);
    }

    /**
     * @param mostrarVer ver botón ver.
     * @param mostrarEditar ver botón editar.
     * @param mostrarCompartir ver botón compartir.
     * @param mostrarEliminar ver botón eliminar.
     */
    public PanelAction(boolean mostrarVer, boolean mostrarEditar, boolean mostrarCompartir, boolean mostrarEliminar) {
        initComponents();
        cmdView.setVisible(mostrarVer);
        cmdEdit.setVisible(mostrarEditar);
        cmdEdit1.setVisible(mostrarCompartir);
        cmdDelete.setVisible(mostrarEliminar);
    }

    /**
     * Conecta cada botón con su acción correspondiente para la fila dada.
     */
    public void initEvent(TableActionEvent event, int row) {
        cmdView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                event.onView(row);
            }
        });
        cmdEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                event.onEdit(row);
            }
        });
        cmdEdit1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                event.onShare(row);
            }
        });
        cmdDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                event.onDelete(row);
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cmdView = new vistas.acciones.ActionButton();
        cmdEdit = new vistas.acciones.ActionButton();
        cmdDelete = new vistas.acciones.ActionButton();
        cmdEdit1 = new vistas.acciones.ActionButton();

        cmdView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/ojo.png"))); // NOI18N

        cmdEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/lapiz.png"))); // NOI18N

        cmdDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/basura.png"))); // NOI18N

        cmdEdit1.setBackground(new java.awt.Color(255, 255, 255));
        cmdEdit1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/compartir.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(cmdEdit1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmdView, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmdEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmdDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cmdView, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .addComponent(cmdEdit, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmdEdit1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cmdDelete, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.acciones.ActionButton cmdDelete;
    private vistas.acciones.ActionButton cmdEdit;
    private vistas.acciones.ActionButton cmdEdit1;
    private vistas.acciones.ActionButton cmdView;
    // End of variables declaration//GEN-END:variables
}
