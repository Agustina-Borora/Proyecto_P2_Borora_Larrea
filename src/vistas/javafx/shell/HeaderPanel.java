package vistas.javafx.shell;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.Sesion;
import utilidades.PreferenciasSistema;
import vistas.javafx.menu.IconoFx;

/**
 * Encabezado (header) de toda la aplicación, arriba del sidebar y del contenido -- nuevo,
 * siguiendo el diseño de Figma (nodo 246:314, el mismo en todas las pantallas del prototipo):
 * marca del laboratorio a la izquierda (antes vivía adentro del sidebar, ver {@link AppShell}),
 * un campo de búsqueda centrado, y los datos de la sesión a la derecha (Figma tiene ahí un
 * usuario de mentira "Admin User" / "Administrador"; acá se muestra quien inició sesión de
 * verdad, con {@link modelo.Sesion}).
 *
 * <p>Se arma con {@link BorderPane} (izquierda/centro/derecha) en vez de una fila simple: así el
 * buscador queda centrado en el espacio libre entre la marca y el usuario, en vez de quedar
 * "pegado" a la marca con un vacío enorme hasta el otro extremo.</p>
 *
 * <p>En Figma ese campo de búsqueda no es un buscador global -- representa que la pantalla que
 * esté activa en cada momento tiene su propia búsqueda (por eso está ahí: para encontrar más
 * fácil, por ejemplo, a los pacientes). Acá se resuelve con {@link #establecerAccionBusqueda}:
 * cada vez que {@code AppShell#navegar} cambia de pantalla, le pasa a este header qué hacer con
 * lo que se escriba (o {@code null} si esa pantalla todavía no tiene su propia búsqueda
 * conectada) y vacía el campo, para no dejar "pegado" el filtro de la pantalla anterior.</p>
 */
public class HeaderPanel {

    private static final String VERDE_OSCURO = "#1E5C3D";
    private static final String VERDE_MAS_OSCURO = "#123B27";
    private static final String GRIS_TEXTO = "#8C9490";
    private static final String GRIS_ICONO = "#6B7C74";
    private static final String TEXTO_NORMAL = "#141C19";

    private final TextField campoBusqueda = new TextField();

    private Consumer<String> accionBusqueda;
    private HBox buscador;

    /** El encabezado que se está mostrando (para poder refrescar el nombre/logo al guardar Configuración). */
    private static HeaderPanel actual;
    private BorderPane raizActual;

    /**
     * Vuelve a armar el nombre y el logo de arriba a la izquierda con lo último guardado en
     * Configuración (2026-10-08: antes solo se leía al abrir la app). Llamar desde el hilo de JavaFX.
     */
    public static void refrescarMarca() {
        if (actual != null && actual.raizActual != null) {
            HBox marca = actual.armarMarca();
            BorderPane.setAlignment(marca, Pos.CENTER_LEFT);
            actual.raizActual.setLeft(marca);
        }
    }

    public Parent construir() {
        BorderPane raiz = new BorderPane();
        actual = this;
        raizActual = raiz;
        raiz.getStyleClass().add("app-header");
        // Mes 8 (rediseño visual): antes el padding vertical era 0 y todo el centrado dependía
        // de BorderPane.setAlignment -- en los hechos la insignia del laboratorio (44x44) quedaba
        // con su borde superior pegado casi al borde redondeado de la tarjeta (a solo 10px del
        // tope), mientras que el avatar del usuario (28x28, más chico) quedaba con bastante más
        // aire (18px). Los dos bloques de TEXTO (nombre del laboratorio / nombre del usuario)
        // terminaban centrados a la misma altura igual, pero el ícono grande "pegado arriba"
        // hacía que todo el encabezado se viera desprolijo/desbalanceado de un vistazo. Ahora se
        // da aire explícito arriba y abajo (13px) y se achica un poco la insignia (44 -> 38, el
        // mismo tamaño que ya usan los íconos de las tarjetas del Escritorio) para que quede
        // proporcionada al avatar y con el mismo margen visual de los dos lados.
        raiz.setPadding(new Insets(13, 24, 13, 21));
        raiz.setPrefHeight(64);
        raiz.setMinHeight(64);

        HBox marca = armarMarca();
        buscador = armarBuscador();
        HBox usuario = armarUsuario();

        BorderPane.setAlignment(marca, Pos.CENTER_LEFT);
        BorderPane.setAlignment(buscador, Pos.CENTER);
        BorderPane.setAlignment(usuario, Pos.CENTER_RIGHT);

        raiz.setLeft(marca);
        raiz.setCenter(buscador);
        raiz.setRight(usuario);
        return raiz;
    }

    /**
     * Antes el nombre de acá estaba fijo ("San Gregorio" / "Laboratorio de Análisis Clínicos"),
     * sin importar lo que se cargara en Configuración &gt; Laboratorio -- se podía cambiar el
     * nombre del laboratorio ahí y el encabezado de arriba de toda la app seguía mostrando el de
     * mentira. Ahora lee {@link PreferenciasSistema#getNombreLaboratorio()} (la misma clave que
     * ya usan el "Informe de Resultados" y el "Comprobante de Orden"), así que cambiarlo en
     * Configuración se nota en el acto en toda la app. El subtítulo ("Laboratorio de Análisis
     * Clínicos") sigue siendo una bajada fija -- es una descripción del tipo de negocio, no un
     * dato propio del laboratorio, y Configuración no tiene (todavía) un campo para eso.
     */
    private HBox armarMarca() {
        // 2026-10-08: el nombre de la app es configurable aparte del nombre largo del laboratorio
        // que va en los informes (Configuración > Laboratorio > "Nombre del sistema").
        String nombreLaboratorio = PreferenciasSistema.getNombreSistema();

        Label inicialesMarca = new Label(inicialesDe(nombreLaboratorio));
        inicialesMarca.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        StackPane insignia = new StackPane();
        insignia.getChildren().add(inicialesMarca);
        insignia.setPrefWidth(38);
        insignia.setPrefHeight(38);
        insignia.setMinWidth(38);
        insignia.setMinHeight(38);
        insignia.setMaxWidth(38);
        insignia.setMaxHeight(38);
        insignia.setStyle("-fx-background-color: linear-gradient(to bottom right, #2C7A54, " + VERDE_MAS_OSCURO + ");"
                + " -fx-background-radius: 12;"
                + " -fx-effect: dropshadow(gaussian, rgba(18,59,39,0.35), 10, 0.15, 0, 2);");

        // Si hay logo cargado y está tildado usarlo, va el logo en lugar de las iniciales.
        String rutaLogo = PreferenciasSistema.getRutaLogo();
        if (PreferenciasSistema.isUsarLogoEnSistema() && rutaLogo != null && !rutaLogo.trim().isEmpty()
                && new java.io.File(rutaLogo.trim()).isFile()) {
            try {
                javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(new java.io.File(rutaLogo.trim()).toURI().toString(), 34, 34, true, true));
                insignia.getChildren().setAll(logo);
                insignia.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                        + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0.1, 0, 1);");
            } catch (RuntimeException e) {
                // Si la imagen no se puede leer, quedan las iniciales.
            }
        }

        Label titulo = new Label(nombreLaboratorio);
        titulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TEXTO_NORMAL + ";");
        Label subtitulo = new Label(PreferenciasSistema.getSubtituloSistema());
        subtitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRIS_TEXTO + ";");
        VBox textos = new VBox(1, titulo, subtitulo);
        textos.setAlignment(Pos.CENTER_LEFT);

        // La insignia (38px) y el bloque de texto se centran entre sí con la misma alineación
        // que usa "usuario" (ver armarUsuario) -- así, aunque la insignia sea más grande que el
        // avatar del usuario, el CENTRO vertical de ambos bloques (y por lo tanto la altura de
        // sus textos) queda exactamente igual, porque los dos quedan centrados dentro de la misma
        // franja vertical del encabezado (ver el padding explícito en construir()).
        HBox marca = new HBox(14, insignia, textos);
        marca.setAlignment(Pos.CENTER_LEFT);
        return marca;
    }

    /**
     * Iniciales para la insignia verde a partir del nombre real del laboratorio: hasta 2 letras,
     * una por cada una de las primeras dos palabras (ej. "Laboratorio San Gregorio" -&gt; "LS",
     * "San Gregorio" -&gt; "SG", igual que antes cuando estaba fijo). Si el nombre quedara vacío
     * por algún motivo, {@code PreferenciasSistema.getNombreLaboratorio()} ya nunca devuelve
     * vacío (cae a "Laboratorio San Gregorio" por defecto), así que esto siempre tiene con qué
     * trabajar.
     */
    private static String inicialesDe(String nombre) {
        String[] palabras = nombre.trim().split("\\s+");
        StringBuilder iniciales = new StringBuilder();
        for (int i = 0; i < palabras.length && iniciales.length() < 2; i++) {
            if (!palabras[i].isEmpty()) {
                iniciales.append(Character.toUpperCase(palabras[i].charAt(0)));
            }
        }
        return iniciales.length() > 0 ? iniciales.toString() : "SG";
    }

    /**
     * Campo de búsqueda con ícono de lupa, centrado en el header -- conectado a lo que
     * {@link #establecerAccionBusqueda} defina en cada momento, a cada tecla (mismo patrón ya
     * usado en el buscador de "Estudios solicitados" de Nuevo Análisis).
     */
    private HBox armarBuscador() {
        campoBusqueda.getStyleClass().add("campo-buscador-header");
        campoBusqueda.setPromptText("Buscar...");
        HBox.setHgrow(campoBusqueda, Priority.ALWAYS);
        campoBusqueda.setOnKeyReleased(evt -> {
            if (accionBusqueda != null) {
                accionBusqueda.accept(campoBusqueda.getText());
            }
        });

        HBox caja = new HBox(10, IconoFx.vista("search", 14, 0x6B7C74), campoBusqueda);
        caja.getStyleClass().add("buscador-header");
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setPrefWidth(340);
        caja.setMaxWidth(340);
        return caja;
    }

    private HBox armarUsuario() {
        String nombre = vacioA(Sesion.nombre, "Usuario");
        String apellido = vacioA(Sesion.apellido, "");
        String rol = vacioA(Sesion.rol, "-");

        String iniciales = (primeraLetra(apellido) + primeraLetra(nombre)).toUpperCase();
        if (iniciales.trim().isEmpty()) {
            iniciales = "?";
        }

        Label inicialesLbl = new Label(iniciales);
        inicialesLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10.5px;");
        StackPane avatar = new StackPane();
        avatar.getChildren().add(inicialesLbl);
        avatar.setPrefWidth(28);
        avatar.setPrefHeight(28);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #2C7A54, " + VERDE_MAS_OSCURO + ");"
                + " -fx-background-radius: 14;");

        // Nombre y rol alineados a la derecha (no a la izquierda, como estaba antes): en el diseño
        // de Figma (nodo 246:339) este bloque termina en un borde parejo contra el margen derecho
        // del header en vez de quedar "pegado" al avatar con un borde irregular de cada lado --
        // esto es lo que se veía como desprolijo. Tamaño del avatar (28px) y separación (7px)
        // también ajustados para coincidir exactamente con esa referencia.
        Label lblNombre = new Label((nombre + " " + apellido).trim());
        lblNombre.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: " + TEXTO_NORMAL + ";");
        Label lblRol = new Label(rol);
        lblRol.setStyle("-fx-font-size: 10.5px; -fx-text-fill: " + GRIS_TEXTO + ";");
        VBox textos = new VBox(1, lblNombre, lblRol);
        textos.setAlignment(Pos.CENTER_RIGHT);

        HBox caja = new HBox(7, avatar, textos);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private static String vacioA(String valor, String porDefecto) {
        return (valor == null || valor.trim().isEmpty()) ? porDefecto : valor.trim();
    }

    private static String primeraLetra(String texto) {
        return (texto == null || texto.isEmpty()) ? "" : texto.substring(0, 1);
    }

    /**
     * La pantalla activa define acá qué hacer cuando cambia el texto del buscador ({@code
     * AppShell#navegar} lo llama cada vez que cambia de pantalla). {@code null} deja el campo
     * visible pero sin ninguna acción -- para las pantallas que todavía no tienen su propia
     * búsqueda conectada.
     */
    public void establecerAccionBusqueda(Consumer<String> accion) {
        this.accionBusqueda = accion;
    }

    /** Vacía el campo de búsqueda -- se llama al cambiar de pantalla para no dejar un filtro "pegado". */
    public void limpiarBusqueda() {
        campoBusqueda.setText("");
    }

    /**
     * Muestra u oculta el buscador del header entero. Se oculta por dos motivos distintos, los
     * dos resueltos en {@code AppShell#navegar}: algunas pantallas (Pacientes, Registros,
     * Registrar Resultados, Nuevo Análisis, Catálogo de Exámenes) ya tienen su propio buscador
     * adentro, y mostrar también el del header sería un segundo buscador repetido y confuso; otras
     * (Estadísticas, Configuración) no tienen nada que buscar, así que dejarlo visible sin
     * conectarlo a nada sería espacio perdido. En el resto de las pantallas (Escritorio, Pagos,
     * Usuarios) queda visible y conectado de verdad con {@link #establecerAccionBusqueda}.
     */
    public void mostrarBuscador(boolean visible) {
        buscador.setVisible(visible);
        buscador.setManaged(visible);
    }
}
