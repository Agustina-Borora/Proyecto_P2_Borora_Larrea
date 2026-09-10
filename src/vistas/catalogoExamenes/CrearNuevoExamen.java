/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas.catalogoExamenes;

/**
 * Formulario "Nuevo Examen" del Catálogo de Exámenes: encabezado (código, nombre, unidad
 * bioquímica), selección del tipo de examen (Numérico / Cualitativo / Texto Libre) y la tabla de
 * analitos y rangos de referencia para el tipo Numérico.
 *
 * Incluye validación de campos:
 *  - Código / Nombre / Unidad Bioquímica (encabezado): ver EncabezadoExamen.marcarYValidar().
 *  - Nombre (analito / parámetro / sección): solo letras y espacios.
 *  - Valor referencial (Numérico): [M|H opcional] + dígitos/puntos/guiones + [% opcional].
 *  - Unidad de medida (Numérico): letras y "/", opcional un dígito final, máx. 6 caracteres.
 *
 * @author agust
 */
public class CrearNuevoExamen extends javax.swing.JPanel {

    /**
     * Creates new form CrearNuevoExamen
     */
    public CrearNuevoExamen() {
        initComponents();

        registrarSeleccionTipo(jPanel3, "NUMERICO", jLabel4, jLabel3);
        registrarSeleccionTipo(jPanel4, "CUALITATIVO", jLabel6, jLabel7);
        registrarSeleccionTipo(jPanel5, "TEXTOLIBRE", jLabel9, jLabel10, jLabel11);

        construirEditorAnalitos();
        seleccionarTipo("NUMERICO");

        jButton3.setText("+ Crear Examen");
        jButton3.addActionListener(evt -> {
            if (!validarFormularioCompleto()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Revisá los campos marcados en rojo.", "Datos inválidos",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Acá continúa la lógica de guardado (persistencia pendiente).
        });
    }

    // --- Selección de tarjeta "Tipo de examen" (mismo criterio que TipoCobertura en Nuevo Análisis) ---

    private final java.util.Map<String, javax.swing.JPanel> tarjetasTipo = new java.util.LinkedHashMap<>();
    private String tipoSeleccionado;

    private static final java.awt.Color FONDO_NORMAL = new java.awt.Color(0xF8, 0xFA, 0xFC);
    private static final java.awt.Color FONDO_SELECCIONADO = new java.awt.Color(0xE3, 0xF2, 0xE9);
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_NORMAL = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(10, 10, 10, 10), new java.awt.Color(0xEE, 0xF2, 0xF6), 1, 8);
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_SELECCIONADO = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(10, 10, 10, 10), new java.awt.Color(0x1E, 0x51, 0x3B), 2, 8);

    // Bordes usados para marcar validez/invalidez en los JTextField de las filas dinámicas
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_CAMPO_OK = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(4, 6, 4, 6), new java.awt.Color(0xCC, 0xD3, 0xD1), 1, 8);
    private static final com.formdev.flatlaf.ui.FlatLineBorder BORDE_CAMPO_ERROR = new com.formdev.flatlaf.ui.FlatLineBorder(
            new java.awt.Insets(4, 6, 4, 6), new java.awt.Color(0xD9, 0x4C, 0x4C), 2, 8);

    /**
     * Registra una tarjeta de tipo de examen: guarda la referencia (para resaltarla/des-resaltarla
     * después) y engancha el mismo click tanto en el panel como en sus etiquetas hijas.
     */
    private void registrarSeleccionTipo(javax.swing.JPanel tarjeta, String clave, javax.swing.JComponent... hijos) {
        tarjetasTipo.put(clave, tarjeta);
        java.awt.event.MouseAdapter clickHandler = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                seleccionarTipo(clave);
            }
        };
        tarjeta.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        tarjeta.addMouseListener(clickHandler);
        for (javax.swing.JComponent hijo : hijos) {
            hijo.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            hijo.addMouseListener(clickHandler);
        }
    }

    /**
     * Marca "clave" como el tipo de examen elegido y resalta solo esa tarjeta.
     * Público por si el "Agregar" necesita saber en un futuro con qué tipo de dato viene la fila.
     */
    public void seleccionarTipo(String clave) {
        if (!tarjetasTipo.containsKey(clave)) {
            return;
        }
        tipoSeleccionado = clave;
        for (java.util.Map.Entry<String, javax.swing.JPanel> entrada : tarjetasTipo.entrySet()) {
            boolean esElegida = entrada.getKey().equals(clave);
            javax.swing.JPanel tarjeta = entrada.getValue();
            tarjeta.setBackground(esElegida ? FONDO_SELECCIONADO : FONDO_NORMAL);
            tarjeta.setBorder(esElegida ? BORDE_SELECCIONADO : BORDE_NORMAL);
            tarjeta.repaint();
        }
        if (cardLayoutTipo != null) {
            cardLayoutTipo.show(panelCentroTipo, clave);
        }
        actualizarBotonAgregar();
    }

    /**
     * "NUMERICO", "CUALITATIVO" o "TEXTOLIBRE" (nunca null: Numérico es el default).
     */
    public String getTipoSeleccionado() {
        return tipoSeleccionado;
    }

    // --- Editor de "la parte de abajo": cambia de contenido según el tipo de examen elegido
    // (Numérico = analitos y rangos, Cualitativo = parámetros con opciones de resultado,
    // Texto Libre = secciones del informe), más "Estado del examen". jPanel6 queda reservado y
    // vacío en el editor visual; todo esto se arma en código porque todavía no persiste en la
    // base -- ver charla pendiente sobre el esquema (código/prestaciones, valores_referencia). ---

    private java.awt.CardLayout cardLayoutTipo;
    private javax.swing.JPanel panelCentroTipo;
    private javax.swing.JPanel listaNumerico;
    private javax.swing.JPanel listaCualitativo;
    private javax.swing.JPanel listaTextoLibre;
    private final java.util.Map<String, javax.swing.JButton> botonesEstado = new java.util.LinkedHashMap<>();
    private String estadoSeleccionado = "ACTIVO";

    private static final String[] OPCIONES_CUALITATIVO_DEFECTO = {"Negativo", "Positivo"};

    private void construirEditorAnalitos() {
        listaNumerico = crearListaVertical();
        listaCualitativo = crearListaVertical();
        listaTextoLibre = crearListaVertical();

        panelCentroTipo = new javax.swing.JPanel(new java.awt.CardLayout());
        panelCentroTipo.setBackground(java.awt.Color.WHITE);
        panelCentroTipo.add(construirTarjetaNumerico(), "NUMERICO");
        panelCentroTipo.add(new javax.swing.JScrollPane(listaCualitativo), "CUALITATIVO");
        panelCentroTipo.add(new javax.swing.JScrollPane(listaTextoLibre), "TEXTOLIBRE");
        cardLayoutTipo = (java.awt.CardLayout) panelCentroTipo.getLayout();

        jPanel6.setLayout(new java.awt.BorderLayout(0, 10));
        jPanel6.add(panelCentroTipo, java.awt.BorderLayout.CENTER);
        jPanel6.add(construirPanelEstado(), java.awt.BorderLayout.SOUTH);

        // Arranca vacío (nunca 0 filas): usamos una fila de ejemplo por tipo, como en el diseño,
        // así el panel siempre tiene contenido real y un alto "preferido" razonable.
        agregarFilaNumerico();
        agregarFilaCualitativo();
        agregarFilaTextoLibre();

        // El GroupLayout generado usa el alto "preferido" de jPanel6 como mínimo Y máximo (ver
        // initComponents): sin este valor fijo el panel queda con el alto que tenía en el
        // instante del pack() del diálogo y no se ve el espacio para agregar más filas.
        jPanel6.setPreferredSize(new java.awt.Dimension(0, 320));

        jButton1.addActionListener(evt -> {
            switch (tipoSeleccionado) {
                case "CUALITATIVO":
                    agregarFilaCualitativo();
                    break;
                case "TEXTOLIBRE":
                    agregarFilaTextoLibre();
                    break;
                default:
                    agregarFilaNumerico();
            }
        });
    }

    private void actualizarBotonAgregar() {
        if (jButton1 == null || tipoSeleccionado == null) {
            return;
        }
        switch (tipoSeleccionado) {
            case "CUALITATIVO":
                jButton1.setText("+ Agregar parámetro");
                jLabel12.setText("Parámetros cualitativos");
                break;
            case "TEXTOLIBRE":
                jButton1.setText("+ Agregar sección");
                jLabel12.setText("Secciones del informe");
                break;
            default:
                jButton1.setText("+ Agregar fila");
                jLabel12.setText("Analitos y Rangos de Referencia");
        }
    }

    private javax.swing.JPanel crearListaVertical() {
        javax.swing.JPanel lista = new javax.swing.JPanel();
        lista.setBackground(java.awt.Color.WHITE);
        lista.setLayout(new javax.swing.BoxLayout(lista, javax.swing.BoxLayout.Y_AXIS));
        return lista;
    }

    private javax.swing.JTextField campoConPlaceholder(String placeholder) {
        javax.swing.JTextField campo = new javax.swing.JTextField();
        campo.putClientProperty("JTextField.placeholderText", placeholder);
        campo.putClientProperty("FlatLaf.style", "arc: 8;");
        return campo;
    }

    private javax.swing.JButton botonEliminarFila(javax.swing.JPanel lista, javax.swing.JPanel fila) {
        javax.swing.JButton boton = new javax.swing.JButton("✕");
        boton.setForeground(new java.awt.Color(0xD9, 0x4C, 0x4C));
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        boton.addActionListener(evt -> eliminarFila(lista, fila));
        return boton;
    }

    private void eliminarFila(javax.swing.JPanel lista, javax.swing.JPanel fila) {
        lista.remove(fila);
        if (lista == listaTextoLibre) {
            renumerarSecciones();
        }
        lista.revalidate();
        lista.repaint();
    }

    // --- Validación de campos: helpers reutilizables ---

    /**
     * Marca el borde de "campo" en verde/neutro si es válido, o en rojo si no lo es.
     */
    private void marcarValidez(javax.swing.JTextField campo, boolean valido) {
        campo.setBorder(valido ? BORDE_CAMPO_OK : BORDE_CAMPO_ERROR);
    }

    /**
     * Engancha un validador en vivo (se dispara con cada tecla) sobre uno o más campos.
     * "accion" corre la validación real y pinta los bordes correspondientes.
     */
    private void engancharValidacionEnVivo(Runnable accion, javax.swing.JTextField... campos) {
        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                accion.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                accion.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                accion.run();
            }
        };
        for (javax.swing.JTextField campo : campos) {
            campo.getDocument().addDocumentListener(listener);
        }
    }

    // --- Numérico: Nombre / Unidad / Valor referencial ---

    /**
     * Envuelve la lista de filas de Numérico con el encabezado de columnas (Nombre / Unidad /
     * Valor referencial), que en Cualitativo y Texto Libre no hace falta porque cada fila ya
     * trae sus propias etiquetas ("VR:", "Opciones:", el número de la sección).
     */
    private javax.swing.JPanel construirTarjetaNumerico() {
        javax.swing.JPanel tarjeta = new javax.swing.JPanel(new java.awt.BorderLayout());
        tarjeta.setBackground(java.awt.Color.WHITE);
        tarjeta.add(construirEncabezadoColumnasNumerico(), java.awt.BorderLayout.NORTH);
        tarjeta.add(new javax.swing.JScrollPane(listaNumerico), java.awt.BorderLayout.CENTER);
        return tarjeta;
    }

    private javax.swing.JPanel construirEncabezadoColumnasNumerico() {
        javax.swing.JPanel encabezado = new javax.swing.JPanel(new java.awt.GridBagLayout());
        encabezado.setBackground(java.awt.Color.WHITE);
        encabezado.setBorder(new javax.swing.border.EmptyBorder(0, 0, 6, 0));

        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(0, 0, 0, 8);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 0.45;
        encabezado.add(etiquetaColumna("Nombre"), c);
        c.gridx = 1;
        c.weightx = 0.20;
        encabezado.add(etiquetaColumna("Unidad"), c);
        c.gridx = 2;
        c.weightx = 0.35;
        encabezado.add(etiquetaColumna("Valor referencial"), c);
        c.gridx = 3;
        c.weightx = 0;
        c.insets = new java.awt.Insets(0, 0, 0, 0);
        encabezado.add(javax.swing.Box.createHorizontalStrut(28), c);
        return encabezado;
    }

    private javax.swing.JLabel etiquetaColumna(String texto) {
        javax.swing.JLabel etiqueta = new javax.swing.JLabel(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        etiqueta.setForeground(new java.awt.Color(0x6E, 0x78, 0x74));
        return etiqueta;
    }

    private void agregarFilaNumerico() {
        javax.swing.JTextField nombre = campoConPlaceholder("Ej: Hemoglobina");
        javax.swing.JTextField unidad = campoConPlaceholder("g/dL");
        javax.swing.JTextField valorReferencia = campoConPlaceholder("Ej: 12 - 14");

        // Validación en vivo: cada campo se pinta apenas se tipea algo inválido.
        engancharValidacionEnVivo(
                () -> marcarValidez(nombre, ValidadorCatalogoExamenes.esNombreValido(nombre.getText())),
                nombre);
        engancharValidacionEnVivo(
                () -> marcarValidez(unidad, ValidadorCatalogoExamenes.esUnidadValida(unidad.getText())),
                unidad);
        engancharValidacionEnVivo(
                () -> marcarValidez(valorReferencia,
                        ValidadorCatalogoExamenes.esValorReferenciaValido(valorReferencia.getText())),
                valorReferencia);

        javax.swing.JPanel fila = new javax.swing.JPanel(new java.awt.GridBagLayout());
        fila.setBackground(java.awt.Color.WHITE);
        fila.setBorder(new javax.swing.border.EmptyBorder(4, 0, 10, 0));

        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(0, 0, 0, 8);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.weightx = 0.45;
        fila.add(nombre, c);
        c.gridx = 1;
        c.weightx = 0.20;
        fila.add(unidad, c);
        c.gridx = 2;
        c.weightx = 0.35;
        fila.add(valorReferencia, c);
        c.gridx = 3;
        c.weightx = 0;
        c.insets = new java.awt.Insets(0, 0, 0, 0);
        fila.add(botonEliminarFila(listaNumerico, fila), c);

        listaNumerico.add(fila);
        listaNumerico.revalidate();
        listaNumerico.repaint();
    }

    // --- Cualitativo: Nombre del parámetro + VR (elegido entre las Opciones) + Opciones ---

    private void agregarFilaCualitativo() {
        javax.swing.JTextField nombre = campoConPlaceholder("Nombre del parámetro");

        // Validación en vivo: solo letras.
        engancharValidacionEnVivo(
                () -> marcarValidez(nombre, ValidadorCatalogoExamenes.esTextoSoloLetras(nombre.getText())),
                nombre);

        javax.swing.DefaultComboBoxModel<String> modeloVr = new javax.swing.DefaultComboBoxModel<>();
        javax.swing.JComboBox<String> comboVr = new javax.swing.JComboBox<>(modeloVr);
        comboVr.putClientProperty("FlatLaf.style", "arc: 8;");

        javax.swing.JPanel chips = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 2));
        chips.setOpaque(false);

        java.util.List<String> opciones = new java.util.ArrayList<>();

        javax.swing.JButton nueva = new javax.swing.JButton("+ Nueva...");
        nueva.setContentAreaFilled(false);
        nueva.setBorderPainted(false);
        nueva.setFocusPainted(false);
        nueva.setForeground(new java.awt.Color(0x1E, 0x51, 0x3B));
        nueva.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        nueva.addActionListener(evt -> {
            String texto = javax.swing.JOptionPane.showInputDialog(this, "Nueva opción de resultado:");
            if (texto != null && !texto.trim().isEmpty() && !opciones.contains(texto.trim())) {
                agregarOpcionCualitativa(opciones, chips, modeloVr, nueva, texto.trim());
            }
        });

        javax.swing.JPanel filaSuperior = new javax.swing.JPanel(new java.awt.GridBagLayout());
        filaSuperior.setBackground(java.awt.Color.WHITE);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(0, 0, 0, 8);
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 0.55;
        filaSuperior.add(nombre, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = java.awt.GridBagConstraints.NONE;
        filaSuperior.add(new javax.swing.JLabel("VR:"), c);
        c.gridx = 2;
        c.weightx = 0.25;
        c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        filaSuperior.add(comboVr, c);

        javax.swing.JPanel fila = new javax.swing.JPanel();
        fila.setLayout(new javax.swing.BoxLayout(fila, javax.swing.BoxLayout.Y_AXIS));
        fila.setBackground(java.awt.Color.WHITE);
        fila.setBorder(new javax.swing.border.EmptyBorder(6, 0, 14, 0));

        c.gridx = 3;
        c.weightx = 0;
        c.fill = java.awt.GridBagConstraints.NONE;
        c.insets = new java.awt.Insets(0, 0, 0, 0);
        filaSuperior.add(botonEliminarFila(listaCualitativo, fila), c);

        javax.swing.JPanel filaOpciones = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
        filaOpciones.setBackground(java.awt.Color.WHITE);
        filaOpciones.add(new javax.swing.JLabel("Opciones:"), java.awt.BorderLayout.WEST);
        chips.add(nueva);
        filaOpciones.add(chips, java.awt.BorderLayout.CENTER);

        fila.add(filaSuperior);
        fila.add(javax.swing.Box.createVerticalStrut(4));
        fila.add(filaOpciones);

        for (String opcion : OPCIONES_CUALITATIVO_DEFECTO) {
            agregarOpcionCualitativa(opciones, chips, modeloVr, nueva, opcion);
        }
        if (modeloVr.getSize() > 0) {
            comboVr.setSelectedIndex(0);
        }

        listaCualitativo.add(fila);
        listaCualitativo.revalidate();
        listaCualitativo.repaint();
    }

    /**
     * Agrega "opcion" como una opción de resultado más (chip con su "✕" para sacarla y entrada en
     * el combo VR), manteniendo el botón "+ Nueva..." siempre al final de la fila de chips.
     */
    private void agregarOpcionCualitativa(java.util.List<String> opciones, javax.swing.JPanel chips,
            javax.swing.DefaultComboBoxModel<String> modeloVr, javax.swing.JButton botonNueva, String opcion) {
        opciones.add(opcion);
        modeloVr.addElement(opcion);

        javax.swing.JPanel chip = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        chip.setBackground(FONDO_SELECCIONADO);
        chip.putClientProperty("FlatLaf.style", "arc: 20;");

        javax.swing.JLabel texto = new javax.swing.JLabel(opcion);
        texto.setForeground(new java.awt.Color(0x1E, 0x51, 0x3B));

        javax.swing.JButton quitar = new javax.swing.JButton("✕");
        quitar.setContentAreaFilled(false);
        quitar.setBorderPainted(false);
        quitar.setFocusPainted(false);
        quitar.setMargin(new java.awt.Insets(0, 2, 0, 0));
        quitar.setForeground(new java.awt.Color(0x1E, 0x51, 0x3B));
        quitar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        quitar.addActionListener(evt -> {
            opciones.remove(opcion);
            modeloVr.removeElement(opcion);
            chips.remove(chip);
            chips.revalidate();
            chips.repaint();
        });

        chip.add(texto);
        chip.add(quitar);

        int posicionNueva = chips.getComponentZOrder(botonNueva);
        if (posicionNueva >= 0) {
            chips.add(chip, posicionNueva);
        } else {
            chips.add(chip);
        }
        chips.revalidate();
        chips.repaint();
    }

    // --- Texto Libre: secciones numeradas del informe ---

    private void agregarFilaTextoLibre() {
        javax.swing.JLabel numero = new javax.swing.JLabel();
        javax.swing.JTextField seccion = campoConPlaceholder("Ej: Observación microscópica");

        // Validación en vivo: solo letras.
        engancharValidacionEnVivo(
                () -> marcarValidez(seccion, ValidadorCatalogoExamenes.esTextoSoloLetras(seccion.getText())),
                seccion);

        javax.swing.JPanel fila = new javax.swing.JPanel(new java.awt.BorderLayout(10, 0));
        fila.setBackground(java.awt.Color.WHITE);
        fila.setBorder(new javax.swing.border.EmptyBorder(4, 0, 8, 0));
        fila.add(numero, java.awt.BorderLayout.WEST);
        fila.add(seccion, java.awt.BorderLayout.CENTER);
        fila.add(botonEliminarFila(listaTextoLibre, fila), java.awt.BorderLayout.EAST);

        listaTextoLibre.add(fila);
        renumerarSecciones();
        listaTextoLibre.revalidate();
        listaTextoLibre.repaint();
    }

    /**
     * Renumera "1.", "2.", ... después de agregar o sacar una sección.
     */
    private void renumerarSecciones() {
        int numero = 1;
        for (java.awt.Component comp : listaTextoLibre.getComponents()) {
            if (!(comp instanceof javax.swing.JPanel)) {
                continue;
            }
            java.awt.Component oeste = ((java.awt.BorderLayout) ((javax.swing.JPanel) comp).getLayout())
                    .getLayoutComponent(java.awt.BorderLayout.WEST);
            if (oeste instanceof javax.swing.JLabel) {
                ((javax.swing.JLabel) oeste).setText(numero + ".");
                numero++;
            }
        }
    }

    // --- Estado del examen (Activo / Inactivo) ---

    private javax.swing.JPanel construirPanelEstado() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
        panel.setBackground(new java.awt.Color(0xF6, 0xF8, 0xF7));
        panel.setBorder(new javax.swing.border.EmptyBorder(10, 14, 10, 14));

        javax.swing.JLabel titulo = new javax.swing.JLabel("Estado del examen");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.PLAIN, 13f));
        panel.add(titulo, java.awt.BorderLayout.WEST);

        javax.swing.JPanel botones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        javax.swing.JButton activo = botonPill("Activo");
        javax.swing.JButton inactivo = botonPill("Inactivo");
        botonesEstado.put("ACTIVO", activo);
        botonesEstado.put("INACTIVO", inactivo);
        activo.addActionListener(evt -> seleccionarEstado("ACTIVO"));
        inactivo.addActionListener(evt -> seleccionarEstado("INACTIVO"));
        botones.add(activo);
        botones.add(inactivo);
        panel.add(botones, java.awt.BorderLayout.EAST);

        seleccionarEstado("ACTIVO");
        return panel;
    }

    private javax.swing.JButton botonPill(String texto) {
        javax.swing.JButton boton = new javax.swing.JButton(texto);
        boton.putClientProperty("FlatLaf.style", "arc: 999; borderWidth: 1;");
        boton.setFocusPainted(false);
        boton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return boton;
    }

    /**
     * "ACTIVO" o "INACTIVO" (nunca null: Activo es el default, igual que en el diseño).
     */
    public void seleccionarEstado(String clave) {
        if (!botonesEstado.containsKey(clave)) {
            return;
        }
        estadoSeleccionado = clave;
        for (java.util.Map.Entry<String, javax.swing.JButton> entrada : botonesEstado.entrySet()) {
            boolean esElegido = entrada.getKey().equals(clave);
            javax.swing.JButton boton = entrada.getValue();
            boton.setContentAreaFilled(true);
            boton.setBackground(esElegido ? FONDO_SELECCIONADO : java.awt.Color.WHITE);
            boton.setForeground(esElegido ? new java.awt.Color(0x1E, 0x51, 0x3B) : new java.awt.Color(0x6E, 0x78, 0x74));
        }
    }

    public String getEstadoSeleccionado() {
        return estadoSeleccionado;
    }

    // --- Validación general del formulario (se llama al presionar "+ Crear Examen") ---

    /**
     * Valida el encabezado (Código / Nombre / Unidad Bioquímica) y las filas del tipo de examen
     * actualmente seleccionado, pintando los bordes en verde/rojo según corresponda, y devuelve
     * true solo si todo es válido. Si en algún momento se persisten los otros dos tipos además
     * del seleccionado, se puede ampliar para recorrer las tres listas siempre.
     */
private boolean validarFormularioCompleto() {

    boolean todoOk = encabezadoExamen1.marcarYValidar();

    if (tipoSeleccionado == null) {
        return false;
    }

    switch (tipoSeleccionado) {

        case "NUMERICO":

            for (java.awt.Component comp : listaNumerico.getComponents()) {

                if (!(comp instanceof javax.swing.JPanel)) {
                    continue;
                }

                javax.swing.JPanel fila =
                        (javax.swing.JPanel) comp;

                javax.swing.JTextField nombre =
                        (javax.swing.JTextField) fila.getComponent(0);

                javax.swing.JTextField unidad =
                        (javax.swing.JTextField) fila.getComponent(1);

                javax.swing.JTextField valorRef =
                        (javax.swing.JTextField) fila.getComponent(2);

                boolean okNombre =
                        ValidadorCatalogoExamenes.esNombreValido(
                                nombre.getText()
                        );

                boolean okUnidad =
                        ValidadorCatalogoExamenes.esUnidadValida(
                                unidad.getText()
                        );

                boolean okValor =
                        ValidadorCatalogoExamenes.esValorReferenciaValido(
                                valorRef.getText()
                        );

                marcarValidez(nombre, okNombre);
                marcarValidez(unidad, okUnidad);
                marcarValidez(valorRef, okValor);

                todoOk = todoOk
                        && okNombre
                        && okUnidad
                        && okValor;
            }

            break;


        case "CUALITATIVO":

            for (java.awt.Component comp :
                    listaCualitativo.getComponents()) {

                if (!(comp instanceof javax.swing.JPanel)) {
                    continue;
                }

                javax.swing.JPanel fila =
                        (javax.swing.JPanel) comp;

                javax.swing.JPanel filaSuperior =
                        (javax.swing.JPanel) fila.getComponent(0);

                javax.swing.JTextField nombre =
                        (javax.swing.JTextField) filaSuperior.getComponent(0);

                boolean okNombre =
                        ValidadorCatalogoExamenes.esTextoSoloLetras(
                                nombre.getText()
                        );

                marcarValidez(nombre, okNombre);

                todoOk = todoOk && okNombre;
            }

            break;


        case "TEXTOLIBRE":

            for (java.awt.Component comp :
                    listaTextoLibre.getComponents()) {

                if (!(comp instanceof javax.swing.JPanel)) {
                    continue;
                }

                javax.swing.JPanel fila =
                        (javax.swing.JPanel) comp;

                java.awt.Component centro =
                        ((java.awt.BorderLayout) fila.getLayout())
                                .getLayoutComponent(
                                        java.awt.BorderLayout.CENTER
                                );

                if (centro instanceof javax.swing.JTextField) {

                    javax.swing.JTextField seccion =
                            (javax.swing.JTextField) centro;

                    boolean okSeccion =
                            ValidadorCatalogoExamenes.esTextoSoloLetras(
                                    seccion.getText()
                            );

                    marcarValidez(seccion, okSeccion);

                    todoOk = todoOk && okSeccion;
                }
            }

            break;
    }

    return todoOk;
}

    // --- Encabezado (Código / Nombre / Unidad Bioquímica) ---

    public String getCodigo() {
        return encabezadoExamen1.getCodigo();
    }

    public String getNombreExamen() {
        return encabezadoExamen1.getNombreExamen();
    }

    public String getUnidadBioquimica() {
        return encabezadoExamen1.getUnidadBioquimica();
    }

    // --- Botones ---

    public javax.swing.JButton getBotonCancelar() {
        return jButton2;
    }

    public javax.swing.JButton getBotonGuardar() {
        return jButton3;
    }



    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        encabezadoExamen1 = new vistas.catalogoExamenes.EncabezadoExamen();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(encabezadoExamen1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(encabezadoExamen1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 16)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 153));
        jLabel3.setText("Con rangos de referencias ");

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel4.setText("Numerico");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addGap(59, 59, 59))))
        );

        jPanel2.add(jPanel3);

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));

        jLabel6.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel6.setText("Cualitativo");

        jLabel7.setFont(new java.awt.Font("SansSerif", 1, 16)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 153, 153));
        jLabel7.setText("Opciones de resultados ");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addGap(56, 56, 56))))
        );

        jPanel2.add(jPanel4);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/escritura.png"))); // NOI18N

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel9.setText("Texto Libre");

        jLabel10.setFont(new java.awt.Font("SansSerif", 1, 16)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(0, 153, 153));
        jLabel10.setText("Seleccione con opcion ");

        jLabel11.setFont(new java.awt.Font("SansSerif", 1, 16)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 153, 153));
        jLabel11.setText("a la carga");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel11)))
                .addGap(37, 37, 37))
        );

        jPanel2.add(jPanel5);

        jLabel12.setFont(new java.awt.Font("SansSerif", 1, 16)); // NOI18N

        jButton1.setText("Agregar");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 263, Short.MAX_VALUE)
        );

        jButton2.setText("Cancelar");

        jButton3.setText("Guardar");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 615, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 1093, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(727, 727, 727)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.catalogoExamenes.EncabezadoExamen encabezadoExamen1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    // End of variables declaration//GEN-END:variables
}
