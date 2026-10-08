package vistas.javafx.registrarResultados;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Paciente;
import modelo.Parametro;
import modelo.Sexo;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla "Cargar Resultados" de un examen puntual, en JavaFX puro. Reemplaza a
 * {@code vistas.registrarResultados.cargarReultados} (Swing), que tenía las columnas Analito /
 * Unidad / Rango Ref. / Valor / Estado con anchos fijos en píxeles (diseñada para 992px): en una
 * ventana de otro tamaño, "Valor" y "Estado" -- justo donde se carga el resultado -- quedaban
 * fuera del área visible sin ningún scroll para llegar a ellas. Acá las columnas usan layouts que
 * se adaptan al ancho real de la ventana (igual que el resto de las pantallas ya migradas), así
 * que ese problema no puede volver a pasar.
 *
 * <p>A diferencia del viejo {@code ContenedorExamenes}, el campo numérico no bloquea letra por
 * letra los caracteres inválidos (eso pedía un {@code DocumentFilter}, específico de Swing) --
 * en cambio, igual que el resto de esta pantalla, valida en vivo a cada tecla y marca en rojo
 * el campo si lo que hay no es un número válido, con el mismo criterio para frenar el guardado.
 * Tampoco usa un área de texto multilínea para el tipo "texto" (queda en un campo de una sola
 * línea, para no romper el alto parejo de las filas) -- si hace falta cargar algo largo, se
 * puede seguir escribiendo, solo que sin salto de línea.</p>
 */
public class CargarResultadosScreen {

    private static final Pattern PATRON_NUMERICO = Pattern.compile("-?\\d+([.,]\\d+)?");

    public interface CancelarListener {
        void onCancelar();
    }

    public interface GuardarListener {
        void onGuardado();
    }

    private final Paciente paciente;
    private final Date fechaAnalisis;
    private final String numeroOrden;
    private final int idPedidoAnalisis;
    private final int idAnalisisTipo;
    private final String nombreExamen;

    private final List<FilaControl> filasControl = new ArrayList<>();
    private List<Sexo> listaSexos = Collections.emptyList();

    private CancelarListener alCancelar;
    private GuardarListener alGuardar;

    /** true si el nivel de acceso del usuario a este examen es "Solo ver" (no "Sin acceso", ese
     * caso ni construye esta pantalla -- ver {@link #construir()}). */
    private boolean soloLectura;

    public CargarResultadosScreen(Paciente paciente, Date fechaAnalisis, String numeroOrden,
            int idPedidoAnalisis, int idAnalisisTipo, String nombreExamen) {
        this.paciente = paciente;
        this.fechaAnalisis = fechaAnalisis;
        this.numeroOrden = numeroOrden;
        this.idPedidoAnalisis = idPedidoAnalisis;
        this.idAnalisisTipo = idAnalisisTipo;
        this.nombreExamen = nombreExamen;
    }

    public void setAlCancelar(CancelarListener listener) {
        this.alCancelar = listener;
    }

    public void setAlGuardar(GuardarListener listener) {
        this.alGuardar = listener;
    }

    public Parent construir() {
        // Bloqueo real (no sólo el botón "Cargar resultados" de RegistrarResultadosScreen, que se
        // puede saltear si algo más adelante llega a abrir esta pantalla directo): un examen "Sin
        // acceso" ni siquiera arma el formulario de carga -- muestra un cartel y listo. "Solo ver"
        // sí arma el formulario pero deja todo deshabilitado (ver armarCampoValor / armarBarraBotones).
        String nivel = PuenteEDT.ejecutar(
                () -> controlador.PermisoController.obtenerPermisosExamenSesion(null).nivel(idAnalisisTipo),
                "Cargar");
        if ("Sin acceso".equals(nivel)) {
            return armarPantallaSinAcceso();
        }
        soloLectura = "Solo ver".equals(nivel);

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");
        raiz.setTop(armarEncabezado());

        listaSexos = PuenteEDT.ejecutar(() -> controlador.SexoController.listarTodos(null), Collections.emptyList());

        controlador.ResultadosController.DatosExamen datos = PuenteEDT.ejecutar(
                () -> controlador.ResultadosController.cargarExamen(
                        null, idAnalisisTipo, idPedidoAnalisis, paciente.getIdSexo()),
                new controlador.ResultadosController.DatosExamen(Collections.emptyList(), Collections.emptyMap()));

        VBox contenido = new VBox(20,
                armarTarjetaPaciente(),
                armarTarjetaExamen(datos.getParametros(), datos.getValoresGuardados()));
        contenido.setPadding(new Insets(16, 2, 24, 2));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        BorderPane.setMargin(scroll, new Insets(16, 0, 0, 0));
        raiz.setCenter(scroll);

        HBox botones = armarBarraBotones();
        BorderPane.setMargin(botones, new Insets(16, 0, 0, 0));
        raiz.setBottom(botones);

        return raiz;
    }

    private VBox armarEncabezado() {
        Label titulo = new Label("Orden #" + valorOGuion(numeroOrden) + "  —  " + valorOGuion(paciente.getNyaPaciente()));
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Cargar resultados del examen: " + valorOGuion(nombreExamen));
        subtitulo.getStyleClass().add("subtitulo-pantalla");

        if (!soloLectura) {
            return new VBox(4, titulo, subtitulo);
        }
        Label avisoSoloLectura = new Label("👁  Tenés acceso de solo lectura a este examen: podés ver los resultados, no cargarlos.");
        avisoSoloLectura.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C6D1F;");
        return new VBox(4, titulo, subtitulo, avisoSoloLectura);
    }

    private VBox armarTarjetaPaciente() {
        Label titulo = new Label("Paciente");
        titulo.getStyleClass().add("titulo-seccion");

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        GridPane grilla = new GridPane();
        grilla.setHgap(24);
        grilla.setVgap(10);

        agregarDato(grilla, 0, 0, "D.N.I", paciente.getDni());
        agregarDato(grilla, 1, 0, "Apellido y Nombre", paciente.getNyaPaciente());
        agregarDato(grilla, 0, 1, "Edad", String.valueOf(paciente.calcularEdad()));
        agregarDato(grilla, 1, 1, "Sexo", nombreSexo(paciente.getIdSexo()));
        agregarDato(grilla, 0, 2, "Celular", paciente.getTelefono());
        agregarDato(grilla, 1, 2, "Fecha de Análisis",
                fechaAnalisis != null ? formatoFecha.format(fechaAnalisis) : "-");

        VBox tarjeta = new VBox(12, titulo, grilla);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    private void agregarDato(GridPane grilla, int columna, int fila, String etiquetaTexto, String valorTexto) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        Label valor = new Label(valorOGuion(valorTexto));
        VBox caja = new VBox(4, etiqueta, valor);
        grilla.add(caja, columna, fila);
    }

    private String nombreSexo(int idSexo) {
        for (Sexo s : listaSexos) {
            if (s.getIdSexo() == idSexo) {
                return s.getNombreSexo();
            }
        }
        return "-";
    }

    private VBox armarTarjetaExamen(List<Parametro> parametros, Map<Integer, String> valoresGuardados) {
        Label titulo = new Label(valorOGuion(nombreExamen));
        titulo.getStyleClass().add("titulo-seccion");

        VBox tarjeta = new VBox(10, titulo);
        tarjeta.getStyleClass().add("tarjeta");

        if (parametros.isEmpty()) {
            Label vacio = new Label("Este examen todavía no tiene parámetros cargados en el sistema.");
            vacio.setStyle("-fx-text-fill: #8C9490; -fx-font-size: 12.5px;");
            tarjeta.getChildren().add(vacio);
            return tarjeta;
        }

        tarjeta.getChildren().add(armarFilaEncabezadoColumnas());

        VBox filas = new VBox();
        boolean fondoAlterno = false;
        for (Parametro p : parametros) {
            filas.getChildren().add(armarFilaParametro(p, valoresGuardados.get(p.getIdParametro()), fondoAlterno));
            fondoAlterno = !fondoAlterno;
        }
        tarjeta.getChildren().add(filas);

        return tarjeta;
    }

    private HBox armarFilaEncabezadoColumnas() {
        HBox fila = new HBox(14,
                etiquetaColumna("Analito", 160, true),
                etiquetaColumna("Unidad", 70, false),
                etiquetaColumna("Rango Ref.", 170, false),
                etiquetaColumna("Valor", 150, false),
                etiquetaColumna("Estado", 110, false));
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(0, 6, 8, 6));
        fila.setStyle("-fx-border-color: transparent transparent #E6E9E7 transparent; -fx-border-width: 0 0 1 0;");
        return fila;
    }

    private Label etiquetaColumna(String texto, double ancho, boolean crece) {
        Label etiqueta = new Label(texto);
        etiqueta.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8C9490;");
        etiqueta.setMinWidth(ancho);
        etiqueta.setPrefWidth(ancho);
        if (crece) {
            HBox.setHgrow(etiqueta, Priority.ALWAYS);
        }
        return etiqueta;
    }

    private HBox armarFilaParametro(Parametro p, String valorGuardado, boolean fondoAlterno) {
        Label nombre = new Label(p.getNombreParametro());
        nombre.setWrapText(true);
        nombre.setMinWidth(160);
        nombre.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        HBox.setHgrow(nombre, Priority.ALWAYS);

        Label unidad = new Label(p.getUnidad() != null ? p.getUnidad() : "");
        unidad.setMinWidth(70);
        unidad.setPrefWidth(70);
        unidad.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 12.5px;");

        Label referencia = new Label(p.getValorReferencia() != null ? p.getValorReferencia() : "");
        referencia.setWrapText(true);
        referencia.setMinWidth(150);
        referencia.setPrefWidth(170);
        referencia.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 12.5px;");

        Label estado = new Label();
        estado.setMinWidth(110);
        estado.setPrefWidth(110);

        Node campoValor = armarCampoValor(p, valorGuardado, referencia, estado);

        HBox fila = new HBox(14, nombre, unidad, referencia, campoValor, estado);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(8, 6, 8, 6));
        fila.setStyle("-fx-background-color: " + (fondoAlterno ? "#F8FAF9" : "white") + ";"
                + " -fx-border-color: transparent transparent #F0F1EF transparent; -fx-border-width: 0 0 1 0;");
        return fila;
    }

    /**
     * Arma el campo de carga que corresponda al tipo_dato del parámetro ("cualitativo" arma un
     * ComboBox con sus opciones más un "Seleccione..." inicial; cualquier otro valor -incluido
     * "texto" y null- cae en un campo numérico con validación en vivo) y lo registra en
     * {@link #filasControl} para poder leerlo después al guardar.
     */
    private Node armarCampoValor(Parametro p, String valorGuardado, Label referenciaLabel, Label estadoLabel) {
        String tipo = p.getTipoDato() == null ? "numerico" : p.getTipoDato();

        if ("cualitativo".equals(tipo)) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().add("Seleccione...");
            if (p.getOpcionesCualitativo() != null && !p.getOpcionesCualitativo().isEmpty()) {
                for (String opcion : p.getOpcionesCualitativo().split(",")) {
                    combo.getItems().add(opcion.trim());
                }
            }
            combo.setValue(valorGuardado != null && !valorGuardado.isEmpty() ? valorGuardado : "Seleccione...");
            combo.setPrefWidth(150);
            combo.setDisable(soloLectura);
            filasControl.add(new FilaControl(p.getIdParametro(), p.getNombreParametro(), tipo, combo));
            return combo;
        }

        if (!"numerico".equals(tipo)) {
            // "texto" (u otro valor no reconocido): campo libre de una sola línea, sin validación.
            TextField campo = new TextField(valorGuardado != null ? valorGuardado : "");
            campo.getStyleClass().add("campo-form");
            campo.setPrefWidth(150);
            campo.setDisable(soloLectura);
            filasControl.add(new FilaControl(p.getIdParametro(), p.getNombreParametro(), tipo, campo));
            return campo;
        }

        TextField campo = new TextField(valorGuardado != null ? valorGuardado : "");
        campo.getStyleClass().add("campo-form");
        campo.setPrefWidth(150);
        campo.setDisable(soloLectura);
        FilaControl fc = new FilaControl(p.getIdParametro(), p.getNombreParametro(), "numerico", campo);
        filasControl.add(fc);

        Runnable revisar = () -> revisarCampoNumerico(campo, fc, referenciaLabel, estadoLabel);
        campo.setOnKeyReleased(evt -> revisar.run());
        revisar.run(); // por si vino precargado con un valor guardado
        return campo;
    }

    private void revisarCampoNumerico(TextField campo, FilaControl fc, Label referenciaLabel, Label estadoLabel) {
        String texto = campo.getText() == null ? "" : campo.getText().trim();
        boolean ok = texto.isEmpty() || PATRON_NUMERICO.matcher(texto).matches();
        fc.valido = ok;

        if (ok) {
            campo.getStyleClass().remove("campo-invalido");
        } else if (!campo.getStyleClass().contains("campo-invalido")) {
            campo.getStyleClass().add("campo-invalido");
        }

        actualizarEstado(texto, ok, referenciaLabel.getText(), estadoLabel);
    }

    /**
     * Recalcula el badge de Estado (Normal / Alto / Bajo) comparando el valor cargado contra el
     * rango de referencia -- mismo criterio que tenía {@code ContenedorExamenes.actualizarEstado}
     * en la versión Swing. Se deja vacío si el toggle "Alertas de valores fuera de rango"
     * (Configuración &gt; Sistema) está apagado, si el valor está vacío o no es válido todavía, o
     * si la referencia no viene en un formato reconocible.
     */
    private void actualizarEstado(String texto, boolean valorValido, String referencia, Label estadoLabel) {
        estadoLabel.getStyleClass().removeAll("pill-activo", "pill-urgente");

        if (!utilidades.PreferenciasSistema.isAlertasFueraDeRango() || texto.isEmpty() || !valorValido) {
            estadoLabel.setText("");
            return;
        }

        double valor;
        try {
            valor = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            estadoLabel.setText("");
            return;
        }

        String estado = utilidades.CalculoEstadoUtil.calcular(valor, referencia);
        if (estado == null) {
            estadoLabel.setText("");
            return;
        }

        boolean normal = "Normal".equals(estado);
        estadoLabel.setText((normal ? "✓ " : "⚠ ") + estado);
        estadoLabel.getStyleClass().add(normal ? "pill-activo" : "pill-urgente");
    }

    private HBox armarBarraBotones() {
        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> confirmarCancelar());

        Button botonGuardar = new Button("Guardar y Notificar");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar());
        botonGuardar.setDisable(soloLectura);

        HBox fila = new HBox(12, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    /**
     * Se muestra en vez de todo el formulario cuando el usuario no tiene ningún acceso a este
     * examen -- bloqueo real, no un botón deshabilitado nomás (ver {@link #construir()}).
     */
    private Parent armarPantallaSinAcceso() {
        Label titulo = new Label("Sin acceso a este examen");
        titulo.getStyleClass().add("titulo-pantalla");

        Label mensaje = new Label("Tu usuario no tiene permiso para ver ni cargar resultados de \""
                + valorOGuion(nombreExamen) + "\". Si creés que es un error, pedile a un "
                + "administrador que te dé acceso desde Usuarios.");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(480);
        mensaje.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 13px;");

        Button botonVolver = new Button("Volver");
        botonVolver.getStyleClass().add("boton-secundario");
        botonVolver.setOnAction(evt -> {
            if (alCancelar != null) {
                alCancelar.onCancelar();
            }
        });

        VBox tarjeta = new VBox(14, titulo, mensaje, botonVolver);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPadding(new Insets(32));

        VBox raiz = new VBox(tarjeta);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(20));
        return raiz;
    }

    private void confirmarCancelar() {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Cancelar la carga de resultados y volver al listado?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Cancelar");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES && alCancelar != null) {
                alCancelar.onCancelar();
            }
        });
    }

    /**
     * Junta el valor cargado en cada renglón y lo guarda -- misma secuencia de validaciones que
     * tenía {@code cargarReultados.guardarResultados} en la versión Swing.
     */
    private void guardar() {
        if (soloLectura) {
            return; // el botón ya queda deshabilitado; este chequeo es sólo defensa extra.
        }
        if (filasControl.isEmpty()) {
            alertar(AlertType.WARNING, "Nada para guardar", "No hay parámetros para guardar en este examen.");
            return;
        }

        List<String> invalidos = new ArrayList<>();
        for (FilaControl fc : filasControl) {
            if (!fc.valido) {
                invalidos.add(fc.nombreParametro);
            }
        }
        if (!invalidos.isEmpty()) {
            alertar(AlertType.WARNING, "Valores inválidos",
                    "Revisá estos valores, no son números válidos:\n· " + String.join("\n· ", invalidos));
            return;
        }

        Map<Integer, String> valores = new HashMap<>();
        boolean faltaAlguno = false;
        for (FilaControl fc : filasControl) {
            String valor = fc.obtenerValor();
            if (valor.isEmpty()) {
                faltaAlguno = true;
                continue;
            }
            valores.put(fc.idParametro, valor);
        }

        if (valores.isEmpty()) {
            alertar(AlertType.WARNING, "Nada para guardar", "Cargá al menos un valor antes de guardar.");
            return;
        }

        boolean faltaAlgunoFinal = faltaAlguno;
        boolean ok = PuenteEDT.ejecutar(
                () -> controlador.ResultadosController.guardarResultados(null, idPedidoAnalisis, valores, faltaAlgunoFinal),
                false);

        if (!ok) {
            return;
        }

        vistas.javafx.AvisoRapido.mostrar("Guardado",
                faltaAlguno
                        ? "Se guardaron los resultados cargados. El examen queda en proceso porque faltan valores."
                        : "Resultados guardados. El examen quedó marcado como completado.");

        if (alGuardar != null) {
            alGuardar.onGuardado();
        }
    }

    private void alertar(AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setHeaderText(null);
        alerta.setTitle(titulo);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    private static String valorOGuion(String texto) {
        return texto == null || texto.trim().isEmpty() ? "-" : texto;
    }

    /**
     * Un renglón de carga: qué campo (JavaFX) se usó y si lo que tiene adentro es válido en este
     * momento -- lo que necesita {@link #guardar()} para leer y validar todo junto sin acoplarse
     * al tipo concreto de control (TextField vs ComboBox).
     */
    private static final class FilaControl {
        final int idParametro;
        final String nombreParametro;
        final String tipoDato;
        final Node control;
        boolean valido = true;

        FilaControl(int idParametro, String nombreParametro, String tipoDato, Node control) {
            this.idParametro = idParametro;
            this.nombreParametro = nombreParametro;
            this.tipoDato = tipoDato;
            this.control = control;
        }

        String obtenerValor() {
            if (control instanceof TextField) {
                String texto = ((TextField) control).getText();
                texto = texto == null ? "" : texto.trim();
                if ("numerico".equals(tipoDato)) {
                    texto = texto.replace(',', '.');
                }
                return texto;
            }
            if (control instanceof ComboBox) {
                Object seleccion = ((ComboBox<?>) control).getValue();
                if (seleccion == null || seleccion.toString().startsWith("Seleccione")) {
                    return "";
                }
                return seleccion.toString();
            }
            return "";
        }
    }
}
