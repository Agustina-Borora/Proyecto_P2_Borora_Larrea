package utilidades;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Preferencias del sistema configurables desde Configuración &gt; Sistema (Figma: tarjeta
 * "Notificaciones del sistema"), guardadas en un archivo de propiedades en la raíz del proyecto
 * para que sobrevivan a un reinicio de la aplicación -- no son datos de un paciente ni de un
 * pedido, así que no hace falta guardarlas en la base.
 *
 * <p>Las tres arrancan activadas (mismo estado que muestra el prototipo de Figma, los 3 toggles
 * en verde) si el archivo todavía no existe o no tiene esa clave puntual -- ver
 * {@link #leerBooleano}.</p>
 */
public final class PreferenciasSistema {

    private static final String ARCHIVO = "preferencias_sistema.properties";

    private static final String CLAVE_NOTIFICAR_TECNICO = "notificar.tecnico";
    private static final String CLAVE_ALERTAS_FUERA_RANGO = "alertas.fuera.rango";
    private static final String CLAVE_RECORDATORIO_PENDIENTES = "recordatorio.pendientes";
    private static final String CLAVE_NOMBRE_LABORATORIO = "informes.nombre.laboratorio";
    private static final String CLAVE_LEYENDA_PIE = "informes.leyenda.pie";
    private static final String CLAVE_SUBTITULO = "laboratorio.subtitulo";
    private static final String CLAVE_DIRECCION = "laboratorio.direccion";
    private static final String CLAVE_TELEFONO = "laboratorio.telefono";
    private static final String CLAVE_LOCALIDAD = "laboratorio.localidad";
    private static final String CLAVE_BIOQUIMICA_RESPONSABLE = "laboratorio.bioquimica.responsable";
    private static final String CLAVE_MATRICULA_BIOQUIMICA = "laboratorio.bioquimica.matricula";
    private static final String CLAVE_RUTA_LOGO = "laboratorio.logo.ruta";
    private static final String CLAVE_RAZON_SOCIAL = "laboratorio.razon.social";
    private static final String CLAVE_CUIT = "laboratorio.cuit";
    private static final String CLAVE_EMAIL_CONTACTO = "laboratorio.email.contacto";
    private static final String CLAVE_HABILITACION_NUMERO = "laboratorio.habilitacion.numero";
    private static final String CLAVE_INFORME_MOSTRAR_LOGO = "informe.resultados.mostrar.logo";
    private static final String CLAVE_INFORME_INCLUIR_REFERENCIA = "informe.resultados.incluir.referencia";
    private static final String CLAVE_INFORME_RESALTAR_FUERA_RANGO = "informe.resultados.resaltar.fuera.rango";
    private static final String CLAVE_INFORME_INCLUIR_MEDICO = "informe.resultados.incluir.medico";
    private static final String CLAVE_INFORME_MOSTRAR_HABILITACION = "informe.resultados.mostrar.habilitacion";

    private static final String CLAVE_INFORME_INCLUIR_COMPROBANTE = "informe.resultados.incluir.comprobante";
    private static final String CLAVE_CARPETA_PDF = "informes.carpeta.pdf";
    private static final String CLAVE_DISENO_INFORME = "informes.diseno";
    private static final String CLAVE_MODO_COMPROBANTE = "comprobante.modo";
    private static final String CLAVE_NOMBRE_SISTEMA = "sistema.nombre";
    private static final String CLAVE_SUBTITULO_SISTEMA = "sistema.subtitulo";
    private static final String CLAVE_LOGO_EN_SISTEMA = "sistema.usar.logo";
    private static final String CLAVE_RUTA_FIRMA = "laboratorio.firma.ruta";
    private static final String CLAVE_USAR_FIRMA_IMAGEN = "informe.resultados.firma.imagen";
    private static final String CLAVE_LOGIN_FOTO = "login.foto.ruta";
    private static final String CLAVE_LOGIN_LOGO = "login.mostrar.logo";
    private static final String CLAVE_RESPALDO_ACTIVO = "respaldo.auto.activo";
    private static final String CLAVE_RESPALDO_HORA = "respaldo.auto.hora";
    private static final String CLAVE_RESPALDO_CARPETA = "respaldo.carpeta";
    private static final String CLAVE_RESPALDO_CONSERVAR = "respaldo.conservar";
    private static final String CLAVE_RESPALDO_ULTIMO = "respaldo.ultimo";

    /** Qué hacer con el Comprobante de Orden al generar una orden en Nuevo Análisis. */
    public static final String COMPROBANTE_PREGUNTAR = "PREGUNTAR";
    public static final String COMPROBANTE_AUTOMATICO = "AUTOMATICO";
    public static final String COMPROBANTE_NUNCA = "NUNCA";

    private static final String NOMBRE_LABORATORIO_POR_DEFECTO = "Laboratorio San Gregorio";

    private PreferenciasSistema() {
    }

    /** "Notificar al técnico cuando un análisis queda listo". */
    public static boolean isNotificarTecnico() {
        return leerBooleano(CLAVE_NOTIFICAR_TECNICO);
    }

    public static void setNotificarTecnico(boolean valor) {
        guardarBooleano(CLAVE_NOTIFICAR_TECNICO, valor);
    }

    /** "Alertas de valores fuera de rango al cargar resultados". */
    public static boolean isAlertasFueraDeRango() {
        return leerBooleano(CLAVE_ALERTAS_FUERA_RANGO);
    }

    public static void setAlertasFueraDeRango(boolean valor) {
        guardarBooleano(CLAVE_ALERTAS_FUERA_RANGO, valor);
    }

    /** "Recordatorio de análisis pendientes al iniciar el día". */
    public static boolean isRecordatorioPendientes() {
        return leerBooleano(CLAVE_RECORDATORIO_PENDIENTES);
    }

    public static void setRecordatorioPendientes(boolean valor) {
        guardarBooleano(CLAVE_RECORDATORIO_PENDIENTES, valor);
    }

    /**
     * Nombre del laboratorio que se imprime en el encabezado del "Comprobante de Orden" (Config.
     * &gt; Informes PDF) -- ver {@link reportes.ComprobanteOrdenPrintable}. Arranca en
     * "Laboratorio San Gregorio" si todavía no se personalizó.
     */
    public static String getNombreLaboratorio() {
        return leerTexto(CLAVE_NOMBRE_LABORATORIO, NOMBRE_LABORATORIO_POR_DEFECTO);
    }

    public static void setNombreLaboratorio(String valor) {
        String limpio = valor == null || valor.trim().isEmpty() ? NOMBRE_LABORATORIO_POR_DEFECTO : valor.trim();
        guardarTexto(CLAVE_NOMBRE_LABORATORIO, limpio);
    }

    /**
     * Leyenda opcional que se imprime al pie del "Comprobante de Orden" (Config. &gt; Informes
     * PDF) -- vacía por defecto (no se imprime ninguna leyenda de pie).
     */
    public static String getLeyendaPie() {
        return leerTexto(CLAVE_LEYENDA_PIE, "");
    }

    public static void setLeyendaPie(String valor) {
        guardarTexto(CLAVE_LEYENDA_PIE, valor == null ? "" : valor.trim());
    }

    /**
     * Datos completos del encabezado del laboratorio (Config. &gt; Laboratorio), pensados para el
     * "Informe de Resultados" (ver {@link reportes.ResultadosPrintable}) -- un encabezado con
     * varias líneas, como el que usa de verdad el laboratorio en papel: nombre del centro,
     * subtítulo, bioquímica responsable + matrícula, dirección/teléfono/localidad. Todos vacíos
     * por defecto salvo lo que ya existía (nombre del laboratorio).
     */
    public static String getSubtitulo() {
        return leerTexto(CLAVE_SUBTITULO, "");
    }

    public static void setSubtitulo(String valor) {
        guardarTexto(CLAVE_SUBTITULO, valor == null ? "" : valor.trim());
    }

    public static String getDireccion() {
        return leerTexto(CLAVE_DIRECCION, "");
    }

    public static void setDireccion(String valor) {
        guardarTexto(CLAVE_DIRECCION, valor == null ? "" : valor.trim());
    }

    public static String getTelefono() {
        return leerTexto(CLAVE_TELEFONO, "");
    }

    public static void setTelefono(String valor) {
        guardarTexto(CLAVE_TELEFONO, valor == null ? "" : valor.trim());
    }

    public static String getLocalidad() {
        return leerTexto(CLAVE_LOCALIDAD, "");
    }

    public static void setLocalidad(String valor) {
        guardarTexto(CLAVE_LOCALIDAD, valor == null ? "" : valor.trim());
    }

    public static String getBioquimicaResponsable() {
        return leerTexto(CLAVE_BIOQUIMICA_RESPONSABLE, "");
    }

    public static void setBioquimicaResponsable(String valor) {
        guardarTexto(CLAVE_BIOQUIMICA_RESPONSABLE, valor == null ? "" : valor.trim());
    }

    public static String getMatriculaBioquimica() {
        return leerTexto(CLAVE_MATRICULA_BIOQUIMICA, "");
    }

    public static void setMatriculaBioquimica(String valor) {
        guardarTexto(CLAVE_MATRICULA_BIOQUIMICA, valor == null ? "" : valor.trim());
    }

    /**
     * Ruta absoluta a un archivo de imagen elegido por la usuaria para el logo del encabezado, o
     * vacío si no cargó ninguno -- el informe se imprime igual sin logo, no es obligatorio.
     */
    public static String getRutaLogo() {
        return leerTexto(CLAVE_RUTA_LOGO, "");
    }

    public static void setRutaLogo(String valor) {
        guardarTexto(CLAVE_RUTA_LOGO, valor == null ? "" : valor.trim());
    }

    /** Razón social del laboratorio (Config. &gt; Laboratorio, tarjeta "Datos del Laboratorio"). */
    public static String getRazonSocial() {
        return leerTexto(CLAVE_RAZON_SOCIAL, "");
    }

    public static void setRazonSocial(String valor) {
        guardarTexto(CLAVE_RAZON_SOCIAL, valor == null ? "" : valor.trim());
    }

    public static String getCuit() {
        return leerTexto(CLAVE_CUIT, "");
    }

    public static void setCuit(String valor) {
        guardarTexto(CLAVE_CUIT, valor == null ? "" : valor.trim());
    }

    /** Email de contacto (Config. &gt; Laboratorio, tarjeta "Contacto y Ubicación"). */
    public static String getEmailContacto() {
        return leerTexto(CLAVE_EMAIL_CONTACTO, "");
    }

    public static void setEmailContacto(String valor) {
        guardarTexto(CLAVE_EMAIL_CONTACTO, valor == null ? "" : valor.trim());
    }

    /** N° de habilitación del laboratorio, para el pie del "Informe de Resultados". */
    public static String getHabilitacionNumero() {
        return leerTexto(CLAVE_HABILITACION_NUMERO, "");
    }

    public static void setHabilitacionNumero(String valor) {
        guardarTexto(CLAVE_HABILITACION_NUMERO, valor == null ? "" : valor.trim());
    }

    /**
     * Las 5 opciones de "Opciones del informe" (Config. &gt; Informes PDF) que controlan qué se
     * imprime en el "Informe de Resultados" (ver {@link reportes.ResultadosPrintable}) -- todas
     * arrancan activadas, mismo criterio que las de "Notificaciones del sistema".
     */
    public static boolean isInformeMostrarLogo() {
        return leerBooleano(CLAVE_INFORME_MOSTRAR_LOGO);
    }

    public static void setInformeMostrarLogo(boolean valor) {
        guardarBooleano(CLAVE_INFORME_MOSTRAR_LOGO, valor);
    }

    public static boolean isInformeIncluirReferencia() {
        return leerBooleano(CLAVE_INFORME_INCLUIR_REFERENCIA);
    }

    public static void setInformeIncluirReferencia(boolean valor) {
        guardarBooleano(CLAVE_INFORME_INCLUIR_REFERENCIA, valor);
    }

    public static boolean isInformeResaltarFueraDeRango() {
        return leerBooleano(CLAVE_INFORME_RESALTAR_FUERA_RANGO);
    }

    public static void setInformeResaltarFueraDeRango(boolean valor) {
        guardarBooleano(CLAVE_INFORME_RESALTAR_FUERA_RANGO, valor);
    }

    public static boolean isInformeIncluirMedico() {
        return leerBooleano(CLAVE_INFORME_INCLUIR_MEDICO);
    }

    public static void setInformeIncluirMedico(boolean valor) {
        guardarBooleano(CLAVE_INFORME_INCLUIR_MEDICO, valor);
    }

    public static boolean isInformeMostrarHabilitacion() {
        return leerBooleano(CLAVE_INFORME_MOSTRAR_HABILITACION);
    }

    public static void setInformeMostrarHabilitacion(boolean valor) {
        guardarBooleano(CLAVE_INFORME_MOSTRAR_HABILITACION, valor);
    }

    /**
     * "Agregar el comprobante de la orden al final del informe de resultados" -- arranca
     * apagado (a diferencia de las otras opciones), porque no todos los laboratorios entregan el
     * comprobante junto con los resultados.
     */
    public static boolean isInformeIncluirComprobante() {
        return leerBooleano(CLAVE_INFORME_INCLUIR_COMPROBANTE, false);
    }

    public static void setInformeIncluirComprobante(boolean valor) {
        guardarBooleano(CLAVE_INFORME_INCLUIR_COMPROBANTE, valor);
    }

    /**
     * Carpeta donde el sistema guarda solo los PDF (informes y comprobantes). Por defecto
     * "Documentos\Laboratorio - PDF" del usuario de Windows. El sistema siempre sabe dónde quedó
     * cada PDF (además lo anota en la orden), así que nunca hay que ir a buscarlo a mano.
     */
    public static String getCarpetaPdf() {
        String guardada = leerTexto(CLAVE_CARPETA_PDF, "");
        if (guardada != null && !guardada.trim().isEmpty()) {
            return guardada.trim();
        }
        File documentos = new File(System.getProperty("user.home"), "Documents");
        File base = documentos.isDirectory() ? documentos : new File(System.getProperty("user.home"));
        return new File(base, "Laboratorio - PDF").getAbsolutePath();
    }

    public static void setCarpetaPdf(String valor) {
        guardarTexto(CLAVE_CARPETA_PDF, valor == null ? "" : valor.trim());
    }

    // ------------------------------------------------------------------ copias de seguridad

    /** Copia de seguridad automática todos los días (de fábrica: sí). */
    public static boolean isRespaldoAutomatico() {
        return leerBooleano(CLAVE_RESPALDO_ACTIVO, true);
    }

    public static void setRespaldoAutomatico(boolean activo) {
        guardarBooleano(CLAVE_RESPALDO_ACTIVO, activo);
    }

    /** Hora de la copia automática, "HH:mm" (de fábrica 13:00, cuando la PC suele estar prendida). */
    public static String getHoraRespaldo() {
        String hora = leerTexto(CLAVE_RESPALDO_HORA, "13:00");
        return hora != null && hora.trim().matches("\\d{1,2}:\\d{2}") ? hora.trim() : "13:00";
    }

    public static void setHoraRespaldo(String hora) {
        guardarTexto(CLAVE_RESPALDO_HORA, hora);
    }

    /** Carpeta de las copias (de fábrica "Documentos\\Laboratorio - Copias de seguridad"). */
    public static String getCarpetaRespaldo() {
        String guardada = leerTexto(CLAVE_RESPALDO_CARPETA, "");
        if (guardada != null && !guardada.trim().isEmpty()) {
            return guardada.trim();
        }
        File documentos = new File(System.getProperty("user.home"), "Documents");
        File base = documentos.isDirectory() ? documentos : new File(System.getProperty("user.home"));
        return new File(base, "Laboratorio - Copias de seguridad").getAbsolutePath();
    }

    public static void setCarpetaRespaldo(String carpeta) {
        guardarTexto(CLAVE_RESPALDO_CARPETA, carpeta == null ? "" : carpeta.trim());
    }

    /** Cuántas copias automáticas se guardan (las más viejas se borran solas). De fábrica 30. */
    public static int getRespaldosAConservar() {
        try {
            return Math.max(1, Math.min(365, Integer.parseInt(leerTexto(CLAVE_RESPALDO_CONSERVAR, "30").trim())));
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public static void setRespaldosAConservar(int cantidad) {
        guardarTexto(CLAVE_RESPALDO_CONSERVAR, String.valueOf(cantidad));
    }

    /** Cuándo se hizo la última copia automática (milisegundos), o 0. */
    public static long getUltimoRespaldo() {
        try {
            return Long.parseLong(leerTexto(CLAVE_RESPALDO_ULTIMO, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void setUltimoRespaldo(long momento) {
        guardarTexto(CLAVE_RESPALDO_ULTIMO, String.valueOf(momento));
    }

    /**
     * Diseño predeterminado del informe (posiciones, columnas, márgenes...), como texto -- ver
     * {@link reportes.DisenoInforme#aTexto()}. Vacío = diseño de fábrica.
     */
    public static String getDisenoInforme() {
        return leerTexto(CLAVE_DISENO_INFORME, "");
    }

    public static void setDisenoInforme(String valor) {
        guardarTexto(CLAVE_DISENO_INFORME, valor == null ? "" : valor.trim());
    }

    /** {@link #COMPROBANTE_PREGUNTAR} (por defecto), {@link #COMPROBANTE_AUTOMATICO} o {@link #COMPROBANTE_NUNCA}. */
    public static String getModoComprobante() {
        String valor = leerTexto(CLAVE_MODO_COMPROBANTE, COMPROBANTE_PREGUNTAR);
        if (COMPROBANTE_AUTOMATICO.equals(valor) || COMPROBANTE_NUNCA.equals(valor)) {
            return valor;
        }
        return COMPROBANTE_PREGUNTAR;
    }

    public static void setModoComprobante(String valor) {
        guardarTexto(CLAVE_MODO_COMPROBANTE, valor == null ? COMPROBANTE_PREGUNTAR : valor);
    }

    /**
     * Nombre con el que se muestra el SISTEMA (arriba a la izquierda de la app y en el login), que
     * puede ser distinto del nombre largo del laboratorio que va en los informes. Si no se cargó,
     * se usa el nombre del laboratorio.
     */
    public static String getNombreSistema() {
        String valor = leerTexto(CLAVE_NOMBRE_SISTEMA, "");
        return valor == null || valor.trim().isEmpty() ? getNombreLaboratorio().replace('\n', ' ') : valor.trim();
    }

    /** Lo que se cargó a mano como nombre del sistema (vacío = usa el del laboratorio). */
    public static String getNombreSistemaGuardado() {
        return leerTexto(CLAVE_NOMBRE_SISTEMA, "");
    }

    public static void setNombreSistema(String valor) {
        guardarTexto(CLAVE_NOMBRE_SISTEMA, valor == null ? "" : valor.trim());
    }

    public static String getSubtituloSistema() {
        return leerTexto(CLAVE_SUBTITULO_SISTEMA, "Laboratorio de Análisis Clínicos");
    }

    public static void setSubtituloSistema(String valor) {
        guardarTexto(CLAVE_SUBTITULO_SISTEMA, valor == null ? "" : valor.trim());
    }

    /** Foto de fondo del panel izquierdo del inicio de sesión ("" = solo el color verde). */
    public static String getFotoLogin() {
        return leerTexto(CLAVE_LOGIN_FOTO, "");
    }

    public static void setFotoLogin(String ruta) {
        guardarTexto(CLAVE_LOGIN_FOTO, ruta == null ? "" : ruta.trim());
    }

    /** Mostrar el logo del laboratorio en el inicio de sesión (de fábrica: sí, si hay logo cargado). */
    public static boolean isLogoEnLogin() {
        return leerBooleano(CLAVE_LOGIN_LOGO, true);
    }

    public static void setLogoEnLogin(boolean valor) {
        guardarBooleano(CLAVE_LOGIN_LOGO, valor);
    }

    /** Mostrar el logo del laboratorio arriba a la izquierda de la app (en vez de las iniciales). */
    public static boolean isUsarLogoEnSistema() {
        return leerBooleano(CLAVE_LOGO_EN_SISTEMA, true);
    }

    public static void setUsarLogoEnSistema(boolean valor) {
        guardarBooleano(CLAVE_LOGO_EN_SISTEMA, valor);
    }

    /**
     * Imagen de la firma (y/o sello) escaneada o fotografiada de la bioquímica, para imprimirla
     * sobre la línea de firma del informe. Vacío = sin firma digitalizada.
     */
    public static String getRutaFirma() {
        return leerTexto(CLAVE_RUTA_FIRMA, "");
    }

    public static void setRutaFirma(String valor) {
        guardarTexto(CLAVE_RUTA_FIRMA, valor == null ? "" : valor.trim());
    }

    /** "Poner la firma digitalizada en el informe" (si hay una cargada). */
    public static boolean isInformeUsarFirmaImagen() {
        return leerBooleano(CLAVE_USAR_FIRMA_IMAGEN, true);
    }

    public static void setInformeUsarFirmaImagen(boolean valor) {
        guardarBooleano(CLAVE_USAR_FIRMA_IMAGEN, valor);
    }

    private static boolean leerBooleano(String clave) {
        return leerBooleano(clave, true);
    }

    private static boolean leerBooleano(String clave, boolean porDefecto) {
        Properties config = cargar();
        return Boolean.parseBoolean(config.getProperty(clave, String.valueOf(porDefecto)));
    }

    private static synchronized void guardarBooleano(String clave, boolean valor) {
        Properties config = cargar();
        config.setProperty(clave, String.valueOf(valor));
        guardar(config);
    }

    private static String leerTexto(String clave, String porDefecto) {
        Properties config = cargar();
        return config.getProperty(clave, porDefecto);
    }

    /*
     * "synchronized": ahora los PDF se arman en segundo plano (otro hilo) mientras la pantalla de
     * Configuración puede estar guardando -- así nunca se lee el archivo a medio escribir.
     */
    private static synchronized void guardarTexto(String clave, String valor) {
        Properties config = cargar();
        config.setProperty(clave, valor);
        guardar(config);
    }

    private static synchronized Properties cargar() {
        Properties config = new Properties();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            return config;
        }
        try (InputStream in = new FileInputStream(archivo)) {
            config.load(in);
        } catch (IOException e) {
            // Si no se puede leer el archivo (dañado, sin permisos), se sigue con los valores
            // por defecto -- todo activado -- en vez de romper la pantalla de Configuración.
        }
        return config;
    }

    private static synchronized void guardar(Properties config) {
        try (OutputStream out = new FileOutputStream(ARCHIVO)) {
            config.store(out, "Preferencias del sistema -- Laboratorio San Gregorio");
        } catch (IOException e) {
            // Falló el guardado (por ejemplo, sin permisos de escritura en la carpeta del
            // proyecto): el cambio del toggle queda solo en memoria del archivo Properties de esta
            // corrida hasta el próximo intento -- no bloquea el resto de la pantalla.
        }
    }
}
