package utilidades;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * "Enviar por WhatsApp" sin ninguna cuenta de WhatsApp Business API (Twilio, Meta, etc. -- esas
 * requieren una cuenta paga y credenciales que este proyecto no tiene): abre WhatsApp Web o
 * Desktop (lo que tenga instalado/logueado Windows) con el celular del paciente y el mensaje ya
 * escrito, vía el link público {@code wa.me}. La usuaria todavía tiene que apretar "Enviar" del
 * lado de WhatsApp -- no hay forma de mandarlo 100% solo sin esa cuenta paga.
 */
public final class WhatsAppUtil {

    private WhatsAppUtil() {
    }

    /**
     * Arma el link de wa.me para un celular y mensaje dados. Devuelve null si el celular está
     * vacío (nada para armar).
     */
    public static String armarLink(String celular, String mensaje) {
        String numero = normalizarCelular(celular);
        if (numero == null) {
            return null;
        }
        String mensajeCodificado;
        try {
            mensajeCodificado = URLEncoder.encode(mensaje == null ? "" : mensaje, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 siempre está soportada en la JVM -- esto nunca pasa en la práctica.
            mensajeCodificado = mensaje == null ? "" : mensaje;
        }
        return "https://wa.me/" + numero + "?text=" + mensajeCodificado;
    }

    /**
     * Abre el link de wa.me en el navegador predeterminado de Windows. Devuelve true si se pudo
     * abrir (no significa que el mensaje ya se mandó, solo que se abrió el chat con todo listo).
     */
    public static boolean abrirChat(String celular, String mensaje) {
        String link = armarLink(celular, mensaje);
        if (link == null) {
            return false;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            Desktop.getDesktop().browse(URI.create(link));
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Deja solo los dígitos y, si el número no parece tener ya un código de país (10 dígitos o
     * menos, formato típico de un celular argentino escrito sin el "54"), le antepone "549"
     * (código de Argentina + el "9" que exige WhatsApp para celulares argentinos). Es una
     * suposición razonable para este laboratorio, no una validación real -- si algún número no
     * es argentino o el link abre mal, se puede corregir el celular a mano en el chat que se abre.
     */
    static String normalizarCelular(String celular) {
        if (celular == null) {
            return null;
        }
        String digitos = celular.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            return null;
        }
        if (digitos.length() <= 10) {
            return "549" + digitos;
        }
        return digitos;
    }
}
