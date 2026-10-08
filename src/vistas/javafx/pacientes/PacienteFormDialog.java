package vistas.javafx.pacientes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.OrdenResumen;
import modelo.Paciente;
import modelo.Sexo;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;
import vistas.javafx.VentanaUtil;

/**
 * Popup "Ver Paciente" / "Editar Paciente", abierto desde {@link PacientesScreen}. Reemplaza a
 * {@code vistas.pacientes.Vistadetallepaciente} (el "ojito") + {@code vistas.pacientes.EditarPaciente}
 * (Swing) en un solo formulario con dos modos, mismo criterio que {@code UsuarioFormDialog} para
 * Usuarios. Diferencias a propósito, encontradas al leer el código viejo antes de portarlo:
 *
 * <ul>
 *   <li><b>No hay modo "Nuevo".</b> Ver el javadoc de {@link PacientesScreen}: un paciente siempre
 *   se crea desde "Nuevo Análisis", nunca desde acá -- ni la versión Swing lo permitía.</li>
 *   <li><b>Se sacó la acción "Compartir".</b> {@code vistas.pacientes.CompartitResultados} (Swing)
 *   era una ventana sin ninguna función real: su botón "Vista Previa" no tenía
 *   {@code addActionListener} conectado, así que no hacía nada al apretarlo. En vez de migrar un
 *   mockup vacío, se sacó -- se puede construir de verdad más adelante si hace falta compartir
 *   resultados con el paciente (mismo criterio que se usó para sacar DNI/Sexo/Celular de Usuarios,
 *   o el botón "+ Nuevo Paciente" de {@link PacientesScreen}: no arrastrar algo que no funciona).</li>
 *   <li><b>Eliminar es una baja lógica</b> ({@code dao.PacienteDAO#eliminar} hace un
 *   {@code UPDATE activo_paciente = 0}, no un {@code DELETE} -- un laboratorio no puede perder el
 *   historial de un paciente sólo por "eliminarlo" de la pantalla) -- ese botón vive en la tabla
 *   ({@link PacientesScreen}), no en este diálogo.</li>
 *   <li><b>Se agregó la pestaña "Historial de Exámenes"</b> (no estaba en {@code EditarPaciente},
 *   sólo en el "ojito" separado): junta ver y editar en un solo lugar, reusando
 *   {@code controlador.PacienteController#listarHistorial} (ya existía, no se usaba desde ningún
 *   diálogo de paciente todavía). Es sólo informativa en los dos modos -- no se edita nada ahí.</li>
 * </ul>
 */
public class PacienteFormDialog {

    public enum Modo {
        VER, EDITAR
    }

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Modo modo;
    private final Paciente pacienteActual;
    private final Runnable alGuardar;

    private TextField campoDni;
    private TextField campoNombreCompleto;
    private DatePicker campoFecha;
    private ComboBox<Sexo> comboSexo;
    private TextField campoCelular;
    private TextField campoEmail;

    private List<Sexo> listaSexos = Collections.emptyList();
    private List<OrdenResumen> historial = Collections.emptyList();

    /**
     * @param paciente paciente a mostrar/editar (nunca null -- no hay modo NUEVO, ver la clase).
     * @param alGuardar se ejecuta al guardar cambios con éxito (no en modo VER, ni si se cancela)
     *                   -- {@link PacientesScreen} lo usa para refrescar la tabla.
     */
    public PacienteFormDialog(Modo modo, Paciente paciente, Runnable alGuardar) {
        this.modo = modo;
        this.pacienteActual = paciente;
        this.alGuardar = alGuardar;
    }

    public void mostrar() {
        Stage ventana = new Stage(StageStyle.DECORATED);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(tituloVentana());

        listaSexos = PuenteEDT.ejecutar(() -> controlador.SexoController.listarTodos(null), Collections.emptyList());
        historial = PuenteEDT.ejecutar(
                () -> controlador.PacienteController.listarHistorial(null, pacienteActual.getIdPaciente()),
                Collections.emptyList());

        Label titulo = new Label(tituloVentana());
        titulo.getStyleClass().add("titulo-pantalla");

        Parent panelDatos = armarPanelDatosPersonales();
        Parent panelHistorial = armarPanelHistorial();
        StackPane areaContenido = new StackPane(panelDatos);

        HBox itemDatos = crearItemPestana("Datos Personales");
        HBox itemHistorial = crearItemPestana("Historial de Examenes");
        itemDatos.getStyleClass().add("tab-item-activo");
        itemDatos.setOnMouseClicked(evt -> {
            if (!itemDatos.getStyleClass().contains("tab-item-activo")) {
                itemDatos.getStyleClass().add("tab-item-activo");
                itemHistorial.getStyleClass().remove("tab-item-activo");
                areaContenido.getChildren().setAll(panelDatos);
            }
        });
        itemHistorial.setOnMouseClicked(evt -> {
            if (!itemHistorial.getStyleClass().contains("tab-item-activo")) {
                itemHistorial.getStyleClass().add("tab-item-activo");
                itemDatos.getStyleClass().remove("tab-item-activo");
                areaContenido.getChildren().setAll(panelHistorial);
            }
        });
        HBox barraPestanas = new HBox(24, itemDatos, itemHistorial);
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
        if (modo == Modo.EDITAR) {
            raizExterna.getChildren().add(armarBotones(ventana));
        }

        Scene escena = VentanaUtil.escenaAdaptable(ventana, raizExterna, 640, 620);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        ventana.showAndWait();
    }

    private String tituloVentana() {
        return modo == Modo.VER ? "Ver Paciente" : "Editar Paciente";
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

        campoDni = campoTexto(pacienteActual.getDni());
        campoDni.setPromptText("Ej: 30123456");
        campoDni.setDisable(soloLectura);

        campoNombreCompleto = campoTexto(pacienteActual.getNyaPaciente());
        campoNombreCompleto.setPromptText("Ej: Perez Juan");
        campoNombreCompleto.setDisable(soloLectura);

        campoFecha = new DatePicker();
        campoFecha.getStyleClass().add("campo-form");
        campoFecha.setMaxWidth(Double.MAX_VALUE);
        if (pacienteActual.getFechaNacimiento() != null) {
            campoFecha.setValue(aLocalDate(pacienteActual.getFechaNacimiento()));
        }
        campoFecha.setDisable(soloLectura);

        comboSexo = new ComboBox<>(FXCollections.observableArrayList(listaSexos));
        comboSexo.getStyleClass().add("campo-form");
        comboSexo.setMaxWidth(Double.MAX_VALUE);
        for (Sexo s : listaSexos) {
            if (s.getIdSexo() == pacienteActual.getIdSexo()) {
                comboSexo.setValue(s);
                break;
            }
        }
        comboSexo.setDisable(soloLectura);

        campoCelular = campoTexto(pacienteActual.getTelefono());
        campoCelular.setDisable(soloLectura);

        campoEmail = campoTexto(pacienteActual.getEmail());
        campoEmail.setDisable(soloLectura);

        VBox contenido = new VBox(18,
                campoConEtiqueta("DNI", campoDni),
                campoConEtiqueta("Apellido y Nombre", campoNombreCompleto),
                campoConEtiqueta("Fecha de Nacimiento", campoFecha),
                campoConEtiqueta("Sexo", comboSexo),
                campoConEtiqueta("Celular", campoCelular),
                campoConEtiqueta("Email", campoEmail));

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    // ---------------------------------------------------------------- Historial de Examenes

    /**
     * Sólo informativo (en los dos modos): todas las órdenes del paciente, de la más reciente a la
     * más vieja -- mismo dato que ya traía {@code DetalleExamenes} en el "ojito" de Swing, pero acá
     * como una tabla simple en vez de un detalle completo por examen (analitos y resultados), que
     * ya tiene su propia pantalla en Registrar/Cargar Resultados.
     */
    private Parent armarPanelHistorial() {
        Label lblTitulo = new Label("Historial de Examenes");
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        TableView<OrdenResumen> tabla = new TableView<>();

        TableColumn<OrdenResumen, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);

        TableColumn<OrdenResumen, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(vacio(d.getValue().getExamen())));
        colExamen.setPrefWidth(200);

        TableColumn<OrdenResumen, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getFecha() != null ? FORMATO_FECHA.format(d.getValue().getFecha()) : "—"));
        colFecha.setPrefWidth(95);

        TableColumn<OrdenResumen, OrdenResumen> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<OrdenResumen, OrdenResumen>() {
            @Override
            protected void updateItem(OrdenResumen orden, boolean vacio) {
                super.updateItem(orden, vacio);
                if (vacio || orden == null) {
                    setGraphic(null);
                    return;
                }
                String estado = orden.getEstado() == null ? "" : orden.getEstado();
                Label pill = new Label(estado.isEmpty() ? "—" : estado);
                pill.getStyleClass().add(claseEstado(estado));
                setGraphic(pill);
            }
        });
        colEstado.setPrefWidth(140);

        tabla.getColumns().setAll(colOrden, colExamen, colFecha, colEstado);
        tabla.setItems(FXCollections.observableArrayList(historial));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Este paciente todavía no tiene examenes registrados."));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox contenido = new VBox(14, lblTitulo, tabla);
        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    /**
     * Misma paleta de "pill" que ya usa {@code RegistrosScreen#claseEstado} -- se repite acá (no se
     * la extrajo a un lugar común) para no tocar esa pantalla en este cambio.
     */
    private static String claseEstado(String estado) {
        if (estado.startsWith("Urgente")) {
            return "pill-urgente";
        }
        if (estado.startsWith("Procesando")) {
            return "pill-procesando";
        }
        if (estado.startsWith("Completado")) {
            return "pill-activo";
        }
        if (estado.startsWith("Cancelado")) {
            return "pill-inactivo";
        }
        return "pill-pendiente";
    }

    // ---------------------------------------------------------------- Botones / guardar

    private HBox armarBotones(Stage ventana) {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonGuardar = new Button("Guardar Cambios");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar(ventana));

        HBox fila = new HBox(12, espaciador, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    /**
     * Junta lo tipeado/elegido en el formulario, lo guarda vía {@code PacienteController#actualizar}
     * y, si salió bien, avisa con {@link #alGuardar} -- mismo criterio de validación (DNI y Apellido
     * y Nombre no pueden estar vacíos) que ya tenía {@code EditarPaciente#guardarCambios} en Swing.
     */
    private void guardar(Stage ventana) {
        String dniTexto = campoDni.getText() == null ? "" : campoDni.getText().trim();
        String nombreTexto = campoNombreCompleto.getText() == null ? "" : campoNombreCompleto.getText().trim();
        if (dniTexto.isEmpty() || nombreTexto.isEmpty()) {
            Alert alerta = new Alert(AlertType.WARNING, "Completá el DNI y el Apellido y Nombre antes de guardar.");
            alerta.setHeaderText(null);
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
            return;
        }

        pacienteActual.setDni(dniTexto);
        pacienteActual.setNyaPaciente(nombreTexto);
        LocalDate fecha = campoFecha.getValue();
        pacienteActual.setFechaNacimiento(fecha != null ? aDate(fecha) : null);
        Sexo sexoElegido = comboSexo.getValue();
        if (sexoElegido != null) {
            pacienteActual.setIdSexo(sexoElegido.getIdSexo());
        }
        pacienteActual.setTelefono(campoCelular.getText());
        pacienteActual.setEmail(campoEmail.getText());

        Boolean ok = PuenteEDT.ejecutar(() -> controlador.PacienteController.actualizar(null, pacienteActual), false);
        if (ok != null && ok) {
            cerrarYAvisar(ventana);
        }
    }

    private void cerrarYAvisar(Stage ventana) {
        ventana.close();
        if (alGuardar != null) {
            alGuardar.run();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static LocalDate aLocalDate(Date fecha) {
        // PacienteDAO carga fecha_nacimiento con ResultSet.getDate(...), que devuelve un
        // java.sql.Date (aunque el campo del modelo sea java.util.Date). java.sql.Date no
        // representa hora/zona, y por diseño su toInstant() SIEMPRE tira
        // UnsupportedOperationException (está documentado en el Javadoc de la clase). Por eso
        // "Ver"/"Editar" en Pacientes explotaba apenas el paciente tenía fecha de nacimiento
        // cargada. java.sql.Date sí tiene su propio toLocalDate(), que es el camino correcto.
        if (fecha instanceof java.sql.Date) {
            return ((java.sql.Date) fecha).toLocalDate();
        }
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

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

    private static String vacio(String texto) {
        return texto == null || texto.trim().isEmpty() ? "—" : texto;
    }
}
