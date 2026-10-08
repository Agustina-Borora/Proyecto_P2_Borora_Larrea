package controlador;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import modelo.OrdenResumen;

/**
 * Controlador para la pantalla Registros: tanto {@link vistas.javafx.registros.RegistrosScreen}
 * (todas las órdenes, con Ver/Editar/Enviar) como el modo que usa Registrar Resultados (solo las
 * pendientes).
 */
public final class RegistroController {

    private RegistroController() {
    }

    public static List<OrdenResumen> listarTodos(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los registros",
                con -> dao.RegistroDAO.listarTodos(con),
                Collections.emptyList());
    }

    public static List<OrdenResumen> listarPendientes(Component padre) {
        return ConexionUtil.ejecutar(padre, "Error al listar los registros",
                con -> dao.RegistroDAO.listarPendientes(con),
                Collections.emptyList());
    }

    /**
     * Guarda todos los cambios de "Editar Orden" (Registros) en una sola transacción: los datos
     * del paciente (nombre, DNI, celular, email -- el resto del paciente, como fecha de
     * nacimiento y sexo, se trae y se deja tal cual estaba), el médico derivante (se busca o se
     * crea por nombre, igual que en Nuevo Análisis), la observación del análisis y su estado. Si
     * cualquier paso falla, no se guarda nada (rollback) en vez de dejar la orden a medio
     * actualizar.
     *
     * @param idPaciente       id_paciente dueño de la orden ({@link modelo.DetalleOrden#getIdPaciente()}).
     * @param idPedido         id_pedido de la orden ({@link modelo.DetalleOrden#getIdPedido()}).
     * @param idPedidoAnalisis id_pedido_analisis de esta fila ({@link modelo.DetalleOrden#getIdPedidoAnalisis()}).
     * @param estadoAnalisis   "pendiente" | "en_proceso" | "completado" (los tres únicos que ofrece
     *                         el selector de "Editar Orden" -- "urgente" y "cancelado" no se tocan
     *                         desde acá).
     * @return true si se guardó todo bien.
     */
    public static boolean guardarEdicionOrden(Component padre, int idPaciente, int idPedido, int idPedidoAnalisis,
            String nombre, String dni, String telefono, String email, String medicoDerivante,
            String observaciones, String estadoAnalisis) {
        Boolean resultado = ConexionUtil.ejecutarTransaccion(padre, "Error al guardar los cambios de la orden",
                con -> {
                    modelo.Paciente paciente = dao.PacienteDAO.buscarPorId(con, idPaciente);
                    if (paciente == null) {
                        return false;
                    }
                    paciente.setNyaPaciente(nombre);
                    paciente.setDni(dni);
                    paciente.setTelefono(telefono);
                    paciente.setEmail(email);
                    if (!dao.PacienteDAO.actualizar(con, paciente)) {
                        return false;
                    }

                    Integer idMedico = dao.MedicoDAO.obtenerOCrear(con, medicoDerivante);
                    if (!dao.RegistroDAO.actualizarMedicoPedido(con, idPedido, idMedico)) {
                        return false;
                    }

                    if (!dao.RegistroDAO.actualizarObservaciones(con, idPedidoAnalisis, observaciones)) {
                        return false;
                    }

                    return dao.PedidoAnalitoResultadoDAO.actualizarEstado(con, idPedidoAnalisis, estadoAnalisis);
                },
                false);
        boolean ok = resultado != null && resultado;
        if (ok) {
            // Si la orden ya tenía su PDF guardado, se vuelve a generar con los datos corregidos.
            reportes.InformesPdfService.pedidoModificado(idPedido);
        }
        return ok;
    }

    /**
     * Registra en `envios` que se mandó un resultado por un canal puntual (email o whatsapp) --
     * se llama solo después de haber mandado el mensaje de verdad (ver
     * {@link vistas.javafx.registros.EnviarResultadosDialog}), nunca antes.
     */
    public static boolean registrarEnvio(Component padre, int idPedido, String canal, String destino) {
        if (modelo.Sesion.idUsuario <= 0) {
            javax.swing.JOptionPane.showMessageDialog(padre,
                    "No hay una sesión iniciada (o se abrió esta pantalla sin pasar por el Login). "
                    + "Iniciá sesión antes de registrar un envío.",
                    "Sesión no iniciada", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return ConexionUtil.ejecutar(padre, "Error al registrar el envío",
                con -> dao.EnvioDAO.registrar(con, idPedido, canal, destino, modelo.Sesion.idUsuario),
                false);
    }

    /**
     * Igual que {@link #registrarEnvio}, pero anotando si se mandó con el PDF y cuál archivo, y
     * SIN carteles: pensado para el hilo en segundo plano de "Enviar Resultados" (un
     * {@code JOptionPane} disparado desde ese hilo era una de las causas de que la app se tildara).
     *
     * @return null si se registró bien, o el motivo por el que no se pudo (para mostrarlo en el resumen).
     */
    public static String registrarEnvioConPdf(int idPedido, String canal, String destino, boolean conPdf, String archivoPdf) {
        if (modelo.Sesion.idUsuario <= 0) {
            return "no hay una sesión iniciada, así que no quedó registrado en el historial";
        }
        java.sql.Connection con = conexiones.Conexion.conectar();
        if (con == null) {
            return "no se pudo conectar a la base para dejarlo registrado";
        }
        try {
            dao.EnvioDAO.registrarConPdf(con, idPedido, canal, destino, modelo.Sesion.idUsuario, conPdf, archivoPdf);
            return null;
        } catch (java.sql.SQLException e) {
            return "no quedó registrado en el historial (" + e.getMessage() + ")";
        } finally {
            try {
                con.close();
            } catch (java.sql.SQLException e) {
                // nada
            }
        }
    }
}
