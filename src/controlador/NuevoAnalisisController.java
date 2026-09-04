package controlador;

import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;
import modelo.Paciente;
import modelo.PedidoCreado;
import modelo.Prestacion;

/**
 * Controlador de la pantalla Nuevo Análisis. Antes, todo esto vivía como el
 * método generarOrden() de formulariosPrincipales.NuevoAnalisis: abría la
 * conexión, manejaba la transacción (commit/rollback) y llamaba a cuatro
 * DAO distintos, todo mezclado con el código de la Vista. Ahora la Vista
 * solo junta los datos que el usuario cargó en pantalla y se los pasa a
 * este Controlador.
 */
public final class NuevoAnalisisController {

    private NuevoAnalisisController() {
    }

    /** Resultado de generar una orden con éxito. */
    public static final class ResultadoOrden {
        private final PedidoCreado pedido;
        private final boolean pacienteNuevo;
        private final boolean pacienteActualizado;

        public ResultadoOrden(PedidoCreado pedido, boolean pacienteNuevo, boolean pacienteActualizado) {
            this.pedido = pedido;
            this.pacienteNuevo = pacienteNuevo;
            this.pacienteActualizado = pacienteActualizado;
        }

        public PedidoCreado getPedido() {
            return pedido;
        }

        public boolean isPacienteNuevo() {
            return pacienteNuevo;
        }

        public boolean isPacienteActualizado() {
            return pacienteActualizado;
        }
    }

    /**
     * Guarda el paciente (nuevo o actualizado), el pedido y sus análisis en
     * una sola transacción: si algo falla a mitad de camino se deshace todo
     * (rollback), para no dejar un pedido "a medias".
     *
     * @param padre                    componente sobre el que centrar los carteles de error.
     * @param pacienteExistente        true si el DNI tipeado corresponde a un paciente ya registrado.
     * @param idPacienteExistente      id_paciente existente (solo se usa si pacienteExistente es true).
     * @param datosPacienteCambiaron   true si hay que actualizar el paciente existente (se ignora si es nuevo).
     * @param datosPaciente            paciente armado con lo que hay en pantalla (nuevo o para actualizar).
     * @param medicoDerivante          nombre tipeado en "Medico Derivante" (puede venir vacío).
     * @param prestaciones             análisis elegidos en Solicitud de Análisis.
     * @return el resultado si se generó la orden, o null si falló (ya se mostró el cartel correspondiente).
     */
    public static ResultadoOrden generarOrden(Component padre, boolean pacienteExistente,
            Integer idPacienteExistente, boolean datosPacienteCambiaron, Paciente datosPaciente,
            String medicoDerivante, List<Prestacion> prestaciones) {

        if (conexiones.Sesion.idUsuario <= 0) {
            JOptionPane.showMessageDialog(padre,
                    "No hay una sesión iniciada (o se abrió esta pantalla sin pasar por el Login). "
                    + "Iniciá sesión antes de generar una orden.",
                    "Sesión no iniciada", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return ConexionUtil.ejecutarTransaccion(padre, "No se pudo generar la orden", con -> {

            boolean pacienteNuevo = false;
            boolean pacienteActualizado = false;
            Integer idPaciente;

            if (pacienteExistente) {
                idPaciente = idPacienteExistente;
                if (datosPacienteCambiaron) {
                    if (!dao.PacienteDAO.actualizar(con, datosPaciente)) {
                        throw new OperacionCancelada();
                    }
                    pacienteActualizado = true;
                }
            } else {
                Integer nuevoId = dao.PacienteDAO.insertar(con, datosPaciente);
                if (nuevoId == null) {
                    throw new OperacionCancelada();
                }
                idPaciente = nuevoId;
                pacienteNuevo = true;
            }

            Integer idMedico = dao.MedicoDAO.obtenerOCrear(con, medicoDerivante);

            PedidoCreado pedido = dao.PedidoDAO.crearPedido(con, idPaciente, idMedico, conexiones.Sesion.idUsuario);
            if (pedido == null) {
                throw new OperacionCancelada();
            }

            for (Prestacion prestacion : prestaciones) {
                Integer idAnalisisTipo = dao.AnalisisTipoDAO.obtenerOCrearDesdeNomenclador(con, prestacion);
                if (idAnalisisTipo == null || !dao.PedidoDAO.agregarAnalisis(con, pedido.getIdPedido(), idAnalisisTipo)) {
                    throw new OperacionCancelada();
                }
            }

            return new ResultadoOrden(pedido, pacienteNuevo, pacienteActualizado);

        }, null);
    }
}
