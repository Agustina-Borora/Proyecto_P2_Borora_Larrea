package reportes;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import utilidades.PreferenciasSistema;

/**
 * Foto de los datos del laboratorio y de las opciones "qué mostrar" de Configuración, tomada una
 * sola vez al armar cada informe (en vez de leer el archivo de preferencias por cada línea que se
 * dibuja). Se puede armar también a mano para la vista previa de Configuración, con lo que la
 * bioquímica todavía no guardó.
 */
public class ConfigInforme {

    public String nombreLaboratorio = "";
    public String subtitulo = "";
    public String direccion = "";
    public String telefono = "";
    public String localidad = "";
    public String email = "";
    public String cuit = "";
    public String bioquimica = "";
    public String matricula = "";
    public String habilitacion = "";
    public String leyendaPie = "";
    public String rutaLogo = "";
    public String rutaFirma = "";
    public boolean usarFirmaImagen = true;

    public boolean mostrarLogo = true;
    public boolean incluirReferencia = true;
    public boolean resaltarFueraDeRango = true;
    public boolean incluirMedico = true;
    public boolean mostrarHabilitacion = true;
    public boolean incluirComprobante = false;

    private BufferedImage logoCargado;
    private String rutaLogoCargado;
    private BufferedImage firmaCargada;
    private String rutaFirmaCargada;

    /** La imagen de la firma digitalizada, o null si no hay, está apagada o no se pudo leer. */
    public synchronized BufferedImage firma() {
        if (!usarFirmaImagen || rutaFirma == null || rutaFirma.trim().isEmpty()) {
            return null;
        }
        if (firmaCargada != null && rutaFirma.equals(rutaFirmaCargada)) {
            return firmaCargada;
        }
        try {
            File archivo = new File(rutaFirma.trim());
            if (!archivo.isFile()) {
                return null;
            }
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                return null;
            }
            firmaCargada = achicar(original, 600);
            rutaFirmaCargada = rutaFirma;
            return firmaCargada;
        } catch (Exception e) {
            return null;
        }
    }

    /** Lee todo lo que está guardado en Configuración ahora mismo. */
    public static ConfigInforme actual() {
        ConfigInforme c = new ConfigInforme();
        c.nombreLaboratorio = PreferenciasSistema.getNombreLaboratorio();
        c.subtitulo = PreferenciasSistema.getSubtitulo();
        c.direccion = PreferenciasSistema.getDireccion();
        c.telefono = PreferenciasSistema.getTelefono();
        c.localidad = PreferenciasSistema.getLocalidad();
        c.email = PreferenciasSistema.getEmailContacto();
        c.cuit = PreferenciasSistema.getCuit();
        c.bioquimica = PreferenciasSistema.getBioquimicaResponsable();
        c.matricula = PreferenciasSistema.getMatriculaBioquimica();
        c.habilitacion = PreferenciasSistema.getHabilitacionNumero();
        c.leyendaPie = PreferenciasSistema.getLeyendaPie();
        c.rutaLogo = PreferenciasSistema.getRutaLogo();
        c.rutaFirma = PreferenciasSistema.getRutaFirma();
        c.usarFirmaImagen = PreferenciasSistema.isInformeUsarFirmaImagen();
        c.mostrarLogo = PreferenciasSistema.isInformeMostrarLogo();
        c.incluirReferencia = PreferenciasSistema.isInformeIncluirReferencia();
        c.resaltarFueraDeRango = PreferenciasSistema.isInformeResaltarFueraDeRango();
        c.incluirMedico = PreferenciasSistema.isInformeIncluirMedico();
        c.mostrarHabilitacion = PreferenciasSistema.isInformeMostrarHabilitacion();
        c.incluirComprobante = PreferenciasSistema.isInformeIncluirComprobante();
        return c;
    }

    /**
     * El logo ya leído y achicado (como mucho 360 px de lado -- de sobra para imprimirlo nítido a
     * ~2 cm, y el PDF queda liviano). null si no hay logo, está apagado o no se pudo leer: el
     * informe sale igual, sin logo.
     */
    public synchronized BufferedImage logo() {
        if (!mostrarLogo || rutaLogo == null || rutaLogo.trim().isEmpty()) {
            return null;
        }
        if (logoCargado != null && rutaLogo.equals(rutaLogoCargado)) {
            return logoCargado;
        }
        try {
            File archivo = new File(rutaLogo.trim());
            if (!archivo.isFile()) {
                return null;
            }
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                return null;
            }
            logoCargado = achicar(original, 360);
            rutaLogoCargado = rutaLogo;
            return logoCargado;
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage achicar(BufferedImage original, int ladoMax) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1.0, ladoMax / (double) Math.max(ancho, alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));
        BufferedImage resultado = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resultado.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();
        return resultado;
    }

    static boolean tiene(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }
}
