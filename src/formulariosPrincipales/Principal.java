
package formulariosPrincipales;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import menu.EventMenuSelected;

/**
 * {@code JFrame} contenedor principal de la aplicación: aloja el menú
 * lateral ({@code menu1}) y el área de contenido central
 * ({@code contenedor}), y es responsable de la navegación entre pantallas
 * ({@link #navegar}) y de que la ventana (sin decoración del sistema
 * operativo, {@code undecorated}) ocupe toda la pantalla disponible y se
 * reacomode de forma responsive a su tamaño real, sea cual sea la
 * resolución de cada equipo. Es el punto de entrada de la aplicación una
 * vez logueado (ver {@link #main}).
 */
public class Principal extends javax.swing.JFrame {

/**
 * Configura la ventana principal: la lleva a ocupar toda la pantalla
 * disponible respetando la barra de tareas de Windows aunque sea
 * undecorated (ver comentario más abajo sobre {@code setMaximizedBounds}),
 * deja armado el reacomodo responsive del sidebar y el contenedor
 * ({@link #ajustarLayout()}) cada vez que cambia el tamaño de la ventana,
 * conecta la navegación del menú lateral con {@link #navegar} y abre el
 * Escritorio como pantalla inicial.
 */
public Principal() {
    initComponents();

    // Arranca ocupando toda la pantalla disponible, sea cual sea la
    // resolución o el porcentaje de escala (100%, 125%, 150%) que tenga
    // configurado Windows en esa compu.
    //
    // Antes esto se hacía calculando "a mano" el rectángulo usable de la
    // pantalla (getMaximumWindowBounds()) y aplicándolo con setBounds().
    // El problema es que ese cálculo manual puede salir mal con ciertas
    // combinaciones de resolución/escala de Windows, y la ventana termina
    // más grande que la pantalla real (hay que arrastrarla para ver el
    // resto). Windows sabe calcular esto perfecto para CUALQUIER
    // resolución/escala/monitor a través del maximizado nativo
    // (MAXIMIZED_BOTH); el único motivo por el que no se usaba antes es
    // que en una ventana undecorated, maximizar tapa la barra de tareas.
    // Se soluciona indicándole a Java, con setMaximizedBounds(), cuál es
    // el rectángulo que tiene que usar al maximizar (el usable, sin la
    // barra de tareas) para que lo respete.
    java.awt.GraphicsConfiguration configPantalla = getGraphicsConfiguration();
    if (configPantalla == null) {
        configPantalla = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }
    java.awt.Rectangle limitesPantalla = configPantalla.getBounds();
    java.awt.Insets bordesPantalla = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(configPantalla);
    java.awt.Rectangle limitesUsables = new java.awt.Rectangle(
            limitesPantalla.x + bordesPantalla.left,
            limitesPantalla.y + bordesPantalla.top,
            limitesPantalla.width - bordesPantalla.left - bordesPantalla.right,
            limitesPantalla.height - bordesPantalla.top - bordesPantalla.bottom);

    if (java.awt.Toolkit.getDefaultToolkit().isFrameStateSupported(java.awt.Frame.MAXIMIZED_BOTH)) {
        setMaximizedBounds(limitesUsables);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
    } else {
        // Respaldo por si el sistema operativo no soporta maximizado
        // nativo (no debería pasar en Windows, pero por las dudas).
        this.setBounds(limitesUsables);
    }

    // El menú (sidebar) y el contenedor de pantallas usan AbsoluteLayout con
    // medidas fijas (venían pensados para 1314x870). Para que ocupen todo el
    // ancho/alto real de la ventana maximizada, los reacomodamos cada vez que
    // panelBorder1 cambia de tamaño.
    panelBorder1.addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            ajustarLayout();
        }
    });
    ajustarLayout();

    // Conectar el menú con la navegación
    menu1.addEventMenuSelected(new EventMenuSelected() {
        @Override
        public void selected(String id) {
            navegar(id);
        }
    });

    // Carga Escritorio por defecto y marca el menú
    navegar("4_1");
}

    /**
     * Recalcula el tamaño de menu1 (sidebar) y contenedor dentro de
     * panelBorder1 para que ocupen todo el alto/ancho disponible, en vez de
     * quedarse con las medidas fijas (870px) con las que se diseñó la pantalla.
     */
    private void ajustarLayout() {
        int ancho = panelBorder1.getWidth();
        int alto = panelBorder1.getHeight();

        if (ancho <= 0 || alto <= 0) {
            return;
        }

        // AbsoluteLayout no expone un metodo para reemplazar las constraints de un
        // componente ya agregado, asi que sacamos y volvemos a agregar cada uno con
        // las nuevas medidas (Container.add(comp, constraints) llama internamente a
        // addLayoutComponent, que si esta soportado por cualquier LayoutManager2).
        panelBorder1.remove(menu1);
        panelBorder1.add(menu1,
                new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, alto));

        panelBorder1.remove(contenedor);
        panelBorder1.add(contenedor,
                new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 0, ancho - 280, alto));

        panelBorder1.revalidate();
        panelBorder1.repaint();
    }

    /**
     * Reemplaza el contenido de {@code contenedor} por el panel recibido,
     * quitando lo que hubiera antes. Es el método que usa {@link #navegar}
     * para mostrar cada pantalla.
     */
    private void setForm(JComponent com) {
        contenedor.removeAll();
        contenedor.setLayout(new java.awt.BorderLayout());
        contenedor.add(com, java.awt.BorderLayout.CENTER);
        contenedor.revalidate();
        contenedor.repaint();
    }

    /**
     * Decide qué pantalla mostrar en el contenedor central según el id de
     * menú elegido en el sidebar ({@code menu1}). El caso "4" (Registrar
     * Resultados) tiene lógica extra: además de mostrar
     * {@link RegistrarResultados}, se suscribe a su
     * {@link RegistrarResultados.OrdenParaResultadosListener} para que,
     * cuando el usuario elige una orden, se abra la pantalla de carga de
     * resultados ya con el paciente y el examen correspondientes, y se
     * vuelva a esta misma pantalla ("4") al cancelar o al guardar. El caso
     * "10" no cambia de pantalla: cierra la sesión.
     */
    private void navegar(String id) {

        switch (id) {

            case "4_1":
                setForm(new Escritorio());
                break;

            case "5_1":
                setForm(new Pacientes());
                break;

            case "2":
                setForm(new Registros());
                break;

            case "3":
                setForm(new NuevoAnalisis());
                break;

            case "4": {
                RegistrarResultados pantallaRegistrar = new RegistrarResultados();
                pantallaRegistrar.addOrdenParaResultadosListener(new RegistrarResultados.OrdenParaResultadosListener() {
                    @Override
                    public void onOrdenSeleccionada(modelo.Paciente paciente, modelo.OrdenResumen orden) {
                        registrarResultados.cargarReultados pantallaCarga = new registrarResultados.cargarReultados();
                        pantallaCarga.cargarDatosPaciente(paciente, orden.getFecha());
                        pantallaCarga.cargarExamen(orden.getIdPedidoAnalisis(), orden.getIdAnalisisTipo(),
                                orden.getExamen(), paciente.getIdSexo());
                        pantallaCarga.addCancelarListener(new registrarResultados.cargarReultados.CancelarListener() {
                            @Override
                            public void onCancelar() {
                                navegar("4");
                            }
                        });
                        pantallaCarga.addGuardarListener(new registrarResultados.cargarReultados.GuardarListener() {
                            @Override
                            public void onGuardado() {
                                navegar("4");
                            }
                        });
                        setForm(pantallaCarga);
                    }
                });
                setForm(pantallaRegistrar);
                break;
            }

            case "5":
                setForm(new CatalogoExamnes());
                break;

            case "1":
                setForm(new Cotizacion());
                break;

            case "6":
                setForm(new ObraSocial());
                break;

            case "7":
                setForm(new Usuarios());
                break;

            case "8":
                setForm(new Estadisticas());
                break;

            case "9":
                setForm(new Configuracion());
                break;

            case "10":
                cerrarSesion();
                break;
        }
    }

    /**
     * Pide confirmación y, si el usuario acepta, cierra esta ventana y
     * abre la pantalla de {@link Login} para volver a autenticarse.
     */
    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Deseas cerrar sesión?",
            "Cerrar Sesión",
            JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            this.dispose();
            new Login().setVisible(true);
        }
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBorder1 = new panels.PanelBorder();
        menu1 = new panels.Menu();
        contenedor = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));
        setUndecorated(true);

        panelBorder1.setBackground(new java.awt.Color(246, 255, 249));
        panelBorder1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        panelBorder1.add(menu1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 870));

        contenedor.setBackground(new java.awt.Color(246, 255, 249));
        contenedor.setOpaque(false);
        contenedor.setLayout(new java.awt.BorderLayout());
        panelBorder1.add(contenedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 0, 1030, 870));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, 1314, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents


    public static void main(String args[]) {
     try {
        com.formdev.flatlaf.FlatLightLaf.setup();
    } catch (Exception ex) {
        System.err.println("Error al inicializar FlatLaf: " + ex.getMessage());
    }

    // 2. Iniciar la interfaz gráfica
    java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
            new Principal().setVisible(true);
        }
    });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contenedor;
    private panels.Menu menu1;
    private panels.PanelBorder panelBorder1;
    // End of variables declaration//GEN-END:variables
}
