package modelo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registro evolutivo de UN paciente: todas sus visitas al laboratorio (órdenes), la edad que
 * tenía en cada una, cuántos meses seguidos fue, y cómo fueron cambiando sus resultados. Se arma
 * con {@link dao.EvolucionPacienteDAO#cargar} y se muestra en Estadísticas &gt; "Evolución de un
 * paciente" (y se puede sacar en PDF).
 */
public class EvolucionPaciente {

    /** Una visita = una orden del paciente. */
    public static final class Visita {
        public final int idPedido;
        public final String numeroOrden;
        public final Date fecha;
        /** Edad en años cumplidos ese día, o -1 si no hay fecha de nacimiento. */
        public final int edad;
        public final String estudios;
        public final String medico;

        public Visita(int idPedido, String numeroOrden, Date fecha, int edad, String estudios, String medico) {
            this.idPedido = idPedido;
            this.numeroOrden = numeroOrden;
            this.fecha = fecha;
            this.edad = edad;
            this.estudios = estudios;
            this.medico = medico;
        }
    }

    /** Un resultado cargado de un parámetro (analito) en una visita. */
    public static final class Medicion {
        public final int idAnalito;
        public final String analito;
        public final String examen;
        public final String unidad;
        public final Date fecha;
        public final int edad;
        public final String valor;
        /** El valor como número, o null si es un texto ("Negativo", "Límpido"...). */
        public final Double numero;
        public final String referencia;
        public final String estado;

        public Medicion(int idAnalito, String analito, String examen, String unidad, Date fecha, int edad, String valor,
                String referencia, String estado) {
            this.idAnalito = idAnalito;
            this.analito = analito;
            this.examen = examen;
            this.unidad = unidad;
            this.fecha = fecha;
            this.edad = edad;
            this.valor = valor;
            this.numero = aNumero(valor);
            this.referencia = referencia;
            this.estado = estado;
        }
    }

    /** Lo que pasó con un mismo parámetro a lo largo del tiempo. */
    public static final class Serie {
        public final int idAnalito;
        public final String analito;
        public final String examen;
        public final String unidad;
        public final List<Medicion> mediciones = new ArrayList<>();

        Serie(Medicion primera) {
            this.idAnalito = primera.idAnalito;
            this.analito = primera.analito;
            this.examen = primera.examen;
            this.unidad = primera.unidad;
        }

        /** Mediciones con número (las que se pueden graficar). */
        public List<Medicion> numericas() {
            List<Medicion> lista = new ArrayList<>();
            for (Medicion m : mediciones) {
                if (m.numero != null) {
                    lista.add(m);
                }
            }
            return lista;
        }

        public boolean graficable() {
            return numericas().size() >= 2;
        }

        /** "Glucosa (Glucemia)" para mostrar en listas. */
        public String etiqueta() {
            String e = analito;
            if (examen != null && !examen.equalsIgnoreCase(analito)) {
                e += " (" + examen + ")";
            }
            return e;
        }

        @Override
        public String toString() {
            return etiqueta() + " - " + mediciones.size() + (mediciones.size() == 1 ? " vez" : " veces");
        }
    }

    /** Resumen de una edad: cuántas visitas tuvo con esa edad y qué estudios se hizo. */
    public static final class PorEdad {
        public final int edad;
        public int visitas;
        public final List<String> estudios = new ArrayList<>();

        PorEdad(int edad) {
            this.edad = edad;
        }
    }

    private int idPaciente;
    private String nombre;
    private String dni;
    private Date fechaNacimiento;
    private String sexo;
    private final List<Visita> visitas = new ArrayList<>();
    private final List<Medicion> mediciones = new ArrayList<>();

    public int getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    /** Visitas de la más vieja a la más nueva. */
    public List<Visita> getVisitas() {
        return visitas;
    }

    public List<Medicion> getMediciones() {
        return mediciones;
    }

    // ------------------------------------------------------------------ cálculos

    public int edadActual() {
        return edadEn(fechaNacimiento, new Date());
    }

    public Date primeraVisita() {
        return visitas.isEmpty() ? null : visitas.get(0).fecha;
    }

    public Date ultimaVisita() {
        return visitas.isEmpty() ? null : visitas.get(visitas.size() - 1).fecha;
    }

    /**
     * Visitas por mes, desde el mes de la primera visita hasta el de la última (los meses sin
     * visitas aparecen con 0, así se ve bien si fue seguido o no). Clave "2026-09".
     */
    public LinkedHashMap<String, Integer> visitasPorMes() {
        LinkedHashMap<String, Integer> mapa = new LinkedHashMap<>();
        if (visitas.isEmpty()) {
            return mapa;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(primeraVisita());
        c.set(Calendar.DAY_OF_MONTH, 1);
        Calendar fin = Calendar.getInstance();
        fin.setTime(ultimaVisita());
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM");
        String ultimoMes = f.format(fin.getTime());
        while (true) {
            String mes = f.format(c.getTime());
            mapa.put(mes, 0);
            if (mes.equals(ultimoMes) || mapa.size() > 600) {
                break;
            }
            c.add(Calendar.MONTH, 1);
        }
        for (Visita v : visitas) {
            String mes = f.format(v.fecha);
            mapa.put(mes, mapa.getOrDefault(mes, 0) + 1);
        }
        return mapa;
    }

    /** Cuántos meses distintos tuvo al menos una visita. */
    public int mesesConVisitas() {
        int n = 0;
        for (int v : visitasPorMes().values()) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    /** La mayor cantidad de meses SEGUIDOS en los que vino (al menos una vez por mes). */
    public int rachaMayorDeMeses() {
        int mayor = 0;
        int actual = 0;
        for (int v : visitasPorMes().values()) {
            actual = v > 0 ? actual + 1 : 0;
            mayor = Math.max(mayor, actual);
        }
        return mayor;
    }

    /** Meses seguidos que lleva viniendo hasta su última visita. */
    public int rachaActualDeMeses() {
        List<Integer> valores = new ArrayList<>(visitasPorMes().values());
        int n = 0;
        for (int i = valores.size() - 1; i >= 0 && valores.get(i) > 0; i--) {
            n++;
        }
        return n;
    }

    /** Resumen por cada edad que tuvo en sus visitas (de menor a mayor). */
    public List<PorEdad> porEdad() {
        TreeMap<Integer, PorEdad> mapa = new TreeMap<>();
        for (Visita v : visitas) {
            PorEdad pe = mapa.get(v.edad);
            if (pe == null) {
                pe = new PorEdad(v.edad);
                mapa.put(v.edad, pe);
            }
            pe.visitas++;
            if (v.estudios != null) {
                for (String e : v.estudios.split(",\\s*")) {
                    if (!e.trim().isEmpty() && !pe.estudios.contains(e.trim())) {
                        pe.estudios.add(e.trim());
                    }
                }
            }
        }
        return new ArrayList<>(mapa.values());
    }

    /** Mediciones agrupadas por parámetro, en el orden en que aparecen por primera vez. */
    public List<Serie> series() {
        Map<String, Serie> mapa = new LinkedHashMap<>();
        for (Medicion m : mediciones) {
            String clave = m.idAnalito + "";
            Serie s = mapa.get(clave);
            if (s == null) {
                s = new Serie(m);
                mapa.put(clave, s);
            }
            s.mediciones.add(m);
        }
        return new ArrayList<>(mapa.values());
    }

    // ------------------------------------------------------------------ utilidades

    /** Años cumplidos a una fecha, o -1 si no se sabe la fecha de nacimiento. */
    public static int edadEn(Date nacimiento, Date fecha) {
        if (nacimiento == null || fecha == null) {
            return -1;
        }
        Calendar n = Calendar.getInstance();
        n.setTime(nacimiento);
        Calendar f = Calendar.getInstance();
        f.setTime(fecha);
        int edad = f.get(Calendar.YEAR) - n.get(Calendar.YEAR);
        if (f.get(Calendar.MONTH) < n.get(Calendar.MONTH)
                || (f.get(Calendar.MONTH) == n.get(Calendar.MONTH) && f.get(Calendar.DAY_OF_MONTH) < n.get(Calendar.DAY_OF_MONTH))) {
            edad--;
        }
        return Math.max(edad, 0);
    }

    public static String textoEdad(int edad) {
        return edad < 0 ? "-" : edad + (edad == 1 ? " año" : " años");
    }

    /** "2026-09" -> "Sep 2026". */
    public static String nombreMes(String clave) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM").parse(clave);
            String t = new SimpleDateFormat("MMM yyyy", new Locale("es", "AR")).format(d).replace(".", "");
            return Character.toUpperCase(t.charAt(0)) + t.substring(1);
        } catch (java.text.ParseException e) {
            return clave;
        }
    }

    /** "12,5" / "12.5" / "4500" -> número; "Negativo" -> null. Acepta "> 10" o "<0.5" tomando el número. */
    static Double aNumero(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim().replace(" ", "");
        if (t.startsWith(">") || t.startsWith("<")) {
            t = t.substring(1).replace("=", "");
        }
        if (t.matches("-?\\d{1,3}(\\.\\d{3})+(,\\d+)?")) {
            t = t.replace(".", "").replace(",", "."); // 4.500 o 4.500,5 (miles con punto)
        } else {
            t = t.replace(",", ".");
        }
        if (!t.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
