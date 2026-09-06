/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas;

/**
 * Pantalla "Token de Confirmación": ingreso del código de 6 dígitos enviado
 * por correo electrónico.
 *
 * Sus componentes fueron creados en el editor visual de NetBeans a partir
 * de la pantalla CambiarContraseña, reutilizando los componentes
 * personalizados de estilo del proyecto ({@link interfaz.PanelCurvo},
 * {@link interfaz.JButtonRedondeado}, {@link interfaz.JTextFieldRedondeado}).
 *
 * Al copiarse, los seis campos para el código quedaron como campos de
 * contraseña ({@code JPasswordFieldRedondeado}), con nombres y un texto de
 * placeholder heredados de la pantalla de origen ("Ingresa tu contraseña"),
 * demasiado largo para el ancho de estas casillas. Se cambiaron a campos de
 * texto plano ({@code JTextFieldRedondeado}, ya que un código de
 * verificación no necesita ocultarse como una contraseña), se renombraron
 * como {@code txtDigito1} a {@code txtDigito6} siguiendo su orden visual de
 * izquierda a derecha, y se les asignó un placeholder corto acorde al
 * ancho de cada casilla.
 *
 * Etapa actual: maquetado visual únicamente. El botón "Verificar Codigo"
 * todavía no valida el código ingresado, ya que esta pantalla no está
 * conectada a la base de datos.
 *
 * @author agust
 */
public class TokenDeConfirmacion extends javax.swing.JPanel {

    /** Placeholder corto para cada casilla del código, acorde a su ancho reducido. */
    private static final String TEXTO_PLACEHOLDER_DIGITO = "—";

    /**
     * Se dispara cuando el usuario completa los 6 dígitos y hace click en
     * "Verificar Codigo". Quien arme esta pantalla (por ejemplo
     * {@link RecuperarContrasenaFrame}) se suscribe para validar el código
     * vía {@link controlador.PasswordController#verificarCodigo}.
     */
    public interface CodigoVerificadoListener {
        void onCodigoIngresado(String codigo);
    }

    private final java.util.List<CodigoVerificadoListener> listenersCodigo = new java.util.ArrayList<>();

    public void addCodigoVerificadoListener(CodigoVerificadoListener listener) {
        listenersCodigo.add(listener);
    }

    /**
     * Creates new form TokenDeConfirmacion
     */
    public TokenDeConfirmacion() {
        initComponents();
        aplicarPlaceholders();
        aplicarEstiloDigitos();
        aplicarFiltroDigitos();
    }

    /**
     * Asigna un placeholder corto a cada casilla del código. El placeholder
     * por defecto de {@link interfaz.JTextFieldRedondeado} ("Ingrese su
     * email") no corresponde a esta pantalla y, además, no entra en el
     * ancho reducido de estas casillas.
     */
    private void aplicarPlaceholders() {
        // El editor de formularios declara estos campos como JTextField
        // estándar (igual que el resto de los campos de esta pantalla y de
        // CambiarContraseña), por lo que setPlaceholder() -- propio de
        // JTextFieldRedondeado -- requiere este downcast.
        ((interfaz.JTextFieldRedondeado) txtDigito1).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
        ((interfaz.JTextFieldRedondeado) txtDigito2).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
        ((interfaz.JTextFieldRedondeado) txtDigito3).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
        ((interfaz.JTextFieldRedondeado) txtDigito4).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
        ((interfaz.JTextFieldRedondeado) txtDigito5).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
        ((interfaz.JTextFieldRedondeado) txtDigito6).setPlaceholder(TEXTO_PLACEHOLDER_DIGITO);
    }

    /**
     * Ajusta el padding interno y la alineación de las seis casillas del
     * código, heredados de {@link interfaz.JTextFieldRedondeado} sin
     * adaptarlos a este nuevo ancho.
     *
     * El padding por defecto del componente (18px a cada lado) está pensado
     * para campos anchos como el de email o contraseña; en una casilla de
     * apenas ~37px de ancho, ese margen no deja espacio visible para el
     * dígito ingresado, por lo que el texto tipeado quedaba prácticamente
     * invisible. Se reduce el padding horizontal y se centra el texto para
     * que cada dígito se vea completo dentro de su casilla.
     */
    private void aplicarEstiloDigitos() {
        javax.swing.border.EmptyBorder margenDigito = new javax.swing.border.EmptyBorder(10, 4, 10, 4);
        javax.swing.JTextField[] casillasDigito = {
            txtDigito1, txtDigito2, txtDigito3, txtDigito4, txtDigito5, txtDigito6
        };
        for (javax.swing.JTextField casilla : casillasDigito) {
            casilla.setBorder(margenDigito);
            casilla.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        }
    }

    /**
     * Le pone a cada casilla del código un {@link javax.swing.text.DocumentFilter}
     * que solo deja escribir un dígito numérico (nada de letras, símbolos ni
     * más de un carácter), que pasa el foco a la siguiente casilla apenas se
     * completa una, y que vuelve a la anterior cuando se borra el dígito con
     * Backspace/Delete — así se puede cargar y corregir el código corrido,
     * sin tocar el mouse ni el Tab.
     *
     * El filtro de documento por sí solo no alcanza para una casilla que YA
     * está vacía: ahí Backspace no dispara ningún cambio en el documento
     * (no hay nada que borrar), así que además se engancha un
     * {@link java.awt.event.KeyListener} en cada casilla para ese caso
     * puntual: Backspace sobre una casilla vacía pasa el foco para atrás
     * igual, sin que haga falta mover el cursor a mano primero.
     */
    private void aplicarFiltroDigitos() {
        javax.swing.JTextField[] casillasDigito = {
            txtDigito1, txtDigito2, txtDigito3, txtDigito4, txtDigito5, txtDigito6
        };
        for (int i = 0; i < casillasDigito.length; i++) {
            final javax.swing.JTextField casillaActual = casillasDigito[i];
            final javax.swing.JTextField anteriorCasilla = (i - 1 >= 0) ? casillasDigito[i - 1] : null;
            javax.swing.JTextField siguienteCasilla = (i + 1 < casillasDigito.length) ? casillasDigito[i + 1] : null;

            ((javax.swing.text.AbstractDocument) casillaActual.getDocument())
                    .setDocumentFilter(new FiltroUnDigito(anteriorCasilla, siguienteCasilla));

            casillaActual.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent evt) {
                    if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                            && casillaActual.getText().isEmpty() && anteriorCasilla != null) {
                        anteriorCasilla.requestFocusInWindow();
                        anteriorCasilla.setCaretPosition(anteriorCasilla.getText().length());
                    }
                }
            });
        }
    }

    /**
     * Filtro de documento para una casilla de código: descarta cualquier
     * carácter que no sea un dígito (0-9), deja como máximo un dígito
     * cargado a la vez (si ya había uno, el nuevo lo reemplaza), pasa el
     * foco a la casilla siguiente al completarse, y a la anterior cuando el
     * usuario borra el dígito y la casilla queda vacía de nuevo.
     */
    private static class FiltroUnDigito extends javax.swing.text.DocumentFilter {

        /** Casilla a la que se vuelve al borrar el dígito de esta, o null si es la primera. */
        private final javax.swing.JTextField anteriorCasilla;
        /** Casilla a la que se pasa el foco al completar esta, o null si es la última. */
        private final javax.swing.JTextField siguienteCasilla;

        FiltroUnDigito(javax.swing.JTextField anteriorCasilla, javax.swing.JTextField siguienteCasilla) {
            this.anteriorCasilla = anteriorCasilla;
            this.siguienteCasilla = siguienteCasilla;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, javax.swing.text.AttributeSet atributos)
                throws javax.swing.text.BadLocationException {
            reemplazarPorDigito(fb, texto, atributos);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String texto, javax.swing.text.AttributeSet atributos)
                throws javax.swing.text.BadLocationException {
            reemplazarPorDigito(fb, texto, atributos);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws javax.swing.text.BadLocationException {
            super.remove(fb, offset, length);
            // Backspace/Delete dejaron la casilla vacía: seguimos corrigiendo hacia atrás.
            if (fb.getDocument().getLength() == 0 && anteriorCasilla != null) {
                anteriorCasilla.requestFocusInWindow();
                // Sin esto el cursor queda al INICIO de la casilla anterior (posición 0),
                // y un Backspace ahí no borra nada por no haber texto antes del cursor:
                // el usuario tenía que apretar Backspace dos veces por casilla para
                // seguir borrando hacia atrás. Lo mandamos al final del dígito para que
                // el siguiente Backspace sí lo borre y siga encadenando hacia atrás.
                anteriorCasilla.setCaretPosition(anteriorCasilla.getText().length());
            }
        }

        private void reemplazarPorDigito(FilterBypass fb, String texto, javax.swing.text.AttributeSet atributos)
                throws javax.swing.text.BadLocationException {
            String soloDigitos = texto == null ? "" : texto.replaceAll("[^0-9]", "");
            if (soloDigitos.isEmpty()) {
                // No había ningún dígito en lo tipeado/pegado (letras, símbolos, etc.): se ignora.
                return;
            }
            // La casilla es de un solo dígito: si se pega texto más largo, se queda con el último.
            String digito = soloDigitos.substring(soloDigitos.length() - 1);
            super.replace(fb, 0, fb.getDocument().getLength(), digito, atributos);
            if (siguienteCasilla != null) {
                siguienteCasilla.requestFocusInWindow();
            }
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

        jPanel2 = new javax.swing.JPanel();
        jPanel1 =  new interfaz.PanelCurvo();
        txtDigito3 = new interfaz.JTextFieldRedondeado();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new interfaz.JButtonRedondeado();
        jLabel5 = new javax.swing.JLabel();
        txtDigito4 = new interfaz.JTextFieldRedondeado();
        txtDigito2 = new interfaz.JTextFieldRedondeado();
        txtDigito1 = new interfaz.JTextFieldRedondeado();
        txtDigito6 = new interfaz.JTextFieldRedondeado();
        txtDigito5 = new interfaz.JTextFieldRedondeado();

        setLayout(new java.awt.GridLayout(1, 0));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 354, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 510, Short.MAX_VALUE)
        );

        add(jPanel2);

        jPanel1.setBackground(new java.awt.Color(0, 102, 51));

        txtDigito3.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito3.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 22)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Revisa tu email");

        jLabel4.setBackground(new java.awt.Color(140, 165, 150));
        jLabel4.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(180, 195, 190));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Te enviamos un código de 6 dígitos a");

        jButton1.setText("Verificar Codigo");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel5.setBackground(new java.awt.Color(140, 165, 150));
        jLabel5.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(180, 195, 190));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("tu dirección de correo registrada");

        txtDigito4.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito4.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        txtDigito2.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito2.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        txtDigito1.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito1.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        txtDigito6.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito6.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        txtDigito5.setBackground(new java.awt.Color(180, 195, 185));
        txtDigito5.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(txtDigito1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDigito2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDigito3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtDigito4, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDigito5, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtDigito6, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8)))))
                .addGap(52, 52, 52))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtDigito6, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDigito5, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDigito4, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDigito3, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDigito2, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtDigito1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(180, Short.MAX_VALUE))
        );

        add(jPanel1);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Concatena los 6 dígitos en el orden visual (izquierda a derecha).
        // getText() acá trae solo lo tipeado: el placeholder ("—") es un
        // dibujo aparte en paintComponent(), nunca queda en el valor real
        // del campo (ver interfaz.JTextFieldRedondeado).
        String codigo = txtDigito1.getText().trim() + txtDigito2.getText().trim()
                + txtDigito3.getText().trim() + txtDigito4.getText().trim()
                + txtDigito5.getText().trim() + txtDigito6.getText().trim();

        if (codigo.length() != 6) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Completá los 6 dígitos del código.",
                    "Código incompleto", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (CodigoVerificadoListener listener : listenersCodigo) {
            listener.onCodigoIngresado(codigo);
        }
    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtDigito1;
    private javax.swing.JTextField txtDigito2;
    private javax.swing.JTextField txtDigito3;
    private javax.swing.JTextField txtDigito4;
    private javax.swing.JTextField txtDigito5;
    private javax.swing.JTextField txtDigito6;
    // End of variables declaration//GEN-END:variables
}
