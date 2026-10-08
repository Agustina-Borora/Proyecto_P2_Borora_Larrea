package vistas.javafx.registrarResultados;

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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.OrdenResumen;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla para elegir, de las órdenes pendientes o en proceso, cuál pasar a cargar resultados --
 * en JavaFX puro (sin FXML), con el mismo look (tarjeta blanca, buscador redondeado, badges de
 * color por Cobertura y Estado) que ya usan {@link vistas.javafx.catalogo.CatalogoExamenesScreen}
 * y {@link vistas.javafx.pagos.PagosScreen}, siguiendo el diseño de Figma (nodo 246:1103,
 * "Registros") que era la referencia más cercana ya que este listado no tiene una pantalla propia
 * en el prototipo.
 *
 * <p>Reemplaza a {@link vistas.formulariosPrincipales.RegistrarResultados}, que mostraba
 * {@code vistas.registros.TablaRegistros} tal cual (columnas de texto plano, sin remarcar
 * urgencias ni estado) y separaba "elegir la fila" de "confirmar" en dos pasos (click en la fila +
 * botón "Siguiente" aparte, abajo de todo). Acá cada fila tiene directo su botón "Cargar
 * resultados": un solo clic, sin tener que adivinar que hay que seleccionar y después buscar un
 * botón en otro lado.</p>
 */
public class RegistrarResultadosScreen {

    /**
     * Se dispara cuando el usuario elige una orden (ya encontrado el paciente dueño) -- mismo
     * contrato que tenía {@code RegistrarResultados.OrdenParaResultadosListener}, para que
     * {@code AppShell} pueda conectarla exactamente igual con la carga de resultados.
     */
    public interface OrdenParaResultadosListener {
        void onOrdenSeleccionada(modelo.Paciente paciente, OrdenResumen orden);
    }

    private final ObservableList<OrdenResumen> ordenesVisibles = FXCollections.observableArrayList();
    private List<OrdenResumen> todasCargadas = Collections.emptyList();
    private controlador.PermisoController.PermisosExamenSesion permisosExamen;

    private final TableView<OrdenResumen> tabla = new TableView<>();
    private final TextField campoBusqueda = new TextField();
    private final Label etiquetaCantidad = new Label();

    private OrdenParaResultadosListener listener;

    public void setOrdenParaResultadosListener(OrdenParaResultadosListener listener) {
        this.listener = listener;
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

    private VBox armarEncabezado() {
        Label titulo = new Label("Registrar Resultados");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Elegí una orden pendiente para cargar sus resultados");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        return new VBox(4, titulo, subtitulo);
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
        colOrden.setPrefWidth(100);

        TableColumn<OrdenResumen, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getDni())));
        colDni.setPrefWidth(90);

        TableColumn<OrdenResumen, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getPaciente())));
        colPaciente.setPrefWidth(190);

        TableColumn<OrdenResumen, String> colExamen = new TableColumn<>("Examen");
        colExamen.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getExamen())));
        colExamen.setPrefWidth(210);

        TableColumn<OrdenResumen, String> colFecha = new TableColumn<>("Fecha");
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? formatoFecha.format(d.getValue().getFecha()) : "—"));
        colFecha.setPrefWidth(100);

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
        colCobertura.setPrefWidth(150);

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
        colEstado.setPrefWidth(110);

        TableColumn<OrdenResumen, OrdenResumen> colAcciones = new TableColumn<>("");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<OrdenResumen, OrdenResumen>() {
            private final Button boton = new Button("Cargar resultados");

            {
                boton.getStyleClass().add("boton-fila-primario");
            }

            @Override
            protected void updateItem(OrdenResumen orden, boolean vacio) {
                super.updateItem(orden, vacio);
                if (vacio || orden == null) {
                    setGraphic(null);
                    return;
                }
                // "Solo ver" entra igual a la carga (CargarResultadosScreen la deja de sólo
                // lectura), pero el botón lo aclara de entrada para no prometer algo que no va a
                // dejar hacer.
                boolean puedeCargar = permisosExamen == null || permisosExamen.puedeCargar(orden.getIdAnalisisTipo());
                boton.setText(puedeCargar ? "Cargar resultados" : "Ver resultados");
                boton.setOnAction(evt -> elegirOrden(orden));
                setGraphic(boton);
            }
        });
        // Mes 8 (rediseño visual): 160 volvió a quedarse corto -- el padding nuevo de las celdas
        // (ver ".table-row-cell .table-cell" en theme.css, para que las tablas no se sintieran
        // apretadas "a lo Swing") le comió el espacio de sobra que tenía este botón y "Cargar
        // resultados" volvió a cortarse ("Cargar result..."). 220 le deja lugar de sobra con el
        // padding nuevo.
        colAcciones.setPrefWidth(220);
        // No resizable: esta fue la causa ORIGINAL de que el botón apareciera cortado -- con
        // CONSTRAINED_RESIZE_POLICY, si la suma de las 8 columnas no entra en el ancho de la
        // ventana, la política achica columnas por debajo de su propio ancho para que todo
        // encaje, y el botón de texto largo de esta columna era el que más se notaba. Marcarla
        // no-resizable la saca del reparto: siempre mide exactamente lo que se le pidió, y son
        // las columnas de texto (Examen, Paciente) las que ceden espacio en su lugar (ver el
        // mismo ajuste en RegistrosScreen/UsuariosScreen/etc., 2026-10-01).
        colAcciones.setResizable(false);

        tabla.getColumns().setAll(colOrden, colDni, colPaciente, colExamen, colFecha, colCobertura, colEstado, colAcciones);
        tabla.setItems(ordenesVisibles);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay órdenes pendientes en este momento."));
        return tabla;
    }

    /**
     * "Urgente" en rojo (para que salte a la vista antes que nada), "Procesando" en azul,
     * cualquier otra cosa (siempre "Pendiente" acá, porque {@code RegistroController.listarPendientes}
     * ya filtra a solo estos tres) en ámbar -- misma paleta de "pill" que ya usa el resto de la
     * app (Catálogo de Exámenes, Pagos) en {@code theme.css}.
     */
    private static String claseEstado(String estado) {
        if (estado.startsWith("Urgente")) {
            return "pill-urgente";
        }
        if (estado.startsWith("Procesando")) {
            return "pill-procesando";
        }
        return "pill-pendiente";
    }

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    /**
     * Trae las órdenes pendientes y, de paso, los permisos por examen de la sesión actual -- las
     * órdenes de un examen "Sin acceso" ni siquiera se guardan en {@link #todasCargadas}, así que
     * no aparecen aunque se busquen por nombre (bloqueo real, no sólo un botón deshabilitado).
     */
    private void cargarDatos() {
        permisosExamen = PuenteEDT.ejecutar(
                () -> controlador.PermisoController.obtenerPermisosExamenSesion(null), null);
        List<OrdenResumen> pendientes = PuenteEDT.ejecutar(
                () -> controlador.RegistroController.listarPendientes(null),
                Collections.emptyList());
        todasCargadas = filtrarPorPermiso(pendientes);
        filtrar();
    }

    private List<OrdenResumen> filtrarPorPermiso(List<OrdenResumen> pendientes) {
        if (permisosExamen == null) {
            return pendientes;
        }
        List<OrdenResumen> resultado = new ArrayList<>();
        for (OrdenResumen orden : pendientes) {
            if (permisosExamen.puedeVer(orden.getIdAnalisisTipo())) {
                resultado.add(orden);
            }
        }
        return resultado;
    }

    /**
     * Filtra las órdenes ya traídas (sin volver a consultar la base) por DNI, paciente, número de
     * orden o examen -- mismo criterio que tenía {@code TablaRegistros.filtrar} -- y actualiza el
     * contador de "N órdenes pendientes" de la tarjeta.
     */
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
        etiquetaCantidad.setText(resultado.size() + (resultado.size() == 1 ? " orden pendiente" : " órdenes pendientes"));
    }

    /**
     * Busca al paciente dueño de la orden (por DNI, igual que hacía el viejo botón "Siguiente" de
     * {@link vistas.formulariosPrincipales.RegistrarResultados}) y, si lo encuentra, avisa al
     * {@link OrdenParaResultadosListener} para que abra la carga de resultados. La búsqueda usa un
     * Controller existente que muestra un {@code JOptionPane} si falla al hablar con la base, así
     * que corre entera en el Event Dispatch Thread de Swing (mismo criterio que
     * {@code EscritorioScreen#editarPacienteDeOrden}) en vez de con {@link PuenteEDT} -- acá no
     * hace falta esperar ningún resultado de vuelta en el hilo de JavaFX, alcanza con avisar al
     * listener ya desde el EDT (que es quien termina de decidir, del lado de {@code AppShell}, si
     * hace falta volver al hilo de JavaFX para algo).
     */
    private void elegirOrden(OrdenResumen orden) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            modelo.Paciente paciente = controlador.PacienteController.buscarPorDni(null, orden.getDni());
            if (paciente == null) {
                return;
            }
            if (listener != null) {
                listener.onOrdenSeleccionada(paciente, orden);
            }
        });
    }
}
