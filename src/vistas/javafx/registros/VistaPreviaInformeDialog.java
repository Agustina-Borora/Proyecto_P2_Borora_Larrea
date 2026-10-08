package vistas.javafx.registros;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.PauseTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javax.swing.SwingUtilities;
import modelo.DetalleOrden;
import reportes.DisenoInforme;
import reportes.DocumentoMaquetado;
import reportes.ImpresionUtil;
import reportes.InformesPdfService;
import reportes.RenderizadorGraphics;
import reportes.ResultadosDatos;
import utilidades.ArchivosUtil;
import utilidades.PreferenciasSistema;
import vistas.javafx.EstiloApp;
import vistas.javafx.TareaFondo;
import vistas.javafx.VentanaUtil;
import vistas.javafx.comunes.EditorDisenoPanel;

/**
 * "Vista previa y diseño del informe" de una ORDEN (todos sus estudios juntos).
 *
 * <p>A la izquierda se ve el informe tal cual va a salir (es exactamente el mismo dibujo que el
 * PDF y que la impresión). A la derecha están los controles para acomodarlo: tamaño de letra,
 * ancho de columnas, márgenes, dónde va el logo y la firma, etc. -- cada cambio se ve al
 * instante en la hoja. Esos cambios se pueden guardar solo para esta orden o como diseño para
 * todas.</p>
 *
 * <p>Abajo: "Guardar PDF" (lo guarda solo en la carpeta de informes, sin el cartel de Windows),
 * "Imprimir" y "Enviar". Todo lo lento (leer la base, armar las hojas, escribir el PDF) corre en
 * segundo plano, así la ventana nunca se tilda.</p>
 */
public class VistaPreviaInformeDialog {

    /** Nitidez de la imagen de cada hoja en pantalla. */
    private static final double ESCALA = 1.6;
    private static final double ANCHO_HOJA_PANTALLA = 600;

    private final DetalleOrden detalle;
    private final Runnable alTerminar;

    private Stage ventana;
    /** Lo que se dibuja: los datos originales con los cambios de texto aplicados. */
    private ResultadosDatos datos;
    private ResultadosDatos datosOriginales;
    private reportes.AjustesInforme ajustesGuardados = new reportes.AjustesInforme();
    private reportes.AjustesInforme ajustesActual = new reportes.AjustesInforme();
    private final VBox contenedorTextos = new VBox();
    private DisenoInforme disenoActual;
    private DisenoInforme disenoGuardado;
    private boolean tieneDisenoPropio;
    private DocumentoMaquetado documento;
    private File pdfGuardado;

    private final VBox hojas = new VBox(18);
    private final StackPane areaHojas = new StackPane();
    private final ProgressIndicator cargando = new ProgressIndicator();
    private final Label etiquetaEstado = new Label();
    private final Label etiquetaDisenoUsado = new Label();
    private final Slider zoom = new Slider(0.5, 1.6, 1.0);
    private final List<ImageView> vistasHojas = new ArrayList<>();
    private final AtomicInteger versionDibujo = new AtomicInteger();
    private final PauseTransition demora = new PauseTransition(Duration.millis(250));
    private EditorDisenoPanel editor;
    private RadioButton opcionSoloEsta;
    private RadioButton opcionTodas;
    private Button botonGuardarPdf;
    private Button botonImprimir;
    private Button botonEnviar;
    private Button botonGuardarDiseno;
    private Button botonVolverPredeterminado;
    private final HBox accionesPdf = new HBox(8);
    private boolean mostrarBotonEnviar = true;

    public VistaPreviaInformeDialog(DetalleOrden detalle) {
        this(detalle, null);
    }

    /** @param alTerminar se ejecuta al cerrar (por ejemplo, para refrescar la tabla de Registros). */
    public VistaPreviaInformeDialog(DetalleOrden detalle, Runnable alTerminar) {
        this.detalle = detalle;
        this.alTerminar = alTerminar;
    }

    /**
     * Esconde el botón "Enviar al paciente..." -- para cuando esta vista previa se abre desde la
     * misma ventana de "Enviar Resultados" (si no, se podrían ir abriendo una adentro de la otra).
     */
    public VistaPreviaInformeDialog sinBotonEnviar() {
        mostrarBotonEnviar = false;
        return this;
    }

    public void mostrar() {
        ventana = new Stage(StageStyle.DECORATED);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Informe de la orden #" + valorOGuion(detalle.getNumeroOrden()) + " -- vista previa y diseño");

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(18));

        raiz.setTop(armarEncabezado());
        BorderPane.setMargin(raiz.getTop(), new Insets(0, 0, 12, 0));

        ScrollPane scrollHojas = armarVisor();
        VBox panelDerecho = armarPanelDiseno();
        ScrollPane scrollDiseno = new ScrollPane(panelDerecho);
        scrollDiseno.setFitToWidth(true);
        scrollDiseno.getStyleClass().add("config-scroll");
        scrollDiseno.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        ScrollPane scrollTextos = new ScrollPane(armarPanelTextos());
        scrollTextos.setFitToWidth(true);
        scrollTextos.getStyleClass().add("config-scroll");
        scrollTextos.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Dos pestañas: "Diseño" (posiciones, letra, márgenes...) y "Textos" (lo que dice el informe).
        javafx.scene.control.TabPane scrollDerecho = new javafx.scene.control.TabPane(
                new javafx.scene.control.Tab("Diseño y posiciones", scrollDiseno),
                new javafx.scene.control.Tab("Textos y contenido", scrollTextos));
        scrollDerecho.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        scrollDerecho.setPrefWidth(460);
        scrollDerecho.setMinWidth(380);

        HBox centro = new HBox(16, scrollHojas, scrollDerecho);
        HBox.setHgrow(scrollHojas, Priority.ALWAYS);
        raiz.setCenter(centro);

        VBox abajo = armarAbajo();
        raiz.setBottom(abajo);
        BorderPane.setMargin(abajo, new Insets(12, 0, 0, 0));

        Scene escena = VentanaUtil.escenaAdaptable(ventana, raiz, 1280, 900);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        ventana.setOnCloseRequest(e -> {
            if (!confirmarCierre()) {
                e.consume();
            }
        });

        habilitar(false);
        demora.setOnFinished(e -> redibujar());
        cargar();
        ventana.showAndWait();
        if (alTerminar != null) {
            alTerminar.run();
        }
    }

    // ------------------------------------------------------------------ carga (en segundo plano)

    private void cargar() {
        mostrarEstado("Cargando el informe de la orden...", false);
        int idPedido = detalle.getIdPedido();
        TareaFondo.ejecutar(() -> {
            Object[] r = new Object[5];
            r[0] = InformesPdfService.cargarDatosOriginales(idPedido);
            r[4] = InformesPdfService.leerAjustes(idPedido);
            r[1] = InformesPdfService.disenoPropio(idPedido);
            r[2] = InformesPdfService.disenoPredeterminado();
            r[3] = InformesPdfService.pdfExistente(idPedido);
            return r;
        }, r -> {
            datosOriginales = (ResultadosDatos) r[0];
            ajustesGuardados = (reportes.AjustesInforme) r[4];
            ajustesActual = ajustesGuardados.copia();
            datos = ajustesActual.aplicar(datosOriginales);
            armarEditorTextos();
            DisenoInforme propio = (DisenoInforme) r[1];
            tieneDisenoPropio = propio != null;
            disenoGuardado = propio != null ? propio : (DisenoInforme) r[2];
            disenoActual = disenoGuardado.copia();
            pdfGuardado = (File) r[3];
            editor.setDiseno(disenoActual);
            actualizarTextoDiseno();
            habilitar(true);
            if (pdfGuardado != null) {
                mostrarPdfGuardado(pdfGuardado, "Esta orden ya tiene su PDF guardado.");
            } else {
                mostrarEstado("Todavía no se guardó el PDF de esta orden. Revisá cómo queda y tocá \"Guardar PDF\".", false);
            }
            redibujar();
        }, error -> {
            cargando.setVisible(false);
            mostrarEstado("No se pudo cargar el informe: " + TareaFondo.mensaje(error), true);
        });
    }

    /** Arma las hojas con el diseño actual y las pasa a imagen, en segundo plano. */
    private void redibujar() {
        if (datos == null) {
            return;
        }
        int version = versionDibujo.incrementAndGet();
        DisenoInforme diseno = disenoActual.copia();
        cargando.setVisible(true);
        TareaFondo.ejecutar(() -> {
            DocumentoMaquetado doc = InformesPdfService.maquetar(datos, diseno);
            List<BufferedImage> imagenes = new ArrayList<>();
            for (DocumentoMaquetado.Pagina p : doc.getPaginas()) {
                imagenes.add(RenderizadorGraphics.aImagen(p, ESCALA));
            }
            return new Object[]{doc, imagenes};
        }, r -> {
            if (version != versionDibujo.get()) {
                return; // ya hay un dibujo más nuevo en camino
            }
            documento = (DocumentoMaquetado) r[0];
            @SuppressWarnings("unchecked")
            List<BufferedImage> imagenes = (List<BufferedImage>) r[1];
            mostrarHojas(imagenes);
            cargando.setVisible(false);
        }, error -> {
            cargando.setVisible(false);
            mostrarEstado("No se pudo dibujar el informe: " + TareaFondo.mensaje(error), true);
        });
    }

    private void mostrarHojas(List<BufferedImage> imagenes) {
        hojas.getChildren().clear();
        vistasHojas.clear();
        for (int i = 0; i < imagenes.size(); i++) {
            Image imagen = SwingFXUtils.toFXImage(imagenes.get(i), null);
            ImageView vista = new ImageView(imagen);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            vista.setFitWidth(ANCHO_HOJA_PANTALLA * zoom.getValue());
            StackPane hoja = new StackPane(vista);
            hoja.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.1, 0, 3);");
            hoja.setMaxWidth(Region.USE_PREF_SIZE);
            Label numero = new Label("Hoja " + (i + 1) + " de " + imagenes.size());
            numero.getStyleClass().add("ayuda-diseno");
            VBox bloque = new VBox(6, numero, hoja);
            bloque.setAlignment(Pos.TOP_CENTER);
            hojas.getChildren().add(bloque);
            vistasHojas.add(vista);
        }
    }

    // ------------------------------------------------------------------ armado de la ventana

    private HBox armarEncabezado() {
        Label titulo = new Label("Informe de la orden #" + valorOGuion(detalle.getNumeroOrden()));
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label(valorOGuion(detalle.getPaciente())
                + "  ·  un solo informe con todos los estudios terminados de la orden");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(2, titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Label lblZoom = new Label("Tamaño en pantalla");
        lblZoom.getStyleClass().add("etiqueta-campo");
        zoom.setPrefWidth(150);
        zoom.valueProperty().addListener((o, a, n) -> {
            for (ImageView v : vistasHojas) {
                v.setFitWidth(ANCHO_HOJA_PANTALLA * n.doubleValue());
            }
        });
        HBox cajaZoom = new HBox(8, lblZoom, zoom);
        cajaZoom.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox(16, textos, espaciador, cajaZoom);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private ScrollPane armarVisor() {
        hojas.setAlignment(Pos.TOP_CENTER);
        hojas.setPadding(new Insets(16));
        cargando.setMaxSize(56, 56);
        areaHojas.getChildren().addAll(hojas, cargando);
        StackPane.setAlignment(cargando, Pos.TOP_CENTER);
        StackPane.setMargin(cargando, new Insets(60, 0, 0, 0));
        areaHojas.setStyle("-fx-background-color: #E4E8E6;");

        ScrollPane scroll = new ScrollPane(areaHojas);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("config-scroll");
        scroll.setStyle("-fx-background: #E4E8E6; -fx-background-color: #E4E8E6;");
        return scroll;
    }

    private VBox armarPanelDiseno() {
        Label titulo = new Label("Acomodar el informe");
        titulo.getStyleClass().add("titulo-seccion");
        Label ayuda = new Label("Lo que cambies acá se ve al instante en la hoja de la izquierda. "
                + "Nada se guarda hasta que toques \"Guardar diseño\" o \"Guardar PDF\".");
        ayuda.getStyleClass().add("ayuda-diseno");
        ayuda.setWrapText(true);

        etiquetaDisenoUsado.setWrapText(true);
        etiquetaDisenoUsado.setStyle("-fx-font-size: 12px; -fx-text-fill: #3A4440;");

        ToggleGroup alcance = new ToggleGroup();
        opcionSoloEsta = new RadioButton("Guardar solo para esta orden");
        opcionTodas = new RadioButton("Guardar para todas las órdenes (diseño predeterminado)");
        opcionTodas.setWrapText(true);
        opcionSoloEsta.setStyle("-fx-text-fill: #141C19; -fx-font-size: 12.5px;");
        opcionTodas.setStyle("-fx-text-fill: #141C19; -fx-font-size: 12.5px;");
        opcionSoloEsta.setToggleGroup(alcance);
        opcionTodas.setToggleGroup(alcance);
        opcionSoloEsta.setSelected(true);

        botonGuardarDiseno = new Button("Guardar diseño");
        botonGuardarDiseno.getStyleClass().add("boton-secundario");
        botonGuardarDiseno.setOnAction(e -> guardarDiseno(null));

        botonVolverPredeterminado = new Button("Usar el diseño predeterminado");
        botonVolverPredeterminado.getStyleClass().add("enlace-accion");
        botonVolverPredeterminado.setOnAction(e -> volverAPredeterminado());

        VBox cajaAlcance = new VBox(8, etiquetaDisenoUsado, opcionSoloEsta, opcionTodas,
                new HBox(8, botonGuardarDiseno, botonVolverPredeterminado));
        cajaAlcance.setPadding(new Insets(12));
        cajaAlcance.setStyle("-fx-background-color: #EEF4F0; -fx-background-radius: 12;");

        editor = new EditorDisenoPanel(DisenoInforme.deFabrica(), nuevo -> {
            disenoActual = nuevo;
            actualizarTextoDiseno();
            demora.playFromStart();
        });

        VBox panel = new VBox(14, titulo, ayuda, cajaAlcance, editor.getVista());
        panel.setPadding(new Insets(4, 12, 12, 4));
        return panel;
    }

    private VBox armarAbajo() {
        etiquetaEstado.setWrapText(true);
        etiquetaEstado.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(etiquetaEstado, Priority.ALWAYS);
        accionesPdf.setAlignment(Pos.CENTER_LEFT);
        HBox filaEstado = new HBox(10, etiquetaEstado, accionesPdf);
        filaEstado.setAlignment(Pos.CENTER_LEFT);

        Button botonCerrar = new Button("Cerrar");
        botonCerrar.getStyleClass().add("boton-secundario");
        botonCerrar.setOnAction(e -> {
            if (confirmarCierre()) {
                ventana.close();
            }
        });

        botonImprimir = new Button("🖨  Imprimir");
        botonImprimir.getStyleClass().add("boton-secundario");
        botonImprimir.setOnAction(e -> imprimir());

        botonEnviar = new Button("✉  Enviar al paciente...");
        botonEnviar.getStyleClass().add("boton-secundario");
        botonEnviar.setOnAction(e -> enviar());
        botonEnviar.setVisible(mostrarBotonEnviar);
        botonEnviar.setManaged(mostrarBotonEnviar);

        botonGuardarPdf = new Button("💾  Guardar PDF");
        botonGuardarPdf.getStyleClass().add("boton-primario");
        botonGuardarPdf.setOnAction(e -> guardarPdf());

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox botones = new HBox(10, botonCerrar, espaciador, botonImprimir, botonEnviar, botonGuardarPdf);
        botones.setAlignment(Pos.CENTER_RIGHT);

        return new VBox(10, filaEstado, botones);
    }

    // ------------------------------------------------------------------ acciones

    private boolean hayCambiosDeDiseno() {
        return disenoActual != null && disenoGuardado != null && !disenoActual.equals(disenoGuardado);
    }

    private boolean hayCambiosDeTextos() {
        String a = ajustesActual.aTexto();
        String b = ajustesGuardados.aTexto();
        return a == null ? b != null : !a.equals(b);
    }

    // ------------------------------------------------------------------ pestaña "Textos"

    private final Label etiquetaTextos = new Label();

    private VBox armarPanelTextos() {
        Label titulo = new Label("Textos del informe");
        titulo.getStyleClass().add("titulo-seccion");
        etiquetaTextos.setWrapText(true);
        etiquetaTextos.setStyle("-fx-font-size: 12px; -fx-text-fill: #3A4440;");
        Button guardar = new Button("Guardar textos");
        guardar.getStyleClass().add("boton-secundario");
        guardar.setOnAction(e -> guardarTextos(null));
        Button deshacer = new Button("Deshacer todos los cambios de texto");
        deshacer.getStyleClass().add("enlace-accion");
        deshacer.setOnAction(e -> {
            ajustesActual = new reportes.AjustesInforme();
            datos = ajustesActual.aplicar(datosOriginales);
            armarEditorTextos();
            actualizarTextoTextos();
            demora.playFromStart();
        });
        HBox botones = new HBox(8, guardar, deshacer);
        botones.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(8, etiquetaTextos, botones);
        cabecera.setPadding(new Insets(12));
        cabecera.setStyle("-fx-background-color: #EEF4F0; -fx-background-radius: 12;");
        contenedorTextos.getChildren().setAll(new Label("Cargando..."));
        VBox panel = new VBox(14, titulo, cabecera, contenedorTextos);
        panel.setPadding(new Insets(4, 12, 12, 4));
        return panel;
    }

    private void armarEditorTextos() {
        EditorTextosInformePanel editorTextos = new EditorTextosInformePanel(datosOriginales, ajustesActual, nuevos -> {
            ajustesActual = nuevos;
            datos = ajustesActual.aplicar(datosOriginales);
            actualizarTextoTextos();
            demora.playFromStart();
        }, this::pasarACatalogo);
        contenedorTextos.getChildren().setAll(editorTextos.getVista());
        actualizarTextoTextos();
    }

    private void actualizarTextoTextos() {
        etiquetaTextos.setText(hayCambiosDeTextos() ? "Hay cambios de texto sin guardar (\"Guardar PDF\" también los guarda)."
                : (ajustesGuardados.estaVacio() ? "Este informe usa los textos del Catálogo."
                : "Este informe tiene textos cambiados a mano (solo para esta orden)."));
    }

    private void guardarTextos(Runnable despues) {
        int idPedido = detalle.getIdPedido();
        reportes.AjustesInforme aGuardar = ajustesActual.copia();
        habilitar(false);
        mostrarEstado("Guardando los textos del informe...", false);
        TareaFondo.ejecutar(() -> {
            InformesPdfService.guardarAjustes(idPedido, aGuardar.estaVacio() ? null : aGuardar);
            return true;
        }, ok -> {
            ajustesGuardados = aGuardar;
            actualizarTextoTextos();
            habilitar(true);
            mostrarEstado("Textos del informe guardados.", false);
            if (despues != null) {
                despues.run();
            } else if (pdfGuardado != null) {
                guardarPdfAhora();
            }
        }, error -> {
            habilitar(true);
            mostrarEstado(TareaFondo.mensaje(error), true);
        });
    }

    /** "Pasar al Catálogo" de un renglón: lo guarda para todas las órdenes y vuelve a cargar el informe. */
    private void pasarACatalogo(int idAnalito, String nombre, String unidad, String referenciaCruda) {
        int idPedido = detalle.getIdPedido();
        habilitar(false);
        mostrarEstado("Guardando en el Catálogo...", false);
        reportes.AjustesInforme sinEseRenglon = ajustesActual.copia();
        for (ResultadosDatos.Estudio e : datosOriginales.getEstudios()) {
            sinEseRenglon.limpiarFila(e.getIdPedidoAnalisis(), idAnalito);
        }
        TareaFondo.ejecutar(() -> {
            InformesPdfService.guardarEnCatalogo(idAnalito, nombre, unidad, referenciaCruda);
            try {
                InformesPdfService.guardarAjustes(idPedido, sinEseRenglon.estaVacio() ? null : sinEseRenglon);
            } catch (java.sql.SQLException e) {
                // Sin el script SQL no hay textos guardados que limpiar.
            }
            return true;
        }, ok -> {
            vistas.javafx.ToastUtil.exito("Guardado en el Catálogo para todas las órdenes");
            cargar();
        }, error -> {
            habilitar(true);
            mostrarEstado("No se pudo guardar en el Catálogo: " + TareaFondo.mensaje(error), true);
        });
    }

    private void actualizarTextoDiseno() {
        String base = tieneDisenoPropio ? "Esta orden tiene su propio diseño." : "Esta orden usa el diseño predeterminado.";
        etiquetaDisenoUsado.setText(hayCambiosDeDiseno() ? base + "  (Hay cambios sin guardar.)" : base);
        botonVolverPredeterminado.setVisible(tieneDisenoPropio);
        botonVolverPredeterminado.setManaged(tieneDisenoPropio);
    }

    /**
     * Guarda el diseño que se está viendo, según lo elegido ("solo esta orden" / "todas"). Si se
     * pasa {@code despues}, lo ejecuta al terminar bien (por ejemplo, guardar el PDF).
     */
    private void guardarDiseno(Runnable despues) {
        DisenoInforme diseno = disenoActual.copia();
        int idPedido = detalle.getIdPedido();
        boolean paraTodas = opcionTodas.isSelected();
        habilitar(false);
        mostrarEstado("Guardando el diseño...", false);
        TareaFondo.ejecutar(() -> {
            if (paraTodas) {
                PreferenciasSistema.setDisenoInforme(diseno.aTexto());
                try {
                    InformesPdfService.guardarDisenoPropio(idPedido, null);
                } catch (java.sql.SQLException e) {
                    // Sin el script SQL no hay diseño propio que borrar: no pasa nada.
                }
            } else {
                InformesPdfService.guardarDisenoPropio(idPedido, diseno);
            }
            return paraTodas;
        }, todas -> {
            disenoGuardado = diseno;
            tieneDisenoPropio = !todas;
            actualizarTextoDiseno();
            habilitar(true);
            mostrarEstado(todas ? "Diseño guardado para todas las órdenes. Los PDF ya guardados se están actualizando solos."
                    : "Diseño guardado para esta orden.", false);
            if (todas) {
                TareaFondo.ejecutar(InformesPdfService::regenerarTodos, null, null);
            }
            if (despues != null) {
                despues.run();
            } else if (pdfGuardado != null) {
                // Ya tenía PDF: se actualiza para que quede igual a lo que se ve.
                guardarPdfAhora();
            }
        }, error -> {
            habilitar(true);
            mostrarEstado(TareaFondo.mensaje(error), true);
        });
    }

    private void volverAPredeterminado() {
        int idPedido = detalle.getIdPedido();
        habilitar(false);
        TareaFondo.ejecutar(() -> {
            InformesPdfService.guardarDisenoPropio(idPedido, null);
            return InformesPdfService.disenoPredeterminado();
        }, predeterminado -> {
            tieneDisenoPropio = false;
            disenoGuardado = predeterminado;
            disenoActual = predeterminado.copia();
            editor.setDiseno(disenoActual);
            actualizarTextoDiseno();
            habilitar(true);
            mostrarEstado("Esta orden vuelve a usar el diseño predeterminado.", false);
            redibujar();
            if (pdfGuardado != null) {
                guardarPdfAhora();
            }
        }, error -> {
            habilitar(true);
            mostrarEstado(TareaFondo.mensaje(error), true);
        });
    }

    /** "Guardar PDF": si hay cambios de diseño, primero los guarda (así el PDF y lo guardado coinciden). */
    private void guardarPdf() {
        guardarPendientesY(this::guardarPdfAhora);
    }

    /** Guarda primero lo que haya sin guardar (textos y/o diseño) y después hace {@code despues}. */
    private void guardarPendientesY(Runnable despues) {
        Runnable conDiseno = () -> {
            if (hayCambiosDeDiseno()) {
                guardarDiseno(despues);
            } else {
                despues.run();
            }
        };
        if (hayCambiosDeTextos()) {
            guardarTextos(conDiseno);
        } else {
            conDiseno.run();
        }
    }

    private void guardarPdfAhora() {
        if (datos == null) {
            return;
        }
        DisenoInforme diseno = disenoActual.copia();
        habilitar(false);
        mostrarEstado("Guardando el PDF...", false);
        TareaFondo.ejecutar(() -> InformesPdfService.guardarInforme(datos, diseno), archivo -> {
            pdfGuardado = archivo;
            habilitar(true);
            mostrarPdfGuardado(archivo, "PDF guardado.");
        }, error -> {
            habilitar(true);
            mostrarEstado("No se pudo guardar el PDF: " + TareaFondo.mensaje(error), true);
        });
    }

    private void imprimir() {
        if (documento == null) {
            return;
        }
        DocumentoMaquetado doc = documento;
        String nombre = "Informe - Orden " + valorOGuion(detalle.getNumeroOrden());
        SwingUtilities.invokeLater(() -> ImpresionUtil.imprimir(doc, nombre));
    }

    private void enviar() {
        Runnable abrir = () -> new EnviarResultadosDialog(detalle, null).mostrar();
        guardarPendientesY(abrir);
    }

    private boolean confirmarCierre() {
        if (!hayCambiosDeDiseno() && !hayCambiosDeTextos()) {
            return true;
        }
        ButtonType guardar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.YES);
        ButtonType descartar = new ButtonType("No guardar", ButtonBar.ButtonData.NO);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert pregunta = new Alert(Alert.AlertType.CONFIRMATION,
                "Cambiaste el informe (diseño o textos) y no lo guardaste. ¿Querés guardarlo antes de cerrar?",
                guardar, descartar, cancelar);
        pregunta.setHeaderText(null);
        pregunta.setTitle("Cambios sin guardar");
        vistas.javafx.AlertaUtil.estilizar(pregunta);
        Optional<ButtonType> r = pregunta.showAndWait();
        if (!r.isPresent() || r.get() == cancelar) {
            return false;
        }
        if (r.get() == guardar) {
            guardarPendientesY(() -> ventana.close());
            return false; // se cierra solo cuando termina de guardar
        }
        return true;
    }

    // ------------------------------------------------------------------ estado

    private void habilitar(boolean si) {
        boolean listo = si && datos != null;
        botonGuardarPdf.setDisable(!listo);
        botonImprimir.setDisable(!listo);
        botonEnviar.setDisable(!listo || datos.getEstudios().isEmpty());
        botonGuardarDiseno.setDisable(!listo);
        botonVolverPredeterminado.setDisable(!listo);
    }

    private void mostrarEstado(String texto, boolean error) {
        etiquetaEstado.setText(texto);
        etiquetaEstado.getStyleClass().removeAll("estado-pdf", "estado-pdf-error");
        etiquetaEstado.getStyleClass().add(error ? "estado-pdf-error" : "estado-pdf");
        accionesPdf.getChildren().clear();
    }

    private void mostrarPdfGuardado(File archivo, String prefijo) {
        mostrarEstado(prefijo + "  📄 " + archivo.getName() + "  (en " + archivo.getParentFile().getAbsolutePath() + ")", false);
        Button abrir = new Button("Abrir");
        abrir.getStyleClass().add("enlace-accion");
        abrir.setOnAction(e -> TareaFondo.ejecutar(() -> {
            ArchivosUtil.abrir(archivo);
            return null;
        }, null, error -> mostrarEstado("No se pudo abrir: " + TareaFondo.mensaje(error), true)));
        Button carpeta = new Button("Ver en la carpeta");
        carpeta.getStyleClass().add("enlace-accion");
        carpeta.setOnAction(e -> TareaFondo.ejecutar(() -> {
            ArchivosUtil.mostrarEnCarpeta(archivo);
            return null;
        }, null, error -> mostrarEstado("No se pudo abrir la carpeta: " + TareaFondo.mensaje(error), true)));
        accionesPdf.getChildren().setAll(abrir, carpeta);
    }

    private static String valorOGuion(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor;
    }
}
