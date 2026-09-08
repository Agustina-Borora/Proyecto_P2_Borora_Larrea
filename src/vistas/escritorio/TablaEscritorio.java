package vistas.escritorio;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import modelo.DetalleOrden;
import modelo.OrdenResumen;
import vistas.acciones.TableActionCellEditor;
import vistas.acciones.TableActionCellRender;
import vistas.acciones.TableActionEvent;

/**
 * Panel que aloja la tabla de "últimas órdenes" del dashboard (escritorio.Escritorio).
 */
public class TablaEscritorio extends javax.swing.JPanel {

    private static final String[] COLUMNAS = {
        "Orden", "Paciente", "Examen", "Fecha", "Estado", "Accion", "Id"
    };

    private static final int CANTIDAD_ULTIMAS_ORDENES = 10;

    /**
     * Se dispara cuando el usuario aprieta "Cargar Resultados" en el detalle de una orden que
     * todavía no está completada.
     */
    public interface CargarResultadosListener {
        void onCargarResultados();
    }

    private final List<CargarResultadosListener> listenersCargarResultados = new java.util.ArrayList<>();

    public void addCargarResultadosListener(CargarResultadosListener listener) {
        listenersCargarResultados.add(listener);
    }

    /**
     * Órdenes que están mostrándose en la tabla en este momento (para poder recuperar la orden
     * real de la fila en la que se hizo clic en "Ver").
     */
    private List<OrdenResumen> ordenesMostradas;

    /**
     * Constructor por defecto.
     */
    public TablaEscritorio() {
        initComponents();
        vistas.panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae las últimas órdenes (EscritorioController) y las carga en la tabla, reemplazando el
     * modelo con datos vacíos que traía initComponents().
     */
    private void cargarDatos() {
        List<OrdenResumen> ordenes = controlador.EscritorioController.listarUltimasOrdenes(this, CANTIDAD_ULTIMAS_ORDENES);
        ordenesMostradas = ordenes;
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], COLUMNAS) {
            boolean[] canEdit = new boolean[]{false, false, false, false, false, true, false};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        for (OrdenResumen orden : ordenes) {
            modelo.addRow(new Object[]{
                orden.getNumeroOrden(),
                orden.getPaciente(),
                orden.getExamen(),
                orden.getFecha() != null ? formatoFecha.format(orden.getFecha()) : "",
                orden.getEstado(),
                "",
                orden.getIdPedidoAnalisis()
            });
        }

        jTable1.setModel(modelo);
        configurarColumnaAccion();

        // La columna Id sigue en el modelo (para recuperar la orden real al editar/hacer clic
        // en "Ver"), solo se oculta de la vista.
        jTable1.getColumnModel().removeColumn(
                jTable1.getColumnModel().getColumn(jTable1.getColumnModel().getColumnCount() - 1));
    }

    /**
     * Instala en la columna "Accion" el panel de acciones, mostrando "ver" (detalle de la orden)
     * y "editar" (datos del paciente de esa orden); no tiene sentido compartir ni eliminar
     * órdenes desde este resumen.
     */
    private void configurarColumnaAccion() {
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                mostrarDetalle(row);
            }

            @Override
            public void onEdit(int row) {
                editarPacienteDeOrden(row);
            }

            @Override
            public void onDelete(int row) {
                System.out.println("Eliminar orden fila: " + row);
            }

            @Override
            public void onShare(int row) {
                // No aplica: el botón compartir está oculto en esta tabla.
            }
        };

        jTable1.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender(true, true, false, false));
        jTable1.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(event, true, true, false, false));

        // Con dos botones visibles (ver + editar), la columna no necesita todo el ancho que le
        // tocaría por reparto automático entre columnas.
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(110);
        jTable1.getColumnModel().getColumn(5).setMaxWidth(110);
    }

    /**
     * Busca al paciente dueño de la orden de esa fila (por DNI, el único dato de paciente que
     * trae {@link OrdenResumen}) y abre "Editar Paciente" -el mismo formulario de
     * vistas.pacientes.TablaPacientes- como ventana flotante modal.
     */
    private void editarPacienteDeOrden(int row) {
        if (ordenesMostradas == null || row < 0 || row >= ordenesMostradas.size()) {
            return;
        }
        OrdenResumen orden = ordenesMostradas.get(row);

        modelo.Paciente paciente = controlador.PacienteController.buscarPorDni(this, orden.getDni());
        if (paciente == null) {
            return;
        }

        vistas.pacientes.EditarPaciente panel = new vistas.pacientes.EditarPaciente();
        panel.cargarDatosPaciente(paciente);

        java.awt.Window ventanaDueña = javax.swing.SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                ventanaDueña, "Editar Paciente", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        panel.setAlCancelar(dialogo::dispose);

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(ventanaDueña);
        dialogo.setVisible(true);
    }

    /**
     * Busca el detalle completo de la orden de la fila indicada y lo muestra en un popup modal
     * (envolviendo {@link EscritorioDetalledeOrden}, sin tocar su initComponents()). El diálogo
     * va sin decoración del sistema operativo -mismo criterio que {@code Principal}- para que se
     * vea como el popup flotante de la referencia (tipo {@code JOptionPane}) y no como una
     * ventana aparte con su propia barra de título. Si el usuario aprieta "Cargar Resultados" ahí
     * adentro, cierra el popup y avisa a los {@link CargarResultadosListener} suscriptos para que
     * la pantalla contenedora decida la navegación (esta tabla no conoce a Principal ni a
     * RegistrarResultados).
     */
    private void mostrarDetalle(int row) {
        if (ordenesMostradas == null || row < 0 || row >= ordenesMostradas.size()) {
            return;
        }
        OrdenResumen orden = ordenesMostradas.get(row);

        DetalleOrden detalle = controlador.EscritorioController.buscarDetalleOrden(this, orden.getIdPedidoAnalisis());
        if (detalle == null) {
            return;
        }

        EscritorioDetalledeOrden panelDetalle = new EscritorioDetalledeOrden();
        panelDetalle.cargarDetalle(detalle);

        java.awt.Window ventanaDueña = javax.swing.SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                ventanaDueña, "Detalle de Orden", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setUndecorated(true);
        dialogo.setResizable(false);
        dialogo.getRootPane().setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 225, 222)));
        dialogo.getContentPane().add(panelDetalle);
        dialogo.setSize(1000, 800);
        dialogo.setLocationRelativeTo(ventanaDueña);

        panelDetalle.addCargarResultadosListener(() -> {
            dialogo.dispose();
            for (CargarResultadosListener listener : listenersCargarResultados) {
                listener.onCargarResultados();
            }
        });

        dialogo.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        jScrollPane1.setBackground(new java.awt.Color(250, 255, 250));

        jTable1.setForeground(new java.awt.Color(255, 255, 255));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Orden", "Paciente", "Examen", "Fecha", "Estado", "Accion"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        add(jScrollPane1);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
