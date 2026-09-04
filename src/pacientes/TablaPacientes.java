package pacientes;
import acciones.TableActionCellEditor;
import acciones.TableActionCellRender;
import acciones.TableActionEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Paciente;

/**
 * Panel personalizado ({@link javax.swing.JPanel}) encargado de alojar, configurar
 * y estilizar la tabla principal de pacientes.
 *
 * <p>Aplica una línea de diseño moderna y minimalista inspirada en Look and Feel FlatLaf,
 * con fondo blanco, bordes sutiles, scrollbars personalizados tipo "píldora" sin flechas,
 * y resaltado dinámico (efecto <em>hover</em>) exclusivo para aquellas filas que contienen información.</p>
 *
 * <p>La usa la pantalla pacientes/Pacientes.java, y colabora con
 * {@link controlador.PacienteController} para traer y eliminar pacientes.</p>
 */
public class TablaPacientes extends javax.swing.JPanel {

    private static final String[] COLUMNAS = {
        "DNI", "Paciente", "Edad", "Telefono", "Email", "Obra Social", "Ultimo Examen", "Accion"
    };

    /**
     * Pacientes que están dibujados en la tabla en este momento (todos, o el
     * subconjunto que dejó el último filtro de búsqueda), en el mismo orden
     * que las filas. Se usa desde los botones de Accion (ver/editar/eliminar)
     * para saber a qué paciente corresponde la fila que tocaste, sin tener
     * que agregar una columna oculta de id a la tabla.
     */
    private List<Paciente> pacientesActuales;

    /**
     * Todos los pacientes traídos de la base la última vez que se cargó la
     * tabla. filtrar() busca sobre esta lista completa (no hace falta volver
     * a golpear la base en cada tecleo del buscador).
     */
    private List<Paciente> pacientesTodos;

    /**
     * Constructor por defecto.
     * <p>Inicializa los componentes gráficos autogenerados por el entorno de desarrollo
     * y aplica la personalización visual a la tabla y su contenedor de desplazamiento.</p>
     */
    public TablaPacientes() {
        initComponents();
        panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae los pacientes reales desde la base de datos (vía PacienteDAO) y
     * los carga en la tabla. Reemplaza el modelo con datos vacíos que traía
     * initComponents() por defecto.
     *
     * "Obra Social" y "Ultimo Examen" salen de JOINs armados dentro de
     * PacienteDAO.listarTodos() (planes_obra_social/obras_sociales y pedidos
     * respectivamente), no de columnas propias de la tabla pacientes.
     */
    private void cargarDatos() {
        pacientesTodos = controlador.PacienteController.listarTodos(this);
        renderizarTabla(pacientesTodos);
    }

    /**
     * Filtra los pacientes ya cargados en memoria (pacientesTodos) por DNI o
     * por Apellido y Nombre, sin distinguir mayúsculas/minúsculas, y vuelve a
     * dibujar la tabla con el resultado. Se llama en cada tecleo del
     * buscador (panels.Busqueda), así que no toca la base de datos.
     *
     * @param texto texto escrito en el buscador; vacío o null muestra todos
     *              los pacientes de nuevo.
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
     * Reconstruye el modelo de la tabla a partir de una lista de pacientes
     * (todos, o el subconjunto que dejó filtrar()) y vuelve a instalar los
     * botones de Accion, que se pierden cada vez que se reemplaza el modelo.
     */
    private void renderizarTabla(List<Paciente> lista) {
        pacientesActuales = lista;
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], COLUMNAS) {
            boolean[] canEdit = new boolean[]{false, false, false, false, false, false, false, true};

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
                ""   // Accion: la pintan TableActionCellRender/Editor, ver configurarColumnaAccion()
            });
        }

        jTable1.setModel(modelo);
        configurarColumnaAccion();
    }

    /**
     * Instala los botones de ver/editar/eliminar en la columna "Accion" y
     * define qué hace cada uno. El ver y el editar todavía son un placeholder
     * (no hay pantalla de detalle/edición de paciente armada todavía); el
     * eliminar sí está conectado de verdad a la base (con confirmación).
     */
    private void configurarColumnaAccion() {
        int columnaAccion = COLUMNAS.length - 1; // "Accion" es siempre la última

        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                // TODO: abrir la ficha del paciente cuando exista esa pantalla.
                Paciente p = pacientesActuales.get(row);
                System.out.println("Ver paciente DNI: " + p.getDni());
            }

            @Override
            public void onEdit(int row) {
                // TODO: abrir el formulario de edición cuando exista esa pantalla.
                Paciente p = pacientesActuales.get(row);
                System.out.println("Editar paciente DNI: " + p.getDni());
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
