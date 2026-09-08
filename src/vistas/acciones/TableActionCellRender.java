package vistas.acciones;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Dibuja el {@link PanelAction} dentro de la celda de la columna "Accion" cuando la fila NO está
 * siendo editada (o sea, en todas las filas salvo aquella sobre la que se hizo clic recién).
 */
public class TableActionCellRender extends DefaultTableCellRenderer {

    private final boolean mostrarVer;
    private final boolean mostrarEditar;
    private final boolean mostrarCompartir;
    private final boolean mostrarEliminar;

    public TableActionCellRender() {
        this(true, true, true, true);
    }

    /**
     * @param mostrarVer ver {@link PanelAction#PanelAction(boolean, boolean, boolean, boolean)}.
     */
    public TableActionCellRender(boolean mostrarVer, boolean mostrarEditar, boolean mostrarCompartir, boolean mostrarEliminar) {
        this.mostrarVer = mostrarVer;
        this.mostrarEditar = mostrarEditar;
        this.mostrarCompartir = mostrarCompartir;
        this.mostrarEliminar = mostrarEliminar;
    }

    /**
     * Devuelve un {@link PanelAction} nuevo como componente de la celda, en lugar del texto por
     * defecto de {@link DefaultTableCellRenderer}.
     */
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean isSeleted, boolean bln1, int row, int column) {
        Component com = super.getTableCellRendererComponent(jtable, o, isSeleted, bln1, row, column);
        PanelAction action = new PanelAction(mostrarVer, mostrarEditar, mostrarCompartir, mostrarEliminar);
        if (isSeleted == false && row % 2 == 0) {
            action.setBackground(Color.WHITE);
        } else {
            action.setBackground(com.getBackground());
        }
        return action;
    }
}
