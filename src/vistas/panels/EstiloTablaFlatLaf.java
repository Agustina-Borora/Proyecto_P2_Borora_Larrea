package vistas.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Estilo visual FlatLaf compartido por las tablas del sistema: fondo blanco, bordes sutiles,
 * encabezado en negrita, resaltado de fila al pasar el mouse (hover) y scrollbars tipo
 * "píldora".
 */
public final class EstiloTablaFlatLaf {

    private EstiloTablaFlatLaf() {
    }

    /**
     * Aplica el estilo completo a una tabla y a su JScrollPane contenedor.
     */
    public static void aplicar(JTable tabla, JScrollPane scroll) {
        final int[] filaHover = {-1};

        tabla.setBackground(Color.WHITE);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        tabla.setBackground(Color.WHITE);
        tabla.setForeground(new Color(50, 50, 50)); // Texto principal oscuro

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(238, 242, 246)); // Líneas divisoras sutiles

        tabla.setRowHeight(42);
        tabla.setIntercellSpacing(new Dimension(0, 1));

        // Estilo FlatLaf: Selección en un azul neutro suave
        tabla.putClientProperty(
                "FlatLaf.style",
                "background: #FFFFFF; "
                + "selectionBackground: #EBF3FE; "
                + "selectionForeground: #1E293B;"
        );

        JTableHeader header = tabla.getTableHeader();
        header.setReorderingAllowed(false);
        header.setOpaque(true);

        // Encabezado más alto (48px)
        header.setPreferredSize(new Dimension(0, 48));

        header.setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240))
        );

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setBackground(Color.WHITE);
                label.setForeground(new Color(51, 65, 85));
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
                label.setHorizontalAlignment(SwingConstants.LEFT);

                return label;
            }
        });

        tabla.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                if (fila != filaHover[0]) {
                    filaHover[0] = fila;
                    tabla.repaint();
                }
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                filaHover[0] = -1;
                tabla.repaint();
            }
        });

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                boolean filaConDatos = filaTieneDatos(table, row);

                if (isSelected) {
                    c.setBackground(new Color(235, 243, 254)); // Selección azul clara
                } else if (row == filaHover[0] && filaConDatos) {
                    c.setBackground(new Color(241, 245, 249)); // Hover gris suave
                } else {
                    c.setBackground(Color.WHITE);
                }

                ((JComponent) c).setForeground(new Color(51, 65, 85));

                return c;
            }
        });

        // ===== Personalización moderna del scrollbar (estilo FlatLaf) =====
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));

        scroll.getVerticalScrollBar().putClientProperty("FlatLaf.style",
                "trackArc: 999; "
                + "thumbArc: 999; "
                + "track: #FFFFFF; "
                + "thumb: #CBD5E1; "
                + "hoverThumbColor: #94A3B8; "
                + "pressedThumbColor: #64748B; "
                + "showButtons: false"
        );

        scroll.getHorizontalScrollBar().putClientProperty("FlatLaf.style",
                "trackArc: 999; "
                + "thumbArc: 999; "
                + "track: #FFFFFF; "
                + "thumb: #CBD5E1; "
                + "hoverThumbColor: #94A3B8; "
                + "pressedThumbColor: #64748B; "
                + "showButtons: false"
        );
    }

    /**
     * true si al menos una celda de la fila tiene un valor no vacío.
     */
    public static boolean filaTieneDatos(JTable table, int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return false;
        }
        for (int col = 0; col < table.getColumnCount(); col++) {
            Object val = table.getValueAt(row, col);
            if (val != null && !val.toString().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
