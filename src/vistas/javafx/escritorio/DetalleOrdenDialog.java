package vistas.javafx.escritorio;

import java.text.SimpleDateFormat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.DetalleOrden;
import vistas.javafx.EstiloApp;

/**
 * Popup modal de "Detalle de Orden", abierto desde {@link EscritorioScreen} al presionar "Ver"
 * en una fila de la tabla de últimas órdenes. Reemplaza a
 * {@link vistas.escritorio.EscritorioDetalledeOrden}.
 */
public class DetalleOrdenDialog {

    private final DetalleOrden detalle;
    private final Runnable alCargarResultados;

    public DetalleOrdenDialog(DetalleOrden detalle, Runnable alCargarResultados) {
        this.detalle = detalle;
        this.alCargarResultados = alCargarResultados;
    }

    public void mostrar() {
        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Detalle de Orden");
        ventana.setResizable(false);

        VBox raiz = new VBox(18);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(24));
        raiz.setPrefWidth(560);

        raiz.getChildren().addAll(armarEncabezado(), armarSeccionPaciente(), armarSeccionAnalisis(), armarBotones(ventana));

        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        vistas.javafx.VentanaUtil.animarAparicion(ventana, raiz);
        ventana.showAndWait();
    }

    private HBox armarEncabezado() {
        Label numero = new Label("Orden #" + (detalle.getNumeroOrden() != null ? detalle.getNumeroOrden() : "-"));
        numero.getStyleClass().add("titulo-pantalla");
        numero.setStyle("-fx-font-size: 20px;");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Label badgeEstado = new Label(detalle.getEstado() != null ? detalle.getEstado() : "-");
        badgeEstado.getStyleClass().add("pill-activo");

        Label badgeCobertura = new Label(detalle.getCobertura() != null ? detalle.getCobertura() : "Particular");
        badgeCobertura.getStyleClass().add("pill-inactivo");

        HBox fila = new HBox(10, numero, espaciador, badgeCobertura, badgeEstado);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox armarSeccionPaciente() {
        Label titulo = new Label("Paciente");
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 16px;");

        GridPane grilla = new GridPane();
        grilla.setHgap(24);
        grilla.setVgap(8);

        agregarDato(grilla, 0, 0, "D.N.I", detalle.getDni());
        agregarDato(grilla, 1, 0, "Apellido y Nombre", detalle.getPaciente());
        agregarDato(grilla, 0, 1, "Celular", detalle.getTelefono());
        agregarDato(grilla, 1, 1, "Email", detalle.getEmail());
        agregarDato(grilla, 0, 2, "Edad", String.valueOf(detalle.getEdad()));
        agregarDato(grilla, 1, 2, "Médico Derivante",
                detalle.getMedicoDerivante() != null ? detalle.getMedicoDerivante() : "-");

        VBox seccion = new VBox(10, titulo, grilla);
        seccion.getStyleClass().add("tarjeta");
        return seccion;
    }

    private VBox armarSeccionAnalisis() {
        Label titulo = new Label("Análisis");
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 16px;");

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        GridPane grilla = new GridPane();
        grilla.setHgap(24);
        grilla.setVgap(8);

        agregarDato(grilla, 0, 0, "Fecha", detalle.getFecha() != null ? formatoFecha.format(detalle.getFecha()) : "-");
        agregarDato(grilla, 1, 0, "Examen", detalle.getExamen());

        Label etiquetaObs = new Label("Observación");
        etiquetaObs.getStyleClass().add("etiqueta-campo");
        Label valorObs = new Label(detalle.getObservacion() != null && !detalle.getObservacion().trim().isEmpty()
                ? detalle.getObservacion() : "-");
        VBox filaObs = new VBox(4, etiquetaObs, valorObs);

        VBox seccion = new VBox(10, titulo, grilla, filaObs);
        seccion.getStyleClass().add("tarjeta");
        return seccion;
    }

    private void agregarDato(GridPane grilla, int columna, int fila, String etiquetaTexto, String valorTexto) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        Label valor = new Label(valorTexto == null ? "-" : valorTexto);
        VBox caja = new VBox(4, etiqueta, valor);
        grilla.add(caja, columna, fila);
    }

    private HBox armarBotones(Stage ventana) {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonCerrar = new Button("Cerrar");
        botonCerrar.getStyleClass().add("boton-secundario");
        botonCerrar.setOnAction(evt -> ventana.close());

        HBox fila = new HBox(12, espaciador, botonCerrar);
        fila.setAlignment(Pos.CENTER_RIGHT);

        boolean completado = "completado".equals(detalle.getEstadoAnalisisRaw());
        if (!completado) {
            Button botonCargarResultados = new Button("Cargar Resultados");
            botonCargarResultados.getStyleClass().add("boton-primario");
            botonCargarResultados.setOnAction(evt -> {
                ventana.close();
                if (alCargarResultados != null) {
                    alCargarResultados.run();
                }
            });
            fila.getChildren().add(1, botonCargarResultados);
        }
        return fila;
    }
}
