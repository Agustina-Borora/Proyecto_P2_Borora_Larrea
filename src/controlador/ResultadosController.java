package controlador;

import java.awt.Component;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import modelo.DetalleOrden;
import modelo.Parametro;

/**
 * Controlador para vistas.registrarResultados.cargarReultados: traer los parámetros de un
 * examen (con lo ya guardado, si lo había) y guardar los valores cargados.
 */
public final class ResultadosController {

    private ResultadosController() {
    }

    /**
     * Parámetros de un examen junto con los valores ya guardados (si los había), por id_analito.
     */
    public static final class DatosExamen {
        private final List<Parametro> parametros;
        private final Map<Integer, String> valoresGuardados;

        public DatosExamen(List<Parametro> parametros, Map<Integer, String> valoresGuardados) {
            this.parametros = parametros;
            this.valoresGuardados = valoresGuardados;
        }

        public List<Parametro> getParametros() {
            return parametros;
        }

        public Map<Integer, String> getValoresGuardados() {
            return valoresGuardados;
        }
    }

    public static DatosExamen cargarExamen(Component padre, int idAnalisisTipo, int idPedidoAnalisis, int idSexoPaciente) {
        return ConexionUtil.ejecutar(padre, "Error al traer los parámetros del examen", con -> {
            List<Parametro> parametros = dao.AnalitoDAO.listarConReferencia(con, idAnalisisTipo, idSexoPaciente);
            Map<Integer, String> valores = dao.PedidoAnalitoResultadoDAO.listarResultados(con, idPedidoAnalisis);
            return new DatosExamen(parametros, valores);
        }, new DatosExamen(Collections.emptyList(), Collections.emptyMap()));
    }

    /**
     * Guarda todos los valores cargados y actualiza el estado del examen (completado si no falta
     * ninguno, en_proceso si falta alguno). Si queda completado, además avisa por email a los
     * técnicos activos -- ver {@link #notificarTecnico}.
     *
     * <p>El aviso por email se dispara en un hilo aparte, después de guardar (no adentro del
     * mismo {@link ConexionUtil#ejecutar}) -- encontrado a partir de que a Agus se le congelaba
     * la pantalla ("Java(TM) Platform SE binary no responde") justo al registrar un resultado
     * que completaba un examen. La causa: desde las pantallas de JavaFX este método se llama a
     * través de {@code vistas.javafx.PuenteEDT}, que usa
     * {@code SwingUtilities.invokeAndWait(...)} -- eso bloquea a la vez el hilo de JavaFX (el que
     * espera) Y el Event Dispatch Thread de Swing (el que ejecuta) hasta que este método termina.
     * Si el envío del email tardaba en fallar (SMTP caído/sin responder -- hasta ~10 segundos por
     * destinatario, ver {@code conexiones.EmailService}), los dos quedaban congelados ese rato,
     * que es justo lo que muestra Windows con el cartel de "no responde". El guardado en la base
     * ya se hizo antes de mandar el email, así que separarlo no cambia qué se guarda -- solo hace
     * que un email lento no trabe la pantalla.</p>
     */
    public static boolean guardarResultados(Component padre, int idPedidoAnalisis,
            Map<Integer, String> valores, boolean faltaAlguno) {
        boolean ok = ConexionUtil.ejecutar(padre, "Error al guardar los resultados", con -> {
            boolean guardadoOk = dao.PedidoAnalitoResultadoDAO.guardarTodos(con, idPedidoAnalisis, valores);
            if (guardadoOk) {
                dao.PedidoAnalitoResultadoDAO.actualizarEstado(con, idPedidoAnalisis,
                        faltaAlguno ? "en_proceso" : "completado");
            }
            return guardadoOk;
        }, false);

        if (ok) {
            // Si la orden ya tenía su PDF guardado (por ejemplo, se está corrigiendo un valor), se
            // vuelve a generar solo, en segundo plano, con el resultado nuevo.
            reportes.InformesPdfService.resultadoModificado(idPedidoAnalisis);
        }

        if (ok && !faltaAlguno) {
            new Thread(() -> notificarTecnicoEnSegundoPlano(idPedidoAnalisis), "notificar-tecnico").start();
        }

        return ok;
    }

    /**
     * Abre su propia Connection (la del guardado ya se cerró para cuando corre esto, en otro
     * hilo) y llama a {@link #notificarTecnico}. No usa {@link ConexionUtil} a propósito: esto ya
     * corre en un hilo aparte, así que no hay pantalla que se pueda congelar, y un
     * {@code JOptionPane} disparado desde un hilo que no es el EDT ni el de JavaFX podría romper.
     * Cualquier error queda solo impreso por consola, igual que ya hacía {@link #notificarTecnico}
     * con las fallas de envío puntuales.
     */
    private static void notificarTecnicoEnSegundoPlano(int idPedidoAnalisis) {
        if (!utilidades.PreferenciasSistema.isNotificarTecnico()) {
            return;
        }
        Connection con = conexiones.Conexion.conectar();
        if (con == null) {
            System.err.println("No se pudo notificar al técnico: no se pudo conectar a la base de datos.");
            return;
        }
        try {
            notificarTecnico(con, idPedidoAnalisis);
        } finally {
            try {
                if (!con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Junta los datos del informe de la ORDEN completa a la que pertenece este examen (todos sus
     * estudios terminados juntos, no solo este) -- ver {@link reportes.InformesPdfService}. El
     * estado (Normal/Alto/Bajo) de cada valor se calcula con el mismo
     * {@link utilidades.CalculoEstadoUtil#calcular(String, String)} que usa "Cargar Resultados".
     */
    public static reportes.ResultadosDatos obtenerParaImprimir(Component padre, DetalleOrden detalle) {
        return ConexionUtil.ejecutar(padre, "Error al traer los resultados para imprimir",
                con -> dao.InformeDAO.cargarInforme(con, detalle.getIdPedido()), null);
    }

    /**
     * Avisa por email a los técnicos activos que un análisis quedó completado (Figma: tarjeta
     * "Notificaciones del sistema" en Configuración &gt; Sistema, toggle "Notificar al técnico
     * cuando un análisis queda listo"). Si el toggle está apagado, no hay ningún técnico con email
     * cargado, o falla el envío (por ejemplo porque {@code email.properties} no está configurado
     * en esta máquina), no se avisa nada -- el guardado de los resultados ya quedó hecho en la
     * base, así que un email que no sale nunca lo frena.
     */
    private static void notificarTecnico(Connection con, int idPedidoAnalisis) {
        if (!utilidades.PreferenciasSistema.isNotificarTecnico()) {
            return;
        }
        List<String> emails = dao.NotificacionDAO.listarEmailsTecnicos(con);
        if (emails.isEmpty()) {
            return;
        }
        dao.NotificacionDAO.InfoParaNotificar info = dao.NotificacionDAO.buscarInfoParaNotificar(con, idPedidoAnalisis);
        if (info == null) {
            return;
        }

        String asunto = "Análisis listo: " + info.getExamen() + " (" + info.getNumeroOrden() + ")";
        String cuerpo = "El análisis \"" + info.getExamen() + "\" de la orden " + info.getNumeroOrden()
                + " (paciente: " + info.getPaciente() + ") quedó completado y listo para revisar.";

        for (String email : emails) {
            try {
                conexiones.EmailService.enviar(email, asunto, cuerpo);
            } catch (MessagingException | IOException e) {
                // Se deja registrado en consola para poder diagnosticarlo (ej. SMTP sin
                // configurar todavía en esta máquina) sin mostrar un error ni frenar el guardado,
                // que ya se hizo.
                System.err.println("No se pudo notificar por email a " + email + ": " + e.getMessage());
            }
        }
    }
}
