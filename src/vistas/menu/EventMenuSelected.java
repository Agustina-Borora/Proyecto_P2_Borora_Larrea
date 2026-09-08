package vistas.menu;

import modelo.Model_Menu;
/**
 * Interfaz de eventos para gestionar las acciones al seleccionar un ítem del menú.
 */
public interface EventMenuSelected {

    /**
     * Se dispara automáticamente cuando el usuario hace clic en una opción del menú.
     *
     * @param id Identificador único del menú seleccionado (definido en Model_Menu, ej: "1", "2").
     */
    public void selected(String id);
}
