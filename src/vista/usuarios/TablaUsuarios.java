/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vista.usuarios;

import vistas.acciones.TableActionCellEditor;
import vistas.acciones.TableActionCellRender;
import vistas.acciones.TableActionEvent;

/**
 *
 * @author agust
 */
public class TablaUsuarios extends javax.swing.JPanel {

    /**
     * Columna "Id" del modelo (queda oculta en la vista, se deja cargada igual que en las demás
     * tablas de la app -- ver {@link vistas.pacientes.TablaPacientes} -- por si hiciera falta más
     * adelante).
     */
    private static final int COLUMNA_ID = 5;
    private static final int COLUMNA_ACCION = 4;

    /**
     * Usuarios que están dibujados en la tabla en este momento, en el mismo orden que las filas
     * -- así {@link #configurarColumnaAccion()} puede recuperar el usuario de una fila sin
     * depender de la columna Id oculta.
     */
    private java.util.List<modelo.Usuario> usuariosActuales;

    /**
     * A quién avisarle cada vez que se (re)carga el listado (ver {@link #setAlCargarUsuarios}) --
     * lo usa {@link vistas.formulariosPrincipales.Usuarios} para mantener actualizados los
     * números de cardUsuarios1 (Total / Administradores / Tecnicos).
     */
    private java.util.function.Consumer<java.util.List<modelo.Usuario>> alCargarUsuarios;

    /**
     * Creates new form TablaUsuarios
     */
    public TablaUsuarios() {
        initComponents();
        configurarColumnaAccion();
        jTable1.getColumnModel().removeColumn(jTable1.getColumnModel().getColumn(COLUMNA_ID));
        ajustarColumnas();
        jTable1.setRowHeight(40);
        cargarUsuarios(controlador.UsuarioController.listarTodos(this));
    }

    /**
     * Reemplaza el contenido de la tabla por esta lista de usuarios.
     */
    public void cargarUsuarios(java.util.List<modelo.Usuario> usuarios) {
        this.usuariosActuales = usuarios;
        javax.swing.table.DefaultTableModel modeloTabla = (javax.swing.table.DefaultTableModel) jTable1.getModel();
        modeloTabla.setRowCount(0);
        for (modelo.Usuario usuario : usuarios) {
            modeloTabla.addRow(new Object[]{
                usuario.getApellidoYNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo() ? "Activo" : "Inactivo",
                "", // Accion: la pintan TableActionCellRender/Editor, ver configurarColumnaAccion()
                usuario.getIdUsuario()
            });
        }
        if (alCargarUsuarios != null) {
            alCargarUsuarios.accept(usuarios);
        }
    }

    /**
     * @param listener se llama cada vez que se (re)carga el listado, con la lista recién cargada.
     * Si ya había datos cargados, se llama una vez de entrada con lo que ya hay (el listado inicial
     * se trae en el constructor, antes de que la pantalla contenedora pueda cablear el listener).
     */
    public void setAlCargarUsuarios(java.util.function.Consumer<java.util.List<modelo.Usuario>> listener) {
        this.alCargarUsuarios = listener;
        if (usuariosActuales != null) {
            listener.accept(usuariosActuales);
        }
    }

    private void recargar() {
        cargarUsuarios(controlador.UsuarioController.listarTodos(this));
    }

    /**
     * Achica y centra las columnas Rol/Estado/Accion -- por defecto la tabla reparte el ancho en
     * partes iguales entre todas las columnas, y les sobraba espacio en blanco a estas tres, muy
     * cortas comparadas con Apellido y Nombre / Email.
     */
    private void ajustarColumnas() {
        javax.swing.table.TableColumnModel columnas = jTable1.getColumnModel();
        fijarAncho(columnas.getColumn(2), 110); // Rol
        fijarAncho(columnas.getColumn(3), 100); // Estado
        fijarAncho(columnas.getColumn(COLUMNA_ACCION), 150);

        javax.swing.table.DefaultTableCellRenderer centrado = new javax.swing.table.DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        columnas.getColumn(2).setCellRenderer(centrado); // Rol
        columnas.getColumn(3).setCellRenderer(centrado); // Estado
    }

    private void fijarAncho(javax.swing.table.TableColumn columna, int ancho) {
        columna.setMinWidth(ancho);
        columna.setMaxWidth(ancho);
        columna.setPreferredWidth(ancho);
    }

    /**
     * Instala los botones redondos de {@link vistas.acciones.PanelAction} (ver/editar/eliminar,
     * sin "compartir") en la columna "Accion" y define qué hace cada uno -- mismo mecanismo que
     * usa {@link vistas.pacientes.TablaPacientes}.
     */
    private void configurarColumnaAccion() {
        TableActionEvent event = new TableActionEvent() {
            @Override
            public void onView(int row) {
                abrirVerUsuario(usuariosActuales.get(row));
            }

            @Override
            public void onEdit(int row) {
                abrirEditarUsuario(usuariosActuales.get(row));
            }

            @Override
            public void onShare(int row) {
                // No se usa en "Cuentas y Permisos" -- el botón de compartir queda oculto.
            }

            @Override
            public void onDelete(int row) {
                confirmarEliminarUsuario(usuariosActuales.get(row));
            }
        };

        jTable1.getColumnModel().getColumn(COLUMNA_ACCION).setCellRenderer(new TableActionCellRender(true, true, false, true));
        jTable1.getColumnModel().getColumn(COLUMNA_ACCION).setCellEditor(new TableActionCellEditor(event, true, true, false, true));
    }

    /**
     * Abre el formulario de usuario en modo sólo lectura, con los datos ya cargados.
     */
    private void abrirVerUsuario(modelo.Usuario usuario) {
        CrearUsuario panel = new CrearUsuario(CrearUsuario.Modo.VER, usuario);

        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Ver Usuario",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        // Se centra respecto a la ventana completa, no a tablaUsuarios1: ese panel es sólo la
        // franja de la tabla, angosta y corrida hacia abajo dentro de la pantalla, así que
        // centrar contra "this" dejaba el diálogo corrido en vez de centrado en la ventana.
        dialogo.setLocationRelativeTo(javax.swing.SwingUtilities.getWindowAncestor(this));
        dialogo.setVisible(true);
    }

    /**
     * Abre el formulario de usuario en modo edición y guarda los cambios al confirmar.
     */
    private void abrirEditarUsuario(modelo.Usuario usuario) {
        CrearUsuario panel = new CrearUsuario(CrearUsuario.Modo.EDITAR, usuario);

        javax.swing.JDialog dialogo = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Editar Usuario",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        panel.getBotonCancelar().addActionListener(evt -> dialogo.dispose());
        panel.getBotonCrearUsuario().addActionListener(evt -> {
            boolean guardado = controlador.UsuarioController.actualizar(panel, panel.obtenerDatosUsuario());
            if (guardado) {
                dialogo.dispose();
                recargar();
            }
        });

        dialogo.getContentPane().add(panel);
        dialogo.pack();
        // Se centra respecto a la ventana completa, no a tablaUsuarios1: ese panel es sólo la
        // franja de la tabla, angosta y corrida hacia abajo dentro de la pantalla, así que
        // centrar contra "this" dejaba el diálogo corrido en vez de centrado en la ventana.
        dialogo.setLocationRelativeTo(javax.swing.SwingUtilities.getWindowAncestor(this));
        dialogo.setVisible(true);
    }

    /**
     * Pide confirmación y da de baja (lógica) al usuario elegido.
     */
    private void confirmarEliminarUsuario(modelo.Usuario usuario) {
        int confirmacion = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Seguro que querés eliminar a " + usuario.getApellidoYNombre() + "?",
                "Eliminar usuario", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        if (confirmacion != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        if (controlador.UsuarioController.desactivar(this, usuario.getIdUsuario())) {
            recargar();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Apellido y Nombre", "Email", "Rol", "Estado", "Accion", "Id"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, true, true, false
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 544, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
