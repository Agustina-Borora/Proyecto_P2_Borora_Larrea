
package vistas.registros;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.Color;
import java.awt.Insets;

/**
 * Panel de filtros de la pantalla de Registros (registros.Registros): tres mini-paneles
 * clicables (Fecha, Cobertura y Estado) que, al tocarlos, abren el diálogo modal
 * correspondiente (SeleccionarFecha, SeleccionarCobertura o SeleccionarEstado).
 */
public class FiltrosBusqueda extends javax.swing.JPanel {

/**
 * Arma el panel, le da a los 3 mini-paneles de filtro (Fecha, Cobertura, Estado) el borde
 * redondeado y fondo blanco comunes, y le aplica al botón "Aplicar" el estilo verde de
 * FlatLaf.
 */
public FiltrosBusqueda() {
    initComponents();
    
    FlatLineBorder bordeRedondeado = new FlatLineBorder(new Insets(5, 5, 5, 5), new Color(0xDC, 0xE1, 0xE6), 1, 12);

    jPanel1.setBackground(Color.WHITE);
    jPanel1.setBorder(bordeRedondeado);

    jPanel2.setBackground(Color.WHITE);
    jPanel2.setBorder(bordeRedondeado);

    jPanel3.setBackground(Color.WHITE);
    jPanel3.setBorder(bordeRedondeado);

    jButton1.putClientProperty("FlatLaf.style", 
        "arc: 12; " +
        "background: #1E513B; " +
        "foreground: #FFFFFF; " +
        "focusedBackground: #163C2C; " +
        "hoverBackground: #27694C"
            
    );
        setFechaFiltro(null);
        setCoberturaFiltro(null);
        setEstadoFiltro(null);
}

/**
 * Actualiza cómo se ve el mini-panel de Fecha.
 *
 * @param fechaTexto texto a mostrar (por ejemplo "01/01/2026 -
 */
public void setFechaFiltro(String fechaTexto) {
    boolean tieneFiltro = (fechaTexto != null && !fechaTexto.trim().isEmpty());

    titulofechafiltro.setVisible(tieneFiltro);
    contenedor.setVisible(tieneFiltro);
    
    if (tieneFiltro) {
        titulofechafiltro.setText("FECHA SELECCIONADA");
        titulofechafiltro.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
        titulofechafiltro.setForeground(new java.awt.Color(120, 140, 130));

        contenedor.setText(fechaTexto);
        contenedor.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        contenedor.setForeground(new java.awt.Color(30, 81, 59));
    }
    
    Rangodefecha.setVisible(!tieneFiltro);
}

/**
 * Igual que {@link #setFechaFiltro(String)} pero para el mini-panel de Cobertura.
 *
 * @param coberturaTexto texto a mostrar (por ejemplo "Obra Social,
 */
public void setCoberturaFiltro(String coberturaTexto) {
    boolean tieneFiltro = (coberturaTexto != null && !coberturaTexto.trim().isEmpty());

    titulocobertura.setVisible(tieneFiltro);
    contenedor2.setVisible(tieneFiltro);
    
    if (tieneFiltro) {
        titulocobertura.setText("COBERTURA SELECCIONADA");
        // Mismo estilo sutil que en Fecha
        titulocobertura.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
        titulocobertura.setForeground(new java.awt.Color(120, 140, 130));

        contenedor2.setText(coberturaTexto);
        // Mismo estilo verde destacado que en Fecha
        contenedor2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        contenedor2.setForeground(new java.awt.Color(30, 81, 59));
    }
    
    Cobertura.setVisible(!tieneFiltro);
}

/**
 * Igual que {@link #setFechaFiltro(String)} pero para el mini-panel de Estado.
 *
 * @param estadoTexto texto a mostrar (por ejemplo "Pendiente" o "3
 */
public void setEstadoFiltro(String estadoTexto) {
    boolean tieneFiltro = (estadoTexto != null && !estadoTexto.trim().isEmpty());

    tituloEstado.setVisible(tieneFiltro);
    contenedor1.setVisible(tieneFiltro);
    
    if (tieneFiltro) {
        tituloEstado.setText("ESTADO SELECCIONADO");
        // Mismo estilo sutil arriba
        tituloEstado.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
        tituloEstado.setForeground(new java.awt.Color(120, 140, 130));

        contenedor1.setText(estadoTexto);
        // Mismo estilo verde destacado abajo
        contenedor1.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        contenedor1.setForeground(new java.awt.Color(30, 81, 59));
    }
    
    Estado.setVisible(!tieneFiltro);
}

/**
 * Delega en el buscador interno (busqueda1) para que la pantalla que contiene este panel
 * (Registros) pueda filtrar su tabla.
 */
public void addBusquedaListener(vistas.panels.Busqueda.BusquedaListener listener) {
    busqueda1.addBusquedaListener(listener);
}

/**
 * Arma y muestra, como diálogo modal centrado sobre esta ventana, el panel
 * SeleccionarCobertura (pasándole {@code this} para que pueda avisar el resultado vía
 * setCoberturaFiltro()).
 */
private void abrirModalCobertura() {
    javax.swing.JDialog dialog = new javax.swing.JDialog(
        (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), 
        "Seleccionar Cobertura", 
        true
    );

    SeleccionarCobertura panelCobertura = new SeleccionarCobertura(this);

    dialog.getContentPane().add(panelCobertura);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setResizable(false);
    dialog.setVisible(true);
}
/**
 * Igual que {@link #abrirModalCobertura()} pero abriendo SeleccionarEstado.
 */
private void abrirModalEstado() {
    javax.swing.JDialog dialog = new javax.swing.JDialog(
        (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), 
        "Seleccionar Estado", 
        true
    );

    SeleccionarEstado panelEstado = new SeleccionarEstado(this);

    dialog.getContentPane().add(panelEstado);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setResizable(false);
    dialog.setVisible(true);
}

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        busqueda1 = new vistas.panels.Busqueda();
        jPanel4 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        Rangodefecha = new javax.swing.JLabel();
        titulofechafiltro = new javax.swing.JLabel();
        contenedor = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        Cobertura = new javax.swing.JLabel();
        titulocobertura = new javax.swing.JLabel();
        contenedor2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        Estado = new javax.swing.JLabel();
        tituloEstado = new javax.swing.JLabel();
        contenedor1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jPanel1.setBackground(new java.awt.Color(250, 255, 250));
        jPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel1MouseClicked(evt);
            }
        });
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Rangodefecha.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        Rangodefecha.setForeground(new java.awt.Color(0, 51, 0));
        Rangodefecha.setText("       Rango de Fecha");
        Rangodefecha.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                RangodefechaMouseClicked(evt);
            }
        });
        jPanel1.add(Rangodefecha, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, 220, 30));

        titulofechafiltro.setText("Fecha Seleccionada");
        jPanel1.add(titulofechafiltro, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));
        jPanel1.add(contenedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 20, 180, 20));

        jPanel4.add(jPanel1);

        jPanel2.setBackground(new java.awt.Color(250, 255, 250));
        jPanel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel2MouseClicked(evt);
            }
        });
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Cobertura.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        Cobertura.setForeground(new java.awt.Color(0, 51, 0));
        Cobertura.setText("Cobertura");
        Cobertura.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CoberturaMouseClicked(evt);
            }
        });
        jPanel2.add(Cobertura, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 20, -1, -1));

        titulocobertura.setText("Cobertura Seleccionada");
        jPanel2.add(titulocobertura, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 6, -1, 20));
        jPanel2.add(contenedor2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, 140, 20));

        jPanel4.add(jPanel2);

        jPanel3.setBackground(new java.awt.Color(250, 255, 250));
        jPanel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel3MouseClicked(evt);
            }
        });
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Estado.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        Estado.setForeground(new java.awt.Color(0, 51, 0));
        Estado.setText("Estado");
        Estado.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                EstadoMouseClicked(evt);
            }
        });
        jPanel3.add(Estado, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 20, 70, 20));

        tituloEstado.setText("Estado Seleccionado");
        jPanel3.add(tituloEstado, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));
        jPanel3.add(contenedor1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 210, 20));

        jPanel4.add(jPanel3);

        jButton1.setBackground(new java.awt.Color(109, 171, 171));
        jButton1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Aplicar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(busqueda1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE)
                        .addGap(65, 65, 65)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(busqueda1, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(34, 34, 34))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * Click sobre el rótulo "Rango de Fecha": arma a mano el mismo diálogo modal que {@link
     * #abrirModalCobertura()}/{@link #abrirModalEstado()} pero con SeleccionarFecha (este caso
     * quedó sin extraer a un método privado propio, a diferencia de los otros dos filtros).
     */
    private void RangodefechaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RangodefechaMouseClicked
        // TODO add your handling code here:                                   
    // Crea una ventanita flotante
  
        javax.swing.JDialog dialog = new javax.swing.JDialog(
              (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), 
              "Seleccionar Fecha", 
              true
          );

          // Le pasa este FiltrosBusqueda a SeleccionarFecha
          SeleccionarFecha panelFecha = new SeleccionarFecha(this);

          dialog.getContentPane().add(panelFecha);
          dialog.pack();
          dialog.setLocationRelativeTo(this);
          dialog.setResizable(false);
          dialog.setVisible(true);

    }//GEN-LAST:event_RangodefechaMouseClicked

    /**
     * Click sobre el mini-panel de Fecha (jPanel1): abre el mismo diálogo modal de
     * SeleccionarFecha que {@link #RangodefechaMouseClicked}, para que clickear en cualquier parte
     * del panel -no solo el rótulo- abra el selector.
     */
    private void jPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MouseClicked
        // TODO add your handling code here:
            // Crea una ventanita flotante
  
        javax.swing.JDialog dialog = new javax.swing.JDialog(
              (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), 
              "Seleccionar Fecha", 
              true
          );

          // Le pasa este FiltrosBusqueda a SeleccionarFecha
          SeleccionarFecha panelFecha = new SeleccionarFecha(this);

          dialog.getContentPane().add(panelFecha);
          dialog.pack();
          dialog.setLocationRelativeTo(this);
          dialog.setResizable(false);
          dialog.setVisible(true);
    
    }//GEN-LAST:event_jPanel1MouseClicked

    private void jPanel2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseClicked
        // TODO add your handling code here:
        abrirModalCobertura();
    }//GEN-LAST:event_jPanel2MouseClicked

    private void CoberturaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CoberturaMouseClicked
        // TODO add your handling code here:
        abrirModalCobertura();
    }//GEN-LAST:event_CoberturaMouseClicked

    private void jPanel3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseClicked
        // TODO add your handling code here:
        abrirModalEstado();
    }//GEN-LAST:event_jPanel3MouseClicked

    private void EstadoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_EstadoMouseClicked
        // TODO add your handling code here:
        abrirModalEstado();
    }//GEN-LAST:event_EstadoMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Cobertura;
    private javax.swing.JLabel Estado;
    private javax.swing.JLabel Rangodefecha;
    private vistas.panels.Busqueda busqueda1;
    private javax.swing.JLabel contenedor;
    private javax.swing.JLabel contenedor1;
    private javax.swing.JLabel contenedor2;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel tituloEstado;
    private javax.swing.JLabel titulocobertura;
    private javax.swing.JLabel titulofechafiltro;
    // End of variables declaration//GEN-END:variables
}
