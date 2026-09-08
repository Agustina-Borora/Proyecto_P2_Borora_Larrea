package vistas.registros;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import modelo.OrdenResumen;
import vistas.acciones.TableActionCellEditor;
import vistas.acciones.TableActionCellRender;
import vistas.acciones.TableActionEvent;

/**
 * Panel personalizado ({@link javax.swing.JPanel}) encargado de alojar, configurar y estilizar
 * la tabla principal de los registros.
 */
public class TablaRegistros extends javax.swing.JPanel {

    /**
     * Todas las órdenes traídas de la base la última vez que se cargó la tabla.
     */
    private List<OrdenResumen> ordenesTodas;

    /**
     * Si es true, cargarDatos() trae solo las órdenes pendientes, en proceso o urgentes (vía
     * RegistroDAO.listarPendientes()) en vez de todas.
     */
    private boolean soloPendientes = false;

    /**
     * Si es true, renderizarTabla() arma el modelo sin la columna Accion (no tiene sentido en
     * Registrar Resultados, que no la usa).
     */
    private boolean ocultarAccion = false;

    /**
     * Órdenes que están mostrándose en la tabla en este momento (todas, o el subconjunto que dejó
     * el último filtrar()).
     */
    private List<OrdenResumen> ordenesMostradas;

    /**
     * Constructor por defecto.
     */
    public TablaRegistros() {
        initComponents();
        vistas.panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae todas las órdenes de análisis (vía RegistroController) y las carga en la tabla.
     */
    private void cargarDatos() {
        ordenesTodas = soloPendientes
                ? controlador.RegistroController.listarPendientes(this)
                : controlador.RegistroController.listarTodos(this);
        renderizarTabla(ordenesTodas);
    }

    /**
     * Configura la tabla para la pantalla Registrar Resultados: de ahí en más solo trae órdenes
     * con estado pendiente, en proceso o urgente -urgentes primero- (ver
     * RegistroDAO.listarPendientes()), sin la columna Accion (acá no se usa) y con selección de
     * una sola fila por vez, para que "Siguiente" siempre tenga una única orden elegida.
     */
    public void configurarParaRegistrarResultados() {
        soloPendientes = true;
        ocultarAccion = true;
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        cargarDatos();
    }

    /**
     * Devuelve la orden correspondiente a la fila actualmente seleccionada en la tabla, según lo
     * que esté mostrado en pantalla en ese momento (ordenesMostradas, no ordenesTodas: si hay un
     * filtro activo el índice tiene que coincidir con lo que ve el usuario).
     */
    public OrdenResumen getOrdenSeleccionada() {
        int fila = jTable1.getSelectedRow();
        if (fila < 0 || ordenesMostradas == null || fila >= ordenesMostradas.size()) {
            return null;
        }
        return ordenesMostradas.get(fila);
    }

    /**
     * Filtra las órdenes ya cargadas en memoria (ordenesTodas) por DNI, Apellido y Nombre, número
     * de orden o examen, sin distinguir mayúsculas/minúsculas, y vuelve a dibujar la tabla con el
     * resultado.
     *
     * @param texto texto escrito en el buscador; vacío o null muestra todas
     */
    public void filtrar(String texto) {
        if (ordenesTodas == null) {
            return;
        }

        if (texto == null || texto.trim().isEmpty()) {
            renderizarTabla(ordenesTodas);
            return;
        }

        String buscado = texto.trim().toLowerCase();
        List<OrdenResumen> filtradas = new java.util.ArrayList<>();
        for (OrdenResumen o : ordenesTodas) {
            boolean coincide =
                    (o.getDni() != null && o.getDni().toLowerCase().contains(buscado))
                    || (o.getPaciente() != null && o.getPaciente().toLowerCase().contains(buscado))
                    || (o.getNumeroOrden() != null && o.getNumeroOrden().toLowerCase().contains(buscado))
                    || (o.getExamen() != null && o.getExamen().toLowerCase().contains(buscado));
            if (coincide) {
                filtradas.add(o);
            }
        }

        renderizarTabla(filtradas);
    }

    /**
     * Reconstruye el modelo de la tabla a partir de una lista de órdenes (todas, o el subconjunto
     * que dejó filtrar()).
     */
    private void renderizarTabla(List<OrdenResumen> lista) {
        ordenesMostradas = lista;

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        String[] columnas = ocultarAccion
                ? new String[]{"Orden", "DNI", "Paciente", "Examen", "Fecha", "Cobertura", "Estado", "Id"}
                : new String[]{"Orden", "DNI", "Paciente", "Examen", "Fecha", "Cobertura", "Estado", "Accion", "Id"};

        final int columnaAccion = ocultarAccion ? -1 : 7;

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], columnas) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == columnaAccion;
            }
        };

        for (OrdenResumen o : lista) {
            Object[] datosComunes = {
                o.getNumeroOrden(),
                o.getDni(),
                o.getPaciente(),
                o.getExamen(),
                o.getFecha() != null ? formatoFecha.format(o.getFecha()) : "-",
                o.getCobertura(),
                o.getEstado()
            };
            modelo.addRow(ocultarAccion ? new Object[]{
                datosComunes[0], datosComunes[1], datosComunes[2], datosComunes[3],
                datosComunes[4], datosComunes[5], datosComunes[6], o.getIdPedidoAnalisis()
            } : new Object[]{
                datosComunes[0], datosComunes[1], datosComunes[2], datosComunes[3],
                datosComunes[4], datosComunes[5], datosComunes[6], "", o.getIdPedidoAnalisis()
            });
        }

        jTable1.setModel(modelo);
        if (!ocultarAccion) {
            configurarColumnaAccion(columnaAccion);
        }

        // La columna Id sigue en el modelo (para recuperar la orden real al seleccionar una fila
        // sin depender solo del índice), solo se oculta de la vista.
        jTable1.getColumnModel().removeColumn(
                jTable1.getColumnModel().getColumn(jTable1.getColumnModel().getColumnCount() - 1));
    }

    /**
     * Instala en la columna "Accion" el panel de acciones, con todos los botones salvo eliminar
     * (los registros no se borran desde acá). No se llama cuando ocultarAccion es true
     * (Registrar Resultados no tiene esta columna).
     */
    private void configurarColumnaAccion(int columnaAccion) {
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
                System.out.println("Compartir orden fila: " + row);
            }
        };

        jTable1.getColumnModel().getColumn(columnaAccion).setCellRenderer(new TableActionCellRender(true, true, true, false));
        jTable1.getColumnModel().getColumn(columnaAccion).setCellEditor(new TableActionCellEditor(event, true, true, true, false));
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
                "Orden", "DNI", "Paciente", "Examen", "Fecha", "Cobertura", "Estado", "Accion"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, true, true, true
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
