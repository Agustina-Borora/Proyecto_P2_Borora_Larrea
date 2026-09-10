package vistas.formulariosPrincipales;

/**
 * Pantalla de tipo asistente ("wizard") para generar una nueva orden de análisis: combina los
 * datos personales del paciente ({@code datosPersonales2}), el tipo de cobertura ({@code
 * tipoCobertura1}) y la selección de prestaciones del nomenclador ({@code
 * solicitudAnalisis1}), y delega en {@link controlador.NuevoAnalisisController#generarOrden}
 * el guardado transaccional del paciente, el pedido y sus análisis.
 */
public class NuevoAnalisis extends javax.swing.JPanel {

    /**
     * Arma la pantalla: configura el {@code JScrollPane} que envuelve el formulario (ver
     * comentarios 1 a 4 más abajo, sobre por qué el ancho de {@code panelBorder1} se ajusta a mano
     * en vez de dejarlo fijo) y conecta el listener de {@code tipoCobertura1} para que la columna
     * "Obra Social" de la tabla de {@code solicitudAnalisis1} se actualice sola al cambiar la
     * cobertura elegida (ver comentario 5).
     */
    public NuevoAnalisis() {
        initComponents();
        
        // 1. Configurar propiedades estéticas y comportamiento del JScrollPane
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPane1.setBorder(null);
        jScrollPane1.getViewport().setOpaque(false);

        int altoDeseado = 1150;

        final int anchoMinimoContenido = panelBorder1.getPreferredSize().width;

        jScrollPane1.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int anchoDisponible = jScrollPane1.getViewport().getWidth();
                int anchoFinal = Math.max(anchoDisponible, anchoMinimoContenido);
                panelBorder1.setPreferredSize(new java.awt.Dimension(anchoFinal, altoDeseado));
                panelBorder1.revalidate();
            }
        });

        javax.swing.SwingUtilities.invokeLater(() -> {
            int anchoInicial = jScrollPane1.getViewport().getWidth();
            if (anchoInicial > 0) {
                int anchoFinal = Math.max(anchoInicial, anchoMinimoContenido);
                panelBorder1.setPreferredSize(new java.awt.Dimension(anchoFinal, altoDeseado));
                panelBorder1.revalidate();
            }
        });

        tipoCobertura1.addCoberturaListener(new vistas.nuevoAnalisis.TipoCobertura.CoberturaListener() {
            @Override
            public void onCoberturaCambiada(String clave) {
                solicitudAnalisis1.actualizarObraSocial(clave);
                actualizarVisibilidadCobertura(clave);
            }
        });
        solicitudAnalisis1.addSeleccionListener(this::actualizarTotal);

        jLabel4.setText("Obra Social");
        jLabel6.setText("Método de Pago");
        actualizarVisibilidadCobertura(tipoCobertura1.getCoberturaSeleccionada());

        String estiloCampo = "arc: 8; ";
        jTextField2.putClientProperty("FlatLaf.style", estiloCampo);
        jComboBox2.putClientProperty("FlatLaf.style", estiloCampo);
        jComboBox3.putClientProperty("FlatLaf.style", estiloCampo);
        jTextField1.putClientProperty("FlatLaf.style", estiloCampo);

        // El total se calcula solo (suma de UB + acto bioquímico, por el precio de la UB) y el
        // precio de la UB sale de la base: ninguno de los dos se tipea a mano.
        jTextField2.setEditable(false);
        jTextField1.setEditable(false);

        cargarObrasSociales();
        cargarMetodosPago();
        cargarValorUb();
        actualizarTotal();
    }

    /**
     * Unidades Bioquímicas fijas del "acto bioquímico" que se suman siempre, además de las UB de
     * cada análisis elegido, para calcular el total.
     */
    private static final java.math.BigDecimal UB_ACTO_BIOQUIMICO = new java.math.BigDecimal(3);

    /**
     * Precio vigente de la UB, cargado una vez al abrir la pantalla (tabla `valor_ub`); null si
     * todavía no hay ningún valor cargado en la base.
     */
    private java.math.BigDecimal valorUbActual;

    /**
     * Trae las obras sociales reales de la base y arma el modelo del combo con sus nombres.
     */
    private void cargarObrasSociales() {
        java.util.List<String> nombres = controlador.ObraSocialController.listarNombres(this);
        javax.swing.DefaultComboBoxModel<String> modelo = new javax.swing.DefaultComboBoxModel<>();
        for (String nombre : nombres) {
            modelo.addElement(nombre);
        }
        jComboBox2.setModel(modelo);
    }

    /**
     * Trae los métodos de pago reales de la base y arma el modelo del combo con sus nombres.
     */
    private void cargarMetodosPago() {
        java.util.List<String> nombres = controlador.MetodoPagoController.listarNombres(this);
        javax.swing.DefaultComboBoxModel<String> modelo = new javax.swing.DefaultComboBoxModel<>();
        for (String nombre : nombres) {
            modelo.addElement(nombre);
        }
        jComboBox3.setModel(modelo);
    }

    /**
     * Trae el precio vigente de la UB (tabla `valor_ub`) y lo muestra en "Precio (UB)".
     */
    private void cargarValorUb() {
        valorUbActual = controlador.ValorUbController.obtenerVigente(this);
        jTextField1.setText(valorUbActual != null ? valorUbActual.toPlainString() : "");
    }

    /**
     * Recalcula el total cada vez que cambia la lista de análisis elegidos: (suma de las UB de
     * cada análisis + {@link #UB_ACTO_BIOQUIMICO}) × {@link #valorUbActual}. Es el mismo total
     * sin importar la cobertura elegida -lo que cambia según la cobertura es quién lo paga, no
     * cuánto es-.
     */
    private void actualizarTotal() {
        java.math.BigDecimal sumaUb = java.math.BigDecimal.ZERO;
        for (modelo.Prestacion prestacion : solicitudAnalisis1.getPrestacionesSeleccionadas()) {
            if (prestacion.getUnidadesBioquimicas() != null) {
                sumaUb = sumaUb.add(prestacion.getUnidadesBioquimicas());
            }
        }
        sumaUb = sumaUb.add(UB_ACTO_BIOQUIMICO);

        java.math.BigDecimal total = valorUbActual != null
                ? sumaUb.multiply(valorUbActual).setScale(2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;
        jTextField2.setText(total.toPlainString());
    }

    /**
     * Muestra u oculta el combo de Obra Social ({@code jComboBox2}) y el de Método de Pago
     * ({@code jComboBox3}) -con su etiqueta cada uno- según la cobertura elegida en {@code
     * tipoCobertura1}: en Particular solo tiene sentido el método de pago, en Obra Social solo la
     * obra social (la cubre al 100%, no hay nada que pagar), y en Mixto se necesitan los dos.
     */
    private void actualizarVisibilidadCobertura(String clave) {
        boolean mostrarObraSocial = "OBRA_SOCIAL".equals(clave) || "MIXTO".equals(clave);
        boolean mostrarMetodoPago = "PARTICULAR".equals(clave) || "MIXTO".equals(clave);

        jLabel4.setVisible(mostrarObraSocial);
        jComboBox2.setVisible(mostrarObraSocial);
        jLabel6.setVisible(mostrarMetodoPago);
        jComboBox3.setVisible(mostrarMetodoPago);

        jPanel1.revalidate();
        jPanel1.repaint();
    }

    /**
     * Junta los nombres de todos los campos obligatorios que falten completar (Medico Derivante y
     * Observacion quedan afuera porque están marcados como Opcional en la pantalla).
     */
    private java.util.List<String> datosFaltantes() {
        java.util.List<String> faltan = new java.util.ArrayList<>();

        String dni = datosPersonales2.getDniPaciente();
        if (dni == null || dni.trim().length() != 8) {
            faltan.add("DNI (debe tener 8 dígitos)");
        } else {
            if (esVacio(datosPersonales2.getApellidoYNombre())) {
                faltan.add("Apellido y Nombre");
            }
            if (datosPersonales2.getFechaNacimientoPaciente() == null) {
                faltan.add("Fecha de Nacimiento");
            }
            if (esVacio(datosPersonales2.getCelularPaciente())) {
                faltan.add("Celular");
            }
            if (esVacio(datosPersonales2.getEmailPaciente())) {
                faltan.add("Email");
            }
        }

        if (solicitudAnalisis1.getPrestacionesSeleccionadas().isEmpty()) {
            faltan.add("Al menos un análisis solicitado");
        }

        return faltan;
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /**
     * Guarda el paciente (nuevo o actualizado), el pedido y sus análisis en la base de datos, todo
     * en una sola transacción: si algo falla a mitad de camino se deshace todo (rollback) para no
     * dejar un pedido "a medias".
     */
    private void generarOrden() {
        boolean pacienteExistente = datosPersonales2.esPacienteExistente();

        controlador.NuevoAnalisisController.ResultadoOrden resultado = controlador.NuevoAnalisisController.generarOrden(
                this,
                pacienteExistente,
                pacienteExistente ? datosPersonales2.getIdPacienteExistente() : null,
                pacienteExistente && datosPersonales2.datosPacienteCambiaron(),
                datosPersonales2.construirPaciente(),
                datosPersonales2.getMedicoDerivante(),
                solicitudAnalisis1.getPrestacionesSeleccionadas());

        if (resultado == null) {
            return;
        }

        StringBuilder mensaje = new StringBuilder();
        if (resultado.isPacienteNuevo()) {
            mensaje.append("Se registró un paciente nuevo.\n");
        } else if (resultado.isPacienteActualizado()) {
            mensaje.append("Se actualizaron los datos del paciente.\n");
        }
        mensaje.append("Orden generada correctamente: ").append(resultado.getPedido().getNumeroPedido());
        javax.swing.JOptionPane.showMessageDialog(this, mensaje.toString(),
                "Orden generada", javax.swing.JOptionPane.INFORMATION_MESSAGE);

        limpiarPantallaCompleta();
    }

    /**
     * Vacía las tres secciones de la pantalla (Datos del Paciente, Tipo de Cobertura vuelve a
     * Particular, y la tabla de análisis elegidos), para dejarla lista para cargar la próxima
     * orden.
     */
    private void limpiarPantallaCompleta() {
        datosPersonales2.limpiarFormulario();
        solicitudAnalisis1.limpiarSeleccion();
        tipoCobertura1.seleccionarCobertura("PARTICULAR");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        panelBorder1 = new vistas.panels.PanelBorder();
        datosPersonales2 = new vistas.nuevoAnalisis.DatosPersonales();
        tipoCobertura1 = new vistas.nuevoAnalisis.TipoCobertura();
        solicitudAnalisis1 = new vistas.nuevoAnalisis.SolicitudAnalisis();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jTextField1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();

        setPreferredSize(new java.awt.Dimension(982, 803));

        jScrollPane1.setBackground(new java.awt.Color(250, 255, 250));
        jScrollPane1.setBorder(null);

        panelBorder1.setPreferredSize(new java.awt.Dimension(982, 803));

        jButton1.setText("Generar Orden ");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Cancelar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jTextField1.setText("jTextField1");

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("total");

        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N

        jLabel6.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 502, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 69, Short.MAX_VALUE)
        );

        jLabel7.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel7.setText("Precio (UB):");

        jTextField2.setText("jTextField1");
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(23, 23, 23))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(213, 213, 213)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(1217, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel3)
                                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(54, 54, 54))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(23, 23, 23)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(85, 85, 85)))
        );

        javax.swing.GroupLayout panelBorder1Layout = new javax.swing.GroupLayout(panelBorder1);
        panelBorder1.setLayout(panelBorder1Layout);
        panelBorder1Layout.setHorizontalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBorder1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37))
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tipoCobertura1, javax.swing.GroupLayout.DEFAULT_SIZE, 1597, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBorder1Layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(panelBorder1Layout.createSequentialGroup()
                        .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(solicitudAnalisis1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(datosPersonales2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(11, 11, 11))))
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(datosPersonales2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tipoCobertura1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(solicitudAnalisis1, javax.swing.GroupLayout.PREFERRED_SIZE, 343, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(panelBorder1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Manejador de "Generar Orden": valida que estén los datos obligatorios antes de intentar
     * guardar nada; si falta algo, corta acá y le muestra al usuario la lista completa de lo que
     * falta en un solo cartel, en vez de generar la orden con datos incompletos.
     */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        java.util.List<String> faltan = datosFaltantes();
        if (!faltan.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Faltan completar los siguientes datos:\n\n• " + String.join("\n• ", faltan),
                    "Datos incompletos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        generarOrden();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * Manejador de "Cancelar": pide confirmación y, si el usuario acepta, vacía todo el formulario
     * mediante {@link #limpiarPantallaCompleta()}.
     */
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int respuesta = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Descartar los datos cargados y vaciar el formulario?",
                "Cancelar", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (respuesta == javax.swing.JOptionPane.YES_OPTION) {
            limpiarPantallaCompleta();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.nuevoAnalisis.DatosPersonales datosPersonales2;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private vistas.panels.PanelBorder panelBorder1;
    private vistas.nuevoAnalisis.SolicitudAnalisis solicitudAnalisis1;
    private vistas.nuevoAnalisis.TipoCobertura tipoCobertura1;
    // End of variables declaration//GEN-END:variables
}
