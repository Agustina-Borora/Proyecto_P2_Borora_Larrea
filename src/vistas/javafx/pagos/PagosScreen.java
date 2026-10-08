package vistas.javafx.pagos;

import dao.PagoDAO.DatosPagos;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.FilaPago;
import modelo.ResumenPagos;
import utilidades.CsvUtil;
import utilidades.SelectorArchivoUtil;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla "Pagos" (sección Administración), en JavaFX puro (sin FXML): resumen financiero del
 * mes en curso y cobros pendientes a obras sociales, siguiendo el diseño de Figma (nodos
 * 246:4449 "Resumen" y 246:4958 "Cobros a Obras Sociales").
 *
 * <p>La pestaña "Resumen" sale de {@code dao.PagoDAO#cargarDatosPagos} (pedidos del mes en
 * curso). La pestaña "Cobros a Obras Sociales" sale de {@code dao.PagoDAO#cargarCobrosObraSocial}
 * -- a diferencia del Resumen, no está limitada al mes, porque una deuda de un mes anterior que
 * todavía no se cobró no debería dejar de verse sólo porque cambió el mes. Desde el 2026-10-01
 * {@code pagos.estado_cobro_os} guarda si cada cobro está "pendiente", "en_gestion" o "cobrado"
 * (ver la migración {@code 2026-10-01_estado_cobro_obra_social.sql}), así que esta pestaña ya no
 * es sólo informativa: se puede cambiar el estado por fila, marcar varias como cobradas de una y
 * exportar lo que está pendiente de gestionar.</p>
 */
public class PagosScreen {

    private static final NumberFormat FORMATO_MONEDA = crearFormatoMoneda();
    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat FORMATO_FECHA_LARGA = new SimpleDateFormat("yyyy-MM-dd");

    private static NumberFormat crearFormatoMoneda() {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        return formato;
    }

    private static String moneda(BigDecimal valor) {
        return "$" + FORMATO_MONEDA.format(valor != null ? valor : BigDecimal.ZERO);
    }

    private final Label valorTotalMes = new Label("$0");
    private final Label valorCobradoParticular = new Label("$0");
    private final Label valorPendienteOs = new Label("$0");
    private final Label valorTotalHoy = new Label("$0");
    private final Label subtituloTotalHoy = new Label("Jornada");

    private final Label valorTotalACobrar = new Label("$0");
    private final Label valorObrasConSaldo = new Label("0");

    private final Label badgePendienteOs = new Label("$0");

    private final ObservableList<FilaPago> ordenes = FXCollections.observableArrayList();
    private final ObservableList<FilaPago> cobrosOs = FXCollections.observableArrayList();

    /**
     * Copia completa (sin filtrar) de lo que trajo la base, para poder volver a filtrar por texto
     * ({@link #filtrarPorTexto}) sin volver a consultarla -- mismo criterio que
     * {@code EscritorioScreen#todasLasOrdenes}.
     */
    private final List<FilaPago> todasLasOrdenes = new ArrayList<>();

    /**
     * Copia completa (sin filtrar, incluye las ya cobradas) de "Cobros a Obras Sociales" -- de
     * acá salen tanto la tabla filtrada ({@link #cobrosOs}) como las tarjetas de totales, que
     * siempre cuentan sólo lo pendiente/en gestión sin importar el filtro de texto o el checkbox
     * "Mostrar cobradas también".
     */
    private final List<FilaPago> todosLosCobrosOs = new ArrayList<>();

    /**
     * Pedidos tildados en la tabla de Cobros a Obras Sociales (por id_pedido), para "Marcar
     * seleccionadas como cobradas". Se guarda acá y no en {@link FilaPago} porque las celdas de
     * la tabla se reciclan -- esto sobrevive a eso.
     */
    private final Set<Integer> idsSeleccionados = new HashSet<>();

    private boolean mostrarCobradas = false;
    private String ultimoTextoBuscado = "";
    private boolean pestanaResumenActiva = true;

    private final StackPane areaContenido = new StackPane();
    private HBox itemResumen;
    private HBox itemCobrosOs;
    private Parent panelResumen;
    private Parent panelCobrosOs;

    public Parent construir() {
        VBox raiz = new VBox(20);
        raiz.getStyleClass().add("pantalla");

        panelResumen = armarPanelResumen();
        panelCobrosOs = armarPanelCobrosOs();
        areaContenido.getChildren().setAll(panelResumen);
        VBox.setVgrow(areaContenido, Priority.ALWAYS);

        raiz.getChildren().addAll(armarEncabezado(), armarBarraPestanas(), areaContenido);

        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        Label titulo = new Label("Pagos");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Resumen financiero y cobros a obras sociales");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(4, titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonExportar = new Button("Exportar CSV");
        botonExportar.getStyleClass().add("boton-secundario");
        botonExportar.setOnAction(evt -> exportarCsv());

        HBox fila = new HBox(0, textos, espaciador, botonExportar);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /**
     * Exporta a .csv lo que está cargado en este momento en la pestaña activa -- "Resumen" (las
     * órdenes del mes) o "Cobros a Obras Sociales" (lo que haya quedado después del filtro de
     * texto y del checkbox "Mostrar cobradas también"; pensado para mandarle a una obra social el
     * listado de lo que todavía se le está por presentar/cobrar) -- Mes 6: Exportación de datos.
     */
    private void exportarCsv() {
        if (pestanaResumenActiva) {
            exportarCsvResumen();
        } else {
            exportarCsvCobrosOs();
        }
    }

    private void exportarCsvResumen() {
        File archivo = vistas.javafx.SelectorArchivoFx.elegirDestinoCsv(null, "pagos.csv");
        if (archivo == null) {
            return;
        }

        String[] encabezados = {
            "Orden", "Paciente", "Examen", "Fecha", "Cobertura", "Total", "Monto OS", "Monto Pac."
        };
        List<String[]> filas = new ArrayList<>();
        for (FilaPago f : ordenes) {
            filas.add(new String[]{
                vacio(f.getNumeroOrden()), vacio(f.getPaciente()), vacio(f.getExamenes()),
                f.getFecha() != null ? FORMATO_FECHA.format(f.getFecha()) : "",
                vacio(f.getTipoCobertura()), moneda(f.getTotal()), moneda(f.getMontoOs()), moneda(f.getMontoPac())
            });
        }

        try {
            CsvUtil.escribir(archivo, encabezados, filas);
            mostrarMensaje("Exportación completa",
                    "Se exportaron " + filas.size() + " órdenes a:\n" + archivo.getAbsolutePath(), false);
        } catch (IOException e) {
            mostrarMensaje("Error", "No se pudo exportar el archivo: " + e.getMessage(), true);
        }
    }

    private void exportarCsvCobrosOs() {
        File archivo = vistas.javafx.SelectorArchivoFx.elegirDestinoCsv(null, "cobros_obras_sociales.csv");
        if (archivo == null) {
            return;
        }

        String[] encabezados = {"Orden", "Paciente", "Obra Social", "Fecha", "Monto OS", "Estado", "Fecha Cobro"};
        List<String[]> filas = new ArrayList<>();
        for (FilaPago f : cobrosOs) {
            filas.add(new String[]{
                vacio(f.getNumeroOrden()), vacio(f.getPaciente()), vacio(f.getNombreObraSocial()),
                f.getFecha() != null ? FORMATO_FECHA.format(f.getFecha()) : "",
                moneda(f.getMontoOs()), etiquetaEstado(f.getEstadoCobroOs()),
                f.getFechaCobroOs() != null ? FORMATO_FECHA.format(f.getFechaCobroOs()) : ""
            });
        }

        try {
            CsvUtil.escribir(archivo, encabezados, filas);
            mostrarMensaje("Exportación completa",
                    "Se exportaron " + filas.size() + " órdenes a:\n" + archivo.getAbsolutePath(), false);
        } catch (IOException e) {
            mostrarMensaje("Error", "No se pudo exportar el archivo: " + e.getMessage(), true);
        }
    }

    private void mostrarMensaje(String titulo, String contenido, boolean error) {
        if (!error) {
            vistas.javafx.AvisoRapido.mostrar(titulo, contenido);
            return;
        }
        Alert alerta = new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alerta.setHeaderText(null);
        alerta.setTitle(titulo);
        alerta.setContentText(contenido);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    // ---- Pestañas (Resumen / Cobros a Obras Sociales) ----

    private HBox armarBarraPestanas() {
        itemResumen = crearItemPestana("Resumen", null);
        itemCobrosOs = crearItemPestana("Cobros a Obras Sociales", badgePendienteOs);

        itemResumen.setOnMouseClicked(evt -> seleccionarPestana(true));
        itemCobrosOs.setOnMouseClicked(evt -> seleccionarPestana(false));

        HBox barra = new HBox(28, itemResumen, itemCobrosOs);
        barra.setStyle("-fx-border-color: transparent transparent #EEF1EE transparent; -fx-border-width: 0 0 1 0;");
        seleccionarPestana(true);
        return barra;
    }

    private HBox crearItemPestana(String texto, Label badge) {
        Label etiqueta = new Label(texto);
        HBox item = badge != null ? new HBox(8, etiqueta, badge) : new HBox(0, etiqueta);
        item.getStyleClass().add("tab-item");
        item.setAlignment(Pos.CENTER_LEFT);
        if (badge != null) {
            badge.getStyleClass().add("tab-badge");
        }
        return item;
    }

    private void seleccionarPestana(boolean resumen) {
        pestanaResumenActiva = resumen;
        itemResumen.getStyleClass().remove("tab-item-activo");
        itemCobrosOs.getStyleClass().remove("tab-item-activo");
        if (resumen) {
            itemResumen.getStyleClass().add("tab-item-activo");
            areaContenido.getChildren().setAll(panelResumen);
        } else {
            itemCobrosOs.getStyleClass().add("tab-item-activo");
            areaContenido.getChildren().setAll(panelCobrosOs);
        }
    }

    // ---- Pestaña Resumen ----

    private VBox armarPanelResumen() {
        VBox panel = new VBox(20, armarTarjetasResumen(), armarSeccionOrdenes());
        return panel;
    }

    /**
     * Mes 8 (rediseño visual): mismo lenguaje visual que ya aprobó la clienta para el Escritorio
     * (ícono + franja de color + efecto hover) en vez de las cajas de color liso de antes -- ver
     * {@link vistas.javafx.TarjetaStatUtil}.
     */
    private HBox armarTarjetasResumen() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir(
                        "Total del mes", valorTotalMes, "Ingresos totales", "azul", "💰"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Cobrado Particular", valorCobradoParticular, "Cobrado en efectivo", "verde", "💵"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Pendiente OS", valorPendienteOs, "Por cobrar a obras sociales", "ambar", "⏳"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Total Hoy", valorTotalHoy, subtituloTotalHoy, "rojo", "📅"));
    }

    private VBox armarSeccionOrdenes() {
        Label titulo = new Label("Todas las órdenes del mes");
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 18px;");

        TableView<FilaPago> tabla = armarTablaOrdenes();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox seccion = new VBox(12, titulo, tabla);
        return seccion;
    }

    private TableView<FilaPago> armarTablaOrdenes() {
        TableView<FilaPago> tabla = new TableView<>();

        TableColumn<FilaPago, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);

        TableColumn<FilaPago, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(160);

        TableColumn<FilaPago, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getExamenes())));
        colExamen.setPrefWidth(200);

        TableColumn<FilaPago, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? FORMATO_FECHA.format(d.getValue().getFecha()) : ""));
        colFecha.setPrefWidth(90);

        TableColumn<FilaPago, FilaPago> colCobertura = new TableColumn<>("Cobertura");
        colCobertura.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colCobertura.setCellFactory(col -> new TableCell<FilaPago, FilaPago>() {
            @Override
            protected void updateItem(FilaPago fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setGraphic(vacio || fila == null ? null : armarPillCobertura(fila.getTipoCobertura()));
            }
        });
        colCobertura.setPrefWidth(110);

        TableColumn<FilaPago, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(moneda(d.getValue().getTotal())));
        colTotal.setPrefWidth(90);

        TableColumn<FilaPago, String> colMontoOs = new TableColumn<>("Monto OS");
        colMontoOs.setCellValueFactory(d -> new SimpleStringProperty(
                esCero(d.getValue().getMontoOs()) ? "—" : moneda(d.getValue().getMontoOs())));
        colMontoOs.setPrefWidth(90);

        TableColumn<FilaPago, String> colMontoPac = new TableColumn<>("Monto Pac.");
        colMontoPac.setCellValueFactory(d -> new SimpleStringProperty(
                esCero(d.getValue().getMontoPac()) ? "—" : moneda(d.getValue().getMontoPac())));
        colMontoPac.setPrefWidth(90);

        TableColumn<FilaPago, FilaPago> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<FilaPago, FilaPago>() {
            private final Button botonAnular = new Button("Anular");

            {
                botonAnular.getStyleClass().add("boton-fila-icono");
                botonAnular.setStyle("-fx-text-fill: #B3261E;");
                botonAnular.setTooltip(new Tooltip("Anular la cobertura/pago registrado para esta orden"));
            }

            @Override
            protected void updateItem(FilaPago fila, boolean vacio) {
                super.updateItem(fila, vacio);
                if (vacio || fila == null) {
                    setGraphic(null);
                    return;
                }
                botonAnular.setOnAction(evt -> confirmarAnular(fila));
                setGraphic(botonAnular);
            }
        });
        colAcciones.setPrefWidth(100);
        colAcciones.setMinWidth(90);
        // No resizable: con 9 columnas en esta tabla, CONSTRAINED_RESIZE_POLICY puede necesitar
        // achicar columnas por debajo de su propio mínimo para que todo encaje, y a esta columna
        // eso le corta el botón "Anular" (mismo ajuste aplicado a todas las tablas con columna de
        // Acciones, 2026-10-01 -- ver el comentario completo en PacientesScreen).
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colOrden, colPaciente, colExamen, colFecha, colCobertura, colTotal, colMontoOs, colMontoPac, colAcciones);
        tabla.setItems(ordenes);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Todavía no hay pedidos cargados este mes."));
        return tabla;
    }

    /**
     * Pide el motivo y anula (borrado lógico -- ver {@code dao.PagoDAO#anularPago}) el
     * pago/cobertura de la orden elegida. El motivo es obligatorio: queda guardado en
     * {@code pagos.motivo_anulacion} para que se pueda auditar después por qué se anuló.
     */
    private void confirmarAnular(FilaPago fila) {
        TextInputDialog dialogoMotivo = new TextInputDialog();
        dialogoMotivo.setTitle("Anular pago");
        dialogoMotivo.setHeaderText(null);
        dialogoMotivo.setContentText("Motivo de la anulación de la orden " + vacio(fila.getNumeroOrden()) + ":");
        Optional<String> motivo = dialogoMotivo.showAndWait();
        if (!motivo.isPresent() || motivo.get().trim().isEmpty()) {
            return;
        }

        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Seguro que querés anular el pago de la orden " + vacio(fila.getNumeroOrden())
                + "? La orden queda sin cobertura registrada (como si nunca se hubiera cargado).",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Anular pago");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton != ButtonType.YES) {
                return;
            }
            Boolean ok = PuenteEDT.ejecutar(
                    () -> controlador.PagoController.anularPago(null, fila.getIdPedido(), motivo.get().trim()),
                    false);
            if (ok != null && ok) {
                cargarDatos();
            } else {
                mostrarMensaje("No se pudo anular",
                        "Esta orden no tiene un pago registrado para anular (puede que ya estuviera anulado).",
                        true);
            }
        });
    }

    private Label armarPillCobertura(String tipoCobertura) {
        String texto;
        String estilo;
        if ("OBRA_SOCIAL".equals(tipoCobertura)) {
            texto = "Obra Social";
            estilo = "pill-obra-social";
        } else if ("MIXTO".equals(tipoCobertura)) {
            texto = "Mixto";
            estilo = "pill-mixto";
        } else {
            texto = "Particular";
            estilo = "pill-particular";
        }
        Label pill = new Label(texto);
        pill.getStyleClass().add(estilo);
        return pill;
    }

    // ---- Pestaña Cobros a Obras Sociales ----

    private VBox armarPanelCobrosOs() {
        return new VBox(20, armarTarjetasCobrosOs(), armarSeccionCobrosOs());
    }

    private HBox armarTarjetasCobrosOs() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir("Total a Cobrar", valorTotalACobrar,
                        "Adeudado por obras sociales (pendiente o en gestión)", "ambar", "⏳"),
                vistas.javafx.TarjetaStatUtil.construir("Obras Sociales con Saldo", valorObrasConSaldo,
                        "Con órdenes pendientes de cobro", "azul", "🏢"));
    }

    private VBox armarSeccionCobrosOs() {
        Label titulo = new Label("Órdenes con cobertura de obra social");
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 18px;");

        // Mes 8 (rediseño visual): la clienta avisó "no se entiende qué hay que hacer" en esta
        // pantalla -- esta bajada explica en una oración el flujo de los 3 estados antes de que
        // aparezca la tabla, y la fila de referencias de abajo lo refuerza con color.
        Label descripcion = new Label(
                "A medida que gestionás el cobro de cada orden con la obra social correspondiente, "
                + "cambiá su estado acá abajo (en la columna \"Estado\" de la tabla, o seleccionando "
                + "varias filas a la vez con la columna \"Sel.\"):");
        descripcion.getStyleClass().add("descripcion-seccion");
        descripcion.setWrapText(true);

        FlowPane referencias = armarReferenciasEstadoCobro();
        HBox controles = armarControlesCobrosOs();
        TableView<FilaPago> tabla = armarTablaCobrosOs();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return new VBox(12, titulo, descripcion, referencias, controles, tabla);
    }

    /**
     * Mes 8 (rediseño visual), segunda vuelta: referencia visual de los tres estados posibles de
     * un cobro a obra social, en lenguaje llano -- mismas 3 píldoras de color (reutiliza
     * {@code .pill-pendiente}/{@code .pill-procesando}/{@code .pill-activo}, ya usadas en otras
     * pantallas para estados parecidos) que después colorean el punto de color al lado del combo
     * de cada fila de la tabla (ver {@code colorEstado}), para que el significado de cada color
     * quede claro antes de llegar a la tabla en sí.
     *
     * <p>La primera vuelta armaba esta fila con un {@link HBox}: con las 3 píldoras + sus 3
     * explicaciones una al lado de la otra, en una ventana angosta no entraba todo el ancho que
     * pedía y el texto de cada {@link Label} se cortaba solo con puntos suspensivos ("Pendi...",
     * "En ges...", "Cob..."), el mismo problema de letras que no entran que ya se había dado en
     * otras pantallas. Un {@link FlowPane} no tiene ese problema: cuando no entra todo en una
     * fila, en vez de achicar el texto de cada ítem hasta que no se lea, pasa el/los ítems que no
     * entraron a una segunda línea -- el texto de cada referencia siempre se ve completo.</p>
     */
    private FlowPane armarReferenciasEstadoCobro() {
        FlowPane fila = new FlowPane(24, 8,
                crearReferenciaEstado("pill-pendiente", "Pendiente",
                        "todavía no se presentó el reclamo a la obra social"),
                crearReferenciaEstado("pill-procesando", "En gestión",
                        "ya se presentó y se está esperando que la obra social pague"),
                crearReferenciaEstado("pill-activo", "Cobrado",
                        "la obra social ya pagó esta orden"));
        return fila;
    }

    private HBox crearReferenciaEstado(String clasePill, String etiqueta, String explicacion) {
        Label pill = new Label(etiqueta);
        pill.getStyleClass().add(clasePill);

        Label texto = new Label(explicacion);
        texto.getStyleClass().add("leyenda-grafico-texto");

        HBox caja = new HBox(8, pill, texto);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private HBox armarControlesCobrosOs() {
        CheckBox checkMostrarCobradas = new CheckBox("Mostrar cobradas también");
        checkMostrarCobradas.setSelected(mostrarCobradas);
        checkMostrarCobradas.setTooltip(new Tooltip(
                "Por default esta tabla no muestra las órdenes que ya están cobradas, para no "
                + "acumular filas viejas. Tildá esto para verlas igual."));
        checkMostrarCobradas.setOnAction(evt -> {
            mostrarCobradas = checkMostrarCobradas.isSelected();
            aplicarFiltrosCobrosOs();
        });

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonMarcarCobradas = new Button("Marcar seleccionadas como cobradas");
        botonMarcarCobradas.getStyleClass().add("boton-primario");
        botonMarcarCobradas.setTooltip(new Tooltip(
                "Pasa a \"Cobrado\" todas las órdenes tildadas en la columna \"Sel.\" de la tabla, "
                + "con la fecha de hoy. Usalo cuando la obra social ya pagó varias órdenes juntas."));
        botonMarcarCobradas.setOnAction(evt -> marcarSeleccionadasComoCobradas());

        HBox fila = new HBox(12, checkMostrarCobradas, espaciador, botonMarcarCobradas);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private TableView<FilaPago> armarTablaCobrosOs() {
        TableView<FilaPago> tabla = new TableView<>();

        // Mes 8: antes esta columna no tenía título (quedaba una columna angosta con sólo un
        // tilde, sin ninguna pista de para qué servía) -- la clienta avisó que no se entendía qué
        // había que hacer en esta pantalla. "Sel." más el ancho un poco mayor (34 -> 50) alcanza
        // para dejar en claro que es para elegir qué órdenes entran en "Marcar seleccionadas como
        // cobradas" de más abajo.
        TableColumn<FilaPago, FilaPago> colSel = new TableColumn<>("Sel.");
        colSel.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colSel.setCellFactory(col -> new TableCell<FilaPago, FilaPago>() {
            private final CheckBox check = new CheckBox();

            @Override
            protected void updateItem(FilaPago fila, boolean vacio) {
                super.updateItem(fila, vacio);
                if (vacio || fila == null) {
                    setGraphic(null);
                    return;
                }
                boolean yaCobrada = "cobrado".equals(fila.getEstadoCobroOs());
                check.setOnAction(null);
                check.setSelected(!yaCobrada && idsSeleccionados.contains(fila.getIdPedido()));
                check.setDisable(yaCobrada);
                check.setOnAction(evt -> {
                    if (check.isSelected()) {
                        idsSeleccionados.add(fila.getIdPedido());
                    } else {
                        idsSeleccionados.remove(fila.getIdPedido());
                    }
                });
                setGraphic(check);
            }
        });
        colSel.setPrefWidth(50);
        colSel.setMinWidth(50);
        colSel.setMaxWidth(50);
        colSel.setResizable(false);

        TableColumn<FilaPago, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);

        TableColumn<FilaPago, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(160);

        TableColumn<FilaPago, String> colObraSocial = new TableColumn<>("Obra Social");
        colObraSocial.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNombreObraSocial())));
        colObraSocial.setPrefWidth(160);

        TableColumn<FilaPago, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? FORMATO_FECHA.format(d.getValue().getFecha()) : ""));
        colFecha.setPrefWidth(90);

        TableColumn<FilaPago, String> colMontoOs = new TableColumn<>("Monto OS");
        colMontoOs.setCellValueFactory(d -> new SimpleStringProperty(moneda(d.getValue().getMontoOs())));
        colMontoOs.setPrefWidth(100);

        TableColumn<FilaPago, FilaPago> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<FilaPago, FilaPago>() {
            private final ComboBox<String> combo = new ComboBox<>(
                    FXCollections.observableArrayList("Pendiente", "En gestión", "Cobrado"));
            // Mes 8 (rediseño visual): punto de color al lado del combo, mismo color que la
            // referencia de armarReferenciasEstadoCobro -- así el estado se reconoce de un
            // vistazo por color además de por texto, igual que las píldoras de otras pantallas
            // (acá no se usa una píldora directamente porque esta celda necesita seguir siendo
            // editable con un combo, no sólo mostrar texto).
            private final Region punto = new Region();
            private final HBox caja = new HBox(6, punto, combo);
            // Se guarda la fila de esta celda acá en vez de usar getTableRow().getItem() -- con
            // generics de por medio (TableCell<FilaPago, FilaPago>) esa llamada le resulta
            // ambigua al compilador real (no al de prueba) y tira "Object cannot be converted to
            // FilaPago" al compilar. Este criterio (guardar el ítem actual en updateItem y leerlo
            // en el listener) es el mismo que ya usaba el combo de Rol/Estado en otras pantallas.
            private FilaPago filaActual;

            {
                combo.setOnAction(evt -> cambiarEstadoDesdeCombo(filaActual, combo));
                punto.setMinSize(10, 10);
                punto.setPrefSize(10, 10);
                punto.setMaxSize(10, 10);
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(FilaPago fila, boolean vacio) {
                super.updateItem(fila, vacio);
                if (vacio || fila == null) {
                    filaActual = null;
                    setGraphic(null);
                    return;
                }
                filaActual = fila;
                combo.setValue(etiquetaEstado(fila.getEstadoCobroOs()));
                punto.setStyle("-fx-background-radius: 5; -fx-background-color: "
                        + colorEstado(fila.getEstadoCobroOs()) + ";");
                setGraphic(caja);
            }
        });
        // Mes 8: 130 ya no alcanzaba -- el padding nuevo de las celdas (ver theme.css) le comía
        // el espacio al ComboBox y "Pendiente"/"En gestión" quedaban cortados ("Pendi..."), que
        // era parte de lo que hacía que esta pantalla "no se entienda qué hay que hacer" (mismo
        // ajuste que ya se hizo en CatalogoExamenesScreen/ObrasSocialesScreen/
        // RegistrarResultadosScreen). 190 (en vez de 170) porque ahora también entra el punto de
        // color de al lado.
        colEstado.setPrefWidth(190);
        colEstado.setResizable(false);

        TableColumn<FilaPago, String> colFechaCobro = new TableColumn<>("Fecha Cobro");
        colFechaCobro.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaCobroOs() != null ? FORMATO_FECHA.format(d.getValue().getFechaCobroOs()) : "-"));
        colFechaCobro.setPrefWidth(100);

        tabla.getColumns().setAll(colSel, colOrden, colPaciente, colObraSocial, colFecha, colMontoOs, colEstado, colFechaCobro);
        tabla.setItems(cobrosOs);
        // Mes 8 (rediseño visual), segunda vuelta: esta tabla tiene 8 columnas (la que más tiene
        // de toda la app) y 2 de ellas son de ancho fijo (Sel. y Estado, por el checkbox/combo
        // que llevan adentro). Con CONSTRAINED_RESIZE_POLICY -- lo que usan el resto de las
        // tablas de la app -- las columnas de ancho fijo no ceden nada y el resto se achica a la
        // fuerza para que todas entren en el ancho de la ventana, aunque eso corte el texto de
        // los encabezados ("Fecha Cobro" quedaba en "Fecl"). Sin fijar una política acá, la
        // tabla usa la que trae por default (UNCONSTRAINED_RESIZE_POLICY): cada columna conserva
        // el ancho que se le dio y, si entre todas no entran en el ancho disponible, la tabla
        // misma muestra su propia barra de desplazamiento horizontal (no depende del scroll
        // general de la pantalla) en vez de aplastar el texto hasta que no se lea.
        tabla.setPlaceholder(new Label("No hay cobros pendientes a obras sociales."));
        return tabla;
    }

    /**
     * Callback del {@link ComboBox} de Estado de una fila: si el valor elegido es distinto del
     * que ya tenía esa orden, lo guarda. Si falla, vuelve a mostrar el valor real en vez de dejar
     * en pantalla un cambio que no se guardó.
     */
    private void cambiarEstadoDesdeCombo(FilaPago fila, ComboBox<String> combo) {
        if (fila == null) {
            return;
        }
        String nuevaClave = claveEstado(combo.getValue());
        if (nuevaClave.equals(fila.getEstadoCobroOs())) {
            return;
        }
        Boolean ok = PuenteEDT.ejecutar(
                () -> controlador.PagoController.actualizarEstadoCobro(null, fila.getIdPedido(), nuevaClave),
                false);
        if (ok != null && ok) {
            cargarDatos();
        } else {
            combo.setValue(etiquetaEstado(fila.getEstadoCobroOs()));
            mostrarMensaje("No se pudo actualizar",
                    "No se pudo cambiar el estado de cobro de la orden " + vacio(fila.getNumeroOrden()) + ".", true);
        }
    }

    /**
     * Botón "Marcar seleccionadas como cobradas": pide confirmación y aplica el cambio a todos
     * los pedidos tildados de una vez (ver {@code controlador.PagoController#actualizarEstadoCobroLote}).
     */
    private void marcarSeleccionadasComoCobradas() {
        if (idsSeleccionados.isEmpty()) {
            mostrarMensaje("Nada seleccionado",
                    "Tildá al menos una orden de la tabla para marcarla como cobrada.", true);
            return;
        }
        List<Integer> ids = new ArrayList<>(idsSeleccionados);
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Marcar " + ids.size() + (ids.size() == 1 ? " orden" : " órdenes") + " como cobradas?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Marcar como cobradas");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton != ButtonType.YES) {
                return;
            }
            Integer actualizadas = PuenteEDT.ejecutar(
                    () -> controlador.PagoController.actualizarEstadoCobroLote(null, ids, "cobrado"),
                    0);
            idsSeleccionados.clear();
            cargarDatos();
            mostrarMensaje("Listo",
                    (actualizadas != null ? actualizadas : 0) + " orden(es) marcadas como cobradas.", false);
        });
    }

    private static String etiquetaEstado(String clave) {
        if ("en_gestion".equals(clave)) {
            return "En gestión";
        }
        if ("cobrado".equals(clave)) {
            return "Cobrado";
        }
        return "Pendiente";
    }

    private static String claveEstado(String etiqueta) {
        if ("En gestión".equals(etiqueta)) {
            return "en_gestion";
        }
        if ("Cobrado".equals(etiqueta)) {
            return "cobrado";
        }
        return "pendiente";
    }

    // Mes 8 (rediseño visual): mismos 3 colores que usan las píldoras .pill-pendiente/
    // .pill-procesando/.pill-activo en theme.css (el tono de texto de cada una, que al ser un
    // color lleno sirve de punto de color), para que el punto de colEstado coincida con la
    // referencia de armarReferenciasEstadoCobro.
    private static String colorEstado(String clave) {
        if ("en_gestion".equals(clave)) {
            return "#1B5FA8";
        }
        if ("cobrado".equals(clave)) {
            return "#1E5C3D";
        }
        return "#B9770E";
    }

    // ---- Datos ----

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    private static boolean esCero(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) == 0;
    }

    private void cargarDatos() {
        DatosPagos datos = PuenteEDT.ejecutar(
                () -> controlador.PagoController.cargarDatosPagos(null),
                new DatosPagos());

        ResumenPagos resumen = datos.resumen;
        valorTotalMes.setText(moneda(resumen.getTotalMes()));
        valorCobradoParticular.setText(moneda(resumen.getCobradoParticular()));
        valorPendienteOs.setText(moneda(resumen.getPendienteOs()));
        valorTotalHoy.setText(moneda(resumen.getTotalHoy()));
        subtituloTotalHoy.setText("Jornada " + FORMATO_FECHA_LARGA.format(new java.util.Date()));

        List<FilaPago> listaOrdenes = datos.ordenes != null ? datos.ordenes : Collections.emptyList();
        todasLasOrdenes.clear();
        todasLasOrdenes.addAll(listaOrdenes);
        ordenes.setAll(listaOrdenes);

        // Siempre se trae todo (incluidas las cobradas): el checkbox "Mostrar cobradas también" y
        // el buscador filtran en memoria (ver aplicarFiltrosCobrosOs), sin volver a consultar la
        // base por cada cambio.
        List<FilaPago> listaCobrosOs = PuenteEDT.ejecutar(
                () -> controlador.PagoController.cargarCobrosObraSocial(null, true),
                Collections.<FilaPago>emptyList());
        todosLosCobrosOs.clear();
        todosLosCobrosOs.addAll(listaCobrosOs);
        aplicarFiltrosCobrosOs();

        BigDecimal totalACobrar = BigDecimal.ZERO;
        Set<String> obrasConSaldo = new HashSet<>();
        for (FilaPago f : todosLosCobrosOs) {
            if (!"cobrado".equals(f.getEstadoCobroOs())) {
                totalACobrar = totalACobrar.add(f.getMontoOs());
                if (f.getNombreObraSocial() != null) {
                    obrasConSaldo.add(f.getNombreObraSocial());
                }
            }
        }
        valorTotalACobrar.setText(moneda(totalACobrar));
        valorObrasConSaldo.setText(String.valueOf(obrasConSaldo.size()));
        badgePendienteOs.setText(moneda(totalACobrar));
    }

    /**
     * Vuelve a calcular {@link #cobrosOs} a partir de {@link #todosLosCobrosOs}, aplicando el
     * checkbox "Mostrar cobradas también" y el último texto buscado -- se llama tanto desde
     * {@link #cargarDatos} como cada vez que cambia cualquiera de los dos filtros, sin volver a
     * consultar la base.
     */
    private void aplicarFiltrosCobrosOs() {
        String buscado = ultimoTextoBuscado == null ? "" : ultimoTextoBuscado.trim().toLowerCase();
        List<FilaPago> filtradas = new ArrayList<>();
        for (FilaPago f : todosLosCobrosOs) {
            if (!mostrarCobradas && "cobrado".equals(f.getEstadoCobroOs())) {
                continue;
            }
            if (!buscado.isEmpty() && !(contiene(f.getNumeroOrden(), buscado) || contiene(f.getPaciente(), buscado)
                    || contiene(f.getExamenes(), buscado) || contiene(f.getNombreObraSocial(), buscado))) {
                continue;
            }
            filtradas.add(f);
        }
        cobrosOs.setAll(filtradas);
    }

    /**
     * Filtra a la vez la tabla de "Todas las órdenes del mes" (por número de orden, paciente o
     * examen) y la de "Cobros a Obras Sociales" (por número de orden, paciente, examen u obra
     * social) -- la llama el buscador del header a cada tecla, cuando esta pantalla está activa
     * (ver {@code AppShell#navegar}, caso "6"). Filtra sobre los datos ya traídos de la base
     * ({@link #todasLasOrdenes} / {@link #todosLosCobrosOs}), sin volver a consultarla. Con el
     * texto vacío vuelve a mostrarlas todas. Se filtran las dos tablas juntas (no sólo la pestaña
     * visible en este momento) para que el filtro no se pierda al cambiar de pestaña.
     */
    public void filtrarPorTexto(String texto) {
        ultimoTextoBuscado = texto == null ? "" : texto;
        String buscado = ultimoTextoBuscado.trim().toLowerCase();

        if (buscado.isEmpty()) {
            ordenes.setAll(todasLasOrdenes);
        } else {
            List<FilaPago> ordenesFiltradas = new ArrayList<>();
            for (FilaPago f : todasLasOrdenes) {
                if (contiene(f.getNumeroOrden(), buscado) || contiene(f.getPaciente(), buscado)
                        || contiene(f.getExamenes(), buscado)) {
                    ordenesFiltradas.add(f);
                }
            }
            ordenes.setAll(ordenesFiltradas);
        }

        aplicarFiltrosCobrosOs();
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase().contains(buscado);
    }
}
