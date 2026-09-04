package acciones;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Dibuja el {@link PanelAction} (los 3 botones) dentro de la celda de la
 * columna "Accion" cuando la fila NO está siendo editada (o sea, en todas
 * las filas salvo aquella sobre la que se hizo clic recién).
 */
public class TableActionCellRender extends DefaultTableCellRenderer {

    /**
     * Devuelve un {@link PanelAction} nuevo como componente de la celda,
     * en lugar del texto por defecto de {@link DefaultTableCellRenderer}.
     * El fondo se ajusta según si la fila está seleccionada y si es par o
     * impar, para mantener el efecto "zebra" del resto de la tabla.
     */
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean isSeleted, boolean bln1, int row, int column) {
        Component com = super.getTableCellRendererComponent(jtable, o, isSeleted, bln1, row, column);
        PanelAction action = new PanelAction();
        if (isSeleted == false && row % 2 == 0) {
            action.setBackground(Color.WHITE);
        } else {
            action.setBackground(com.getBackground());
        }
        return action;
    }
}
