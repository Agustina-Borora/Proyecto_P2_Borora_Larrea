package vistas.javafx.estadisticas;

import dao.EvolucionPacienteDAO.PacienteEncontrado;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
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
import javafx.stage.Window;
import javafx.util.Duration;
import modelo.EvolucionPaciente;
import vistas.javafx.EstiloApp;
import vistas.javafx.TareaFondo;
import vistas.javafx.ToastUtil;
import vistas.javafx.VentanaUtil;

/**
 * "Evolución de un paciente" (Estadísticas): se busca al paciente por nombre o DNI y se ve su
 * registro evolutivo -- cuántas veces vino, cuántos meses seguidos, qué edad tenía en cada visita,
 * y cómo fueron cambiando sus resultados (con gráfico). Se puede guardar en PDF o imprimir, por
 * ejemplo si el paciente o su médico lo piden.
 */
public final class EvolucionPacienteDialog {

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat formatoCorto = new SimpleDateFormat("dd/MM/yy");

    private final VBox detalle = new VBox(18);
    private final ProgressIndicator cargando = new ProgressIndicator();
    private EvolucionPaciente actual;
    private Button botonPdf;
    private Button botonImprimir;

    /** Abre la ventana; si {@code buscar} no es null, ya arranca buscando ese nombre. */
    public void mostrar(Window duena, String buscar) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        if (duena != null) {
            ventana.initOwner(duena);
        }
        ventana.setTitle("Evolución de un paciente");

        Label titulo = new Label("Evolución de un paciente");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Buscá al paciente por nombre o DNI y elegilo de la lista.");
        subtitulo.getStyleClass().add("subtitulo-pantalla");

        TextField campoBuscar = new TextField(buscar == null ? "" : buscar);
        campoBuscar.setPromptText("Nombre o DNI del paciente...");
        campoBuscar.getStyleClass().add("campo-form");
        campoBuscar.setPrefWidth(320);
        ListView<PacienteEncontrado> resultados = new ListView<>();
        resultados.setPrefHeight(120);
        resultados.setPlaceholder(new Label("No se encontraron pacientes."));

        botonPdf = new Button("Guardar PDF");
        botonPdf.getStyleClass().add("boton-primario");
        botonPdf.setDisable(true);
        botonPdf.setOnAction(e -> guardarPdf());
        botonImprimir = new Button("Imprimir");
        botonImprimir.getStyleClass().add("boton-secundario");
        botonImprimir.setDisable(true);
        botonImprimir.setOnAction(e -> imprimir());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox filaBusqueda = new HBox(10, campoBuscar, espacio, botonImprimir, botonPdf);
        filaBusqueda.setAlignment(Pos.CENTER_LEFT);

        PauseTransition demora = new PauseTransition(Duration.millis(300));
        demora.setOnFinished(e -> buscar(campoBuscar.getText(), resultados));
        campoBuscar.textProperty().addListener((o, a, n) -> demora.playFromStart());
        resultados.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                cargar(n.idPaciente);
            }
        });

        cargando.setMaxSize(46, 46);
        cargando.setVisible(false);
        Label inicial = new Label("Elegí un paciente de la lista para ver su evolución.");
        inicial.getStyleClass().add("subtitulo-pantalla");
        detalle.getChildren().add(inicial);
        StackPane zonaDetalle = new StackPane(detalle, cargando);
        StackPane.setAlignment(detalle, Pos.TOP_LEFT);

        VBox contenido = new VBox(14, titulo, subtitulo, filaBusqueda, resultados, zonaDetalle);
        contenido.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("config-scroll");
        VBox raiz = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        raiz.getStyleClass().add("pantalla");

        Scene escena = VentanaUtil.escenaAdaptable(ventana, raiz, 1080, 760);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        buscar(campoBuscar.getText(), resultados);
        ventana.showAndWait();
    }

    private void buscar(String texto, ListView<PacienteEncontrado> lista) {
        TareaFondo.ejecutar(() -> reportes.InformesPdfService.buscarPacientes(texto), encontrados -> {
            lista.getItems().setAll(encontrados);
            if (encontrados.size() == 1) {
                lista.getSelectionModel().select(0);
            }
        }, error -> ToastUtil.error("No se pudo buscar: " + TareaFondo.mensaje(error)));
    }

    private void cargar(int idPaciente) {
        cargando.setVisible(true);
        detalle.setOpacity(0.4);
        TareaFondo.ejecutar(() -> reportes.InformesPdfService.cargarEvolucion(idPaciente), ev -> {
            cargando.setVisible(false);
            detalle.setOpacity(1);
            actual = ev;
            botonPdf.setDisable(false);
            botonImprimir.setDisable(false);
            mostrarEvolucion(ev);
        }, error -> {
            cargando.setVisible(false);
            detalle.setOpacity(1);
            ToastUtil.error("No se pudo cargar la evolución: " + TareaFondo.mensaje(error));
        });
    }

    // ------------------------------------------------------------------ armado

    private void mostrarEvolucion(EvolucionPaciente ev) {
        detalle.getChildren().clear();

        Label nombre = new Label(ev.getNombre());
        nombre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        String nac = ev.getFechaNacimiento() == null ? "Sin fecha de nacimiento cargada"
                : "Nació el " + formatoFecha.format(ev.getFechaNacimiento()) + "  ·  " + EvolucionPaciente.textoEdad(ev.edadActual());
        Label datos = new Label("DNI " + (ev.getDni() == null ? "-" : ev.getDni()) + "  ·  " + nac
                + (ev.getSexo() == null ? "" : "  ·  " + ev.getSexo()));
        datos.getStyleClass().add("subtitulo-pantalla");
        detalle.getChildren().add(new VBox(4, nombre, datos));

        if (ev.getVisitas().isEmpty()) {
            detalle.getChildren().add(new Label("Este paciente todavía no tiene órdenes."));
            return;
        }

        HBox tarjetas = new HBox(14,
                tarjetaNumero(String.valueOf(ev.getVisitas().size()), "visitas en total"),
                tarjetaNumero(String.valueOf(ev.rachaMayorDeMeses()), "meses seguidos (racha mayor)"),
                tarjetaNumero(String.valueOf(ev.rachaActualDeMeses()), "meses seguidos hasta la última visita"),
                tarjetaNumero(formatoCorto.format(ev.primeraVisita()) + " → " + formatoCorto.format(ev.ultimaVisita()),
                        "primera y última visita"));
        detalle.getChildren().add(tarjetas);

        detalle.getChildren().add(tarjeta("Visitas por mes",
                "Cada barra es un mes desde su primera visita; donde no hay barra, ese mes no vino.", graficoMeses(ev)));

        HBox tablas = new HBox(18, tarjeta("Por edad", "Cuántas veces vino con cada edad y qué estudios se hizo.",
                tablaPorEdad(ev)), tarjeta("Detalle de las visitas", null, tablaVisitas(ev)));
        for (Node n : tablas.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        detalle.getChildren().add(tablas);

        detalle.getChildren().add(seccionResultados(ev));
    }

    /** Ejes con letra visible (el tema de la app no les pone color propio). */
    private static void estilizarEjes(javafx.scene.chart.Axis<?> x, javafx.scene.chart.Axis<?> y) {
        for (javafx.scene.chart.Axis<?> eje : new javafx.scene.chart.Axis<?>[]{x, y}) {
            eje.setTickLabelFill(javafx.scene.paint.Color.web("#5F6966"));
            eje.setTickLabelFont(javafx.scene.text.Font.font("SansSerif", 11));
            eje.setStyle("-fx-font-size: 11px; -fx-text-fill: #5F6966;");
        }
    }

    private Node graficoMeses(EvolucionPaciente ev) {
        CategoryAxis ejeX = new CategoryAxis();
        NumberAxis ejeY = new NumberAxis();
        estilizarEjes(ejeX, ejeY);
        ejeY.setTickUnit(1);
        ejeY.setMinorTickVisible(false);
        ejeY.setForceZeroInRange(true);
        BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        grafico.setPrefHeight(220);
        grafico.setStyle("CHART_COLOR_1: #1E5C3D;");
        Map<String, Integer> meses = ev.visitasPorMes();
        // Con pocos meses, barras más finas (si no, quedan dos bloques enormes).
        grafico.setCategoryGap(meses.size() <= 2 ? 300 : meses.size() <= 6 ? 90 : meses.size() <= 14 ? 24 : 6);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<String, Integer> e : meses.entrySet()) {
            serie.getData().add(new XYChart.Data<String, Number>(EvolucionPaciente.nombreMes(e.getKey()), e.getValue()));
        }
        grafico.getData().add(serie);
        return grafico;
    }

    private Node tablaPorEdad(EvolucionPaciente ev) {
        TableView<EvolucionPaciente.PorEdad> tabla = new TableView<>();
        TableColumn<EvolucionPaciente.PorEdad, String> colEdad = new TableColumn<>("Edad");
        colEdad.setCellValueFactory(d -> new SimpleStringProperty(EvolucionPaciente.textoEdad(d.getValue().edad)));
        TableColumn<EvolucionPaciente.PorEdad, String> colVisitas = new TableColumn<>("Visitas");
        colVisitas.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().visitas)));
        TableColumn<EvolucionPaciente.PorEdad, String> colEstudios = new TableColumn<>("Estudios");
        colEstudios.setCellValueFactory(d -> new SimpleStringProperty(String.join(", ", d.getValue().estudios)));
        colEdad.setPrefWidth(70);
        colVisitas.setPrefWidth(60);
        colEstudios.setPrefWidth(420);
        colEstudios.setCellFactory(c -> celdaConTooltip());
        tabla.getColumns().setAll(colEdad, colVisitas, colEstudios);
        tabla.getItems().setAll(ev.porEdad());
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(230);
        return tabla;
    }

    private Node tablaVisitas(EvolucionPaciente ev) {
        TableView<EvolucionPaciente.Visita> tabla = new TableView<>();
        TableColumn<EvolucionPaciente.Visita, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(formatoFecha.format(d.getValue().fecha)));
        TableColumn<EvolucionPaciente.Visita, String> colEdad = new TableColumn<>("Edad");
        colEdad.setCellValueFactory(d -> new SimpleStringProperty(EvolucionPaciente.textoEdad(d.getValue().edad)));
        TableColumn<EvolucionPaciente.Visita, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().numeroOrden));
        TableColumn<EvolucionPaciente.Visita, String> colEstudios = new TableColumn<>("Estudios");
        colEstudios.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estudios));
        colFecha.setPrefWidth(85);
        colEdad.setPrefWidth(65);
        colOrden.setPrefWidth(95);
        colEstudios.setPrefWidth(300);
        colEstudios.setCellFactory(c -> celdaConTooltip());
        tabla.getColumns().setAll(colFecha, colEdad, colOrden, colEstudios);
        // De la más nueva a la más vieja en pantalla.
        List<EvolucionPaciente.Visita> visitas = new java.util.ArrayList<>(ev.getVisitas());
        java.util.Collections.reverse(visitas);
        tabla.getItems().setAll(visitas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(230);
        return tabla;
    }

    private Node seccionResultados(EvolucionPaciente ev) {
        List<EvolucionPaciente.Serie> series = ev.series();
        if (series.isEmpty()) {
            return tarjeta("Evolución de los resultados", null, new Label("Todavía no hay resultados cargados."));
        }
        ComboBox<EvolucionPaciente.Serie> combo = new ComboBox<>();
        combo.getItems().setAll(series);
        combo.setPrefWidth(420);
        VBox zona = new VBox(10);
        combo.valueProperty().addListener((o, a, s) -> zona.getChildren().setAll(vistaSerie(s)));
        // Arranca con el que más veces se midió.
        EvolucionPaciente.Serie mejor = series.get(0);
        for (EvolucionPaciente.Serie s : series) {
            if (s.numericas().size() > mejor.numericas().size()) {
                mejor = s;
            }
        }
        combo.setValue(mejor);
        Label lblParametro = new Label("Parámetro:");
        lblParametro.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
        HBox fila = new HBox(10, lblParametro, combo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return tarjeta("Evolución de los resultados",
                "Elegí un parámetro para ver cómo fue cambiando. En rojo, los valores fuera de la referencia actual.",
                new VBox(12, fila, zona));
    }

    private List<Node> vistaSerie(EvolucionPaciente.Serie s) {
        List<Node> nodos = new java.util.ArrayList<>();
        if (s == null) {
            return nodos;
        }
        if (s.graficable()) {
            CategoryAxis ejeX = new CategoryAxis();
            NumberAxis ejeY = new NumberAxis();
            ejeY.setForceZeroInRange(false);
            estilizarEjes(ejeX, ejeY);
            ejeY.setLabel(s.unidad == null ? "" : s.unidad);
            LineChart<String, Number> grafico = new LineChart<>(ejeX, ejeY);
            grafico.setLegendVisible(false);
            grafico.setAnimated(false);
            grafico.setPrefHeight(260);
            grafico.setStyle("CHART_COLOR_1: #2563EB;");
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            int i = 0;
            for (EvolucionPaciente.Medicion m : s.numericas()) {
                // Se le agrega un espacio invisible por posición para que dos fechas iguales no se pisen.
                String x = formatoCorto.format(m.fecha) + (m.edad >= 0 ? " (" + m.edad + " a)" : "")
                        + new String(new char[i++]).replace('\0', '​');
                serie.getData().add(new XYChart.Data<String, Number>(x, m.numero));
            }
            grafico.getData().add(serie);
            nodos.add(grafico);
        } else {
            Label nota = new Label(s.numericas().size() < 2 && s.mediciones.size() >= 2
                    ? "Este resultado es de texto: se muestra en la tabla."
                    : "Por ahora hay un solo valor: con dos o más se dibuja el gráfico.");
            nota.getStyleClass().add("ayuda-diseno");
            nodos.add(nota);
        }
        TableView<EvolucionPaciente.Medicion> tabla = new TableView<>();
        TableColumn<EvolucionPaciente.Medicion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(formatoFecha.format(d.getValue().fecha)));
        TableColumn<EvolucionPaciente.Medicion, String> colEdad = new TableColumn<>("Edad");
        colEdad.setCellValueFactory(d -> new SimpleStringProperty(EvolucionPaciente.textoEdad(d.getValue().edad)));
        TableColumn<EvolucionPaciente.Medicion, String> colValor = new TableColumn<>("Resultado");
        colValor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().valor
                + (d.getValue().unidad == null || d.getValue().numero == null ? "" : " " + d.getValue().unidad)));
        TableColumn<EvolucionPaciente.Medicion, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estado == null ? "-" : d.getValue().estado));
        colEstado.setCellFactory(c -> new TableCell<EvolucionPaciente.Medicion, String>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                setText(vacio ? null : texto);
                boolean fuera = "Alto".equalsIgnoreCase(texto) || "Bajo".equalsIgnoreCase(texto);
                setStyle(fuera ? "-fx-text-fill: #B91C1C; -fx-font-weight: bold;" : "");
            }
        });
        TableColumn<EvolucionPaciente.Medicion, String> colRef = new TableColumn<>("Referencia");
        colRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().referencia == null ? "" : d.getValue().referencia));
        colRef.setPrefWidth(240);
        tabla.getColumns().setAll(colFecha, colEdad, colValor, colEstado, colRef);
        tabla.getItems().setAll(s.mediciones);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(Math.min(300, 52 + s.mediciones.size() * 46));
        nodos.add(tabla);
        return nodos;
    }

    /** Celda que muestra el texto completo al pasar el mouse (por si no entra). */
    private static <T> TableCell<T, String> celdaConTooltip() {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                setText(vacio ? null : texto);
                setTooltip(vacio || texto == null || texto.isEmpty() ? null : new javafx.scene.control.Tooltip(texto));
            }
        };
    }

    private VBox tarjeta(String titulo, String descripcion, Node contenido) {
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("titulo-seccion");
        VBox caja = new VBox(8, lbl);
        if (descripcion != null) {
            Label d = new Label(descripcion);
            d.getStyleClass().add("ayuda-diseno");
            d.setWrapText(true);
            caja.getChildren().add(d);
        }
        caja.getChildren().add(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(18));
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private VBox tarjetaNumero(String numero, String texto) {
        Label n = new Label(numero);
        n.setStyle("-fx-font-size: " + (numero.length() > 8 ? 16 : 26) + "px; -fx-font-weight: bold; -fx-text-fill: #1E5C3D;");
        Label t = new Label(texto);
        t.setWrapText(true);
        t.setStyle("-fx-font-size: 12px; -fx-text-fill: #5F6966;");
        VBox caja = new VBox(4, n, t);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(14, 16, 14, 16));
        caja.setPrefWidth(230);
        caja.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    // ------------------------------------------------------------------ PDF / imprimir

    private void guardarPdf() {
        EvolucionPaciente ev = actual;
        if (ev == null) {
            return;
        }
        botonPdf.setDisable(true);
        TareaFondo.ejecutar(() -> reportes.InformesPdfService.guardarEvolucion(ev), archivo -> {
            botonPdf.setDisable(false);
            vistas.javafx.registros.RegistrosScreen.avisoPdf(archivo, "Se guardó el registro evolutivo de " + ev.getNombre());
        }, error -> {
            botonPdf.setDisable(false);
            ToastUtil.error("No se pudo guardar el PDF: " + TareaFondo.mensaje(error));
        });
    }

    private void imprimir() {
        EvolucionPaciente ev = actual;
        if (ev == null) {
            return;
        }
        TareaFondo.ejecutar(() -> reportes.InformesPdfService.maquetarEvolucion(ev), doc ->
                javax.swing.SwingUtilities.invokeLater(() -> reportes.ImpresionUtil.imprimir(doc,
                        "Registro evolutivo - " + ev.getNombre())),
                error -> ToastUtil.error("No se pudo preparar la impresión: " + TareaFondo.mensaje(error)));
    }
}
