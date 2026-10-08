package vistas.javafx.escritorio;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import modelo.DetalleOrden;
import modelo.EstadisticasEscritorio;
import modelo.OrdenResumen;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla de inicio (dashboard), en JavaFX puro (sin FXML). Reemplaza a
 * {@link vistas.formulariosPrincipales.Escritorio}: 4 tarjetas de estadísticas del mes, la
 * alerta de resultados pendientes hace tiempo (Mes 6: Alertas) y la tabla de últimas órdenes,
 * con Ver (detalle de orden) y Editar (datos del paciente de esa orden).
 */
public class EscritorioScreen {

    private static final int CANTIDAD_ULTIMAS_ORDENES = 10;

    /**
     * A partir de cuántos días sin resultado cargado una orden pendiente o en proceso aparece en
     * la alerta de "Resultados pendientes hace tiempo".
     */
    private static final int DIAS_ALERTA_PENDIENTES = 3;

    private final Label valorTotalMes = new Label("0");
    private final Label valorEmitidas = new Label("0");
    private final Label valorEnProceso = new Label("0");
    private final Label valorPendientes = new Label("0");

    private final ObservableList<OrdenResumen> ordenes = FXCollections.observableArrayList();

    /**
     * Órdenes con resultado pendiente hace más de {@link #DIAS_ALERTA_PENDIENTES} días, mostradas
     * en la sección de alertas.
     */
    private final ObservableList<OrdenResumen> alertasPendientes = FXCollections.observableArrayList();
    private final Label lblAlertaTitulo = new Label();
    private final VBox seccionAlertas = new VBox(10);
    private final TableView<OrdenResumen> tablaAlertas = new TableView<>();

    /** Alto de fila fijado en ".table-view" (theme.css, -fx-fixed-cell-size) y alto de la fila de
     * encabezado fijado en ".column-header" (-fx-size) -- se usan acá para calcularle a esta
     * tablita un alto que se ajuste de verdad a la cantidad de filas que tiene, ver
     * {@link #ajustarAltoTablaAlertas()}. */
    private static final double ALTO_FILA_TABLA = 46;
    private static final double ALTO_CABECERA_TABLA = 43;
    private static final int MAX_FILAS_VISIBLES_ALERTAS = 4;

    /**
     * Todas las órdenes cargadas desde la base (sin filtrar) -- {@link #ordenes}, la lista que ve
     * la tabla, puede quedar filtrada por {@link #filtrarPorPaciente}; esta es la copia completa
     * para poder volver a filtrar sin consultar la base de nuevo.
     */
    private final List<OrdenResumen> todasLasOrdenes = new ArrayList<>();

    /**
     * A quién avisarle cuando, desde el detalle de una orden, se aprieta "Cargar Resultados"
     * -esta pantalla no navega por su cuenta, sólo reenvía el aviso a quien la contenga (ver
     * {@link vistas.formulariosPrincipales.Escritorio.CargarResultadosListener}).
     */
    private Runnable alCargarResultados;

    public void setAlCargarResultados(Runnable listener) {
        this.alCargarResultados = listener;
    }

    public Parent construir() {
        VBox raiz = new VBox(24);
        raiz.getStyleClass().add("pantalla");

        VBox seccionOrdenes = armarSeccionOrdenes();
        raiz.getChildren().addAll(armarTarjetas(), armarSeccionAlertas(), seccionOrdenes);
        VBox.setVgrow(seccionOrdenes, Priority.ALWAYS);

        cargarDatos();
        return raiz;
    }

    /**
     * Arma la caja de alerta "Resultados pendientes hace tiempo" -- arranca oculta (no ocupa
     * lugar) hasta que {@link #cargarDatos()} traiga datos reales y decida si hay algo para
     * mostrar.
     */
    private VBox armarSeccionAlertas() {
        lblAlertaTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #B9770E;");

        armarTablaAlertas();
        // Mes 8 (rediseño visual): desde que las tablas de toda la app pasaron a tener filas más
        // altas (ver ".table-view" en theme.css, -fx-fixed-cell-size), un alto fijo cualquiera acá
        // quedaba mal -- 160 se quedaba corto y la tabla se dibujaba por encima de su límite
        // (superpuesta con "Últimas Órdenes", el título de la sección de abajo); 190 alcanzaba
        // para las filas pero dejaba un colchón de espacio vacío abajo cuando había 1 o 2
        // pendientes nada más (que es el caso normal -- ver {@link #ajustarAltoTablaAlertas}, que
        // ahora la ajusta a la cantidad real de filas en vez de un número fijo).
        tablaAlertas.setMinHeight(Region.USE_PREF_SIZE);
        tablaAlertas.setMaxHeight(Region.USE_PREF_SIZE);
        ajustarAltoTablaAlertas();

        seccionAlertas.getChildren().setAll(lblAlertaTitulo, tablaAlertas);
        seccionAlertas.setPadding(new Insets(16));
        seccionAlertas.setStyle("-fx-background-color: #FDF6E9; -fx-background-radius: 12; "
                + "-fx-border-color: #F5DFA6; -fx-border-radius: 12; -fx-border-width: 1;");
        seccionAlertas.setVisible(false);
        seccionAlertas.setManaged(false);
        return seccionAlertas;
    }

    /**
     * Le da a {@link #tablaAlertas} el alto justo para la cantidad de filas que tiene en este
     * momento (cabecera + una fila por cada pendiente), en vez de un número fijo -- así no queda
     * ni recortada (como con 160) ni con un colchón de espacio vacío de más (como con 190 cuando
     * hay 1 o 2 nada más, el caso normal). Tope de {@link #MAX_FILAS_VISIBLES_ALERTAS} filas de
     * alto -- si algún día hay más pendientes que eso, la tabla no sigue creciendo y se ve el
     * resto haciendo scroll adentro, para no empujar el resto del Escritorio hacia abajo.
     */
    private void ajustarAltoTablaAlertas() {
        int filas = Math.max(1, Math.min(alertasPendientes.size(), MAX_FILAS_VISIBLES_ALERTAS));
        tablaAlertas.setPrefHeight(ALTO_CABECERA_TABLA + filas * ALTO_FILA_TABLA);
    }

    private void armarTablaAlertas() {
        TableView<OrdenResumen> tabla = tablaAlertas;

        TableColumn<OrdenResumen, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);

        TableColumn<OrdenResumen, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(200);

        TableColumn<OrdenResumen, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getExamen())));
        colExamen.setPrefWidth(200);

        TableColumn<OrdenResumen, String> colFecha = new TableColumn<>("Pedido el");
        SimpleDateFormat formatoFechaAlerta = new SimpleDateFormat("dd/MM/yyyy");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? formatoFechaAlerta.format(d.getValue().getFecha()) : ""));
        colFecha.setPrefWidth(100);

        TableColumn<OrdenResumen, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getEstado())));
        colEstado.setPrefWidth(110);

        tabla.getColumns().setAll(colOrden, colPaciente, colExamen, colFecha, colEstado);
        tabla.setItems(alertasPendientes);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private HBox armarTarjetas() {
        HBox fila = new HBox(16,
                construirTarjeta("Total del mes", valorTotalMes, "Análisis registrados", "azul", "📦"),
                construirTarjeta("Emitidas", valorEmitidas, "Con resultado cargado", "verde", "✅"),
                construirTarjeta("En proceso", valorEnProceso, "En laboratorio", "ambar", "⏳"),
                construirTarjeta("Pendientes", valorPendientes, "Sin iniciar", "rojo", "⚠️"));
        return fila;
    }

    /**
     * Mes 8 (rediseño visual, propuesta que aprobó la clienta): antes estas 4 tarjetas eran cajas
     * de color liso sin ningún ícono ni reacción al mouse (ver el git log de este método). Ahora
     * son tarjetas blancas con una franja de color a la izquierda (clase {@code .tarjeta-stat-
     * <clave>} en theme.css, {@code clave} es "azul"/"verde"/"ambar"/"rojo"), una insignia
     * circular de color arriba con un glifo adentro, y un efecto de "se levanta" al pasar el
     * mouse (ver {@link #animarHoverTarjeta}).
     *
     * <p><b>Prueba de emoji a color (📦 ✅ ⏳ ⚠️):</b> antes acá había símbolos de texto plano
     * ("●", "✓", "◐", "⚠") en vez de emoji con color, porque el motor gráfico de JavaFX 8
     * (Prism) históricamente tiene soporte inconsistente para fuentes de emoji a color y puede
     * llegar a mostrar un cuadrado vacío en su lugar. En vez de asumir que va a fallar, se prueba
     * acá con emoji de verdad -- si en la pantalla de la bioquímica se ven bien (iconitos a
     * color, como en cualquier página web), se puede extender el mismo criterio al resto de la
     * app (toasts, avisos, etc.); si aparece algún cuadrado vacío, se vuelve a los símbolos de
     * texto plano de antes (que sí están garantizados -- "✓" y "⚠" ya se usaban así en Cargar
     * Resultados).</p>
     */
    private VBox construirTarjeta(String titulo, Label valorLabel, String descripcion, String claveColor, String glifo) {
        Label lblGlifo = new Label(glifo);
        lblGlifo.getStyleClass().addAll("tarjeta-stat-icono-glifo", "tarjeta-stat-icono-glifo-" + claveColor);

        StackPane icono = new StackPane(lblGlifo);
        icono.getStyleClass().addAll("tarjeta-stat-icono", "tarjeta-stat-icono-" + claveColor);
        icono.setPrefSize(38, 38);
        icono.setMinSize(38, 38);
        icono.setMaxSize(38, 38);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("tarjeta-stat-etiqueta");

        valorLabel.getStyleClass().add("tarjeta-stat-valor");

        Label lblDescripcion = new Label(descripcion);
        lblDescripcion.getStyleClass().add("tarjeta-stat-sub");

        VBox tarjeta = new VBox(4, icono, valorLabel, lblTitulo, lblDescripcion);
        tarjeta.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(icono, new Insets(0, 0, 10, 0));
        tarjeta.getStyleClass().addAll("tarjeta-stat", "tarjeta-stat-" + claveColor);
        tarjeta.setPadding(new Insets(20));
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);

        animarHoverTarjeta(tarjeta);
        return tarjeta;
    }

    /**
     * Hace que {@code tarjeta} "se levante" unos pocos píxeles (con una animación suave, no de
     * golpe) al pasar el mouse por arriba, y vuelva a su lugar al salir -- mismo recurso que ya
     * usan los diálogos al abrirse (ver {@code vistas.javafx.VentanaUtil#animarAparicion}), acá
     * aplicado a un nodo que se queda fijo en la pantalla en vez de a una ventana que recién
     * aparece. La sombra más marcada mientras está "levantada" es instantánea (clase CSS
     * ".tarjeta-stat-hover", ver theme.css) -- JavaFX no anima el cambio de una sombra con
     * transición propia, así que la parte que sí se anima es el movimiento.
     */
    private void animarHoverTarjeta(Region tarjeta) {
        TranslateTransition subir = new TranslateTransition(Duration.millis(140), tarjeta);
        subir.setToY(-4);

        TranslateTransition bajar = new TranslateTransition(Duration.millis(140), tarjeta);
        bajar.setToY(0);

        tarjeta.setOnMouseEntered(evt -> {
            bajar.stop();
            subir.playFromStart();
            tarjeta.getStyleClass().add("tarjeta-stat-hover");
        });
        tarjeta.setOnMouseExited(evt -> {
            subir.stop();
            bajar.playFromStart();
            tarjeta.getStyleClass().remove("tarjeta-stat-hover");
        });
    }

    private VBox armarSeccionOrdenes() {
        Label titulo = new Label("Últimas Órdenes");
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 18px;");

        TableView<OrdenResumen> tabla = armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox seccion = new VBox(12, titulo, tabla);
        return seccion;
    }

    private TableView<OrdenResumen> armarTabla() {
        TableView<OrdenResumen> tabla = new TableView<>();

        TableColumn<OrdenResumen, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);

        TableColumn<OrdenResumen, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(220);

        TableColumn<OrdenResumen, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getExamen())));
        colExamen.setPrefWidth(220);

        TableColumn<OrdenResumen, String> colFecha = new TableColumn<>("Fecha");
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? formatoFecha.format(d.getValue().getFecha()) : ""));
        colFecha.setPrefWidth(110);

        TableColumn<OrdenResumen, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getEstado())));
        colEstado.setPrefWidth(130);

        TableColumn<OrdenResumen, OrdenResumen> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<OrdenResumen, OrdenResumen>() {
            private final Button botonVer = new Button("Ver");
            private final Button botonEditar = new Button("Editar");
            private final HBox contenedor = new HBox(10, botonVer, botonEditar);

            {
                botonVer.getStyleClass().add("boton-fila");
                botonEditar.getStyleClass().add("boton-fila");
            }

            @Override
            protected void updateItem(OrdenResumen orden, boolean vacio) {
                super.updateItem(orden, vacio);
                if (vacio || orden == null) {
                    setGraphic(null);
                    return;
                }
                botonVer.setOnAction(evt -> mostrarDetalle(orden));
                botonEditar.setOnAction(evt -> editarPacienteDeOrden(orden));
                setGraphic(contenedor);
            }
        });
        colAcciones.setPrefWidth(140);
        // No resizable: con CONSTRAINED_RESIZE_POLICY, si la ventana queda angosta la política
        // achica columnas por debajo de su propio mínimo para que todo encaje -- a esta columna
        // eso la deja con los botones amontonados o cortados. Marcarla no-resizable la saca del
        // reparto; las columnas de texto ceden espacio en su lugar (ver el mismo ajuste en
        // RegistrosScreen).
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colOrden, colPaciente, colExamen, colFecha, colEstado, colAcciones);
        tabla.setItems(ordenes);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Todavía no hay órdenes cargadas."));
        return tabla;
    }

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    private void cargarDatos() {
        EstadisticasEscritorio stats = PuenteEDT.ejecutar(
                () -> controlador.EscritorioController.obtenerEstadisticasDelMes(null),
                new EstadisticasEscritorio());
        valorTotalMes.setText(String.valueOf(stats.getTotalMes()));
        valorEmitidas.setText(String.valueOf(stats.getEmitidas()));
        valorEnProceso.setText(String.valueOf(stats.getEnProceso()));
        valorPendientes.setText(String.valueOf(stats.getPendientes()));

        List<OrdenResumen> lista = PuenteEDT.ejecutar(
                () -> controlador.EscritorioController.listarUltimasOrdenes(null, CANTIDAD_ULTIMAS_ORDENES),
                Collections.emptyList());
        todasLasOrdenes.clear();
        todasLasOrdenes.addAll(lista);
        ordenes.setAll(lista);

        List<OrdenResumen> pendientesHaceRato = utilidades.PreferenciasSistema.isRecordatorioPendientes()
                ? PuenteEDT.ejecutar(
                        () -> controlador.EscritorioController.listarResultadosPendientesHaceRato(null, DIAS_ALERTA_PENDIENTES),
                        Collections.emptyList())
                : Collections.emptyList();
        alertasPendientes.setAll(pendientesHaceRato);
        ajustarAltoTablaAlertas();

        // "Recordatorio de análisis pendientes al iniciar el día" (Configuración > Sistema): si
        // está apagado, ni se consulta la base -- se trata igual que "no hay nada pendiente".
        boolean hayAlertas = !pendientesHaceRato.isEmpty();
        seccionAlertas.setVisible(hayAlertas);
        seccionAlertas.setManaged(hayAlertas);
        lblAlertaTitulo.setText("⚠ " + pendientesHaceRato.size()
                + (pendientesHaceRato.size() == 1 ? " resultado pendiente" : " resultados pendientes")
                + " de carga hace más de " + DIAS_ALERTA_PENDIENTES + " días");
    }

    /**
     * Filtra la tabla de "Últimas Órdenes" por paciente -- la llama el buscador del header a cada
     * tecla, cuando esta pantalla está activa (ver {@code AppShell#navegar}, caso "4_1"). Filtra
     * sobre los datos ya traídos de la base ({@link #todasLasOrdenes}), sin volver a consultarla.
     * Con el texto vacío vuelve a mostrarlas todas.
     */
    public void filtrarPorPaciente(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        if (buscado.isEmpty()) {
            ordenes.setAll(todasLasOrdenes);
            return;
        }
        List<OrdenResumen> filtradas = new ArrayList<>();
        for (OrdenResumen orden : todasLasOrdenes) {
            String paciente = orden.getPaciente();
            if (paciente != null && paciente.toLowerCase().contains(buscado)) {
                filtradas.add(orden);
            }
        }
        ordenes.setAll(filtradas);
    }

    private void mostrarDetalle(OrdenResumen orden) {
        DetalleOrden detalle = PuenteEDT.ejecutar(
                () -> controlador.EscritorioController.buscarDetalleOrden(null, orden.getIdPedidoAnalisis()),
                null);
        if (detalle == null) {
            return;
        }
        new DetalleOrdenDialog(detalle, alCargarResultados).mostrar();
    }

    /**
     * Busca al paciente dueño de la orden (por DNI) y abre "Editar Paciente" -- ahora la pantalla
     * JavaFX ({@code vistas.javafx.pacientes.PacienteFormDialog}), no la vieja Swing (Pacientes ya
     * está migrado, ver {@link vistas.javafx.pacientes.PacientesScreen}).
     */
    private void editarPacienteDeOrden(OrdenResumen orden) {
        modelo.Paciente paciente = PuenteEDT.ejecutar(
                () -> controlador.PacienteController.buscarPorDni(null, orden.getDni()), null);
        if (paciente == null) {
            return;
        }

        new vistas.javafx.pacientes.PacienteFormDialog(
                vistas.javafx.pacientes.PacienteFormDialog.Modo.EDITAR, paciente,
                // Si cambió el nombre del paciente, que se vea reflejado sin tener que salir y
                // volver a entrar a Escritorio.
                this::cargarDatos).mostrar();
    }
}
