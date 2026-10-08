package vistas.javafx.registros;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import modelo.TramoTexto;
import reportes.AjustesInforme;
import reportes.ResultadosDatos;
import utilidades.TramosTextoUtil;
import vistas.javafx.comunes.EditorTramosDialog;

/**
 * Pestaña "Textos" de la vista previa del informe: un editor sencillo, "como un Word chiquito",
 * para corregir lo que dice ESTE informe sin ir al Catálogo de Exámenes:
 *
 * <ul>
 *   <li>Cambiar el orden de los estudios (↑ ↓), ocultar uno, cambiarle el título y la observación.</li>
 *   <li>En cada renglón: ocultarlo, cambiar el nombre, la unidad, y la referencia con sus colores
 *       (con el mismo editor de colores del Catálogo).</li>
 *   <li>Una nota libre al final del informe.</li>
 * </ul>
 *
 * <p>Todo eso vale solo para esta orden. Si un cambio tiene que quedar para todas las órdenes,
 * cada renglón tiene "Pasar al Catálogo". Los VALORES de los resultados no se editan acá (eso es
 * en Registrar Resultados), para que el informe diga siempre lo que quedó cargado.</p>
 */
public class EditorTextosInformePanel {

    /** Para avisar que hay que pasar un renglón al Catálogo (lo hace la ventana, en segundo plano). */
    public interface PasarACatalogo {
        void pasar(int idAnalito, String nombre, String unidad, String referenciaCruda);
    }

    private final ResultadosDatos originales;
    private final AjustesInforme ajustes;
    private final Consumer<AjustesInforme> alCambiar;
    private final PasarACatalogo pasarACatalogo;
    private final VBox vista = new VBox(14);
    private final VBox listaEstudios = new VBox(12);

    public EditorTextosInformePanel(ResultadosDatos originales, AjustesInforme inicial,
            Consumer<AjustesInforme> alCambiar, PasarACatalogo pasarACatalogo) {
        this.originales = originales;
        this.ajustes = inicial == null ? new AjustesInforme() : inicial.copia();
        this.alCambiar = alCambiar;
        this.pasarACatalogo = pasarACatalogo;
        armar();
    }

    public Node getVista() {
        return vista;
    }

    public AjustesInforme getAjustes() {
        return ajustes.copia();
    }

    private void avisar() {
        if (alCambiar != null) {
            alCambiar.accept(ajustes.copia());
        }
    }

    // ------------------------------------------------------------------ armado

    private void armar() {
        Label ayuda = new Label("Corregí lo que dice ESTE informe sin tocar el Catálogo. Lo que cambies se ve al "
                + "instante en la hoja. Si un cambio tiene que quedar para todas las órdenes, usá \"Pasar al "
                + "Catálogo\" en ese renglón. Los valores de los resultados se corrigen en Registrar Resultados.");
        ayuda.getStyleClass().add("ayuda-diseno");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(Region.USE_PREF_SIZE);

        Label tituloNota = new Label("Nota al final del informe (opcional)");
        tituloNota.getStyleClass().add("titulo-grupo-diseno");
        TextArea nota = new TextArea(ajustes.getNotaFinal() == null ? "" : ajustes.getNotaFinal());
        nota.setPromptText("Ej: Se sugiere repetir el estudio en 30 días. / Muestra levemente hemolizada.");
        nota.setPrefRowCount(3);
        nota.setWrapText(true);
        nota.textProperty().addListener((o, a, n) -> {
            ajustes.setNotaFinal(n);
            avisar();
        });
        VBox cajaNota = caja(tituloNota, nota);

        dibujarEstudios();
        vista.getChildren().addAll(ayuda, listaEstudios, cajaNota);
    }

    /** Estudios en el orden actual (el elegido con ↑↓, o el de siempre). */
    private List<ResultadosDatos.Estudio> estudiosOrdenados() {
        List<ResultadosDatos.Estudio> resto = new ArrayList<>(originales.getEstudios());
        List<ResultadosDatos.Estudio> orden = new ArrayList<>();
        for (Integer id : ajustes.getOrden()) {
            for (ResultadosDatos.Estudio e : resto) {
                if (e.getIdPedidoAnalisis() == id) {
                    orden.add(e);
                    resto.remove(e);
                    break;
                }
            }
        }
        orden.addAll(resto);
        return orden;
    }

    private void dibujarEstudios() {
        listaEstudios.getChildren().clear();
        List<ResultadosDatos.Estudio> estudios = estudiosOrdenados();
        if (estudios.isEmpty()) {
            Label vacio = new Label("Esta orden todavía no tiene estudios terminados.");
            vacio.getStyleClass().add("ayuda-diseno");
            listaEstudios.getChildren().add(vacio);
        }
        for (int i = 0; i < estudios.size(); i++) {
            listaEstudios.getChildren().add(tarjetaEstudio(estudios, i));
        }
    }

    private void mover(List<ResultadosDatos.Estudio> estudios, int desde, int hacia) {
        List<Integer> ids = new ArrayList<>();
        for (ResultadosDatos.Estudio e : estudios) {
            ids.add(e.getIdPedidoAnalisis());
        }
        Integer id = ids.remove(desde);
        ids.add(hacia, id);
        ajustes.setOrden(ids);
        dibujarEstudios();
        avisar();
    }

    private Node tarjetaEstudio(List<ResultadosDatos.Estudio> estudios, int indice) {
        ResultadosDatos.Estudio e = estudios.get(indice);
        int idPa = e.getIdPedidoAnalisis();

        Button subir = botonChico("↑", "Subir este estudio");
        subir.setDisable(indice == 0);
        subir.setOnAction(evt -> mover(estudios, indice, indice - 1));
        Button bajar = botonChico("↓", "Bajar este estudio");
        bajar.setDisable(indice == estudios.size() - 1);
        bajar.setOnAction(evt -> mover(estudios, indice, indice + 1));

        TextField titulo = new TextField(ajustes.getTituloEstudio(idPa) != null ? ajustes.getTituloEstudio(idPa) : e.getNombre());
        titulo.getStyleClass().add("campo-form");
        titulo.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(titulo, Priority.ALWAYS);
        titulo.textProperty().addListener((o, a, n) -> {
            ajustes.setTituloEstudio(idPa, n == null || n.trim().isEmpty() || n.equals(e.getNombre()) ? null : n);
            avisar();
        });

        CheckBox mostrar = new CheckBox("Mostrar");
        mostrar.setSelected(!ajustes.isEstudioOculto(idPa));
        mostrar.setStyle("-fx-text-fill: #141C19; -fx-font-size: 12px;");
        mostrar.setTooltip(new Tooltip("Destildá para que este estudio no salga en el informe"));
        mostrar.setOnAction(evt -> {
            ajustes.setEstudioOculto(idPa, !mostrar.isSelected());
            avisar();
        });

        HBox cabecera = new HBox(6, subir, bajar, titulo, mostrar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        TextArea obs = new TextArea(ajustes.getObservacion(idPa) != null ? ajustes.getObservacion(idPa)
                : (e.getObservacion() == null ? "" : e.getObservacion()));
        obs.setPromptText("Observación de este estudio (opcional)");
        obs.setPrefRowCount(2);
        obs.setWrapText(true);
        obs.textProperty().addListener((o, a, n) -> {
            String original = e.getObservacion() == null ? "" : e.getObservacion();
            ajustes.setObservacion(idPa, n.equals(original) ? null : n);
            avisar();
        });

        VBox filas = new VBox(8);
        Label encabezadoFilas = new Label("Renglones (destildá para ocultar uno)");
        encabezadoFilas.getStyleClass().add("ayuda-diseno");
        filas.getChildren().add(encabezadoFilas);
        for (ResultadosDatos.Fila f : e.getFilas()) {
            filas.getChildren().add(renglon(idPa, f));
        }

        return caja(cabecera, obs, filas);
    }

    private Node renglon(int idPa, ResultadosDatos.Fila f) {
        int idAn = f.getIdAnalito();

        CheckBox visible = new CheckBox();
        visible.setSelected(!ajustes.isFilaOculta(idPa, idAn));
        visible.setTooltip(new Tooltip("Mostrar u ocultar este renglón en el informe"));

        TextField nombre = new TextField(valorO(ajustes.getNombreFila(idPa, idAn), f.getNombreParametro()));
        nombre.getStyleClass().add("campo-form");
        nombre.setPromptText("Nombre");
        HBox.setHgrow(nombre, Priority.ALWAYS);

        TextField unidad = new TextField(valorO(ajustes.getUnidadFila(idPa, idAn), f.getUnidad()));
        unidad.getStyleClass().add("campo-form");
        unidad.setPromptText("Unidad");
        unidad.setPrefWidth(80);
        unidad.setMaxWidth(90);

        Label valor = new Label("= " + (f.getValor() == null ? "-" : f.getValor()));
        valor.setStyle("-fx-font-weight: bold; -fx-text-fill: #141C19;");
        valor.setMinWidth(Region.USE_PREF_SIZE);

        HBox fila1 = new HBox(6, visible, nombre, valor, unidad);
        fila1.setAlignment(Pos.CENTER_LEFT);

        // Referencia (con colores) + botones
        VBox contenedorRef = new VBox();
        Runnable dibujarRef = () -> {
            String cruda = ajustes.getReferenciaFila(idPa, idAn);
            List<TramoTexto> tramos = cruda != null ? TramosTextoUtil.parsear(cruda) : f.getTramosReferencia();
            TextFlow flujo = TramosTextoUtil.aTextFlow(tramos, "12px");
            Label prefijo = new Label("VR: ");
            prefijo.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 12px;");
            HBox h = new HBox(2, prefijo, flujo);
            h.setAlignment(Pos.CENTER_LEFT);
            contenedorRef.getChildren().setAll(h);
        };
        dibujarRef.run();

        Button colores = new Button("🎨 Referencia y colores");
        colores.getStyleClass().add("enlace-accion");
        colores.setOnAction(evt -> {
            String cruda = ajustes.getReferenciaFila(idPa, idAn);
            List<TramoTexto> actuales = cruda != null ? TramosTextoUtil.parsear(cruda)
                    : new ArrayList<>(f.getTramosReferencia());
            if (actuales.isEmpty()) {
                actuales.add(new TramoTexto(f.getReferencia() == null ? "" : f.getReferencia(), null));
            }
            EditorTramosDialog.mostrar(nombre.getText(), actuales, resultado -> {
                ajustes.setReferenciaFila(idPa, idAn, TramosTextoUtil.serializar(resultado));
                dibujarRef.run();
                avisar();
            });
        });

        Button original = new Button("↺ Original");
        original.getStyleClass().add("enlace-accion");
        original.setTooltip(new Tooltip("Volver a lo que dice el Catálogo para este renglón"));

        Button catalogo = new Button("Pasar al Catálogo");
        catalogo.getStyleClass().add("enlace-accion");
        catalogo.setTooltip(new Tooltip("Guardar nombre, unidad y referencia de este renglón en el Catálogo, "
                + "para todas las órdenes"));
        catalogo.setDisable(idAn <= 0);
        catalogo.setOnAction(evt -> {
            Alert pregunta = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Guardar el nombre, la unidad y la referencia de \"" + nombre.getText() + "\" en el Catálogo?\n\n"
                            + "Va a quedar así para TODAS las órdenes (y los PDF ya guardados se actualizan solos).",
                    ButtonType.YES, ButtonType.NO);
            pregunta.setHeaderText(null);
            pregunta.setTitle("Pasar al Catálogo");
            vistas.javafx.AlertaUtil.estilizar(pregunta);
            pregunta.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES && pasarACatalogo != null) {
                    String cruda = ajustes.getReferenciaFila(idPa, idAn);
                    pasarACatalogo.pasar(idAn, nombre.getText(), unidad.getText(), cruda);
                }
            });
        });

        HBox fila2 = new HBox(4, contenedorRef);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(2, colores, original, espacio, catalogo);
        acciones.setAlignment(Pos.CENTER_LEFT);

        nombre.textProperty().addListener((o, a, n) -> {
            ajustes.setNombreFila(idPa, idAn, n.equals(f.getNombreParametro()) ? null : n);
            avisar();
        });
        unidad.textProperty().addListener((o, a, n) -> {
            String orig = f.getUnidad() == null ? "" : f.getUnidad();
            ajustes.setUnidadFila(idPa, idAn, n.equals(orig) ? null : n);
            avisar();
        });
        visible.setOnAction(evt -> {
            ajustes.setFilaOculta(idPa, idAn, !visible.isSelected());
            nombre.setDisable(!visible.isSelected());
            avisar();
        });
        nombre.setDisable(!visible.isSelected());
        original.setOnAction(evt -> {
            ajustes.limpiarFila(idPa, idAn);
            nombre.setText(f.getNombreParametro());
            unidad.setText(f.getUnidad() == null ? "" : f.getUnidad());
            visible.setSelected(true);
            nombre.setDisable(false);
            dibujarRef.run();
            avisar();
        });

        VBox caja = new VBox(4, fila1, fila2, acciones);
        caja.setPadding(new Insets(8));
        caja.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        return caja;
    }

    // ------------------------------------------------------------------ piezas

    private static VBox caja(Node... hijos) {
        VBox c = new VBox(8, hijos);
        c.setPadding(new Insets(12));
        c.setStyle("-fx-background-color: #F6F8F7; -fx-background-radius: 12;");
        return c;
    }

    private static Button botonChico(String texto, String ayuda) {
        Button b = new Button(texto);
        b.getStyleClass().add("segmento");
        b.setTooltip(new Tooltip(ayuda));
        b.setMinWidth(30);
        return b;
    }

    private static String valorO(String cambiado, String original) {
        return cambiado != null ? cambiado : (original == null ? "" : original);
    }
}
