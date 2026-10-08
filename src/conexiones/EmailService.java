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
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Envío de emails por SMTP.
 */
public final class EmailService {

    private static final String ARCHIVO_CONFIG = "email.properties";

    private EmailService() {
    }

    /**
     * Manda un email de texto plano por SMTP, usando los datos de {@code email.properties}.
     *
     * @param destinatario Email de destino.
     * @param asunto Asunto del mensaje.
     * @param cuerpo Cuerpo del mensaje, en texto plano.
     * @throws MessagingException Si falla el envío (SMTP caído, credenciales inválidas, etc.).
     * @throws IOException Si no se pudo leer {@code email.properties}.
     */
    public static void enviar(String destinatario, String asunto, String cuerpo) throws MessagingException, IOException {
        enviar(destinatario, asunto, cuerpo, null);
    }

    /**
     * Igual que {@link #enviar(String, String, String)}, pero adjuntando un archivo (pensado
     * para el PDF de resultados que se guarda con "Imprimir" en Registros -- ver
     * {@code vistas.javafx.registros.EnviarResultadosDialog} y {@code reportes.ResultadosPrintable}).
     * Si {@code adjunto} viene null, manda el mensaje de texto plano de siempre (sin armar un
     * mail multiparte de más).
     *
     * @param adjunto Archivo a adjuntar (por ejemplo el PDF de resultados guardado a mano con
     *                "Microsoft Print to PDF"), o null para no adjuntar nada.
     * @throws MessagingException Si falla el envío, o si {@code adjunto} no se pudo adjuntar
     *                             (por ejemplo porque el archivo ya no existe).
     * @throws IOException Si no se pudo leer {@code email.properties} ni el archivo adjunto.
     */
    public static void enviar(String destinatario, String asunto, String cuerpo, File adjunto)
            throws MessagingException, IOException {
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
        // Sin estos tres, si el servidor SMTP no responde (wifi caída, servidor caído, puerto
        // bloqueado por el firewall, etc.) JavaMail se queda esperando con el timeout por
        // default del sistema operativo -- en Windows puede ser más de un minuto. Con esto,
        // como mucho tarda ~10 segundos en avisar que falló en vez de dejar la pantalla
        // colgada. (Además, desde 2026-09-24, el envío ya no corre en el hilo de JavaFX -- ver
        // EnviarResultadosDialog -- así que ahora esto es un límite de "cuánto tardar", no de
        // "si se congela la pantalla".)
        propsSesion.put("mail.smtp.connectiontimeout", "10000");
        propsSesion.put("mail.smtp.timeout", "10000");
        propsSesion.put("mail.smtp.writetimeout", "10000");

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

        if (adjunto == null) {
            mensaje.setText(cuerpo);
        } else {
            MimeBodyPart parteTexto = new MimeBodyPart();
            parteTexto.setText(cuerpo);

            // attachFile hace todo el trabajo de adivinar el tipo de archivo y armar el
            // DataHandler -- evita depender directo de javax.activation en este archivo, que en
            // algunas versiones viejas del .jar de JavaMail viene aparte.
            MimeBodyPart parteAdjunto = new MimeBodyPart();
            parteAdjunto.attachFile(adjunto);

            MimeMultipart contenido = new MimeMultipart();
            contenido.addBodyPart(parteTexto);
            contenido.addBodyPart(parteAdjunto);
            ((MimeMessage) mensaje).setContent(contenido);
        }

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
