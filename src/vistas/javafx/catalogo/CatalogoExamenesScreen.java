package vistas.javafx.catalogo;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.AnalisisTipo;
import modelo.ValorUb;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla ABM de Catálogo de Exámenes, en JavaFX puro (sin FXML). Reemplaza a
 * {@link vistas.formulariosPrincipales.CatalogoExamenes}, que traía la tabla sin conectar a la
 * base (Ver/Editar/Eliminar sólo hacían un {@code System.out.println}).
 */
public class CatalogoExamenesScreen {

    private final ObservableList<AnalisisTipo> datosVisibles = FXCollections.observableArrayList();
    private List<AnalisisTipo> todosCargados = Collections.emptyList();

    private final TableView<AnalisisTipo> tabla = new TableView<>();
    private final TextField campoBusqueda = new TextField();
    private final Label etiquetaValorUb = new Label();

    public Parent construir() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");

        VBox encabezado = new VBox(16, armarEncabezado(), armarBarraValorUb(), armarBarraBusqueda());
        raiz.setTop(encabezado);

        TableView<AnalisisTipo> tablaArmada = armarTabla();
        raiz.setCenter(tablaArmada);
        BorderPane.setMargin(tablaArmada, new Insets(16, 0, 0, 0));

        cargarValorUb();
        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        VBox textos = new VBox(4);
        Label titulo = new Label("Catálogo de Exámenes");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Todos los exámenes disponibles y sus unidades bioquímicas");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        textos.getChildren().addAll(titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonNuevo = new Button("+ Nuevo Examen");
        botonNuevo.getStyleClass().add("boton-primario");
        botonNuevo.setOnAction(evt -> abrirFormulario(null));

        HBox fila = new HBox(12, textos, espaciador, botonNuevo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private HBox armarBarraBusqueda() {
        campoBusqueda.getStyleClass().add("campo-form");
        campoBusqueda.setPromptText("Buscar por nombre, código o categoría...");
        campoBusqueda.setPrefWidth(320);
        campoBusqueda.setOnKeyReleased(evt -> filtrar());
        campoBusqueda.setOnAction(evt -> filtrar());

        HBox fila = new HBox(0, campoBusqueda);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private HBox armarBarraValorUb() {
        etiquetaValorUb.getStyleClass().add("subtitulo-pantalla");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonActualizar = new Button("Actualizar valor UB");
        botonActualizar.getStyleClass().add("boton-secundario");
        botonActualizar.setOnAction(evt -> new ValorUbFormDialog(this::alActualizarValorUb).mostrar());

        HBox fila = new HBox(12, etiquetaValorUb, espaciador, botonActualizar);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /**
     * (Re)carga el valor vigente de la UB y lo muestra en {@code etiquetaValorUb} -- se llama al
     * construir la pantalla y después de cargar un valor nuevo.
     */
    private void cargarValorUb() {
        ValorUb vigente = PuenteEDT.ejecutar(
                () -> controlador.ValorUbController.obtenerVigenteCompleto(null),
                null);
        if (vigente == null) {
            etiquetaValorUb.setText("Valor de la UB: todavía no hay ninguno cargado.");
            return;
        }
        etiquetaValorUb.setText("Valor de la UB vigente: $" + vigente.getValor().toPlainString()
                + " (desde " + formatearFecha(vigente.getVigenteDesde()) + ")");
    }

    /**
     * Se ejecuta después de cargar un valor de UB nuevo desde {@link ValorUbFormDialog} --
     * refresca tanto la etiqueta como los precios de la tabla, que dependen del valor de la UB.
     */
    private void alActualizarValorUb() {
        cargarValorUb();
        cargarDatos();
    }

    private static String formatearFecha(java.time.LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private TableView<AnalisisTipo> armarTabla() {
        TableColumn<AnalisisTipo, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCodigoAnalisis() == null ? "—" : String.valueOf(d.getValue().getCodigoAnalisis())));
        colCodigo.setPrefWidth(90);

        TableColumn<AnalisisTipo, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreAnalisis()));
        colNombre.setPrefWidth(260);

        TableColumn<AnalisisTipo, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCategoria() == null || d.getValue().getCategoria().trim().isEmpty()
                        ? "—" : d.getValue().getCategoria()));
        colCategoria.setPrefWidth(140);

        TableColumn<AnalisisTipo, String> colUnidades = new TableColumn<>("Unidad Bioquímica");
        colUnidades.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                formatearDecimal(d.getValue().getUnidadesBioquimicas())));
        colUnidades.setPrefWidth(150);

        TableColumn<AnalisisTipo, String> colPrecio = new TableColumn<>("Precio estimado");
        colPrecio.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                formatearPrecio(d.getValue().getPrecioEstimado())));
        colPrecio.setPrefWidth(130);

        TableColumn<AnalisisTipo, AnalisisTipo> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<AnalisisTipo, AnalisisTipo>() {
            @Override
            protected void updateItem(AnalisisTipo tipo, boolean vacio) {
                super.updateItem(tipo, vacio);
                if (vacio || tipo == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(tipo.isActivo() ? "Activo" : "Inactivo");
                pill.getStyleClass().add(tipo.isActivo() ? "pill-activo" : "pill-inactivo");
                setGraphic(pill);
            }
        });
        colEstado.setPrefWidth(90);

        TableColumn<AnalisisTipo, AnalisisTipo> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<AnalisisTipo, AnalisisTipo>() {
            private final Button botonEditar = new Button("Editar");
            private final Button botonBaja = new Button("Eliminar");
            private final HBox contenedor = new HBox(10, botonEditar, botonBaja);

            {
                botonEditar.getStyleClass().add("boton-fila");
                botonBaja.getStyleClass().addAll("boton-fila", "boton-fila-eliminar");
            }

            @Override
            protected void updateItem(AnalisisTipo tipo, boolean vacio) {
                super.updateItem(tipo, vacio);
                if (vacio || tipo == null) {
                    setGraphic(null);
                    return;
                }
                botonEditar.setOnAction(evt -> abrirFormulario(tipo));
                botonBaja.setOnAction(evt -> confirmarBaja(tipo));
                setGraphic(contenedor);
            }
        });
        // Mes 8 (rediseño visual): 150 ya no alcanzaba -- desde que las celdas de toda la app
        // tienen más padding (.table-row-cell .table-cell en theme.css, para que no se sientan
        // apretadas "a lo Swing"), ese mismo aire de más le comía el espacio a los dos botones de
        // texto ("Editar" + "Eliminar") y quedaban cortados ("Edit..." / "Elimin..."). 200 les deja
        // lugar de sobra a los dos con el padding nuevo.
        colAcciones.setPrefWidth(200);
        // No resizable: evita que CONSTRAINED_RESIZE_POLICY la achique por debajo de su ancho
        // cuando la ventana queda angosta (ver el mismo ajuste en RegistrosScreen/UsuariosScreen).
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colCodigo, colNombre, colCategoria, colUnidades, colPrecio, colEstado, colAcciones);
        tabla.setItems(datosVisibles);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Todavía no hay exámenes cargados."));
        return tabla;
    }

    private static String formatearDecimal(BigDecimal valor) {
        return valor == null ? "—" : valor.stripTrailingZeros().toPlainString();
    }

    private static String formatearPrecio(BigDecimal valor) {
        return valor == null ? "—" : "$" + String.format(Locale.US, "%,.2f", valor);
    }

    /**
     * (Re)carga el listado completo desde la base de datos -- se llama al construir la pantalla
     * y después de cada alta/edición/baja para reflejar el cambio en la tabla.
     */
    private void cargarDatos() {
        todosCargados = PuenteEDT.ejecutar(
                () -> controlador.AnalisisTipoController.listarTodosParaAbm(null),
                Collections.emptyList());
        filtrar();
    }

    /**
     * Filtra {@code todosCargados} por nombre, código o categoría con el texto de
     * {@code campoBusqueda} y refleja el resultado en la tabla -- se llama al cargar datos
     * nuevos y cada vez que cambia la búsqueda.
     */
    private void filtrar() {
        String texto = campoBusqueda.getText();
        if (texto == null || texto.trim().isEmpty()) {
            datosVisibles.setAll(todosCargados);
            return;
        }
        String buscado = texto.trim().toLowerCase(Locale.getDefault());
        List<AnalisisTipo> filtrados = new ArrayList<>();
        for (AnalisisTipo tipo : todosCargados) {
            boolean coincideNombre = tipo.getNombreAnalisis() != null
                    && tipo.getNombreAnalisis().toLowerCase(Locale.getDefault()).contains(buscado);
            boolean coincideCodigo = tipo.getCodigoAnalisis() != null
                    && String.valueOf(tipo.getCodigoAnalisis()).contains(buscado);
            boolean coincideCategoria = tipo.getCategoria() != null
                    && tipo.getCategoria().toLowerCase(Locale.getDefault()).contains(buscado);
            if (coincideNombre || coincideCodigo || coincideCategoria) {
                filtrados.add(tipo);
            }
        }
        datosVisibles.setAll(filtrados);
    }

    private void abrirFormulario(AnalisisTipo tipo) {
        new ExamenFormDialog(tipo, this::cargarDatos).mostrar();
    }

    private void confirmarBaja(AnalisisTipo tipo) {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Seguro que querés dar de baja \"" + tipo.getNombreAnalisis() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Eliminar examen");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES) {
                boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.AnalisisTipoController.cambiarActivo(null, tipo.getIdAnalisisTipo(), false),
                        false);
                if (ok) {
                    cargarDatos();
                }
            }
        });
    }
}
