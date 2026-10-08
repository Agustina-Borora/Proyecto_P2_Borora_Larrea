package reportes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Las posiciones y medidas del informe que la bioquímica puede cambiar sin tocar código: tamaño
 * de letra, cuánto ocupa cada columna de la tabla, márgenes de la hoja, de qué lado va el logo
 * y la firma, etc.
 *
 * <p>Hay un diseño "predeterminado" para todas las órdenes (Configuración &gt; Informes PDF) y,
 * si hace falta, cada orden puede tener el suyo propio (desde la vista previa de esa orden). Se
 * guarda como un texto corto del estilo {@code "letra=10;colResultado=20;..."} -- ver
 * {@link #aTexto()} y {@link #desdeTexto(String)}.</p>
 */
public class DisenoInforme {

    /** Cómo se arma el informe en general. */
    public enum Estilo {
        CLASICO("Clásico"), MODERNO("Moderno");

        public final String etiqueta;

        Estilo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    public enum Hoja {
        A4("A4", DocumentoMaquetado.ANCHO_HOJA, DocumentoMaquetado.ALTO_HOJA),
        CARTA("Carta", DocumentoMaquetado.ANCHO_CARTA, DocumentoMaquetado.ALTO_CARTA);

        public final String etiqueta;
        public final double ancho;
        public final double alto;

        Hoja(String etiqueta, double ancho, double alto) {
            this.etiqueta = etiqueta;
            this.ancho = ancho;
            this.alto = alto;
        }
    }

    public enum Espaciado {
        COMPACTO("Compacto", 2.0), NORMAL("Normal", 4.0), AMPLIO("Amplio", 7.0);

        public final String etiqueta;
        /** Espacio libre arriba y abajo de cada fila, en puntos. */
        public final double relleno;

        Espaciado(String etiqueta, double relleno) {
            this.etiqueta = etiqueta;
            this.relleno = relleno;
        }
    }

    public enum Alineacion {
        IZQUIERDA("Izquierda"), CENTRO("Centro"), DERECHA("Derecha");

        public final String etiqueta;

        Alineacion(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    public enum EstiloTabla {
        LINEAS("Con líneas"), CEBRA("Filas alternadas"), SIMPLE("Sin líneas");

        public final String etiqueta;

        EstiloTabla(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    public enum ColorAcento {
        VERDE("Verde", 0x1E5C3D, 0xE8F1EC), AZUL("Azul", 0x1E40AF, 0xE8EEFB), GRIS("Gris", 0x374151, 0xEEF0F2),
        NEGRO("Negro", 0x111111, 0xEDEDED);

        public final String etiqueta;
        public final int fuerte;
        public final int suave;

        ColorAcento(String etiqueta, int fuerte, int suave) {
            this.etiqueta = etiqueta;
            this.fuerte = fuerte;
            this.suave = suave;
        }
    }

    // Límites que se ofrecen en pantalla (y que se respetan aunque el texto guardado diga otra cosa).
    public static final double LETRA_MIN = 8;
    public static final double LETRA_MAX = 12;
    public static final int COLUMNA_MIN = 10;
    public static final int ESTADO_MIN = 8;

    private Estilo estilo = Estilo.CLASICO;
    private Hoja hoja = Hoja.A4;
    private int columnas = 2;
    private double tamanoLetra = 10;
    private Espaciado espaciado = Espaciado.NORMAL;
    private int colDeterminacion = 34;
    private int colResultado = 20;
    private int colReferencia = 32;
    private double margenSuperiorMm = 14;
    private double margenInferiorMm = 12;
    private double margenLateralMm = 15;
    private boolean mostrarEncabezado = true;
    private Alineacion alineacionEncabezado = Alineacion.IZQUIERDA;
    private Alineacion posicionLogo = Alineacion.IZQUIERDA;
    private Alineacion posicionFirma = Alineacion.DERECHA;
    private EstiloTabla estiloTabla = EstiloTabla.LINEAS;
    private ColorAcento colorAcento = ColorAcento.VERDE;
    private boolean estudioEnHojaNueva = false;
    private boolean mostrarFirma = true;
    private boolean repetirDatos = true;
    private boolean firmaAlPie = false;

    public DisenoInforme() {
    }

    /** Diseño de fábrica (el que se usa si nunca se cambió nada). */
    public static DisenoInforme deFabrica() {
        return new DisenoInforme();
    }

    public DisenoInforme copia() {
        return desdeTexto(aTexto());
    }

    // ------------------------------------------------------------------ texto <-> diseño

    public String aTexto() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("estilo", estilo.name());
        m.put("hoja", hoja.name());
        m.put("columnas", String.valueOf(columnas));
        m.put("letra", String.format(Locale.ROOT, "%.1f", tamanoLetra));
        m.put("espaciado", espaciado.name());
        m.put("colDeterminacion", String.valueOf(colDeterminacion));
        m.put("colResultado", String.valueOf(colResultado));
        m.put("colReferencia", String.valueOf(colReferencia));
        m.put("margenSup", String.format(Locale.ROOT, "%.1f", margenSuperiorMm));
        m.put("margenInf", String.format(Locale.ROOT, "%.1f", margenInferiorMm));
        m.put("margenLat", String.format(Locale.ROOT, "%.1f", margenLateralMm));
        m.put("encabezado", String.valueOf(mostrarEncabezado));
        m.put("alinEncabezado", alineacionEncabezado.name());
        m.put("logo", posicionLogo.name());
        m.put("firma", posicionFirma.name());
        m.put("tabla", estiloTabla.name());
        m.put("color", colorAcento.name());
        m.put("hojaPorEstudio", String.valueOf(estudioEnHojaNueva));
        m.put("firmaVisible", String.valueOf(mostrarFirma));
        m.put("repetirDatos", String.valueOf(repetirDatos));
        m.put("firmaAlPie", String.valueOf(firmaAlPie));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Lee un diseño guardado. Cualquier dato que falte o no se entienda queda en su valor de
     * fábrica -- nunca falla.
     */
    public static DisenoInforme desdeTexto(String texto) {
        DisenoInforme d = new DisenoInforme();
        if (texto == null || texto.trim().isEmpty()) {
            return d;
        }
        for (String par : texto.split(";")) {
            int igual = par.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            String clave = par.substring(0, igual).trim();
            String valor = par.substring(igual + 1).trim();
            try {
                switch (clave) {
                    case "estilo": d.estilo = Estilo.valueOf(valor); break;
                    case "hoja": d.hoja = Hoja.valueOf(valor); break;
                    case "columnas": d.setColumnasClasico(Integer.parseInt(valor)); break;
                    case "letra": d.setTamanoLetra(Double.parseDouble(valor)); break;
                    case "espaciado": d.espaciado = Espaciado.valueOf(valor); break;
                    case "colDeterminacion": d.colDeterminacion = Integer.parseInt(valor); break;
                    case "colResultado": d.colResultado = Integer.parseInt(valor); break;
                    case "colReferencia": d.colReferencia = Integer.parseInt(valor); break;
                    case "margenSup": d.setMargenSuperiorMm(Double.parseDouble(valor)); break;
                    case "margenInf": d.setMargenInferiorMm(Double.parseDouble(valor)); break;
                    case "margenLat": d.setMargenLateralMm(Double.parseDouble(valor)); break;
                    case "encabezado": d.mostrarEncabezado = Boolean.parseBoolean(valor); break;
                    case "alinEncabezado": d.alineacionEncabezado = Alineacion.valueOf(valor); break;
                    case "logo": d.posicionLogo = Alineacion.valueOf(valor); break;
                    case "firma": d.posicionFirma = Alineacion.valueOf(valor); break;
                    case "tabla": d.estiloTabla = EstiloTabla.valueOf(valor); break;
                    case "color": d.colorAcento = ColorAcento.valueOf(valor); break;
                    case "hojaPorEstudio": d.estudioEnHojaNueva = Boolean.parseBoolean(valor); break;
                    case "firmaVisible": d.mostrarFirma = Boolean.parseBoolean(valor); break;
                    case "repetirDatos": d.repetirDatos = Boolean.parseBoolean(valor); break;
                    case "firmaAlPie": d.firmaAlPie = Boolean.parseBoolean(valor); break;
                    default: break;
                }
            } catch (RuntimeException e) {
                // Valor raro: se deja el de fábrica para ese dato.
            }
        }
        d.ajustarColumnas();
        return d;
    }

    /** Deja las columnas dentro de lo posible (cada una con un mínimo y "Estado" con lo que sobra). */
    private void ajustarColumnas() {
        colDeterminacion = limitar(colDeterminacion, COLUMNA_MIN, 70);
        colResultado = limitar(colResultado, COLUMNA_MIN, 60);
        colReferencia = limitar(colReferencia, COLUMNA_MIN, 70);
        int sobra = 100 - ESTADO_MIN - colDeterminacion - colResultado - colReferencia;
        if (sobra < 0) {
            // Se le saca primero a la columna más ancha.
            while (sobra < 0) {
                if (colReferencia >= colDeterminacion && colReferencia > COLUMNA_MIN) {
                    colReferencia--;
                } else if (colDeterminacion > COLUMNA_MIN) {
                    colDeterminacion--;
                } else if (colResultado > COLUMNA_MIN) {
                    colResultado--;
                } else {
                    break;
                }
                sobra++;
            }
        }
    }

    private static int limitar(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    private static double limitar(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }

    // ------------------------------------------------------------------ getters / setters

    /** "Clásico": como el informe que arma el laboratorio en Word (texto en columnas, "VR:"). "Moderno": en tabla. */
    public Estilo getEstilo() {
        return estilo;
    }

    public void setEstilo(Estilo estilo) {
        this.estilo = estilo == null ? Estilo.CLASICO : estilo;
    }

    public Hoja getHoja() {
        return hoja;
    }

    public void setHoja(Hoja hoja) {
        this.hoja = hoja == null ? Hoja.A4 : hoja;
    }

    /** En cuántas columnas se reparten los estudios en el estilo Clásico (1 o 2). */
    public int getColumnasClasico() {
        return columnas;
    }

    public void setColumnasClasico(int columnas) {
        this.columnas = columnas <= 1 ? 1 : 2;
    }

    public double getTamanoLetra() {
        return tamanoLetra;
    }

    public void setTamanoLetra(double tamanoLetra) {
        this.tamanoLetra = limitar(Math.round(tamanoLetra * 2) / 2.0, LETRA_MIN, LETRA_MAX);
    }

    public Espaciado getEspaciado() {
        return espaciado;
    }

    public void setEspaciado(Espaciado espaciado) {
        this.espaciado = espaciado == null ? Espaciado.NORMAL : espaciado;
    }

    public int getColDeterminacion() {
        return colDeterminacion;
    }

    public int getColResultado() {
        return colResultado;
    }

    public int getColReferencia() {
        return colReferencia;
    }

    /** Lo que queda para "Estado" (100% menos las otras tres). */
    public int getColEstado() {
        return 100 - colDeterminacion - colResultado - colReferencia;
    }

    /** Cambia los anchos de columna (en %); "Estado" se queda con lo que sobra. */
    public void setColumnas(int determinacion, int resultado, int referencia) {
        this.colDeterminacion = determinacion;
        this.colResultado = resultado;
        this.colReferencia = referencia;
        ajustarColumnas();
    }

    public double getMargenSuperiorMm() {
        return margenSuperiorMm;
    }

    public void setMargenSuperiorMm(double mm) {
        this.margenSuperiorMm = limitar(mm, 6, 80);
    }

    public double getMargenInferiorMm() {
        return margenInferiorMm;
    }

    public void setMargenInferiorMm(double mm) {
        this.margenInferiorMm = limitar(mm, 6, 50);
    }

    public double getMargenLateralMm() {
        return margenLateralMm;
    }

    public void setMargenLateralMm(double mm) {
        this.margenLateralMm = limitar(mm, 8, 30);
    }

    public boolean isMostrarEncabezado() {
        return mostrarEncabezado;
    }

    public void setMostrarEncabezado(boolean mostrarEncabezado) {
        this.mostrarEncabezado = mostrarEncabezado;
    }

    public Alineacion getAlineacionEncabezado() {
        return alineacionEncabezado;
    }

    public void setAlineacionEncabezado(Alineacion alineacion) {
        this.alineacionEncabezado = alineacion == Alineacion.CENTRO ? Alineacion.CENTRO : Alineacion.IZQUIERDA;
    }

    public Alineacion getPosicionLogo() {
        return posicionLogo;
    }

    /** Izquierda, Derecha, o Centro (arriba del nombre, todo centrado). */
    public void setPosicionLogo(Alineacion posicion) {
        this.posicionLogo = posicion == null ? Alineacion.IZQUIERDA : posicion;
    }

    /**
     * Repetir el encabezado del laboratorio y los datos del paciente en cada hoja (si el informe
     * ocupa más de una). Así, si se imprimen y se separan las hojas, cada una sigue diciendo de
     * quién es.
     */
    public boolean isRepetirDatos() {
        return repetirDatos;
    }

    public void setRepetirDatos(boolean repetirDatos) {
        this.repetirDatos = repetirDatos;
    }

    public Alineacion getPosicionFirma() {
        return posicionFirma;
    }

    public void setPosicionFirma(Alineacion posicion) {
        this.posicionFirma = posicion == null ? Alineacion.DERECHA : posicion;
    }

    public EstiloTabla getEstiloTabla() {
        return estiloTabla;
    }

    public void setEstiloTabla(EstiloTabla estiloTabla) {
        this.estiloTabla = estiloTabla == null ? EstiloTabla.LINEAS : estiloTabla;
    }

    public ColorAcento getColorAcento() {
        return colorAcento;
    }

    public void setColorAcento(ColorAcento colorAcento) {
        this.colorAcento = colorAcento == null ? ColorAcento.VERDE : colorAcento;
    }

    /** Línea de firma de la bioquímica al pie de la última hoja (se puede sacar si firman/sellan a mano). */
    public boolean isMostrarFirma() {
        return mostrarFirma;
    }

    public void setMostrarFirma(boolean mostrarFirma) {
        this.mostrarFirma = mostrarFirma;
    }

    /**
     * false (de fábrica): la firma va justo debajo del último resultado, así si sobra hoja se
     * puede cortar sin perder la firma. true: la firma va abajo de todo, al pie de la hoja.
     */
    public boolean isFirmaAlPie() {
        return firmaAlPie;
    }

    public void setFirmaAlPie(boolean firmaAlPie) {
        this.firmaAlPie = firmaAlPie;
    }

    public boolean isEstudioEnHojaNueva() {
        return estudioEnHojaNueva;
    }

    public void setEstudioEnHojaNueva(boolean estudioEnHojaNueva) {
        this.estudioEnHojaNueva = estudioEnHojaNueva;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof DisenoInforme && aTexto().equals(((DisenoInforme) otro).aTexto());
    }

    @Override
    public int hashCode() {
        return aTexto().hashCode();
    }
}
