package vistas;

import java.awt.CardLayout;

/**
 * Ventana que coordina el flujo completo de "olvidé mi contraseña": pide el
 * email en {@link PedirEmailPanel}, le pide a
 * {@link controlador.PasswordController} que genere y mande el código de
 * verificación, lo valida en {@link TokenDeConfirmacion} y finalmente deja
 * elegir una contraseña nueva en {@link CambiarContraseña}. Se abre desde el
 * link "¿Olvidaste tu contraseña?" de {@link formulariosPrincipales.Login}
 * y se cierra sola al terminar con éxito.
 *
 * A propósito NO tiene un .form propio: en vez de diseñar una pantalla
 * nueva en el editor visual de NetBeans (con el riesgo de corromper el
 * .form si el editor no resuelve bien algún componente custom), reutiliza
 * los tres paneles ya diseñados con el mismo estilo del proyecto
 * (PedirEmailPanel, TokenDeConfirmacion, CambiarContraseña) montados sobre
 * un CardLayout.
 */
public class RecuperarContrasenaFrame extends javax.swing.JFrame {

    private static final String CARTA_EMAIL = "email";
    private static final String CARTA_TOKEN = "token";
    private static final String CARTA_CAMBIAR = "cambiar";

    /**
     * PedirEmailPanel es una tarjeta chica y autocontenida (ver su Javadoc),
     * a diferencia de TokenDeConfirmacion/CambiarContraseña que están
     * pensadas para el tamaño grande de Login. Por eso la ventana cambia de
     * tamaño según qué carta esté mostrando (ver {@link #mostrarCarta}).
     */
    private static final java.awt.Dimension TAMANO_EMAIL = new java.awt.Dimension(560, 320);
    private static final java.awt.Dimension TAMANO_ESTANDAR = new java.awt.Dimension(900, 550);

    private final CardLayout layout = new CardLayout();
    private final javax.swing.JPanel contenedor = new javax.swing.JPanel(layout);

    /** Usuario para el que se está verificando el código / cambiando la contraseña. */
    private int idUsuarioEnProceso;

    /**
     * Arma la ventana con las tres pantallas del flujo y la deja mostrando
     * la de pedir el email. Quien la crea solo tiene que hacer
     * {@code setVisible(true)}.
     */
    public RecuperarContrasenaFrame() {
        setTitle("Recuperar contraseña");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        PedirEmailPanel panelEmail = new PedirEmailPanel();
        panelEmail.addEmailIngresadoListener(this::onEmailIngresado);
        panelEmail.addVolverListener(this::dispose);
        contenedor.add(panelEmail, CARTA_EMAIL);

        TokenDeConfirmacion panelToken = new TokenDeConfirmacion();
        panelToken.addCodigoVerificadoListener(this::onCodigoIngresado);
        contenedor.add(panelToken, CARTA_TOKEN);

        CambiarContraseña panelCambiar = new CambiarContraseña();
        panelCambiar.addContrasenaGuardadaListener(this::onContrasenaElegida);
        contenedor.add(panelCambiar, CARTA_CAMBIAR);

        getContentPane().add(contenedor);

        mostrarCarta(CARTA_EMAIL, TAMANO_EMAIL);
    }

    /**
     * Cambia de carta en el CardLayout y, ya que no todas las pantallas del
     * flujo miden lo mismo, redimensiona y recentra la ventana acorde a la
     * carta nueva.
     */
    private void mostrarCarta(String carta, java.awt.Dimension tamano) {
        layout.show(contenedor, carta);
        setSize(tamano);
        setLocationRelativeTo(null);
    }

    /**
     * Callback de {@link PedirEmailPanel.EmailIngresadoListener}: le pide a
     * PasswordController que genere y mande el código. Si algo falla
     * (email no encontrado, error de base de datos, error al mandar el
     * mail), PasswordController ya le mostró el cartel correspondiente y
     * nos quedamos en esta misma pantalla para que el usuario pueda
     * corregir el email y reintentar.
     */
    private void onEmailIngresado(String email) {
        Integer idUsuario = controlador.PasswordController.solicitarCodigo(this, email);
        if (idUsuario != null) {
            this.idUsuarioEnProceso = idUsuario;
            mostrarCarta(CARTA_TOKEN, TAMANO_ESTANDAR);
        }
    }

    /** Callback de {@link TokenDeConfirmacion.CodigoVerificadoListener}. */
    private void onCodigoIngresado(String codigo) {
        boolean valido = controlador.PasswordController.verificarCodigo(this, idUsuarioEnProceso, codigo);
        if (valido) {
            mostrarCarta(CARTA_CAMBIAR, TAMANO_ESTANDAR);
        }
    }

    /** Callback de {@link CambiarContraseña.ContrasenaGuardadaListener}. */
    private void onContrasenaElegida(String nuevaPassword) {
        boolean guardada = controlador.PasswordController.cambiarPassword(this, idUsuarioEnProceso, nuevaPassword);
        if (guardada) {
            dispose();
        }
    }
}
