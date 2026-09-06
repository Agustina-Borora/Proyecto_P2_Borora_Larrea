package controlador;

import conexiones.Usuario;
import dao.PasswordResetDAO;
import java.awt.Component;
import java.security.SecureRandom;
import java.sql.Timestamp;
import javax.mail.MessagingException;
import javax.swing.JOptionPane;

/**
 * Orquesta el flujo de "olvidé mi contraseña": generar y mandar el código
 * de verificación por email, validarlo, y guardar la contraseña nueva ya
 * hasheada. Las pantallas de {@code vistas/} (TokenDeConfirmacion,
 * CambiarContraseña) y {@link vistas.RecuperarContrasenaFrame} solo le
 * piden datos a este Controlador, igual que el resto de la Vista con sus
 * Controladores — no abren conexiones ni conocen los DAO ni el hasheo.
 */
public final class PasswordController {

    /** Cuánto tiempo es válido el código antes de vencer. */
    private static final int MINUTOS_VALIDEZ_CODIGO = 15;
    private static final int LARGO_MINIMO_PASSWORD = 8;

    private PasswordController() {
    }

    /**
     * Genera un código de 6 dígitos para el email dado (si corresponde a un
     * usuario activo) y se lo manda por correo.
     *
     * Por seguridad, tanto si el email existe como si no, se le muestra al
     * usuario el mismo cartel genérico ("si el email está registrado...")
     * para no revelar qué emails están dados de alta en el sistema.
     *
     * @return el id del usuario si se pudo generar y mandar el código;
     *         {@code null} si el email no corresponde a nadie activo, o si
     *         falló el guardado o el envío.
     */
    public static Integer solicitarCodigo(Component padre, String email) {
        Integer idUsuario = ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> {
                    int id = Usuario.buscarIdPorEmail(con, email);
                    return id == -1 ? null : id;
                }, null);

        if (idUsuario == null) {
            mostrarMensajeGenerico(padre);
            return null;
        }

        String codigo = generarCodigo();
        Timestamp expiracion = new Timestamp(System.currentTimeMillis() + MINUTOS_VALIDEZ_CODIGO * 60_000L);

        Boolean guardado = ConexionUtil.ejecutar(padre, "Error de base de datos",
                con -> {
                    PasswordResetDAO.crear(con, idUsuario, PasswordHasher.hash(codigo), expiracion);
                    return true;
                }, false);

        if (guardado == null || !guardado) {
            return null;
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
            return null;
        }

        mostrarMensajeGenerico(padre);
        return idUsuario;
    }

    /**
     * Verifica el código de 6 dígitos ingresado contra el último vigente
     * del usuario. Si coincide, lo marca como usado (un mismo código no se
     * puede canjear dos veces). Si no coincide o venció, le avisa al
     * usuario con un cartel.
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
     * Valida el largo mínimo y guarda la nueva contraseña (hasheada) para
     * el usuario. La validación de "las dos casillas coinciden" es
     * responsabilidad de la Vista ({@code vistas.CambiarContraseña}); acá
     * solo se valida el largo mínimo antes de persistir.
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

    /** Genera un código aleatorio de 6 dígitos (con ceros a la izquierda si hace falta). */
    private static String generarCodigo() {
        int numero = new SecureRandom().nextInt(1_000_000); // 0 a 999999
        return String.format("%06d", numero);
    }

    private static void mostrarMensajeGenerico(Component padre) {
        JOptionPane.showMessageDialog(padre,
                "Si el email está registrado, te va a llegar un código de verificación en los próximos minutos.",
                "Revisá tu email", JOptionPane.INFORMATION_MESSAGE);
    }
}
