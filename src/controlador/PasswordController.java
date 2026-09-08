package controlador;

import dao.Usuario;
import dao.PasswordResetDAO;
import java.awt.Component;
import java.security.SecureRandom;
import java.sql.Timestamp;
import javax.mail.MessagingException;
import javax.swing.JOptionPane;

/**
 * Orquesta el flujo de "olvidé mi contraseña": generar y mandar el código de verificación por
 * email, validarlo, y guardar la contraseña nueva ya hasheada.
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
     * email), para que {@link vistas.recuperarContrasena.RecuperarContrasenaFrame} sepa si avanzar de pantalla, volver
     * al login o dejar reintentar en la misma pantalla.
     */
    public static final class ResultadoSolicitud {

        private enum Estado {
            ENVIADO, EMAIL_NO_REGISTRADO, ERROR
        }

        private final Estado estado;
        private final Integer idUsuario;

        private ResultadoSolicitud(Estado estado, Integer idUsuario) {
            this.estado = estado;
            this.idUsuario = idUsuario;
        }

        public boolean fueEnviado() {
            return estado == Estado.ENVIADO;
        }

        public boolean emailNoRegistrado() {
            return estado == Estado.EMAIL_NO_REGISTRADO;
        }

        /**
         * Válido solo cuando {@link #fueEnviado()} devuelve {@code true}.
         */
        public int getIdUsuario() {
            return idUsuario;
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
            JOptionPane.showMessageDialog(padre,
                    "Ese email no está registrado en el sistema. Deberás "
                    + "comunicarte con la bioquímica a cargo para verificar tu cuenta.",
                    "Email no registrado", JOptionPane.WARNING_MESSAGE);
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.EMAIL_NO_REGISTRADO, null);
        }

        String codigo = generarCodigo();
        Timestamp expiracion = new Timestamp(System.currentTimeMillis() + MINUTOS_VALIDEZ_CODIGO * 60_000L);

        Boolean guardado = ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> {
                    PasswordResetDAO.crear(con, idUsuario, PasswordHasher.hash(codigo), expiracion);
                    return true;
                }, false);

        if (guardado == null || !guardado) {
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.ERROR, null);
        }

        try {
            conexiones.EmailService.enviar(email, "Código para recuperar tu contraseña",
                    "Tu código de verificación es: " + codigo + "\n\n"
                    + "Vence en " + MINUTOS_VALIDEZ_CODIGO + " minutos. "
                    + "Si no pediste este código, podés ignorar este mensaje.");
        } catch (MessagingException | java.io.IOException e) {
            JOptionPane.showMessageDialog(padre,
                    "No se pudo enviar el email: " + e.getMessage(),
                    "Error al enviar", JOptionPane.ERROR_MESSAGE);
            return new ResultadoSolicitud(ResultadoSolicitud.Estado.ERROR, null);
        }

        JOptionPane.showMessageDialog(padre,
                "Te enviamos un código de verificación a tu email.",
                "Código enviado", JOptionPane.INFORMATION_MESSAGE);
        return new ResultadoSolicitud(ResultadoSolicitud.Estado.ENVIADO, idUsuario);
    }

    /**
     * Verifica el código de 6 dígitos ingresado contra el último vigente del usuario.
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

        boolean esValido = valido != null && valido;
        if (!esValido) {
            JOptionPane.showMessageDialog(padre,
                    "El código es incorrecto o venció. Pedí uno nuevo.",
                    "Código inválido", JOptionPane.WARNING_MESSAGE);
        }
        return esValido;
    }

    /**
     * Valida el largo mínimo y guarda la nueva contraseña (hasheada) para el usuario.
     */
    public static boolean cambiarPassword(Component padre, int idUsuario, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < LARGO_MINIMO_PASSWORD) {
            JOptionPane.showMessageDialog(padre,
                    "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres.",
                    "Contraseña muy corta", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        Boolean ok = ConexionUtil.ejecutar(padre, "Error de base de datos", con -> {
            Usuario.actualizarPassword(con, idUsuario, PasswordHasher.hash(nuevaPassword));
            return true;
        }, false);

        if (ok != null && ok) {
            JOptionPane.showMessageDialog(padre, "Contraseña actualizada correctamente.");
            return true;
        }
        return false;
    }

    /**
     * Genera un código aleatorio de 6 dígitos (con ceros a la izquierda si hace falta).
     */
    private static String generarCodigo() {
        int numero = new SecureRandom().nextInt(1_000_000); // 0 a 999999
        return String.format("%06d", numero);
    }
}
