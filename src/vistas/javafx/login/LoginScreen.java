package vistas.javafx.login;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vistas.javafx.AvisoStage;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla de inicio de sesión, en JavaFX puro (sin FXML), que reemplaza al {@code JFrame} de
 * {@code vista.formulariosPrincipales.Login} (que a su vez seguía usando {@code GroupLayout}
 * generado por el editor visual de NetBeans). La autenticación en sí sigue viviendo en {@code
 * controlador.LoginController}/{@code dao.Usuario} -- esta clase sólo arma la pantalla y llama a
 * ese controlador, mismo criterio que el resto de las pantallas ya migradas.
 *
 * <p>El panel izquierdo del diseño original ({@code jPanel2}, blanco y vacío -- a juzgar por
 * cómo quedó, pensado para un logo o una imagen que nunca se puso) se reemplaza acá por un panel
 * de marca con el color y la tipografía que ya usa el resto de la app ({@link EstiloApp}), en vez
 * de dejarlo en blanco. El formulario de la derecha pasa a usar los mismos estilos que cualquier
 * otro formulario de la app ({@code campo-form}, {@code etiqueta-campo}, {@code boton-primario})
 * en vez del verde oscuro propio que tenía el Login viejo, para que no se note el salto de
 * pantalla a pantalla.</p>
 */
public class LoginScreen {

    /**
     * Se dispara cuando el login fue exitoso (usuario y contraseña correctos). Quien construye
     * esta pantalla decide qué hacer después (cerrar esta ventana y abrir la principal, etc.).
     */
    public interface LoginExitosoListener {

        void onLoginExitoso();
    }

    private final LoginExitosoListener alIniciarSesion;

    // Mes 8: antes sólo aceptaba el email -- la clienta avisó que cansa tener que escribirlo
    // siempre, así que ahora también se puede entrar con el nombre de pila (ver dao.Usuario#
    // ingresar para el detalle de cómo se resuelve, incluido qué pasa si dos usuarios activos
    // comparten el mismo nombre).
    private final TextField campoIdentificador = new TextField();
    private final PasswordField campoPassword = new PasswordField();

    public LoginScreen(LoginExitosoListener alIniciarSesion) {
        this.alIniciarSesion = alIniciarSesion;
    }

    public Parent construir() {
        javafx.scene.layout.StackPane panelMarca = armarPanelMarca();
        VBox panelFormulario = armarPanelFormulario();

        HBox raiz = new HBox(panelMarca, panelFormulario);
        raiz.setPrefSize(900, 550);
        return raiz;
    }

    /**
     * Panel izquierdo: el logo del laboratorio (en una tarjeta blanca, así se ven bien sus
     * colores), el nombre del sistema y su bajada. De fondo, la foto que se elija en
     * Configuración &gt; Laboratorio (con un velo verde para que el texto se lea), o un degradado
     * verde si no hay foto.
     */
    private javafx.scene.layout.StackPane armarPanelMarca() {
        javafx.scene.layout.StackPane panel = new javafx.scene.layout.StackPane();
        panel.setPrefWidth(400);
        panel.setMinWidth(300);
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom right, #2C9463, " + EstiloApp.VERDE_PRINCIPAL
                + " 55%, #123B27);");

        javafx.scene.image.Image foto = cargarImagen(utilidades.PreferenciasSistema.getFotoLogin(), 0);
        if (foto != null) {
            // La foto cubre todo el panel (se recorta lo que sobra, sin deformarla).
            javafx.scene.image.ImageView vista = new javafx.scene.image.ImageView(foto);
            vista.setPreserveRatio(true);
            javafx.scene.layout.Region velo = new javafx.scene.layout.Region();
            velo.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(18,59,39,0.35), rgba(18,59,39,0.82));");
            javafx.scene.layout.Pane marco = new javafx.scene.layout.Pane(vista);
            javafx.scene.shape.Rectangle recorte = new javafx.scene.shape.Rectangle();
            recorte.widthProperty().bind(marco.widthProperty());
            recorte.heightProperty().bind(marco.heightProperty());
            marco.setClip(recorte);
            marco.setMinSize(0, 0);
            Runnable cubrir = () -> {
                double w = marco.getWidth();
                double h = marco.getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                double escala = Math.max(w / foto.getWidth(), h / foto.getHeight());
                vista.setFitWidth(foto.getWidth() * escala);
                vista.setFitHeight(foto.getHeight() * escala);
                vista.setLayoutX((w - foto.getWidth() * escala) / 2);
                vista.setLayoutY((h - foto.getHeight() * escala) / 2);
            };
            marco.widthProperty().addListener((o, x, n) -> cubrir.run());
            marco.heightProperty().addListener((o, x, n) -> cubrir.run());
            panel.getChildren().addAll(marco, velo);
        }

        VBox contenido = new VBox(14);
        contenido.setAlignment(Pos.CENTER);
        contenido.setPadding(new Insets(40));

        javafx.scene.image.Image logo = utilidades.PreferenciasSistema.isLogoEnLogin()
                ? cargarImagen(utilidades.PreferenciasSistema.getRutaLogo(), 360) : null;
        if (logo != null) {
            javafx.scene.image.ImageView vistaLogo = new javafx.scene.image.ImageView(logo);
            vistaLogo.setPreserveRatio(true);
            vistaLogo.setSmooth(true);
            if (logo.getWidth() / logo.getHeight() > 1.6) {
                vistaLogo.setFitWidth(200);
            } else {
                vistaLogo.setFitHeight(110);
            }
            javafx.scene.layout.StackPane tarjetaLogo = new javafx.scene.layout.StackPane(vistaLogo);
            tarjetaLogo.setPadding(new Insets(14));
            tarjetaLogo.setStyle("-fx-background-color: white; -fx-background-radius: 22; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 18, 0.1, 0, 4);");
            tarjetaLogo.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
            contenido.getChildren().add(tarjetaLogo);
        }

        Label titulo = new Label(utilidades.PreferenciasSistema.getNombreSistema());
        titulo.setWrapText(true);
        titulo.setMaxWidth(320);
        titulo.setAlignment(Pos.CENTER);
        titulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 0, 1);");

        String bajada = utilidades.PreferenciasSistema.getSubtituloSistema();
        Label subtitulo = new Label(bajada == null || bajada.trim().isEmpty() ? "Sistema de gestión de laboratorio" : bajada);
        subtitulo.setWrapText(true);
        subtitulo.setMaxWidth(320);
        subtitulo.setAlignment(Pos.CENTER);
        subtitulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #E4F5EA;");

        contenido.getChildren().addAll(titulo, subtitulo);
        panel.getChildren().add(contenido);
        return panel;
    }

    /** Lee una imagen del disco; null si no hay ruta o no se puede abrir (nunca rompe el login). */
    private static javafx.scene.image.Image cargarImagen(String ruta, double ladoMaximo) {
        if (ruta == null || ruta.trim().isEmpty() || !new java.io.File(ruta.trim()).isFile()) {
            return null;
        }
        try {
            String url = new java.io.File(ruta.trim()).toURI().toString();
            javafx.scene.image.Image img = ladoMaximo > 0
                    ? new javafx.scene.image.Image(url, ladoMaximo, ladoMaximo, true, true)
                    : new javafx.scene.image.Image(url, 1400, 1400, true, true);
            return img.isError() ? null : img;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private VBox armarPanelFormulario() {
        Label titulo = new Label("Bienvenido");
        titulo.getStyleClass().add("titulo-pantalla");

        Label subtitulo = new Label("Iniciá sesión para entrar a tu cuenta");
        subtitulo.getStyleClass().add("subtitulo-pantalla");

        Label lblIdentificador = new Label("Email o nombre de usuario");
        lblIdentificador.getStyleClass().add("etiqueta-campo");
        campoIdentificador.getStyleClass().add("campo-form");
        campoIdentificador.setPromptText("tu.email@ejemplo.com o tu nombre");
        campoIdentificador.setOnAction(evt -> intentarIniciarSesion());

        Label lblPassword = new Label("Contraseña");
        lblPassword.getStyleClass().add("etiqueta-campo");
        campoPassword.getStyleClass().add("campo-form");
        campoPassword.setOnAction(evt -> intentarIniciarSesion());

        Button botonIniciar = new Button("Iniciar sesión");
        botonIniciar.getStyleClass().add("boton-primario");
        botonIniciar.setMaxWidth(Double.MAX_VALUE);
        botonIniciar.setOnAction(evt -> intentarIniciarSesion());

        Button botonOlvido = new Button("¿Olvidaste tu contraseña?");
        botonOlvido.getStyleClass().add("boton-fila");
        botonOlvido.setOnAction(evt -> new RecuperarContrasenaStage().mostrar());

        VBox caja = new VBox(14, titulo, subtitulo, lblIdentificador, campoIdentificador, lblPassword,
                campoPassword, botonIniciar, botonOlvido);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setMaxWidth(320);

        VBox panel = new VBox(caja);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: white;");
        panel.setPrefWidth(500);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private void intentarIniciarSesion() {
        String identificador = campoIdentificador.getText() == null ? "" : campoIdentificador.getText().trim();
        String password = campoPassword.getText() == null ? "" : campoPassword.getText().trim();

        if (identificador.isEmpty() || password.isEmpty()) {
            mostrarAlerta(AvisoStage.Tipo.ADVERTENCIA, "Atención", "Por favor llene todos los campos.");
            return;
        }

        Boolean loginExitoso = PuenteEDT.ejecutar(
                () -> controlador.LoginController.autenticar(null, identificador, password), null);

        if (loginExitoso == null) {
            return;
        }

        if (loginExitoso) {
            mostrarAlerta(AvisoStage.Tipo.EXITO, "Bienvenido",
                    "¡Bienvenido " + modelo.Sesion.nombre + " " + modelo.Sesion.apellido + "!");
            if (alIniciarSesion != null) {
                alIniciarSesion.onLoginExitoso();
            }
        } else {
            // Mes 8: ahora puede fallar por tres motivos (contraseña mal, email/nombre que no
            // existe, o nombre ambiguo entre dos usuarios activos -- ver dao.Usuario#ingresar), y
            // por seguridad no conviene distinguirle cuál fue para no confirmarle a quien intenta
            // entrar si un nombre/email existe o no.
            mostrarAlerta(AvisoStage.Tipo.ERROR, "Error de Acceso", "Usuario o contraseña incorrectos.");
        }
    }

    // Mes 8 (rediseño visual), cuarta vuelta: antes armaba un Alert + AlertaUtil -- el cartel de
    // "Bienvenido" tenía un hueco vacío que no se pudo arreglar en tres intentos distintos (ver el
    // javadoc de AvisoStage para el detalle). Ahora arma su propia ventana, sin ningún Alert de
    // por medio.
    private void mostrarAlerta(AvisoStage.Tipo tipo, String titulo, String contenido) {
        if (tipo == AvisoStage.Tipo.EXITO) {
            // "Bienvenido": aparece, se va solo y entra al sistema (sin tener que tocar Aceptar).
            vistas.javafx.AvisoRapido.mostrar(titulo, contenido);
            return;
        }
        AvisoStage.mostrar(tipo, titulo, contenido);
    }
}
