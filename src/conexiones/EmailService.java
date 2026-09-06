package conexiones;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Envío de emails por SMTP. Se usa para mandar el código de verificación
 * del flujo "olvidé mi contraseña" ({@link controlador.PasswordController}),
 * al email que ya está registrado para cada usuario en {@code usuarios.email_usuario}
 * — no hace falta que el destinatario registre nada nuevo.
 *
 * Requiere agregar la librería JavaMail ({@code javax.mail}, artefacto
 * {@code com.sun.mail:javax.mail:1.6.2} o similar) a las librerías del
 * proyecto en NetBeans; ver README.md, sección "Recuperar contraseña por
 * email".
 *
 * Las credenciales del correo NO están en el código: se leen de un archivo
 * {@code email.properties} en la raíz del proyecto (al lado de build.xml),
 * que cada quien completa en su propia copia y que {@code .gitignore}
 * excluye para que nunca se suba al repositorio. Ver
 * {@code email.properties.example} para el formato esperado.
 */
public final class EmailService {

    private static final String ARCHIVO_CONFIG = "email.properties";

    private EmailService() {
    }

    /**
     * Manda un email de texto plano por SMTP, usando los datos de
     * {@code email.properties}.
     *
     * @param destinatario Email de destino.
     * @param asunto Asunto del mensaje.
     * @param cuerpo Cuerpo del mensaje, en texto plano.
     * @throws MessagingException Si falla el envío (SMTP caído, credenciales inválidas, etc.).
     * @throws IOException Si no se pudo leer {@code email.properties}.
     */
    public static void enviar(String destinatario, String asunto, String cuerpo) throws MessagingException, IOException {
        Properties config = cargarConfig();

        String host = config.getProperty("smtp.host");
        String puerto = config.getProperty("smtp.port", "587");
        final String usuario = config.getProperty("smtp.user");
        final String password = config.getProperty("smtp.password");
        String remitente = config.getProperty("smtp.from", usuario);

        Properties propsSesion = new Properties();
        propsSesion.put("mail.smtp.auth", "true");
        propsSesion.put("mail.smtp.starttls.enable", "true");
        propsSesion.put("mail.smtp.host", host);
        propsSesion.put("mail.smtp.port", puerto);

        Session session = Session.getInstance(propsSesion, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, password);
            }
        });

        Message mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        Transport.send(mensaje);
    }

    private static Properties cargarConfig() throws IOException {
        Properties config = new Properties();
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            throw new IOException("Falta el archivo " + ARCHIVO_CONFIG
                    + " con los datos de conexión SMTP en la raíz del proyecto"
                    + " (copiá email.properties.example y completá tus datos).");
        }
        try (InputStream in = new FileInputStream(archivo)) {
            config.load(in);
        }
        return config;
    }
}
