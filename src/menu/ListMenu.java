package menu;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

/**
 * Componente JList personalizado para renderizar elementos de menú interactivos
 * con soporte para efectos Hover (mouse encima) y Selección.
 * 
 * @param <E> Tipo de elemento alojado en la lista.
 */
public class ListMenu<E extends Object> extends JList<E> {

    private final DefaultListModel model;
    private int selectedIndex = -1; // Índice del ítem seleccionado actualmente
    private int overIndex = -1;     // Índice del ítem sobre el cual está el cursor (Hover)
    private EventMenuSelected event;

    /**
     * Registra el listener para los eventos de selección en el menú.
     * @param event Instancia que implementa la interfaz EventMenuSelected.
     */
    public void addEventMenuSelected(EventMenuSelected event) {
        this.event = event;
    }

    public ListMenu() {
        model = new DefaultListModel();
        setModel(model);

        // Control de clics con el ratón
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isLeftMouseButton(me)) {
                    int index = locationToIndex(me.getPoint());
                    Object o = model.getElementAt(index);
                    
                    if (o instanceof Model_Menu) {
                        Model_Menu menu = (Model_Menu) o;
                        // Solo se activa la selección si el elemento es de tipo MENU
                        if (menu.getType() == Model_Menu.MenuType.MENU) {
                            selectedIndex = index;
                            if (event != null) {
                                event.selected(menu.getIcon());
                            }
                        }
                    } else {
                        selectedIndex = index;
                    }
                    repaint(); // Redibuja la lista para actualizar el diseño visual
                }
            }

            @Override
            public void mouseExited(MouseEvent me) {
                // Al salir del área de la lista, elimina el efecto hover
                overIndex = -1;
                repaint();
            }
        });

        // Control de movimiento del ratón para el efecto Hover
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                int index = locationToIndex(me.getPoint());
                if (index != overIndex) {
                    Object o = model.getElementAt(index);
                    if (o instanceof Model_Menu) {
                        Model_Menu menu = (Model_Menu) o;
                        if (menu.getType() == Model_Menu.MenuType.MENU) {
                            overIndex = index;
                        } else {
                            overIndex = -1; // Desactiva el hover si es un TÍTULO o espacio VACÍO
                        }
                        repaint();
                    }
                }
            }
        });
    }

    /**
     * Retorna el renderizador de celdas personalizado que dibuja cada objeto MenuItem.
     */
    @Override
    public ListCellRenderer<? super E> getCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int index, boolean selected, boolean focus) {
                Model_Menu data;
                if (o instanceof Model_Menu) {
                    data = (Model_Menu) o;
                } else {
                    data = new Model_Menu("", o + "", Model_Menu.MenuType.EMPTY);
                }
                
                // Crea la vista individual de cada fila según sus estados
                MenuItem item = new MenuItem(data);
                item.setSelected(selectedIndex == index);
                item.setOver(overIndex == index);
                return item;
            }
        };
    }
    /**
     * Establece el índice seleccionado de forma manual y actualiza la vista.
     */
    @Override
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        repaint(); // Vuelve a pintar para que 'MenuItem' aplique setSelected(true)
    }

    /**
     * Agrega un nuevo objeto de menú al modelo de la lista.
     * @param data Objeto Model_Menu a registrar.
     */
    public void addItem(Model_Menu data) {
        model.addElement(data);
    }
}