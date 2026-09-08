package vistas.escritorio;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
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
     * Instala en la columna "Accion" el panel de acciones, mostrando solo "ver" (el escritorio
     * es un resumen, no tiene sentido editar ni eliminar órdenes desde ahí).
     */
    private void configurarColumnaAccion() {
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                System.out.println("Ver orden fila: " + row);
            }

            @Override
            public void onEdit(int row) {
                System.out.println("Editar orden fila: " + row);
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

        jTable1.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender(true, false, false, false));
        jTable1.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(event, true, false, false, false));

        // Con un solo botón visible, la columna no necesita todo el ancho que le tocaría por
        // reparto automático entre columnas.
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(60);
        jTable1.getColumnModel().getColumn(5).setMaxWidth(60);
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
