package controlador;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilidad para hashear y verificar contraseñas (y códigos de verificación)
 * con PBKDF2/HMAC-SHA256, sin depender de ninguna librería externa: usa
 * únicamente {@code javax.crypto}, que ya viene incluido en el JDK. Se
 * eligió por eso en vez de bcrypt: no hace falta agregar ningún .jar nuevo
 * al proyecto para tener contraseñas hasheadas correctamente.
 *
 * El valor guardado tiene el formato {@code "iteraciones:saltEnBase64:hashEnBase64"},
 * todo en un solo String que se persiste tal cual en la base (por ejemplo en
 * la columna {@code usuarios.password_usuario}). Guardar las iteraciones
 * junto con el hash permite en el futuro subir ese número (hacerlo más
 * lento/seguro) sin invalidar los hashes ya guardados con un número menor.
 *
 * Ver {@link conexiones.Usuario#ingresar} para la migración automática de
 * las contraseñas viejas, guardadas en texto plano antes de esta clase.
 */
public final class PasswordHasher {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 65536;
    private static final int LARGO_CLAVE_BITS = 256;
    private static final int LARGO_SALT_BYTES = 16;

    private PasswordHasher() {
    }

    /**
     * Genera un hash nuevo (con salt aleatorio) para el valor dado. Usar al
     * crear un usuario, al cambiar su contraseña, o al generar un código de
     * verificación de un solo uso.
     */
    public static String hash(String valor) {
        byte[] salt = new byte[LARGO_SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(valor.toCharArray(), salt, ITERACIONES);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Compara un valor en texto plano (contraseña o código) contra un hash
     * guardado con {@link #hash(String)}. Devuelve false (en vez de lanzar
     * excepción) si el valor guardado no tiene el formato esperado, para
     * que quien llama pueda decidir tratarlo como texto plano legacy.
     */
    public static boolean verificar(String valor, String hashGuardado) {
        if (!esFormatoValido(hashGuardado)) {
            return false;
        }
        String[] partes = hashGuardado.split(":", 3);
        int iteraciones = Integer.parseInt(partes[0]);
        byte[] salt = Base64.getDecoder().decode(partes[1]);
        byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);
        byte[] hashCalculado = pbkdf2(valor.toCharArray(), salt, iteraciones);
        return sonIguales(hashEsperado, hashCalculado);
    }

    /**
     * Indica si el valor tiene el formato "iteraciones:salt:hash" producido
     * por {@link #hash(String)}. Se usa para distinguir un hash real de una
     * contraseña vieja guardada en texto plano (ver
     * {@link conexiones.Usuario#ingresar}), sin arriesgarse a una excepción
     * si el valor guardado es cualquier otra cosa.
     */
    public static boolean esFormatoValido(String valorGuardado) {
        if (valorGuardado == null) {
            return false;
        }
        String[] partes = valorGuardado.split(":", 3);
        if (partes.length != 3) {
            return false;
        }
        try {
            Integer.parseInt(partes[0]);
            Base64.getDecoder().decode(partes[1]);
            Base64.getDecoder().decode(partes[2]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] valor, byte[] salt, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(valor, salt, iteraciones, LARGO_CLAVE_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITMO);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }

    /** Comparación en tiempo constante, para no filtrar información por timing. */
    private static boolean sonIguales(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int resultado = 0;
        for (int i = 0; i < a.length; i++) {
            resultado |= a[i] ^ b[i];
        }
        return resultado == 0;
    }
}
