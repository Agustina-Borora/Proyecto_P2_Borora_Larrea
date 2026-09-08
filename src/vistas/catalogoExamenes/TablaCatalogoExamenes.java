
package vistas.catalogoExamenes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import vistas.acciones.TableActionCellEditor;
import vistas.acciones.TableActionCellRender;
import vistas.acciones.TableActionEvent;

/**
 * Tabla del catálogo de exámenes.
 *
 * @author agust
 */
public class TablaCatalogoExamenes extends javax.swing.JPanel {

    private static final Color VERDE_TEXTO = new Color(30, 92, 61);
    private static final Color VERDE_FONDO = new Color(228, 245, 234);
    private static final Color GRIS_TEXTO_SUAVE = new Color(95, 103, 99);
    private static final Color GRIS_ENCABEZADO_COL = new Color(140, 148, 144);
    private static final Color GRIS_TEXTO_FUERTE = new Color(20, 28, 25);
    private static final Color BORDE_TABLA = new Color(230, 233, 231);
    private static final Color FONDO_FILA_PAR = new Color(250, 252, 251);

    /**
     * Creates new form TablaCatalogoExamenes
     */
    public TablaCatalogoExamenes() {
        initComponents();
        ajustarContenedor();
        estilizarTabla();
    }

    /**
     * Deja que la tabla (y su scroll) ocupen todo el ancho disponible, en vez del tamaño fijo que
     * deja el editor de forms.
     */
    private void ajustarContenedor() {
        setLayout(new BorderLayout());
        add(TablaExamenes, BorderLayout.CENTER);
        TablaExamenes.setPreferredSize(new Dimension(10, 460));
        quitarBordesDelScroll();
    }

    /**
     * JScrollPane trae bordes propios del look-and-feel (el del componente y el del viewport).
     */
    private void quitarBordesDelScroll() {
        TablaExamenes.setBorder(null);
        TablaExamenes.setViewportBorder(null);
        jTable1.setBorder(null);

        TablaExamenes.addPropertyChangeListener("border", evt -> {
            if (TablaExamenes.getBorder() != null) {
                TablaExamenes.setBorder(null);
            }
        });
        TablaExamenes.addPropertyChangeListener("viewportBorder", evt -> {
            if (TablaExamenes.getViewportBorder() != null) {
                TablaExamenes.setViewportBorder(null);
            }
        });
    }

    /**
     * Tamaño de columnas a medida, estilo acorde al resto de la pantalla (colores, tipografía) y
     * oculta la columna Id -- sigue en el modelo, solo no se muestra.
     */
    private void estilizarTabla() {
        vaciarTabla();

        jTable1.setRowHeight(46);
        jTable1.setShowGrid(false);
        jTable1.setIntercellSpacing(new Dimension(0, 0));
        jTable1.setSelectionBackground(VERDE_FONDO);
        jTable1.setSelectionForeground(GRIS_TEXTO_FUERTE);
        jTable1.setFont(jTable1.getFont().deriveFont(Font.PLAIN, 13f));
        jTable1.setBackground(Color.WHITE);
        jTable1.setFillsViewportHeight(true);

        JTableHeader encabezado = jTable1.getTableHeader();
        encabezado.setDefaultRenderer(new RenderizadorEncabezado());
        encabezado.setPreferredSize(new Dimension(encabezado.getPreferredSize().width, 40));
        encabezado.setReorderingAllowed(false);
        encabezado.setResizingAllowed(true);

        jTable1.getColumnModel().getColumn(0).setPreferredWidth(90);   // Codigo
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(280);  // Nombre
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(170);  // Unidad Bioquimica
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(110);  // Estado
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(110);  // Accion

        jTable1.getColumnModel().getColumn(0).setCellRenderer(new RenderizadorGeneral(SwingConstants.LEFT, true));
        jTable1.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorGeneral(SwingConstants.LEFT, false));
        jTable1.getColumnModel().getColumn(2).setCellRenderer(new RenderizadorGeneral(SwingConstants.LEFT, true));
        jTable1.getColumnModel().getColumn(3).setCellRenderer(new RenderizadorEstado());
        configurarColumnaAccion();

        TableColumn columnaId = jTable1.getColumnModel().getColumn(5);
        jTable1.getColumnModel().removeColumn(columnaId);
    }

    /**
     * Instala en la columna "Accion" el mismo panel de ver/editar/eliminar que usa
     * {@code TablaPacientes}, sin el botón de compartir.
     */
    private void configurarColumnaAccion() {
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                System.out.println("Ver examen fila: " + row);
            }

            @Override
            public void onEdit(int row) {
                System.out.println("Editar examen fila: " + row);
            }

            @Override
            public void onDelete(int row) {
                System.out.println("Eliminar examen fila: " + row);
            }

            @Override
            public void onShare(int row) {
                // No aplica: el botón compartir está oculto en esta tabla.
            }
        };

        jTable1.getColumnModel().getColumn(4).setCellRenderer(new TableActionCellRender(true, true, false, true));
        jTable1.getColumnModel().getColumn(4).setCellEditor(new TableActionCellEditor(event, true, true, false, true));
    }

    /**
     * Elimina las filas de ejemplo que deja el modelo por defecto generado por el editor de
     * formularios.
     */
    private void vaciarTabla() {
        DefaultTableModel modelo = (DefaultTableModel) jTable1.getModel();
        modelo.setRowCount(0);
    }

    private static Color fondoFila(boolean isSelected, int row) {
        if (isSelected) {
            return VERDE_FONDO;
        }
        return row % 2 == 0 ? Color.WHITE : FONDO_FILA_PAR;
    }

    // --- Renderers ---
    private class RenderizadorEncabezado extends DefaultTableCellRenderer {

        RenderizadorEncabezado() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(true);
            setBackground(new Color(250, 251, 250));
            setForeground(GRIS_ENCABEZADO_COL);
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDE_TABLA), new EmptyBorder(0, 12, 0, 12)));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString().toUpperCase());
            return this;
        }
    }

    private class RenderizadorGeneral extends DefaultTableCellRenderer {

        private final int alineacion;
        private final boolean textoSuave;

        RenderizadorGeneral(int alineacion, boolean textoSuave) {
            this.alineacion = alineacion;
            this.textoSuave = textoSuave;
            setBorder(new EmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alineacion);
            setForeground(textoSuave ? GRIS_TEXTO_SUAVE : GRIS_TEXTO_FUERTE);
            setFont(getFont().deriveFont(textoSuave ? Font.PLAIN : Font.BOLD, 13f));
            setBackground(fondoFila(isSelected, row));
            return this;
        }
    }

    /**
     * Píldora redondeada para el estado; primero se pinta el fondo completo de la fila, para
     * mantener el patrón de filas alternadas, y luego la píldora encima.
     */
    private class RenderizadorEstado extends JLabel implements TableCellRenderer {

        private Color fondoFilaActual = Color.WHITE;

        RenderizadorEstado() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(getFont().deriveFont(Font.PLAIN, 11.5f));
            setBorder(new EmptyBorder(0, 26, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(fondoFilaActual);
            g2.fillRect(0, 0, getWidth(), getHeight());

            java.awt.FontMetrics fm = g2.getFontMetrics(getFont());
            int anchoPill = fm.stringWidth(getText()) + 24;
            int altoPill = 22;
            int y = (getHeight() - altoPill) / 2;
            g2.setColor(getBackground());
            g2.fillRoundRect(14, y, anchoPill, altoPill, altoPill, altoPill);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            boolean activo = "Activo".equals(value);
            setBackground(activo ? VERDE_FONDO : new Color(240, 241, 239));
            setForeground(activo ? VERDE_TEXTO : GRIS_TEXTO_SUAVE);
            fondoFilaActual = fondoFila(isSelected, row);
            return this;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TablaExamenes = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setBackground(new java.awt.Color(246, 255, 249));

        TablaExamenes.setBackground(new java.awt.Color(246, 255, 249));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Codigo", "Nombre", "Unidad Bioquimica", "Estado", "Accion", "Id"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, true, true, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        TablaExamenes.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(3).setResizable(false);
        }

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TablaExamenes, javax.swing.GroupLayout.DEFAULT_SIZE, 978, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TablaExamenes, javax.swing.GroupLayout.PREFERRED_SIZE, 559, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane TablaExamenes;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
