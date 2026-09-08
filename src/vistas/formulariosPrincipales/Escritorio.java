
package vistas.formulariosPrincipales;

import java.awt.Color;
import modelo.EstadisticasEscritorio;

/**
 * Panel de inicio (dashboard) que se muestra por defecto al entrar a la aplicación.
 */
public class Escritorio extends javax.swing.JPanel {

    /**
     * Se dispara cuando, desde el detalle de una orden de {@code tablaEscritorio2}, el usuario
     * aprieta "Cargar Resultados". Este panel no navega por su cuenta -no conoce a Principal ni
     * a las otras pantallas-, solo reenvía el aviso hacia quien lo contenga.
     */
    public interface CargarResultadosListener {
        void onCargarResultados();
    }

    private final java.util.List<CargarResultadosListener> listenersCargarResultados = new java.util.ArrayList<>();

    public void addCargarResultadosListener(CargarResultadosListener listener) {
        listenersCargarResultados.add(listener);
    }

    /**
     * Arma la pantalla: define el color de texto de cada tarjeta de estadísticas, dispara la carga
     * de datos del mes ({@link #cargarDatos()}) y reemplaza el {@code LayoutManager} de {@code
     * panelBorder2} por uno a medida (ver comentario más abajo) para que las tarjetas y la tabla
     * de últimas órdenes se reacomoden al ancho real del panel sin parpadeo.
     */
    public Escritorio() {
        initComponents();

        tablaEscritorio2.addCargarResultadosListener(() -> {
            for (CargarResultadosListener listener : listenersCargarResultados) {
                listener.onCargarResultados();
            }
        });
        // 1. Tarjeta Azul
        card1.setTextColor(new Color(13, 110, 253)); // Azul fuerte para el texto

        // 2. Tarjeta Verde
        card2.setTextColor(new Color(25, 135, 84));  // Verde fuerte para el texto

        // 3. Tarjeta Naranja
        card3.setTextColor(new Color(202, 126, 8));  // Naranja fuerte para el texto

        // 4. Tarjeta Roja
        card4.setTextColor(new Color(220, 53, 69));  // Rojo fuerte para el texto

        cargarDatos();

        panelBorder2.setLayout(new java.awt.LayoutManager() {
            @Override
            public void addLayoutComponent(String name, java.awt.Component comp) {
            }

            @Override
            public void removeLayoutComponent(java.awt.Component comp) {
            }

            @Override
            public java.awt.Dimension preferredLayoutSize(java.awt.Container parent) {
                return new java.awt.Dimension(982, 803);
            }

            @Override
            public java.awt.Dimension minimumLayoutSize(java.awt.Container parent) {
                return new java.awt.Dimension(300, 400);
            }

            @Override
            public void layoutContainer(java.awt.Container parent) {
                int ancho = parent.getWidth();
                int alto = parent.getHeight();
                if (ancho <= 0 || alto <= 0) {
                    return;
                }

                jPanel2.setBounds(12, 29, Math.max(ancho - 24, 0), 190);

                java.awt.Dimension tituloTam = jLabel1.getPreferredSize();
                jLabel1.setBounds(22, 232, tituloTam.width, tituloTam.height);

                tablaEscritorio2.setBounds(10, 280, Math.max(ancho - 20, 0), Math.max(alto - 290, 0));
            }
        });
    }

    /**
     * Trae las estadísticas del mes (EscritorioDAO) y las vuelca en las 4 tarjetas; la tabla de
     * últimas órdenes se carga sola en su propio constructor (tablaEscritorio2.cargarDatos()).
     */
    private void cargarDatos() {
        EstadisticasEscritorio stats = controlador.EscritorioController.obtenerEstadisticasDelMes(this);

        card1.setTitle("Total del mes");
        card1.setValues(String.valueOf(stats.getTotalMes()));
        card1.setDescription("Análisis registrados");

        card2.setTitle("Emitidas");
        card2.setValues(String.valueOf(stats.getEmitidas()));
        card2.setDescription("Con resultado cargado");

        card3.setTitle("En proceso");
        card3.setValues(String.valueOf(stats.getEnProceso()));
        card3.setDescription("En laboratorio");

        card4.setTitle("Pendientes");
        card4.setValues(String.valueOf(stats.getPendientes()));
        card4.setDescription("Sin iniciar");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBorder2 = new vistas.panels.PanelBorder();
        jPanel2 = new javax.swing.JPanel();
        card1 = new vistas.escritorio.Card();
        card2 = new vistas.escritorio.Card();
        card3 = new vistas.escritorio.Card();
        card4 = new vistas.escritorio.Card();
        jLabel1 = new javax.swing.JLabel();
        tablaEscritorio2 = new vistas.escritorio.TablaEscritorio();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        panelBorder2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(250, 255, 250));
        jPanel2.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        card1.setColor1(new java.awt.Color(207, 226, 255));
        card1.setColor2(new java.awt.Color(207, 226, 255));
        jPanel2.add(card1);

        card2.setColor1(new java.awt.Color(209, 231, 221));
        card2.setColor2(new java.awt.Color(209, 231, 221));
        jPanel2.add(card2);

        card3.setColor1(new java.awt.Color(255, 243, 205));
        card3.setColor2(new java.awt.Color(255, 243, 205));
        jPanel2.add(card3);

        card4.setColor1(new java.awt.Color(248, 215, 218));
        card4.setColor2(new java.awt.Color(248, 215, 218));
        jPanel2.add(card4);

        panelBorder2.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(12, 29, 957, 190));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel1.setText("Ultimas Ordenes");
        panelBorder2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(22, 232, -1, -1));
        panelBorder2.add(tablaEscritorio2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 280, 960, 510));

        add(panelBorder2);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.escritorio.Card card1;
    private vistas.escritorio.Card card2;
    private vistas.escritorio.Card card3;
    private vistas.escritorio.Card card4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel2;
    private vistas.panels.PanelBorder panelBorder2;
    private vistas.escritorio.TablaEscritorio tablaEscritorio2;
    // End of variables declaration//GEN-END:variables
}
