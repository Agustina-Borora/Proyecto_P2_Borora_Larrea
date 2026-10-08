package vistas.javafx.registros;

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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.DetalleOrden;
import modelo.OrdenResumen;
import vistas.javafx.PuenteEDT;
import vistas.javafx.TareaFondo;
import vistas.javafx.escritorio.DetalleOrdenDialog;
import vistas.javafx.menu.IconoFx;

/**
 * Pantalla "Registros de Análisis" (Figma: nodo 246:1103), en JavaFX puro -- reemplaza a
 * {@code vistas.formulariosPrincipales.Registros} + {@code vistas.registros.TablaRegistros}, cuyas
 * columnas "Ver"/"Editar"/acciones eran sólo un {@code System.out.println} sin ninguna
 * funcionalidad real detrás. Acá:
 *
 * <ul>
 *   <li>"Ver" reutiliza {@link DetalleOrdenDialog}, el mismo popup que ya usa Escritorio.</li>
 *   <li>"Editar" abre {@link EditarOrdenDialog}, nuevo, con más campos que el viejo
 *       {@code EditarPaciente} (médico derivante, observación y estado, además del paciente).</li>
 *   <li>"Enviar"/"Reenviar" (solo en órdenes completadas) abre {@link EnviarResultadosDialog},
 *       que manda el resultado por email y/o WhatsApp de verdad.</li>
 * </ul>
 */
public class RegistrosScreen {

    private final ObservableList<OrdenResumen> ordenesVisibles = FXCollections.observableArrayList();
    private List<OrdenResumen> todasCargadas = Collections.emptyList();

    private final TableView<OrdenResumen> tabla = new TableView<>();
    private final TextField campoBusqueda = new TextField();
    private final Label etiquetaCantidad = new Label();

    private Runnable alNuevoAnalisis;
    private Runnable alCargarResultados;

    /**
     * Se dispara al presionar "+ Nuevo Análisis" -- {@code AppShell} lo conecta para navegar a
     * la pantalla "3" (Nuevo Análisis), igual que hace el sidebar.
     */
    public void setAlNuevoAnalisis(Runnable listener) {
        this.alNuevoAnalisis = listener;
    }

    /**
     * Se dispara al presionar "Cargar Resultados" adentro del popup "Ver" de una orden todavía no
     * completada -- {@code AppShell} lo conecta para navegar a la pantalla "4" (Registrar
     * Resultados), mismo criterio que {@code EscritorioScreen#setAlCargarResultados}.
     */
    public void setAlCargarResultados(Runnable listener) {
        this.alCargarResultados = listener;
    }

    public Parent construir() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");

        VBox encabezado = new VBox(16, armarEncabezado(), armarBarraBusqueda());
        raiz.setTop(encabezado);

        VBox tarjeta = armarTarjeta();
        raiz.setCenter(tarjeta);
        BorderPane.setMargin(tarjeta, new Insets(16, 0, 0, 0));

        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        Label titulo = new Label("Registros");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Todas las órdenes de análisis del laboratorio");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(4, titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonNuevo = new Button("+ Nuevo Análisis");
        botonNuevo.getStyleClass().add("boton-primario");
        botonNuevo.setOnAction(evt -> {
            if (alNuevoAnalisis != null) {
                alNuevoAnalisis.run();
            }
        });

        HBox fila = new HBox(12, textos, espaciador, botonNuevo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private HBox armarBarraBusqueda() {
        campoBusqueda.getStyleClass().add("campo-form");
        campoBusqueda.setPromptText("Buscar por DNI, paciente, número de orden o examen...");
        campoBusqueda.setPrefWidth(360);
        campoBusqueda.setOnKeyReleased(evt -> filtrar());
        campoBusqueda.setOnAction(evt -> filtrar());

        HBox fila = new HBox(0, campoBusqueda);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox armarTarjeta() {
        etiquetaCantidad.getStyleClass().add("titulo-seccion");

        TableView<OrdenResumen> tablaArmada = armarTabla();
        VBox.setVgrow(tablaArmada, Priority.ALWAYS);

        VBox tarjeta = new VBox(14, etiquetaCantidad, tablaArmada);
        tarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private TableView<OrdenResumen> armarTabla() {
        TableColumn<OrdenResumen, String> colOrden = new TableColumn<>("Orden");
        colOrden.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getNumeroOrden())));
        colOrden.setPrefWidth(90);
        colOrden.setMinWidth(70);

        TableColumn<OrdenResumen, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(180);
        colPaciente.setMinWidth(110);

        TableColumn<OrdenResumen, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getDni())));
        colDni.setPrefWidth(90);
        colDni.setMinWidth(70);

        TableColumn<OrdenResumen, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getExamen())));
        colExamen.setPrefWidth(190);
        colExamen.setMinWidth(120);

        TableColumn<OrdenResumen, String> colFecha = new TableColumn<>("Fecha");
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? formatoFecha.format(d.getValue().getFecha()) : "—"));
        colFecha.setPrefWidth(95);
        colFecha.setMinWidth(85);

        TableColumn<OrdenResumen, OrdenResumen> colCobertura = new TableColumn<>("Cobertura");
        colCobertura.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colCobertura.setCellFactory(col -> new TableCell<OrdenResumen, OrdenResumen>() {
            @Override
            protected void updateItem(OrdenResumen orden, boolean vacio) {
                super.updateItem(orden, vacio);
                if (vacio || orden == null) {
                    setGraphic(null);
                    return;
                }
                boolean particular = orden.getCobertura() == null || "Particular".equalsIgnoreCase(orden.getCobertura());
                Label pill = new Label(particular ? "Particular" : orden.getCobertura());
                pill.getStyleClass().add(particular ? "pill-particular" : "pill-obra-social");
                setGraphic(pill);
            }
        });
        colCobertura.setPrefWidth(140);
        colCobertura.setMinWidth(100);

        TableColumn<OrdenResumen, OrdenResumen> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
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
        colEstado.setPrefWidth(150);
        colEstado.setMinWidth(110);

        TableColumn<OrdenResumen, OrdenResumen> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<OrdenResumen, OrdenResumen>() {
            private final Button botonVer = new Button();
            private final Button botonEditar = new Button();
            private final MenuItem itemVistaPrevia = new MenuItem("🔍  Ver y acomodar el informe (toda la orden)");
            private final MenuItem itemGuardarPdf = new MenuItem("💾  Guardar PDF del informe");
            private final MenuItem itemAbrirPdf = new MenuItem("📄  Abrir el PDF guardado");
            private final MenuItem itemImprimir = new MenuItem("🖨  Imprimir informe");
            private final MenuItem itemEnviar = new MenuItem();
            private final MenuButton botonMas = new MenuButton("⋮");
            private final HBox caja = new HBox(6, botonVer, botonEditar, botonMas);

            /**
             * Ver/Editar (ícono solo, con {@link Tooltip} para que se entienda qué hace cada uno
             * sin tener que adivinarlo por el dibujito) siempre visibles; el resto de las acciones
             * de una orden completada -- vista previa, imprimir, enviar -- quedan agrupadas en un
             * menú "⋮ Más acciones", cada una con su nombre completo en vez de solo un ícono.
             * Antes estaban las 5 como botones sueltos en la fila: además de ser más difícil de
             * entender a simple vista qué hacía cada ícono (pedido de Agus), en una ventana angosta
             * se apretaban tanto entre sí que se llegaban a cortar -- con un solo botón agrupador
             * de ancho fijo, la fila nunca necesita más espacio aunque se agreguen más acciones al
             * menú en el futuro.
             */
            {
                botonVer.setGraphic(IconoFx.vista("ojo", 16));
                botonVer.getStyleClass().add("boton-fila-icono");
                botonVer.setTooltip(new Tooltip("Ver el detalle de la orden"));

                botonEditar.setGraphic(IconoFx.vista("lapiz", 16));
                botonEditar.getStyleClass().add("boton-fila-icono");
                botonEditar.setTooltip(new Tooltip("Editar paciente, médico derivante u observación"));

                botonMas.getStyleClass().add("boton-fila-icono");
                botonMas.setTooltip(new Tooltip("Informe de la orden: ver, guardar en PDF, imprimir o enviar"));
                botonMas.getItems().addAll(itemVistaPrevia, itemGuardarPdf, itemAbrirPdf, itemImprimir,
                        new SeparatorMenuItem(), itemEnviar);

                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OrdenResumen orden, boolean vacio) {
                super.updateItem(orden, vacio);
                if (vacio || orden == null) {
                    setGraphic(null);
                    return;
                }
                botonVer.setOnAction(evt -> verOrden(orden));
                botonEditar.setOnAction(evt -> editarOrden(orden));
                itemVistaPrevia.setOnAction(evt -> vistaPreviaOrden(orden));
                itemGuardarPdf.setOnAction(evt -> guardarPdfOrden(orden));
                itemAbrirPdf.setOnAction(evt -> abrirPdfOrden(orden));
                itemImprimir.setOnAction(evt -> imprimirOrden(orden));

                String estado = orden.getEstado() == null ? "" : orden.getEstado();
                boolean completado = estado.startsWith("Completado");

                if (completado) {
                    itemEnviar.setText(estado.contains("Sin enviar") ? "✉  Enviar resultados" : "✉  Reenviar resultados");
                    itemEnviar.setOnAction(evt -> enviarOrden(orden));
                    if (!caja.getChildren().contains(botonMas)) {
                        caja.getChildren().add(botonMas);
                    }
                } else {
                    caja.getChildren().remove(botonMas);
                }
                setGraphic(caja);
            }
        });
        colAcciones.setPrefWidth(140);
        colAcciones.setMinWidth(130);
        // No resizable: con CONSTRAINED_RESIZE_POLICY, si la suma de todas las columnas no entra
        // en el ancho de la ventana, la política achica columnas por debajo de su propio mínimo
        // para que todo encaje -- a una columna de texto eso la deja con elipsis (fea, pero
        // legible), pero a esta la deja con los botones amontonados o cortados, imposibles de
        // clickear bien. Marcarla no-resizable la saca del reparto: siempre mide exactamente lo
        // que se le pidió, y son las columnas de texto las que ceden espacio en su lugar.
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colOrden, colPaciente, colDni, colExamen, colFecha, colCobertura, colEstado, colAcciones);
        tabla.setItems(ordenesVisibles);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay registros cargados todavía."));
        return tabla;
    }

    /**
     * Misma paleta de "pill" que ya usa el resto de la app -- acá con dos casos más que
     * {@code RegistrarResultadosScreen} (que sólo ve pendientes/procesando/urgente) porque Registros
     * lista TODAS las órdenes: "Completado · ..." en verde (reutilizando {@code pill-activo}, el
     * mismo verde de "Activo" en otras pantallas) y "Cancelado" en gris (reutilizando
     * {@code pill-inactivo}).
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

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    private void cargarDatos() {
        todasCargadas = PuenteEDT.ejecutar(
                () -> controlador.RegistroController.listarTodos(null),
                Collections.emptyList());
        filtrar();
    }

    private void filtrar() {
        String texto = campoBusqueda.getText();
        List<OrdenResumen> resultado;
        if (texto == null || texto.trim().isEmpty()) {
            resultado = todasCargadas;
        } else {
            String buscado = texto.trim().toLowerCase(Locale.getDefault());
            resultado = new ArrayList<>();
            for (OrdenResumen o : todasCargadas) {
                boolean coincide =
                        (o.getDni() != null && o.getDni().toLowerCase(Locale.getDefault()).contains(buscado))
                        || (o.getPaciente() != null && o.getPaciente().toLowerCase(Locale.getDefault()).contains(buscado))
                        || (o.getNumeroOrden() != null && o.getNumeroOrden().toLowerCase(Locale.getDefault()).contains(buscado))
                        || (o.getExamen() != null && o.getExamen().toLowerCase(Locale.getDefault()).contains(buscado));
                if (coincide) {
                    resultado.add(o);
                }
            }
        }
        ordenesVisibles.setAll(resultado);
        etiquetaCantidad.setText("Todos los registros (" + resultado.size() + ")");
    }

    /**
     * Trae el {@link DetalleOrden} completo de una fila (la tabla sólo tiene lo mínimo para
     * listar, ver {@link OrdenResumen}) -- lo comparten "Ver", "Editar", el informe y "Enviar".
     *
     * <p>Se busca en segundo plano (ver {@link TareaFondo}) y recién cuando llega se abre lo que
     * corresponda: así la tabla nunca se queda congelada esperando a la base de datos.</p>
     */
    private void conDetalle(OrdenResumen orden, java.util.function.Consumer<DetalleOrden> accion) {
        tabla.setCursor(javafx.scene.Cursor.WAIT);
        TareaFondo.ejecutar(() -> {
            try (java.sql.Connection con = conexiones.Conexion.conectar()) {
                if (con == null) {
                    throw new java.sql.SQLException("No se pudo conectar a la base de datos.");
                }
                DetalleOrden d = dao.EscritorioDAO.buscarDetalleOrden(con, orden.getIdPedidoAnalisis());
                if (d == null) {
                    throw new java.sql.SQLException("No se encontró la orden.");
                }
                return d;
            }
        }, detalle -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            accion.accept(detalle);
        }, error -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            mostrarError("No se pudo abrir la orden: " + TareaFondo.mensaje(error));
        });
    }

    private void verOrden(OrdenResumen orden) {
        conDetalle(orden, detalle -> new DetalleOrdenDialog(detalle, alCargarResultados).mostrar());
    }

    private void editarOrden(OrdenResumen orden) {
        conDetalle(orden, detalle -> new EditarOrdenDialog(detalle, this::cargarDatos).mostrar());
    }

    private void enviarOrden(OrdenResumen orden) {
        conDetalle(orden, detalle -> new EnviarResultadosDialog(detalle, this::cargarDatos).mostrar());
    }

    /**
     * "Ver y acomodar el informe": abre {@link VistaPreviaInformeDialog} con el informe de TODA la
     * orden (todos sus estudios terminados juntos), donde además se puede cambiar el diseño,
     * guardar el PDF, imprimirlo o mandarlo.
     */
    private void vistaPreviaOrden(OrdenResumen orden) {
        conDetalle(orden, detalle -> new VistaPreviaInformeDialog(detalle, this::cargarDatos).mostrar());
    }

    /** "Guardar PDF del informe": lo guarda solo en la carpeta de informes, sin ningún cartel de Windows. */
    private void guardarPdfOrden(OrdenResumen orden) {
        tabla.setCursor(javafx.scene.Cursor.WAIT);
        TareaFondo.ejecutar(() -> {
            int idPedido = reportes.InformesPdfService.idPedidoDe(orden.getIdPedidoAnalisis());
            return reportes.InformesPdfService.guardarInforme(idPedido);
        }, archivo -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            avisoPdf(archivo, "Listo, el PDF del informe quedó guardado.");
        }, error -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            mostrarError("No se pudo guardar el PDF: " + TareaFondo.mensaje(error));
        });
    }

    /** "Abrir el PDF guardado": lo abre si ya existe; si no, ofrece guardarlo. */
    private void abrirPdfOrden(OrdenResumen orden) {
        TareaFondo.ejecutar(() -> {
            int idPedido = reportes.InformesPdfService.idPedidoDe(orden.getIdPedidoAnalisis());
            java.io.File existente = reportes.InformesPdfService.pdfExistente(idPedido);
            if (existente != null) {
                utilidades.ArchivosUtil.abrir(existente);
            }
            return existente != null;
        }, abierto -> {
            if (!abierto) {
                Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                        "Esta orden todavía no tiene su PDF guardado. ¿Querés guardarlo ahora?",
                        ButtonType.YES, ButtonType.NO);
                aviso.setHeaderText(null);
                aviso.setTitle("PDF del informe");
                vistas.javafx.AlertaUtil.estilizar(aviso);
                aviso.showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        guardarPdfOrden(orden);
                    }
                });
            }
        }, error -> mostrarError("No se pudo abrir el PDF: " + TareaFondo.mensaje(error)));
    }

    /**
     * "Imprimir informe": arma el informe de toda la orden (con su diseño) y abre el cartel de
     * impresión para elegir la impresora. Para tenerlo en PDF ya no hace falta "Microsoft Print
     * to PDF": está "Guardar PDF del informe".
     */
    private void imprimirOrden(OrdenResumen orden) {
        tabla.setCursor(javafx.scene.Cursor.WAIT);
        TareaFondo.ejecutar(() -> {
            int idPedido = reportes.InformesPdfService.idPedidoDe(orden.getIdPedidoAnalisis());
            reportes.ResultadosDatos datos = reportes.InformesPdfService.cargarDatos(idPedido);
            return reportes.InformesPdfService.maquetar(datos, reportes.InformesPdfService.disenoParaPedido(idPedido));
        }, documento -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            String nombre = "Informe - Orden " + (orden.getNumeroOrden() == null ? "" : orden.getNumeroOrden());
            javax.swing.SwingUtilities.invokeLater(() -> reportes.ImpresionUtil.imprimir(documento, nombre));
        }, error -> {
            tabla.setCursor(javafx.scene.Cursor.DEFAULT);
            mostrarError("No se pudo preparar la impresión: " + TareaFondo.mensaje(error));
        });
    }

    /** Cartelito con dónde quedó el PDF y botones para abrirlo o verlo en su carpeta. */
    public static void avisoPdf(java.io.File archivo, String titulo) {
        ButtonType abrir = new ButtonType("Abrir PDF", ButtonBar.ButtonData.YES);
        ButtonType carpeta = new ButtonType("Ver en la carpeta", ButtonBar.ButtonData.OTHER);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert aviso = new Alert(Alert.AlertType.INFORMATION,
                titulo + "\n\n" + archivo.getName() + "\nen: " + archivo.getParentFile().getAbsolutePath(),
                abrir, carpeta, cerrar);
        aviso.setHeaderText(null);
        aviso.setTitle("PDF del informe");
        vistas.javafx.AlertaUtil.estilizar(aviso);
        aviso.showAndWait().ifPresent(b -> {
            if (b == abrir || b == carpeta) {
                TareaFondo.ejecutar(() -> {
                    if (b == abrir) {
                        utilidades.ArchivosUtil.abrir(archivo);
                    } else {
                        utilidades.ArchivosUtil.mostrarEnCarpeta(archivo);
                    }
                    return null;
                }, null, error -> mostrarError(TareaFondo.mensaje(error)));
            }
        });
    }

    private static void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.setHeaderText(null);
        alerta.setTitle("Registros");
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }
}
