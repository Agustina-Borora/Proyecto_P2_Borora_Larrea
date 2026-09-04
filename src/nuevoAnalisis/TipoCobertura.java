package nuevoAnalisis;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" para elegir la cobertura de la orden:
 * Particular, Obra Social o Mixto.
 * <p>
 * Las tres opciones se muestran como tarjetas clickeables ({@link panels.PanelBorder});
 * al elegir una se resalta y se avisa el cambio a los {@link CoberturaListener} registrados
 * (en la práctica, {@link SolicitudAnalisis}, para actualizar la columna "Obra Social" de
 * su tabla). {@link formulariosPrincipales.NuevoAnalisis} obtiene el valor final con
 * {@link #getCoberturaSeleccionada()}.
 * </p>
 */
public class TipoCobertura extends javax.swing.JPanel {

    /**
     * Prepara el panel: aplica el estilo visual del contenedor, registra el click de
     * las tres tarjetas de cobertura (tarjeta y sus etiquetas hijas, para que un click
     * sobre el texto también cuente) y deja "Particular" seleccionada por defecto.
     */
public TipoCobertura() {
    initComponents();

    // 1. Estilo para el contenedor general de Tipo de Cobertura (Igual a Datos del Paciente)
    com.formdev.flatlaf.ui.FlatLineBorder bordeContenedor = new com.formdev.flatlaf.ui.FlatLineBorder(
        new java.awt.Insets(15, 15, 15, 15),
        new java.awt.Color(0xDC, 0xE1, 0xE6), // Color de borde suave
        1,
        12 // Radio del borde redondeado
    );
    this.setBackground(java.awt.Color.WHITE);
    this.setBorder(bordeContenedor);

    // 2. Las 3 tarjetas (Particular, Obra Social, Mixto) ahora son clickeables:
    // clickear la tarjeta o su texto la marca como elegida (resalta borde y
    // fondo) y libera las otras dos. NuevoAnalisis lee el valor elegido con
    // getCoberturaSeleccionada().
    registrarSeleccion(panelBorder3, "PARTICULAR", jLabel3, jLabel4);
    registrarSeleccion(panelBorder2, "OBRA_SOCIAL", jLabel8, jLabel5);
    registrarSeleccion(panelBorder1, "MIXTO", jLabel9, jLabel7, jLabel6);

    // Particular queda elegido por defecto al abrir la pantalla.
    seleccionarCobertura("PARTICULAR");
}

private final java.util.Map<String, panels.PanelBorder> tarjetasCobertura = new java.util.LinkedHashMap<>();
private String coberturaSeleccionada;

/** Se avisa cada vez que cambia la tarjeta elegida, para que Solicitud de Análisis actualice la columna "Obra Social". */
public interface CoberturaListener {
    void onCoberturaCambiada(String clave);
}

private final java.util.List<CoberturaListener> listenersCobertura = new java.util.ArrayList<>();

public void addCoberturaListener(CoberturaListener listener) {
    listenersCobertura.add(listener);
}

private static final java.awt.Color FONDO_NORMAL = new java.awt.Color(0xF8, 0xFA, 0xFC);
private static final java.awt.Color FONDO_SELECCIONADO = new java.awt.Color(0xE3, 0xF2, 0xE9);
private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_NORMAL = new com.formdev.flatlaf.ui.FlatLineBorder(
        new java.awt.Insets(10, 10, 10, 10), new java.awt.Color(0xEE, 0xF2, 0xF6), 1, 8);
private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_SELECCIONADO = new com.formdev.flatlaf.ui.FlatLineBorder(
        new java.awt.Insets(10, 10, 10, 10), new java.awt.Color(0x1E, 0x51, 0x3B), 2, 8);

/**
 * Registra una tarjeta de cobertura: guarda la referencia (para poder
 * resaltarla/des-resaltarla despues) y engancha el mismo click tanto en el
 * panel como en sus etiquetas hijas, porque un click sobre el texto lo
 * recibe el JLabel y nunca llega al panel contenedor.
 */
private void registrarSeleccion(panels.PanelBorder tarjeta, String clave, javax.swing.JComponent... hijos) {
    tarjetasCobertura.put(clave, tarjeta);
    java.awt.event.MouseAdapter clickHandler = new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            seleccionarCobertura(clave);
        }
    };
    tarjeta.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    tarjeta.addMouseListener(clickHandler);
    for (javax.swing.JComponent hijo : hijos) {
        hijo.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        hijo.addMouseListener(clickHandler);
    }
}

/** Marca "clave" como la cobertura elegida y resalta solo esa tarjeta. */
public void seleccionarCobertura(String clave) {
    if (!tarjetasCobertura.containsKey(clave)) {
        return;
    }
    coberturaSeleccionada = clave;
    for (java.util.Map.Entry<String, panels.PanelBorder> entrada : tarjetasCobertura.entrySet()) {
        boolean esElegida = entrada.getKey().equals(clave);
        panels.PanelBorder tarjeta = entrada.getValue();
        tarjeta.setBackground(esElegida ? FONDO_SELECCIONADO : FONDO_NORMAL);
        tarjeta.setBorder(esElegida ? BORDE_SELECCIONADO : BORDE_NORMAL);
        tarjeta.repaint();
    }
    for (CoberturaListener listener : listenersCobertura) {
        listener.onCoberturaCambiada(clave);
    }
}

/** "PARTICULAR", "OBRA_SOCIAL" o "MIXTO" (nunca null: Particular es el default). */
public String getCoberturaSeleccionada() {
    return coberturaSeleccionada;
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        panelBorder3 = new panels.PanelBorder();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        panelBorder2 = new panels.PanelBorder();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        panelBorder1 = new panels.PanelBorder();
        jLabel7 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("Particular");

        jLabel4.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel4.setText("El paciente abona el 100% del valor del Estudio Realizado");

        javax.swing.GroupLayout panelBorder3Layout = new javax.swing.GroupLayout(panelBorder3);
        panelBorder3.setLayout(panelBorder3Layout);
        panelBorder3Layout.setHorizontalGroup(
            panelBorder3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder3Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addComponent(jLabel3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBorder3Layout.createSequentialGroup()
                .addContainerGap(106, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addGap(45, 45, 45))
        );
        panelBorder3Layout.setVerticalGroup(
            panelBorder3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(panelBorder3);

        jLabel5.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel5.setText("La Obra Social cubre el 100% del Estudio Realizado");

        jLabel8.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel8.setText("Obra Social");

        javax.swing.GroupLayout panelBorder2Layout = new javax.swing.GroupLayout(panelBorder2);
        panelBorder2.setLayout(panelBorder2Layout);
        panelBorder2Layout.setHorizontalGroup(
            panelBorder2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder2Layout.createSequentialGroup()
                .addGroup(panelBorder2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBorder2Layout.createSequentialGroup()
                        .addGap(84, 84, 84)
                        .addComponent(jLabel5))
                    .addGroup(panelBorder2Layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(jLabel8)))
                .addContainerGap(100, Short.MAX_VALUE))
        );
        panelBorder2Layout.setVerticalGroup(
            panelBorder2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(49, Short.MAX_VALUE))
        );

        jPanel1.add(panelBorder2);

        jLabel7.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel7.setText("La Obra Social cubre  parte del Estudio ");

        jLabel6.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel6.setText("El paciente abonara la diferencia");

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel9.setText("Mixto");

        javax.swing.GroupLayout panelBorder1Layout = new javax.swing.GroupLayout(panelBorder1);
        panelBorder1.setLayout(panelBorder1Layout);
        panelBorder1Layout.setHorizontalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(78, 78, 78)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBorder1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(jLabel9)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBorder1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addContainerGap(39, Short.MAX_VALUE))
        );

        jPanel1.add(panelBorder1);

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel1.setText("Tipo de Cobertura");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1431, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private panels.PanelBorder panelBorder1;
    private panels.PanelBorder panelBorder2;
    private panels.PanelBorder panelBorder3;
    // End of variables declaration//GEN-END:variables
}
