package vistas.javafx.login;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Segundo paso del flujo "Olvidé mi contraseña": ingreso del código de 6 dígitos enviado por
 * email. Versión JavaFX de {@code vistas.recuperarContrasena.TokenDeConfirmacion} -- la
 * navegación automática entre casillas (avanzar al completar un dígito, retroceder con Backspace
 * sobre una casilla vacía) se rearma acá con {@link TextFormatter} en vez del {@code
 * DocumentFilter} que usaba la versión Swing.
 */
public class TokenConfirmacionPanel {

    public interface CodigoVerificadoListener {

        void onCodigoIngresado(String codigo);
    }

    private final List<CodigoVerificadoListener> listenersCodigo = new ArrayList<>();

    private final TextField[] casillas = new TextField[6];

    public void addCodigoVerificadoListener(CodigoVerificadoListener listener) {
        listenersCodigo.add(listener);
    }

    public Parent construir() {
        Label titulo = new Label("Revisá tu email");
        titulo.getStyleClass().add("titulo-pantalla");

        Label subtitulo = new Label("Te enviamos un código de 6 dígitos a tu dirección de correo registrada.");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        subtitulo.setWrapText(true);

        HBox filaCasillas = new HBox(8);
        filaCasillas.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < casillas.length; i++) {
            casillas[i] = crearCasillaDigito();
            filaCasillas.getChildren().add(casillas[i]);
        }
        for (int i = 0; i < casillas.length; i++) {
            TextField anterior = i > 0 ? casillas[i - 1] : null;
            TextField siguiente = i + 1 < casillas.length ? casillas[i + 1] : null;
            conectarNavegacion(casillas[i], anterior, siguiente);
        }

        Button botonVerificar = new Button("Verificar código");
        botonVerificar.getStyleClass().add("boton-primario");
        botonVerificar.setOnAction(evt -> verificarClicked());

        VBox caja = new VBox(16, titulo, subtitulo, filaCasillas, botonVerificar);
        caja.setMaxWidth(420);
        caja.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(caja);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: white;");
        return panel;
    }

    private TextField crearCasillaDigito() {
        TextField campo = new TextField();
        campo.getStyleClass().add("campo-form");
        campo.setPrefWidth(48);
        campo.setPrefHeight(48);
        // TextField no tiene setAlignment (a diferencia de un Labeled) -- centrar el texto
        // adentro de la casilla se hace con el atributo CSS -fx-alignment.
        campo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
        // Sólo deja que la casilla quede vacía o con un único dígito numérico (mismo espíritu
        // que el FiltroUnDigito de la versión Swing, simplificado: acá alcanza con rechazar
        // cualquier otro cambio en vez de reescribirlo).
        campo.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("[0-9]?") ? cambio : null));
        return campo;
    }

    private void conectarNavegacion(TextField actual, TextField anterior, TextField siguiente) {
        actual.textProperty().addListener((obs, textoViejo, textoNuevo) -> {
            if (textoNuevo != null && textoNuevo.length() == 1 && siguiente != null) {
                siguiente.requestFocus();
                siguiente.selectAll();
            }
        });
        actual.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.BACK_SPACE && actual.getText().isEmpty() && anterior != null) {
                anterior.requestFocus();
                anterior.selectAll();
            }
        });
    }

    private void verificarClicked() {
        StringBuilder codigo = new StringBuilder();
        for (TextField casilla : casillas) {
            codigo.append(casilla.getText() == null ? "" : casilla.getText().trim());
        }

        if (codigo.length() != 6) {
            Alert alerta = new Alert(AlertType.WARNING);
            alerta.setHeaderText(null);
            alerta.setTitle("Código incompleto");
            alerta.setContentText("Completá los 6 dígitos del código.");
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
            return;
        }

        for (CodigoVerificadoListener listener : listenersCodigo) {
            listener.onCodigoIngresado(codigo.toString());
        }
    }
}
