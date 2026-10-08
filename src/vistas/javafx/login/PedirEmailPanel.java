package vistas.javafx.login;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Primer paso del flujo "Olvidé mi contraseña": pide el email de la cuenta antes de mandar el
 * código de verificación. Versión JavaFX de {@code vistas.recuperarContrasena.PedirEmailPanel}.
 */
public class PedirEmailPanel {

    public interface EmailIngresadoListener {

        void onEmailIngresado(String email);
    }

    public interface VolverListener {

        void onVolverClicked();
    }

    /**
     * Validación de formato básica, sólo para dar una respuesta inmediata ante un typo evidente
     * (falta el "@", falta el dominio, etc.) antes de consultar la base de datos.
     */
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final List<EmailIngresadoListener> listenersEmail = new ArrayList<>();
    private final List<VolverListener> listenersVolver = new ArrayList<>();

    private final TextField campoEmail = new TextField();
    private final Button botonVolver = new Button("← Regresar al login");
    private final Button botonEnviar = new Button("Enviar");

    public void addEmailIngresadoListener(EmailIngresadoListener listener) {
        listenersEmail.add(listener);
    }

    public void addVolverListener(VolverListener listener) {
        listenersVolver.add(listener);
    }

    public Parent construir() {
        Label titulo = new Label("Recuperar contraseña");
        titulo.getStyleClass().add("titulo-pantalla");

        Label subtitulo = new Label("Ingresá el email de tu cuenta para mandarte un código de verificación.");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        subtitulo.setWrapText(true);

        Label lblEmail = new Label("Email");
        lblEmail.getStyleClass().add("etiqueta-campo");
        campoEmail.getStyleClass().add("campo-form");
        campoEmail.setPromptText("tu.email@ejemplo.com");
        campoEmail.setOnAction(evt -> enviarClicked());

        botonVolver.getStyleClass().add("boton-fila");
        botonVolver.setOnAction(evt -> {
            for (VolverListener listener : listenersVolver) {
                listener.onVolverClicked();
            }
        });

        botonEnviar.getStyleClass().add("boton-primario");
        botonEnviar.setOnAction(evt -> enviarClicked());

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox filaBotones = new HBox(botonVolver, espaciador, botonEnviar);
        filaBotones.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(14, titulo, subtitulo, lblEmail, campoEmail, filaBotones);
        caja.setMaxWidth(420);
        caja.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(caja);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: white;");
        return panel;
    }

    private void enviarClicked() {
        String email = campoEmail.getText() == null ? "" : campoEmail.getText().trim();
        if (email.isEmpty()) {
            mostrarAlerta("Falta el email", "Ingresá el email de tu cuenta.");
            return;
        }
        if (!PATRON_EMAIL.matcher(email).matches()) {
            mostrarAlerta("Email inválido", "Ingresá un email válido (por ejemplo, nombre@dominio.com).");
            return;
        }

        for (EmailIngresadoListener listener : listenersEmail) {
            listener.onEmailIngresado(email);
        }
    }

    /**
     * Activa o desactiva el estado de "enviando" mientras se manda el código por email -- una
     * llamada de red que puede tardar varios segundos. Mientras está activo se deshabilitan los
     * controles para que no se pueda disparar el mismo pedido dos veces haciendo doble click, y
     * el texto del botón deja en claro que el pedido ya está en curso.
     */
    public void setEnviando(boolean enviando) {
        botonEnviar.setDisable(enviando);
        botonEnviar.setText(enviando ? "Enviando..." : "Enviar");
        botonVolver.setDisable(enviando);
        campoEmail.setDisable(enviando);
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
