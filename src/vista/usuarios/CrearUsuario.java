/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vista.usuarios;

/**
 *
 * @author agust
 */
public class CrearUsuario extends javax.swing.JPanel {

    /**
     * Modo en el que se abre el formulario: alta de un usuario nuevo, sólo lectura (Ver) o edición
     * de un usuario existente (Editar). Determina si los campos se pueden tocar, si se muestra la
     * barra de acciones y cómo se maneja la contraseña (ver {@link #configurarPassword()}).
     */
    public enum Modo {
        NUEVO, VER, EDITAR
    }

    private Modo modo = Modo.NUEVO;

    /**
     * Usuario que se está viendo/editando (null en modo NUEVO). Hace falta para saber a quién
     * actualizar o a quién generarle una contraseña nueva.
     */
    private modelo.Usuario usuarioActual;

    // Índice elegido dentro de cada grupo Rol / Estado (0 = primer botón del array pasado a
    // activarGrupoSeleccion). Los JButton del Form Editor no tienen un modelo de selección real
    // -- sólo cambian de estilo -- así que hay que llevar la cuenta aparte para poder armar el
    // Usuario a guardar (ver obtenerDatosUsuario()).
    private int indiceRolSeleccionado = 1;
    private int indiceEstadoSeleccionado = 0;

    /**
     * Toggle de "Acceso a Pantalla" por nombre de pantalla (ej. "Escritorio", "Pacientes"), en el
     * mismo orden en que aparecen en el menú. Se arman en {@link #activarTogglesAccesoPantalla()}.
     */
    private final java.util.Map<String, vistas.acciones.ToggleButton> togglesAcceso = new java.util.LinkedHashMap<>();

    private static final java.awt.Color VERDE_INSTITUCIONAL = new java.awt.Color(39, 103, 73);
    private static final java.awt.Color GRIS_BORDE = new java.awt.Color(224, 228, 226);
    private static final java.awt.Color BLANCO = new java.awt.Color(255, 255, 255);
    private static final java.awt.Color GRIS_FILA_ALTERNA = new java.awt.Color(246, 248, 247);
    // Se probó un tinte verde clarito de fondo en las fichas/tabla y no convenció -- queda en gris
    // neutro, dejando el verde institucional solo para títulos, el puntito del encabezado y el
    // radio seleccionado (ver PermisosExamen.IconoRadioVerde).
    private static final java.awt.Color GRIS_TEXTO = new java.awt.Color(51, 65, 85);

    /**
     * Botones de la barra de acciones (ver {@link #agregarBarraDeAcciones()}). El Form Editor no
     * los declaraba -- se exponen para que quien contenga a este panel (el diálogo "Nuevo
     * Usuario") les cablee la lógica de guardar/cancelar.
     */
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnCrearUsuario;

    /**
     * Creates new form CrearUsuario. Equivale a abrirlo en modo alta (NUEVO).
     */
    public CrearUsuario() {
        this(Modo.NUEVO, null);
    }

    /**
     * Abre el formulario para ver o editar un usuario existente. En modo VER todos los campos
     * quedan de sólo lectura, se ocultan la contraseña y la barra de acciones; en modo EDITAR se
     * puede modificar y guardar, pero la contraseña sólo se puede regenerar, nunca ver la
     * anterior (ver {@link #configurarPassword()}).
     */
    public CrearUsuario(Modo modo, modelo.Usuario usuario) {
        // El modo se fija antes de armar la UI: varios de los métodos de abajo (toggles, radios de
        // permisos, barra de acciones, campo de contraseña) necesitan saber si están en modo VER
        // para no quedar interactivos.
        this.modo = modo;
        this.usuarioActual = usuario;
        initComponents();
        blanquearFondoPermisos();
        estilizarDatosPersonales();
        agregarBarraDeAcciones();
        activarTogglesAccesoPantalla();
        cargarPermisosExamen();
        cargarDatosUsuario(usuario);
    }

    /**
     * Vuelca al formulario los datos básicos de un usuario existente (Ver/Editar). El DNI, el
     * Sexo y el Celular quedan sin cargar: el sistema no guarda esos datos para un usuario todavía
     * (sólo nombre, apellido, email, rol y estado -- ver {@link dao.Usuario}).
     */
    private void cargarDatosUsuario(modelo.Usuario usuario) {
        if (usuario == null) {
            return;
        }
        ayn.setText(usuario.getApellidoYNombre());
        // "edad" es el nombre de variable que le puso el Form Editor al campo que en el diseño es
        // "Email" (ver jLabel11) -- no es un error, así quedó declarado en el GEN.
        edad.setText(usuario.getEmail());

        indiceRolSeleccionado = "Administrador".equalsIgnoreCase(usuario.getRol()) ? 0 : 1;
        marcarSeleccionado(new javax.swing.JButton[]{jButton5, jButton2}, indiceRolSeleccionado);

        indiceEstadoSeleccionado = usuario.isActivo() ? 0 : 1;
        marcarSeleccionado(new javax.swing.JButton[]{jButton3, jButton4}, indiceEstadoSeleccionado);
    }

    /**
     * Arma un {@link modelo.Usuario} con lo que hay cargado en el formulario, para guardar los
     * cambios (modo EDITAR). No toca la contraseña -- eso se maneja aparte con
     * {@link #generarNuevaPassword()}.
     */
    public modelo.Usuario obtenerDatosUsuario() {
        modelo.Usuario usuario = new modelo.Usuario();
        if (usuarioActual != null) {
            usuario.setIdUsuario(usuarioActual.getIdUsuario());
        }

        String texto = ayn.getText() == null ? "" : ayn.getText().trim();
        int espacio = texto.indexOf(' ');
        usuario.setApellido(espacio == -1 ? texto : texto.substring(0, espacio));
        usuario.setNombre(espacio == -1 ? "" : texto.substring(espacio + 1).trim());

        usuario.setEmail(edad.getText());
        usuario.setRol(indiceRolSeleccionado == 0 ? "Administrador" : "Tecnico");
        usuario.setActivo(indiceEstadoSeleccionado == 0);
        return usuario;
    }

    /**
     * jPanel1 ("Datos Personales") tiene el mismo problema que jPanel24/jPanel25: el Form Editor
     * no le pone fondo blanco, y los campos y los botones de Rol / Estado quedan con el aspecto
     * plano por defecto del Look & Feel, sin ningún estado "seleccionado" (son JButton comunes,
     * no hay lógica de selección declarada en el GEN).
     */
    private void estilizarDatosPersonales() {
        jPanel1.setBackground(BLANCO);

        String estiloCampo = "arc: 8; borderColor: #E0E4E2; focusedBorderColor: #276749";
        for (javax.swing.JTextField campo : new javax.swing.JTextField[]{dni, ayn, edad, celular, celular1}) {
            campo.putClientProperty("FlatLaf.style", estiloCampo);
        }
        sexo.putClientProperty("FlatLaf.style", estiloCampo);

        // jButton5/jButton2 = Administrador/Tecnico (Rol); jButton3/jButton4 = Activo/Inactivo
        // (Estado). Se arman como dos grupos de selección única, con estilo tipo "píldora" y
        // resaltado verde para el que está elegido -- "Tecnico" y "Activo" arrancan seleccionados
        // salvo que después cargarDatosUsuario() las pise con los datos reales del usuario.
        activarGrupoSeleccion(new javax.swing.JButton[]{jButton5, jButton2}, 1, indice -> indiceRolSeleccionado = indice);
        activarGrupoSeleccion(new javax.swing.JButton[]{jButton3, jButton4}, 0, indice -> indiceEstadoSeleccionado = indice);

        configurarPassword();

        if (modo == Modo.VER) {
            // DNI/Sexo/Celular no se guardan todavía (ver dao.Usuario), pero igual quedan
            // deshabilitados en modo Ver para que el formulario se vea consistentemente de sólo
            // lectura.
            for (javax.swing.JTextField campo : new javax.swing.JTextField[]{dni, ayn, edad, celular1}) {
                campo.setEditable(false);
            }
            sexo.setEnabled(false);
        }
    }

    private void activarGrupoSeleccion(javax.swing.JButton[] botones, int indiceSeleccionadoInicial,
            java.util.function.IntConsumer alSeleccionar) {
        for (int i = 0; i < botones.length; i++) {
            int indice = i;
            botones[i].addActionListener(evt -> {
                // En modo Ver, Rol y Estado no se pueden tocar -- se ignora el click en vez de
                // deshabilitar los botones, para no perder el resaltado verde del valor actual.
                if (modo == Modo.VER) {
                    return;
                }
                marcarSeleccionado(botones, indice);
                alSeleccionar.accept(indice);
            });
        }
        marcarSeleccionado(botones, indiceSeleccionadoInicial);
        alSeleccionar.accept(indiceSeleccionadoInicial);
    }

    /**
     * La "Contraseña" (jLabel12 / campo celular, ver el comentario en la declaración de
     * variables) se comporta distinto según el modo: en Ver se oculta por completo (ni la
     * etiqueta ni el campo); en Editar no se puede ver ni escribir la anterior, sólo generarle una
     * nueva a través de {@link controlador.PasswordController}; en Nuevo queda como campo normal.
     */
    private void configurarPassword() {
        switch (modo) {
            case VER:
                jLabel12.setVisible(false);
                celular.setVisible(false);
                break;
            case EDITAR:
                celular.setEditable(false);
                celular.setText("Click para generar una nueva contraseña");
                celular.setForeground(new java.awt.Color(140, 148, 144));
                celular.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                celular.setToolTipText("Le asigna una contraseña nueva al usuario. No se puede ver la anterior.");
                celular.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        generarNuevaPassword();
                    }
                });
                break;
            default:
                // NUEVO: el campo queda como lo declara el Form Editor (editable, para cuando se
                // cablee el alta de un usuario nuevo).
                break;
        }
    }

    /**
     * Le pide al usuario una contraseña nueva y se la asigna al usuario que se está editando, sin
     * mostrar en ningún momento la anterior. Reutiliza {@link controlador.PasswordController},
     * que valida el largo mínimo y guarda el hash.
     */
    private void generarNuevaPassword() {
        if (usuarioActual == null) {
            return;
        }
        String nueva = javax.swing.JOptionPane.showInputDialog(this,
                "Ingresá la nueva contraseña para este usuario (mínimo 8 caracteres).",
                "Generar nueva contraseña", javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (nueva == null || nueva.trim().isEmpty()) {
            return;
        }
        boolean ok = controlador.PasswordController.cambiarPassword(this, usuarioActual.getIdUsuario(), nueva);
        if (ok) {
            celular.setText("Contraseña actualizada");
        }
    }

    private void marcarSeleccionado(javax.swing.JButton[] botones, int indiceSeleccionado) {
        for (int i = 0; i < botones.length; i++) {
            boolean seleccionado = i == indiceSeleccionado;
            botones[i].putClientProperty("FlatLaf.style", seleccionado
                    ? "arc: 10; background: #E8F5EC; borderColor: #276749; borderWidth: 1.5; foreground: #276749"
                    : "arc: 10; background: #FFFFFF; borderColor: #E0E4E2; foreground: #33414D");
            botones[i].setFont(botones[i].getFont().deriveFont(seleccionado ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
        }
    }

    public javax.swing.JButton getBotonCancelar() {
        return btnCancelar;
    }

    public javax.swing.JButton getBotonCrearUsuario() {
        return btnCrearUsuario;
    }

    /**
     * El Form Editor solo ubica a jLabel3 y jTabbedPane1 en este panel -- no hay lugar para los
     * botones "Cancelar" / "Crear Usuario" del diseño. Se reemplaza el GroupLayout vacío que arma
     * el Form Editor por un BorderLayout hecho a mano: jLabel3 arriba, las pestañas al medio y una
     * barra de acciones nueva abajo, con los dos botones.
     */
    private void agregarBarraDeAcciones() {
        removeAll();
        setLayout(new java.awt.BorderLayout());

        javax.swing.JPanel encabezado = new javax.swing.JPanel(new java.awt.BorderLayout());
        encabezado.setOpaque(false);
        encabezado.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 8, 20));
        encabezado.add(jLabel3, java.awt.BorderLayout.WEST);
        add(encabezado, java.awt.BorderLayout.NORTH);

        add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        // En modo Ver no hay nada para cancelar ni guardar -- se pidió explícitamente que no se
        // vean los botones, así que directamente no se agrega la barra.
        if (modo != Modo.VER) {
            add(construirBarraDeAcciones(), java.awt.BorderLayout.SOUTH);
        }
    }

    private javax.swing.JPanel construirBarraDeAcciones() {
        javax.swing.JPanel barra = new javax.swing.JPanel(new java.awt.BorderLayout());
        barra.setBackground(BLANCO);
        barra.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, GRIS_BORDE),
                javax.swing.BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        javax.swing.JPanel botones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);

        btnCancelar = new javax.swing.JButton("Cancelar");
        btnCancelar.putClientProperty("FlatLaf.style", "arc: 10; borderColor: #E0E4E2; background: #FFFFFF");

        btnCrearUsuario = new javax.swing.JButton(modo == Modo.EDITAR ? "Guardar Cambios" : "+  Crear Usuario");
        btnCrearUsuario.setForeground(BLANCO);
        btnCrearUsuario.putClientProperty("FlatLaf.style",
                "arc: 10; background: #276749; hoverBackground: #1F5238; pressedBackground: #1A4530");

        botones.add(btnCancelar);
        botones.add(btnCrearUsuario);
        barra.add(botones, java.awt.BorderLayout.EAST);
        return barra;
    }

    /**
     * El Form Editor le pone fondo blanco a este panel y a jTabbedPane1, pero no a jPanel24,
     * jScrollPane7 ni a jPanel25 (el contenido real de la pestaña "Permisos y Acceso") -- quedan
     * con el gris por defecto del Look & Feel, que no se notaba con los colores neutros de antes
     * pero desentona con las fichas verdes.
     */
    private void blanquearFondoPermisos() {
        jPanel24.setBackground(java.awt.Color.WHITE);
        jScrollPane7.setBackground(java.awt.Color.WHITE);
        jScrollPane7.getViewport().setBackground(java.awt.Color.WHITE);
        jPanel25.setBackground(java.awt.Color.WHITE);
    }

    /**
     * Reemplaza cada etiqueta de "Acceso a Pantalla" (Escritorio, Pacientes, etc.) por una fila
     * con la etiqueta a la izquierda y un {@link vistas.acciones.ToggleButton} a la derecha, igual
     * que en el diseño. Arrancan todos apagados -- el usuario los prende a mano según lo que le
     * quiera dar de alta a este perfil.
     */
    private void activarTogglesAccesoPantalla() {
        activarToggleAcceso(p1, lblEscritorio, "Escritorio");
        activarToggleAcceso(p3, lblRegistro, "Registros");
        activarToggleAcceso(p5, lblRR, "Registrar Resultados");
        activarToggleAcceso(p7, lblCotizacion, "Cotizacion");
        activarToggleAcceso(p9, lblUsuarios, "Usuarios");
        activarToggleAcceso(p11, lblConf, "Configuracion");
        activarToggleAcceso(p2, lblPac, "Pacientes");
        activarToggleAcceso(p4, lblNA, "Nuevo Analisis");
        activarToggleAcceso(p6, lblCE, "Catalogo de Examenes");
        activarToggleAcceso(p8, lblPagos, "Pagos");
        activarToggleAcceso(p10, lblEstadisticas, "Estadisticas");
    }

    private void activarToggleAcceso(javax.swing.JPanel fila, javax.swing.JLabel etiqueta, String pantalla) {
        fila.removeAll();
        fila.setLayout(new java.awt.BorderLayout());
        fila.setOpaque(false);

        // Ficha redondeada envolviendo la etiqueta y el toggle, como en el diseño.
        PanelRedondeado ficha = new PanelRedondeado(new java.awt.BorderLayout(), 10);
        ficha.setBackground(GRIS_FILA_ALTERNA);
        ficha.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));

        etiqueta.setBorder(null);
        etiqueta.setForeground(GRIS_TEXTO);
        ficha.add(etiqueta, java.awt.BorderLayout.WEST);

        vistas.acciones.ToggleButton toggle = new vistas.acciones.ToggleButton();
        if (modo == Modo.VER) {
            // ToggleButton no respeta setEnabled(false) (su propio mouse listener no lo consulta),
            // así que en modo Ver se le sacan directamente los listeners para que quede de sólo
            // lectura.
            for (java.awt.event.MouseListener listener : toggle.getMouseListeners()) {
                toggle.removeMouseListener(listener);
            }
        }
        javax.swing.JPanel envoltorio = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        envoltorio.setOpaque(false);
        envoltorio.add(toggle);
        ficha.add(envoltorio, java.awt.BorderLayout.EAST);

        fila.add(ficha, java.awt.BorderLayout.CENTER);

        // El layout que arma el Form Editor (jPanel25Layout) le da a cada pX su propio
        // preferredSize sin ningún ancho fijo de por medio (a diferencia de otros componentes de
        // esta pantalla), así que acá sí queda firme el override por código: se fuerza el mismo
        // ancho en las 11 filas para que la ficha quede alineada en columna en vez de correrse
        // según lo larga que sea cada etiqueta (pasaba sobre todo con "Registrar Resultados" y
        // "Catalogo de Examenes", que no tienen ancho fijo en el label original).
        fila.setPreferredSize(new java.awt.Dimension(340, 46));

        togglesAcceso.put(pantalla, toggle);
    }

    /**
     * JPanel con fondo redondeado, pintado a mano con Graphics2D en vez de dejar que se rellene
     * un rectángulo -- se usa para las fichas de Acceso a Pantalla y para la tabla de Permisos
     * por examen, buscando el aspecto del diseño.
     */
    private static class PanelRedondeado extends javax.swing.JPanel {

        private final int arco;

        PanelRedondeado(java.awt.LayoutManager layout, int arco) {
            super(layout);
            this.arco = arco;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arco, arco);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Estado actual de cada toggle de "Acceso a Pantalla", por nombre de pantalla. Pensado para
     * cuando se cablee el guardado del usuario.
     */
    public java.util.Map<String, Boolean> obtenerAccesosPantalla() {
        java.util.Map<String, Boolean> accesos = new java.util.LinkedHashMap<>();
        togglesAcceso.forEach((pantalla, toggle) -> accesos.put(pantalla, toggle.isSelected()));
        return accesos;
    }

    /**
     * Encabezado de columnas de "Permisos por examen" (EXAMEN / Sin acceso / Solo ver / Cargar).
     * Usa los mismos anchos y espacios que declara {@link vista.usuarios.PermisosExamen} para
     * que las columnas queden alineadas con las filas.
     */
    private javax.swing.JPanel construirEncabezadoPermisos() {
        javax.swing.JPanel encabezado = new javax.swing.JPanel(new java.awt.GridBagLayout());
        javax.swing.JLabel lblExamen = new javax.swing.JLabel("EXAMEN");
        // Puntito verde antes del título de cada columna de nivel de acceso, como en el diseño.
        javax.swing.JLabel lblSinAcceso = new javax.swing.JLabel("<html><font color='#276749'>&#9679;</font>&nbsp;Sin acceso</html>");
        javax.swing.JLabel lblSoloVer = new javax.swing.JLabel("<html><font color='#276749'>&#9679;</font>&nbsp;Solo ver</html>");
        javax.swing.JLabel lblCargar = new javax.swing.JLabel("<html><font color='#276749'>&#9679;</font>&nbsp;Cargar</html>");

        java.awt.Font fuenteEncabezado = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12);
        java.awt.Color grisEncabezado = new java.awt.Color(140, 148, 144);
        for (javax.swing.JLabel etiqueta : new javax.swing.JLabel[]{lblExamen, lblSinAcceso, lblSoloVer, lblCargar}) {
            etiqueta.setFont(fuenteEncabezado);
            etiqueta.setForeground(grisEncabezado);
        }
        // Mismo ancho fijo que usa vista.usuarios.PermisosExamen.reorganizarLayout() para su
        // columna del nombre, así el encabezado queda alineado con las filas.
        lblExamen.setPreferredSize(new java.awt.Dimension(280, lblExamen.getPreferredSize().height));

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridy = 0;
        // Mismos insets que vista.usuarios.PermisosExamen.reorganizarLayout() usa para sus filas
        // -- tienen que coincidir para que las columnas queden alineadas.
        gbc.insets = new java.awt.Insets(8, 16, 8, 12);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        encabezado.add(lblExamen, gbc);

        gbc.weightx = 1;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;
        gbc.gridx = 1;
        encabezado.add(lblSinAcceso, gbc);
        gbc.gridx = 2;
        encabezado.add(lblSoloVer, gbc);
        gbc.gridx = 3;
        encabezado.add(lblCargar, gbc);

        return encabezado;
    }

    /**
     * Trae los exámenes reales (analisis_tipos) y arma una fila de {@link
     * vista.usuarios.PermisosExamen} por cada uno dentro de "Contenedor", con el encabezado de
     * columnas arriba. Reemplaza el GroupLayout vacío que deja el Form Editor en "Contenedor"
     * por uno que apila las filas -- "Contenedor" crece con la cantidad de exámenes y el
     * jScrollPane7 que ya envuelve toda la pestaña se encarga de que se pueda scrollear.
     */
    private void cargarPermisosExamen() {
        // "Contenedor" pasa a alojar dos cosas apiladas: el subtítulo explicativo arriba y la
        // tarjeta redondeada con la tabla debajo -- por eso el BorderLayout en vez de apilar filas
        // directamente en él como antes.
        Contenedor.setLayout(new java.awt.BorderLayout());
        Contenedor.setOpaque(false);

        javax.swing.JLabel subtitulo = new javax.swing.JLabel(
                "Define qué puede hacer este usuario con cada tipo de análisis.");
        subtitulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        subtitulo.setForeground(new java.awt.Color(120, 130, 126));
        subtitulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 2, 8, 0));
        Contenedor.add(subtitulo, java.awt.BorderLayout.NORTH);

        // Tarjeta redondeada que agrupa el encabezado y todas las filas, como en el diseño.
        PanelRedondeado tarjeta = new PanelRedondeado(new java.awt.GridLayout(0, 1, 0, 1), 12);
        tarjeta.setBackground(BLANCO);
        tarjeta.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new com.formdev.flatlaf.ui.FlatLineBorder(new java.awt.Insets(1, 1, 1, 1), GRIS_BORDE, 1f, 12),
                javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        javax.swing.JPanel encabezado = construirEncabezadoPermisos();
        encabezado.setOpaque(true);
        encabezado.setBackground(GRIS_FILA_ALTERNA);
        tarjeta.add(encabezado);

        java.util.List<modelo.AnalisisTipo> examenes = controlador.AnalisisTipoController.listarTodos(this);
        boolean filaAlterna = false;
        for (modelo.AnalisisTipo examen : examenes) {
            vista.usuarios.PermisosExamen fila = new vista.usuarios.PermisosExamen();
            fila.cargarExamen(examen);
            // Filas alternadas (blanco / gris muy clarito).
            fila.setOpaque(true);
            fila.setBackground(filaAlterna ? GRIS_FILA_ALTERNA : BLANCO);
            filaAlterna = !filaAlterna;
            if (modo == Modo.VER) {
                fila.setSoloLectura(true);
            }
            tarjeta.add(fila);
        }

        Contenedor.add(tarjeta, java.awt.BorderLayout.CENTER);

        // jPanel25Layout ubica a Contenedor usando su propio preferredSize (sin ningún número fijo
        // de por medio), y como las filas con GridBagLayout piden bastante menos ancho del que
        // hay disponible, Contenedor se achica solo y todo queda apretado contra la izquierda. Se
        // lo fuerza a ocupar el ancho real del jScrollPane7 (844px) para que las columnas de cada
        // fila tengan lugar de sobra donde repartirse.
        Contenedor.setPreferredSize(new java.awt.Dimension(810, Contenedor.getPreferredSize().height));
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        dni = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        ayn = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        edad = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        celular = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        celular1 = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        sexo = new javax.swing.JComboBox<>();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel24 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jPanel25 = new javax.swing.JPanel();
        p1 = new javax.swing.JPanel();
        lblEscritorio = new javax.swing.JLabel();
        p3 = new javax.swing.JPanel();
        lblRegistro = new javax.swing.JLabel();
        p5 = new javax.swing.JPanel();
        lblRR = new javax.swing.JLabel();
        p7 = new javax.swing.JPanel();
        lblCotizacion = new javax.swing.JLabel();
        p9 = new javax.swing.JPanel();
        lblUsuarios = new javax.swing.JLabel();
        p2 = new javax.swing.JPanel();
        lblPac = new javax.swing.JLabel();
        p4 = new javax.swing.JPanel();
        lblNA = new javax.swing.JLabel();
        p6 = new javax.swing.JPanel();
        lblCE = new javax.swing.JLabel();
        p8 = new javax.swing.JPanel();
        lblPagos = new javax.swing.JLabel();
        p10 = new javax.swing.JPanel();
        lblEstadisticas = new javax.swing.JLabel();
        p11 = new javax.swing.JPanel();
        lblConf = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        Contenedor = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N

        jTabbedPane1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel4.setText("DNI");

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel9.setText("Apellido y Nombre");

        jLabel11.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel11.setText("Email");

        jLabel10.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel10.setText("Rol");

        jLabel12.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel12.setText("Contraseña");

        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel5.setText("Sexo");

        sexo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        sexo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sexoActionPerformed(evt);
            }
        });

        jButton2.setText("Tecnico");

        jButton3.setText("Activo");

        jButton4.setText("Inactivo");

        jButton5.setText("Administrador");

        jLabel13.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel13.setText("Celular(opcional)");

        jLabel14.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel14.setText("Estado de la cuenta");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(dni, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                                        .addComponent(celular1, javax.swing.GroupLayout.Alignment.LEADING))
                                    .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(71, 71, 71)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel11)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ayn, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                                        .addComponent(edad)
                                        .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 227, Short.MAX_VALUE)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(66, 66, 66))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(184, 184, 184))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(83, 83, 83)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel9))
                .addGap(9, 9, 9)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ayn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dni, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(13, 13, 13)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(edad, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sexo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(celular, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(celular1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(38, 38, 38)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(95, 95, 95))
        );

        jTabbedPane1.addTab("Datos Personales ", jPanel1);

        lblEscritorio.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblEscritorio.setText("Escritorio");

        javax.swing.GroupLayout p1Layout = new javax.swing.GroupLayout(p1);
        p1.setLayout(p1Layout);
        p1Layout.setHorizontalGroup(
            p1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p1Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblEscritorio, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p1Layout.setVerticalGroup(
            p1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblEscritorio, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblRegistro.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblRegistro.setText("Registros");

        javax.swing.GroupLayout p3Layout = new javax.swing.GroupLayout(p3);
        p3.setLayout(p3Layout);
        p3Layout.setHorizontalGroup(
            p3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p3Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p3Layout.setVerticalGroup(
            p3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblRegistro, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblRR.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblRR.setText("Registrar Resultados");

        javax.swing.GroupLayout p5Layout = new javax.swing.GroupLayout(p5);
        p5.setLayout(p5Layout);
        p5Layout.setHorizontalGroup(
            p5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p5Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblRR)
                .addContainerGap(121, Short.MAX_VALUE))
        );
        p5Layout.setVerticalGroup(
            p5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblRR, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblCotizacion.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblCotizacion.setText("Cotizacion");

        javax.swing.GroupLayout p7Layout = new javax.swing.GroupLayout(p7);
        p7.setLayout(p7Layout);
        p7Layout.setHorizontalGroup(
            p7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p7Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblCotizacion, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p7Layout.setVerticalGroup(
            p7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblCotizacion, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblUsuarios.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblUsuarios.setText("Usuarios");

        javax.swing.GroupLayout p9Layout = new javax.swing.GroupLayout(p9);
        p9.setLayout(p9Layout);
        p9Layout.setHorizontalGroup(
            p9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p9Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblUsuarios, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p9Layout.setVerticalGroup(
            p9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblUsuarios, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblPac.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblPac.setText("Pacientes");

        javax.swing.GroupLayout p2Layout = new javax.swing.GroupLayout(p2);
        p2.setLayout(p2Layout);
        p2Layout.setHorizontalGroup(
            p2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p2Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblPac, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p2Layout.setVerticalGroup(
            p2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblPac, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblNA.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblNA.setText("Nuevo Analisis");

        javax.swing.GroupLayout p4Layout = new javax.swing.GroupLayout(p4);
        p4.setLayout(p4Layout);
        p4Layout.setHorizontalGroup(
            p4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p4Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblNA, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p4Layout.setVerticalGroup(
            p4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblNA, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblCE.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblCE.setText("Catalogo de Examenes");

        javax.swing.GroupLayout p6Layout = new javax.swing.GroupLayout(p6);
        p6.setLayout(p6Layout);
        p6Layout.setHorizontalGroup(
            p6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p6Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblCE)
                .addContainerGap(109, Short.MAX_VALUE))
        );
        p6Layout.setVerticalGroup(
            p6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblCE, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblPagos.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblPagos.setText("Pagos");

        javax.swing.GroupLayout p8Layout = new javax.swing.GroupLayout(p8);
        p8.setLayout(p8Layout);
        p8Layout.setHorizontalGroup(
            p8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p8Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblPagos, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p8Layout.setVerticalGroup(
            p8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblPagos, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblEstadisticas.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblEstadisticas.setText("Estadisticas");

        javax.swing.GroupLayout p10Layout = new javax.swing.GroupLayout(p10);
        p10.setLayout(p10Layout);
        p10Layout.setHorizontalGroup(
            p10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p10Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblEstadisticas, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p10Layout.setVerticalGroup(
            p10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblEstadisticas, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        lblConf.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        lblConf.setText("Configuracion");

        javax.swing.GroupLayout p11Layout = new javax.swing.GroupLayout(p11);
        p11.setLayout(p11Layout);
        p11Layout.setHorizontalGroup(
            p11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(p11Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblConf, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(141, Short.MAX_VALUE))
        );
        p11Layout.setVerticalGroup(
            p11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblConf, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
        );

        jLabel47.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel47.setForeground(new java.awt.Color(51, 102, 0));
        jLabel47.setText("Permisos por examen");

        javax.swing.GroupLayout ContenedorLayout = new javax.swing.GroupLayout(Contenedor);
        Contenedor.setLayout(ContenedorLayout);
        ContenedorLayout.setHorizontalGroup(
            ContenedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 814, Short.MAX_VALUE)
        );
        ContenedorLayout.setVerticalGroup(
            ContenedorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 126, Short.MAX_VALUE)
        );

        jLabel35.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel35.setForeground(new java.awt.Color(51, 102, 0));
        jLabel35.setText("Acceso a Pantalla");

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addComponent(Contenedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 527, Short.MAX_VALUE))
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(p1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(92, 92, 92)
                        .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(p2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(p10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel47, javax.swing.GroupLayout.PREFERRED_SIZE, 833, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 812, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addComponent(p1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addComponent(p2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(p10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jLabel47)
                .addGap(47, 47, 47)
                .addComponent(Contenedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 91, Short.MAX_VALUE))
        );

        jScrollPane7.setViewportView(jPanel25);

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 844, Short.MAX_VALUE)
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel24Layout.createSequentialGroup()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 538, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Permisos y Acceso", jPanel24);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 849, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 537, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(51, 51, 51))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sexoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sexoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Contenedor;
    private javax.swing.JTextField ayn;
    private javax.swing.JTextField celular;
    private javax.swing.JTextField celular1;
    private javax.swing.JTextField dni;
    private javax.swing.JTextField edad;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblCE;
    private javax.swing.JLabel lblConf;
    private javax.swing.JLabel lblCotizacion;
    private javax.swing.JLabel lblEscritorio;
    private javax.swing.JLabel lblEstadisticas;
    private javax.swing.JLabel lblNA;
    private javax.swing.JLabel lblPac;
    private javax.swing.JLabel lblPagos;
    private javax.swing.JLabel lblRR;
    private javax.swing.JLabel lblRegistro;
    private javax.swing.JLabel lblUsuarios;
    private javax.swing.JPanel p1;
    private javax.swing.JPanel p10;
    private javax.swing.JPanel p11;
    private javax.swing.JPanel p2;
    private javax.swing.JPanel p3;
    private javax.swing.JPanel p4;
    private javax.swing.JPanel p5;
    private javax.swing.JPanel p6;
    private javax.swing.JPanel p7;
    private javax.swing.JPanel p8;
    private javax.swing.JPanel p9;
    private javax.swing.JComboBox<String> sexo;
    // End of variables declaration//GEN-END:variables
}
