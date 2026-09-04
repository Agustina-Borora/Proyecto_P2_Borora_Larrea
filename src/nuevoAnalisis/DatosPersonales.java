package nuevoAnalisis;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" con los datos del paciente (nuevo o existente).
 * <p>
 * El flujo gira en torno al DNI: mientras no se completan sus {@value #LARGO_DNI} dígitos,
 * el resto de los campos del paciente queda deshabilitado y vacío (ver
 * {@link #bloquearCamposPaciente()}); al completarse, se busca el paciente mediante
 * {@link controlador.PacienteController#buscarPorDni} y, si existe, se autocompletan sus
 * datos (ver {@link #buscarPacientePorDni(String)}). {@link #construirPaciente()} arma luego
 * un {@link modelo.Paciente} con lo cargado en pantalla, listo para insertar o actualizar.
 * </p>
 */
public class DatosPersonales extends javax.swing.JPanel {

    /**
     * Prepara el panel: aplica el estilo visual (bordes redondeados, campos con esquinas
     * suaves), fija la fecha de nacimiento como no editable a mano (solo por calendario),
     * instala los filtros de validación de cada campo de texto, carga los sexos reales
     * desde la base (reemplazando el combo dummy del editor visual) y arranca la lógica
     * de búsqueda por DNI dejando los campos del paciente bloqueados hasta que se complete.
     */
    public DatosPersonales() {
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
        celular.putClientProperty("FlatLaf.style", estiloCampo);
        email.putClientProperty("FlatLaf.style", estiloCampo);
        medico.putClientProperty("FlatLaf.style", estiloCampo);
        observacion.putClientProperty("FlatLaf.style", estiloCampo);
        sexo.putClientProperty("FlatLaf.style", estiloCampo);

        // Estilo para el JDateChooser (si usas JDateChooser para la fecha)
        if (fecha != null && fecha.getDateEditor() != null) {
            javax.swing.JComponent editorUi = fecha.getDateEditor().getUiComponent();
            editorUi.putClientProperty("FlatLaf.style", estiloCampo);

            // Fecha de Nacimiento: solo se elige desde el calendario, no se
            // tipea a mano (evita fechas mal escritas o con formato raro).
            // Ni JDateChooser.setEditable ni IDateEditor.setEditable existen
            // en la versión de la librería que tiene el proyecto, así que se
            // deshabilita la edición directo sobre el JTextField interno
            // (getUiComponent() en la práctica siempre devuelve uno).
            if (editorUi instanceof javax.swing.text.JTextComponent) {
                ((javax.swing.text.JTextComponent) editorUi).setEditable(false);
            }
        }

        // Validación de los campos de texto: nombres solo con letras (nada
        // de "222222Lucía"), Celular solo dígitos con un tope razonable de
        // largo, y Observación bloqueando símbolos sueltos tipo "@@@@@" pero
        // dejando la puntuación básica de una nota clínica corta.
        ((javax.swing.text.AbstractDocument) ayn.getDocument()).setDocumentFilter(new panels.FiltroSoloLetras());
        ((javax.swing.text.AbstractDocument) medico.getDocument()).setDocumentFilter(new panels.FiltroSoloLetras());
        ((javax.swing.text.AbstractDocument) celular.getDocument()).setDocumentFilter(new panels.FiltroSoloDigitos(LARGO_MAX_CELULAR));
        ((javax.swing.text.AbstractDocument) observacion.getDocument()).setDocumentFilter(new panels.FiltroTextoObservacion());

        // Sexo real desde la base (reemplaza el combo dummy "Item 1..4").
        cargarSexos();

        // El DNI busca al paciente cuando se completan los 8 dígitos: hasta
        // entonces el resto de los datos del paciente queda bloqueado (no
        // tiene sentido cargarlos sin saber a quién pertenecen), y una vez
        // completo se desbloquean, autocompletados si el DNI ya está
        // registrado.
        configurarDni();
        bloquearCamposPaciente();
    }

    private static final int LARGO_DNI = 8;
    private static final int LARGO_MAX_CELULAR = 15;

    /** Sexos cargados desde la tabla `sexos`, en el mismo orden que las opciones del combo. */
    private java.util.List<modelo.Sexo> listaSexos = new java.util.ArrayList<>();

    /** id_paciente si el DNI tipeado corresponde a un paciente ya registrado; null si es uno nuevo. */
    private Integer idPacienteExistente;

    /**
     * Snapshot del paciente tal como estaba en la base al momento de
     * encontrarlo por DNI (null si es un paciente nuevo). Sirve para poder
     * comparar contra lo que haya en pantalla al generar la orden y saber si
     * hay que actualizarlo o no (ver datosPacienteCambiaron()).
     */
    private modelo.Paciente pacienteOriginal;

    /**
     * Trae los sexos reales de la base (tabla `sexos`) y arma el modelo del
     * combo con sus nombres. `sexo` sigue siendo JComboBox<String> (así lo
     * generó el editor visual), así que el id_sexo de cada opción se guarda
     * aparte en listaSexos, en el mismo orden que los items del combo.
     */
    private void cargarSexos() {
        listaSexos = controlador.SexoController.listarTodos(this);

        javax.swing.DefaultComboBoxModel<String> modeloSexo = new javax.swing.DefaultComboBoxModel<>();
        for (modelo.Sexo s : listaSexos) {
            modeloSexo.addElement(s.getNombreSexo());
        }
        sexo.setModel(modeloSexo);
    }

    /** Ubica en el combo el sexo cuyo id coincide con idSexoBuscado y lo selecciona; no hace nada si no lo encuentra. */
    private void seleccionarSexoPorId(int idSexoBuscado) {
        for (int i = 0; i < listaSexos.size(); i++) {
            if (listaSexos.get(i).getIdSexo() == idSexoBuscado) {
                sexo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Restringe el DNI a solo dígitos y a un máximo de LARGO_DNI caracteres,
     * y dispara la búsqueda/bloqueo del resto de los campos apenas cambia.
     */
    private void configurarDni() {
        // Misma lógica que antes (solo dígitos, tope de largo), ahora en la
        // clase compartida panels.FiltroSoloDigitos (se reusa en Celular).
        ((javax.swing.text.AbstractDocument) dni.getDocument()).setDocumentFilter(new panels.FiltroSoloDigitos(LARGO_DNI));

        dni.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { procesarCambioDni(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { procesarCambioDni(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { procesarCambioDni(); }
        });
    }

    /**
     * Reacciona a cada cambio del campo DNI: si ya tiene los {@value #LARGO_DNI} dígitos
     * dispara la búsqueda del paciente, y si no, vuelve a bloquear y vaciar el resto de
     * los campos (por si se estaba borrando un DNI ya completo).
     */
    private void procesarCambioDni() {
        String texto = dni.getText();
        if (texto.length() == LARGO_DNI) {
            buscarPacientePorDni(texto);
        } else {
            bloquearCamposPaciente();
        }
    }

    /** Vacía los campos del paciente (no toca el estado enabled/disabled). */
    private void limpiarCamposPaciente() {
        idPacienteExistente = null;
        pacienteOriginal = null;
        ayn.setText("");
        email.setText("");
        celular.setText("");
        fecha.setDate(null);
        if (sexo.getItemCount() > 0) {
            sexo.setSelectedIndex(0);
        }
    }

    /**
     * Bloquea (deshabilita y vacía) los campos que dependen de un DNI
     * completo: no tiene sentido cargarlos sin saber a qué paciente
     * corresponden. Medico Derivante y Observacion no dependen del DNI (son
     * datos del pedido, no del paciente), así que quedan siempre habilitados.
     */
    private void bloquearCamposPaciente() {
        limpiarCamposPaciente();
        ayn.setEnabled(false);
        email.setEnabled(false);
        celular.setEnabled(false);
        fecha.setEnabled(false);
        sexo.setEnabled(false);
    }

    /** Habilita los campos del paciente una vez que el DNI está completo (exista o no ya un registro con ese DNI). */
    private void desbloquearCamposPaciente() {
        ayn.setEnabled(true);
        email.setEnabled(true);
        celular.setEnabled(true);
        fecha.setEnabled(true);
        sexo.setEnabled(true);
    }

    /**
     * Se llama al completar los 8 dígitos del DNI: busca si ya existe un
     * paciente con ese DNI y, si lo encuentra, autocompleta sus datos. En
     * ambos casos (exista o no) los campos quedan habilitados, para poder
     * cargar un paciente nuevo o corregir uno existente.
     */
    private void buscarPacientePorDni(String dniTexto) {
        modelo.Paciente encontrado = controlador.PacienteController.buscarPorDni(this, dniTexto);

        desbloquearCamposPaciente();

        if (encontrado != null) {
            idPacienteExistente = encontrado.getIdPaciente();
            pacienteOriginal = encontrado;
            ayn.setText(encontrado.getNyaPaciente());
            email.setText(encontrado.getEmail());
            celular.setText(encontrado.getTelefono());
            fecha.setDate(encontrado.getFechaNacimiento());
            seleccionarSexoPorId(encontrado.getIdSexo());
        } else {
            limpiarCamposPaciente();
        }
    }

    /** true si el DNI tipeado corresponde a un paciente ya registrado. */
    public boolean esPacienteExistente() {
        return idPacienteExistente != null;
    }

    /** id_paciente del registro existente, o null si el DNI actual es de un paciente nuevo. */
    public Integer getIdPacienteExistente() {
        return idPacienteExistente;
    }

    public String getDniPaciente() {
        return dni.getText();
    }

    public String getApellidoYNombre() {
        return ayn.getText();
    }

    public java.util.Date getFechaNacimientoPaciente() {
        return fecha.getDate();
    }

    public String getEmailPaciente() {
        return email.getText();
    }

    public String getCelularPaciente() {
        return celular.getText();
    }

    public String getMedicoDerivante() {
        return medico.getText();
    }

    public String getObservacionPedido() {
        return observacion.getText();
    }

    /** id_sexo elegido en el combo, o null si todavía no hay sexos cargados. */
    public Integer getIdSexoSeleccionado() {
        int indice = sexo.getSelectedIndex();
        if (indice < 0 || indice >= listaSexos.size()) {
            return null;
        }
        return listaSexos.get(indice).getIdSexo();
    }

    /**
     * true si el DNI corresponde a un paciente existente Y algo de lo que
     * está en pantalla (nombre, email, celular, fecha de nacimiento o sexo)
     * es distinto de lo que había en la base cuando se lo encontró. Si es un
     * paciente nuevo devuelve false (no hay "cambio", hay que insertarlo).
     */
    public boolean datosPacienteCambiaron() {
        if (pacienteOriginal == null) {
            return false;
        }
        if (!normalizarTexto(ayn.getText()).equals(normalizarTexto(pacienteOriginal.getNyaPaciente()))) {
            return true;
        }
        if (!normalizarTexto(email.getText()).equals(normalizarTexto(pacienteOriginal.getEmail()))) {
            return true;
        }
        if (!normalizarTexto(celular.getText()).equals(normalizarTexto(pacienteOriginal.getTelefono()))) {
            return true;
        }
        if (!mismaFecha(fecha.getDate(), pacienteOriginal.getFechaNacimiento())) {
            return true;
        }
        Integer idSexoActual = getIdSexoSeleccionado();
        if (idSexoActual == null || idSexoActual != pacienteOriginal.getIdSexo()) {
            return true;
        }
        return false;
    }

    private String normalizarTexto(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean mismaFecha(java.util.Date a, java.util.Date b) {
        if (a == null || b == null) {
            return a == b;
        }
        java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return formato.format(a).equals(formato.format(b));
    }

    /**
     * Arma un modelo.Paciente con lo que hay cargado en pantalla, listo para
     * pasarle a PacienteDAO.insertar() (paciente nuevo) o .actualizar()
     * (paciente existente, si datosPacienteCambiaron() dio true). id_plan y
     * nro_afiliado quedan sin completar: todavía no hay en esta pantalla un
     * campo para elegir la Obra Social/Plan puntual del paciente.
     */
    public modelo.Paciente construirPaciente() {
        modelo.Paciente p = new modelo.Paciente();
        if (idPacienteExistente != null) {
            p.setIdPaciente(idPacienteExistente);
        }
        p.setNyaPaciente(ayn.getText().trim());
        p.setDni(dni.getText().trim());
        p.setFechaNacimiento(fecha.getDate());
        Integer idSexo = getIdSexoSeleccionado();
        p.setIdSexo(idSexo != null ? idSexo : 0);
        p.setTelefono(celular.getText().trim());
        p.setEmail(email.getText().trim());
        p.setIdPlan(null);
        p.setNroAfiliado(null);
        p.setIdRegistradoPor(conexiones.Sesion.idUsuario);
        return p;
    }

    /**
     * Se llama después de generar una orden con éxito: vacía el DNI (lo que
     * dispara el bloqueo/limpieza automática del resto de los datos del
     * paciente) y los dos campos que no dependen del DNI, para dejar la
     * pantalla lista para cargar la próxima orden sin arrastrar datos de la
     * anterior.
     */
    public void limpiarFormulario() {
        dni.setText("");
        medico.setText("");
        observacion.setText("");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        dni = new javax.swing.JTextField();
        celular = new javax.swing.JTextField();
        email = new javax.swing.JTextField();
        medico = new javax.swing.JTextField();
        observacion = new javax.swing.JTextField();
        ayn = new javax.swing.JTextField();
        fecha = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        sexo = new javax.swing.JComboBox<>();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(982, 300));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel1.setText("Datos del Paciente");

        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel2.setText("Fecha de Nacimiento");

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("DNI");

        medico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                medicoActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel4.setText("Sexo");

        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel5.setText("Celular");

        jLabel6.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel6.setText("Email");

        jLabel7.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel7.setText("Medico Derivante (Opcional)");

        jLabel8.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel8.setText("Observacion (Opcional)");

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel9.setText("Apellido y Nombre");

        sexo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        sexo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sexoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(101, 101, 101)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(115, 115, 115)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(email, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ayn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(92, 92, 92)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addComponent(medico, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(52, 52, 52)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(observacion, javax.swing.GroupLayout.PREFERRED_SIZE, 804, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(222, 222, 222))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(24, 24, 24))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(23, 23, 23)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(medico, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(email, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(observacion, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(85, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sexoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sexoActionPerformed

    private void medicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_medicoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_medicoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField ayn;
    private javax.swing.JTextField celular;
    private javax.swing.JTextField dni;
    private javax.swing.JTextField email;
    private com.toedter.calendar.JDateChooser fecha;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField medico;
    private javax.swing.JTextField observacion;
    private javax.swing.JComboBox<String> sexo;
    // End of variables declaration//GEN-END:variables
}
