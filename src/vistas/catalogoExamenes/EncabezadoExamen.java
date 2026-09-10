/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas.catalogoExamenes;

/**
 * Encabezado del formulario "Nuevo Examen": Código, Nombre y Unidad Bioquímica.
 *
 * @author agust
 */
public class EncabezadoExamen extends javax.swing.JPanel {

    /**
     * Creates new form EncabezadoExamen
     */
    public EncabezadoExamen() {
        initComponents();

        String estiloCampo = "arc: 8;";
        dni.putClientProperty("FlatLaf.style", estiloCampo);
        ayn1.putClientProperty("FlatLaf.style", estiloCampo);
        ayn.putClientProperty("FlatLaf.style", estiloCampo);

        dni.putClientProperty("JTextField.placeholderText", "Ej: 1001");
        ayn1.putClientProperty("JTextField.placeholderText", "Ej: Hemograma Completo");
        ayn.putClientProperty("JTextField.placeholderText", "Ej: 1.5");

        // Código: solo dígitos. Nombre: solo texto. Unidad Bioquímica: decimal.
        ((javax.swing.text.AbstractDocument) dni.getDocument())
                .setDocumentFilter(new vistas.panels.FiltroSoloDigitos(9));
        ((javax.swing.text.AbstractDocument) ayn1.getDocument())
                .setDocumentFilter(new vistas.panels.FiltroSoloLetras());
        ((javax.swing.text.AbstractDocument) ayn.getDocument())
                .setDocumentFilter(new vistas.panels.FiltroDecimal());

        // Validación en vivo (mismo criterio que CrearNuevoExamen: borde verde/rojo según
        // ValidadorCatalogoExamenes), para que "+ Crear Examen" también exija estos tres campos
        // completos y válidos, no solo los de la parte de abajo.
        engancharValidacionEnVivo(dni, () -> ValidadorCatalogoExamenes.esCodigoValido(dni.getText()));
        engancharValidacionEnVivo(ayn1, () -> ValidadorCatalogoExamenes.esNombreValido(ayn1.getText()));
        engancharValidacionEnVivo(ayn, () -> ValidadorCatalogoExamenes.esDecimalValido(ayn.getText()));
    }

    // Mismos colores de borde que CrearNuevoExamen.BORDE_CAMPO_OK/BORDE_CAMPO_ERROR; se
    // duplican acá porque EncabezadoExamen es un panel independiente y esos campos son privados.
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_CAMPO_OK = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(4, 6, 4, 6), new java.awt.Color(0xCC, 0xD3, 0xD1), 1, 8);
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_CAMPO_ERROR = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(4, 6, 4, 6), new java.awt.Color(0xD9, 0x4C, 0x4C), 2, 8);

    /**
     * Engancha un validador en vivo sobre "campo": repinta su borde en verde/rojo con cada tecla,
     * según lo que diga "condicion" (mismo criterio que
     * CrearNuevoExamen.engancharValidacionEnVivo).
     */
    private void engancharValidacionEnVivo(javax.swing.JTextField campo, java.util.function.BooleanSupplier condicion) {
        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                repintar();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                repintar();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                repintar();
            }

            private void repintar() {
                campo.setBorder(condicion.getAsBoolean() ? BORDE_CAMPO_OK : BORDE_CAMPO_ERROR);
            }
        };
        campo.getDocument().addDocumentListener(listener);
    }

    /**
     * Revalida y repinta los tres campos del encabezado (se llama al presionar "+ Crear Examen",
     * así quedan marcados en rojo también si están vacíos). Devuelve true solo si los tres son
     * válidos.
     */
    public boolean marcarYValidar() {
        boolean okCodigo = ValidadorCatalogoExamenes.esCodigoValido(getCodigo());
        boolean okNombre = ValidadorCatalogoExamenes.esNombreValido(getNombreExamen());
        boolean okUnidad = ValidadorCatalogoExamenes.esDecimalValido(getUnidadBioquimica());
        dni.setBorder(okCodigo ? BORDE_CAMPO_OK : BORDE_CAMPO_ERROR);
        ayn1.setBorder(okNombre ? BORDE_CAMPO_OK : BORDE_CAMPO_ERROR);
        ayn.setBorder(okUnidad ? BORDE_CAMPO_OK : BORDE_CAMPO_ERROR);
        return okCodigo && okNombre && okUnidad;
    }

    public String getCodigo() {
        return dni.getText().trim();
    }

    public String getNombreExamen() {
        return ayn1.getText().trim();
    }

    public String getUnidadBioquimica() {
        return ayn.getText().trim();
    }

    public void limpiarFormulario() {
        dni.setText("");
        ayn1.setText("");
        ayn.setText("");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel3 = new javax.swing.JLabel();
        dni = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        ayn = new javax.swing.JTextField();
        ayn1 = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel3.setText("Codigo");

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel9.setText("Nombre");

        jLabel10.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel10.setText("Unidad Bioquimica");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(0, 154, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ayn1, javax.swing.GroupLayout.PREFERRED_SIZE, 327, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 172, Short.MAX_VALUE)
                        .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(58, 58, 58))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ayn1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(34, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField ayn;
    private javax.swing.JTextField ayn1;
    private javax.swing.JTextField dni;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel9;
    // End of variables declaration//GEN-END:variables
}
