package modelo;

import vistas.menu.EventMenuSelected;
import vistas.menu.ListMenu;
import vistas.menu.MenuItem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Modelo (POJO) que representa un ítem del menú lateral de navegación.
 */
public class Model_Menu {

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Identificador estable de navegación que usa {@link vistas.formulariosPrincipales.Principal#navegar}
     * para decidir qué pantalla abrir. Es independiente del nombre del ícono, para poder cambiar
     * el ícono de una opción sin afectar a qué pantalla lleva.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indica el rol de este ítem dentro del menú (opción navegable, título de sección o espacio
     * vacío).
     */
    public MenuType getType() {
        return type;
    }

    public void setType(MenuType type) {
        this.type = type;
    }

    /**
     * Construye un ítem de menú completo, usando el nombre del ícono también como id de
     * navegación (caso más común: título, separador, u opción cuyo ícono no cambia).
     *
     * @param icon Nombre base del recurso de ícono en {@code /icon/} (sin extensión),
     * @param name Texto visible del ítem (nombre de la opción o del título de sección).
     * @param type Rol del ítem dentro del menú: {@link MenuType#MENU}, {@link MenuType#TITLE}
     */
    public Model_Menu(String icon, String name, MenuType type) {
        this(icon, icon, name, type);
    }

    /**
     * Construye un ítem de menú con un id de navegación distinto del nombre del ícono, para
     * poder darle a una opción un ícono nuevo sin cambiar a qué pantalla lleva.
     *
     * @param id Identificador de navegación (ver {@link #getId()}).
     * @param icon Nombre base del recurso de ícono en {@code /icon/} (sin extensión).
     * @param name Texto visible del ítem.
     * @param type Rol del ítem dentro del menú.
     */
    public Model_Menu(String id, String icon, String name, MenuType type) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.type = type;
    }

    public Model_Menu() {
    }

    private String id;
    private String icon;
    private String name;
    private MenuType type;

    /**
     * Carga el ícono del ítem con sus colores originales (sin recoloreo).
     *
     * @return Ícono ubicado en {@code /icon/<icon>.png}.
     */
    public Icon toIcon() {
        return new ImageIcon(getClass().getResource("/icon/" + icon + ".png"));
    }

    /**
     * Devuelve el icono "teñido" (recolored) con el color indicado, conservando el canal alpha
     * original.
     */
    public Icon toIcon(Color color) {
        Image original = new ImageIcon(getClass().getResource("/icon/" + icon + ".png")).getImage();
        int w = original.getWidth(null);
        int h = original.getHeight(null);
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        g2.drawImage(original, 0, 0, null);
        g2.dispose();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = buffered.getRGB(x, y);
                int alpha = (argb >>> 24);
                if (alpha != 0) {
                    int newArgb = (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                    buffered.setRGB(x, y, newArgb);
                }
            }
        }
        return new ImageIcon(buffered);
    }

    /**
     * Rol que cumple un {@link Model_Menu} dentro de la lista del menú lateral: <ul> <li>{@link
     * #TITLE} — encabezado de sección, no es clickeable.</li> <li>{@link #MENU} — opción de
     * navegación real, dispara {@link EventMenuSelected}.</li> <li>{@link #EMPTY} —
     * espacio/separador vacío entre secciones.</li> </ul>
     */
    public static enum MenuType {
        TITLE, MENU, EMPTY
    }
}
