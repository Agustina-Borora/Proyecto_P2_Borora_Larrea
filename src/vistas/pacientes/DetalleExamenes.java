/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas.pacientes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import modelo.AnalitoReferencia;
import modelo.OrdenResumen;
import modelo.Paciente;
import modelo.Sexo;

/**
 * Detalle de un paciente: pestaña "Ultimo Examen" (analitos, valores de referencia por sexo y
 * resultados cargados) y pestaña "Historial" (todas sus ordenes anteriores). Se muestra dentro
 * de {@link Vistadetallepaciente}, debajo de {@link EncabezadoVistaPaciente}.
 *
 * @author agust
 */
public class DetalleExamenes extends javax.swing.JPanel {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    /** id_sexo -> nombre_sexo, para armar los prefijos "M:"/"H:" de la columna de referencia. */
    private final Map<Integer, String> nombresSexo = new HashMap<>();

    private JLabel lblExamenNombre;
    private JLabel lblExamenFecha;
    private JLabel lblExamenEstado;
    private JTable tablaAnalitos;
    private JTable tablaHistorial;

    /** Paciente cargado actualmente (para saber contra qué sexo evaluar fuera de rango). */
    private Paciente pacienteActual;
    private int idSexoPacienteActual;

    /** Mismo orden que las filas de tablaAnalitos, para poder evaluar fuera de rango por fila. */
    private List<AnalitoReferencia> analitosMostrados = new ArrayList<>();

    /**
     * Creates new form DetalleExamenes
     */
    public DetalleExamenes() {
        initComponents();

        for (Sexo s : controlador.SexoController.listarTodos(this)) {
            nombresSexo.put(s.getIdSexo(), s.getNombreSexo());
        }

        armarTabUltimoExamen();
        armarTabHistorial();

        jButton1.addActionListener(evt -> {
            Window ventana = SwingUtilities.getWindowAncestor(this);
            if (ventana != null) {
                ventana.dispose();
            }
        });

        jButton2.addActionListener(evt -> abrirCompartirResultados());
    }

    /**
     * Arma la pestaña "Ultimo Examen": una tarjeta con el nombre/fecha/estado del examen más
     * reciente, y debajo la tabla de analitos con su valor de referencia y el resultado cargado.
     */
    private void armarTabUltimoExamen() {
        JPanel tarjeta = new JPanel(new GridLayout(1, 3, 10, 0));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        lblExamenNombre = new JLabel();
        lblExamenNombre.setFont(new Font("SansSerif", Font.BOLD, 16));

        lblExamenFecha = new JLabel();
        lblExamenFecha.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblExamenFecha.setForeground(new Color(0x70, 0x70, 0x70));
        lblExamenFecha.setHorizontalAlignment(SwingConstants.CENTER);

        lblExamenEstado = new JLabel();
        lblExamenEstado.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblExamenEstado.setHorizontalAlignment(SwingConstants.RIGHT);

        tarjeta.add(lblExamenNombre);
        tarjeta.add(lblExamenFecha);
        tarjeta.add(lblExamenEstado);

        tablaAnalitos = new JTable();
        tablaAnalitos.setRowHeight(28);
        tablaAnalitos.setModel(new DefaultTableModel(new Object[0][0],
                new String[]{"Analito", "Valor de Referencia", "Resultado"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        tablaAnalitos.setDefaultRenderer(Object.class, new ResultadoCellRenderer());

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(Color.WHITE);
        contenedor.add(tarjeta, BorderLayout.NORTH);
        contenedor.add(new JScrollPane(tablaAnalitos), BorderLayout.CENTER);

        jScrollPane1.setViewportView(contenedor);
        jScrollPane1.setBorder(null);
    }

    /**
     * Arma la pestaña "Historial": una tabla simple con todas las ordenes anteriores del
     * paciente.
     */
    private void armarTabHistorial() {
        tablaHistorial = new JTable();
        tablaHistorial.setRowHeight(26);
        tablaHistorial.setModel(new DefaultTableModel(new Object[0][0],
                new String[]{"Examen", "Fecha", "Estado"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        jScrollPane2.setViewportView(tablaHistorial);
    }

    /**
     * Carga el ultimo examen y el historial completo de un paciente.
     */
    public void cargarPaciente(Paciente paciente) {
        this.pacienteActual = paciente;
        this.idSexoPacienteActual = paciente != null ? paciente.getIdSexo() : 0;

        if (paciente == null) {
            return;
        }

        OrdenResumen ultimoExamen = controlador.PacienteController.buscarUltimoExamen(this, paciente.getIdPaciente());
        cargarUltimoExamen(ultimoExamen);

        List<OrdenResumen> historial = controlador.PacienteController.listarHistorial(this, paciente.getIdPaciente());
        cargarHistorial(historial);
    }

    private void cargarUltimoExamen(OrdenResumen examen) {
        DefaultTableModel modeloTabla = (DefaultTableModel) tablaAnalitos.getModel();
        modeloTabla.setRowCount(0);
        analitosMostrados = new ArrayList<>();

        if (examen == null) {
            lblExamenNombre.setText("Sin examenes registrados");
            lblExamenFecha.setText("");
            lblExamenEstado.setText("");
            return;
        }

        lblExamenNombre.setText(examen.getExamen());
        lblExamenFecha.setText(examen.getFecha() != null ? FORMATO_FECHA.format(examen.getFecha()) : "-");
        lblExamenEstado.setText(examen.getEstado());

        controlador.PacienteController.DetalleExamenPaciente detalle = controlador.PacienteController
                .cargarDetalleExamen(this, examen.getIdAnalisisTipo(), examen.getIdPedidoAnalisis());

        analitosMostrados = detalle.getAnalitos();

        for (AnalitoReferencia analito : analitosMostrados) {
            String valor = detalle.getResultados().get(analito.getIdAnalito());
            modeloTabla.addRow(new Object[]{
                analito.getNombreAnalito(),
                formatearReferencia(analito),
                valor != null ? valor : "-"
            });
        }
    }

    private void cargarHistorial(List<OrdenResumen> historial) {
        DefaultTableModel modeloTabla = (DefaultTableModel) tablaHistorial.getModel();
        modeloTabla.setRowCount(0);

        for (OrdenResumen orden : historial) {
            modeloTabla.addRow(new Object[]{
                orden.getExamen(),
                orden.getFecha() != null ? FORMATO_FECHA.format(orden.getFecha()) : "-",
                orden.getEstado()
            });
        }

        jTabbedPane1.setTitleAt(0, "Historial (" + historial.size() + ")");
    }

    /**
     * Arma el texto de la columna "Valor de Referencia": si el analito tiene rangos distintos
     * por sexo los muestra todos juntos (ej. "M: 12-15  |  H: 13-17"); si tiene uno solo
     * (aplica a cualquier sexo) muestra ese; si no tiene ninguno cargado, "-".
     */
    private String formatearReferencia(AnalitoReferencia analito) {
        if (!analito.getReferenciasPorSexo().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> entrada : analito.getReferenciasPorSexo().entrySet()) {
                if (sb.length() > 0) {
                    sb.append("  |  ");
                }
                String nombreSexo = nombresSexo.get(entrada.getKey());
                String inicial = nombreSexo != null && !nombreSexo.isEmpty()
                        ? nombreSexo.substring(0, 1).toUpperCase() : "?";
                sb.append(inicial).append(": ").append(entrada.getValue());
            }
            return sb.toString();
        }
        return analito.getReferenciaGeneral() != null ? analito.getReferenciaGeneral() : "-";
    }

    /**
     * True si el resultado es numerico y cae fuera del rango de referencia aplicable al sexo
     * del paciente (se buscan los dos primeros numeros dentro del texto de referencia, para no
     * depender de que venga en un formato exacto tipo "min-max"; si no se pueden encontrar dos
     * numeros, o el resultado no es numerico, no se marca nada para evitar falsos positivos).
     */
    private boolean estaFueraDeRango(AnalitoReferencia analito, Object valor) {
        if (valor == null || !"numerico".equals(analito.getTipoDato())) {
            return false;
        }
        String referencia = analito.referenciaParaSexo(idSexoPacienteActual);
        if (referencia == null) {
            return false;
        }

        List<Double> numeros = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+(?:[.,]\\d+)?").matcher(referencia);
        while (m.find() && numeros.size() < 2) {
            numeros.add(Double.parseDouble(m.group().replace(",", ".")));
        }
        if (numeros.size() != 2) {
            return false;
        }

        try {
            double numero = Double.parseDouble(valor.toString().replace(",", ".").trim());
            return numero < numeros.get(0) || numero > numeros.get(1);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Resalta en rojo (con un icono de advertencia) los resultados numericos fuera del rango de
     * referencia del paciente.
     */
    private class ResultadoCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel etiqueta = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            etiqueta.setForeground(Color.BLACK);
            etiqueta.setText(value != null ? value.toString() : "");

            if (column == 2 && row < analitosMostrados.size() && estaFueraDeRango(analitosMostrados.get(row), value)) {
                etiqueta.setForeground(new Color(0xC6, 0x28, 0x28));
                etiqueta.setText("⚠ " + etiqueta.getText());
            }

            return etiqueta;
        }
    }

    /**
     * Abre "Compartir Resultados" como ventana flotante sin decoracion, igual que el resto de
     * los popups de detalle de la aplicacion.
     */
    private void abrirCompartirResultados() {
        CompartitResultados panel = new CompartitResultados();

        Window ventanaDueña = SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                ventanaDueña, "Compartir Resultados", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setUndecorated(true);
        dialogo.setResizable(false);
        dialogo.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(220, 225, 222)));
        panel.setAlCancelar(dialogo::dispose);

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));

        jTabbedPane1.setBackground(new java.awt.Color(255, 255, 255));
        jTabbedPane1.addTab("Historial", jScrollPane2);
        jTabbedPane1.addTab("Ultimo Examen", jScrollPane1);

        jButton1.setText("Cerrar");

        jButton2.setText("Compartir");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(511, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(31, 31, 31)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(501, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 556, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(592, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(12, 12, 12)))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
}
