/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Pantalla "Detalle de Orden": muestra los datos del paciente y del análisis
 * solicitado para una orden puntual, junto con los resultados a cargar.
 *
 * Los componentes (título, secciones "Paciente" y "Analisis", badges de
 * cabecera, tabla y botones) fueron ubicados mediante el editor visual de
 * NetBeans con posicionamiento absoluto; el método {@code initComponents()}
 * generado por dicho editor se mantiene sin modificaciones. Esta clase se
 * limita a aplicar estilo visual sobre los componentes ya existentes una vez
 * inicializados, sin alterar su estructura ni las posiciones definidas en el
 * formulario.
 *
 * Etapa actual: maquetado visual únicamente. La pantalla todavía no está
 * conectada a la base de datos: ni los datos del paciente/análisis ni los
 * badges de estado y tipo de cobertura tienen datos reales todavía. Los
 * badges muestran un valor de muestra únicamente a fines de maquetado (ver
 * {@link #estilizarBadges()}).
 *
 * @author agust
 */
public class EscritorioDetalledeOrden extends javax.swing.JPanel {

    /** Paleta de colores de esta pantalla, consistente con el resto del sistema. */
    private static final Color VERDE_PRINCIPAL = new Color(30, 92, 61);
    private static final Color VERDE_FONDO = new Color(228, 245, 234);
    private static final Color GRIS_TEXTO_FUERTE = new Color(20, 28, 25);
    private static final Color BORDE_SUAVE = new Color(230, 233, 231);

    /** Colores del badge de estado (por ejemplo "Pendiente", "Entregado"). */
    private static final Color AMBAR_FONDO_BADGE = new Color(253, 236, 210);
    private static final Color AMBAR_TEXTO_BADGE = new Color(158, 101, 20);

    /** Colores del badge de tipo de cobertura (por ejemplo "Particular", "Obra Social"). */
    private static final Color GRIS_FONDO_BADGE = new Color(237, 239, 238);
    private static final Color GRIS_TEXTO_BADGE = new Color(90, 98, 94);

    /**
     * Valores de muestra para los badges de cabecera. El estado real de la
     * orden y su tipo de cobertura provienen de la base de datos; mientras
     * esta pantalla no esté conectada, se muestran estos valores fijos solo
     * para poder maquetar y revisar el estilo visual.
     */
    private static final String TEXTO_BADGE_ESTADO_DEFECTO = "Pendiente";
    private static final String TEXTO_BADGE_TIPO_DEFECTO = "Particular";

    /**
     * Creates new form EscritorioDetalledeOrden
     */
    public EscritorioDetalledeOrden() {
        initComponents();
        aplicarEstilos();
    }

    /** Aplica el estilo visual de la pantalla sobre los componentes ya inicializados. */
    private void aplicarEstilos() {
        estilizarBadges();
        estilizarTabla();
        estilizarBotones();
    }

    /**
     * Da formato de "badge" (etiqueta con fondo de color y esquinas
     * redondeadas) a los dos indicadores de la cabecera: el estado de la
     * orden y su tipo de cobertura.
     */
    private void estilizarBadges() {
        estilizarBadge(jLabel6, TEXTO_BADGE_ESTADO_DEFECTO, AMBAR_FONDO_BADGE, AMBAR_TEXTO_BADGE);
        estilizarBadge(jLabel5, TEXTO_BADGE_TIPO_DEFECTO, GRIS_FONDO_BADGE, GRIS_TEXTO_BADGE);
    }

    /**
     * Aplica el estilo de badge a una etiqueta puntual: texto centrado,
     * fondo de color y esquinas redondeadas.
     *
     * @param badge etiqueta a estilizar
     * @param texto texto de muestra a mostrar mientras no haya datos reales
     * @param colorFondo color de fondo del badge
     * @param colorTexto color del texto del badge
     */
    private void estilizarBadge(javax.swing.JLabel badge, String texto, Color colorFondo, Color colorTexto) {
        badge.setText(texto);
        badge.setOpaque(true);
        badge.setBackground(colorFondo);
        badge.setForeground(colorTexto);
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        badge.setBorder(new javax.swing.border.EmptyBorder(4, 10, 4, 10));
        // Esquinas redondeadas cuando la aplicación corre con el tema FlatLaf
        // del proyecto; sin efecto si el Look & Feel no lo soporta.
        badge.putClientProperty(FlatClientProperties.STYLE, "arc: 14");
    }

    /**
     * Armoniza la apariencia de la tabla de exámenes con el resto del
     * sistema (ver {@link TablaCatalogoExamenes}): sin líneas de grilla,
     * encabezado resaltado y selección en verde suave.
     *
     * Nota: el modelo de la tabla todavía no define columnas ni carga datos
     * reales, ya que eso depende de la conexión con la base de datos.
     */
    private void estilizarTabla() {
        jTable1.setRowHeight(30);
        jTable1.setShowGrid(false);
        jTable1.setIntercellSpacing(new Dimension(0, 0));
        jTable1.setFont(new Font("SansSerif", Font.PLAIN, 13));
        jTable1.setForeground(GRIS_TEXTO_FUERTE);
        jTable1.setSelectionBackground(VERDE_FONDO);
        jTable1.setSelectionForeground(GRIS_TEXTO_FUERTE);
        jTable1.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        jTable1.getTableHeader().setBackground(VERDE_FONDO);
        jTable1.getTableHeader().setForeground(VERDE_PRINCIPAL);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(BORDE_SUAVE));
        jScrollPane1.getViewport().setBackground(Color.WHITE);
    }

    /**
     * Da estilo a los botones de acción: "Cargar Resultados" como acción
     * principal (verde, relleno) y "Cerrar" como acción secundaria (borde
     * suave, fondo blanco).
     */
    private void estilizarBotones() {
        jButton1.setBackground(VERDE_PRINCIPAL);
        jButton1.setForeground(Color.WHITE);
        jButton1.setContentAreaFilled(true);
        jButton1.setFocusPainted(false);
        jButton1.setFont(jButton1.getFont().deriveFont(Font.BOLD, 13f));
        jButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jButton1.putClientProperty(FlatClientProperties.STYLE, "arc: 12; borderWidth: 0; focusWidth: 0");

        jButton2.setBackground(Color.WHITE);
        jButton2.setForeground(GRIS_TEXTO_FUERTE);
        jButton2.setContentAreaFilled(true);
        jButton2.setFocusPainted(false);
        jButton2.setFont(jButton2.getFont().deriveFont(Font.BOLD, 13f));
        jButton2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jButton2.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 1; borderColor: #DCE1E6; focusWidth: 0");
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
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 30)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(51, 102, 0));
        jLabel1.setText("Detalle de Orden ");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(12, 28, -1, -1));

        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 30)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(51, 102, 0));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(265, 28, 106, 39));

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(77, 104, 21));
        jLabel4.setText("Edad");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(748, 197, 60, 37));
        add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(812, 77, 90, 44));
        add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(12, 77, 140, 37));

        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 22)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(51, 102, 0));
        jLabel2.setText("Paciente");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 128, -1, -1));

        jLabel7.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(77, 104, 21));
        jLabel7.setText("Apellido y Nombre");
        add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 197, 209, 37));

        jLabel8.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(77, 104, 21));
        jLabel8.setText("Email");
        add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 274, 65, 38));

        jLabel9.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(77, 104, 21));
        jLabel9.setText("D.N.I");
        add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(176, 197, 60, 37));

        jLabel10.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(77, 104, 21));
        jLabel10.setText("Celular");
        add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(176, 275, 91, 37));

        jLabel11.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(77, 104, 21));
        jLabel11.setText("Medico Derivante");
        add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(748, 274, -1, -1));

        jLabel12.setFont(new java.awt.Font("SansSerif", 1, 22)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(51, 102, 0));
        jLabel12.setText("Analisis");
        add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 371, 98, 39));

        jLabel13.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(77, 104, 21));
        jLabel13.setText("Fecha");
        add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 430, 108, 37));

        jLabel14.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(77, 104, 21));
        jLabel14.setText("Examenes");
        add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(176, 430, 108, 37));

        jLabel15.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(77, 104, 21));
        jLabel15.setText("Observacion");
        add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(587, 430, 142, 37));

        jLabel16.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(176, 235, 82, 26));

        jLabel17.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(748, 235, 63, 26));

        jLabel18.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(176, 312, 148, 26));

        jLabel19.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 235, 306, 26));

        jLabel20.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(748, 318, 191, 26));

        jLabel21.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 312, 247, 32));

        jLabel22.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(587, 467, 327, 34));

        jLabel23.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(51, 51, 51));
        add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(381, 467, 151, 34));

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(jTable1);

        add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 470, 190, 120));

        jButton1.setText("Cargar Resultados");
        add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 650, 200, 60));

        jButton2.setText("Cerrar");
        add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 650, 170, 60));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
