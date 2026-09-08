package vistas.formulariosPrincipales;

/**
 * Pantalla de listado completo de órdenes: muestra todas las órdenes en {@code
 * tablaRegistros1} y permite acotarlas con los filtros de {@code filtrosBusqueda2} (DNI,
 * Apellido y Nombre, número de orden o examen solicitado).
 */
public class Registros extends javax.swing.JPanel {

    /**
     * Arma la pantalla y conecta el panel de filtros con la tabla: cada búsqueda (tecleo o Enter)
     * filtra sobre las órdenes ya cargadas en {@code tablaRegistros1}.
     */
    public Registros() {
        initComponents();

        filtrosBusqueda2.addBusquedaListener(new vistas.panels.Busqueda.BusquedaListener() {
            @Override
            public void onBuscar(String texto) {
                tablaRegistros1.filtrar(texto);
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBorder1 = new vistas.panels.PanelBorder();
        filtrosBusqueda2 = new vistas.registros.FiltrosBusqueda();
        tablaRegistros1 = new vistas.registros.TablaRegistros();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(982, 803));

        panelBorder1.setPreferredSize(new java.awt.Dimension(982, 803));
        panelBorder1.setLayout(new java.awt.BorderLayout());
        panelBorder1.add(filtrosBusqueda2, java.awt.BorderLayout.PAGE_START);
        panelBorder1.add(tablaRegistros1, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 1266, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.registros.FiltrosBusqueda filtrosBusqueda2;
    private vistas.panels.PanelBorder panelBorder1;
    private vistas.registros.TablaRegistros tablaRegistros1;
    // End of variables declaration//GEN-END:variables
}
