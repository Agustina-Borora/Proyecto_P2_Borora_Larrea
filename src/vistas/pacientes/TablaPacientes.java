package vistas.pacientes;
import vistas.acciones.TableActionCellEditor;
import vistas.acciones.TableActionCellRender;
import vistas.acciones.TableActionEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Paciente;

/**
 * Panel personalizado ({@link javax.swing.JPanel}) encargado de alojar, configurar y estilizar
 * la tabla principal de pacientes.
 */
public class TablaPacientes extends javax.swing.JPanel {

    private static final String[] COLUMNAS = {
        "DNI", "Paciente", "Edad", "Telefono", "Email", "Obra Social", "Ultimo Examen", "Accion", "Id"
    };

    /**
     * Índice fijo de la columna Accion: no se puede calcular como COLUMNAS.length - 1 porque Id
     * quedó después.
     */
    private static final int COLUMNA_ACCION = 7;

    /**
     * Pacientes que están dibujados en la tabla en este momento (todos, o el subconjunto que dejó
     * el último filtro de búsqueda), en el mismo orden que las filas.
     */
    private List<Paciente> pacientesActuales;

    /**
     * Todos los pacientes traídos de la base la última vez que se cargó la tabla.
     */
    private List<Paciente> pacientesTodos;

    /**
     * Constructor por defecto.
     */
    public TablaPacientes() {
        initComponents();
        vistas.panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae los pacientes reales desde la base de datos (vía PacienteDAO) y los carga en la tabla.
     */
    private void cargarDatos() {
        pacientesTodos = controlador.PacienteController.listarTodos(this);
        renderizarTabla(pacientesTodos);
    }

    /**
     * Filtra los pacientes ya cargados en memoria (pacientesTodos) por DNI o por Apellido y
     * Nombre, sin distinguir mayúsculas/minúsculas, y vuelve a dibujar la tabla con el resultado.
     *
     * @param texto texto escrito en el buscador; vacío o null muestra todos
     */
    public void filtrar(String texto) {
        if (pacientesTodos == null) {
            return;
        }

        if (texto == null || texto.trim().isEmpty()) {
            renderizarTabla(pacientesTodos);
            return;
        }

        String buscado = texto.trim().toLowerCase();
        List<Paciente> filtrados = new java.util.ArrayList<>();
        for (Paciente p : pacientesTodos) {
            boolean coincideDni = p.getDni() != null && p.getDni().toLowerCase().contains(buscado);
            boolean coincideNombre = p.getNyaPaciente() != null && p.getNyaPaciente().toLowerCase().contains(buscado);
            if (coincideDni || coincideNombre) {
                filtrados.add(p);
            }
        }

        renderizarTabla(filtrados);
    }

    /**
     * Reconstruye el modelo de la tabla a partir de una lista de pacientes (todos, o el
     * subconjunto que dejó filtrar()) y vuelve a instalar los botones de Accion, que se pierden
     * cada vez que se reemplaza el modelo.
     */
    private void renderizarTabla(List<Paciente> lista) {
        pacientesActuales = lista;
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], COLUMNAS) {
            boolean[] canEdit = new boolean[]{false, false, false, false, false, false, false, true, false};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        for (Paciente p : lista) {
            modelo.addRow(new Object[]{
                p.getDni(),
                p.getNyaPaciente(),
                p.calcularEdad(),
                p.getTelefono(),
                p.getEmail(),
                p.getNombreObraSocial() != null ? p.getNombreObraSocial() : "Particular",
                p.getUltimoExamen() != null ? formatoFecha.format(p.getUltimoExamen()) : "-",
                "",   // Accion: la pintan TableActionCellRender/Editor, ver configurarColumnaAccion()
                p.getIdPaciente()
            });
        }

        jTable1.setModel(modelo);
        configurarColumnaAccion();

        // La columna Id sigue en el modelo (por si en algún momento hace falta recuperar el
        // paciente real por otra vía que no sea el índice de fila), solo se oculta de la vista.
        jTable1.getColumnModel().removeColumn(
                jTable1.getColumnModel().getColumn(jTable1.getColumnModel().getColumnCount() - 1));
    }

    /**
     * Instala los botones de ver/editar/eliminar en la columna "Accion" y define qué hace cada
     * uno.
     */
    private void configurarColumnaAccion() {
        int columnaAccion = COLUMNA_ACCION;

        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                Paciente p = pacientesActuales.get(row);
                System.out.println("Ver paciente DNI: " + p.getDni());
            }

            @Override
            public void onEdit(int row) {
                Paciente p = pacientesActuales.get(row);
                abrirEditarPaciente(p);
            }

            @Override
            public void onShare(int row) {
                abrirCompartirResultados();
            }

            @Override
            public void onDelete(int row) {
                Paciente p = pacientesActuales.get(row);

                int confirmacion = JOptionPane.showConfirmDialog(
                        TablaPacientes.this,
                        "¿Estás seguro de eliminar al paciente " + p.getNyaPaciente() + "?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirmacion != JOptionPane.YES_OPTION) {
                    return;
                }

                boolean ok = controlador.PacienteController.eliminar(TablaPacientes.this, p.getIdPaciente());
                if (ok) {
                    ((DefaultTableModel) jTable1.getModel()).removeRow(row);
                    pacientesActuales.remove(row);
                }
            }
        };

        jTable1.getColumnModel().getColumn(columnaAccion).setCellRenderer(new TableActionCellRender());
        jTable1.getColumnModel().getColumn(columnaAccion).setCellEditor(new TableActionCellEditor(event));
    }

    /**
     * Abre "Editar Paciente" como ventana flotante (modal), precargada con los datos del
     * paciente elegido.
     */
    private void abrirEditarPaciente(Paciente paciente) {
        EditarPaciente panel = new EditarPaciente();
        panel.cargarDatosPaciente(paciente);

        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Editar Paciente",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        panel.setAlCancelar(dialogo::dispose);

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    /**
     * Abre "Compartir Resultados" como ventana flotante (modal).
     */
    private void abrirCompartirResultados() {
        CompartitResultados panel = new CompartitResultados();

        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Compartir Resultados",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        panel.setAlCancelar(dialogo::dispose);

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));

        jScrollPane1.setBackground(new java.awt.Color(250, 255, 250));

        jTable1.setForeground(new java.awt.Color(255, 255, 255));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "DNI", "Paciente", "Edad", "Telefono", "Email", "Obra Social", "Ultimo Examen", "Accion"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 930, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
