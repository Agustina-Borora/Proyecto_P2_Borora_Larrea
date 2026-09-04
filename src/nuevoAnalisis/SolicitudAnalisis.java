package nuevoAnalisis;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" para buscar y seleccionar las prestaciones
 * del nomenclador que va a incluir la orden.
 * <p>
 * La búsqueda (campo Código/Nombre + botón "Agregar") se resuelve contra
 * {@link controlador.NomencladorController#buscar} y los resultados se acumulan en
 * {@link #seleccionados}, reflejados en la tabla. La columna "Obra Social" de cada fila
 * depende del tipo de cobertura elegido en {@link TipoCobertura}, que avisa los cambios
 * llamando a {@link #actualizarObraSocial(String)}.
 * </p>
 */
public class SolicitudAnalisis extends javax.swing.JPanel {

    /**
     * Prepara el panel: aplica el estilo visual (bordes redondeados), instala el filtro
     * de validación del campo de búsqueda, reemplaza el modelo dummy de la tabla por uno
     * vacío cuya columna "Obra Social" solo es editable con cobertura Mixta, y engancha
     * el botón "Agregar" (y Enter en el campo de búsqueda) a {@link #agregarAnalisis()}.
     */
public SolicitudAnalisis() {
    initComponents();

    // 1. Borde contenedor del panel principal
    com.formdev.flatlaf.ui.FlatLineBorder bordePanel = new com.formdev.flatlaf.ui.FlatLineBorder(
        new java.awt.Insets(15, 15, 15, 15), 
        new java.awt.Color(0xDC, 0xE1, 0xE6), 
        1, 
        12
    );

    this.setBackground(java.awt.Color.WHITE);
    this.setBorder(bordePanel);

    // 2. Estilo redondeado solo con 'arc' para la caja de texto y el botón
    String estiloRedondeado = "arc: 8;";
    
    jTextField1.putClientProperty("FlatLaf.style", estiloRedondeado);
    jButton1.putClientProperty("FlatLaf.style", estiloRedondeado);

    // 3. Quitar el borde feo del scrollpane de la tabla para que encaje impecable
    jScrollPane2.setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(
        new java.awt.Insets(0, 0, 0, 0),
        new java.awt.Color(0xDC, 0xE1, 0xE6),
        1,
        8
    ));

    // 4. El campo Codigo/Nombre busca en el nomenclador (la tabla que salió
    // del Excel que ya habíamos cargado): misma validación que los
    // buscadores de Pacientes/Registros -- nada de símbolos, y si arranca
    // con número sigue en número, si arranca con letra sigue en letra.
    ((javax.swing.text.AbstractDocument) jTextField1.getDocument()).setDocumentFilter(new panels.FiltroLetraONumero());

    // 5. Reemplaza las 4 filas dummy que trae el editor visual por un
    // modelo real y vacío. La columna "Obra Social" (indice 2) solo se
    // puede tocar a mano cuando la cobertura elegida es Mixto: ahi arranca
    // en "Si" por fila pero se puede pasar a "No" si esa prestacion en
    // particular no la cubre la obra social. En Particular/Obra Social es
    // fijo (No/Si) para todas las filas y no se edita.
    jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[0][0], COLUMNAS_TABLA) {
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 && "MIXTO".equals(coberturaActual);
        }
    });
    jTable1.getColumnModel().getColumn(2).setCellEditor(
            new javax.swing.DefaultCellEditor(new javax.swing.JComboBox<>(new String[]{"Sí", "No"})));

    // 6. "Agregar" busca por código exacto (si el texto es numérico) o por
    // nombre (si es texto) y agrega la(s) prestación(es) encontradas a la
    // tabla; Enter en el campo hace lo mismo que clickear el botón.
    jButton1.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            agregarAnalisis();
        }
    });
    jTextField1.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            agregarAnalisis();
        }
    });
}

private static final String[] COLUMNAS_TABLA = {"Codigo", "Nombre", "Obra Social"};

/** Prestaciones ya agregadas a la orden, en el mismo orden que las filas de jTable1. */
private final java.util.List<modelo.Prestacion> seleccionados = new java.util.ArrayList<>();

/** Tipo de cobertura elegido en la pantalla (lo manda TipoCobertura via listener). Particular es el default. */
private String coberturaActual = "PARTICULAR";

/**
 * Busca en el nomenclador lo que haya en el campo Codigo/Nombre: si arranca
 * con un dígito se busca por código exacto (un solo resultado posible), si
 * arranca con una letra se busca por nombre (puede traer más de uno). Lo
 * que encuentra se agrega a la tabla, salvo que ya esté agregado.
 */
private void agregarAnalisis() {
    String texto = jTextField1.getText().trim();
    if (texto.isEmpty()) {
        return;
    }

    java.util.List<modelo.Prestacion> encontrados = controlador.NomencladorController.buscar(this, texto);

    if (encontrados.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(this, "No se encontró ningún análisis con \"" + texto + "\".",
                "Sin resultados", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    javax.swing.table.DefaultTableModel modeloTabla = (javax.swing.table.DefaultTableModel) jTable1.getModel();
    java.util.List<String> yaAgregados = new java.util.ArrayList<>();
    for (modelo.Prestacion p : encontrados) {
        if (yaAgregado(p.getCodigo())) {
            yaAgregados.add(p.getCodigo() + " - " + p.getNombrePrestacion());
            continue; // no duplicar la misma prestación si se busca dos veces
        }
        seleccionados.add(p);
        modeloTabla.addRow(new Object[]{p.getCodigo(), p.getNombrePrestacion(), valorObraSocialPorDefecto()});
    }

    if (!yaAgregados.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(this,
                "Ya está agregado a la orden:\n" + String.join("\n", yaAgregados),
                "Análisis ya seleccionado", javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    jTextField1.setText("");
}

private boolean yaAgregado(int codigo) {
    for (modelo.Prestacion p : seleccionados) {
        if (p.getCodigo() == codigo) {
            return true;
        }
    }
    return false;
}

/** "No" si es Particular, "Si" en cualquier otro caso (Obra Social fijo en Si, Mixto arranca en Si y se puede editar). */
private String valorObraSocialPorDefecto() {
    return "PARTICULAR".equals(coberturaActual) ? "No" : "Sí";
}

/** Prestaciones (del nomenclador) agregadas a la orden hasta el momento. */
public java.util.List<modelo.Prestacion> getPrestacionesSeleccionadas() {
    return new java.util.ArrayList<>(seleccionados);
}

/** Se llama después de generar una orden con éxito: vacía la tabla y la lista para la próxima orden. */
public void limpiarSeleccion() {
    seleccionados.clear();
    javax.swing.table.DefaultTableModel modeloTabla = (javax.swing.table.DefaultTableModel) jTable1.getModel();
    modeloTabla.setRowCount(0);
    jTextField1.setText("");
}

/**
 * Se llama desde NuevoAnalisis cada vez que cambia la tarjeta elegida en
 * Tipo de Cobertura ("PARTICULAR", "OBRA_SOCIAL" o "MIXTO"). Actualiza la
 * columna "Obra Social" de todas las filas ya agregadas al nuevo valor por
 * defecto y deja la columna editable o no segun corresponda (solo Mixto
 * permite tocarla a mano).
 */
public void actualizarObraSocial(String clave) {
    coberturaActual = clave;
    javax.swing.table.DefaultTableModel modeloTabla = (javax.swing.table.DefaultTableModel) jTable1.getModel();
    String valorPorDefecto = valorObraSocialPorDefecto();
    for (int fila = 0; fila < modeloTabla.getRowCount(); fila++) {
        modeloTabla.setValueAt(valorPorDefecto, fila, 2);
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel1.setText("Seleccione los analisis solicitados");

        jButton1.setText("Agregar");

        jLabel8.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel8.setText("Codigo/Nombre");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Codigo", "Nombre", "Cobertura"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 61, Short.MAX_VALUE)
                        .addComponent(jLabel8)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 549, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(78, 78, 78))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
