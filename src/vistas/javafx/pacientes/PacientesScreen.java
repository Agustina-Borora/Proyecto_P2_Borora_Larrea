package vistas.javafx.pacientes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Paciente;
import vistas.javafx.PuenteEDT;
import vistas.javafx.menu.IconoFx;

/**
 * Pantalla "Pacientes", en JavaFX puro -- reemplaza a {@code vistas.formulariosPrincipales.Pacientes}
 * + {@code vistas.pacientes.TablaPacientes} (Swing), que seguía con el aspecto de antes de pasar el
 * resto del sistema a JavaFX (mismo pedido de Agus que ya se hizo con Usuarios/Pagos/Registros).
 * El detalle/edición vive en {@link PacienteFormDialog} -- ver su javadoc para qué se dejó afuera
 * a propósito.
 *
 * <p><b>No hay botón "+ Nuevo Paciente".</b> En el sistema entero -- Swing y ahora JavaFX -- nunca
 * existió un alta de paciente desde esta pantalla: un paciente se crea únicamente como parte del
 * flujo "Nuevo Análisis" (ver {@code controlador.NuevoAnalisisController#generarOrden}, que hace el
 * insert directo). Agregar un alta acá sería una funcionalidad nueva que no se pidió, así que se
 * mantiene igual que en la versión Swing: esta pantalla sólo lista, ve, edita y elimina.</p>
 */
public class PacientesScreen {

    private final ObservableList<Paciente> pacientesVisibles = FXCollections.observableArrayList();

    /**
     * Copia completa (sin filtrar) de lo que trajo la base, para poder volver a filtrar por texto
     * ({@link #filtrarPorTexto}) sin volver a consultarla -- mismo criterio que
     * {@code UsuariosScreen#todosLosUsuarios}.
     */
    private final List<Paciente> todosLosPacientes = new ArrayList<>();

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Label valorTotal = new Label("0");
    private final Label valorConObraSocial = new Label("0");
    private final Label valorParticulares = new Label("0");

    public Parent construir() {
        VBox raiz = new VBox(20);
        raiz.getStyleClass().add("pantalla");

        VBox tarjetaTabla = armarTarjetaTabla();
        VBox.setVgrow(tarjetaTabla, Priority.ALWAYS);

        raiz.getChildren().addAll(armarEncabezado(), armarTarjetasResumen(), tarjetaTabla);

        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        Label titulo = new Label("Pacientes");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Pacientes cargados en el sistema");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(4, titulo, subtitulo);

        HBox fila = new HBox(12, textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /**
     * Mes 8 (rediseño visual): tarjetas con el mismo lenguaje visual que ya aprobó la clienta
     * para el Escritorio (ícono + franja de color + efecto hover) en vez de las cajas de color
     * liso de antes -- ver {@link vistas.javafx.TarjetaStatUtil}, que ahora reutilizan todas las
     * pantallas con tarjetas de resumen.
     */
    private HBox armarTarjetasResumen() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir(
                        "Total de Pacientes", valorTotal, "Pacientes cargados en el sistema", "azul", "👥"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Con Obra Social", valorConObraSocial, "Tienen al menos una asociada", "verde", "🛡️"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Particulares", valorParticulares, "Sin obra social asociada", "ambar", "👤"));
    }

    private VBox armarTarjetaTabla() {
        Label titulo = new Label("Listado");
        titulo.getStyleClass().add("titulo-seccion");

        TableView<Paciente> tabla = armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox tarjeta = new VBox(14, titulo, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private TableView<Paciente> armarTabla() {
        TableColumn<Paciente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getDni())));
        colDni.setPrefWidth(100);
        colDni.setMinWidth(80);

        TableColumn<Paciente, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNyaPaciente())));
        colPaciente.setPrefWidth(200);
        colPaciente.setMinWidth(140);

        TableColumn<Paciente, String> colEdad = new TableColumn<>("Edad");
        colEdad.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFechaNacimiento() != null
                ? d.getValue().calcularEdad() + " años" : "—"));
        colEdad.setPrefWidth(80);
        colEdad.setMinWidth(70);

        TableColumn<Paciente, String> colTelefono = new TableColumn<>("Telefono");
        colTelefono.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getTelefono())));
        colTelefono.setPrefWidth(120);
        colTelefono.setMinWidth(90);

        TableColumn<Paciente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getEmail())));
        colEmail.setPrefWidth(200);
        colEmail.setMinWidth(120);

        TableColumn<Paciente, String> colObraSocial = new TableColumn<>("Obra Social");
        colObraSocial.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNombreObraSocial() != null ? d.getValue().getNombreObraSocial() : "Particular"));
        colObraSocial.setPrefWidth(180);
        colObraSocial.setMinWidth(110);

        TableColumn<Paciente, String> colUltimoExamen = new TableColumn<>("Ultimo Examen");
        colUltimoExamen.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUltimoExamen() != null ? FORMATO_FECHA.format(d.getValue().getUltimoExamen()) : "—"));
        colUltimoExamen.setPrefWidth(110);
        colUltimoExamen.setMinWidth(95);

        TableColumn<Paciente, Paciente> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<Paciente, Paciente>() {
            private final Button botonVer = new Button();
            private final Button botonEditar = new Button();
            private final Button botonEliminar = new Button();
            private final HBox caja = new HBox(6, botonVer, botonEditar, botonEliminar);

            {
                botonVer.setGraphic(IconoFx.vista("ojo", 16));
                botonVer.getStyleClass().add("boton-fila-icono");
                botonVer.setTooltip(new Tooltip("Ver el detalle del paciente"));

                botonEditar.setGraphic(IconoFx.vista("lapiz", 16));
                botonEditar.getStyleClass().add("boton-fila-icono");
                botonEditar.setTooltip(new Tooltip("Editar DNI, nombre, fecha de nacimiento, sexo, celular o email"));

                // Texto en vez de un ícono cargado por archivo -- mismo motivo que ya se usó en
                // UsuariosScreen: no hay garantía de que exista un ícono de tacho de basura entre
                // los recursos de este proyecto.
                botonEliminar.setText("🗑");
                botonEliminar.getStyleClass().add("boton-fila-icono");
                botonEliminar.setStyle("-fx-text-fill: #B3261E;");
                botonEliminar.setTooltip(new Tooltip("Eliminar el paciente"));

                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Paciente paciente, boolean vacio) {
                super.updateItem(paciente, vacio);
                if (vacio || paciente == null) {
                    setGraphic(null);
                    return;
                }
                botonVer.setOnAction(evt -> new PacienteFormDialog(PacienteFormDialog.Modo.VER, paciente, null).mostrar());
                botonEditar.setOnAction(evt -> new PacienteFormDialog(
                        PacienteFormDialog.Modo.EDITAR, paciente, PacientesScreen.this::cargarDatos).mostrar());
                botonEliminar.setOnAction(evt -> confirmarEliminar(paciente));
                setGraphic(caja);
            }
        });
        colAcciones.setPrefWidth(120);
        colAcciones.setMinWidth(110);
        // No resizable: esta es la causa más probable de "las acciones no funcionan" -- con 8
        // columnas en esta tabla, CONSTRAINED_RESIZE_POLICY puede necesitar achicar columnas por
        // DEBAJO de su propio mínimo para que todo encajen en una ventana no muy ancha (el
        // minWidth de arriba no alcanza por sí solo para evitarlo), y a esta columna eso la deja
        // con los 3 botones (Ver/Editar/Eliminar) amontonados o superpuestos: se siguen viendo más
        // o menos, pero el click cae en el botón de al lado o en ningún lado, así que parecen no
        // responder. Marcarla no-resizable la saca del reparto: siempre mide exactamente lo que se
        // le pidió (2026-10-01, mismo ajuste aplicado a todas las tablas con columna de Acciones).
        colAcciones.setResizable(false);

        TableView<Paciente> tabla = new TableView<>();
        tabla.getColumns().setAll(colDni, colPaciente, colEdad, colTelefono, colEmail, colObraSocial, colUltimoExamen, colAcciones);
        tabla.setItems(pacientesVisibles);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay pacientes cargados todavía."));
        return tabla;
    }

    /**
     * Pide confirmación y da de baja (lógica, no se borra la fila -- ver el javadoc de
     * {@code dao.PacienteDAO#eliminar}) al paciente elegido. Mismo mecanismo que
     * {@code UsuariosScreen#confirmarEliminar}: el paciente deja de listarse acá, pero su historial
     * de órdenes/resultados se conserva igual.
     */
    private void confirmarEliminar(Paciente paciente) {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Seguro que querés eliminar a " + vacio(paciente.getNyaPaciente())
                + "? Deja de aparecer en este listado, pero su historial de examenes se conserva.",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Eliminar paciente");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES) {
                Boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.PacienteController.eliminar(null, paciente.getIdPaciente()), false);
                if (ok != null && ok) {
                    cargarDatos();
                }
            }
        });
    }

    private void cargarDatos() {
        List<Paciente> pacientes = PuenteEDT.ejecutar(
                () -> controlador.PacienteController.listarTodos(null), Collections.emptyList());
        todosLosPacientes.clear();
        todosLosPacientes.addAll(pacientes);
        pacientesVisibles.setAll(pacientes);
        actualizarTotales(pacientes);
    }

    /**
     * Cuenta pacientes con/sin obra social sobre la lista dada -- se llama también desde
     * {@link #filtrarPorTexto} para que las tarjetas de arriba siempre coincidan con lo que se ve
     * en la tabla (mismo criterio que {@code UsuariosScreen#actualizarTotales}).
     */
    private void actualizarTotales(List<Paciente> pacientes) {
        int conObraSocial = 0;
        for (Paciente p : pacientes) {
            if (p.getNombreObraSocial() != null && !p.getNombreObraSocial().trim().isEmpty()) {
                conObraSocial++;
            }
        }
        valorTotal.setText(String.valueOf(pacientes.size()));
        valorConObraSocial.setText(String.valueOf(conObraSocial));
        valorParticulares.setText(String.valueOf(pacientes.size() - conObraSocial));
    }

    /**
     * Filtra por DNI, Apellido y Nombre, email u obra social -- la llama el buscador del header a
     * cada tecla, cuando esta pantalla está activa (ver {@code AppShell#navegar}, caso "5_1").
     * Filtra sobre {@link #todosLosPacientes} (lo ya traído de la base), sin volver a consultarla.
     * Con el texto vacío vuelve a mostrarlos todos.
     */
    public void filtrarPorTexto(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.getDefault());
        if (buscado.isEmpty()) {
            pacientesVisibles.setAll(todosLosPacientes);
            actualizarTotales(todosLosPacientes);
            return;
        }
        List<Paciente> filtrados = new ArrayList<>();
        for (Paciente p : todosLosPacientes) {
            if (contiene(p.getDni(), buscado) || contiene(p.getNyaPaciente(), buscado)
                    || contiene(p.getEmail(), buscado) || contiene(p.getNombreObraSocial(), buscado)) {
                filtrados.add(p);
            }
        }
        pacientesVisibles.setAll(filtrados);
        actualizarTotales(filtrados);
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.getDefault()).contains(buscado);
    }

    private static String vacio(String texto) {
        return texto == null || texto.trim().isEmpty() ? "—" : texto;
    }
}
