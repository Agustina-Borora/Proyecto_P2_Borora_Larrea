package panels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import menu.Model_Menu;
import menu.EventMenuSelected;

/**
 * Menú lateral (sidebar) de navegación de toda la aplicación. Arma, sobre
 * {@code listMenu1} ({@link menu.ListMenu}), la lista fija de secciones
 * (Escritorio, Pacientes, Registros, Nuevo Análisis, Registrar Resultados,
 * Catálogo de Exámenes, Cotización, Pagos, Usuarios, Estadísticas,
 * Configuración, Cerrar Sesión) usando los mismos ids de opción que
 * {@code Principal.navegar(String id)} espera para mostrar la pantalla
 * correspondiente. No navega por sí mismo: cuando el usuario elige una
 * opción, avisa a través de {@link EventMenuSelected} (registrado con
 * {@link #addEventMenuSelected}) y es Principal quien decide qué pantalla
 * mostrar.
 */
public class Menu extends javax.swing.JPanel {

    /**
     * Arma el panel, deja transparente el fondo (para que se vean las
     * esquinas redondeadas pintadas en {@link #paintChildren}) y carga las
     * opciones del menú.
     */
    public Menu() {
        initComponents();
        setOpaque(false);          // Permite bordes redondeados transparentes
        listMenu1.setOpaque(false);

        // Ajustes para evitar el marco y fondo gris predeterminado del JScrollPane
        jScrollPane1.setOpaque(false);
        jScrollPane1.getViewport().setOpaque(false);
        jScrollPane1.setBorder(null);

        init();
    }

    /**
     * Registra el listener que va a recibir el id de la opción elegida por
     * el usuario (ver {@link EventMenuSelected}); lo usa Principal para
     * decidir qué pantalla mostrar.
     */
    public void addEventMenuSelected(EventMenuSelected event) {
    listMenu1.addEventMenuSelected(event);
}

    /**
     * Carga en {@code listMenu1} las secciones fijas del menú (con sus ids,
     * títulos de grupo y separadores) y deja "Escritorio" seleccionado por
     * defecto al arrancar la aplicación.
     */
    private void init() {
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));
        listMenu1.addItem(new Model_Menu("", "Principal", Model_Menu.MenuType.TITLE));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));
        listMenu1.addItem(new Model_Menu("4_1", "Escritorio", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("5_1", "Pacientes", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("2", "Registros", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));

        listMenu1.addItem(new Model_Menu("", "Analisis", Model_Menu.MenuType.TITLE));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));
        listMenu1.addItem(new Model_Menu("3", "Nuevo Analisis", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("4", "Registrar Resultados", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("5", "Catalogo de Examenes", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("1", "Cotización", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));

        listMenu1.addItem(new Model_Menu("", "Administracion", Model_Menu.MenuType.TITLE));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));
        listMenu1.addItem(new Model_Menu("6", "Pagos", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("7", "Usuarios", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("8", "Estadisticas", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("9", "Configuracion", Model_Menu.MenuType.MENU));
        listMenu1.addItem(new Model_Menu("", " ", Model_Menu.MenuType.EMPTY));
        listMenu1.addItem(new Model_Menu("10", "Cerrar Sesion", Model_Menu.MenuType.MENU));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listMenu1.setSelectedIndex(3);
            }
        });
    }

  
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listMenu1 = new menu.ListMenu<>();

        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 24)); // NOI18N
        jLabel1.setText("San Gregorio");

        jLabel2.setBackground(new java.awt.Color(255, 255, 255));
        jLabel2.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        jLabel2.setText("Laboratorio de Analisis Clinicos");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        jScrollPane1.setViewportView(listMenu1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
/**
     * Dibuja el fondo blanco redondeado del panel lateral antes de pintar los componentes.
     */
    @Override
    protected void paintChildren(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        
        // Activa suavizado de bordes
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Color blanco para el menú
        g2.setColor(Color.decode("#FFFFFF"));
        
        // Cuerpo principal redondeado
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        
        // Rectángulo a la derecha para aplanar ese borde y acoplarlo a la pantalla
        g2.fillRect(getWidth() - 20, 0, 20, getHeight());
        
        g2.dispose(); // Libera la copia del Graphics2D
        
        super.paintChildren(grphcs);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private menu.ListMenu<String> listMenu1;
    // End of variables declaration//GEN-END:variables
}
