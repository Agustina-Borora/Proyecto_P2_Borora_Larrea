package vistas.javafx.catalogo;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import modelo.ValorUb;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;

/**
 * Formulario modal para cargar un nuevo valor de la Unidad Bioquímica (UB), abierto desde
 * {@link CatalogoExamenesScreen}. Siempre es un Alta -- {@code valor_ub} es un histórico, nunca
 * se edita ni se borra una fila vieja: se carga una nueva, vigente desde hoy, y el resto de la
 * app (el cálculo de precios) agarra sola la más reciente.
 */
public class ValorUbFormDialog {

    private final Runnable alGuardar;
    private final TextField campoValor = new TextField();

    public ValorUbFormDialog(Runnable alGuardar) {
        this.alGuardar = alGuardar;
    }

    public void mostrar() {
        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Actualizar valor de la UB");
        ventana.setResizable(false);

        VBox raiz = new VBox(18);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(24));

        Label titulo = new Label("Actualizar valor de la UB");
        titulo.getStyleClass().add("titulo-pantalla");

        Label ayuda = new Label("Se carga con la fecha de hoy. Los precios de todos los exámenes "
                + "se recalculan solos a partir de este valor -- no hace falta tocar cada examen.");
        ayuda.getStyleClass().add("subtitulo-pantalla");
        ayuda.setWrapText(true);
        ayuda.setPrefWidth(380);

        Label vigenteActual = new Label(textoVigenteActual());

        GridPane grilla = new GridPane();
        grilla.setHgap(14);
        grilla.setVgap(12);
        Label etiquetaValor = new Label("Nuevo valor ($) *");
        etiquetaValor.getStyleClass().add("etiqueta-campo");
        campoValor.getStyleClass().add("campo-form");
        campoValor.setPrefWidth(200);
        grilla.add(etiquetaValor, 0, 0);
        grilla.add(campoValor, 1, 0);

        HBox botones = armarBotones(ventana);

        raiz.getChildren().addAll(titulo, ayuda, vigenteActual, grilla, botones);

        Scene escena = new Scene(raiz, 440, 320);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        vistas.javafx.VentanaUtil.animarAparicion(ventana, raiz);
        ventana.showAndWait();
    }

    private String textoVigenteActual() {
        ValorUb vigente = PuenteEDT.ejecutar(
                () -> controlador.ValorUbController.obtenerVigenteCompleto(null),
                null);
        if (vigente == null) {
            return "Todavía no hay ningún valor cargado.";
        }
        return "Valor actual: $" + vigente.getValor().toPlainString() + " (desde "
                + vigente.getVigenteDesde().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
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
        String texto = campoValor.getText() == null ? "" : campoValor.getText().trim().replace(",", ".");
        if (texto.isEmpty()) {
            mostrarAlerta("El valor es obligatorio.");
            return;
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            mostrarAlerta("Ingresá un número válido (ej: 1800.00).");
            return;
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarAlerta("El valor tiene que ser mayor a cero.");
            return;
        }

        Integer id = PuenteEDT.ejecutar(
                () -> controlador.ValorUbController.crear(null, valor),
                null);
        if (id == null) {
            mostrarAlerta("No se pudo guardar el nuevo valor.");
            return;
        }

        if (alGuardar != null) {
            alGuardar.run();
        }

        vistas.javafx.AvisoRapido.mostrar(ventana, "Valor de la UB actualizado",
                "Los precios de todos los exámenes ya se recalcularon.");

        ventana.close();
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje);
        alerta.setHeaderText(null);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }
}
