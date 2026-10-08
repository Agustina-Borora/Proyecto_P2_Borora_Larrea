package vistas.javafx.registros;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import vistas.javafx.PuenteEDT;

/**
 * Popup modal "Editar Orden" (Figma: nodo 246:1434), abierto desde {@link RegistrosScreen} al
 * presionar el ícono de lápiz de una fila. A diferencia de {@code vistas.pacientes.EditarPaciente}
 * (que sólo toca al paciente), acá se edita todo junto en una sola pantalla -- datos del
 * paciente, médico derivante, observación del análisis y su estado -- y se guarda todo en una
 * sola transacción vía {@link controlador.RegistroController#guardarEdicionOrden}, para no dejar
 * la orden a medio actualizar si algo falla a mitad de camino.
 */
public class EditarOrdenDialog {

    private static final String[] ESTADOS_RAW = {"pendiente", "en_proceso", "completado"};
    private static final String[] ESTADOS_TEXTO = {"Pendiente", "Procesando", "Completado"};

    private final DetalleOrden detalle;
    private final Runnable alGuardar;

    private final TextField campoNombre = new TextField();
    private final TextField campoDni = new TextField();
    private final TextField campoCelular = new TextField();
    private final TextField campoEmail = new TextField();
    private final TextField campoMedico = new TextField();
    private final TextField campoObservaciones = new TextField();
    private final Button[] botonesEstado = new Button[ESTADOS_RAW.length];

    /**
     * Estado crudo elegido en este momento -- arranca en el estado actual de la orden (incluso si
     * es "urgente" o "cancelado", que no tienen botón propio acá) y solo cambia si se toca uno de
     * los 3 botones, para no pisarlo por accidente con solo abrir y guardar sin tocar nada.
     */
    private String estadoElegido;

    public EditarOrdenDialog(DetalleOrden detalle, Runnable alGuardar) {
        this.detalle = detalle;
        this.alGuardar = alGuardar;
        this.estadoElegido = detalle.getEstadoAnalisisRaw();
    }

    public void mostrar() {
        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Editar Orden");
        ventana.setResizable(false);

        VBox raiz = new VBox(18);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(24));
        raiz.setPrefWidth(560);

        Label titulo = new Label("Editar Orden #" + valorOGuion(detalle.getNumeroOrden()));
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 18px;");

        precargarCampos();

        raiz.getChildren().addAll(titulo, armarCampos(), armarSeccionEstado(), armarBotones(ventana));

        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        vistas.javafx.VentanaUtil.animarAparicion(ventana, raiz);
        ventana.showAndWait();
    }

    private void precargarCampos() {
        campoNombre.setText(valorVacio(detalle.getPaciente()));
        campoDni.setText(valorVacio(detalle.getDni()));
        campoCelular.setText(valorVacio(detalle.getTelefono()));
        campoEmail.setText(valorVacio(detalle.getEmail()));
        campoMedico.setText(valorVacio(detalle.getMedicoDerivante()));
        campoObservaciones.setText(valorVacio(detalle.getObservacion()));
    }

    private GridPane armarCampos() {
        GridPane grilla = new GridPane();
        grilla.setHgap(20);
        grilla.setVgap(14);

        agregarCampo(grilla, 0, 0, "Nombre", campoNombre);
        agregarCampo(grilla, 1, 0, "DNI", campoDni);
        agregarCampo(grilla, 0, 1, "Celular", campoCelular);
        agregarCampo(grilla, 1, 1, "Email", campoEmail);
        agregarCampo(grilla, 0, 2, "Médico derivante", campoMedico);
        agregarCampo(grilla, 1, 2, "Observaciones", campoObservaciones);

        for (Node hijo : grilla.getChildren()) {
            GridPane.setHgrow(hijo, Priority.ALWAYS);
        }
        return grilla;
    }

    private void agregarCampo(GridPane grilla, int columna, int fila, String etiquetaTexto, TextField campo) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        campo.getStyleClass().add("campo-form");
        campo.setPrefWidth(240);
        VBox caja = new VBox(6, etiqueta, campo);
        grilla.add(caja, columna, fila);
    }

    private VBox armarSeccionEstado() {
        Label etiqueta = new Label("Estado");
        etiqueta.getStyleClass().add("etiqueta-campo");

        HBox fila = new HBox(10);
        for (int i = 0; i < ESTADOS_RAW.length; i++) {
            int indice = i;
            Button boton = new Button(ESTADOS_TEXTO[i]);
            boton.setOnAction(evt -> elegirEstado(ESTADOS_RAW[indice]));
            botonesEstado[i] = boton;
            fila.getChildren().add(boton);
        }
        actualizarEstiloBotonesEstado();

        return new VBox(8, etiqueta, fila);
    }

    private void elegirEstado(String estadoRaw) {
        estadoElegido = estadoRaw;
        actualizarEstiloBotonesEstado();
    }

    private void actualizarEstiloBotonesEstado() {
        for (int i = 0; i < ESTADOS_RAW.length; i++) {
            botonesEstado[i].getStyleClass().removeAll("boton-primario", "boton-secundario");
            botonesEstado[i].getStyleClass().add(
                    ESTADOS_RAW[i].equals(estadoElegido) ? "boton-primario" : "boton-secundario");
        }
    }

    private HBox armarBotones(Stage ventana) {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonGuardar = new Button("Guardar Cambios");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar(ventana));

        HBox fila = new HBox(12, espaciador, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    private void guardar(Stage ventana) {
        String estadoFinal = estadoElegido != null ? estadoElegido : detalle.getEstadoAnalisisRaw();
        boolean ok = PuenteEDT.ejecutar(
                () -> controlador.RegistroController.guardarEdicionOrden(null,
                        detalle.getIdPaciente(), detalle.getIdPedido(), detalle.getIdPedidoAnalisis(),
                        campoNombre.getText(), campoDni.getText(), campoCelular.getText(), campoEmail.getText(),
                        campoMedico.getText(), campoObservaciones.getText(), estadoFinal),
                false);
        if (ok) {
            ventana.close();
            if (alGuardar != null) {
                alGuardar.run();
            }
        }
    }

    private static String valorVacio(String texto) {
        return texto == null ? "" : texto;
    }

    private static String valorOGuion(String texto) {
        return texto == null || texto.trim().isEmpty() ? "-" : texto;
    }
}
