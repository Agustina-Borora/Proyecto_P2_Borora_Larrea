package registrarResultados;

import nuevoAnalisis.*;

/**
 * Encabezado de solo lectura con los datos del paciente y de la orden que
 * se muestra arriba de la pantalla de carga de resultados (cargarReultados
 * lo usa como sub-componente, embebido vía el Form Editor). Trae la lista
 * real de sexos con controlador.SexoController para armar el combo "Sexo" y
 * se completa llamando a cargarDatosPaciente con el paciente y la fecha de
 * la orden elegida en Registrar Resultados.
 */
public class EncabezadoDatosPaciente extends javax.swing.JPanel {

    /**
     * Arma el panel (initComponents), aplica el estilo visual (bordes
     * redondeados de FlatLaf) a los campos y al panel contenedor, y trae
     * del controlador la lista real de sexos para reemplazar el combo
     * dummy generado por el Form Editor.
     */
    public EncabezadoDatosPaciente() {
        initComponents();

        // Borde redondeado suave para el panel contenedor
        com.formdev.flatlaf.ui.FlatLineBorder bordePanel = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(15, 15, 15, 15),
            new java.awt.Color(0xDC, 0xE1, 0xE6),
            1,
            12
        );

        this.setBackground(java.awt.Color.WHITE);
        this.setBorder(bordePanel);

        // Estilo redondeado y margen interno para las cajas de texto y combobox
        String estiloCampo = "arc: 8; ";

        dni.putClientProperty("FlatLaf.style", estiloCampo);
        ayn.putClientProperty("FlatLaf.style", estiloCampo);
        edad.putClientProperty("FlatLaf.style", estiloCampo);
        celular.putClientProperty("FlatLaf.style", estiloCampo);

        sexo.putClientProperty("FlatLaf.style", estiloCampo);

        // Estilo para el JDateChooser (si usas JDateChooser para la fecha)
        if (fecha != null && fecha.getDateEditor() != null) {
            fecha.getDateEditor().getUiComponent().putClientProperty("FlatLaf.style", estiloCampo);
        }

        // Sexo real desde la base (reemplaza el combo dummy "Item 1..4").
        cargarSexos();
    }

    /** Sexos cargados desde la tabla `sexos`, en el mismo orden que las opciones del combo. */
    private java.util.List<modelo.Sexo> listaSexos = new java.util.ArrayList<>();

    /**
     * Trae los sexos reales de la base (tabla `sexos`) y arma el modelo del
     * combo con sus nombres. Mismo patrón que nuevoAnalisis.DatosPersonales.
     */
    private void cargarSexos() {
        listaSexos = controlador.SexoController.listarTodos(this);

        javax.swing.DefaultComboBoxModel<String> modeloSexo = new javax.swing.DefaultComboBoxModel<>();
        for (modelo.Sexo s : listaSexos) {
            modeloSexo.addElement(s.getNombreSexo());
        }
        sexo.setModel(modeloSexo);
    }

    /** Ubica en listaSexos el sexo con ese id y lo selecciona en el combo. */
    private void seleccionarSexoPorId(int idSexoBuscado) {
        for (int i = 0; i < listaSexos.size(); i++) {
            if (listaSexos.get(i).getIdSexo() == idSexoBuscado) {
                sexo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Llena el encabezado con los datos de un paciente ya existente (lo usa
     * Registrar Resultados al pasar a esta pantalla, con la orden ya
     * elegida de la lista). Todos los campos quedan de solo lectura: acá no
     * se edita al paciente, solo se muestra a quién le pertenece la orden.
     *
     * @param paciente paciente dueño de la orden.
     * @param fechaAnalisis fecha del pedido (pe.fecha_pedido), mostrada en
     *                      el campo "Fecha de Analisis".
     */
    public void cargarDatosPaciente(modelo.Paciente paciente, java.util.Date fechaAnalisis) {
        if (paciente == null) {
            return;
        }

        dni.setText(paciente.getDni());
        ayn.setText(paciente.getNyaPaciente());
        celular.setText(paciente.getTelefono());
        edad.setText(String.valueOf(paciente.calcularEdad()));
        if (fechaAnalisis != null) {
            fecha.setDate(fechaAnalisis);
        }
        seleccionarSexoPorId(paciente.getIdSexo());

        // Solo lectura: esta pantalla es para cargar resultados, no para
        // editar al paciente (eso se hace desde Nuevo Análisis / Pacientes).
        dni.setEditable(false);
        ayn.setEditable(false);
        celular.setEditable(false);
        edad.setEditable(false);
        sexo.setEnabled(false);
        fecha.setEnabled(false);
        if (fecha.getDateEditor() != null) {
            javax.swing.JComponent editorUi = fecha.getDateEditor().getUiComponent();
            if (editorUi instanceof javax.swing.text.JTextComponent) {
                ((javax.swing.text.JTextComponent) editorUi).setEditable(false);
            }
        }
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        dni = new javax.swing.JTextField();
        ayn = new javax.swing.JTextField();
        fecha = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        sexo = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        celular = new javax.swing.JTextField();
        edad = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(982, 300));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N

        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel2.setText("Fecha de Analisis");

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("DNI");

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel4.setText("Sexo");

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel9.setText("Apellido y Nombre");

        sexo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        sexo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sexoActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel10.setText("Celular");

        jLabel11.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel11.setText("Edad");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(226, 226, 226)
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(edad, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel11)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel10)))))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(edad, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sexoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sexoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField ayn;
    private javax.swing.JTextField celular;
    private javax.swing.JTextField dni;
    private javax.swing.JTextField edad;
    private com.toedter.calendar.JDateChooser fecha;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JComboBox<String> sexo;
    // End of variables declaration//GEN-END:variables
}
