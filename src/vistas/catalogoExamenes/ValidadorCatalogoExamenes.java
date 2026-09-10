package vistas.catalogoExamenes;

import java.util.regex.Pattern;

public class ValidadorCatalogoExamenes {

    private static final Pattern NOMBRE =
            Pattern.compile("^[\\p{L} ]+$");

    private static final Pattern VALOR_REFERENCIA =
            Pattern.compile(
                "^(?:"
                + "(?:[mMhH]:?\\s*)?[0-9.,]+\\s*(?:-|a)\\s*[0-9.,]+"
                + "(?:\\s+[mMhH]:?\\s*[0-9.,]+\\s*(?:-|a)\\s*[0-9.,]+)?"
                + ")\\s*%?$"
            );

    private static final Pattern UNIDAD =
            Pattern.compile("^(?=.{1,6}$)[a-zA-Z/%]*[0-9]?$");

    private static final Pattern CODIGO =
            Pattern.compile("^[0-9]+$");

    private static final Pattern DECIMAL =
            Pattern.compile("^[0-9]+(?:[.,][0-9]+)?$");

    public static boolean esCodigoValido(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && CODIGO.matcher(texto.trim()).matches();
    }

    public static boolean esDecimalValido(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && DECIMAL.matcher(texto.trim()).matches();
    }

    public static boolean esNombreValido(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && NOMBRE.matcher(texto.trim()).matches();
    }

    public static boolean esValorReferenciaValido(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && VALOR_REFERENCIA.matcher(texto.trim()).matches();
    }

    public static boolean esUnidadValida(String texto) {

        return texto != null
                && !texto.trim().isEmpty()
                && UNIDAD.matcher(texto.trim()).matches();
    }

    public static boolean esTextoSoloLetras(String texto) {

        return esNombreValido(texto);
    }
}
