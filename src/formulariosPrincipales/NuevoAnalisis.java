package formulariosPrincipales;

/**
 * Pantalla de tipo asistente ("wizard") para generar una nueva orden de
 * análisis: combina los datos personales del paciente
 * ({@code datosPersonales2}), el tipo de cobertura ({@code tipoCobertura1})
 * y la selección de prestaciones del nomenclador
 * ({@code solicitudAnalisis1}), y delega en
 * {@link controlador.NuevoAnalisisController#generarOrden} el guardado
 * transaccional del paciente, el pedido y sus análisis.
 */
public class NuevoAnalisis extends javax.swing.JPanel {

    /**
     * Arma la pantalla: configura el {@code JScrollPane} que envuelve el
     * formulario (ver comentarios 1 a 4 más abajo, sobre por qué el ancho
     * de {@code panelBorder1} se ajusta a mano en vez de dejarlo fijo) y
     * conecta el listener de {@code tipoCobertura1} para que la columna
     * "Obra Social" de la tabla de {@code solicitudAnalisis1} se actualice
     * sola al cambiar la cobertura elegida (ver comentario 5).
     */
    public NuevoAnalisis() {
        initComponents();
        
        // 1. Configurar propiedades estéticas y comportamiento del JScrollPane
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // Antes estaba en NEVER: en pantallas mas chicas que el ancho de
        // diseño (~1300px) los campos de la derecha (Fecha de Nacimiento,
        // Celular, Mixto, etc.) quedaban tapados sin ninguna forma de
        // llegar a verlos. Con AS_NEEDED aparece la barra horizontal solo
        // cuando hace falta (pantallas chicas), y no molesta en las
        // pantallas donde ya entraba todo.
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPane1.setBorder(null);
        jScrollPane1.getViewport().setOpaque(false);

        // 2. Definir una altura total holgada (ej. 1150px para dar espacio al final)
        int altoDeseado = 1150;

        // 2.1 Ancho mínimo real que necesita el contenido (datosPersonales2 +
        // tipoCobertura1 + solicitudAnalisis1 con sus medidas de diseño), tal
        // como lo calcula el GroupLayout de panelBorder1 ANTES de que el
        // listener de más abajo empiece a forzarle otro ancho. Si en algún
        // momento se lo achica por debajo de esto, el scroll horizontal
        // nunca aparece (el JScrollPane cree que entra todo) y los campos de
        // la derecha quedan tapados sin forma de llegar a ellos.
        final int anchoMinimoContenido = panelBorder1.getPreferredSize().width;

        // 3. Listener para ajustar el ancho al JScrollPane dinámicamente: en
        // pantallas grandes estira panelBorder1 para que ocupe todo el
        // viewport (se ve mejor centrado/ancho); en pantallas chicas nunca
        // lo achica por debajo de anchoMinimoContenido, así el JScrollPane
        // detecta el sobrante y muestra la barra horizontal en vez de
        // recortar el contenido.
        jScrollPane1.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int anchoDisponible = jScrollPane1.getViewport().getWidth();
                int anchoFinal = Math.max(anchoDisponible, anchoMinimoContenido);
                panelBorder1.setPreferredSize(new java.awt.Dimension(anchoFinal, altoDeseado));
                panelBorder1.revalidate();
            }
        });

        // 4. Forzar el tamaño inicial apenas abre la pantalla
        javax.swing.SwingUtilities.invokeLater(() -> {
            int anchoInicial = jScrollPane1.getViewport().getWidth();
            if (anchoInicial > 0) {
                int anchoFinal = Math.max(anchoInicial, anchoMinimoContenido);
                panelBorder1.setPreferredSize(new java.awt.Dimension(anchoFinal, altoDeseado));
                panelBorder1.revalidate();
            }
        });

        // 5. Cuando cambia la tarjeta elegida en Tipo de Cobertura, la
        // columna "Obra Social" de la tabla de Solicitud de Análisis se
        // actualiza sola (Particular -> No fijo, Obra Social -> Si fijo,
        // Mixto -> Si por defecto pero editable).
        tipoCobertura1.addCoberturaListener(new nuevoAnalisis.TipoCobertura.CoberturaListener() {
            @Override
            public void onCoberturaCambiada(String clave) {
                solicitudAnalisis1.actualizarObraSocial(clave);
            }
        });
    }

    /**
     * Junta los nombres de todos los campos obligatorios que falten completar
     * (Medico Derivante y Observacion quedan afuera porque están marcados
     * como Opcional en la pantalla). Mientras el DNI no tenga los 8 dígitos
     * el resto de los datos del paciente sigue bloqueado y vacío, así que en
     * ese caso solo se pide completar el DNI.
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
     * Guarda el paciente (nuevo o actualizado), el pedido y sus análisis en
     * la base de datos, todo en una sola transacción: si algo falla a mitad
     * de camino se deshace todo (rollback) para no dejar un pedido "a
     * medias". Al terminar bien, Registros y Escritorio lo van a mostrar
     * solos la próxima vez que se abran/actualicen (ya listan por
     * created_at DESC), y en el historial del paciente también.
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
     * Vacía las tres secciones de la pantalla (Datos del Paciente, Tipo de
     * Cobertura vuelve a Particular, y la tabla de análisis elegidos), para
     * dejarla lista para cargar la próxima orden. Se usa tanto después de
     * generar una orden con éxito como al apretar "Cancelar".
     */
    private void limpiarPantallaCompleta() {
        datosPersonales2.limpiarFormulario();
        solicitudAnalisis1.limpiarSeleccion();
        tipoCobertura1.seleccionarCobertura("PARTICULAR");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        panelBorder1 = new panels.PanelBorder();
        datosPersonales2 = new nuevoAnalisis.DatosPersonales();
        tipoCobertura1 = new nuevoAnalisis.TipoCobertura();
        solicitudAnalisis1 = new nuevoAnalisis.SolicitudAnalisis();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBorder1Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(tipoCobertura1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(solicitudAnalisis1, javax.swing.GroupLayout.DEFAULT_SIZE, 1192, Short.MAX_VALUE)
                    .addComponent(datosPersonales2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(11, 11, 11))
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(datosPersonales2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tipoCobertura1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(solicitudAnalisis1, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE)
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1214, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Manejador de "Generar Orden": valida que estén los datos
     * obligatorios antes de intentar guardar nada; si falta algo, corta
     * acá y le muestra al usuario la lista completa de lo que falta en un
     * solo cartel, en vez de generar la orden con datos incompletos.
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
     * Manejador de "Cancelar": pide confirmación y, si el usuario acepta,
     * vacía todo el formulario mediante {@link #limpiarPantallaCompleta()}.
     */
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        int respuesta = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Descartar los datos cargados y vaciar el formulario?",
                "Cancelar", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (respuesta == javax.swing.JOptionPane.YES_OPTION) {
            limpiarPantallaCompleta();
        }
    }//GEN-LAST:event_jButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private nuevoAnalisis.DatosPersonales datosPersonales2;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private panels.PanelBorder panelBorder1;
    private nuevoAnalisis.SolicitudAnalisis solicitudAnalisis1;
    private nuevoAnalisis.TipoCobertura tipoCobertura1;
    // End of variables declaration//GEN-END:variables
}
