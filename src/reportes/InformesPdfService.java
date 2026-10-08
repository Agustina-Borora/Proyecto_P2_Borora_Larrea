package reportes;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import dao.InformeDAO;
import utilidades.PreferenciasSistema;

/**
 * Todo lo que tiene que ver con los PDF, en un solo lugar:
 *
 * <ul>
 *   <li>Arma el informe de una orden (todos sus estudios juntos) y lo guarda SOLO como PDF en la
 *       carpeta configurada ({@code <carpeta>\Informes}), anotando en la orden dónde quedó -- así
 *       el sistema siempre sabe dónde está cada PDF para adjuntarlo o abrirlo.</li>
 *   <li>Guarda el Comprobante de Orden como PDF ({@code <carpeta>\Comprobantes}).</li>
 *   <li>Vuelve a generar solos los PDF ya guardados cuando cambia algo que aparece en ellos: un
 *       color o un rango en el Catálogo de Exámenes, un resultado corregido, los datos del
 *       paciente o el diseño. (Se hace en segundo plano y no frena nada.)</li>
 * </ul>
 *
 * <p>Nada de esta clase muestra carteles ni toca la pantalla: puede (y debe) llamarse desde un
 * hilo en segundo plano. Si algo falla, tira una excepción con un mensaje entendible.</p>
 */
public final class InformesPdfService {

    /** Para las regeneraciones automáticas: un solo hilo, de a una por vez, en segundo plano. */
    private static final ScheduledExecutorService REGENERADOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "regenerar-pdf");
        t.setDaemon(true);
        return t;
    });
    private static final Set<Integer> EXAMENES_PENDIENTES = new HashSet<>();
    private static ScheduledFuture<?> regeneracionProgramada;

    private InformesPdfService() {
    }

    // ------------------------------------------------------------------ carpetas y nombres

    public static File carpetaBase() {
        return new File(PreferenciasSistema.getCarpetaPdf());
    }

    public static File carpetaInformes() {
        return new File(carpetaBase(), "Informes");
    }

    public static File carpetaComprobantes() {
        return new File(carpetaBase(), "Comprobantes");
    }

    /** Ej: "Informe_2026-10-0003_Torres_Ana_Maria.pdf". */
    public static File archivoInforme(ResultadosDatos datos) {
        return new File(carpetaInformes(), "Informe_" + limpiarNombre(datos.getNumeroOrden()) + "_"
                + limpiarNombre(datos.getNombrePaciente()) + ".pdf");
    }

    public static File archivoComprobante(ComprobanteOrdenDatos datos) {
        return new File(carpetaComprobantes(), "Comprobante_" + limpiarNombre(datos.getNumeroOrden()) + "_"
                + limpiarNombre(datos.getNombrePaciente()) + ".pdf");
    }

    /** Saca tildes y caracteres que Windows no acepta en un nombre de archivo. */
    static String limpiarNombre(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "sin_dato";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpio = sinTildes.replaceAll("[^A-Za-z0-9\\-]+", "_").replaceAll("_+", "_");
        limpio = limpio.replaceAll("^_|_$", "");
        if (limpio.length() > 60) {
            limpio = limpio.substring(0, 60);
        }
        return limpio.isEmpty() ? "sin_dato" : limpio;
    }

    // ------------------------------------------------------------------ datos y diseño

    /** Abre una conexión propia (sin carteles) -- pensada para hilos en segundo plano. */
    static Connection conectar() throws SQLException {
        Connection con = conexiones.Conexion.conectar();
        if (con == null) {
            throw new SQLException("No se pudo conectar a la base de datos (¿está prendido MySQL?).");
        }
        return con;
    }

    public static int idPedidoDe(int idPedidoAnalisis) throws SQLException {
        try (Connection con = conectar()) {
            return InformeDAO.idPedidoDe(con, idPedidoAnalisis);
        }
    }

    /**
     * Los datos del informe de la orden, CON los cambios de texto que se le hayan hecho a mano
     * desde la vista previa (ver {@link AjustesInforme}). Es lo que se usa para el PDF, la
     * impresión y el envío.
     */
    public static ResultadosDatos cargarDatos(int idPedido) throws SQLException {
        return leerAjustes(idPedido).aplicar(cargarDatosOriginales(idPedido));
    }

    /** Los datos tal cual salen de la base y del Catálogo, sin cambios a mano. */
    public static ResultadosDatos cargarDatosOriginales(int idPedido) throws SQLException {
        try (Connection con = conectar()) {
            ResultadosDatos datos = InformeDAO.cargarInforme(con, idPedido);
            if (datos == null) {
                throw new SQLException("No se encontró la orden.");
            }
            return datos;
        }
    }

    /** Cambios de texto hechos a mano en el informe de la orden (vacío si no hay, o si falta el script SQL). */
    public static AjustesInforme leerAjustes(int idPedido) {
        try (Connection con = conectar()) {
            return AjustesInforme.desdeTexto(InformeDAO.leerAjustes(con, idPedido));
        } catch (SQLException e) {
            return new AjustesInforme();
        }
    }

    public static void guardarAjustes(int idPedido, AjustesInforme ajustes) throws SQLException {
        try (Connection con = conectar()) {
            InformeDAO.guardarAjustes(con, idPedido, ajustes == null ? null : ajustes.aTexto());
        } catch (SQLException e) {
            if (InformeDAO.faltaScript(e)) {
                throw new SQLException("Para guardar los cambios de texto del informe falta correr el script "
                        + "sql/2026-10-08_informes_pdf.sql en MySQL Workbench (se puede correr de nuevo sin problema).", e);
            }
            throw e;
        }
    }

    /**
     * Pasa al Catálogo el nombre/unidad/referencia de un analito (para todas las órdenes) y
     * actualiza solos los PDF ya guardados que lo usan.
     */
    public static void guardarEnCatalogo(int idAnalito, String nombre, String unidad, String referenciaCruda)
            throws SQLException {
        int idExamen;
        try (Connection con = conectar()) {
            InformeDAO.guardarEnCatalogo(con, idAnalito, nombre, unidad, referenciaCruda);
            idExamen = InformeDAO.examenDeAnalito(con, idAnalito);
        }
        if (idExamen > 0) {
            examenModificado(idExamen);
        }
    }

    /** Diseño predeterminado (Configuración &gt; Informes PDF). */
    public static DisenoInforme disenoPredeterminado() {
        return DisenoInforme.desdeTexto(PreferenciasSistema.getDisenoInforme());
    }

    /**
     * Diseño propio de la orden, o null si usa el predeterminado (o si todavía no se corrió el
     * script SQL que agrega esa columna).
     */
    public static DisenoInforme disenoPropio(int idPedido) {
        try (Connection con = conectar()) {
            String texto = InformeDAO.leerDiseno(con, idPedido);
            return texto == null || texto.trim().isEmpty() ? null : DisenoInforme.desdeTexto(texto);
        } catch (SQLException e) {
            return null;
        }
    }

    /** El diseño que se usa para una orden: el suyo propio si tiene, si no el predeterminado. */
    public static DisenoInforme disenoParaPedido(int idPedido) {
        DisenoInforme propio = disenoPropio(idPedido);
        return propio != null ? propio : disenoPredeterminado();
    }

    /**
     * Guarda un diseño solo para esta orden (o lo borra, con null, para que vuelva a usar el
     * predeterminado).
     *
     * @throws SQLException si falta correr el script SQL (con un mensaje que lo explica).
     */
    public static void guardarDisenoPropio(int idPedido, DisenoInforme diseno) throws SQLException {
        try (Connection con = conectar()) {
            InformeDAO.guardarDiseno(con, idPedido, diseno == null ? null : diseno.aTexto());
        } catch (SQLException e) {
            if (InformeDAO.faltaScript(e)) {
                throw new SQLException("Para guardar un diseño propio por orden falta correr el script "
                        + "sql/2026-10-08_informes_pdf.sql en MySQL Workbench. Mientras tanto podés guardarlo "
                        + "como diseño para todas las órdenes.", e);
            }
            throw e;
        }
    }

    public static DocumentoMaquetado maquetar(ResultadosDatos datos, DisenoInforme diseno) {
        return MaquetadorInforme.armar(datos, diseno, ConfigInforme.actual(), false);
    }

    // ------------------------------------------------------------------ guardar PDF

    /** Arma y guarda el PDF de la orden con su diseño, y anota en la orden dónde quedó. */
    public static File guardarInforme(int idPedido) throws SQLException, IOException {
        ResultadosDatos datos = cargarDatos(idPedido);
        return guardarInforme(datos, disenoParaPedido(idPedido));
    }

    /** Igual, con datos y diseño ya cargados (por ejemplo, lo que se está viendo en la vista previa). */
    public static File guardarInforme(ResultadosDatos datos, DisenoInforme diseno) throws IOException {
        File destino = archivoInforme(datos);
        EscritorPdf.guardar(maquetar(datos, diseno), destino);
        anotarRuta(datos.getIdPedido(), destino);
        return destino;
    }

    /** Anota la ruta en la orden, y si antes estaba en otro lado (cambió el nombre del paciente o la carpeta), borra el viejo. */
    private static void anotarRuta(int idPedido, File nuevo) {
        if (idPedido <= 0) {
            return;
        }
        try (Connection con = conectar()) {
            String anterior = InformeDAO.leerRutaPdf(con, idPedido);
            InformeDAO.guardarRutaPdf(con, idPedido, nuevo.getAbsolutePath());
            if (anterior != null && !anterior.trim().isEmpty()) {
                File viejo = new File(anterior);
                if (!viejo.getAbsoluteFile().equals(nuevo.getAbsoluteFile()) && viejo.isFile()
                        && viejo.getName().startsWith("Informe_")) {
                    viejo.delete();
                }
            }
        } catch (SQLException e) {
            // Sin el script SQL no se puede anotar la ruta: el PDF igual quedó guardado en la
            // carpeta de siempre, con un nombre fijo, así que el sistema lo vuelve a encontrar.
        }
    }

    /** El PDF ya guardado de la orden (si existe), sin generar nada. */
    public static File pdfExistente(int idPedido) {
        try (Connection con = conectar()) {
            String ruta = InformeDAO.leerRutaPdf(con, idPedido);
            if (ruta != null && new File(ruta).isFile()) {
                return new File(ruta);
            }
        } catch (SQLException e) {
            // Sigue abajo con el nombre fijo.
        }
        try {
            File porNombre = archivoInforme(cargarDatos(idPedido));
            return porNombre.isFile() ? porNombre : null;
        } catch (SQLException e) {
            return null;
        }
    }

    public static File guardarComprobante(ComprobanteOrdenDatos datos) throws IOException {
        File destino = archivoComprobante(datos);
        EscritorPdf.guardar(MaquetadorComprobante.armar(datos, disenoPredeterminado(), ConfigInforme.actual()), destino);
        return destino;
    }

    public static DocumentoMaquetado maquetarComprobante(ComprobanteOrdenDatos datos) {
        return MaquetadorComprobante.armar(datos, disenoPredeterminado(), ConfigInforme.actual());
    }

    // ------------------------------------------------------------------ regeneración automática

    /**
     * Se cambió un examen en el Catálogo (por ejemplo el color o el rango de una referencia):
     * vuelve a generar, en segundo plano, los PDF ya guardados de las órdenes que lo incluyen.
     * Espera 2 segundos por si se están guardando varios cambios seguidos, y los hace todos juntos.
     */
    public static void examenModificado(int idAnalisisTipo) {
        synchronized (EXAMENES_PENDIENTES) {
            EXAMENES_PENDIENTES.add(idAnalisisTipo);
            if (regeneracionProgramada != null) {
                regeneracionProgramada.cancel(false);
            }
            regeneracionProgramada = REGENERADOR.schedule(InformesPdfService::regenerarExamenesPendientes, 2,
                    TimeUnit.SECONDS);
        }
    }

    private static void regenerarExamenesPendientes() {
        Set<Integer> examenes;
        synchronized (EXAMENES_PENDIENTES) {
            examenes = new HashSet<>(EXAMENES_PENDIENTES);
            EXAMENES_PENDIENTES.clear();
        }
        Set<Integer> pedidos = new HashSet<>();
        try (Connection con = conectar()) {
            for (Integer idExamen : examenes) {
                pedidos.addAll(InformeDAO.pedidosConPdfPorExamen(con, idExamen));
            }
        } catch (SQLException e) {
            return; // sin el script SQL no hay registro de qué PDF existen: no hay nada que regenerar
        }
        for (Integer idPedido : pedidos) {
            regenerarSiExiste(idPedido);
        }
    }

    /** Se guardó/corrigió un resultado de un examen: regenera el PDF de su orden si ya existía. */
    public static void resultadoModificado(int idPedidoAnalisis) {
        REGENERADOR.execute(() -> {
            try {
                regenerarSiExiste(idPedidoDe(idPedidoAnalisis));
            } catch (SQLException e) {
                // no hay nada más para hacer en segundo plano
            }
        });
    }

    /** Se editaron datos de la orden o del paciente: regenera su PDF si ya existía. */
    public static void pedidoModificado(int idPedido) {
        REGENERADOR.execute(() -> regenerarSiExiste(idPedido));
    }

    /**
     * Vuelve a generar TODOS los PDF ya guardados (por ejemplo después de cambiar el diseño o
     * los datos del laboratorio). Devuelve cuántos se actualizaron. Es lento si hay muchos:
     * llamarlo siempre desde un hilo en segundo plano.
     */
    public static int regenerarTodos() {
        List<Integer> pedidos;
        try (Connection con = conectar()) {
            pedidos = InformeDAO.pedidosConPdf(con);
        } catch (SQLException e) {
            return 0;
        }
        int cantidad = 0;
        for (Integer idPedido : pedidos) {
            if (regenerarSiExiste(idPedido)) {
                cantidad++;
            }
        }
        return cantidad;
    }

    // ------------------------------------------------------------------ registro evolutivo

    public static File carpetaEvoluciones() {
        return new File(carpetaBase(), "Evoluciones");
    }

    public static java.util.List<dao.EvolucionPacienteDAO.PacienteEncontrado> buscarPacientes(String texto)
            throws SQLException {
        try (Connection con = conectar()) {
            return dao.EvolucionPacienteDAO.buscar(con, texto);
        }
    }

    public static modelo.EvolucionPaciente cargarEvolucion(int idPaciente) throws SQLException {
        try (Connection con = conectar()) {
            return dao.EvolucionPacienteDAO.cargar(con, idPaciente);
        }
    }

    public static DocumentoMaquetado maquetarEvolucion(modelo.EvolucionPaciente ev) {
        return MaquetadorEvolucion.armar(ev, disenoPredeterminado(), ConfigInforme.actual());
    }

    /** Guarda el PDF del registro evolutivo en {@code <carpeta>\\Evoluciones} (uno por paciente y día). */
    public static File guardarEvolucion(modelo.EvolucionPaciente ev) throws IOException {
        String dia = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        File destino = new File(carpetaEvoluciones(), "Evolucion_" + limpiarNombre(ev.getNombre()) + "_" + dia + ".pdf");
        EscritorPdf.guardar(maquetarEvolucion(ev), destino);
        return destino;
    }

    // ------------------------------------------------------------------ cambio de carpeta

    /**
     * Cambia la carpeta de los PDF y MUEVE ahí todo lo que ya estaba guardado (informes y
     * comprobantes), anotando las rutas nuevas en las órdenes y en los envíos. Así nunca quedan
     * PDF repartidos entre la carpeta vieja y la nueva. También trae los informes que, por algún
     * motivo, hayan quedado en otra carpeta distinta. Llamarlo en segundo plano.
     *
     * @return cuántos archivos se movieron
     */
    public static int cambiarCarpeta(String nuevaCarpeta) throws IOException {
        File vieja = carpetaBase().getAbsoluteFile();
        File nueva = new File(nuevaCarpeta).getAbsoluteFile();
        PreferenciasSistema.setCarpetaPdf(nueva.getAbsolutePath());
        if (mismaCarpeta(vieja, nueva)) {
            return 0;
        }
        File informesNuevos = new File(nueva, "Informes");
        File comprobantesNuevos = new File(nueva, "Comprobantes");
        if (!informesNuevos.isDirectory() && !informesNuevos.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + informesNuevos.getAbsolutePath()
                    + ". Fijate que exista y que se pueda escribir ahí.");
        }
        comprobantesNuevos.mkdirs();

        int movidos = 0;
        java.util.Map<String, String> cambios = new java.util.HashMap<>(); // ruta vieja -> ruta nueva
        movidos += moverContenido(new File(vieja, "Informes"), informesNuevos, cambios);
        movidos += moverContenido(new File(vieja, "Comprobantes"), comprobantesNuevos, cambios);
        File evolucionesViejas = new File(vieja, "Evoluciones");
        if (evolucionesViejas.isDirectory()) {
            File evolucionesNuevas = new File(nueva, "Evoluciones");
            evolucionesNuevas.mkdirs();
            movidos += moverContenido(evolucionesViejas, evolucionesNuevas, cambios);
            borrarSiVacia(evolucionesViejas);
        }

        try (Connection con = conectar()) {
            for (Object[] fila : InformeDAO.rutasPdf(con)) {
                int idPedido = (Integer) fila[0];
                String ruta = (String) fila[1];
                String rutaNueva = cambios.get(new File(ruta).getAbsolutePath());
                if (rutaNueva == null) {
                    // Quedó en otra carpeta (por ejemplo una anterior): se trae también.
                    File suelto = new File(ruta);
                    if (suelto.isFile() && !mismaCarpeta(suelto.getParentFile(), informesNuevos)) {
                        File destino = new File(informesNuevos, suelto.getName());
                        mover(suelto, destino);
                        borrarSiVacia(suelto.getParentFile());
                        movidos++;
                        rutaNueva = destino.getAbsolutePath();
                        cambios.put(suelto.getAbsolutePath(), rutaNueva);
                    }
                }
                if (rutaNueva != null) {
                    InformeDAO.guardarRutaPdf(con, idPedido, rutaNueva);
                }
            }
            for (java.util.Map.Entry<String, String> e : cambios.entrySet()) {
                InformeDAO.cambiarRutaEnvios(con, e.getKey(), e.getValue());
            }
        } catch (SQLException e) {
            // Sin el script SQL no hay rutas anotadas: los archivos igual quedaron movidos y el
            // sistema los encuentra por su nombre en la carpeta nueva.
        }
        borrarSiVacia(new File(vieja, "Informes"));
        borrarSiVacia(new File(vieja, "Comprobantes"));
        if ("Laboratorio - PDF".equals(vieja.getName())) {
            borrarSiVacia(vieja); // solo la carpeta que creó el sistema, nunca una elegida a mano
        }
        return movidos;
    }

    private static int moverContenido(File origen, File destino, java.util.Map<String, String> cambios) throws IOException {
        File[] archivos = origen.listFiles();
        if (archivos == null) {
            return 0;
        }
        int movidos = 0;
        for (File f : archivos) {
            if (!f.isFile()) {
                continue;
            }
            File nuevo = new File(destino, f.getName());
            mover(f, nuevo);
            cambios.put(f.getAbsolutePath(), nuevo.getAbsolutePath());
            movidos++;
        }
        return movidos;
    }

    private static void mover(File origen, File destino) throws IOException {
        try {
            java.nio.file.Files.move(origen.toPath(), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.FileSystemException e) {
            throw new IOException("No se pudo mover \"" + origen.getName() + "\". Si está abierto (por ejemplo en el "
                    + "lector de PDF o en WhatsApp), cerralo y probá de nuevo.", e);
        }
    }

    private static boolean mismaCarpeta(File a, File b) {
        if (a == null || b == null) {
            return false;
        }
        try {
            return a.getCanonicalFile().equals(b.getCanonicalFile());
        } catch (IOException e) {
            return a.getAbsoluteFile().equals(b.getAbsoluteFile());
        }
    }

    private static void borrarSiVacia(File carpeta) {
        String[] dentro = carpeta.list();
        if (dentro != null && dentro.length == 0) {
            carpeta.delete();
        }
    }

    /** Regenera el PDF de la orden solo si ya había uno guardado. true si lo regeneró. */
    private static boolean regenerarSiExiste(int idPedido) {
        if (idPedido <= 0 || pdfExistente(idPedido) == null) {
            return false;
        }
        try {
            guardarInforme(idPedido);
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo actualizar el PDF de la orden " + idPedido + ": " + e.getMessage());
            return false;
        }
    }
}
