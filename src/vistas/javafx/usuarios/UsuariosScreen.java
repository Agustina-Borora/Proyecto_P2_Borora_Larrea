package vistas.javafx.usuarios;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import modelo.Usuario;
import vistas.javafx.PuenteEDT;
import vistas.javafx.menu.IconoFx;

/**
 * Pantalla "Usuarios" (sección Administración), en JavaFX puro -- reemplaza a
 * {@code vistas.formulariosPrincipales.Usuarios} + {@code vista.usuarios.TablaUsuarios} +
 * {@code vista.usuarios.CardUsuarios} (Swing), que seguían con el aspecto de antes de pasar el
 * resto del sistema a JavaFX (pedido explícito de Agus). El formulario de alta/edición/detalle
 * vive en {@link UsuarioFormDialog} -- ver su javadoc para qué cambió de comportamiento además del
 * aspecto visual (el más importante: "+ Nuevo Usuario" antes no guardaba nada).
 */
public class UsuariosScreen {

    private final ObservableList<Usuario> usuariosVisibles = FXCollections.observableArrayList();

    /**
     * Copia completa (sin filtrar) de lo que trajo la base, para poder volver a filtrar por texto
     * ({@link #filtrarPorTexto}) sin volver a consultarla -- mismo criterio que
     * {@code PagosScreen#todasLasOrdenes}.
     */
    private final List<Usuario> todosLosUsuarios = new ArrayList<>();

    private final Label valorTotal = new Label("0");
    private final Label valorAdministradores = new Label("0");
    private final Label valorTecnicos = new Label("0");

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
        Label titulo = new Label("Usuarios");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Cuentas y permisos del sistema");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        VBox textos = new VBox(4, titulo, subtitulo);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonNuevo = new Button("+ Nuevo Usuario");
        botonNuevo.getStyleClass().add("boton-primario");
        botonNuevo.setOnAction(evt ->
                new UsuarioFormDialog(UsuarioFormDialog.Modo.NUEVO, null, this::cargarDatos).mostrar());

        HBox fila = new HBox(12, textos, espaciador, botonNuevo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /**
     * Mes 8 (rediseño visual): mismo lenguaje visual que ya aprobó la clienta para el Escritorio
     * (ícono + franja de color + efecto hover) en vez de las cajas de color liso de antes -- ver
     * {@link vistas.javafx.TarjetaStatUtil}.
     */
    private HBox armarTarjetasResumen() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir(
                        "Total de Usuarios", valorTotal, "Cuentas cargadas en el sistema", "azul", "👥"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Administradores", valorAdministradores, "Acceso completo al sistema", "verde", "🔑"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Tecnicos", valorTecnicos, "Acceso operativo", "ambar", "🔧"));
    }

    private VBox armarTarjetaTabla() {
        Label titulo = new Label("Listado");
        titulo.getStyleClass().add("titulo-seccion");

        TableView<Usuario> tabla = armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox tarjeta = new VBox(14, titulo, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private TableView<Usuario> armarTabla() {
        TableColumn<Usuario, String> colNombre = new TableColumn<>("Apellido y Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getApellidoYNombre())));
        colNombre.setPrefWidth(220);
        colNombre.setMinWidth(140);

        TableColumn<Usuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(vacio(d.getValue().getEmail())));
        colEmail.setPrefWidth(240);
        colEmail.setMinWidth(150);

        TableColumn<Usuario, Usuario> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colRol.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            @Override
            protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                setGraphic(vacio || usuario == null ? null : armarPillRol(usuario.getRol()));
            }
        });
        colRol.setPrefWidth(140);
        colRol.setMinWidth(100);

        TableColumn<Usuario, Usuario> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            @Override
            protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                if (vacio || usuario == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(usuario.isActivo() ? "Activo" : "Inactivo");
                pill.getStyleClass().add(usuario.isActivo() ? "pill-activo" : "pill-inactivo");
                setGraphic(pill);
            }
        });
        colEstado.setPrefWidth(120);
        colEstado.setMinWidth(90);

        TableColumn<Usuario, Usuario> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            private final Button botonVer = new Button();
            private final Button botonEditar = new Button();
            private final Button botonEliminar = new Button();
            private final HBox caja = new HBox(6, botonVer, botonEditar, botonEliminar);

            {
                botonVer.setGraphic(IconoFx.vista("ojo", 16));
                botonVer.getStyleClass().add("boton-fila-icono");
                botonVer.setTooltip(new Tooltip("Ver el detalle del usuario"));

                botonEditar.setGraphic(IconoFx.vista("lapiz", 16));
                botonEditar.getStyleClass().add("boton-fila-icono");
                botonEditar.setTooltip(new Tooltip("Editar nombre, email, rol o estado"));

                // Texto en vez de un ícono cargado por archivo -- mismo motivo que ya se usó para
                // "Vista previa"/"Imprimir" en Registros: no hay garantía de que exista un ícono
                // de tacho de basura entre los recursos de este proyecto.
                botonEliminar.setText("🗑");
                botonEliminar.getStyleClass().add("boton-fila-icono");
                botonEliminar.setStyle("-fx-text-fill: #B3261E;");
                botonEliminar.setTooltip(new Tooltip("Dar de baja al usuario"));

                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Usuario usuario, boolean vacio) {
                super.updateItem(usuario, vacio);
                if (vacio || usuario == null) {
                    setGraphic(null);
                    return;
                }
                botonVer.setOnAction(evt ->
                        new UsuarioFormDialog(UsuarioFormDialog.Modo.VER, usuario, null).mostrar());
                botonEditar.setOnAction(evt -> new UsuarioFormDialog(
                        UsuarioFormDialog.Modo.EDITAR, usuario, UsuariosScreen.this::cargarDatos).mostrar());
                botonEliminar.setOnAction(evt -> confirmarEliminar(usuario));
                setGraphic(caja);
            }
        });
        colAcciones.setPrefWidth(120);
        colAcciones.setMinWidth(110);
        // No resizable: el minWidth de arriba no alcanza por sí solo -- con CONSTRAINED_RESIZE_POLICY,
        // si la ventana queda angosta, la política achica columnas por DEBAJO de su propio mínimo
        // para que todo encaje igual, y a esta columna eso la deja con los 3 botones amontonados o
        // cortados (2026-10-01, encontrado al revisar el mismo síntoma en Pacientes). Marcarla no
        // resizable la saca del reparto: siempre mide exactamente lo que se le pidió.
        colAcciones.setResizable(false);

        TableView<Usuario> tabla = new TableView<>();
        tabla.getColumns().setAll(colNombre, colEmail, colRol, colEstado, colAcciones);
        tabla.setItems(usuariosVisibles);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("No hay usuarios cargados todavía."));
        return tabla;
    }

    private Label armarPillRol(String rol) {
        boolean admin = "Administrador".equalsIgnoreCase(rol);
        Label pill = new Label(vacio(rol));
        pill.setStyle(admin
                ? "-fx-background-color: #CFE2FF; -fx-text-fill: #0D6EFD; -fx-background-radius: 12; "
                + "-fx-padding: 3 12 3 12; -fx-font-size: 11.5px; -fx-font-weight: bold;"
                : "-fx-background-color: #F0F1EF; -fx-text-fill: #3A423E; -fx-background-radius: 12; "
                + "-fx-padding: 3 12 3 12; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        return pill;
    }

    /**
     * Pide confirmación y da de baja (lógica, no se borra el registro) al usuario elegido -- mismo
     * mecanismo que {@code vista.usuarios.TablaUsuarios#confirmarEliminarUsuario} en Swing.
     */
    private void confirmarEliminar(Usuario usuario) {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Seguro que querés dar de baja a " + vacio(usuario.getApellidoYNombre()) + "?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Eliminar usuario");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES) {
                Boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.UsuarioController.desactivar(null, usuario.getIdUsuario()), false);
                if (ok != null && ok) {
                    cargarDatos();
                }
            }
        });
    }

    private void cargarDatos() {
        List<Usuario> usuarios = PuenteEDT.ejecutar(
                () -> controlador.UsuarioController.listarTodos(null), Collections.emptyList());
        todosLosUsuarios.clear();
        todosLosUsuarios.addAll(usuarios);
        usuariosVisibles.setAll(usuarios);
        actualizarTotales(usuarios);
    }

    /**
     * Cuenta administradores/técnicos sobre la lista dada -- mismo cálculo que ya hacía
     * {@code vistas.formulariosPrincipales.Usuarios#actualizarTotales} en Swing, pero acá se llama
     * también desde {@link #filtrarPorTexto} para que las tarjetas de arriba siempre coincidan con
     * lo que se ve en la tabla (en Swing no se recalculaban al filtrar -- quedaban con el total
     * general aunque se estuviera viendo una búsqueda).
     */
    private void actualizarTotales(List<Usuario> usuarios) {
        int administradores = 0;
        int tecnicos = 0;
        for (Usuario u : usuarios) {
            if ("Administrador".equalsIgnoreCase(u.getRol())) {
                administradores++;
            } else if ("Tecnico".equalsIgnoreCase(u.getRol())) {
                tecnicos++;
            }
        }
        valorTotal.setText(String.valueOf(usuarios.size()));
        valorAdministradores.setText(String.valueOf(administradores));
        valorTecnicos.setText(String.valueOf(tecnicos));
    }

    /**
     * Filtra por nombre y apellido, email o rol -- la llama el buscador del header a cada tecla,
     * cuando esta pantalla está activa (ver {@code AppShell#navegar}, caso "7"). Filtra sobre
     * {@link #todosLosUsuarios} (lo ya traído de la base), sin volver a consultarla. Con el texto
     * vacío vuelve a mostrarlos todos.
     */
    public void filtrarPorTexto(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.getDefault());
        if (buscado.isEmpty()) {
            usuariosVisibles.setAll(todosLosUsuarios);
            actualizarTotales(todosLosUsuarios);
            return;
        }
        List<Usuario> filtrados = new ArrayList<>();
        for (Usuario u : todosLosUsuarios) {
            if (contiene(u.getApellidoYNombre(), buscado) || contiene(u.getEmail(), buscado)
                    || contiene(u.getRol(), buscado)) {
                filtrados.add(u);
            }
        }
        usuariosVisibles.setAll(filtrados);
        actualizarTotales(filtrados);
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.getDefault()).contains(buscado);
    }

    private static String vacio(String texto) {
        return texto == null || texto.trim().isEmpty() ? "—" : texto;
    }
}
