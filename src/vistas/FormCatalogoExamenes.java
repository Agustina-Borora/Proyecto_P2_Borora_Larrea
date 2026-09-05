/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * Pantalla "Catálogo de Exámenes".
 *
 * Los componentes de esta pantalla (título, subtítulo, barra de búsqueda,
 * botón y tabla) fueron ubicados mediante el editor visual de NetBeans; el
 * método {@code initComponents()} generado por dicho editor se mantiene sin
 * modificaciones. Esta clase se limita a aplicar el estilo visual sobre los
 * componentes ya existentes una vez inicializados, sin alterar su estructura
 * ni el layout definido en el formulario.
 *
 * Etapa actual: maquetado visual únicamente. La pantalla todavía no está
 * conectada a la base de datos; los datos de ejemplo mostrados en la tabla
 * provienen de {@link TablaCatalogoExamenes}.
 *
 * @author agust
 */
public class FormCatalogoExamenes extends javax.swing.JPanel {

    /** Paleta de colores de esta pantalla, compartida con {@link TablaCatalogoExamenes}. */
    private static final Color VERDE_PRINCIPAL = new Color(30, 92, 61);
    private static final Color GRIS_TEXTO_SUAVE = new Color(110, 120, 116);
    private static final Color GRIS_TEXTO_FUERTE = new Color(20, 28, 25);

    /**
     * Textos de la fila de filtro mientras no esté conectada a la base de
     * datos. {@code TEXTO_CONTADOR_PLACEHOLDER} deberá reemplazarse por la
     * cantidad real de exámenes obtenida del DAO correspondiente.
     */
    private static final String TEXTO_FILTRO_DEFECTO = "Total de Exámenes";
    private static final String TEXTO_CONTADOR_PLACEHOLDER = "0";

    /**
     * Creates new form CatalogoExamenes
     */
    public FormCatalogoExamenes() {
        initComponents();
        aplicarEstilos();
    }

    /**
     * Aplica el estilo visual (tipografía, colores y bordes) a los
     * componentes generados por el editor de formularios. No agrega, quita
     * ni reposiciona componentes: esas responsabilidades quedan a cargo del
     * editor visual de NetBeans y de {@code initComponents()}.
     */
    private void aplicarEstilos() {
        setBackground(new Color(246, 255, 249));
        estilizarTitulo();
        estilizarSubtitulo();
        estilizarBoton();
        estilizarFiltro();
    }

    private void estilizarTitulo() {
        jLabel1.setFont(jLabel1.getFont().deriveFont(Font.BOLD, 24f));
        jLabel1.setForeground(GRIS_TEXTO_FUERTE);
    }

    private void estilizarSubtitulo() {
        jLabel2.setFont(jLabel2.getFont().deriveFont(Font.PLAIN, 13f));
        jLabel2.setForeground(GRIS_TEXTO_SUAVE);
    }

    /**
     * Estiliza el botón "+ Nuevo Examen". El color de fondo y de texto se
     * asignan mediante la API estándar de Swing para asegurar que se vean
     * correctamente sin importar el Look and Feel activo; el redondeado de
     * bordes se aplica como mejora visual adicional a través de la
     * propiedad de estilo de FlatLaf (mismo mecanismo utilizado en
     * {@link panels.Busqueda} para la barra de búsqueda).
     */
    private void estilizarBoton() {
        jButton1.setBackground(VERDE_PRINCIPAL);
        jButton1.setForeground(Color.WHITE);
        jButton1.setContentAreaFilled(true);
        jButton1.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14;"
                + "borderWidth: 0;"
                + "focusWidth: 0"
        );
        jButton1.setFont(jButton1.getFont().deriveFont(Font.BOLD, 13f));
        jButton1.setMargin(new Insets(10, 22, 10, 22));
        jButton1.setFocusPainted(false);
        jButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Estiliza la fila de filtro (etiqueta descriptiva, contador y combo de
     * categorías). Esta fila es, por el momento, únicamente maquetado
     * visual: el combo todavía no filtra la tabla y el contador no refleja
     * datos reales, ya que ambos quedarán conectados a la base de datos en
     * una etapa posterior del desarrollo.
     */
    private void estilizarFiltro() {
        jLabel3.setText(TEXTO_FILTRO_DEFECTO);
        jLabel3.setFont(jLabel3.getFont().deriveFont(Font.PLAIN, 13f));
        jLabel3.setForeground(GRIS_TEXTO_SUAVE);

        jLabel4.setText(TEXTO_CONTADOR_PLACEHOLDER);
        jLabel4.setFont(jLabel4.getFont().deriveFont(Font.BOLD, 15f));
        jLabel4.setForeground(GRIS_TEXTO_FUERTE);

        jComboBox1.setFont(jComboBox1.getFont().deriveFont(Font.PLAIN, 13f));
        jComboBox1.putClientProperty(FlatClientProperties.STYLE,
                "arc: 10;"
                + "borderColor: #DCE1E6;"
                + "focusedBorderColor: #1E513B"
        );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tablaCatalogoExamenes1 = new vistas.TablaCatalogoExamenes();
        jButton1 = new javax.swing.JButton();
        busqueda1 = new panels.Busqueda();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();

        setBackground(new java.awt.Color(246, 255, 249));

        jButton1.setText("+Nuevo Examen");

        jLabel1.setText("Catalogo de Examnes ");

        jLabel2.setText("Todos los examnes  y sus unidades ");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE))
                .addGap(21, 21, 21))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(tablaCatalogoExamenes1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(busqueda1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(busqueda1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(tablaCatalogoExamenes1, javax.swing.GroupLayout.PREFERRED_SIZE, 569, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private panels.Busqueda busqueda1;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private vistas.TablaCatalogoExamenes tablaCatalogoExamenes1;
    // End of variables declaration//GEN-END:variables
}
