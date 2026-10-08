package reportes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import modelo.TramoTexto;

/**
 * Todo lo que va en el "Informe de Resultados" de UNA ORDEN completa: los datos del paciente y
 * TODOS sus estudios terminados juntos (antes salía un PDF por cada examen suelto, ahora sale uno
 * solo por orden). Se arma desde la base con {@link dao.InformeDAO#cargarInforme}.
 *
 * <p>Los valores de referencia (con sus colores) se leen del Catálogo de Exámenes al momento de
 * armar el informe -- por eso, si se cambia un color o un rango en el Catálogo, el PDF de la
 * orden se vuelve a generar con lo nuevo (ver {@link InformesPdfService}).</p>
 */
public class ResultadosDatos {

    /** Una fila de la tabla: un parámetro (analito) con su valor, unidad, referencia y estado. */
    public static final class Fila {
        private final String nombreParametro;
        private final String valor;
        private final String unidad;
        private final String referencia;
        private final List<TramoTexto> tramosReferencia;
        private final String estado;
        private final int idAnalito;

        public Fila(String nombreParametro, String valor, String unidad, String referencia,
                List<TramoTexto> tramosReferencia, String estado) {
            this(0, nombreParametro, valor, unidad, referencia, tramosReferencia, estado);
        }

        public Fila(int idAnalito, String nombreParametro, String valor, String unidad, String referencia,
                List<TramoTexto> tramosReferencia, String estado) {
            this.idAnalito = idAnalito;
            this.nombreParametro = nombreParametro;
            this.valor = valor;
            this.unidad = unidad;
            this.referencia = referencia;
            this.tramosReferencia = tramosReferencia == null ? Collections.<TramoTexto>emptyList() : tramosReferencia;
            this.estado = estado;
        }

        /** id del analito en el Catálogo (0 en los datos de ejemplo). */
        public int getIdAnalito() {
            return idAnalito;
        }

        public String getNombreParametro() {
            return nombreParametro;
        }

        public String getValor() {
            return valor;
        }

        public String getUnidad() {
            return unidad;
        }

        /** Referencia en texto plano (sin colores). */
        public String getReferencia() {
            return referencia;
        }

        /** La misma referencia separada en tramos de color. Nunca null. */
        public List<TramoTexto> getTramosReferencia() {
            return tramosReferencia;
        }

        /** "Normal", "Alto", "Bajo", o null si no se pudo calcular. */
        public String getEstado() {
            return estado;
        }
    }

    /** Un estudio (examen) de la orden, con su tabla de resultados. */
    public static final class Estudio {
        private final int idPedidoAnalisis;
        private final String nombre;
        private final String observacion;
        private final List<Fila> filas;

        public Estudio(int idPedidoAnalisis, String nombre, String observacion, List<Fila> filas) {
            this.idPedidoAnalisis = idPedidoAnalisis;
            this.nombre = nombre;
            this.observacion = observacion;
            this.filas = filas == null ? new ArrayList<Fila>() : filas;
        }

        public int getIdPedidoAnalisis() {
            return idPedidoAnalisis;
        }

        public String getNombre() {
            return nombre;
        }

        public String getObservacion() {
            return observacion;
        }

        public List<Fila> getFilas() {
            return filas;
        }
    }

    private int idPedido;
    private String numeroOrden;
    private Date fecha;
    private String nombrePaciente;
    private String dni;
    private int edad;
    private String telefono;
    private String email;
    private String medicoDerivante;
    private String cobertura;
    private final List<Estudio> estudios = new ArrayList<>();
    private final List<String> estudiosPendientes = new ArrayList<>();
    private ComprobanteOrdenDatos comprobante;
    private String notaFinal;

    /** Nota libre al final del informe (se escribe en la vista previa), o null. */
    public String getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(String notaFinal) {
        this.notaFinal = notaFinal;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(String numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente = nombrePaciente;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMedicoDerivante() {
        return medicoDerivante;
    }

    public void setMedicoDerivante(String medicoDerivante) {
        this.medicoDerivante = medicoDerivante;
    }

    /** Nombre de la obra social, o "Particular". */
    public String getCobertura() {
        return cobertura;
    }

    public void setCobertura(String cobertura) {
        this.cobertura = cobertura;
    }

    /** Estudios ya completados (los que tienen resultados para informar). */
    public List<Estudio> getEstudios() {
        return estudios;
    }

    /** Nombres de los estudios de la orden que todavía no están terminados. */
    public List<String> getEstudiosPendientes() {
        return estudiosPendientes;
    }

    /** Datos del comprobante de la orden (para agregarlo al final si así se configuró), o null. */
    public ComprobanteOrdenDatos getComprobante() {
        return comprobante;
    }

    public void setComprobante(ComprobanteOrdenDatos comprobante) {
        this.comprobante = comprobante;
    }

    /** Nombres de los estudios incluidos, separados por coma (para el email / la pantalla). */
    public String nombresEstudios() {
        StringBuilder sb = new StringBuilder();
        for (Estudio e : estudios) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e.getNombre());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ ejemplo

    /**
     * Datos inventados para la vista previa de Configuración (no salen de la base): sirven para
     * ver cómo queda el diseño sin tener que abrir una orden real. Incluye a propósito una
     * referencia larga y con colores, un valor alto y uno bajo.
     */
    public static ResultadosDatos ejemplo() {
        ResultadosDatos d = new ResultadosDatos();
        d.setIdPedido(0);
        d.setNumeroOrden("2026-10-0001");
        d.setFecha(new Date());
        d.setNombrePaciente("Torres, Ana María");
        d.setDni("30.111.222");
        d.setEdad(42);
        d.setMedicoDerivante("Dr. Gómez, Martín");
        d.setCobertura("OSDE");

        List<Fila> hemograma = new ArrayList<>();
        hemograma.add(new Fila("Glóbulos rojos", "4.6", "x10⁶/µL", "4.2 - 5.4 x10⁶/µL",
                tramos("4.2 - 5.4 x10⁶/µL", null), "Normal"));
        hemograma.add(new Fila("Hemoglobina", "11.2", "g/dL", "M: 12 - 16 g/dL  H: 14 - 18 g/dL",
                tramos("M: 12 - 16 g/dL", "DB2777", "  H: 14 - 18 g/dL", "2563EB"), "Bajo"));
        hemograma.add(new Fila("Glóbulos blancos", "12800", "/mm³", "4500 - 11000 /mm³",
                tramos("4500 - 11000 /mm³", null), "Alto"));
        hemograma.add(new Fila("Plaquetas", "250000", "/mm³", "150000 - 450000 /mm³",
                tramos("150000 - 450000 /mm³", null), "Normal"));
        d.getEstudios().add(new Estudio(-1, "Hemograma completo", null, hemograma));

        List<Fila> glucemia = new ArrayList<>();
        glucemia.add(new Fila("Glucosa en ayunas", "92", "mg/dL", "70 - 100 mg/dL",
                tramos("70 - 100 mg/dL", null), "Normal"));
        d.getEstudios().add(new Estudio(-2, "Glucemia", "Muestra tomada con 8 horas de ayuno.", glucemia));

        List<Fila> orina = new ArrayList<>();
        orina.add(new Fila("Aspecto", "Límpido", null, "Límpido", tramos("Límpido", null), null));
        orina.add(new Fila("Sedimento", "Escasas células epiteliales planas",
                null, "Sin elementos patológicos. Se informan sólo hallazgos significativos en el sedimento urinario",
                tramos("Sin elementos patológicos. ", "16A34A",
                        "Se informan sólo hallazgos significativos en el sedimento urinario", null), null));
        d.getEstudios().add(new Estudio(-3, "Orina completa", null, orina));

        List<Fila> exudado = new ArrayList<>();
        exudado.add(new Fila("Examen en fresco", "Regular células epiteliales\nEscasos leucocitos", null, null, null, null));
        exudado.add(new Fila("Examen micológico", "No se observan levaduras", null, null, null, null));
        d.getEstudios().add(new Estudio(-4, "Exudado vaginal", null, exudado));

        d.getEstudiosPendientes().add("Perfil tiroideo");
        return d;
    }

    private static List<TramoTexto> tramos(String... textoYColor) {
        List<TramoTexto> lista = new ArrayList<>();
        for (int i = 0; i + 1 < textoYColor.length; i += 2) {
            lista.add(new TramoTexto(textoYColor[i], textoYColor[i + 1]));
        }
        return lista;
    }
}
