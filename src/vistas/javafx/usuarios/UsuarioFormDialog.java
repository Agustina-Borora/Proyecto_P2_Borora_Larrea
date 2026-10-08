package vistas.javafx.usuarios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.AnalisisTipo;
import modelo.Usuario;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;
import vistas.javafx.VentanaUtil;

/**
 * Popup "Nuevo Usuario" / "Ver Usuario" / "Editar Usuario", abierto desde {@link UsuariosScreen}.
 * Reemplaza a {@code vista.usuarios.CrearUsuario} (Swing) -- misma idea de tres modos en un solo
 * formulario, pero con diferencias a propósito, encontradas al leer el código viejo antes de
 * portarlo:
 *
 * <ul>
 *   <li><b>"+ Crear Usuario" ahora sí guarda.</b> En la versión Swing el botón estaba armado pero
 *   nadie le conectaba una acción -- no existía ni siquiera un método para insertar un usuario
 *   nuevo (ver {@code controlador.UsuarioController#crear} y {@code dao.UsuarioDAO#crear},
 *   agregados junto con este archivo). Acá sí crea la cuenta de verdad, con contraseña incluida.</li>
 *   <li><b>Se sacaron DNI, Sexo y Celular.</b> Estaban en el formulario de Swing pero nunca se
 *   guardaban -- ni {@code modelo.Usuario} tiene esos campos, ni ninguna tabla los recibía
 *   (confirmado leyendo {@code CrearUsuario#obtenerDatosUsuario} y {@code dao.UsuarioDAO}). Sexo
 *   ni siquiera tenía opciones reales cargadas ("Item 1".."Item 4", placeholder del Form Editor).
 *   En vez de arrastrar tres campos que no hacían nada a la pantalla nueva, se sacaron -- se
 *   pueden agregar de verdad más adelante si hace falta guardarlos (necesitaría columnas nuevas
 *   en la tabla <code>usuarios</code>).</li>
 *   <li><b>La contraseña, en modo Editar, es un botón de verdad</b> ("🔑 Generar nueva
 *   contraseña") en vez de un campo de texto disfrazado de botón (lo que hacía la versión Swing:
 *   un {@code JTextField} no editable con cursor de mano y un listener de mouse encima).</li>
 * </ul>
 *
 * <p>La pestaña "Permisos y Acceso" tiene dos partes, las dos con guardado real:</p>
 * <ul>
 *   <li><b>Acceso a Pantalla</b>: 11 interruptores (estilo switch verde, igual al diseño) que se
 *   guardan y releen de verdad (ver {@code controlador.UsuarioController#guardarPermisosPantalla}
 *   / {@code #obtenerPantallasPermitidas} -&gt; {@code dao.UsuarioDAO} -&gt; tabla
 *   {@code permisos_pantalla}, migración {@code sql/2026-09-29_permisos_pantalla.sql}).</li>
 *   <li><b>Permisos por Examen</b>: reemplaza a {@code vista.usuarios.PermisosExamen} (Swing) --
 *   esa grilla existía pero no estaba conectada a nada (no había tabla ni DAO), así que se tildaba
 *   sin guardar. Acá cada examen del catálogo tiene un selector de 3 opciones (Sin acceso / Solo
 *   ver / Cargar, mismo orden y default que el radio-group viejo) que también se guarda y relee de
 *   verdad (ver {@code controlador.UsuarioController#guardarPermisosExamen} /
 *   {@code #obtenerNivelesExamen} -&gt; {@code dao.PermisoExamenDAO} -&gt; tabla
 *   {@code permisos_examen}, migración {@code sql/2026-09-29_permisos_examen.sql}). Como el
 *   catálogo real puede tener muchos más exámenes que los 4 de ejemplo del diseño, se le agregó un
 *   buscador arriba de la grilla para no tener que scrollear toda la lista.</li>
 * </ul>
 *
 * <p><b>Las dos partes de arriba ya bloquean de verdad, no sólo guardan.</b> "Acceso a Pantalla" lo
 * hace a través de {@code AppShell} (ver {@code PANTALLA_POR_ID}, {@code construirDatosMenu} y el
 * chequeo al principio de {@code navegar}): sin permiso, el ítem ni aparece en el menú lateral y,
 * si de todos modos se intenta entrar por un salto interno, sale un cartel de "Acceso restringido".
 * "Permisos por Examen" lo hace a través de {@code controlador.PermisoController}, pero
 * <b>ojo con el alcance</b>: sólo lo consultan Registrar Resultados (para no listar órdenes sin
 * acceso) y Cargar Resultados (solo lectura o bloqueo total según el nivel). Nuevo Análisis y
 * Catálogo de Exámenes siguen bloqueados sólo a nivel pantalla completa -- no filtran examen por
 * examen adentro. Extender eso es un paso aparte, no incluido en este cambio.</p>
 */
public class UsuarioFormDialog {

    public enum Modo {
        NUEVO, VER, EDITAR
    }

    /** Mismo orden que tenía {@code vista.usuarios.CrearUsuario#activarTogglesAccesoPantalla}. */
    private static final String[] PANTALLAS_ACCESO = {
        "Escritorio", "Registros", "Registrar Resultados", "Cotizacion", "Usuarios", "Configuracion",
        "Pacientes", "Nuevo Analisis", "Catalogo de Examenes", "Pagos", "Estadisticas"
    };

    /** Mismo orden e izquierda-a-derecha que {@code vista.usuarios.PermisosExamen}. */
    private static final String[] NIVELES_EXAMEN = {"Sin acceso", "Solo ver", "Cargar"};

    private final Modo modo;
    private final Usuario usuarioActual;
    private final Runnable alGuardar;

    private TextField campoNombreCompleto;
    private TextField campoEmail;
    private PasswordField campoPassword;
    private Label etiquetaEstadoPassword;

    private final Button[] botonesRol = new Button[2];
    private final Button[] botonesEstado = new Button[2];
    private int indiceRolSeleccionado = 1;
    private int indiceEstadoSeleccionado = 0;

    /** Pantalla -&gt; estado del interruptor (array de 1 como "caja" mutable para el lambda del click). */
    private final Map<String, boolean[]> togglesAcceso = new LinkedHashMap<>();
    private Set<String> pantallasPermitidasIniciales = Collections.emptySet();

    private List<AnalisisTipo> examenesCatalogo = Collections.emptyList();
    private Map<Integer, String> nivelesExamenIniciales = Collections.emptyMap();
    /** Id de examen -&gt; índice elegido en {@link #NIVELES_EXAMEN} (mismo truco de array-caja). */
    private final Map<Integer, int[]> nivelSeleccionadoPorExamen = new LinkedHashMap<>();
    private final List<HBox> filasExamen = new ArrayList<>();
    private final List<String> nombresPorFilaExamen = new ArrayList<>();
    private TextField campoBuscarExamen;

    /**
     * @param usuario null en modo NUEVO; el usuario a mostrar/editar en VER/EDITAR.
     * @param alGuardar se ejecuta al crear o guardar cambios con éxito (no en modo VER, ni si se
     *                   cancela) -- {@link UsuariosScreen} lo usa para refrescar la tabla.
     */
    public UsuarioFormDialog(Modo modo, Usuario usuario, Runnable alGuardar) {
        this.modo = modo;
        this.usuarioActual = usuario;
        this.alGuardar = alGuardar;
    }

    public void mostrar() {
        Stage ventana = new Stage(StageStyle.DECORATED);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(tituloVentana());

        examenesCatalogo = PuenteEDT.ejecutar(
                () -> controlador.AnalisisTipoController.listarTodos(null), Collections.emptyList());
        if (usuarioActual != null) {
            pantallasPermitidasIniciales = PuenteEDT.ejecutar(
                    () -> controlador.UsuarioController.obtenerPantallasPermitidas(null, usuarioActual.getIdUsuario()),
                    Collections.emptySet());
            nivelesExamenIniciales = PuenteEDT.ejecutar(
                    () -> controlador.UsuarioController.obtenerNivelesExamen(null, usuarioActual.getIdUsuario()),
                    Collections.emptyMap());
        }

        Label titulo = new Label(tituloVentana());
        titulo.getStyleClass().add("titulo-pantalla");

        Parent panelDatos = armarPanelDatosPersonales();
        Parent panelPermisos = armarPanelPermisos();
        StackPane areaContenido = new StackPane(panelDatos);

        HBox itemDatos = crearItemPestana("Datos Personales");
        HBox itemPermisos = crearItemPestana("Permisos y Acceso");
        itemDatos.getStyleClass().add("tab-item-activo");
        itemDatos.setOnMouseClicked(evt -> {
            if (!itemDatos.getStyleClass().contains("tab-item-activo")) {
                itemDatos.getStyleClass().add("tab-item-activo");
                itemPermisos.getStyleClass().remove("tab-item-activo");
                areaContenido.getChildren().setAll(panelDatos);
            }
        });
        itemPermisos.setOnMouseClicked(evt -> {
            if (!itemPermisos.getStyleClass().contains("tab-item-activo")) {
                itemPermisos.getStyleClass().add("tab-item-activo");
                itemDatos.getStyleClass().remove("tab-item-activo");
                areaContenido.getChildren().setAll(panelPermisos);
            }
        });
        HBox barraPestanas = new HBox(24, itemDatos, itemPermisos);
        barraPestanas.setStyle("-fx-border-color: transparent transparent #EEF1EE transparent; -fx-border-width: 0 0 1 0;");

        VBox contenidoScroll = new VBox(16, titulo, barraPestanas, areaContenido);
        ScrollPane scroll = new ScrollPane(contenidoScroll);
        scroll.getStyleClass().add("config-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox raizExterna = new VBox(16, scroll);
        raizExterna.getStyleClass().add("pantalla");
        raizExterna.setPadding(new Insets(20));
        if (modo != Modo.VER) {
            raizExterna.getChildren().add(armarBotones(ventana));
        }

        Scene escena = VentanaUtil.escenaAdaptable(ventana, raizExterna, 720, 700);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        ventana.showAndWait();
    }

    private String tituloVentana() {
        switch (modo) {
            case VER:
                return "Ver Usuario";
            case EDITAR:
                return "Editar Usuario";
            default:
                return "Nuevo Usuario";
        }
    }

    private HBox crearItemPestana(String texto) {
        Label etiqueta = new Label(texto);
        HBox item = new HBox(0, etiqueta);
        item.getStyleClass().add("tab-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    // ---------------------------------------------------------------- Datos Personales

    private Parent armarPanelDatosPersonales() {
        boolean soloLectura = modo == Modo.VER;

        campoNombreCompleto = campoTexto(usuarioActual != null ? usuarioActual.getApellidoYNombre() : "");
        campoNombreCompleto.setPromptText("Ej: Larrea Zaira");
        campoNombreCompleto.setDisable(soloLectura);

        campoEmail = campoTexto(usuarioActual != null ? usuarioActual.getEmail() : "");
        campoEmail.setPromptText("Ej: zaira@labsangregorio.com");
        campoEmail.setDisable(soloLectura);

        indiceRolSeleccionado = usuarioActual != null && "Administrador".equalsIgnoreCase(usuarioActual.getRol()) ? 0 : 1;
        indiceEstadoSeleccionado = usuarioActual != null && !usuarioActual.isActivo() ? 1 : 0;

        HBox filaRol = armarSelectorPildora(botonesRol, new String[]{"Administrador", "Tecnico"},
                indiceRolSeleccionado, indice -> indiceRolSeleccionado = indice, soloLectura);
        HBox filaEstado = armarSelectorPildora(botonesEstado, new String[]{"Activo", "Inactivo"},
                indiceEstadoSeleccionado, indice -> indiceEstadoSeleccionado = indice, soloLectura);

        VBox contenido = new VBox(18,
                campoConEtiqueta("Apellido y Nombre", campoNombreCompleto),
                campoConEtiqueta("Email", campoEmail),
                campoConEtiqueta("Rol", filaRol),
                campoConEtiqueta("Estado de la cuenta", filaEstado));

        if (modo != Modo.VER) {
            contenido.getChildren().add(armarSeccionPassword());
        }

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    private VBox armarSeccionPassword() {
        if (modo == Modo.NUEVO) {
            campoPassword = new PasswordField();
            campoPassword.getStyleClass().add("campo-form");
            campoPassword.setPromptText("Mínimo 8 caracteres");
            campoPassword.setMaxWidth(Double.MAX_VALUE);
            return campoConEtiqueta("Contraseña", campoPassword);
        }

        // EDITAR: no se puede ver ni escribir la anterior, sólo generarle una nueva -- mismo
        // criterio que ya tenía CrearUsuario en Swing, con un botón normal en vez de disfrazar de
        // botón un campo de texto (ver el javadoc de la clase).
        etiquetaEstadoPassword = new Label("Sin cambios");
        etiquetaEstadoPassword.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 12.5px;");

        Button botonGenerar = new Button("🔑  Generar nueva contraseña");
        botonGenerar.getStyleClass().add("boton-secundario");
        botonGenerar.setOnAction(evt -> generarNuevaPassword());

        HBox fila = new HBox(12, botonGenerar, etiquetaEstadoPassword);
        fila.setAlignment(Pos.CENTER_LEFT);
        return campoConEtiqueta("Contraseña", fila);
    }

    private void generarNuevaPassword() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Generar nueva contraseña");
        dialogo.setHeaderText(null);
        dialogo.setContentText("Ingresá la nueva contraseña para este usuario (mínimo 8 caracteres):");
        Optional<String> nueva = dialogo.showAndWait();
        if (!nueva.isPresent() || nueva.get().trim().isEmpty()) {
            return;
        }
        String nuevaPassword = nueva.get();
        int largoMinimo = controlador.PasswordController.getLargoMinimoPassword();
        if (nuevaPassword.length() < largoMinimo) {
            Alert alerta = new Alert(AlertType.WARNING,
                    "La contraseña debe tener al menos " + largoMinimo + " caracteres.");
            alerta.setHeaderText(null);
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
            return;
        }
        Boolean ok = PuenteEDT.ejecutar(
                () -> controlador.PasswordController.cambiarPassword(null, usuarioActual.getIdUsuario(), nuevaPassword),
                false);
        if (ok != null && ok) {
            etiquetaEstadoPassword.setText("✓ Contraseña actualizada");
            etiquetaEstadoPassword.setStyle("-fx-text-fill: #1E7A42; -fx-font-size: 12.5px;");
        } else {
            Alert alerta = new Alert(AlertType.ERROR, "Ocurrió un error al guardar la contraseña. Probá de nuevo.");
            alerta.setHeaderText(null);
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
        }
    }

    /**
     * Par de botones tipo "píldora" de selección única (Rol: Administrador/Tecnico, Estado:
     * Activo/Inactivo) -- no hay ningún control nativo de JavaFX con este aspecto en el resto de
     * la app, así que se arma a mano con dos {@link Button} normales y se lleva la selección en
     * una variable aparte (mismo criterio que ya usaba {@code CrearUsuario.marcarSeleccionado} en
     * Swing), en vez de {@code ToggleButton}/{@code ToggleGroup} para no sumar un control que
     * después habría que re-estilizar de cero para que no se vea como un checkbox cualquiera.
     */
    private HBox armarSelectorPildora(Button[] botones, String[] opciones, int indiceInicial,
            IntConsumer alSeleccionar, boolean soloLectura) {
        for (int i = 0; i < opciones.length; i++) {
            int indice = i;
            Button boton = new Button(opciones[i]);
            botones[i] = boton;
            boton.setDisable(soloLectura);
            boton.setOnAction(evt -> {
                marcarPildoraSeleccionada(botones, indice);
                alSeleccionar.accept(indice);
            });
        }
        marcarPildoraSeleccionada(botones, indiceInicial);
        return new HBox(8, botones);
    }

    private void marcarPildoraSeleccionada(Button[] botones, int indiceSeleccionado) {
        for (int i = 0; i < botones.length; i++) {
            boolean seleccionado = i == indiceSeleccionado;
            botones[i].setStyle(seleccionado
                    ? "-fx-background-color: #E4F5EA; -fx-text-fill: #1E5C3D; -fx-font-weight: bold; "
                    + "-fx-background-radius: 14; -fx-border-color: #1E5C3D; -fx-border-radius: 14; "
                    + "-fx-padding: 6 16 6 16; -fx-cursor: hand;"
                    : "-fx-background-color: white; -fx-text-fill: #3A423E; -fx-background-radius: 14; "
                    + "-fx-border-color: #DCE1E6; -fx-border-radius: 14; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        }
    }

    // ---------------------------------------------------------------- Permisos y Acceso

    private Parent armarPanelPermisos() {
        boolean soloLectura = modo == Modo.VER;

        VBox contenedor = new VBox(16, armarTarjetaAccesoPantalla(soloLectura), armarTarjetaPermisosExamen(soloLectura));
        return contenedor;
    }

    private Parent armarTarjetaAccesoPantalla(boolean soloLectura) {
        Label lblTitulo = new Label("Acceso a Pantalla");
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label lblAviso = new Label(
                "Sin acceso, la pantalla no aparece en el menú y, si se intenta entrar igual, "
                + "queda bloqueada con un aviso.");
        lblAviso.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #8C9490;");
        lblAviso.setWrapText(true);
        lblAviso.setMaxWidth(600);

        VBox columnaIzquierda = new VBox(10);
        VBox columnaDerecha = new VBox(10);
        HBox.setHgrow(columnaIzquierda, Priority.ALWAYS);
        HBox.setHgrow(columnaDerecha, Priority.ALWAYS);
        for (int i = 0; i < PANTALLAS_ACCESO.length; i++) {
            String pantalla = PANTALLAS_ACCESO[i];
            HBox fila = crearFilaAccesoPantalla(pantalla, soloLectura);
            (i < 6 ? columnaIzquierda : columnaDerecha).getChildren().add(fila);
        }
        HBox columnas = new HBox(16, columnaIzquierda, columnaDerecha);

        VBox contenido = new VBox(14, lblTitulo, lblAviso, columnas);
        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    /**
     * Una fila de "Acceso a Pantalla": nombre a la izquierda, interruptor verde a la derecha --
     * mismo estilo de switch que pedía el diseño (antes eran {@link javafx.scene.control.CheckBox}
     * comunes).
     */
    private HBox crearFilaAccesoPantalla(String pantalla, boolean soloLectura) {
        boolean[] estado = {pantallasPermitidasIniciales.contains(pantalla)};
        togglesAcceso.put(pantalla, estado);

        Label etiqueta = new Label(pantalla);
        etiqueta.setStyle("-fx-font-size: 13px; -fx-text-fill: #3A423E;");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        StackPane interruptor = crearInterruptor(estado, soloLectura);

        HBox fila = new HBox(10, etiqueta, espaciador, interruptor);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setStyle("-fx-background-color: white; -fx-border-color: #E7EAE8; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14 10 14;");
        return fila;
    }

    /**
     * Interruptor tipo switch (pista + perilla), armado a mano igual que
     * {@link #armarSelectorPildora} -- no hay ningún control nativo de JavaFX con este aspecto.
     * {@code estado} es un array de 1 posición para que el lambda del click pueda mutar el valor
     * (mismo truco que ya usa el resto de la app para variables "por referencia", ej.
     * {@code AppShell} con la referencia a la pantalla Swing en un array).
     */
    private StackPane crearInterruptor(boolean[] estado, boolean soloLectura) {
        Region perilla = new Region();
        perilla.setPrefSize(14, 14);
        perilla.setMaxSize(14, 14);
        perilla.setStyle("-fx-background-color: white; -fx-background-radius: 7;");

        StackPane pista = new StackPane(perilla);
        pista.setPrefSize(38, 20);
        pista.setMinSize(38, 20);
        pista.setMaxSize(38, 20);
        StackPane.setMargin(perilla, new Insets(0, 3, 0, 3));

        Runnable refrescar = () -> {
            pista.setStyle("-fx-background-radius: 10; -fx-background-color: "
                    + (estado[0] ? "#1E5C3D" : "#DCE1E6") + "; -fx-cursor: " + (soloLectura ? "default" : "hand") + ";");
            StackPane.setAlignment(perilla, estado[0] ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        };
        refrescar.run();

        if (!soloLectura) {
            pista.setOnMouseClicked(evt -> {
                estado[0] = !estado[0];
                refrescar.run();
            });
        }
        return pista;
    }

    /**
     * Tarjeta "Permisos por Examen" -- reemplazo funcional de {@code vista.usuarios.PermisosExamen}
     * (ver javadoc de la clase). Trae el catálogo activo completo, así que se le suma un buscador
     * arriba de la grilla para no tener que scrollear toda la lista.
     */
    private Parent armarTarjetaPermisosExamen(boolean soloLectura) {
        Label lblTitulo = new Label("Permisos por Examen");
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label lblSubtitulo = new Label(
                "Define qué puede hacer este usuario con cada tipo de análisis en Registrar/Cargar "
                + "Resultados (Nuevo Análisis y Catálogo de Exámenes no distinguen por examen).");
        lblSubtitulo.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #8C9490;");

        campoBuscarExamen = new TextField();
        campoBuscarExamen.getStyleClass().add("campo-form");
        campoBuscarExamen.setPromptText("🔍  Buscar examen...");
        campoBuscarExamen.setMaxWidth(Double.MAX_VALUE);
        campoBuscarExamen.setOnKeyReleased(evt -> filtrarFilasExamen());

        HBox encabezado = armarEncabezadoPermisosExamen();

        VBox filas = new VBox(6);
        filasExamen.clear();
        nombresPorFilaExamen.clear();
        for (AnalisisTipo examen : examenesCatalogo) {
            HBox fila = crearFilaPermisoExamen(examen, soloLectura);
            filasExamen.add(fila);
            nombresPorFilaExamen.add(examen.getNombreAnalisis() == null ? "" : examen.getNombreAnalisis().toLowerCase());
            filas.getChildren().add(fila);
        }
        if (examenesCatalogo.isEmpty()) {
            Label lblVacio = new Label("No hay exámenes activos en el catálogo.");
            lblVacio.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");
            filas.getChildren().add(lblVacio);
        }

        VBox contenido = new VBox(12, lblTitulo, lblSubtitulo, campoBuscarExamen, encabezado, filas);
        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    private HBox armarEncabezadoPermisosExamen() {
        Label lblExamen = new Label("EXAMEN");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        HBox colsNiveles = new HBox(8);
        for (String nivel : NIVELES_EXAMEN) {
            Label lbl = new Label(nivel);
            lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #8C9490;");
            lbl.setMinWidth(76);
            lbl.setAlignment(Pos.CENTER);
            colsNiveles.getChildren().add(lbl);
        }
        lblExamen.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #8C9490;");

        HBox encabezado = new HBox(10, lblExamen, espaciador, colsNiveles);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.setPadding(new Insets(0, 14, 0, 14));
        return encabezado;
    }

    /**
     * Una fila de "Permisos por Examen": nombre a la izquierda, selector de 3 opciones (Sin
     * acceso / Solo ver / Cargar) a la derecha -- reusa {@link #armarSelectorPildora}, guardando
     * el índice elegido en {@link #nivelSeleccionadoPorExamen} para poder leerlo al guardar.
     */
    private HBox crearFilaPermisoExamen(AnalisisTipo examen, boolean soloLectura) {
        int idExamen = examen.getIdAnalisisTipo();
        String nivelInicial = nivelesExamenIniciales.getOrDefault(idExamen, NIVELES_EXAMEN[0]);
        int indiceInicial = 0;
        for (int i = 0; i < NIVELES_EXAMEN.length; i++) {
            if (NIVELES_EXAMEN[i].equals(nivelInicial)) {
                indiceInicial = i;
                break;
            }
        }

        Label etiqueta = new Label(examen.getNombreAnalisis());
        etiqueta.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #3A423E;");
        etiqueta.setWrapText(true);
        etiqueta.setPrefWidth(230);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        int[] indiceSeleccionado = {indiceInicial};
        nivelSeleccionadoPorExamen.put(idExamen, indiceSeleccionado);
        Button[] botones = new Button[NIVELES_EXAMEN.length];
        HBox selector = armarSelectorPildora(botones, NIVELES_EXAMEN, indiceInicial,
                indice -> indiceSeleccionado[0] = indice, soloLectura);

        HBox fila = new HBox(10, etiqueta, espaciador, selector);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setStyle("-fx-background-color: white; -fx-border-color: #E7EAE8; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14 10 14;");
        return fila;
    }

    private void filtrarFilasExamen() {
        String texto = campoBuscarExamen.getText() == null ? "" : campoBuscarExamen.getText().trim().toLowerCase();
        for (int i = 0; i < filasExamen.size(); i++) {
            boolean visible = texto.isEmpty() || nombresPorFilaExamen.get(i).contains(texto);
            filasExamen.get(i).setVisible(visible);
            filasExamen.get(i).setManaged(visible);
        }
    }

    // ---------------------------------------------------------------- Botones / guardar

    private HBox armarBotones(Stage ventana) {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonGuardar = new Button(modo == Modo.EDITAR ? "Guardar Cambios" : "+  Crear Usuario");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar(ventana));

        HBox fila = new HBox(12, espaciador, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    /**
     * Arma el {@link Usuario} con lo cargado en el formulario y lo crea o actualiza según el modo
     * -- mismo split de "Apellido y Nombre" en dos campos separados que ya usaba
     * {@code CrearUsuario.obtenerDatosUsuario} en Swing (primer espacio separa apellido de
     * nombre), para no cambiar cómo quedan guardados los usuarios existentes.
     */
    private void guardar(Stage ventana) {
        String texto = campoNombreCompleto.getText() == null ? "" : campoNombreCompleto.getText().trim();
        String email = campoEmail.getText() == null ? "" : campoEmail.getText().trim();
        if (texto.isEmpty() || email.isEmpty()) {
            Alert alerta = new Alert(AlertType.WARNING, "Completá el nombre y el email antes de guardar.");
            alerta.setHeaderText(null);
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
            return;
        }

        int espacio = texto.indexOf(' ');
        String apellido = espacio == -1 ? texto : texto.substring(0, espacio);
        String nombre = espacio == -1 ? "" : texto.substring(espacio + 1).trim();
        String rol = indiceRolSeleccionado == 0 ? "Administrador" : "Tecnico";
        boolean activo = indiceEstadoSeleccionado == 0;

        if (modo == Modo.NUEVO) {
            Usuario nuevo = new Usuario();
            nuevo.setApellido(apellido);
            nuevo.setNombre(nombre);
            nuevo.setEmail(email);
            nuevo.setRol(rol);
            nuevo.setActivo(activo);
            String password = campoPassword.getText();

            Integer idCreado = PuenteEDT.ejecutar(
                    () -> controlador.UsuarioController.crear(null, nuevo, password), null);
            if (idCreado != null) {
                guardarPermisosPantalla(idCreado);
                guardarPermisosExamen(idCreado);
                cerrarYAvisar(ventana);
            }
        } else {
            Usuario editado = new Usuario();
            editado.setIdUsuario(usuarioActual.getIdUsuario());
            editado.setApellido(apellido);
            editado.setNombre(nombre);
            editado.setEmail(email);
            editado.setRol(rol);
            editado.setActivo(activo);

            Boolean ok = PuenteEDT.ejecutar(() -> controlador.UsuarioController.actualizar(null, editado), false);
            if (ok != null && ok) {
                guardarPermisosPantalla(editado.getIdUsuario());
                guardarPermisosExamen(editado.getIdUsuario());
                cerrarYAvisar(ventana);
            }
        }
    }

    /**
     * Guarda el estado actual de los 11 interruptores de "Acceso a Pantalla" para el usuario
     * recién creado/editado. Se llama después de que {@code crear}/{@code actualizar} confirmó
     * éxito -- si esto llegara a fallar, el usuario ya quedó guardado igual (mismo criterio que
     * {@link #generarNuevaPassword()}: son acciones aparte, no atadas a la misma transacción).
     */
    private void guardarPermisosPantalla(int idUsuario) {
        Map<String, Boolean> permisos = new LinkedHashMap<>();
        for (Map.Entry<String, boolean[]> entrada : togglesAcceso.entrySet()) {
            permisos.put(entrada.getKey(), entrada.getValue()[0]);
        }
        PuenteEDT.ejecutar(
                () -> controlador.UsuarioController.guardarPermisosPantalla(null, idUsuario, permisos), false);
    }

    /**
     * Guarda el nivel elegido para cada examen de "Permisos por Examen" -- mismo criterio que
     * {@link #guardarPermisosPantalla}, acción aparte después de confirmado el alta/edición.
     */
    private void guardarPermisosExamen(int idUsuario) {
        Map<Integer, String> niveles = new LinkedHashMap<>();
        for (Map.Entry<Integer, int[]> entrada : nivelSeleccionadoPorExamen.entrySet()) {
            niveles.put(entrada.getKey(), NIVELES_EXAMEN[entrada.getValue()[0]]);
        }
        PuenteEDT.ejecutar(
                () -> controlador.UsuarioController.guardarPermisosExamen(null, idUsuario, niveles), false);
    }

    private void cerrarYAvisar(Stage ventana) {
        ventana.close();
        if (alGuardar != null) {
            alGuardar.run();
        }
    }

    // ---------------------------------------------------------------- helpers de layout
    // (mismo criterio que vistas.javafx.configuracion.ConfiguracionScreen)

    private TextField campoTexto(String valorInicial) {
        TextField campo = new TextField(valorInicial);
        campo.getStyleClass().add("campo-form");
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private VBox campoConEtiqueta(String etiquetaTexto, Node campo) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        return new VBox(6, etiqueta, campo);
    }
}
