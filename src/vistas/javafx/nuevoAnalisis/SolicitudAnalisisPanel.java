package vistas.javafx.nuevoAnalisis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Prestacion;
import vistas.javafx.PuenteEDT;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" para buscar y seleccionar las prestaciones del
 * nomenclador que va a incluir la orden -- rehecho siguiendo el diseño de Figma "Nuevo Análisis
 * - Por parte de la bioquímica" (nodo 246:2842): un campo de búsqueda con sugerencias en vivo
 * en vez de campo + botón "Agregar", cada análisis elegido como una fila individual con su
 * precio y una "×" para sacarlo (en vez de una tabla), y un total en vivo abajo.
 *
 * <p>El precio de cada fila y el total se calculan igual que antes (UB de cada análisis, más un
 * cargo fijo de "acto bioquímico", todo × el valor vigente de la UB) -- ver
 * {@link #actualizarValorUb}, que la pantalla principal llama una vez al cargar los datos.</p>
 */
public class SolicitudAnalisisPanel {

    /**
     * Unidades Bioquímicas fijas del "acto bioquímico" que se suman siempre, además de las UB de
     * cada análisis elegido, para calcular el total (mismo valor que antes vivía en
     * NuevoAnalisisScreen; se duplica acá porque ahora es este panel el que arma el total).
     */
    private static final BigDecimal UB_ACTO_BIOQUIMICO = new BigDecimal(3);

    private static final int MAX_SUGERENCIAS = 8;

    /**
     * Se avisa cada vez que cambia la lista de prestaciones agregadas (alta, baja o vaciado
     * completo), para que la pantalla principal recalcule el estado del botón "Generar Orden".
     */
    public interface SeleccionListener {
        void onSeleccionCambiada();
    }

    private final TextField campoBusqueda = new TextField();
    private final VBox listaSugerencias = new VBox(2);
    private final VBox listaSeleccionados = new VBox(7);
    private final Label placeholderVacio = new Label("Ningún estudio seleccionado aún.");
    private final HBox filaTotal = new HBox();
    private final Label etiquetaTotal = new Label();
    private final Label valorTotal = new Label();

    private final List<Prestacion> seleccionados = new ArrayList<>();
    private final List<SeleccionListener> listenersSeleccion = new ArrayList<>();

    /** Precio vigente de la UB; null si todavía no se cargó (no se muestra precio en ese caso). */
    private BigDecimal valorUbActual;

    public Parent construir() {
        VBox raiz = new VBox(16);
        raiz.getStyleClass().add("tarjeta");

        Label titulo = new Label("Estudios solicitados");
        titulo.getStyleClass().add("titulo-seccion");

        campoBusqueda.getStyleClass().add("campo-form");
        campoBusqueda.setPromptText("Agregar estudio... (código o nombre)");
        campoBusqueda.setMaxWidth(Double.MAX_VALUE);
        campoBusqueda.setOnKeyReleased(evt -> buscarSugerencias());

        listaSugerencias.setVisible(false);
        listaSugerencias.setManaged(false);

        placeholderVacio.setStyle("-fx-text-fill: #8C9490; -fx-font-size: 12.5px;");

        etiquetaTotal.setText("Total");
        etiquetaTotal.setStyle("-fx-text-fill: #1E5C3D; -fx-font-weight: bold; -fx-font-size: 12.5px;");
        valorTotal.setStyle("-fx-text-fill: #1E5C3D; -fx-font-weight: bold; -fx-font-size: 16px;");
        filaTotal.getChildren().addAll(etiquetaTotal, valorTotal);
        filaTotal.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(etiquetaTotal, Priority.ALWAYS);
        filaTotal.getStyleClass().add("barra-total");
        filaTotal.setVisible(false);
        filaTotal.setManaged(false);

        raiz.getChildren().addAll(titulo, campoBusqueda, listaSugerencias, listaSeleccionados, filaTotal);

        renderizarSeleccionados();

        return raiz;
    }

    /**
     * Busca en el nomenclador lo que haya en el campo de búsqueda a cada tecla y arma la lista
     * de sugerencias clickeables debajo del campo. Envuelto en try/catch para que, si algo
     * revienta acá (por ejemplo un problema de conexión a la base), se vea un cartel en vez de
     * que el campo quede "sin hacer nada" -- JavaFX traga las excepciones de los manejadores de
     * eventos y solo las imprime en la consola de NetBeans.
     */
    private void buscarSugerencias() {
        try {
            String texto = campoBusqueda.getText() == null ? "" : campoBusqueda.getText().trim();
            if (texto.length() < 2) {
                ocultarSugerencias();
                return;
            }

            List<Prestacion> encontrados = PuenteEDT.ejecutar(
                    () -> controlador.NomencladorController.buscar(null, texto),
                    Collections.emptyList());

            listaSugerencias.getChildren().clear();
            int mostrados = 0;
            for (Prestacion p : encontrados) {
                if (yaAgregado(p.getCodigo()) || mostrados >= MAX_SUGERENCIAS) {
                    continue;
                }
                listaSugerencias.getChildren().add(armarFilaSugerencia(p));
                mostrados++;
            }

            if (listaSugerencias.getChildren().isEmpty()) {
                Label sinResultados = new Label(
                        encontrados.isEmpty() ? "No se encontró ningún análisis con \"" + texto + "\"."
                                : "Ya agregaste todos los que coinciden con \"" + texto + "\".");
                sinResultados.setStyle("-fx-text-fill: #8C9490; -fx-font-size: 12px; -fx-padding: 6 4 6 4;");
                listaSugerencias.getChildren().add(sinResultados);
            }

            listaSugerencias.setVisible(true);
            listaSugerencias.setManaged(true);
        } catch (RuntimeException e) {
            e.printStackTrace();
            ocultarSugerencias();
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al buscar el análisis");
            alerta.setHeaderText(null);
            alerta.setContentText("Pasó algo inesperado buscando en el nomenclador:\n" + e);
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
        }
    }

    private HBox armarFilaSugerencia(Prestacion p) {
        Label texto = new Label(p.getCodigo() + " - " + p.getNombrePrestacion());
        texto.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #141C19;");
        HBox fila = new HBox();
        fila.getChildren().add(texto);
        fila.getStyleClass().add("fila-sugerencia");
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setOnMouseClicked(evt -> agregarPrestacion(p));
        return fila;
    }

    private void ocultarSugerencias() {
        listaSugerencias.getChildren().clear();
        listaSugerencias.setVisible(false);
        listaSugerencias.setManaged(false);
    }

    private void agregarPrestacion(Prestacion p) {
        if (yaAgregado(p.getCodigo())) {
            return;
        }
        seleccionados.add(p);
        campoBusqueda.setText("");
        ocultarSugerencias();
        renderizarSeleccionados();
        avisarSeleccionCambiada();
    }

    private void quitarPrestacion(Prestacion prestacion) {
        seleccionados.remove(prestacion);
        renderizarSeleccionados();
        avisarSeleccionCambiada();
    }

    private boolean yaAgregado(int codigo) {
        for (Prestacion p : seleccionados) {
            if (p.getCodigo() == codigo) {
                return true;
            }
        }
        return false;
    }

    /**
     * Redibuja la lista de análisis elegidos (una fila por cada uno, con su precio y la "×" para
     * sacarlo), el placeholder de "ningún estudio seleccionado" y la barra de total, según
     * corresponda.
     */
    private void renderizarSeleccionados() {
        listaSeleccionados.getChildren().clear();

        if (seleccionados.isEmpty()) {
            listaSeleccionados.getChildren().add(placeholderVacio);
            filaTotal.setVisible(false);
            filaTotal.setManaged(false);
            return;
        }

        for (Prestacion p : seleccionados) {
            listaSeleccionados.getChildren().add(armarFilaSeleccionada(p));
        }

        etiquetaTotal.setText("Total — " + seleccionados.size() + (seleccionados.size() == 1 ? " estudio" : " estudios"));
        valorTotal.setText("$" + calcularTotal().toPlainString());
        filaTotal.setVisible(true);
        filaTotal.setManaged(true);
    }

    private HBox armarFilaSeleccionada(Prestacion p) {
        Label nombre = new Label(p.getNombrePrestacion());
        nombre.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label precio = new Label(valorUbActual != null ? "$" + calcularPrecio(p).toPlainString() : "");
        precio.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #1E5C3D;");

        Button botonQuitar = new Button("×");
        botonQuitar.getStyleClass().addAll("boton-fila", "boton-fila-eliminar");
        botonQuitar.setOnAction(evt -> quitarPrestacion(p));

        HBox derecha = new HBox(14, precio, botonQuitar);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox();
        fila.getChildren().addAll(nombre, derecha);
        HBox.setHgrow(nombre, Priority.ALWAYS);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("fila-estudio");
        return fila;
    }

    private BigDecimal calcularPrecio(Prestacion p) {
        if (valorUbActual == null || p.getUnidadesBioquimicas() == null) {
            return BigDecimal.ZERO;
        }
        return p.getUnidadesBioquimicas().multiply(valorUbActual).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Suma de UB de todos los análisis elegidos + el acto bioquímico, × el valor vigente de la
     * UB. Es el mismo total sin importar la cobertura -- lo que cambia según la cobertura es
     * quién lo paga, no cuánto es.
     */
    public BigDecimal calcularTotal() {
        if (valorUbActual == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumaUb = BigDecimal.ZERO;
        for (Prestacion p : seleccionados) {
            if (p.getUnidadesBioquimicas() != null) {
                sumaUb = sumaUb.add(p.getUnidadesBioquimicas());
            }
        }
        sumaUb = sumaUb.add(UB_ACTO_BIOQUIMICO);
        return sumaUb.multiply(valorUbActual).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * La pantalla principal la llama una vez, al cargar (después de traer el valor vigente de la
     * UB de la base), para que este panel pueda mostrar precio por fila y el total.
     */
    public void actualizarValorUb(BigDecimal valorUb) {
        this.valorUbActual = valorUb;
        renderizarSeleccionados();
    }

    /**
     * Prestaciones (del nomenclador) agregadas a la orden hasta el momento.
     */
    public List<Prestacion> getPrestacionesSeleccionadas() {
        return new ArrayList<>(seleccionados);
    }

    /**
     * Se llama después de generar una orden con éxito: vacía la lista y el campo de búsqueda
     * para dejarla lista para la próxima orden.
     */
    public void limpiarSeleccion() {
        seleccionados.clear();
        campoBusqueda.setText("");
        ocultarSugerencias();
        renderizarSeleccionados();
        avisarSeleccionCambiada();
    }

    public void addSeleccionListener(SeleccionListener listener) {
        listenersSeleccion.add(listener);
    }

    private void avisarSeleccionCambiada() {
        for (SeleccionListener listener : listenersSeleccion) {
            listener.onSeleccionCambiada();
        }
    }
}
