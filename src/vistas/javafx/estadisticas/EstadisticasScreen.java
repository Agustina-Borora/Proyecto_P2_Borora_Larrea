package vistas.javafx.estadisticas;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.ConteoExamen;
import modelo.EstadisticasPeriodo;
import modelo.PacienteFrecuente;
import modelo.PuntoMensual;
import modelo.ResumenObraSocial;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla "Estadísticas" (JavaFX puro, sin FXML): reemplaza al stub vacío que dejó el editor de
 * formularios de NetBeans en {@link vistas.formulariosPrincipales.Estadisticas}. Muestra un
 * resumen del período elegido (este mes / trimestre / este año): 4 tarjetas, un gráfico de
 * barras "Análisis por mes" (Particular vs Obra Social), un gráfico de torta "Exámenes más
 * solicitados", y dos tablas (órdenes por obra social y pacientes frecuentes).
 *
 * <p>Todo sale de {@code dao.EstadisticasDAO#obtener}, que arma un solo {@link
 * EstadisticasPeriodo} por consulta -- ver el javadoc de ese método para el detalle de cada
 * consulta SQL. El gráfico de barras siempre muestra los últimos 6 meses (no cambia con el
 * período elegido, ver el DAO); las tarjetas y las dos tablas sí cambian según el período.</p>
 */
public class EstadisticasScreen {

    private static final NumberFormat FORMATO_MONEDA = crearFormatoMoneda();
    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private static NumberFormat crearFormatoMoneda() {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        return formato;
    }

    private static String moneda(BigDecimal valor) {
        return "$" + FORMATO_MONEDA.format(valor != null ? valor : BigDecimal.ZERO);
    }

    // Mes 8 (rediseño visual), tercera vuelta: colores a mano para cada porción de la torta de
    // "Exámenes más solicitados" -- ver #armarGraficoExamenes y #actualizarDesgloseExamenes para
    // el porqué (la leyenda nativa de JavaFX dejaba de mostrar el texto). El color de la porción
    // N usa PALETA_TORTA[N % PALETA_TORTA.length] (ver ".torta-examenes .dataN.chart-pie" en
    // theme.css) y el cuadradito de la lista de abajo usa ese mismo color, así que coinciden.
    private static final String[] PALETA_TORTA = {
        "#2563EB", "#237A4E", "#B9770E", "#C2412F", "#7C3AED", "#0D9488"
    };

    private String periodoActual = "mes";

    private final Label valorAnalisis = new Label("0");
    private final Label valorPacientes = new Label("0");
    private final Label valorIngreso = new Label("$0");
    private final Label valorPorCobrar = new Label("$0");

    private Button botonMes;
    private Button botonTrimestre;
    private Button botonAnio;

    private final XYChart.Series<String, Number> seriePorMesParticular = new XYChart.Series<>();
    private final XYChart.Series<String, Number> seriePorMesObraSocial = new XYChart.Series<>();
    private final HBox etiquetasPorMes = new HBox();

    private final ObservableList<PieChart.Data> datosTorta = FXCollections.observableArrayList();
    private final VBox desgloseExamenes = new VBox(8);

    private final ObservableList<ResumenObraSocial> ordenesPorObraSocial = FXCollections.observableArrayList();
    private final ObservableList<PacienteFrecuente> pacientesFrecuentes = FXCollections.observableArrayList();

    public Parent construir() {
        VBox raiz = new VBox(20);
        raiz.getStyleClass().add("pantalla");

        BarChart<String, Number> graficoPorMes = armarGraficoPorMes();
        VBox contenidoPorMes = new VBox(6, graficoPorMes, armarEtiquetasPorMes(), armarLeyendaPorMes());

        PieChart graficoExamenes = armarGraficoExamenes();
        StackPane graficoExamenesCentrado = new StackPane(graficoExamenes);
        graficoExamenesCentrado.setAlignment(Pos.CENTER);
        VBox contenidoExamenes = new VBox(10, graficoExamenesCentrado, desgloseExamenes);

        // Mes 8 (rediseño visual): la clienta avisó "no se entiende" -- ninguno de los dos
        // gráficos explicaba qué estaba calculando. Se le agregó una bajada corta a cada tarjeta
        // más abajo, y una referencia armada a mano debajo de cada gráfico (ver
        // #armarLeyendaPorMes y #actualizarDesgloseExamenes) en vez de la leyenda nativa de
        // JavaFX, que dejaba de mostrar el texto cuando el gráfico tenía poco alto.
        HBox graficos = new HBox(20,
                envolverEnTarjeta("Análisis por mes",
                        "Cantidad de análisis registrados en los últimos 6 meses, particulares vs. obra social",
                        contenidoPorMes),
                envolverEnTarjeta("Exámenes más solicitados",
                        "Cuántas veces se pidió cada examen en el período elegido arriba",
                        contenidoExamenes));

        HBox tablas = new HBox(20, armarSeccionObrasSociales(), armarSeccionPacientesFrecuentes());

        raiz.getChildren().addAll(armarEncabezado(), armarTarjetasResumen(), graficos, tablas);

        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        Label titulo = new Label("Estadísticas");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Resumen de actividad del laboratorio");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(4, titulo, subtitulo);

        botonMes = crearBotonPeriodo("Este mes", "mes");
        botonTrimestre = crearBotonPeriodo("Trimestre", "trimestre");
        botonAnio = crearBotonPeriodo("Este año", "anio");
        HBox selectorPeriodo = new HBox(8, botonMes, botonTrimestre, botonAnio);

        // Registro evolutivo de un paciente (visitas por mes, por edad, resultados en el tiempo).
        Button botonEvolucion = new Button("📈 Evolución de un paciente");
        botonEvolucion.getStyleClass().add("boton-primario");
        botonEvolucion.setOnAction(e -> new EvolucionPacienteDialog().mostrar(
                botonEvolucion.getScene() == null ? null : botonEvolucion.getScene().getWindow(), null));

        HBox fila = new HBox(16, textos, crearEspaciador(), botonEvolucion, selectorPeriodo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private Region crearEspaciador() {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        return espaciador;
    }

    private Button crearBotonPeriodo(String texto, String periodo) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(periodo.equals(periodoActual) ? "boton-primario" : "boton-secundario");
        boton.setOnAction(evt -> cambiarPeriodo(periodo));
        return boton;
    }

    private void cambiarPeriodo(String periodo) {
        if (periodo.equals(periodoActual)) {
            return;
        }
        periodoActual = periodo;
        actualizarEstiloBotonesPeriodo();
        cargarDatos();
    }

    private void actualizarEstiloBotonesPeriodo() {
        for (Button boton : new Button[]{botonMes, botonTrimestre, botonAnio}) {
            boton.getStyleClass().removeAll("boton-primario", "boton-secundario");
        }
        botonMes.getStyleClass().add("mes".equals(periodoActual) ? "boton-primario" : "boton-secundario");
        botonTrimestre.getStyleClass().add("trimestre".equals(periodoActual) ? "boton-primario" : "boton-secundario");
        botonAnio.getStyleClass().add("anio".equals(periodoActual) ? "boton-primario" : "boton-secundario");
    }

    /**
     * Mes 8 (rediseño visual): mismo lenguaje visual que ya aprobó la clienta para el Escritorio
     * (ícono + franja de color + efecto hover) en vez de las cajas de color liso de antes -- ver
     * {@link vistas.javafx.TarjetaStatUtil}.
     */
    private HBox armarTarjetasResumen() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir(
                        "Análisis Realizados", valorAnalisis, "En el período elegido", "azul", "🧪"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Pacientes Únicos", valorPacientes, "Atendidos en el período", "verde", "👥"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Ingreso Total", valorIngreso, "Facturado en el período", "ambar", "💰"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Por Cobrar a OS", valorPorCobrar, "Adeudado por obras sociales", "rojo", "⏳"));
    }

    private BarChart<String, Number> armarGraficoPorMes() {
        CategoryAxis ejeX = new CategoryAxis();
        // Mes 8 (rediseño visual), cuarta vuelta: el eje X nativo (los nombres de mes debajo de
        // cada par de barras, "May", "Jun", etc.) dejaba de mostrarse por el mismo motivo que ya
        // había pasado con la leyenda -- JavaFX no le garantiza espacio a las etiquetas de un eje
        // cuando el gráfico está acotado en alto (acá sigue acotado, ver más abajo, para que no
        // vuelva a "globearse" como pasó antes de tener un tope). Sin esos nombres, la clienta no
        // podía saber qué mes era cada par de barras ("queda ambiguo"). Mismo criterio que ya se
        // usó para la leyenda: se apaga el texto nativo del eje y se arma a mano, con los mismos
        // nombres de mes, en #armarEtiquetasPorMes/#actualizarEtiquetasPorMes -- así no depende de
        // cuánto espacio le calcule JavaFX al eje.
        ejeX.setTickLabelsVisible(false);
        ejeX.setTickMarkVisible(false);
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Cantidad de análisis");

        BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setAnimated(false);
        grafico.getStyleClass().add("analisis-por-mes");
        seriePorMesParticular.setName("Particular");
        seriePorMesObraSocial.setName("Obra Social");
        grafico.getData().setAll(seriePorMesParticular, seriePorMesObraSocial);
        // Mes 8 (rediseño visual), tercera vuelta: la leyenda nativa de abajo (los cuadraditos de
        // color de "Particular"/"Obra Social") dejó de mostrar el texto apenas el gráfico tuvo
        // poco alto disponible -- JavaFX no le garantiza suficiente espacio a la leyenda cuando el
        // gráfico está acotado. Se apaga acá y se arma a mano, con colores fijos, en
        // #armarLeyendaPorMes (ver también ".analisis-por-mes .default-colorN.chart-bar" en
        // theme.css, que son esos mismos colores aplicados a las barras).
        grafico.setLegendVisible(false);
        grafico.setMinHeight(260);
        grafico.setMaxHeight(300);
        return grafico;
    }

    /**
     * Mes 8 (rediseño visual), cuarta vuelta: fila con los 6 nombres de mes ("May", "Jun", ...),
     * uno debajo de cada par de barras -- reemplaza a las etiquetas nativas del eje X de
     * {@link #armarGraficoPorMes()} (ver el comentario ahí). Se arma vacía acá (antes de tener
     * datos) y se completa en {@link #actualizarEtiquetasPorMes}; como las 6 categorías del
     * gráfico tienen todas el mismo ancho, repartir el mismo espacio en partes iguales (6 Label
     * con grow parejo) alinea cada nombre debajo de su par de barras sin tener que calcularle el
     * ancho exacto a cada una.
     */
    private HBox armarEtiquetasPorMes() {
        etiquetasPorMes.setAlignment(Pos.CENTER);
        return etiquetasPorMes;
    }

    private void actualizarEtiquetasPorMes(List<PuntoMensual> porMes) {
        etiquetasPorMes.getChildren().clear();
        for (PuntoMensual punto : porMes) {
            Label etiqueta = new Label(punto.getMes());
            etiqueta.getStyleClass().add("leyenda-grafico-texto");
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            etiqueta.setAlignment(Pos.CENTER);
            HBox.setHgrow(etiqueta, Priority.ALWAYS);
            etiquetasPorMes.getChildren().add(etiqueta);
        }
    }

    /**
     * Referencia de colores de "Análisis por mes", armada a mano -- reemplaza a la leyenda nativa
     * de {@link BarChart} (ver el comentario en {@link #armarGraficoPorMes()}).
     */
    private HBox armarLeyendaPorMes() {
        HBox leyenda = new HBox(20,
                crearItemLeyenda("#2563EB", "Particular"),
                crearItemLeyenda("#B9770E", "Obra Social"));
        leyenda.setAlignment(Pos.CENTER);
        return leyenda;
    }

    private HBox crearItemLeyenda(String colorHex, String texto) {
        Region cuadrito = new Region();
        cuadrito.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 3;");
        cuadrito.setMinSize(10, 10);
        cuadrito.setMaxSize(10, 10);

        Label lbl = new Label(texto);
        lbl.getStyleClass().add("leyenda-grafico-texto");

        HBox item = new HBox(6, cuadrito, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private PieChart armarGraficoExamenes() {
        PieChart grafico = new PieChart(datosTorta);
        grafico.setAnimated(false);
        grafico.getStyleClass().add("torta-examenes");
        // Mes 8 (rediseño visual), tercera vuelta: tanto las etiquetas nativas sobre la torta como
        // su leyenda de abajo dejaban de mostrar texto -- JavaFX le da prioridad al círculo y a
        // esas dos cosas les puede tocar un espacio tan chico que el texto no entra para nada (se
        // veía sólo el cuadradito de color de la leyenda, sin nombre ni cantidad). En vez de
        // seguir ajustando el alto a ciegas, se apagan las dos (dependen de ese reparto de
        // espacio) y se arma la referencia a mano en #actualizarDesgloseExamenes, con texto que no
        // depende de nada de eso -- así queda garantizado que siempre se vea completo, con nombre,
        // cantidad Y porcentaje (más información que la que tenía antes, no menos).
        grafico.setLabelsVisible(false);
        grafico.setLegendVisible(false);
        // Mismo motivo que ".analisis-por-mes" de al lado: con un límite (y no sólo un mínimo), el
        // círculo queda de un tamaño prolijo sea cual sea la ventana, en vez de un círculo gigante
        // ("no me gusta cómo se ve"). Al no tener que repartir espacio con ninguna leyenda nativa,
        // ahora puede ser bastante más chico sin perder nada de información (esa información vive
        // en #desglose, no en el gráfico).
        grafico.setMinSize(180, 180);
        grafico.setMaxSize(200, 200);
        return grafico;
    }

    /**
     * Lista armada a mano con nombre, cantidad y porcentaje de cada examen -- reemplaza a la
     * leyenda nativa de la torta (ver el comentario en {@link #armarGraficoExamenes()}) para
     * garantizar que el texto siempre se vea completo. El color de cada cuadradito usa la misma
     * paleta, por índice, que los colores reales de cada porción (ver {@link #PALETA_TORTA} y
     * ".torta-examenes .dataN.chart-pie" en theme.css), así que coinciden.
     */
    private void actualizarDesgloseExamenes(List<ConteoExamen> examenes) {
        desgloseExamenes.getChildren().clear();

        if (examenes.isEmpty()) {
            Label vacio = new Label("Todavía no hay análisis registrados en este período.");
            vacio.getStyleClass().add("descripcion-seccion");
            desgloseExamenes.getChildren().add(vacio);
            return;
        }

        int total = 0;
        for (ConteoExamen examen : examenes) {
            total += examen.getCantidad();
        }

        for (int i = 0; i < examenes.size(); i++) {
            ConteoExamen examen = examenes.get(i);
            String color = PALETA_TORTA[i % PALETA_TORTA.length];
            int porcentaje = total == 0 ? 0 : Math.round(examen.getCantidad() * 100f / total);
            String vecesTexto = examen.getCantidad() == 1 ? "vez" : "veces";

            HBox fila = crearItemLeyenda(color,
                    examen.getNombreExamen() + " — " + examen.getCantidad() + " " + vecesTexto
                            + " (" + porcentaje + "%)");
            desgloseExamenes.getChildren().add(fila);
        }
    }

    private VBox envolverEnTarjeta(String titulo, Node contenido) {
        return envolverEnTarjeta(titulo, null, contenido);
    }

    private VBox envolverEnTarjeta(String titulo, String descripcion, Node contenido) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-seccion");
        VBox tarjeta;
        if (descripcion == null) {
            tarjeta = new VBox(12, lblTitulo, contenido);
        } else {
            Label lblDescripcion = new Label(descripcion);
            lblDescripcion.getStyleClass().add("descripcion-seccion");
            lblDescripcion.setWrapText(true);
            VBox encabezado = new VBox(2, lblTitulo, lblDescripcion);
            tarjeta = new VBox(12, encabezado, contenido);
        }
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private VBox armarSeccionObrasSociales() {
        Label titulo = new Label("Órdenes por Obra Social");
        titulo.getStyleClass().add("titulo-seccion");

        TableView<ResumenObraSocial> tabla = armarTablaObrasSociales();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox tarjeta = new VBox(12, titulo, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private TableView<ResumenObraSocial> armarTablaObrasSociales() {
        TableView<ResumenObraSocial> tabla = new TableView<>();

        TableColumn<ResumenObraSocial, String> colNombre = new TableColumn<>("Obra Social");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNombreObraSocial())));
        colNombre.setPrefWidth(180);

        TableColumn<ResumenObraSocial, String> colOrdenes = new TableColumn<>("Órdenes");
        colOrdenes.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCantidadOrdenes())));
        colOrdenes.setPrefWidth(80);

        TableColumn<ResumenObraSocial, String> colUltimaOrden = new TableColumn<>("Última Orden");
        colUltimaOrden.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUltimaOrden() != null ? FORMATO_FECHA.format(d.getValue().getUltimaOrden()) : "-"));
        colUltimaOrden.setPrefWidth(110);

        TableColumn<ResumenObraSocial, String> colTotalAdeudado = new TableColumn<>("Adeudado");
        colTotalAdeudado.setCellValueFactory(d -> new SimpleStringProperty(moneda(d.getValue().getTotalAdeudado())));
        colTotalAdeudado.setPrefWidth(110);

        tabla.getColumns().setAll(colNombre, colOrdenes, colUltimaOrden, colTotalAdeudado);
        tabla.setItems(ordenesPorObraSocial);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay órdenes de obras sociales en el período elegido."));
        return tabla;
    }

    private VBox armarSeccionPacientesFrecuentes() {
        Label titulo = new Label("Pacientes Frecuentes");
        titulo.getStyleClass().add("titulo-seccion");

        TableView<PacienteFrecuente> tabla = armarTablaPacientesFrecuentes();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label ayuda = new Label("Doble clic en un paciente para ver su evolución.");
        ayuda.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");
        VBox tarjeta = new VBox(12, titulo, ayuda, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private TableView<PacienteFrecuente> armarTablaPacientesFrecuentes() {
        TableView<PacienteFrecuente> tabla = new TableView<>();

        TableColumn<PacienteFrecuente, String> colNombre = new TableColumn<>("Paciente");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNombrePaciente())));
        colNombre.setPrefWidth(180);

        TableColumn<PacienteFrecuente, String> colVisitas = new TableColumn<>("Visitas");
        colVisitas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getVisitas())));
        colVisitas.setPrefWidth(70);

        TableColumn<PacienteFrecuente, String> colUltimoEstudio = new TableColumn<>("Último Estudio");
        colUltimoEstudio.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getUltimoEstudio())));
        colUltimoEstudio.setPrefWidth(150);

        TableColumn<PacienteFrecuente, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaUltimoEstudio() != null ? FORMATO_FECHA.format(d.getValue().getFechaUltimoEstudio()) : "-"));
        colFecha.setPrefWidth(100);

        tabla.getColumns().setAll(colNombre, colVisitas, colUltimoEstudio, colFecha);
        tabla.setItems(pacientesFrecuentes);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay pacientes con más de una visita en el período elegido."));
        // Doble clic en un paciente: abre su evolución.
        tabla.setRowFactory(t -> {
            javafx.scene.control.TableRow<PacienteFrecuente> fila = new javafx.scene.control.TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    new EvolucionPacienteDialog().mostrar(tabla.getScene().getWindow(), fila.getItem().getNombrePaciente());
                }
            });
            return fila;
        });
        return tabla;
    }

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    /**
     * (Re)carga todos los datos de la pantalla para el {@link #periodoActual} -- se llama al
     * construir la pantalla y cada vez que se toca un botón de período.
     */
    private void cargarDatos() {
        EstadisticasPeriodo datos = PuenteEDT.ejecutar(
                () -> controlador.EstadisticasController.obtener(null, periodoActual),
                new EstadisticasPeriodo());

        valorAnalisis.setText(String.valueOf(datos.getAnalisisRealizados()));
        valorPacientes.setText(String.valueOf(datos.getPacientesUnicos()));
        valorIngreso.setText(moneda(datos.getIngresoTotal()));
        valorPorCobrar.setText(moneda(datos.getPorCobrarOs()));

        List<PuntoMensual> porMes = datos.getPorMes() != null ? datos.getPorMes() : Collections.emptyList();
        ObservableList<XYChart.Data<String, Number>> datosParticular = FXCollections.observableArrayList();
        ObservableList<XYChart.Data<String, Number>> datosObraSocial = FXCollections.observableArrayList();
        for (PuntoMensual punto : porMes) {
            datosParticular.add(new XYChart.Data<String, Number>(punto.getMes(), punto.getParticular()));
            datosObraSocial.add(new XYChart.Data<String, Number>(punto.getMes(), punto.getObraSocial()));
        }
        seriePorMesParticular.getData().setAll(datosParticular);
        seriePorMesObraSocial.getData().setAll(datosObraSocial);
        actualizarEtiquetasPorMes(porMes);

        List<ConteoExamen> examenes = datos.getExamenesMasSolicitados() != null
                ? datos.getExamenesMasSolicitados() : Collections.emptyList();
        ObservableList<PieChart.Data> torta = FXCollections.observableArrayList();
        for (ConteoExamen examen : examenes) {
            torta.add(new PieChart.Data(examen.getNombreExamen(), examen.getCantidad()));
        }
        datosTorta.setAll(torta);
        // Nombre, cantidad Y porcentaje de cada examen -- ver #actualizarDesgloseExamenes.
        actualizarDesgloseExamenes(examenes);

        List<ResumenObraSocial> os = datos.getOrdenesPorObraSocial() != null
                ? datos.getOrdenesPorObraSocial() : Collections.emptyList();
        ordenesPorObraSocial.setAll(os);

        List<PacienteFrecuente> frecuentes = datos.getPacientesFrecuentes() != null
                ? datos.getPacientesFrecuentes() : Collections.emptyList();
        pacientesFrecuentes.setAll(frecuentes);
    }
}
