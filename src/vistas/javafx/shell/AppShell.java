package vistas.javafx.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.Model_Menu;
import modelo.Model_Menu.MenuType;
import vistas.javafx.PuenteEDT;
import vistas.javafx.menu.IconoFx;

/**
 * Cascarón (shell) de toda la aplicación, en JavaFX puro: sidebar de navegación + área de
 * contenido central, ambos armados con paneles de layout (VBox/BorderPane) que se acomodan solos
 * al tamaño real de la ventana. Reemplaza a {@code vistas.formulariosPrincipales.Principal} +
 * {@code vistas.panels.Menu} (el JFrame con AbsoluteLayout en píxeles fijos que se cortaba en
 * pantallas más chicas que la notebook con la que se diseñó).
 *
 * <p>Las pantallas ya migradas a JavaFX (Escritorio, Pagos, Configuración, Obras Sociales -esta
 * última embebida adentro de Configuración, ya no como ítem propio del sidebar-) se muestran
 * directo. Las que
 * todavía son Swing se alojan con {@link SwingNode}, que las deja convivir dentro de esta misma
 * escena sin tener que reescribirlas todas de un saque -- se van migrando una por una.</p>
 */
public class AppShell {

    // Misma paleta que vistas.menu.MenuItem (versión Swing del ítem de menú), para que el
    // sidebar nuevo se vea igual que el viejo.
    private static final String GREEN_BG_HEX = "#E8F5E9";
    private static final String GREEN_TITLE_HEX = "#8FAE9C";
    private static final String TEXT_NORMAL_HEX = "#2B2B2B";
    private static final int ICON_NORMAL_RGB = 0x6B7C74;
    // Mismo color que ICON_NORMAL_RGB, pero como string -- lo necesita el emoji de respaldo (ver
    // EMOJI_RESPALDO_POR_ICONO) para -fx-text-fill, que no acepta un int 0xRRGGBB como IconoFx.
    private static final String ICON_NORMAL_HEX = "#6B7C74";
    private static final int RED_RGB = 0xE53935;
    private static final String RED_HEX = "#E53935";
    // Mes 8 (rediseño visual): la fila del ítem seleccionado del sidebar pasó de fondo verde
    // clarito + texto verde oscuro a un degradado verde oscuro de fondo (ver ".sidebar-fila-
    // seleccionada" en theme.css) -- así que ahora el ícono y el texto de esa fila van en blanco
    // en vez de en verde oscuro, para que se sigan leyendo sobre el fondo nuevo.
    private static final int BLANCO_RGB = 0xFFFFFF;
    private static final String BLANCO_HEX = "#FFFFFF";

    private static final double TAMANO_ICONO = 18;

    /**
     * Nombre de pantalla (tabla {@code permisos_pantalla}) para cada id de menú que sí tiene
     * control de acceso real. "Escritorio" ("4_1") queda afuera a propósito: es la pantalla de
     * aterrizaje al iniciar sesión, así que se deja siempre accesible -- si a un Técnico nuevo se
     * le olvida tildarle ese permiso, igual puede entrar al sistema en vez de quedar
     * completamente afuera sin ver nada.
     */
    private static final Map<String, String> PANTALLA_POR_ID = new LinkedHashMap<>();

    static {
        PANTALLA_POR_ID.put("5_1", "Pacientes");
        PANTALLA_POR_ID.put("2", "Registros");
        PANTALLA_POR_ID.put("3", "Nuevo Analisis");
        PANTALLA_POR_ID.put("4", "Registrar Resultados");
        PANTALLA_POR_ID.put("5", "Catalogo de Examenes");
        PANTALLA_POR_ID.put("6", "Pagos");
        PANTALLA_POR_ID.put("7", "Usuarios");
        PANTALLA_POR_ID.put("8", "Estadisticas");
        PANTALLA_POR_ID.put("9", "Configuracion");
        // "Historial" (pantalla real: AuditoriaScreen, 2026-10-01 -- "Historial" es el nombre que
        // ve Zaira en el sidebar, le quedaba más claro que "Auditoría") queda registrada acá (para
        // que navegar() la bloquee "de verdad", no sólo estéticamente, igual que el resto) pero a
        // propósito NO tiene ningún toggle en Usuarios para dársela a un Técnico -- es de
        // sólo-Administrador por diseño (ver el javadoc de AuditoriaScreen). Como
        // pantallasPermitidas nunca va a contener "Historial" para nadie, sólo pasa el chequeo de
        // agregarSiPermitido/navegar quien ya tiene el bypass de esAdministrador.
        PANTALLA_POR_ID.put("11", "Historial");
    }

    /**
     * Emoji de respaldo para un ítem del sidebar cuyo ícono ({@code /icon/<nombre>.png}) todavía
     * no existe en el proyecto -- sin esto, {@link IconoFx#vista} devuelve un {@link ImageView}
     * vacío y el ítem queda con un hueco en blanco en vez de un ícono (pasó con "Historial",
     * 2026-10-01: no hay ningún {@code historial.png}/{@code auditoria.png} entre los recursos).
     * Se usa sólo como respaldo -- en cuanto exista el archivo real para esa clave, {@link
     * #crearFila} vuelve a usarlo solo, sin tocar nada de acá.
     */
    private static final Map<String, String> EMOJI_RESPALDO_POR_ICONO = new LinkedHashMap<>();

    static {
        EMOJI_RESPALDO_POR_ICONO.put("historial", "🕓");
    }

    private final StackPane contenido = new StackPane();
    private final Map<String, SidebarFila> filas = new LinkedHashMap<>();
    private final HeaderPanel header = new HeaderPanel();

    /**
     * Permisos de acceso a pantalla del usuario de la sesión actual, resueltos una sola vez acá
     * (no en cada click) -- ver {@link #PANTALLA_POR_ID}. Un Administrador nunca los consulta:
     * {@link #esAdministrador} ya lo deja pasar por todo (mismo bypass que ya tenía
     * {@code dao.Usuario#tienePermiso}).
     */
    private boolean esAdministrador;
    private Set<String> pantallasPermitidas = Collections.emptySet();

    private Runnable alCerrarSesion;

    /**
     * Define a quién avisarle cuando el usuario confirma "Cerrar Sesion" (Principal es quien
     * sabe cerrar el Stage y volver a abrir Login).
     */
    public void setAlCerrarSesion(Runnable listener) {
        this.alCerrarSesion = listener;
    }

    /**
     * Arma la ventana completa (fondo + tarjeta blanca redondeada con sidebar y contenido) y deja
     * cargado "Escritorio" como pantalla inicial, igual que hacía {@code Principal.navegar("4_1")}
     * en el constructor original.
     */
    public Parent construir() {
        esAdministrador = "Admin".equalsIgnoreCase(modelo.Sesion.rol) || "Administrador".equalsIgnoreCase(modelo.Sesion.rol);
        pantallasPermitidas = esAdministrador ? Collections.emptySet()
                : PuenteEDT.ejecutar(
                        () -> controlador.UsuarioController.obtenerPantallasPermitidas(null, modelo.Sesion.idUsuario),
                        Collections.emptySet());

        StackPane fondo = new StackPane();
        fondo.getStyleClass().add("app-fondo");
        // Registra este panel como "anfitrión" de los toasts (ver ToastUtil) -- así cualquier
        // pantalla o diálogo puede mostrar un cartel de éxito/error sin recibir ninguna
        // referencia, y queda flotando sobre toda la app (header + sidebar + contenido), no sólo
        // sobre la pantalla activa.
        vistas.javafx.ToastUtil.registrarOverlay(fondo);
        // Copias de seguridad automáticas de la base (a la hora elegida en Configuración > Sistema).
        controlador.CopiasSeguridad.iniciar(vistas.javafx.ToastUtil::info, vistas.javafx.ToastUtil::error);

        BorderPane tarjeta = new BorderPane();
        tarjeta.getStyleClass().add("app-card");
        tarjeta.setTop(header.construir());
        tarjeta.setLeft(armarSidebar());

        contenido.getStyleClass().add("app-contenido");
        tarjeta.setCenter(contenido);

        fondo.getChildren().add(tarjeta);
        StackPane.setMargin(tarjeta, new Insets(16));

        navegar("4_1");

        return fondo;
    }

    // ---------------------------------------------------------------- sidebar

    private Node armarSidebar() {
        VBox raiz = new VBox();
        raiz.getStyleClass().add("sidebar");
        raiz.setPrefWidth(272);
        raiz.setMinWidth(230);
        raiz.setMaxWidth(320);

        VBox items = new VBox(3);
        items.setPadding(new Insets(16, 8, 16, 8));
        for (Model_Menu dato : construirDatosMenu()) {
            items.getChildren().add(crearFila(dato));
        }

        ScrollPane scroll = new ScrollPane(items);
        scroll.getStyleClass().add("sidebar-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        raiz.getChildren().addAll(scroll, armarPieSidebar());
        return raiz;
    }

    /**
     * "Cerrar Sesión" separado del resto, fuera del área que se desplaza -- así se ve siempre,
     * abajo de todo, igual que en el diseño de Figma (antes vivía como una fila más, mezclada con
     * el resto de las opciones de "Administración").
     */
    private Node armarPieSidebar() {
        VBox pie = new VBox();
        pie.getStyleClass().add("sidebar-pie");
        pie.setPadding(new Insets(10, 8, 14, 8));
        pie.getChildren().add(crearFila(new Model_Menu("10", "Cerrar Sesion", MenuType.MENU)));
        return pie;
    }

    /**
     * Misma estructura de secciones/ids/íconos que {@code vistas.panels.Menu#init()} -- salvo
     * "Registros" (que en el diseño de Figma está en "Análisis", no en "Principal") y "Pacientes",
     * que Figma no contempla pero sí lleva a una pantalla real de este sistema: queda junto a la
     * opción más parecida (Pacientes con Escritorio). No cambia a qué pantalla lleva cada opción.
     *
     * <p>"Cotización" (id "1") se sacó del menú: no tiene diseño propio en Figma y su función --
     * ver precios antes de confirmar un pedido -- ya la cubren Catálogo de Exámenes (columna
     * PRECIO) y el total en vivo de Nuevo Análisis. La pantalla Swing {@code Cotizacion.java}
     * queda sin usar en el proyecto por si hace falta recuperarla, no la borra este cambio.</p>
     *
     * <p>Cada opción (salvo "Escritorio", ver {@link #PANTALLA_POR_ID}) se agrega sólo si el
     * usuario de la sesión actual tiene permiso de esa pantalla ({@link #pantallasPermitidas}) --
     * un Administrador las ve todas igual que antes. Una sección entera ("Análisis",
     * "Administración") se omite si ninguna de sus opciones quedó visible, para no dejar un
     * título de sección "colgado" sin nada abajo.</p>
     */
    private List<Model_Menu> construirDatosMenu() {
        List<Model_Menu> datos = new ArrayList<>();
        datos.add(new Model_Menu("", "Principal", MenuType.TITLE));
        datos.add(new Model_Menu("4_1", "escritorio", "Escritorio", MenuType.MENU));
        agregarSiPermitido(datos, "5_1", "pacientes", "Pacientes");

        List<Model_Menu> seccionAnalisis = new ArrayList<>();
        agregarSiPermitido(seccionAnalisis, "2", "registros", "Registros");
        agregarSiPermitido(seccionAnalisis, "3", "nuevoanalisis", "Nuevo Analisis");
        agregarSiPermitido(seccionAnalisis, "4", "registrarresultados", "Registrar Resultados");
        agregarSiPermitido(seccionAnalisis, "5", "catalogodeexamen", "Catalogo de Examenes");
        if (!seccionAnalisis.isEmpty()) {
            datos.add(new Model_Menu("", "Análisis", MenuType.TITLE));
            datos.addAll(seccionAnalisis);
        }

        List<Model_Menu> seccionAdmin = new ArrayList<>();
        agregarSiPermitido(seccionAdmin, "6", "pagos", "Pagos");
        agregarSiPermitido(seccionAdmin, "7", "usuario", "Usuarios");
        agregarSiPermitido(seccionAdmin, "8", "estadistica", "Estadisticas");
        agregarSiPermitido(seccionAdmin, "9", "configuracion", "Configuracion");
        agregarSiPermitido(seccionAdmin, "11", "historial", "Historial");
        if (!seccionAdmin.isEmpty()) {
            datos.add(new Model_Menu("", "Administración", MenuType.TITLE));
            datos.addAll(seccionAdmin);
        }
        return datos;
    }

    private void agregarSiPermitido(List<Model_Menu> destino, String id, String icono, String nombre) {
        if (esAdministrador || pantallasPermitidas.contains(PANTALLA_POR_ID.get(id))) {
            destino.add(new Model_Menu(id, icono, nombre, MenuType.MENU));
        }
    }

    private Node crearFila(Model_Menu dato) {
        if (dato.getType() == MenuType.TITLE) {
            Label lbl = new Label(dato.getName().toUpperCase());
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + GREEN_TITLE_HEX + ";");
            HBox envoltorio = new HBox(0, lbl);
            envoltorio.setPadding(new Insets(14, 12, 6, 12));
            return envoltorio;
        }

        SidebarFila fila = new SidebarFila(dato);
        filas.put(dato.getId(), fila);
        return fila;
    }

    private void seleccionarEnSidebar(String id) {
        for (Map.Entry<String, SidebarFila> entrada : filas.entrySet()) {
            entrada.getValue().setSeleccionada(entrada.getKey().equals(id));
        }
    }

    /**
     * Una fila clickeable del sidebar (opción de menú real). Equivalente en JavaFX de
     * {@code vistas.menu.MenuItem}: mismos colores, mismo comportamiento (normal / hover /
     * seleccionado), salvo que acá el hover lo resuelve solo el CSS ({@code .sidebar-fila:hover})
     * en vez de pintarlo a mano.
     */
    private final class SidebarFila extends HBox {

        private final Model_Menu dato;
        private final boolean logout;

        /**
         * Normalmente un {@link ImageView} (ícono real de {@code /icon/<nombre>.png}), que
         * {@link #setSeleccionada} recolorea según el estado. Para un ítem sin archivo real
         * todavía (ver {@link #EMOJI_RESPALDO_POR_ICONO}) es un {@link Label} con un emoji en vez
         * -- no hay imagen que recolorear ahí, así que {@link #setSeleccionada} lo deja como está
         * y sólo cambia el color del texto.
         */
        private final Node icono;
        private final Label texto;

        SidebarFila(Model_Menu dato) {
            super(14);
            this.dato = dato;
            this.logout = "Cerrar Sesion".equalsIgnoreCase(dato.getName().trim());

            setPadding(new Insets(10, 14, 10, 14));
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("sidebar-fila");

            String emojiRespaldo = EMOJI_RESPALDO_POR_ICONO.get(dato.getIcon());
            if (emojiRespaldo != null && IconoFx.imagen(dato.getIcon(), 0) == null) {
                Label emoji = new Label(emojiRespaldo);
                emoji.setMinWidth(TAMANO_ICONO);
                emoji.setAlignment(Pos.CENTER);
                icono = emoji;
            } else {
                icono = IconoFx.vista(dato.getIcon(), TAMANO_ICONO, logout ? RED_RGB : ICON_NORMAL_RGB);
            }
            texto = new Label(dato.getName());
            aplicarColorTexto(logout ? RED_HEX : TEXT_NORMAL_HEX);
            // Un emoji no es una imagen que IconoFx pueda teñir -- sin esto, el Label se queda sin
            // -fx-text-fill propio y termina heredando el blanco de algún ancestro del sidebar (lo
            // que se veía como "el emoji está blanco", casi invisible sobre el fondo claro).
            if (icono instanceof Label) {
                aplicarColorEmoji((Label) icono, logout ? RED_HEX : ICON_NORMAL_HEX);
            }

            getChildren().addAll(icono, texto);
            setOnMouseClicked(evento -> navegar(dato.getId()));
        }

        void setSeleccionada(boolean seleccionada) {
            getStyleClass().remove("sidebar-fila-seleccionada");
            if (logout) {
                return; // "Cerrar Sesion" nunca queda "marcada" como pantalla actual
            }
            if (seleccionada) {
                getStyleClass().add("sidebar-fila-seleccionada");
                if (icono instanceof ImageView) {
                    ((ImageView) icono).setImage(IconoFx.imagen(dato.getIcon(), BLANCO_RGB));
                } else if (icono instanceof Label) {
                    aplicarColorEmoji((Label) icono, BLANCO_HEX);
                }
                aplicarColorTexto(BLANCO_HEX);
            } else {
                if (icono instanceof ImageView) {
                    ((ImageView) icono).setImage(IconoFx.imagen(dato.getIcon(), ICON_NORMAL_RGB));
                } else if (icono instanceof Label) {
                    aplicarColorEmoji((Label) icono, ICON_NORMAL_HEX);
                }
                aplicarColorTexto(TEXT_NORMAL_HEX);
            }
        }

        private void aplicarColorTexto(String colorHex) {
            texto.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        }

        private void aplicarColorEmoji(Label emoji, String colorHex) {
            emoji.setStyle("-fx-font-size: " + TAMANO_ICONO + "px; -fx-text-fill: " + colorHex + ";");
        }
    }

    // ---------------------------------------------------------------- navegación

    /**
     * Decide qué mostrar en el área de contenido según el id de menú elegido -- misma lógica y
     * mismos casos que tenía {@code Principal.navegar(String)}, para no cambiar a qué pantalla
     * lleva cada opción del sidebar.
     */
    public void navegar(String id) {
        if ("10".equals(id)) {
            cerrarSesion();
            return; // no cambia de pantalla ni marca selección nueva
        }

        // Bloqueo real, no sólo estético: aunque el sidebar ya oculta las opciones sin permiso
        // (construirDatosMenu), varias pantallas saltan directo a otra por su cuenta (ej. Escritorio
        // y Registros llaman navegar("4") desde un botón interno, no desde el sidebar) -- ese salto
        // pasa por acá también, así que este chequeo es el único lugar que hace falta tocar para que
        // "bloquear" sea de verdad, no sólo esconder el ítem del menú.
        String nombrePantalla = PANTALLA_POR_ID.get(id);
        if (nombrePantalla != null && !esAdministrador && !pantallasPermitidas.contains(nombrePantalla)) {
            mostrarAccesoDenegado(nombrePantalla);
            return;
        }

        // Cada pantalla define su propia búsqueda (o ninguna) apenas se muestra, así que primero
        // se limpia lo que haya quedado de la pantalla anterior -- ver HeaderPanel. Por defecto el
        // buscador del header queda visible; las pantallas que ya traen su propio buscador adentro
        // (Registros, Registrar Resultados, Nuevo Análisis, Catálogo de Exámenes) lo ocultan más
        // abajo para no mostrar dos buscadores repetidos en la misma pantalla, y Estadísticas y
        // Configuración lo ocultan directamente porque no hay nada ahí para buscar. Pagos, Usuarios
        // y Pacientes sí lo conectan de verdad (filtra lo que se ve en cada una) -- dejarlo visible
        // pero sin acción en una pantalla que no lo usa es espacio perdido y confuso, mejor ocultarlo
        // derecho.
        header.establecerAccionBusqueda(null);
        header.limpiarBusqueda();
        header.mostrarBuscador(true);

        switch (id) {
            case "4_1": {
                vistas.javafx.escritorio.EscritorioScreen pantalla = new vistas.javafx.escritorio.EscritorioScreen();
                pantalla.setAlCargarResultados(() -> navegar("4"));
                mostrarFx(pantalla.construir());
                header.establecerAccionBusqueda(pantalla::filtrarPorPaciente);
                break;
            }

            case "5_1": {
                vistas.javafx.pacientes.PacientesScreen pantalla = new vistas.javafx.pacientes.PacientesScreen();
                mostrarFx(pantalla.construir());
                header.establecerAccionBusqueda(pantalla::filtrarPorTexto);
                break;
            }

            case "2": {
                header.mostrarBuscador(false); // Registros ya tiene su propio buscador
                vistas.javafx.registros.RegistrosScreen pantalla = new vistas.javafx.registros.RegistrosScreen();
                pantalla.setAlNuevoAnalisis(() -> navegar("3"));
                pantalla.setAlCargarResultados(() -> navegar("4"));
                mostrarFx(pantalla.construir());
                break;
            }

            case "3":
                header.mostrarBuscador(false); // Nuevo Análisis ya tiene su propio buscador de estudios
                mostrarFx(new vistas.javafx.nuevoAnalisis.NuevoAnalisisScreen().construir());
                break;

            case "4": {
                header.mostrarBuscador(false); // El selector de "Registrar Resultados" ya tiene su propio buscador
                vistas.javafx.registrarResultados.RegistrarResultadosScreen pantalla =
                        new vistas.javafx.registrarResultados.RegistrarResultadosScreen();
                pantalla.setOrdenParaResultadosListener((paciente, orden) ->
                        Platform.runLater(() -> abrirCargaDeResultados(paciente, orden)));
                mostrarFx(pantalla.construir());
                break;
            }

            case "5":
                header.mostrarBuscador(false); // Catálogo de Exámenes ya tiene su propio buscador
                mostrarFx(new vistas.javafx.catalogo.CatalogoExamenesScreen().construir());
                break;

            case "6": {
                vistas.javafx.pagos.PagosScreen pantalla = new vistas.javafx.pagos.PagosScreen();
                mostrarFx(pantalla.construir());
                header.establecerAccionBusqueda(pantalla::filtrarPorTexto);
                break;
            }

            case "7": {
                vistas.javafx.usuarios.UsuariosScreen pantalla = new vistas.javafx.usuarios.UsuariosScreen();
                mostrarFx(pantalla.construir());
                header.establecerAccionBusqueda(pantalla::filtrarPorTexto);
                break;
            }

            case "8":
                header.mostrarBuscador(false); // Estadísticas no tiene nada que tenga sentido buscar acá
                mostrarFx(new vistas.javafx.estadisticas.EstadisticasScreen().construir());
                break;

            case "9":
                // Configuración no tiene nada que tenga sentido buscar acá (Obras Sociales, la
                // pestaña que sí tiene una tabla larga, vive adentro pero no tiene buscador propio
                // todavía -- se puede agregar aparte si hace falta).
                header.mostrarBuscador(false);
                mostrarFx(new vistas.javafx.configuracion.ConfiguracionScreen().construir());
                break;

            case "11": {
                vistas.javafx.auditoria.AuditoriaScreen pantalla = new vistas.javafx.auditoria.AuditoriaScreen();
                mostrarFx(pantalla.construir());
                header.establecerAccionBusqueda(pantalla::filtrarPorTexto);
                break;
            }

            default:
                return;
        }

        seleccionarEnSidebar(id);
    }

    /**
     * Cambia la pantalla activa con un fade corto (en vez de aparecer de golpe) -- este es el
     * único lugar por el que pasa cualquier cambio de pantalla JavaFX (ver {@link #navegar}), así
     * que alcanza con tocar acá para que la transición se sienta en toda la app.
     *
     * <p>Mes 8: cada pantalla JavaFX iba directo adentro de {@link #contenido} (un
     * {@code StackPane} sin scroll propio), así que cualquier pantalla con más contenido que alto
     * de ventana -- Estadísticas con los 4 gráficos/tablas fue el caso que avisó la clienta, pero
     * aplica a cualquier otra con una lista larga -- quedaba cortada abajo sin ninguna forma de
     * bajar a verla ("se desborda ... no se puede ir para abajo"). Envolviendo acá, una sola vez
     * para TODAS las pantallas, en un {@link ScrollPane} transparente (mismo truco que ya usaban
     * el sidebar y, antes de este cambio, Configuración con el suyo propio -- ver
     * {@link vistas.javafx.configuracion.ConfiguracionScreen#construir()}, que ya no necesita el
     * suyo) se puede bajar siempre que haga falta, sea cual sea la pantalla.</p>
     *
     * <p>Ojo con un detalle: varias pantallas (Pacientes, Usuarios, Pagos, Registros, Registrar
     * Resultados) usan {@code VBox.setVgrow(tabla, Priority.ALWAYS)} para que su tabla ocupe todo
     * el alto sobrante de la ventana -- un {@code ScrollPane} sin más, al no acotar el alto de su
     * contenido, hace que ese "sobrante" deje de existir y esas tablas queden con una altura chica
     * por defecto aunque la ventana tenga lugar de sobra. Por eso se ata el {@code minHeight} de
     * la pantalla al alto real del ScrollPane: mientras el contenido entra, sigue estirándose para
     * llenar la ventana igual que antes (el vgrow sigue funcionando); sólo cuando el contenido
     * necesita más alto que eso, crece más allá del viewport y ahí el ScrollPane deja bajar a
     * verlo -- mismo resultado que un {@code fitToHeight} fijo, pero sin perder el estirado.</p>
     */
    private void mostrarFx(Node nodo) {
        ScrollPane scroll = new ScrollPane(nodo);
        scroll.getStyleClass().add("contenido-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        if (nodo instanceof Region) {
            ((Region) nodo).minHeightProperty().bind(scroll.heightProperty());
        }

        scroll.setOpacity(0);
        contenido.getChildren().setAll(scroll);

        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), scroll);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void mostrarSwing(java.util.function.Supplier<javax.swing.JComponent> fabrica) {
        SwingHostPane alojador = new SwingHostPane();
        contenido.getChildren().setAll(alojador);
        javax.swing.SwingUtilities.invokeLater(() -> alojador.mostrarComponente(fabrica.get()));
    }

    /**
     * Abre la carga de resultados de la orden elegida en "Registrar Resultados" (ver
     * {@link vistas.javafx.registrarResultados.RegistrarResultadosScreen}), con la pantalla
     * JavaFX {@link vistas.javafx.registrarResultados.CargarResultadosScreen} -- reemplaza a la
     * vieja {@code vistas.registrarResultados.cargarReultados} (Swing, con columnas de ancho fijo
     * que dejaban el campo "Valor" fuera del área visible en ventanas de otro tamaño).
     */
    private void abrirCargaDeResultados(modelo.Paciente paciente, modelo.OrdenResumen orden) {
        vistas.javafx.registrarResultados.CargarResultadosScreen pantallaCarga =
                new vistas.javafx.registrarResultados.CargarResultadosScreen(
                        paciente, orden.getFecha(), orden.getNumeroOrden(),
                        orden.getIdPedidoAnalisis(), orden.getIdAnalisisTipo(), orden.getExamen());
        pantallaCarga.setAlCancelar(() -> navegar("4"));
        pantallaCarga.setAlGuardar(() -> navegar("4"));
        mostrarFx(pantallaCarga.construir());
    }

    /**
     * Aloja una pantalla Swing dentro del árbol de JavaFX y la fuerza a ocupar siempre todo el
     * espacio disponible.
     *
     * <p>Es necesario porque {@link SwingNode}, a diferencia de un {@code Region} de JavaFX
     * común, NO se agranda ni se achica solo cuando cambia el tamaño de su contenedor: su tamaño
     * mínimo/preferido/máximo queda fijado al tamaño de diseño del {@code JComponent} que aloja
     * (los 982x803, 1207x842, etc. en píxeles fijos con los que se armaron estas pantallas en el
     * editor de NetBeans). Sin este ajuste manual, en cualquier ventana más chica que ese diseño
     * la pantalla queda recortada -- tablas y botones corridos fuera del área visible, exactamente
     * el mismo problema del sidebar que se estaba resolviendo, pero ahora en el contenido.</p>
     *
     * <p>La solución (técnica estándar para alojar Swing dentro de JavaFX): en vez de dejar que
     * JavaFX decida el tamaño del {@code SwingNode} solo, {@link #layoutChildren()} -- que JavaFX
     * llama automáticamente cada vez que este panel cambia de tamaño -- le pide explícitamente al
     * {@code SwingNode} que ocupe exactamente el ancho/alto real disponibles. El {@code JComponent}
     * en sí se envuelve en un {@code JPanel} con {@code BorderLayout}, que sí estira su contenido
     * central para llenar el tamaño que se le da, sea cual sea su tamaño preferido de diseño --
     * mismo mecanismo que ya usaba {@code Principal.setForm()} en la versión anterior.</p>
     */
    private static final class SwingHostPane extends Pane {

        private final SwingNode nodo = new SwingNode();

        SwingHostPane() {
            getChildren().add(nodo);
        }

        /**
         * Reemplaza el contenido Swing alojado. Debe llamarse desde el Event Dispatch Thread de
         * Swing (igual que el viejo {@code Principal.setForm}).
         *
         * <p>El contenido va adentro de un {@code JScrollPane} (a través de {@link
         * PanelConAnchoDeVentana}) en vez de agregarse directo: estas pantallas Swing vienen con un
         * alto de diseño fijo en píxeles (982x803, 1207x842, etc., el tamaño del lienzo en el
         * editor de NetBeans), y si la ventana real termina siendo más baja que ese diseño, lo que
         * quede más abajo (por ejemplo el botón "Siguiente" de Registrar Resultados) se corta y
         * queda invisible en vez de mostrarse -- exactamente lo que se veía como un espacio en
         * blanco sin nada para hacer clic. Con el scroll, en vez de cortarse, se puede bajar para
         * llegar a esa parte. El ancho sigue estirándose para llenar la ventana como antes (ver
         * {@link PanelConAnchoDeVentana#getScrollableTracksViewportWidth()}); sólo el alto pasa a
         * poder desplazarse cuando hace falta.</p>
         */
        void mostrarComponente(javax.swing.JComponent componente) {
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(new PanelConAnchoDeVentana(componente));
            scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            javax.swing.JPanel envoltorio = new javax.swing.JPanel(new java.awt.BorderLayout());
            envoltorio.add(scroll, java.awt.BorderLayout.CENTER);
            nodo.setContent(envoltorio);
            // nodo.resize() es un método de Node y sólo puede llamarse desde el hilo de JavaFX.
            Platform.runLater(() -> nodo.resize(getWidth(), getHeight()));
        }

        @Override
        protected void layoutChildren() {
            nodo.resize(getWidth(), getHeight());
        }
    }

    /**
     * Envoltorio para meter una pantalla Swing dentro de un {@code JScrollPane} sin que pierda el
     * estirado horizontal que ya tenía (ver {@link SwingHostPane#mostrarComponente}): por defecto,
     * un {@code JScrollPane} le da a su contenido su propio ancho preferido de diseño en vez de
     * estirarlo, así que en una ventana más ancha que ese diseño quedaría una franja vacía a la
     * derecha. Implementando {@code Scrollable} y devolviendo {@code true} en
     * {@code getScrollableTracksViewportWidth()} se le dice al scroll "el ancho siempre es el de la
     * ventana" (mismo comportamiento de antes), mientras que en alto se deja su tamaño preferido de
     * diseño (no lo trackea), que es justamente lo que habilita el scroll vertical cuando ese alto
     * no entra en la ventana real.
     */
    private static final class PanelConAnchoDeVentana extends javax.swing.JPanel implements javax.swing.Scrollable {

        PanelConAnchoDeVentana(javax.swing.JComponent contenido) {
            super(new java.awt.BorderLayout());
            add(contenido, java.awt.BorderLayout.CENTER);
        }

        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle rectangulo, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle rectangulo, int orientacion, int direccion) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Se muestra cuando {@link #navegar} bloquea un intento de entrar a una pantalla sin permiso
     * -- la pantalla actual se queda como estaba (no se llega a limpiar {@code contenido}).
     */
    private void mostrarAccesoDenegado(String nombrePantalla) {
        Alert alerta = new Alert(AlertType.WARNING,
                "Tu usuario no tiene permiso para entrar a \"" + nombrePantalla + "\". Si creés que es un "
                + "error, pedile a un administrador que te dé acceso desde Usuarios.");
        alerta.setHeaderText(null);
        alerta.setTitle("Acceso restringido");
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    private void cerrarSesion() {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Deseas cerrar sesión?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Cerrar Sesión");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES && alCerrarSesion != null) {
                alCerrarSesion.run();
            }
        });
    }
}
