package vistas.javafx.login;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;

/**
 * Ventana que coordina el flujo completo de "olvidé mi contraseña": pide el email en {@link
 * PedirEmailPanel}, le pide a {@link controlador.PasswordController} que genere y mande el
 * código de verificación, lo valida en {@link TokenConfirmacionPanel} y finalmente deja elegir
 * una contraseña nueva en {@link CambiarContrasenaPanel}. Versión JavaFX de {@code
 * vistas.recuperarContrasena.RecuperarContrasenaFrame} -- reemplaza el {@code CardLayout} de
 * Swing por un {@link StackPane} al que se le reemplaza el contenido en cada paso.
 */
public class RecuperarContrasenaStage {

    private static final double ANCHO_EMAIL = 560;
    private static final double ALTO_EMAIL = 340;
    private static final double ANCHO_ESTANDAR = 900;
    private static final double ALTO_ESTANDAR = 550;

    private final Stage stage = new Stage();
    private final StackPane contenedor = new StackPane();

    /**
     * Usuario para el que se está verificando el código / cambiando la contraseña.
     */
    private int idUsuarioEnProceso;

    /**
     * Referencia al panel del paso 1 para poder mostrarle el estado de "Enviando..." mientras el
     * código se manda por email en segundo plano (ver {@link #onEmailIngresado}).
     */
    private final PedirEmailPanel panelEmail = new PedirEmailPanel();

    public RecuperarContrasenaStage() {
        stage.setTitle("Recuperar contraseña");
        stage.setResizable(false);

        Scene escena = new Scene(contenedor);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        stage.setScene(escena);

        panelEmail.addEmailIngresadoListener(this::onEmailIngresado);
        panelEmail.addVolverListener(stage::close);
        mostrarPaso(panelEmail.construir(), ANCHO_EMAIL, ALTO_EMAIL);
    }

    public void mostrar() {
        stage.show();
    }

    /**
     * Reemplaza el contenido del {@link StackPane} y, ya que no todos los pasos del flujo miden
     * lo mismo, redimensiona y recentra la ventana acorde al paso nuevo.
     */
    private void mostrarPaso(javafx.scene.Parent raiz, double ancho, double alto) {
        contenedor.getChildren().setAll(raiz);
        stage.setWidth(ancho);
        stage.setHeight(alto);
        stage.centerOnScreen();
    }

    /**
     * Callback de {@link PedirEmailPanel.EmailIngresadoListener}: le pide a PasswordController
     * que genere y mande el código. {@code PasswordController} muestra sus propios {@code
     * JOptionPane} (de error o de confirmación) -- eso sólo es seguro desde el Event Dispatch
     * Thread de Swing, así que la llamada se hace a través de {@link PuenteEDT}, igual que el
     * resto de las pantallas JavaFX que usan Controllers existentes (ver el javadoc de {@link
     * PuenteEDT}).
     *
     * <p>Lo que hace que esto sea distinto de, por ejemplo, {@link #onCodigoIngresado} es que
     * {@code solicitarCodigo} manda un email (una llamada de red que puede demorar varios
     * segundos, sobre todo si el servidor SMTP no responde) antes de devolver el resultado.
     * {@link PuenteEDT#ejecutar} bloquea el hilo que lo llama hasta que termina -- si esa llamada
     * se hace directo desde el hilo de la aplicación JavaFX (como pasaba antes acá), toda la
     * interfaz se queda congelada ("No responde" en Windows) mientras se espera al servidor de
     * email, porque ese hilo es el único que dibuja las pantallas. La solución, igual a la que ya
     * se usa para el envío de resultados por email (ver {@code EnviarResultadosDialog} y el
     * comentario en {@code EmailService}), es mandar todo este trabajo a un hilo de fondo
     * aparte: ese hilo sí puede quedarse esperando sin que se congele nada, porque no es el que
     * dibuja la pantalla. El resultado se trae de vuelta al hilo de JavaFX con
     * {@code Platform.runLater} antes de tocar cualquier control.
     */
    private void onEmailIngresado(String email) {
        panelEmail.setEnviando(true);

        Thread hiloEnvio = new Thread(() -> {
            // 2026-10-08: se llama DIRECTO desde este hilo de fondo, ya no a través de PuenteEDT.
            // PuenteEDT mandaba todo (incluido el envío del email, que puede tardar ~10 segundos
            // si el servidor no responde) a correr en el hilo de Swing -- y como las pantallas
            // JavaFX viven adentro de una ventana Swing, eso congelaba igual toda la aplicación.
            // Los carteles de error de la base ahora se mandan solos al hilo de Swing (ver
            // ConexionUtil#mostrarError), así que llamarlo desde acá es seguro.
            controlador.PasswordController.ResultadoSolicitud resultado;
            try {
                resultado = controlador.PasswordController.solicitarCodigo(null, email);
            } catch (RuntimeException e) {
                e.printStackTrace();
                resultado = null;
            }
            controlador.PasswordController.ResultadoSolicitud resultadoFinal = resultado;

            Platform.runLater(() -> {
                panelEmail.setEnviando(false);
                procesarResultadoSolicitud(resultadoFinal);
            });
        }, "solicitar-codigo-recuperacion");
        hiloEnvio.setDaemon(true);
        hiloEnvio.start();
    }

    /**
     * Parte de {@link #onEmailIngresado} que sí toca controles de JavaFX (cerrar la ventana,
     * mostrar el paso siguiente, avisar errores) -- se ejecuta en el hilo de JavaFX, después de
     * que el hilo de fondo ya terminó de mandar el email. {@code PasswordController} ya no
     * muestra sus propios mensajes (ver su javadoc), así que acá se cubren los tres resultados
     * posibles.
     */
    private void procesarResultadoSolicitud(controlador.PasswordController.ResultadoSolicitud resultado) {
        if (resultado == null) {
            return;
        }
        if (resultado.emailNoRegistrado()) {
            mostrarAlerta(AlertType.WARNING, "Email no registrado",
                    "Ese email no está registrado en el sistema. Deberás comunicarte con la "
                    + "bioquímica a cargo para verificar tu cuenta.");
            stage.close();
            return;
        }
        if (resultado.hayError()) {
            mostrarAlerta(AlertType.ERROR, "No se pudo enviar el código",
                    resultado.getMensajeError() != null
                            ? resultado.getMensajeError()
                            : "Ocurrió un error inesperado. Probá de nuevo.");
            return;
        }
        if (resultado.fueEnviado()) {
            this.idUsuarioEnProceso = resultado.getIdUsuario();
            TokenConfirmacionPanel panelToken = new TokenConfirmacionPanel();
            panelToken.addCodigoVerificadoListener(this::onCodigoIngresado);
            mostrarPaso(panelToken.construir(), ANCHO_ESTANDAR, ALTO_ESTANDAR);
        }
    }

    private void mostrarAlerta(AlertType tipo, String titulo, String contenido) {
        Alert alerta = new Alert(tipo, contenido);
        alerta.setHeaderText(null);
        alerta.setTitle(titulo);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    /**
     * Callback de {@link TokenConfirmacionPanel.CodigoVerificadoListener}.
     */
    private void onCodigoIngresado(String codigo) {
        Boolean valido = PuenteEDT.ejecutar(
                () -> controlador.PasswordController.verificarCodigo(null, idUsuarioEnProceso, codigo), false);
        if (valido != null && valido) {
            CambiarContrasenaPanel panelCambiar = new CambiarContrasenaPanel();
            panelCambiar.addContrasenaGuardadaListener(this::onContrasenaElegida);
            mostrarPaso(panelCambiar.construir(), ANCHO_ESTANDAR, ALTO_ESTANDAR);
        } else {
            mostrarAlerta(AlertType.WARNING, "Código inválido", "El código es incorrecto o venció. Pedí uno nuevo.");
        }
    }

    /**
     * Callback de {@link CambiarContrasenaPanel.ContrasenaGuardadaListener}.
     */
    private void onContrasenaElegida(String nuevaPassword) {
        Boolean guardada = PuenteEDT.ejecutar(
                () -> controlador.PasswordController.cambiarPassword(null, idUsuarioEnProceso, nuevaPassword), false);
        if (guardada != null && guardada) {
            vistas.javafx.AvisoRapido.mostrar(stage, "Listo", "Contraseña actualizada correctamente.");
            stage.close();
        } else {
            mostrarAlerta(AlertType.ERROR, "No se pudo guardar",
                    "Ocurrió un error al guardar la contraseña. Probá de nuevo.");
        }
    }
}
