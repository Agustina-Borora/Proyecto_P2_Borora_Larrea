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
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * Tercer y último paso del flujo "Olvidé mi contraseña": elegir una contraseña nueva. Versión
 * JavaFX de {@code vistas.recuperarContrasena.CambiarContraseña}.
 */
public class CambiarContrasenaPanel {

    public interface ContrasenaGuardadaListener {

        void onContrasenaElegida(String nuevaPassword);
    }

    private final List<ContrasenaGuardadaListener> listenersGuardado = new ArrayList<>();

    private final PasswordField campoNueva = new PasswordField();
    private final PasswordField campoConfirmar = new PasswordField();

    public void addContrasenaGuardadaListener(ContrasenaGuardadaListener listener) {
        listenersGuardado.add(listener);
    }

    public Parent construir() {
        Label titulo = new Label("Nueva contraseña");
        titulo.getStyleClass().add("titulo-pantalla");

        Label subtitulo = new Label("Elegí una contraseña segura para proteger tu cuenta.");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        subtitulo.setWrapText(true);

        Label lblNueva = new Label("Nueva contraseña");
        lblNueva.getStyleClass().add("etiqueta-campo");
        campoNueva.getStyleClass().add("campo-form");

        Label lblConfirmar = new Label("Confirmar contraseña");
        lblConfirmar.getStyleClass().add("etiqueta-campo");
        campoConfirmar.getStyleClass().add("campo-form");
        campoConfirmar.setOnAction(evt -> guardarClicked());

        Button botonGuardar = new Button("Guardar contraseña");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setMaxWidth(Double.MAX_VALUE);
        botonGuardar.setOnAction(evt -> guardarClicked());

        VBox caja = new VBox(14, titulo, subtitulo, lblNueva, campoNueva, lblConfirmar, campoConfirmar,
                botonGuardar);
        caja.setMaxWidth(380);
        caja.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(caja);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: white;");
        return panel;
    }

    private void guardarClicked() {
        String nueva = campoNueva.getText();
        String confirmar = campoConfirmar.getText();

        if (nueva == null || nueva.isEmpty() || confirmar == null || confirmar.isEmpty()) {
            mostrarAlerta("Faltan datos", "Completá los dos campos de contraseña.");
            return;
        }
        if (!nueva.equals(confirmar)) {
            mostrarAlerta("Contraseñas distintas", "Las contraseñas no coinciden.");
            return;
        }
        int largoMinimo = controlador.PasswordController.getLargoMinimoPassword();
        if (nueva.length() < largoMinimo) {
            mostrarAlerta("Contraseña muy corta", "La contraseña debe tener al menos " + largoMinimo + " caracteres.");
            return;
        }

        for (ContrasenaGuardadaListener listener : listenersGuardado) {
            listener.onContrasenaElegida(nueva);
        }
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alerta = new Alert(AlertType.WARNING);
        alerta.setHeaderText(null);
        alerta.setTitle(titulo);
        alerta.setContentText(contenido);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }
}
