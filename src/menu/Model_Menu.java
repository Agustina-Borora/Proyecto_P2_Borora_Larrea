package menu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Modelo (POJO) que representa un ítem del menú lateral de navegación.
 * Es consumido por {@link ListMenu} (que arma la lista) y {@link MenuItem}
 * (que dibuja cada fila), y define tanto las opciones de navegación reales
 * como los títulos de sección y separadores vacíos que decoran el menú.
 */
public class Model_Menu {

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indica el rol de este ítem dentro del menú (opción navegable, título de
     * sección o espacio vacío). {@link ListMenu} usa este valor para decidir
     * si el ítem responde a clics/hover y si dispara {@link EventMenuSelected}.
     */
    public MenuType getType() {
        return type;
    }

    public void setType(MenuType type) {
        this.type = type;
    }

    /**
     * Construye un ítem de menú completo.
     *
     * @param icon Nombre base del recurso de ícono en {@code /icon/} (sin extensión),
     *             usado por {@link #toIcon()} y {@link #toIcon(java.awt.Color)}.
     * @param name Texto visible del ítem (nombre de la opción o del título de sección).
     * @param type Rol del ítem dentro del menú: {@link MenuType#MENU}, {@link MenuType#TITLE}
     *             o {@link MenuType#EMPTY}.
     */
    public Model_Menu(String icon, String name, MenuType type) {
        this.icon = icon;
        this.name = name;
        this.type = type;
    }

    public Model_Menu() {
    }

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
     * Devuelve el icono "teñido" (recolored) con el color indicado,
     * conservando el canal alpha original. Sirve para pintar el mismo
     * PNG en gris (estado normal) o en verde (estado seleccionado) sin
     * necesitar un archivo de imagen distinto por color.
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
     * Rol que cumple un {@link Model_Menu} dentro de la lista del menú lateral:
     * <ul>
     *   <li>{@link #TITLE} — encabezado de sección, no es clickeable.</li>
     *   <li>{@link #MENU} — opción de navegación real, dispara {@link EventMenuSelected}.</li>
     *   <li>{@link #EMPTY} — espacio/separador vacío entre secciones.</li>
     * </ul>
     */
    public static enum MenuType {
        TITLE, MENU, EMPTY
    }
}
