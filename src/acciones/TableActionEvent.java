package acciones;

/**
 * Callback que implementa cada pantalla (TablaPacientes, etc.) para decidir
 * qué pasa cuando se aprieta cada uno de los 3 botones de una fila.
 */
public interface TableActionEvent {

    /** Se dispara al presionar el botón editar de la fila indicada. */
    public void onEdit(int row);

    /** Se dispara al presionar el botón eliminar de la fila indicada. */
    public void onDelete(int row);

    /** Se dispara al presionar el botón ver de la fila indicada. */
    public void onView(int row);
}
