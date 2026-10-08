package vistas.javafx.comunes;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.TramoTexto;
import utilidades.TramosTextoUtil;
import vistas.javafx.EstiloApp;
import vistas.javafx.VentanaUtil;

/**
 * Diálogo modal para editar un texto armado de "tramos" con colores (ver
 * {@link utilidades.TramosTextoUtil}) -- lo usan tanto el Catálogo de Exámenes (para el valor de
 * referencia por defecto de un analito) como Cargar Resultados (para un ajuste puntual de un
 * paciente).
 *
 * <p>En vez de un editor de texto enriquecido de verdad (seleccionar una porción del texto y
 * pintarla), que es mucho más complejo de armar bien en JavaFX puro, cada tramo es una fila
 * separada: un campo de texto y un desplegable de color. Alcanza para el caso real (separar en
 * dos o tres partes un valor de referencia, por ejemplo mujer/hombre) sin construir un control
 * nuevo desde cero.</p>
 */
public final class EditorTramosDialog {

    public interface Callback {
        void onAceptar(List<TramoTexto> tramos);
    }

    private EditorTramosDialog() {
    }

    public static void mostrar(String tituloCampo, List<TramoTexto> tramosIniciales, Callback callback) {
        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Colores de \"" + tituloCampo + "\"");

        VBox raiz = new VBox(16);
        raiz.setPadding(new Insets(22));

        Label titulo = new Label("Colores de \"" + tituloCampo + "\"");
        titulo.getStyleClass().add("titulo-pantalla");

        Label ayuda = new Label(
                "Separá el texto en tramos y elegí un color para cada uno -- por ejemplo, un tramo "
                + "\"M:11-14\" en un color y \"H:12-15\" en otro. Un tramo en \"Negro (predeterminado)\" "
                + "se imprime igual que el resto del informe.");
        ayuda.setWrapText(true);
        ayuda.setMaxWidth(460);
        ayuda.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");

        VBox contenedorFilas = new VBox(8);
        List<FilaTramo> filas = new ArrayList<>();

        Label lblVistaPrevia = new Label("Vista previa");
        lblVistaPrevia.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        TextFlow vistaPrevia = new TextFlow();
        VBox cajaVistaPrevia = new VBox(6, lblVistaPrevia, vistaPrevia);
        cajaVistaPrevia.setPadding(new Insets(12));
        cajaVistaPrevia.setStyle("-fx-background-color: #F6F8F7; -fx-background-radius: 8;");

        Runnable actualizarVistaPrevia = () -> {
            List<TramoTexto> actuales = new ArrayList<>();
            for (FilaTramo fila : filas) {
                actuales.add(fila.aTramo());
            }
            vistaPrevia.getChildren().setAll(TramosTextoUtil.aTextFlow(actuales, "13px").getChildren());
        };

        List<TramoTexto> iniciales = tramosIniciales == null || tramosIniciales.isEmpty()
                ? java.util.Collections.singletonList(new TramoTexto("", null))
                : tramosIniciales;
        for (TramoTexto tramo : iniciales) {
            FilaTramo fila = new FilaTramo(tramo, contenedorFilas, filas, actualizarVistaPrevia);
            filas.add(fila);
            contenedorFilas.getChildren().add(fila.contenedor);
        }
        actualizarVistaPrevia.run();

        Button botonAgregar = new Button("+ Agregar tramo");
        botonAgregar.getStyleClass().add("boton-secundario");
        botonAgregar.setOnAction(evt -> {
            FilaTramo fila = new FilaTramo(new TramoTexto("", null), contenedorFilas, filas, actualizarVistaPrevia);
            filas.add(fila);
            contenedorFilas.getChildren().add(fila.contenedor);
            actualizarVistaPrevia.run();
        });

        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonAceptar = new Button("Aceptar");
        botonAceptar.getStyleClass().add("boton-primario");
        botonAceptar.setOnAction(evt -> {
            List<TramoTexto> resultado = new ArrayList<>();
            for (FilaTramo fila : filas) {
                TramoTexto tramo = fila.aTramo();
                if (tramo.getTexto() != null && !tramo.getTexto().trim().isEmpty()) {
                    resultado.add(tramo);
                }
            }
            ventana.close();
            if (callback != null) {
                callback.onAceptar(resultado);
            }
        });

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox filaBotones = new HBox(12, espaciador, botonCancelar, botonAceptar);
        filaBotones.setAlignment(Pos.CENTER_RIGHT);

        raiz.getChildren().addAll(titulo, ayuda, contenedorFilas, botonAgregar, cajaVistaPrevia);

        // Misma idea que en ExamenFormDialog (ver VentanaUtil): todo menos "Cancelar"/"Aceptar"
        // va en un ScrollPane propio, así que si se agregan varios tramos y el contenido no entra
        // en una pantalla chica, se puede bajar en vez de perder el acceso a esos dos botones.
        ScrollPane scrollExterno = new ScrollPane(raiz);
        scrollExterno.getStyleClass().add("config-scroll");
        scrollExterno.setFitToWidth(true);
        scrollExterno.setHbarPolicy(ScrollBarPolicy.NEVER);
        scrollExterno.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        VBox raizExterna = new VBox(scrollExterno, filaBotones);
        raizExterna.getStyleClass().add("pantalla");
        raizExterna.setPadding(new Insets(0, 22, 18, 0));
        VBox.setVgrow(scrollExterno, Priority.ALWAYS);
        filaBotones.setPadding(new Insets(10, 22, 0, 0));

        Scene escena = VentanaUtil.escenaAdaptable(ventana, raizExterna, 520, 460);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.showAndWait();
    }

    /** Una fila editable: el texto del tramo + el desplegable de color + "Quitar". */
    private static final class FilaTramo {
        private final TextField campoTexto = new TextField();
        private final ComboBox<String> comboColor =
                new ComboBox<>(FXCollections.observableArrayList(TramosTextoUtil.PALETA_COLORES.keySet()));
        private final HBox contenedor;
        private boolean quitada = false;

        FilaTramo(TramoTexto inicial, VBox contenedorFilas, List<FilaTramo> listaFilas, Runnable alCambiar) {
            campoTexto.getStyleClass().add("campo-form");
            campoTexto.setPromptText("Ej: M:11-14 mg/dl");
            campoTexto.setPrefWidth(260);
            campoTexto.setText(inicial.getTexto() == null ? "" : inicial.getTexto());
            campoTexto.setOnKeyReleased(evt -> alCambiar.run());

            comboColor.setPrefWidth(170);
            comboColor.setValue(TramosTextoUtil.nombrePorColor(inicial.getColorHex()));
            comboColor.setOnAction(evt -> alCambiar.run());

            Button botonQuitar = new Button("Quitar");
            botonQuitar.getStyleClass().addAll("boton-fila", "boton-fila-eliminar");
            botonQuitar.setMinWidth(Region.USE_PREF_SIZE);

            contenedor = new HBox(10, campoTexto, comboColor, botonQuitar);
            contenedor.setAlignment(Pos.CENTER_LEFT);

            // El handler se conecta después de armar "contenedor" -- una lambda que lo usa antes de
            // que esté asignado no compila (el campo es final, capturado por la lambda).
            botonQuitar.setOnAction(evt -> {
                quitada = true;
                contenedorFilas.getChildren().remove(contenedor);
                listaFilas.remove(this);
                alCambiar.run();
            });
        }

        TramoTexto aTramo() {
            if (quitada) {
                return new TramoTexto("", null);
            }
            String texto = campoTexto.getText() == null ? "" : campoTexto.getText();
            String color = TramosTextoUtil.PALETA_COLORES.get(comboColor.getValue());
            return new TramoTexto(texto, color);
        }
    }
}
