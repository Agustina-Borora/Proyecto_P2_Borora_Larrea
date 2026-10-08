package vistas.javafx.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.Analito;
import modelo.AnalisisTipo;
import modelo.Prestacion;
import modelo.TramoTexto;
import utilidades.TramosTextoUtil;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;
import vistas.javafx.VentanaUtil;
import vistas.javafx.comunes.EditorTramosDialog;

/**
 * Formulario modal de Alta/Edición de un examen del Catálogo, abierto desde {@link
 * CatalogoExamenesScreen}. Si {@code analisisTipo} es null es un Alta; si no, se edita esa
 * instancia. Además de los datos propios de {@code analisis_tipos}, permite editar los analitos
 * (parámetros) que componen el examen -- equivalente al modal "Editar" del Figma, que muestra
 * Nombre/Compañía + una tabla de Nombre/Rango Min/Rango Max/Unidad con "+ Agregar Analito".
 * Acá el rango se maneja como un único texto de valor de referencia (por ejemplo "70 - 110
 * mg/dL"), no como mínimo y máximo separados, porque así está guardada la columna
 * {@code valores_referencia.texto_referencia} en la base real.
 */
public class ExamenFormDialog {

    private static final List<String> TIPOS_DATO = java.util.Arrays.asList("numerico", "cualitativo", "texto");

    /**
     * Unidades más usadas en los análisis clínicos del laboratorio -- se ofrecen en
     * {@link FilaAnalito#campoUnidadAnalito} como combo (en vez de tenerlas que escribir cada
     * vez), pero sigue siendo editable: si el analito usa una unidad que no está en esta lista, se
     * puede escribir igual, no queda limitado a estas.
     */
    private static final List<String> UNIDADES_COMUNES = java.util.Arrays.asList(
            "mg/dL", "g/dL", "g/L", "%", "mEq/L", "mmol/L", "UI/L", "U/L", "mU/L", "µUI/mL",
            "ng/mL", "ng/dL", "pg/mL", "µg/dL", "mg/L", "/mm³", "x10³/µL", "x10⁶/µL", "seg", "mm/h");

    private final AnalisisTipo analisisTipo;
    private final Runnable alGuardar;

    private final TextField campoNombre = new TextField();
    private final TextField campoCodigo = new TextField();
    private final ComboBox<String> campoCategoria = new ComboBox<>();
    private final CheckBox campoActivo = new CheckBox("Activo");
    private final Label etiquetaPrecio = new Label("—");

    private final TextField campoBuscarNomenclador = new TextField();
    private final ComboBox<OpcionPrestacion> comboResultadosNomenclador = new ComboBox<>();

    private final VBox contenedorAnalitos = new VBox(10);
    private final List<FilaAnalito> filas = new ArrayList<>();
    private final List<Integer> idsAnalitosOriginales = new ArrayList<>();

    /**
     * @param analisisTipo null para un Alta, o el examen a editar.
     * @param alGuardar se ejecuta después de guardar con éxito, para que quien abrió el
     * formulario (la pantalla de listado) pueda refrescar su tabla.
     */
    public ExamenFormDialog(AnalisisTipo analisisTipo, Runnable alGuardar) {
        this.analisisTipo = analisisTipo;
        this.alGuardar = alGuardar;
    }

    public void mostrar() {
        boolean esEdicion = analisisTipo != null;

        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(esEdicion ? "Editar Examen" : "Nuevo Examen");

        VBox raiz = new VBox(18);
        raiz.setPadding(new Insets(24));

        Label titulo = new Label(esEdicion ? "Editar Examen" : "Nuevo Examen");
        titulo.getStyleClass().add("titulo-pantalla");

        GridPane grilla = armarGrilla(esEdicion);

        Label subtituloAnalitos = new Label("Analitos (parámetros del examen)");
        subtituloAnalitos.getStyleClass().add("subtitulo-pantalla");

        // "Agregar analito" va ARRIBA, al lado del título de la sección, así se ve siempre sin
        // tener que bajar. La fila nueva aparece al final de la lista, la lista baja sola hasta
        // ella y queda el cursor en el nombre, lista para escribir.
        ScrollPane scrollAnalitos = new ScrollPane(contenedorAnalitos);
        Button botonAgregarAnalito = new Button("+ Agregar Analito");
        botonAgregarAnalito.getStyleClass().add("boton-primario");
        botonAgregarAnalito.setOnAction(evt -> {
            FilaAnalito nueva = agregarFilaAnalito(null);
            javafx.application.Platform.runLater(() -> {
                scrollAnalitos.layout();
                scrollAnalitos.setVvalue(scrollAnalitos.getVmax());
                nueva.campoNombreAnalito.requestFocus();
            });
        });
        Region espacioAnalitos = new Region();
        HBox.setHgrow(espacioAnalitos, Priority.ALWAYS);
        HBox encabezadoAnalitos = new HBox(12, subtituloAnalitos, espacioAnalitos, botonAgregarAnalito);
        encabezadoAnalitos.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        scrollAnalitos.setFitToWidth(true);
        // Sin alto fijo chico: con pocos analitos ocupa lo justo; con muchos, aparece la barra
        // para bajar (y la ventana entera también baja si la pantalla es chica).
        scrollAnalitos.setPrefHeight(Region.USE_COMPUTED_SIZE);
        scrollAnalitos.setPrefViewportHeight(230);
        scrollAnalitos.setMaxHeight(260);
        scrollAnalitos.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollAnalitos.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        precargarSiCorresponde();

        HBox botones = armarBotones(ventana);

        raiz.getChildren().addAll(titulo, grilla, encabezadoAnalitos, scrollAnalitos);

        // Todo lo de arriba (menos la fila de botones) va dentro de un ScrollPane propio, y la
        // fila de botones queda fija abajo de todo -- así, en una pantalla más chica que el
        // diseño (notebook con poca altura, zoom de Windows arriba de 100%), el contenido baja en
        // vez de cortarse, y "Guardar"/"Cancelar" siempre quedan visibles y clickeables (antes,
        // con la ventana en tamaño fijo y sin scroll, esos dos botones podían quedar tapados por
        // la barra de tareas). Ver VentanaUtil.
        ScrollPane scrollExterno = new ScrollPane(raiz);
        scrollExterno.getStyleClass().add("config-scroll");
        scrollExterno.setFitToWidth(true);
        scrollExterno.setHbarPolicy(ScrollBarPolicy.NEVER);
        scrollExterno.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        VBox raizExterna = new VBox(scrollExterno, botones);
        raizExterna.getStyleClass().add("pantalla");
        raizExterna.setPadding(new Insets(0, 24, 20, 0));
        VBox.setVgrow(scrollExterno, Priority.ALWAYS);
        botones.setPadding(new Insets(12, 24, 0, 0));

        // Ancho pensado para que la fila de un analito (Nombre + Unidad + Tipo de dato + Valor de
        // referencia + "Quitar") entre completa: con 560px (el ancho viejo) esos 5 controles no
        // entraban y JavaFX los recortaba mostrando "..." en vez del texto real -- nada informaba
        // bien qué tipo de dato tenía cada analito ni se leía el botón "Quitar".
        Scene escena = VentanaUtil.escenaAdaptable(ventana, raizExterna, 820, 720);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.showAndWait();
    }

    private GridPane armarGrilla(boolean esEdicion) {
        GridPane grilla = new GridPane();
        grilla.setHgap(14);
        grilla.setVgap(12);

        campoNombre.getStyleClass().add("campo-form");
        campoNombre.setPrefWidth(420);
        campoCodigo.getStyleClass().add("campo-form");
        campoCodigo.setPrefWidth(420);
        campoCodigo.setPromptText("Código de nomenclador (opcional)");

        campoCategoria.getStyleClass().add("campo-form");
        campoCategoria.setPrefWidth(420);
        campoCategoria.setEditable(true);
        campoCategoria.setPromptText("Ej: Hematología, Bioquímica... (opcional)");
        cargarCategorias();

        int fila = 0;
        grilla.add(new EtiquetaCampo("Nombre *"), 0, fila);
        grilla.add(campoNombre, 1, fila);
        fila++;

        grilla.add(new EtiquetaCampo("Código"), 0, fila);
        grilla.add(campoCodigo, 1, fila);
        fila++;

        grilla.add(new EtiquetaCampo("Buscar en nomenclador"), 0, fila);
        grilla.add(armarBuscadorNomenclador(), 1, fila);
        fila++;

        grilla.add(new EtiquetaCampo("Categoría"), 0, fila);
        grilla.add(campoCategoria, 1, fila);
        fila++;

        grilla.add(new EtiquetaCampo("Precio estimado"), 0, fila);
        etiquetaPrecio.getStyleClass().add("subtitulo-pantalla");
        grilla.add(etiquetaPrecio, 1, fila);
        fila++;

        if (esEdicion) {
            grilla.add(campoActivo, 1, fila);
        }

        return grilla;
    }

    /** Label auxiliar con la clase de estilo de etiqueta de campo -- evita repetir el patrón. */
    private static final class EtiquetaCampo extends Label {
        EtiquetaCampo(String texto) {
            super(texto);
            getStyleClass().add("etiqueta-campo");
        }
    }

    /**
     * Trae las categorías ya usadas en otros exámenes para ofrecerlas en el desplegable --
     * sigue siendo editable, así que también se puede escribir una categoría nueva.
     */
    private void cargarCategorias() {
        List<String> categorias = PuenteEDT.ejecutar(
                () -> controlador.AnalisisTipoController.listarCategorias(null),
                java.util.Collections.emptyList());
        campoCategoria.getItems().setAll(categorias);
    }

    /**
     * Buscador del nomenclador cargado (tabla {@code prestaciones}): escribís parte del nombre
     * o el código y elegís de la lista -- así no hace falta saberse el código de memoria. Al
     * elegir una opción, completa el campo Código de arriba.
     */
    private VBox armarBuscadorNomenclador() {
        campoBuscarNomenclador.getStyleClass().add("campo-form");
        campoBuscarNomenclador.setPromptText("Nombre o código (ej: glucemia, 412)");
        campoBuscarNomenclador.setPrefWidth(420);
        campoBuscarNomenclador.setOnKeyReleased(evt -> buscarEnNomenclador());

        comboResultadosNomenclador.setPrefWidth(420);
        comboResultadosNomenclador.setPromptText("Resultados de la búsqueda");
        comboResultadosNomenclador.setOnAction(evt -> seleccionarResultadoNomenclador());

        VBox contenedor = new VBox(6, campoBuscarNomenclador, comboResultadosNomenclador);
        return contenedor;
    }

    private void buscarEnNomenclador() {
        String texto = campoBuscarNomenclador.getText() == null ? "" : campoBuscarNomenclador.getText().trim();
        if (texto.length() < 2) {
            comboResultadosNomenclador.getItems().clear();
            return;
        }
        List<Prestacion> resultados = PuenteEDT.ejecutar(
                () -> controlador.NomencladorController.buscar(null, texto),
                java.util.Collections.emptyList());
        List<OpcionPrestacion> opciones = new ArrayList<>();
        for (Prestacion prestacion : resultados) {
            opciones.add(new OpcionPrestacion(prestacion));
        }
        comboResultadosNomenclador.getItems().setAll(opciones);
        if (!opciones.isEmpty()) {
            comboResultadosNomenclador.show();
        }
    }

    private void seleccionarResultadoNomenclador() {
        OpcionPrestacion elegida = comboResultadosNomenclador.getValue();
        if (elegida == null) {
            return;
        }
        campoCodigo.setText(String.valueOf(elegida.prestacion.getCodigo()));
    }

    /** Envuelve una {@link Prestacion} para mostrarla legible en el combo de resultados. */
    private static final class OpcionPrestacion {
        private final Prestacion prestacion;

        OpcionPrestacion(Prestacion prestacion) {
            this.prestacion = prestacion;
        }

        @Override
        public String toString() {
            BigDecimal unidades = prestacion.getUnidadesBioquimicas();
            String textoUnidades = unidades == null ? "sin UB" : unidades.stripTrailingZeros().toPlainString() + " UB";
            return prestacion.getCodigo() + " - " + prestacion.getNombrePrestacion() + " (" + textoUnidades + ")";
        }
    }

    private void precargarSiCorresponde() {
        if (analisisTipo == null) {
            campoActivo.setSelected(true);
            etiquetaPrecio.setText("Se calcula al guardar un código de nomenclador válido.");
            return;
        }
        campoNombre.setText(analisisTipo.getNombreAnalisis());
        campoCodigo.setText(analisisTipo.getCodigoAnalisis() == null ? "" : String.valueOf(analisisTipo.getCodigoAnalisis()));
        campoCategoria.setValue(analisisTipo.getCategoria());
        campoActivo.setSelected(analisisTipo.isActivo());
        etiquetaPrecio.setText(formatearPrecio(analisisTipo.getPrecioEstimado()));

        List<Analito> analitos = PuenteEDT.ejecutar(
                () -> controlador.AnalisisTipoController.listarAnalitos(null, analisisTipo.getIdAnalisisTipo()),
                java.util.Collections.emptyList());
        for (Analito analito : analitos) {
            idsAnalitosOriginales.add(analito.getIdAnalito());
            agregarFilaAnalito(analito);
        }
    }

    private static String formatearPrecio(BigDecimal valor) {
        return valor == null ? "—" : "$" + String.format(Locale.US, "%,.2f", valor);
    }

    private FilaAnalito agregarFilaAnalito(Analito existente) {
        FilaAnalito fila = new FilaAnalito(existente);
        filas.add(fila);
        contenedorAnalitos.getChildren().add(fila.contenedor);
        return fila;
    }

    private void quitarFilaAnalito(FilaAnalito fila) {
        filas.remove(fila);
        contenedorAnalitos.getChildren().remove(fila.contenedor);
    }

    /**
     * Pregunta antes de sacar una fila de analito -- para que un clic de más en "Quitar" no
     * borre un parámetro por accidente. Si el analito ya existía en la base, aclara que la baja
     * recién se aplica de verdad al tocar "Guardar" (hasta ese momento, "Cancelar" deja todo como
     * estaba).
     */
    private void confirmarQuitarFila(FilaAnalito fila) {
        String nombre = fila.campoNombreAnalito.getText();
        String etiqueta = (nombre == null || nombre.trim().isEmpty()) ? "esta fila" : "\"" + nombre.trim() + "\"";
        String mensaje = fila.idAnalito == null
                ? "¿Seguro que querés quitar " + etiqueta + "?"
                : "¿Seguro que querés quitar " + etiqueta + "? Se va a dar de baja recién al tocar \"Guardar\" "
                        + "-- con \"Cancelar\" queda todo como estaba.";

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, mensaje,
                javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Quitar analito");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == javafx.scene.control.ButtonType.YES) {
                quitarFilaAnalito(fila);
            }
        });
    }

    /** Una fila editable de la sub-tabla de analitos (parámetros) del examen. */
    private final class FilaAnalito {

        private final Integer idAnalito;
        private final TextField campoNombreAnalito = new TextField();
        private final ComboBox<String> campoUnidadAnalito =
                new ComboBox<>(FXCollections.observableArrayList(UNIDADES_COMUNES));
        private final ComboBox<String> campoTipoDato = new ComboBox<>(FXCollections.observableArrayList(TIPOS_DATO));
        private final TextField campoValorReferencia = new TextField();
        private final Button botonColor = new Button("🎨");
        private final HBox contenedor;

        /**
         * Tramos con color del valor de referencia (ver {@link TramosTextoUtil}). Vacía mientras
         * se edita como texto plano de siempre en {@link #campoValorReferencia}; deja de estar
         * vacía apenas se le pone color a algún tramo desde "🎨", momento en el que el campo de
         * texto simple pasa a ser de solo lectura (para no pisar los colores escribiendo encima).
         */
        private List<TramoTexto> tramosConColor = new ArrayList<>();

        FilaAnalito(Analito existente) {
            this.idAnalito = existente == null ? null : existente.getIdAnalito();

            campoNombreAnalito.getStyleClass().add("campo-form");
            campoNombreAnalito.setPromptText("Nombre del analito");
            campoNombreAnalito.setPrefWidth(180);

            campoUnidadAnalito.getStyleClass().add("campo-form");
            campoUnidadAnalito.setEditable(true); // además de elegir de la lista, se puede escribir una unidad que no esté
            campoUnidadAnalito.setPromptText("Unidad");
            campoUnidadAnalito.setPrefWidth(120);

            campoTipoDato.setValue("numerico");
            campoTipoDato.setPrefWidth(130);

            campoValorReferencia.getStyleClass().add("campo-form");
            // Vale cualquier texto, no solo rangos numéricos -- un analito cualitativo puede
            // llevar algo como "No se observan" o "Negativo" en vez de un rango; el cálculo de
            // Normal/Alto/Bajo (ver utilidades.CalculoEstadoUtil) simplemente no se aplica en esos
            // casos, no hace falta nada especial para cargarlo así.
            campoValorReferencia.setPromptText("Ej: 70 - 110 mg/dL, o un texto como \"No se observan\"");
            campoValorReferencia.setPrefWidth(190);

            // Sin Tooltip a propósito (no hay stub/uso de ese control en el resto del proyecto):
            // el texto del botón ("🎨") ya alcanza para que se entienda qué hace.
            botonColor.getStyleClass().add("boton-fila");
            botonColor.setMinWidth(Region.USE_PREF_SIZE);
            botonColor.setOnAction(evt -> editarColores());

            Button botonQuitar = new Button("Quitar");
            botonQuitar.getStyleClass().addAll("boton-fila", "boton-fila-eliminar");
            botonQuitar.setMinWidth(Region.USE_PREF_SIZE); // que "Quitar" no se recorte a "..." si el HBox anda justo de espacio
            botonQuitar.setOnAction(evt -> confirmarQuitarFila(this));

            if (existente != null) {
                campoNombreAnalito.setText(existente.getNombreAnalito());
                campoUnidadAnalito.setValue(existente.getUnidad());
                campoTipoDato.setValue(existente.getTipoDato() == null ? "numerico" : existente.getTipoDato());
                String crudo = existente.getValorReferencia();
                if (TramosTextoUtil.tieneColor(crudo)) {
                    tramosConColor = TramosTextoUtil.parsear(crudo);
                    aplicarModoColor();
                } else {
                    campoValorReferencia.setText(TramosTextoUtil.textoPlano(crudo));
                }
            }

            contenedor = new HBox(10, campoNombreAnalito, campoUnidadAnalito, campoTipoDato,
                    campoValorReferencia, botonColor, botonQuitar);
            contenedor.setAlignment(Pos.CENTER_LEFT);
        }

        /**
         * Abre {@link EditorTramosDialog} sembrado con lo que haya ahora (los tramos con color si
         * ya tenía, o el texto plano del campo como un único tramo negro si todavía no). Al
         * aceptar, si el resultado tiene algún tramo con color de verdad, la fila pasa a "modo
         * color" (campo de texto deshabilitado, se edita solo desde "🎨"); si no, vuelve al modo
         * de texto plano de siempre.
         */
        private void editarColores() {
            List<TramoTexto> siembra = !tramosConColor.isEmpty()
                    ? tramosConColor
                    : java.util.Collections.singletonList(new TramoTexto(campoValorReferencia.getText(), null));
            EditorTramosDialog.mostrar(campoNombreAnalito.getText(), siembra, resultado -> {
                tramosConColor = resultado;
                boolean tieneColorDeVerdad = TramosTextoUtil.tieneColor(TramosTextoUtil.serializar(resultado));
                if (tieneColorDeVerdad) {
                    aplicarModoColor();
                } else {
                    campoValorReferencia.setDisable(false);
                    String plano = TramosTextoUtil.textoPlano(TramosTextoUtil.serializar(resultado));
                    campoValorReferencia.setText(plano);
                    tramosConColor = new ArrayList<>();
                }
            });
        }

        private void aplicarModoColor() {
            campoValorReferencia.setDisable(true);
            campoValorReferencia.setText(TramosTextoUtil.textoPlano(TramosTextoUtil.serializar(tramosConColor))
                    + "  (con colores, tocá 🎨 para editar)");
        }

        boolean estaVacia() {
            String nombre = campoNombreAnalito.getText();
            return nombre == null || nombre.trim().isEmpty();
        }

        Analito construirAnalito(int idAnalisisTipoPadre, int orden) {
            Analito analito = new Analito();
            if (idAnalito != null) {
                analito.setIdAnalito(idAnalito);
            }
            analito.setIdAnalisisTipo(idAnalisisTipoPadre);
            analito.setNombreAnalito(campoNombreAnalito.getText().trim());
            analito.setUnidad(vacioANull(campoUnidadAnalito.getValue()));
            analito.setTipoDato(campoTipoDato.getValue() == null ? "numerico" : campoTipoDato.getValue());
            analito.setValorReferencia(!tramosConColor.isEmpty()
                    ? textoConColoresONull(TramosTextoUtil.serializar(tramosConColor))
                    : vacioANull(campoValorReferencia.getText()));
            analito.setOrdenAnalito(orden);
            return analito;
        }
    }

    private HBox armarBotones(Stage ventana) {
        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonGuardar = new Button("Guardar");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar(ventana));

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        HBox fila = new HBox(12, espaciador, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    private void guardar(Stage ventana) {
        String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "El nombre es obligatorio.");
            return;
        }

        Integer codigo;
        String textoCodigo = campoCodigo.getText() == null ? "" : campoCodigo.getText().trim();
        if (textoCodigo.isEmpty()) {
            codigo = null;
        } else {
            try {
                codigo = Integer.valueOf(textoCodigo);
            } catch (NumberFormatException ex) {
                mostrarAlerta(Alert.AlertType.WARNING, "El código tiene que ser un número.");
                return;
            }
        }

        if (!avisarSiCodigoNoEstaEnNomenclador(codigo)) {
            return;
        }

        AnalisisTipo datos = new AnalisisTipo();
        datos.setNombreAnalisis(nombre);
        datos.setCodigoAnalisis(codigo);
        datos.setCategoria(vacioANull(campoCategoria.getValue()));

        int idAnalisisTipoFinal;
        boolean ok;
        if (analisisTipo == null) {
            datos.setActivo(true);
            Integer id = PuenteEDT.ejecutar(
                    () -> controlador.AnalisisTipoController.crear(null, datos),
                    null);
            ok = id != null;
            idAnalisisTipoFinal = ok ? id : -1;
        } else {
            datos.setIdAnalisisTipo(analisisTipo.getIdAnalisisTipo());
            datos.setActivo(campoActivo.isSelected());
            ok = PuenteEDT.ejecutar(
                    () -> controlador.AnalisisTipoController.actualizar(null, datos),
                    false);
            idAnalisisTipoFinal = analisisTipo.getIdAnalisisTipo();
        }

        if (!ok) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar el examen.");
            return;
        }

        boolean analitosOk = guardarAnalitos(idAnalisisTipoFinal);
        // Si cambió un color, un rango o un nombre, los PDF ya guardados de las órdenes que tienen
        // este examen se vuelven a generar solos (en segundo plano) con lo nuevo.
        reportes.InformesPdfService.examenModificado(idAnalisisTipoFinal);

        if (!analitosOk) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "El examen se guardó, pero hubo un problema al guardar alguno de los analitos.");
            if (alGuardar != null) {
                alGuardar.run();
            }
            return;
        }

        if (alGuardar != null) {
            alGuardar.run();
        }

        vistas.javafx.AvisoRapido.mostrar(ventana, "Examen guardado", null);

        ventana.close();
    }

    /**
     * Si se cargó un código que todavía no está en el nomenclador ({@code prestaciones}), avisa
     * antes de guardar -- el examen se puede guardar igual, pero el precio va a quedar sin
     * calcular hasta que ese código se cargue. Devuelve false si eligió no guardar (para corregir
     * el código), true si hay que seguir con el guardado.
     */
    private boolean avisarSiCodigoNoEstaEnNomenclador(Integer codigo) {
        if (codigo == null) {
            return true;
        }
        List<Prestacion> encontrado = PuenteEDT.ejecutar(
                () -> controlador.NomencladorController.buscar(null, String.valueOf(codigo)),
                java.util.Collections.emptyList());
        if (!encontrado.isEmpty()) {
            return true;
        }

        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                "El código " + codigo + " todavía no está cargado en el nomenclador -- "
                        + "el precio va a quedar sin calcular hasta que se cargue ese código. "
                        + "¿Guardar el examen igual?",
                javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        aviso.setHeaderText(null);
        aviso.setTitle("Código no encontrado");
        vistas.javafx.AlertaUtil.estilizar(aviso);
        java.util.Optional<javafx.scene.control.ButtonType> respuesta = aviso.showAndWait();
        return respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.YES;
    }

    /**
     * Sincroniza los analitos de la pantalla con la base: crea los nuevos, actualiza los
     * existentes, y da de baja los que estaban cargados originalmente pero ya no están en la
     * lista de filas (se sacaron con "Quitar").
     */
    private boolean guardarAnalitos(int idAnalisisTipoPadre) {
        List<Integer> idsConservados = new ArrayList<>();
        boolean todoOk = true;
        int orden = 0;

        for (FilaAnalito fila : filas) {
            if (fila.estaVacia()) {
                continue;
            }
            Analito analito = fila.construirAnalito(idAnalisisTipoPadre, orden++);
            if (fila.idAnalito == null) {
                Integer nuevoId = PuenteEDT.ejecutar(
                        () -> controlador.AnalisisTipoController.crearAnalito(null, analito),
                        null);
                todoOk &= nuevoId != null;
            } else {
                idsConservados.add(fila.idAnalito);
                boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.AnalisisTipoController.actualizarAnalito(null, analito),
                        false);
                todoOk &= ok;
            }
        }

        for (Integer idOriginal : idsAnalitosOriginales) {
            if (!idsConservados.contains(idOriginal)) {
                boolean ok = PuenteEDT.ejecutar(
                        () -> controlador.AnalisisTipoController.eliminarAnalito(null, idOriginal),
                        false);
                todoOk &= ok;
            }
        }

        return todoOk;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setHeaderText(null);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    /**
     * Como {@link #vacioANull}, pero SIN {@code String.trim()}: trim() borra todo caracter menor
     * o igual al espacio, y eso incluye la marca invisible de color (U+0001) con la que arranca
     * una referencia coloreada -- se perdía el color del primer tramo y quedaba guardado, por
     * ejemplo, "16A34A" como si fuera parte del texto (2026-10-08).
     */
    private static String textoConColoresONull(String texto) {
        if (texto == null || utilidades.TramosTextoUtil.textoPlano(texto).trim().isEmpty()) {
            return null;
        }
        return texto;
    }

    private static String vacioANull(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
