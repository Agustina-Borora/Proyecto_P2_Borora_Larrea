package controlador;

import dao.Usuario;
import dao.PasswordResetDAO;
import java.awt.Component;
import java.security.SecureRandom;
import java.sql.Timestamp;
import javax.mail.MessagingException;

/**
 * Orquesta el flujo de "olvidé mi contraseña": generar y mandar el código de verificación por
 * email, validarlo, y guardar la contraseña nueva ya hasheada.
 *
 * <p>Esta clase ya no muestra sus propios {@code JOptionPane} (salvo los que siguen viniendo de
 * {@link ConexionUtil#ejecutar} para errores de base de datos, que es un caso aparte): sólo hace
 * el trabajo de datos y devuelve el resultado para que quien la llama -- siempre pantallas
 * JavaFX, ver {@link vistas.javafx.login.RecuperarContrasenaStage} y {@link
 * vistas.javafx.usuarios.UsuarioFormDialog} -- decida cómo mostrarlo. Antes del 2026-10-01 esta
 * clase mostraba esos mensajes ella misma con {@code JOptionPane.showMessageDialog(padre, ...)}
 * con {@code padre == null}; en una app que ya es enteramente JavaFX (sin ningún {@code JFrame}
 * visible) ese diálogo de Swing no tiene ninguna ventana a la que anclarse, así que terminaba
 * abriéndose sin foco e invisible para la persona -- y como {@code PuenteEDT.ejecutar} espera a
 * que el diálogo se cierre, todo quedaba trabado esperando un diálogo que nadie podía ver ni
 * cerrar (el síntoma: "me llega el código pero no pasa nada").
 */
public final class PasswordController {

    /**
     * Cuánto tiempo es válido el código antes de vencer.
     */
    private static final int MINUTOS_VALIDEZ_CODIGO = 15;
    private static final int LARGO_MINIMO_PASSWORD = 8;

    private PasswordController() {
    }

    /**
     * Resultado de {@link #solicitarCodigo}: distingue si el código se mandó, si el email no
     * corresponde a ningún usuario activo, o si hubo un error de sistema (base de datos o envío de
     * email), para que {@link vistas.javafx.login.RecuperarContrasenaStage} sepa si avanzar de
     * pantalla, volver al login o dejar reintentar en la misma pantalla.
     */
    public static final class ResultadoSolicitud {

        private enum Estado {
            ENVIADO, EMAIL_NO_REGISTRADO, ERROR
        }

        private final Estado estado;
        private final Integer idUsuario;
        private final String mensajeError;

        private ResultadoSolicitud(Estado estado, Integer idUsuario, String mensajeError) {
            this.estado = estado;
            this.idUsuario = idUsuario;
            this.mensajeError = mensajeError;
        }

        public boolean fueEnviado() {
            return estado == Estado.ENVIADO;
        }

        public boolean emailNoRegistrado() {
            return estado == Estado.EMAIL_NO_REGISTRADO;
        }

        public boolean hayError() {
            return estado == Estado.ERROR;
        }

        /**
         * Válido solo cuando {@link #fueEnviado()} devuelve {@code true}.
         */
        public int getIdUsuario() {
            return idUsuario;
        }

        /**
         * Detalle para mostrarle a la persona cuando {@link #hayError()} devuelve {@code true}.
         */
        public String getMensajeError() {
            return mensajeError;
        }
    }

    /**
     * Genera un código de 6 dígitos para el email dado (si corresponde a un usuario activo) y se
     * lo manda por correo.
     * Es más claro para el usuario, pero tiene un costo de seguridad conocido: permite usar esta
     * pantalla para determinar qué emails están dados de alta en el sistema (enumeración de
     * usuarios).
     * Para un sistema interno como este, con pocos usuarios y acceso controlado por la
     * institución, se consideró un compromiso aceptable.
     *
     * @return el resultado de la operación; ver {@link ResultadoSolicitud}.
     */
    public static ResultadoSolicitud solicitarCodigo(Component padre, String email) {
        Integer idUsuario = ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> {
                    int id = Usuario.buscarIdPorEmail(con, email);
                    return id == -1 ? null : id;
                }, null);

        if (idUsuario == null) {
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.EMAIL_NO_REGISTRADO, null, null);
        }

        String codigo = generarCodigo();
        Timestamp expiracion = new Timestamp(System.currentTimeMillis() + MINUTOS_VALIDEZ_CODIGO * 60_000L);

        Boolean guardado = ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> {
                    PasswordResetDAO.crear(con, idUsuario, PasswordHasher.hash(codigo), expiracion);
                    return true;
                }, false);

        if (guardado == null || !guardado) {
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.ERROR, null,
                    "No se pudo guardar el código de verificación.");
        }

        try {
            conexiones.EmailService.enviar(email, "Código para recuperar tu contraseña",
                    "Tu código de verificación es: " + codigo + "\n\n"
                    + "Vence en " + MINUTOS_VALIDEZ_CODIGO + " minutos. "
                    + "Si no pediste este código, podés ignorar este mensaje.");
        } catch (MessagingException | java.io.IOException e) {
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.ERROR, null,
                    "No se pudo enviar el email: " + e.getMessage());
        }

        return new ResultadoSolicitud(ResultadoSolicitud.Estado.ENVIADO, idUsuario, null);
    }

    /**
     * Verifica el código de 6 dígitos ingresado contra el último vigente del usuario. Si da
     * {@code false} (código incorrecto, vencido, o ya usado), la pantalla que llama es la que
     * decide cómo avisarlo.
     */
    public static boolean verificarCodigo(Component padre, int idUsuario, String codigoIngresado) {
        Boolean valido = ConexionUtil.ejecutar(padre, "Error de base de datos", con -> {
            PasswordResetDAO.Token token = PasswordResetDAO.buscarVigente(con, idUsuario);
            if (token == null) {
                return false;
            }
            boolean coincide = PasswordHasher.verificar(codigoIngresado, token.getCodigoHash());
            if (coincide) {
                PasswordResetDAO.marcarUsado(con, token.getIdToken());
            }
            return coincide;
        }, false);

        return valido != null && valido;
    }

    /**
     * Cuánto tiene que medir como mínimo una contraseña nueva; expuesto para que las pantallas
     * puedan validar el largo ellas mismas antes de llamar a {@link #cambiarPassword}.
     */
    public static int getLargoMinimoPassword() {
        return LARGO_MINIMO_PASSWORD;
    }

    /**
     * Guarda la nueva contraseña (hasheada) para el usuario. Devuelve {@code false} si el largo
     * mínimo no se cumple o si falló el guardado; quien llama decide cómo avisarlo (ver {@link
     * #getLargoMinimoPassword()} para validar el largo de entrada antes de llegar hasta acá).
     */
    public static boolean cambiarPassword(Component padre, int idUsuario, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < LARGO_MINIMO_PASSWORD) {
            return false;
        }

        Boolean ok = ConexionUtil.ejecutar(padre, "Error de base de datos", con -> {
            Usuario.actualizarPassword(con, idUsuario, PasswordHasher.hash(nuevaPassword));
            return true;
        }, false);

        return ok != null && ok;
    }

    /**
     * Genera un código aleatorio de 6 dígitos (con ceros a la izquierda si hace falta).
     */
    private static String generarCodigo() {
        int numero = new SecureRandom().nextInt(1_000_000); // 0 a 999999
        return String.format("%06d", numero);
    }
}
