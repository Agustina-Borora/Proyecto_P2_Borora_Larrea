package vistas.registrarResultados;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla de carga de resultados de un examen puntual.
 */
public class cargarReultados extends javax.swing.JPanel {

    private static final int ALTO_FILA = 38;
    private static final int ALTO_ENCABEZADO = 78;
    private static final int ALTO_MINIMO_CENTRO = ALTO_FILA + 6;
    private static final int ALTO_MAXIMO_CENTRO = 340;

    /**
     * Se dispara cuando el usuario confirma "Cancelar".
     */
    public interface CancelarListener {
        void onCancelar();
    }

    /**
     * Se dispara después de guardar los resultados con éxito.
     */
    public interface GuardarListener {
        void onGuardado();
    }

    private final java.util.List<CancelarListener> listenersCancelar = new java.util.ArrayList<>();
    private final java.util.List<GuardarListener> listenersGuardar = new java.util.ArrayList<>();

    public void addCancelarListener(CancelarListener listener) {
        listenersCancelar.add(listener);
    }

    public void addGuardarListener(GuardarListener listener) {
        listenersGuardar.add(listener);
    }

    private int idPedidoAnalisis;
    private final List<ContenedorExamenes> filasParametros = new ArrayList<>();

    /**
     * Arma el panel (initComponents) y conecta los dos botones de acción: "Cancelar" pide
     * confirmación y, si se acepta, avisa a los CancelarListener sin guardar nada; "Guardar"
     * dispara {@link #guardarResultados()}.
     */
    public cargarReultados() {
        initComponents();

        jPanel1.setLayout(new BorderLayout());

        jButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int respuesta = javax.swing.JOptionPane.showConfirmDialog(cargarReultados.this,
                        "¿Cancelar la carga de resultados y volver al listado?",
                        "Cancelar", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
                if (respuesta == javax.swing.JOptionPane.YES_OPTION) {
                    for (CancelarListener listener : listenersCancelar) {
                        listener.onCancelar();
                    }
                }
            }
        });

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guardarResultados();
            }
        });
    }

    /**
     * Llena el encabezado con los datos del paciente dueño de la orden elegida en Registrar
     * Resultados.
     */
    public void cargarDatosPaciente(modelo.Paciente paciente, java.util.Date fechaAnalisis) {
        encabezadoDatosPaciente2.cargarDatosPaciente(paciente, fechaAnalisis);
    }

    /**
     * Arma la lista de parámetros del examen elegido: busca en analitos (+ valores_referencia) los
     * renglones que le corresponden a este analisis_tipo (filtrados por el sexo del paciente) y a
     * cada uno le arma un ContenedorExamenes, precargando el valor si ya se había guardado algo
     * antes.
     */
    public void cargarExamen(int idPedidoAnalisis, int idAnalisisTipo, String nombreExamen, int idSexoPaciente) {
        this.idPedidoAnalisis = idPedidoAnalisis;
        filasParametros.clear();

        controlador.ResultadosController.DatosExamen datos = controlador.ResultadosController.cargarExamen(
                this, idAnalisisTipo, idPedidoAnalisis, idSexoPaciente);

        armarListaParametros(nombreExamen, datos.getParametros(), datos.getValoresGuardados());
    }

    /**
     * Reconstruye el contenido de jPanel1 a partir de la lista de parámetros del examen: título
     * del examen, encabezado de columnas y un ContenedorExamenes por parámetro (con su valor ya
     * guardado, si lo había), guardados en filasParametros para poder leerlos después al guardar.
     */
    private void armarListaParametros(String nombreExamen, List<modelo.Parametro> parametros,
            Map<Integer, String> valoresGuardados) {

        jPanel1.removeAll();

        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(Color.WHITE);

        JLabel titulo = new JLabel(nombreExamen != null ? nombreExamen : "Examen");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setForeground(new Color(40, 40, 40));
        titulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        encabezado.add(titulo);

        if (parametros.isEmpty()) {
            JLabel sinParametros = new JLabel(
                    "Este examen todavía no tiene parámetros cargados en el sistema.");
            sinParametros.setForeground(new Color(140, 140, 140));
            encabezado.add(sinParametros);
            jPanel1.add(encabezado, BorderLayout.NORTH);
            jPanel1.setPreferredSize(new Dimension(992, ALTO_ENCABEZADO + 20));
            jPanel1.revalidate();
            jPanel1.repaint();
            return;
        }

        encabezado.add(crearFilaEncabezadoColumnas());
        jPanel1.add(encabezado, BorderLayout.NORTH);

        JPanel listaFilas = new JPanel();
        listaFilas.setLayout(new BoxLayout(listaFilas, BoxLayout.Y_AXIS));
        listaFilas.setBackground(Color.WHITE);

        boolean par = true;
        for (modelo.Parametro parametro : parametros) {
            ContenedorExamenes fila = new ContenedorExamenes();
            fila.configurarParametro(parametro, valoresGuardados.get(parametro.getIdParametro()));
            fila.marcarFilaPar(par);
            par = !par;
            filasParametros.add(fila);
            listaFilas.add(fila);
        }

        JScrollPane scroll = new JScrollPane(listaFilas);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA);
        jPanel1.add(scroll, BorderLayout.CENTER);

        int altoFilas = parametros.size() * (ALTO_FILA + 2);
        int altoCentro = Math.max(ALTO_MINIMO_CENTRO, Math.min(altoFilas, ALTO_MAXIMO_CENTRO));
        jPanel1.setPreferredSize(new Dimension(992, ALTO_ENCABEZADO + altoCentro));

        jPanel1.revalidate();
        jPanel1.repaint();
    }

    private JPanel crearFilaEncabezadoColumnas() {
        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.LINE_AXIS));
        fila.setBackground(new Color(246, 248, 247));
        fila.setBorder(new CompoundBorder(
                new javax.swing.border.MatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
                new EmptyBorder(6, 0, 6, 0)));

        fila.add(crearEtiquetaColumna("Parámetro", 300, false));
        fila.add(crearEtiquetaColumna("Resultado", 150, false));
        fila.add(crearEtiquetaColumna("Unidad", 100, false));
        fila.add(crearEtiquetaColumna("Valor de Referencia", 300, true));

        return fila;
    }

    private JLabel crearEtiquetaColumna(String texto, int ancho, boolean expandible) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 12f));
        etiqueta.setPreferredSize(new Dimension(ancho, 24));
        etiqueta.setMaximumSize(new Dimension(expandible ? Short.MAX_VALUE : ancho, 24));
        return etiqueta;
    }

    /**
     * Junta el valor cargado en cada renglón y lo guarda.
     */
    private void guardarResultados() {
        if (filasParametros.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No hay parámetros para guardar en este examen.",
                    "Nada para guardar", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Antes de guardar nada: si algún numérico quedó con algo que no es
        // un número (ya se ve en rojo en la fila), se frena todo el guardado
        // para no meter datos inválidos en la base.
        List<String> invalidos = new ArrayList<>();
        for (ContenedorExamenes fila : filasParametros) {
            if (!fila.esValido()) {
                invalidos.add(fila.getNombreParametro());
            }
        }
        if (!invalidos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Revisá estos valores, no son números válidos:\n· " + String.join("\n· ", invalidos),
                    "Valores inválidos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<Integer, String> valores = new HashMap<>();
        boolean faltaAlguno = false;
        for (ContenedorExamenes fila : filasParametros) {
            String valor = fila.obtenerValor();
            if (valor.isEmpty()) {
                faltaAlguno = true;
                continue;
            }
            valores.put(fila.getIdParametro(), valor);
        }

        if (valores.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Cargá al menos un valor antes de guardar.",
                    "Nada para guardar", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = controlador.ResultadosController.guardarResultados(this, idPedidoAnalisis, valores, faltaAlguno);

        if (!ok) {
            return;
        }

        javax.swing.JOptionPane.showMessageDialog(this,
                faltaAlguno
                        ? "Se guardaron los resultados cargados. El examen queda en proceso porque faltan valores."
                        : "Resultados guardados. El examen quedó marcado como completado.",
                "Guardado", javax.swing.JOptionPane.INFORMATION_MESSAGE);

        for (GuardarListener listener : listenersGuardar) {
            listener.onGuardado();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBorder1 = new vistas.panels.PanelBorder();
        encabezadoDatosPaciente2 = new vistas.registrarResultados.EncabezadoDatosPaciente();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 992, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 80, Short.MAX_VALUE)
        );

        jButton1.setText("Guardar");

        jButton2.setText("Cancelar");

        javax.swing.GroupLayout panelBorder1Layout = new javax.swing.GroupLayout(panelBorder1);
        panelBorder1.setLayout(panelBorder1Layout);
        panelBorder1Layout.setHorizontalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(encabezadoDatosPaciente2, javax.swing.GroupLayout.DEFAULT_SIZE, 992, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBorder1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        panelBorder1Layout.setVerticalGroup(
            panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBorder1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(encabezadoDatosPaciente2, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelBorder1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(32, 32, 32))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelBorder1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private vistas.registrarResultados.EncabezadoDatosPaciente encabezadoDatosPaciente2;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private vistas.panels.PanelBorder panelBorder1;
    // End of variables declaration//GEN-END:variables
}
