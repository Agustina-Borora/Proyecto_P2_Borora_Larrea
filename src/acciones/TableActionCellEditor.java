package acciones;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 * Hace que los botones de {@link PanelAction} realmente reaccionen al clic.
 * En un JTable, una celda solo recibe eventos de mouse en sus componentes
 * internos si esa celda está "en edición" - por eso esta clase, aunque
 * técnicamente es un CellEditor, en la práctica solo sirve para habilitar
 * los clics de los 3 botones.
 */
public class TableActionCellEditor extends DefaultCellEditor {

    /** Callback de la pantalla que implementa qué hacer al ver/editar/eliminar cada fila. */
    private TableActionEvent event;

    /**
     * @param event implementación (de la pantalla dueña de la tabla) que
     *              recibe los clics de ver/editar/eliminar de cada fila.
     */
    public TableActionCellEditor(TableActionEvent event) {
        super(new JCheckBox());
        this.event = event;
    }

    /**
     * Se invoca cuando el usuario hace clic sobre la celda de acciones de una
     * fila. Crea un {@link PanelAction} nuevo, lo conecta al {@link #event}
     * para esa fila puntual y lo devuelve para que JTable lo use como
     * componente "en edición" de la celda (así los botones responden al clic).
     */
    @Override
    public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int row, int column) {
        PanelAction action = new PanelAction();
        action.initEvent(event, row);
        action.setBackground(jtable.getSelectionBackground());
        return action;
    }
}
