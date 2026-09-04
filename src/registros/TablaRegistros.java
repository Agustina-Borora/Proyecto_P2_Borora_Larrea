package registros;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import modelo.OrdenResumen;

/**
 * Panel personalizado ({@link javax.swing.JPanel}) encargado de alojar, configurar 
 * y estilizar la tabla principal de los registros.
 *
 * <p>Aplica una línea de diseño moderna y minimalista inspirada en Look and Feel FlatLaf, 
 * con fondo blanco, bordes sutiles, scrollbars personalizados tipo "píldora" sin flechas, 
 * y resaltado dinámico (efecto <em>hover</em>) exclusivo para aquellas filas que contienen información.</p>
 *
 * <p>La usan tanto Registros.java (todas las órdenes) como
 * RegistrarResultados.java (solo las pendientes, sin la columna Accion, vía
 * {@link #configurarParaRegistrarResultados()}), y colabora con
 * {@link controlador.RegistroController} para traer los datos.</p>
 */
public class TablaRegistros extends javax.swing.JPanel {

    private static final String[] COLUMNAS = {
        "Orden", "DNI", "Paciente", "Examen", "Fecha", "Cobertura", "Estado", "Accion"
    };

    /**
     * Todas las órdenes traídas de la base la última vez que se cargó la
     * tabla. filtrar() busca sobre esta lista completa.
     */
    private List<OrdenResumen> ordenesTodas;

    /**
     * Si es true, cargarDatos() trae solo las órdenes pendientes, en
     * proceso o urgentes (vía RegistroDAO.listarPendientes()) en vez de
     * todas. Lo activa Registrar Resultados con
     * configurarParaRegistrarResultados(); la pantalla de Registros no lo
     * toca y sigue mostrando todo.
     */
    private boolean soloPendientes = false;

    /**
     * Si es true, renderizarTabla() arma el modelo sin la columna Accion
     * (no tiene sentido en Registrar Resultados, que no la usa). Se activa
     * junto con soloPendientes.
     */
    private boolean ocultarAccion = false;

    /**
     * Órdenes que están mostrándose en la tabla en este momento (todas, o
     * el subconjunto que dejó el último filtrar()). getOrdenSeleccionada()
     * usa esta lista -no ordenesTodas- para que el índice de fila
     * coincida con lo que el usuario ve.
     */
    private List<OrdenResumen> ordenesMostradas;

    /**
     * Constructor por defecto.
     * <p>Inicializa los componentes gráficos autogenerados por el entorno de desarrollo
     * y aplica la personalización visual a la tabla y su contenedor de desplazamiento.</p>
     */
    public TablaRegistros() {
        initComponents();
        panels.EstiloTablaFlatLaf.aplicar(jTable1, jScrollPane1);
        cargarDatos();
    }

    /**
     * Trae todas las órdenes de análisis (vía RegistroController) y las
     * carga en la tabla. Reemplaza el modelo con datos vacíos que traía
     * initComponents() por defecto.
     */
    private void cargarDatos() {
        ordenesTodas = soloPendientes
                ? controlador.RegistroController.listarPendientes(this)
                : controlador.RegistroController.listarTodos(this);
        renderizarTabla(ordenesTodas);
    }

    /**
     * Configura la tabla para la pantalla Registrar Resultados: de ahí en
     * más solo trae órdenes con estado pendiente, en proceso o urgente
     * -urgentes primero- (ver RegistroDAO.listarPendientes()), sin la
     * columna Accion (acá no se usa) y con selección de una sola fila por
     * vez, para que "Siguiente" siempre tenga una única orden elegida. Se
     * llama una sola vez, después de construir el panel.
     */
    public void configurarParaRegistrarResultados() {
        soloPendientes = true;
        ocultarAccion = true;
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        cargarDatos();
    }

    /**
     * Devuelve la orden correspondiente a la fila actualmente seleccionada
     * en la tabla, según lo que esté mostrado en pantalla en ese momento
     * (ordenesMostradas, no ordenesTodas: si hay un filtro activo el
     * índice tiene que coincidir con lo que ve el usuario). Null si no hay
     * ninguna fila seleccionada.
     */
    public OrdenResumen getOrdenSeleccionada() {
        int fila = jTable1.getSelectedRow();
        if (fila < 0 || ordenesMostradas == null || fila >= ordenesMostradas.size()) {
            return null;
        }
        return ordenesMostradas.get(fila);
    }

    /**
     * Filtra las órdenes ya cargadas en memoria (ordenesTodas) por DNI,
     * Apellido y Nombre, número de orden o examen, sin distinguir
     * mayúsculas/minúsculas, y vuelve a dibujar la tabla con el resultado.
     * Se llama en cada tecleo del buscador (panels.Busqueda dentro de
     * FiltrosBusqueda), así que no toca la base de datos.
     *
     * @param texto texto escrito en el buscador; vacío o null muestra todas
     *              las órdenes de nuevo.
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
     * Reconstruye el modelo de la tabla a partir de una lista de órdenes
     * (todas, o el subconjunto que dejó filtrar()).
     */
    private void renderizarTabla(List<OrdenResumen> lista) {
        ordenesMostradas = lista;

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        String[] columnas = ocultarAccion
                ? new String[]{"Orden", "DNI", "Paciente", "Examen", "Fecha", "Cobertura", "Estado"}
                : COLUMNAS;

        DefaultTableModel modelo = new DefaultTableModel(new Object[0][0], columnas) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
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
            modelo.addRow(ocultarAccion ? datosComunes : new Object[]{
                datosComunes[0], datosComunes[1], datosComunes[2], datosComunes[3],
                datosComunes[4], datosComunes[5], datosComunes[6], ""
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
