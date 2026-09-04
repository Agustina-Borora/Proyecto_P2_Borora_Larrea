package escritorio;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import modelo.OrdenResumen;

/**
 * Panel que aloja la tabla de "últimas órdenes" del dashboard (escritorio.Escritorio).
 * <p>
 * Al construirse le aplica el estilo visual compartido de {@link panels.EstiloTablaFlatLaf}
 * y carga los datos reales pidiéndole a {@link controlador.EscritorioController} las
 * {@value #CANTIDAD_ULTIMAS_ORDENES} órdenes más recientes, reemplazando el modelo
 * de filas vacías que trae {@code initComponents()}.
 * </p>
 */
public class TablaEscritorio extends javax.swing.JPanel {

    private static final String[] COLUMNAS = {
        "Orden", "Paciente", "Examen", "Fecha", "Estado", "Accion"
    };

    private static final int CANTIDAD_ULTIMAS_ORDENES = 10;

    /**
     * Constructor por defecto.
     * <p>Inicializa los componentes gráficos autogenerados por el entorno de desarrollo
     * y aplica la personalización visual a la tabla y su contenedor de desplazamiento.</p>
     */
    public TablaEscritorio() {
        initComponents();
        panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae las últimas órdenes (EscritorioController) y las carga en la tabla,
     * reemplazando el modelo con datos vacíos que traía initComponents().
     */
    private void cargarDatos() {
        List<OrdenResumen> ordenes = controlador.EscritorioController.listarUltimasOrdenes(this, CANTIDAD_ULTIMAS_ORDENES);
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], COLUMNAS) {
            boolean[] canEdit = new boolean[]{false, false, false, false, false, true};

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
                "Ver"
            });
        }

        jTable1.setModel(modelo);
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
