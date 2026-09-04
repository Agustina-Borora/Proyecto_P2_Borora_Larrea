package escritorio;

import javax.swing.Icon;

/**
 * POJO de soporte para poblar un {@link Card}: agrupa el icono, título, valor destacado
 * y descripción que se le van a asignar a la tarjeta, para poder armarlos y pasarlos
 * juntos (ej. al construir el conjunto de tarjetas del dashboard) en vez de llamar
 * a los setters de {@link Card} uno por uno.
 */
public class Model_Card {

    /** Icono que se mostrará en la tarjeta. */
    private Icon icon;

    /** Título principal de la tarjeta. */
    private String title;

    /** Valor o dato numérico/destacado que mostrará la tarjeta. */
    private String values;

    /** Descripción corta o información secundaria de la tarjeta. */
    private String description;

    /**
     * Constructor por defecto.
     * Crea una instancia vacía del modelo sin inicializar sus atributos.
     */
    public Model_Card() {
    }

    /**
     * Constructor parametrizado para inicializar todos los campos de la tarjeta.
     * 
     * @param icon        El icono visual que se asignará a la tarjeta.
     * @param title       El título principal.
     * @param values      El valor o métrica destacada.
     * @param description La descripción o texto secundario.
     */
    public Model_Card(Icon icon, String title, String values, String description) {
        this.icon = icon;
        this.title = title;
        this.values = values;
        this.description = description;
    }

    /**
     * Obtiene el icono representativo de la tarjeta.
     * 
     * @return El objeto {@link Icon} asignado.
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Establece el icono representativo de la tarjeta.
     * 
     * @param icon El nuevo objeto {@link Icon} a mostrar.
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /**
     * Obtiene el título principal de la tarjeta.
     * 
     * @return El título como {@link String}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Establece el título principal de la tarjeta.
     * 
     * @param title El nuevo título.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene los valores o métricas mostradas en la tarjeta.
     * 
     * @return El valor principal como {@link String}.
     */
    public String getValues() {
        return values;
    }

    /**
     * Establece los valores o métricas que se mostrarán en la tarjeta.
     * 
     * @param values El texto o cifra destacada a asignar.
     */
    public void setValues(String values) {
        this.values = values;
    }

    /**
     * Obtiene la descripción secundaria de la tarjeta.
     * 
     * @return La descripción como {@link String}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción secundaria de la tarjeta.
     * 
     * @param description La nueva descripción.
     */
    public void setDescription(String description) {
        this.description = description;
    }
}