package registrarResultados;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Un renglón de resultado dentro de la lista de parámetros de un examen
 * (ver cargarReultados): nombre del parámetro, campo para cargar el valor
 * (según tipo_dato: numérico, cualitativo o texto libre), unidad y valor
 * de referencia. Una instancia por cada modelo.Parametro del analisis_tipo
 * seleccionado.
 */
public class ContenedorExamenes extends javax.swing.JPanel {

    private static final int ALTO_FILA = 38;

    /** Acepta enteros o decimales (con punto o coma), con signo opcional. */
    private static final java.util.regex.Pattern PATRON_NUMERICO =
            java.util.regex.Pattern.compile("-?\\d+([.,]\\d+)?");

    /** Caracteres que se dejan escribir en un campo numérico (letras y símbolos raros ni entran). */
    private static final java.util.regex.Pattern CARACTERES_PERMITIDOS =
            java.util.regex.Pattern.compile("[0-9.,-]*");

    private static final Color COLOR_INVALIDO = new Color(217, 83, 79);
    private static final Color FONDO_INVALIDO = new Color(253, 237, 237);

    private int idParametro;
    private String tipoDato;
    private boolean valorInvalido = false;
    private Border bordeOriginalCampo;
    private Color fondoOriginalCampo;

    /** El campo de carga actual (JTextField, JComboBox o JTextArea, según tipoDato). */
    private javax.swing.JComponent campoActual;

    /**
     * Arma el panel (initComponents) y deja listo el renglón vacío: layout
     * del panelCampo donde después se inserta el campo de carga, y los
     * anchos/paddings de las columnas (ajustarProporciones). El contenido
     * real (nombre, campo, unidad, referencia) se completa después con
     * {@link #configurarParametro(modelo.Parametro, String)}.
     */
    public ContenedorExamenes() {
        initComponents();
        configurarPanelCampo();
        ajustarProporciones();
    }

    private void configurarPanelCampo() {
        panelCampo.setLayout(new BorderLayout());
    }

    /**
     * Fija el ancho de cada columna (nombre / campo / unidad / referencia)
     * para que todos los renglones queden alineados, como en una tabla, y
     * les da un poco de aire (padding) para que no se vea amontonado.
     */
    private void ajustarProporciones() {
        setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        lblNombre.setPreferredSize(new Dimension(300, ALTO_FILA));
        lblNombre.setMaximumSize(new Dimension(300, ALTO_FILA));
        lblNombre.setBorder(new EmptyBorder(0, 0, 0, 10));

        panelCampo.setPreferredSize(new Dimension(150, ALTO_FILA - 6));
        panelCampo.setMaximumSize(new Dimension(150, ALTO_FILA - 6));
        panelCampo.setBorder(new EmptyBorder(0, 0, 0, 14));

        lblUnidad.setPreferredSize(new Dimension(100, ALTO_FILA));
        lblUnidad.setMaximumSize(new Dimension(100, ALTO_FILA));
        lblUnidad.setForeground(new Color(110, 110, 110));

        lblReferencia.setPreferredSize(new Dimension(300, ALTO_FILA));
        lblReferencia.setMaximumSize(new Dimension(Short.MAX_VALUE, ALTO_FILA));
        lblReferencia.setForeground(new Color(90, 90, 90));
    }

    /**
     * Arma este renglón a partir de un parámetro real: nombre, unidad,
     * referencia y el campo de carga que corresponda según su tipo. Si
     * valorActual no es null (el examen ya tenía algo cargado), lo
     * precarga en el campo.
     */
    public void configurarParametro(modelo.Parametro parametro, String valorActual) {
        this.idParametro = parametro.getIdParametro();
        this.tipoDato = parametro.getTipoDato();

        lblNombre.setText(parametro.getNombreParametro());
        lblNombre.setToolTipText(parametro.getNombreParametro());
        lblUnidad.setText(parametro.getUnidad() != null ? parametro.getUnidad() : "");
        String referencia = parametro.getValorReferencia() != null ? parametro.getValorReferencia() : "";
        lblReferencia.setText(referencia);
        lblReferencia.setToolTipText(referencia.isEmpty() ? null : referencia);

        configurarCampo(tipoDato, parametro.getOpcionesCualitativo());
        setearValor(valorActual);
    }

    /** Alterna blanco / gris muy claro entre renglones, como en una tabla. */
    public void marcarFilaPar(boolean par) {
        Color fondo = par ? Color.WHITE : new Color(248, 250, 249);
        setBackground(fondo);
        panelCampo.setBackground(fondo);
    }

    public int getIdParametro() {
        return idParametro;
    }

    public String getNombreParametro() {
        return lblNombre.getText();
    }

    /**
     * Para parámetros numéricos: false si el usuario cargó algo que no es
     * un número válido (el renglón ya se pinta de rojo solo, esto es lo
     * que usa cargarReultados para frenar el guardado). Cualitativo y
     * texto libre siempre son válidos.
     */
    public boolean esValido() {
        return !valorInvalido;
    }

    /** Devuelve lo que el usuario cargó en este renglón (o cadena vacía si no cargó nada). */
    public String obtenerValor() {
        if (campoActual instanceof JTextField) {
            String texto = ((JTextField) campoActual).getText().trim();
            if ("numerico".equals(tipoDato)) {
                texto = texto.replace(',', '.');
            }
            return texto;
        }
        if (campoActual instanceof JTextArea) {
            return ((JTextArea) campoActual).getText().trim();
        }
        if (campoActual instanceof JComboBox) {
            Object seleccion = ((JComboBox<?>) campoActual).getSelectedItem();
            if (seleccion == null || seleccion.toString().startsWith("Seleccione")) {
                return "";
            }
            return seleccion.toString();
        }
        return "";
    }

    private void setearValor(String valor) {
        if (valor == null || valor.isEmpty()) {
            return;
        }
        if (campoActual instanceof JTextField) {
            ((JTextField) campoActual).setText(valor);
        } else if (campoActual instanceof JTextArea) {
            ((JTextArea) campoActual).setText(valor);
        } else if (campoActual instanceof JComboBox) {
            ((JComboBox<?>) campoActual).setSelectedItem(valor);
        }
    }

    // --- MÉTODOS PARA CAMBIAR EL CAMPO DINÁMICAMENTE ---
    /**
     * Vacía panelCampo y le pone el componente de carga que corresponde al
     * tipo_dato del parámetro: "cualitativo" arma un JComboBox con
     * opcionesCualitativo (separadas por coma) más un "Seleccione..."
     * inicial; "texto" arma un JTextArea con scroll para texto libre;
     * cualquier otro valor (incluido null) cae en "numerico", un JTextField
     * con validación en vivo (ver aplicarValidacionNumerica). Guarda el
     * componente elegido en campoActual, que es lo que después leen
     * obtenerValor/setearValor.
     */
    private void configurarCampo(String tipo, String opcionesCualitativo) {

        panelCampo.removeAll();
        campoActual = null;

        if (tipo == null) {
            tipo = "numerico";
        }

        switch (tipo) {

            case "cualitativo": {
                JComboBox<String> combo = new JComboBox<>();
                combo.addItem("Seleccione...");
                if (opcionesCualitativo != null && !opcionesCualitativo.isEmpty()) {
                    for (String opcion : opcionesCualitativo.split(",")) {
                        combo.addItem(opcion.trim());
                    }
                }
                panelCampo.add(combo, BorderLayout.CENTER);
                campoActual = combo;
                break;
            }

            case "texto": {
                JTextArea txtArea = new JTextArea();
                txtArea.setLineWrap(true);
                txtArea.setWrapStyleWord(true);
                JScrollPane scroll = new JScrollPane(txtArea);
                panelCampo.add(scroll, BorderLayout.CENTER);
                campoActual = txtArea;
                break;
            }

            case "numerico":
            default: {
                JTextField txtNumerico = new JTextField();
                bordeOriginalCampo = txtNumerico.getBorder();
                fondoOriginalCampo = txtNumerico.getBackground();
                aplicarValidacionNumerica(txtNumerico);
                panelCampo.add(txtNumerico, BorderLayout.CENTER);
                campoActual = txtNumerico;
                break;
            }
        }

        valorInvalido = false;
        panelCampo.revalidate();
        panelCampo.repaint();
    }

    /**
     * Valida en vivo lo que se va tecleando (o se pega) en un campo
     * numérico, en dos niveles:
     * <p>
     * 1) directamente no deja entrar letras ni símbolos que no pintan nada
     * en un número (solo dígitos, coma, punto y signo) -- un "@" ni se
     * escribe;
     * <p>
     * 2) lo que sí entra pero no forma un número válido todavía (un "-"
     * solo, dos puntos, etc.) se sigue marcando en rojo con
     * esValido() = false, hasta que se corrija o se borre. Vacío siempre
     * cuenta como válido (el parámetro simplemente no se guarda).
     */
    private void aplicarValidacionNumerica(JTextField campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && CARACTERES_PERMITIDOS.matcher(string).matches()) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null || CARACTERES_PERMITIDOS.matcher(text).matches()) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                revisar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                revisar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                revisar();
            }

            private void revisar() {
                String texto = campo.getText().trim();
                boolean ok = texto.isEmpty() || PATRON_NUMERICO.matcher(texto).matches();
                valorInvalido = !ok;
                campo.setBorder(ok ? bordeOriginalCampo : new CompoundBorder(
                        new LineBorder(COLOR_INVALIDO, 1), new EmptyBorder(1, 2, 1, 2)));
                campo.setBackground(ok ? fondoOriginalCampo : FONDO_INVALIDO);
            }
        });
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblNombre = new javax.swing.JLabel();
        panelCampo = new javax.swing.JPanel();
        lblUnidad = new javax.swing.JLabel();
        lblReferencia = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        lblNombre.setText("Nombre");
        add(lblNombre);

        javax.swing.GroupLayout panelCampoLayout = new javax.swing.GroupLayout(panelCampo);
        panelCampo.setLayout(panelCampoLayout);
        panelCampoLayout.setHorizontalGroup(
            panelCampoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 150, Short.MAX_VALUE)
        );
        panelCampoLayout.setVerticalGroup(
            panelCampoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 34, Short.MAX_VALUE)
        );

        add(panelCampo);

        lblUnidad.setText("Unidad");
        add(lblUnidad);

        lblReferencia.setText("Referencia");
        add(lblReferencia);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblReferencia;
    private javax.swing.JLabel lblUnidad;
    private javax.swing.JPanel panelCampo;
    // End of variables declaration//GEN-END:variables
}
