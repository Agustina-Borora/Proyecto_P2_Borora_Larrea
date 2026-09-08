package vistas.formulariosPrincipales;

/**
 * Pantalla para elegir, de las órdenes pendientes o en proceso, cuál pasar a cargar
 * resultados.
 */
public class RegistrarResultados extends javax.swing.JPanel {

    /**
     * Se dispara cuando el usuario elige una orden de la lista y confirma con "Siguiente".
     */
    public interface OrdenParaResultadosListener {
        void onOrdenSeleccionada(modelo.Paciente paciente, modelo.OrdenResumen orden);
    }

    private final java.util.List<OrdenParaResultadosListener> listenersOrden = new java.util.ArrayList<>();

    public void addOrdenParaResultadosListener(OrdenParaResultadosListener listener) {
        listenersOrden.add(listener);
    }

    /**
     * Arma la pantalla: configura {@code tablaRegistros3} para mostrar solo lo pendiente de
     * resultados (ver comentario debajo), conecta el buscador con el filtro de la tabla, y agrega
     * el manejador de "Siguiente" que busca al paciente de la orden elegida y notifica a los
     * {@link OrdenParaResultadosListener} suscriptos.
     */
    public RegistrarResultados() {
        initComponents();

        tablaRegistros3.configurarParaRegistrarResultados();

        busqueda2.addBusquedaListener(new vistas.panels.Busqueda.BusquedaListener() {
            @Override
            public void onBuscar(String texto) {
                tablaRegistros3.filtrar(texto);
            }
        });

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modelo.OrdenResumen orden = tablaRegistros3.getOrdenSeleccionada();
                if (orden == null) {
                    javax.swing.JOptionPane.showMessageDialog(RegistrarResultados.this,
                            "Seleccioná una orden de la lista antes de continuar.",
                            "Ninguna orden seleccionada", javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }

                modelo.Paciente paciente = controlador.PacienteController.buscarPorDni(
                        RegistrarResultados.this, orden.getDni());

                if (paciente == null) {
                    return;
                }

                for (OrdenParaResultadosListener listener : listenersOrden) {
                    listener.onOrdenSeleccionada(paciente, orden);
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tablaRegistros2 = new vistas.registros.TablaRegistros();
        panelBorder1 = new vistas.panels.PanelBorder();
        jPanel1 = new javax.swing.JPanel();
        tablaRegistros3 = new vistas.registros.TablaRegistros();
        busqueda2 = new vistas.panels.Busqueda();
        jButton1 = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(982, 803));

        panelBorder1.setPreferredSize(new java.awt.Dimension(982, 803));

        jPanel1.setBackground(new java.awt.Color(250, 255, 250));

        jButton1.setText("Siguiente");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tablaRegistros3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(busqueda2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1183, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(busqueda2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(57, 57, 57)
                .addComponent(tablaRegistros3, javax.swing.GroupLayout.PREFERRED_SIZE, 656, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelBorder1Layout = new javax.swing.GroupLayout(panelBorder1);
        panelBorder1.setLayout(panelBorder1Layout);
        panelBorder1Layout.setHorizontalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 1207, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 842, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.panels.Busqueda busqueda2;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private vistas.panels.PanelBorder panelBorder1;
    private vistas.registros.TablaRegistros tablaRegistros2;
    private vistas.registros.TablaRegistros tablaRegistros3;
    // End of variables declaration//GEN-END:variables
}
