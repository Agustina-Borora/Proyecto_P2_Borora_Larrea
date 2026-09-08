package vistas.acciones;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 * Hace que los botones de {@link PanelAction} realmente reaccionen al clic.
 */
public class TableActionCellEditor extends DefaultCellEditor {

    /**
     * Callback de la pantalla que implementa qué hacer al ver/editar/eliminar cada fila.
     */
    private TableActionEvent event;

    private final boolean mostrarVer;
    private final boolean mostrarEditar;
    private final boolean mostrarCompartir;
    private final boolean mostrarEliminar;

    /**
     * recibe los clics de ver/editar/eliminar de cada fila.
     *
     * @param event implementación (de la pantalla dueña de la tabla) que
     */
    public TableActionCellEditor(TableActionEvent event) {
        this(event, true, true, true, true);
    }

    /**
     * @param mostrarVer ver {@link PanelAction#PanelAction(boolean, boolean, boolean, boolean)}.
     */
    public TableActionCellEditor(TableActionEvent event, boolean mostrarVer, boolean mostrarEditar, boolean mostrarCompartir, boolean mostrarEliminar) {
        super(new JCheckBox());
        this.event = event;
        this.mostrarVer = mostrarVer;
        this.mostrarEditar = mostrarEditar;
        this.mostrarCompartir = mostrarCompartir;
        this.mostrarEliminar = mostrarEliminar;
    }

    /**
     * Se invoca cuando el usuario hace clic sobre la celda de acciones de una fila.
     */
    @Override
    public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int row, int column) {
        PanelAction action = new PanelAction(mostrarVer, mostrarEditar, mostrarCompartir, mostrarEliminar);
        action.initEvent(event, row);
        // Fondo blanco fijo en vez del selectionBackground de la tabla: ese color quedaba gris y
        // no acompañaba el resto de la fila (blanca).
        action.setBackground(java.awt.Color.WHITE);
        return action;
    }
}
