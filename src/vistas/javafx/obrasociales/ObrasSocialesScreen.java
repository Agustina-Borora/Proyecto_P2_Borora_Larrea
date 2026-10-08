package vistas.javafx.obrasociales;

import java.util.Collections;
import java.util.List;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.ObraSocial;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla ABM de Obras Sociales, en JavaFX puro (sin FXML). Reemplaza al stub vacío que dejó el
 * editor de formularios de NetBeans en {@link vistas.formulariosPrincipales.ObraSocial}.
 */
public class ObrasSocialesScreen {

    private final ObservableList<ObraSocial> datos = FXCollections.observableArrayList();
    private final TableView<ObraSocial> tabla = new TableView<>();

    public Parent construir() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");

        raiz.setTop(armarEncabezado());

        TableView<ObraSocial> tablaArmada = armarTabla();
        raiz.setCenter(tablaArmada);
        BorderPane.setMargin(tablaArmada, new Insets(16, 0, 0, 0));

        cargarDatos();
        return raiz;
    }

    private HBox armarEncabezado() {
        VBox textos = new VBox(4);
        Label titulo = new Label("Obras Sociales");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Listado de obras sociales convenidas con el laboratorio");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        textos.getChildren().addAll(titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonNueva = new Button("+ Nueva Obra Social");
        botonNueva.getStyleClass().add("boton-primario");
        botonNueva.setOnAction(evt -> abrirFormulario(null));

        HBox fila = new HBox(12, textos, espaciador, botonNueva);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private TableView<ObraSocial> armarTabla() {
        TableColumn<ObraSocial, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colNombre.setPrefWidth(220);

        TableColumn<ObraSocial, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getCodigoInterno())));
        colCodigo.setPrefWidth(110);

        TableColumn<ObraSocial, String> colCuit = new TableColumn<>("CUIT");
        colCuit.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getCuit())));
        colCuit.setPrefWidth(130);

        TableColumn<ObraSocial, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getTelefono())));
        colTelefono.setPrefWidth(130);

        TableColumn<ObraSocial, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getEmail())));
        colEmail.setPrefWidth(200);

        TableColumn<ObraSocial, ObraSocial> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<ObraSocial, ObraSocial>() {
            @Override
            protected void updateItem(ObraSocial os, boolean vacio) {
                super.updateItem(os, vacio);
                if (vacio || os == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(os.isActiva() ? "Activa" : "Inactiva");
                pill.getStyleClass().add(os.isActiva() ? "pill-activo" : "pill-inactivo");
                setGraphic(pill);
            }
        });
        colEstado.setPrefWidth(90);

        TableColumn<ObraSocial, ObraSocial> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<ObraSocial, ObraSocial>() {
            private final Button botonEditar = new Button("Editar");
            private final Button botonBaja = new Button("Eliminar");
            private final HBox contenedor = new HBox(10, botonEditar, botonBaja);

            {
                botonEditar.getStyleClass().add("boton-fila");
                botonBaja.getStyleClass().addAll("boton-fila", "boton-fila-eliminar");
            }

            @Override
            protected void updateItem(ObraSocial os, boolean vacio) {
                super.updateItem(os, vacio);
                if (vacio || os == null) {
                    setGraphic(null);
                    return;
                }
                botonEditar.setOnAction(evt -> abrirFormulario(os));
                botonBaja.setOnAction(evt -> confirmarBaja(os));
                setGraphic(contenedor);
            }
        });
        // Mes 8 (rediseño visual): 150 ya no alcanzaba -- el padding nuevo de las celdas (ver
        // theme.css) le comía el espacio a "Editar" + "Eliminar" y quedaban cortados. 200 les deja
        // lugar de sobra (mismo ajuste en CatalogoExamenesScreen).
        colAcciones.setPrefWidth(200);
        // No resizable: evita que CONSTRAINED_RESIZE_POLICY la achique por debajo de su ancho
        // cuando la ventana queda angosta (ver el mismo ajuste en RegistrosScreen/UsuariosScreen).
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colNombre, colCodigo, colCuit, colTelefono, colEmail, colEstado, colAcciones);
        tabla.setItems(datos);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Todavía no hay obras sociales cargadas."));
        return tabla;
    }

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    /**
     * (Re)carga el listado completo desde la base de datos -- se llama al construir la pantalla
     * y después de cada alta/edición/baja para reflejar el cambio en la tabla.
     */
    private void cargarDatos() {
        List<ObraSocial> lista = PuenteEDT.ejecutar(
                () -> controlador.ObraSocialController.listarTodas(null),
                Collections.emptyList());
        datos.setAll(lista);
    }

    private void abrirFormulario(ObraSocial obraSocial) {
        new ObraSocialFormDialog(obraSocial, this::cargarDatos).mostrar();
    }

    private void confirmarBaja(ObraSocial obraSocial) {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Seguro que querés eliminar \"" + obraSocial.getNombre() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Eliminar obra social");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES) {
                boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.ObraSocialController.desactivar(null, obraSocial.getIdObraSocial()),
                        false);
                if (ok) {
                    cargarDatos();
                }
            }
        });
    }
}
