package controlador;

import conexiones.Conexion;
import dao.CopiaSeguridadDAO;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import utilidades.PreferenciasSistema;

/**
 * Copias de seguridad automáticas de la base de datos, "como WhatsApp": se elige la hora y la
 * carpeta una vez, y el sistema hace solo una copia completa por día.
 *
 * <ul>
 *   <li>Mientras el sistema está abierto, revisa cada minuto si ya es la hora elegida.</li>
 *   <li>Si a esa hora la PC estaba apagada o el sistema cerrado, la copia se hace apenas se
 *       vuelve a abrir (nunca se pierde un día).</li>
 *   <li>Se guardan las últimas N copias automáticas (las más viejas se borran solas); las
 *       manuales y las de "antes de restaurar" no se borran nunca.</li>
 *   <li>Si la carpeta elegida está dentro de Google Drive, OneDrive o Dropbox, esos programas
 *       la suben solos a la nube: queda una copia en la PC y otra en internet.</li>
 * </ul>
 *
 * <p>Todo corre en un hilo aparte, así nunca tilda la pantalla.</p>
 */
public final class CopiasSeguridad {

    private static final ScheduledExecutorService HILO = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "copias-de-seguridad");
        t.setDaemon(true);
        return t;
    });
    private static final long REINTENTO_MS = 30L * 60 * 1000;

    private static boolean iniciado;
    private static long ultimoFallo;
    private static Consumer<String> alTerminarBien;
    private static Consumer<String> alFallar;

    private CopiasSeguridad() {
    }

    /**
     * Arranca la revisión automática (una sola vez, la llama la pantalla principal al abrir).
     *
     * @param avisoBien qué hacer cuando se hizo una copia automática (por ejemplo un cartelito)
     * @param avisoError qué hacer si falló
     */
    public static synchronized void iniciar(Consumer<String> avisoBien, Consumer<String> avisoError) {
        alTerminarBien = avisoBien;
        alFallar = avisoError;
        if (iniciado) {
            return;
        }
        iniciado = true;
        // La primera revisión, un rato después de abrir (para no demorar el arranque).
        HILO.scheduleWithFixedDelay(CopiasSeguridad::revisar, 40, 60, TimeUnit.SECONDS);
    }

    private static void revisar() {
        try {
            if (!PreferenciasSistema.isRespaldoAutomatico()) {
                return;
            }
            long ahora = System.currentTimeMillis();
            if (ahora - ultimoFallo < REINTENTO_MS) {
                return;
            }
            if (PreferenciasSistema.getUltimoRespaldo() >= ultimoMomentoProgramado(ahora)) {
                return; // la de hoy (o la que tocaba) ya está hecha
            }
            File copia = hacerCopia("auto");
            PreferenciasSistema.setUltimoRespaldo(System.currentTimeMillis());
            borrarViejas();
            if (alTerminarBien != null) {
                alTerminarBien.accept("Copia de seguridad automática guardada (" + copia.getName() + ")");
            }
        } catch (Exception e) {
            ultimoFallo = System.currentTimeMillis();
            if (alFallar != null) {
                alFallar.accept("No se pudo hacer la copia de seguridad automática: " + e.getMessage()
                        + ". Se vuelve a intentar en media hora.");
            }
        }
    }

    /** El último momento (hoy o ayer) en que tocaba hacer copia, según la hora elegida. */
    static long ultimoMomentoProgramado(long ahora) {
        String[] partes = PreferenciasSistema.getHoraRespaldo().split(":");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ahora);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(partes[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(partes[1]));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() > ahora) {
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return c.getTimeInMillis();
    }

    /** Cuándo toca la próxima copia automática (para mostrarlo en pantalla). */
    public static long proximoMomento() {
        long ahora = System.currentTimeMillis();
        long ultimo = ultimoMomentoProgramado(ahora);
        if (PreferenciasSistema.getUltimoRespaldo() < ultimo) {
            return ahora; // está pendiente: se hace en el próximo minuto
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ultimo);
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static File carpeta() {
        return new File(PreferenciasSistema.getCarpetaRespaldo());
    }

    /**
     * Hace una copia completa ahora mismo. Tarda unos segundos: llamarla en segundo plano.
     *
     * @param tipo "auto", "manual" o "antes-de-restaurar"
     */
    public static synchronized File hacerCopia(String tipo) throws SQLException, java.io.IOException {
        try (Connection con = conectar()) {
            return CopiaSeguridadDAO.crearCopia(con, carpeta(), tipo);
        }
    }

    /**
     * Vuelve la base al estado de la copia elegida. ANTES hace una copia de cómo está todo ahora
     * (por si se eligió la copia equivocada, se puede volver atrás).
     *
     * @return la copia de "antes de restaurar" que se hizo
     */
    public static synchronized File restaurar(File archivo) throws SQLException, java.io.IOException {
        if (!archivo.isFile()) {
            throw new java.io.IOException("No se encontró el archivo " + archivo.getAbsolutePath());
        }
        if (!CopiaSeguridadDAO.pareceCopia(archivo)) {
            throw new java.io.IOException("Ese archivo no parece una copia de seguridad de la base de datos "
                    + "(no tiene tablas adentro).");
        }
        File antes = hacerCopia("antes-de-restaurar");
        try (Connection con = conectar()) {
            CopiaSeguridadDAO.restaurar(con, archivo);
        }
        return antes;
    }

    /** Copias que hay en la carpeta, de la más nueva a la más vieja. */
    public static List<File> listar() {
        File[] archivos = carpeta().listFiles((dir, nombre) -> nombre.startsWith(CopiaSeguridadDAO.PREFIJO)
                && nombre.endsWith(".sql"));
        List<File> lista = archivos == null ? new ArrayList<File>() : new ArrayList<>(Arrays.asList(archivos));
        // El nombre lleva la fecha (año-mes-día_hora): ordenarlo por nombre es ordenarlo por fecha.
        lista.sort((a, b) -> b.getName().compareTo(a.getName()));
        return lista;
    }

    /** Deja solo las últimas N copias AUTOMÁTICAS (las manuales no se tocan). */
    public static void borrarViejas() {
        int conservar = PreferenciasSistema.getRespaldosAConservar();
        int automaticas = 0;
        for (File f : listar()) {
            if (f.getName().endsWith("_auto.sql")) {
                automaticas++;
                if (automaticas > conservar) {
                    f.delete();
                }
            }
        }
    }

    /** Carpetas de Google Drive / OneDrive / Dropbox que existen en esta PC (para sugerirlas). */
    public static List<File> carpetasEnLaNube() {
        List<File> lista = new ArrayList<>();
        File home = new File(System.getProperty("user.home"));
        File[] enHome = home.listFiles(f -> f.isDirectory() && (f.getName().startsWith("OneDrive")
                || f.getName().equals("Google Drive") || f.getName().equals("Dropbox")));
        if (enHome != null) {
            lista.addAll(Arrays.asList(enHome));
        }
        for (String letra : new String[]{"G", "H", "I"}) {
            for (String nombre : new String[]{"Mi unidad", "My Drive"}) {
                File f = new File(letra + ":\\" + nombre);
                if (f.isDirectory()) {
                    lista.add(f);
                }
            }
        }
        return lista;
    }

    private static Connection conectar() throws SQLException {
        Connection con = Conexion.conectar();
        if (con == null) {
            throw new SQLException("No hay conexión con la base de datos (¿está prendido MySQL?)");
        }
        return con;
    }
}
