package vistas.javafx.auditoria;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.RegistroAuditoria;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla "Historial" (sección Administración; en el código sigue llamándose Auditoría -- sólo
 * el texto que ve Zaira cambió, 2026-10-01, a pedido de la clienta porque le quedaba más claro),
 * sólo para Administradores -- ver {@code AppShell#PANTALLA_POR_ID} y
 * {@code AppShell#construirDatosMenu}: no hay ningún toggle en Usuarios para dársela a un Técnico
 * a propósito, así que {@code pantallasPermitidas} nunca la va a tener y sólo queda
 * visible/accesible para quien ya pasa por el bypass de Administrador.
 *
 * <p>De sólo lectura: no tiene "+ Nuevo", ni editar, ni eliminar -- una fila de auditoría no se
 * edita ni se borra (ni lógicamente: no tendría sentido "dar de baja" el registro de que algo
 * pasó). Lo que llena esta tabla lo escribe {@code dao.AuditoriaDAO#registrar}, llamado desde
 * cada DAO que hace un movimiento importante -- ver su javadoc para la lista exacta.</p>
 */
public class AuditoriaScreen {

    private static final SimpleDateFormat FORMATO_FECHA_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final ObservableList<RegistroAuditoria> registrosVisibles = FXCollections.observableArrayList();

    /** Copia completa (sin filtrar), mismo criterio que {@code UsuariosScreen#todosLosUsuarios}. */
    private final List<RegistroAuditoria> todosLosRegistros = new ArrayList<>();

    private final Label valorTotal = new Label("0");
    private final Label valorHoy = new Label("0");
    private final Label valorFallidos = new Label("0");

    public Parent construir() {
        VBox raiz = new VBox(20);
        raiz.getStyleClass().add("pantalla");

        VBox tarjetaTabla = armarTarjetaTabla();
        VBox.setVgrow(tarjetaTabla, Priority.ALWAYS);

        raiz.getChildren().addAll(armarEncabezado(), armarTarjetasResumen(), tarjetaTabla);

        cargarDatos();
        return raiz;
    }

    private VBox armarEncabezado() {
        Label titulo = new Label("Historial");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Quién hizo qué, y cuándo -- últimos " + 500 + " movimientos del sistema");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        return new VBox(4, titulo, subtitulo);
    }

    private HBox armarTarjetasResumen() {
        return new HBox(16,
                vistas.javafx.TarjetaStatUtil.construir(
                        "Movimientos", valorTotal, "Listado actual (últimos 500)", "azul", "📋"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Hoy", valorHoy, "Movimientos de hoy", "verde", "📅"),
                vistas.javafx.TarjetaStatUtil.construir(
                        "Inicios Fallidos", valorFallidos, "Intentos de inicio de sesión fallidos", "ambar", "⚠️"));
    }

    private VBox armarTarjetaTabla() {
        Label titulo = new Label("Listado");
        titulo.getStyleClass().add("titulo-seccion");

        TableView<RegistroAuditoria> tabla = armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox tarjeta = new VBox(14, titulo, tabla);
        tarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    /**
     * Sin {@code CONSTRAINED_RESIZE_POLICY} a propósito -- esta tabla tiene una columna de texto
     * libre ("Detalle") que puede ser larga, y ya se vio este mes (Pagos / Cobros a Obras
     * Sociales) que forzar todas las columnas a entrar en el ancho de la tabla termina
     * recortándolas con "..." en vez de mostrarse completas. Con la política por defecto
     * (sin llamar a {@code setColumnResizePolicy}) cada columna conserva su ancho y, si no entra
     * todo, la tabla muestra su propia barra de scroll horizontal -- y "Detalle" además tiene un
     * tooltip con el texto completo por si igual queda cortado.
     */
    private TableView<RegistroAuditoria> armarTabla() {
        TableColumn<RegistroAuditoria, String> colFecha = new TableColumn<>("Fecha / Hora");
        colFecha.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getFechaHora() == null ? "—" : FORMATO_FECHA_HORA.format(d.getValue().getFechaHora())));
        colFecha.setPrefWidth(140);
        colFecha.setMinWidth(130);

        TableColumn<RegistroAuditoria, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                vacio(d.getValue().getNombreUsuario())));
        colUsuario.setPrefWidth(180);
        colUsuario.setMinWidth(140);

        TableColumn<RegistroAuditoria, RegistroAuditoria> colAccion = new TableColumn<>("Acción");
        colAccion.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colAccion.setCellFactory(col -> new TableCell<RegistroAuditoria, RegistroAuditoria>() {
            @Override
            protected void updateItem(RegistroAuditoria registro, boolean vacio) {
                super.updateItem(registro, vacio);
                setGraphic(vacio || registro == null ? null : armarPillAccion(registro.getAccion()));
            }
        });
        colAccion.setPrefWidth(150);
        colAccion.setMinWidth(130);

        TableColumn<RegistroAuditoria, String> colEntidad = new TableColumn<>("Entidad");
        colEntidad.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                etiquetaEntidad(d.getValue().getEntidad())));
        colEntidad.setPrefWidth(170);
        colEntidad.setMinWidth(140);

        TableColumn<RegistroAuditoria, String> colDetalle = new TableColumn<>("Detalle");
        colDetalle.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                vacio(d.getValue().getDetalle())));
        colDetalle.setCellFactory(col -> new TableCell<RegistroAuditoria, String>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                if (vacio || texto == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(texto);
                setTooltip(new Tooltip(texto));
            }
        });
        colDetalle.setPrefWidth(380);
        colDetalle.setMinWidth(220);

        TableView<RegistroAuditoria> tabla = new TableView<>();
        tabla.getColumns().setAll(colFecha, colUsuario, colAccion, colEntidad, colDetalle);
        tabla.setItems(registrosVisibles);
        tabla.setPlaceholder(new Label("Todavía no hay movimientos registrados."));
        return tabla;
    }

    private Label armarPillAccion(String accion) {
        String etiqueta;
        String clasePill;
        switch (accion == null ? "" : accion) {
            case "ALTA":
                etiqueta = "Alta";
                clasePill = "pill-activo";
                break;
            case "EDICION":
                etiqueta = "Edición";
                clasePill = "pill-procesando";
                break;
            case "BAJA":
                etiqueta = "Baja";
                clasePill = "pill-urgente";
                break;
            case "CAMBIO_ESTADO":
                etiqueta = "Cambio de estado";
                clasePill = "pill-mixto";
                break;
            case "ANULACION":
                etiqueta = "Anulación";
                clasePill = "pill-urgente";
                break;
            case "LOGIN":
                etiqueta = "Inicio de sesión";
                clasePill = "pill-activo";
                break;
            case "LOGIN_FALLIDO":
                etiqueta = "Inicio fallido";
                clasePill = "pill-urgente";
                break;
            default:
                etiqueta = vacio(accion);
                clasePill = "pill-inactivo";
        }
        Label pill = new Label(etiqueta);
        pill.getStyleClass().add(clasePill);
        return pill;
    }

    private String etiquetaEntidad(String entidad) {
        if (entidad == null) {
            return "—";
        }
        switch (entidad) {
            case "pacientes":
                return "Pacientes";
            case "usuarios":
                return "Usuarios";
            case "pagos":
                return "Pagos";
            case "obras_sociales":
                return "Obras Sociales";
            case "analisis_tipos":
                return "Catálogo de Exámenes";
            default:
                return entidad;
        }
    }

    private void cargarDatos() {
        List<RegistroAuditoria> registros = PuenteEDT.ejecutar(
                () -> controlador.AuditoriaController.listarRecientes(null), Collections.emptyList());
        todosLosRegistros.clear();
        todosLosRegistros.addAll(registros);
        registrosVisibles.setAll(registros);
        actualizarTotales(registros);
    }

    /**
     * Cuenta "hoy" e "inicios fallidos" sobre la lista dada -- mismo criterio que
     * {@code UsuariosScreen#actualizarTotales}: se recalcula también al filtrar, para que las
     * tarjetas de arriba siempre coincidan con lo que se ve en la tabla.
     */
    private void actualizarTotales(List<RegistroAuditoria> registros) {
        Calendar hoy = Calendar.getInstance();
        int hoyAnio = hoy.get(Calendar.YEAR);
        int hoyDia = hoy.get(Calendar.DAY_OF_YEAR);

        int movimientosHoy = 0;
        int fallidos = 0;
        for (RegistroAuditoria registro : registros) {
            if (registro.getFechaHora() != null) {
                Calendar fecha = Calendar.getInstance();
                fecha.setTime(registro.getFechaHora());
                if (fecha.get(Calendar.YEAR) == hoyAnio && fecha.get(Calendar.DAY_OF_YEAR) == hoyDia) {
                    movimientosHoy++;
                }
            }
            if ("LOGIN_FALLIDO".equals(registro.getAccion())) {
                fallidos++;
            }
        }
        valorTotal.setText(String.valueOf(registros.size()));
        valorHoy.setText(String.valueOf(movimientosHoy));
        valorFallidos.setText(String.valueOf(fallidos));
    }

    /**
     * Filtra por usuario, acción, entidad o detalle -- la llama el buscador del header a cada
     * tecla, cuando esta pantalla está activa (ver {@code AppShell#navegar}, caso "11"). Filtra
     * sobre {@link #todosLosRegistros} (lo ya traído de la base), sin volver a consultarla. Con el
     * texto vacío vuelve a mostrarlos todos.
     */
    public void filtrarPorTexto(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.getDefault());
        if (buscado.isEmpty()) {
            registrosVisibles.setAll(todosLosRegistros);
            actualizarTotales(todosLosRegistros);
            return;
        }
        List<RegistroAuditoria> filtrados = new ArrayList<>();
        for (RegistroAuditoria registro : todosLosRegistros) {
            if (contiene(registro.getNombreUsuario(), buscado) || contiene(registro.getAccion(), buscado)
                    || contiene(etiquetaEntidad(registro.getEntidad()), buscado)
                    || contiene(registro.getDetalle(), buscado)) {
                filtrados.add(registro);
            }
        }
        registrosVisibles.setAll(filtrados);
        actualizarTotales(filtrados);
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.getDefault()).contains(buscado);
    }

    private static String vacio(String texto) {
        return texto == null || texto.trim().isEmpty() ? "—" : texto;
    }
}
