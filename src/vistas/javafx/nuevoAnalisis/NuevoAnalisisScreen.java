package vistas.javafx.nuevoAnalisis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import modelo.DatosCobertura;
import modelo.ObraSocial;
import vistas.javafx.PuenteEDT;

/**
 * Pantalla "Nuevo Análisis", en JavaFX puro (sin FXML). Reemplaza a
 * {@link vistas.formulariosPrincipales.NuevoAnalisis}, cuyo layout (GroupLayout con anchos fijos
 * en píxeles, ej. 982, 1597, 804) no se adaptaba al tamaño de la ventana.
 *
 * <p>Combina tres sub-paneles (Datos del Paciente, Tipo de Cobertura -con sus datos de obra
 * social/mixto adentro- y Estudios solicitados -con precio por fila y total en vivo-), todo
 * dentro de un {@link ScrollPane} que ajusta el ancho al de la ventana ({@code setFitToWidth}),
 * para que la pantalla completa se pueda usar en cualquier resolución sin scroll horizontal.
 * Sigue el diseño de Figma "Nuevo Análisis - Por parte de la bioquímica" (nodos 246:2617 /
 * 246:2842).</p>
 */
public class NuevoAnalisisScreen {

    private final DatosPersonalesPanel datosPersonalesPanel = new DatosPersonalesPanel();
    private final TipoCoberturaPanel tipoCoberturaPanel = new TipoCoberturaPanel();
    private final SolicitudAnalisisPanel solicitudAnalisisPanel = new SolicitudAnalisisPanel();

    private Button botonGenerar;

    /**
     * Precio vigente de la UB, cargado una vez al abrir la pantalla (tabla `valor_ub`); null si
     * todavía no hay ningún valor cargado en la base.
     */
    private BigDecimal valorUbActual;

    public Parent construir() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("pantalla");
        raiz.setTop(armarEncabezado());

        VBox contenido = new VBox(20,
                datosPersonalesPanel.construir(),
                tipoCoberturaPanel.construir(),
                solicitudAnalisisPanel.construir(),
                armarBarraBotones());
        contenido.setPadding(new Insets(16, 2, 24, 2));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        BorderPane.setMargin(scroll, new Insets(16, 0, 0, 0));
        raiz.setCenter(scroll);

        solicitudAnalisisPanel.addSeleccionListener(this::actualizarEstadoBotonGenerar);

        cargarValorUb();
        actualizarEstadoBotonGenerar();

        return raiz;
    }

    private VBox armarEncabezado() {
        Label titulo = new Label("Nuevo Análisis");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Registrar nuevo paciente y solicitar análisis");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        return new VBox(4, titulo, subtitulo);
    }

    private HBox armarBarraBotones() {
        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> confirmarCancelar());

        botonGenerar = new Button();
        botonGenerar.getStyleClass().add("boton-primario");
        botonGenerar.setOnAction(evt -> intentarGenerarOrden());

        HBox fila = new HBox(12, botonCancelar, botonGenerar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    /**
     * El botón "Generar Orden" muestra cuántos estudios hay elegidos y se deshabilita mientras
     * no haya ninguno, siguiendo el diseño de Figma ("Seleccioná al menos un estudio" → "Generar
     * Orden (N estudios)").
     */
    private void actualizarEstadoBotonGenerar() {
        int cantidad = solicitudAnalisisPanel.getPrestacionesSeleccionadas().size();
        if (cantidad == 0) {
            botonGenerar.setText("Seleccioná al menos un estudio");
            botonGenerar.setDisable(true);
        } else {
            botonGenerar.setText("Generar Orden (" + cantidad + (cantidad == 1 ? " estudio)" : " estudios)"));
            botonGenerar.setDisable(false);
        }
    }

    /**
     * Trae el precio vigente de la UB (tabla `valor_ub`) y se lo pasa a Estudios Solicitados,
     * que es quien ahora muestra el precio por fila y el total.
     */
    private void cargarValorUb() {
        valorUbActual = PuenteEDT.ejecutar(
                () -> controlador.ValorUbController.obtenerVigente(null),
                null);
        solicitudAnalisisPanel.actualizarValorUb(valorUbActual);
    }

    /**
     * Junta los nombres de todos los campos obligatorios que falten completar (Médico Derivante,
     * Observación, Plan y Nro. de Afiliado quedan afuera porque son opcionales).
     */
    private List<String> datosFaltantes() {
        List<String> faltan = new ArrayList<>();

        String dni = datosPersonalesPanel.getDniPaciente();
        if (dni == null || dni.trim().length() != 8) {
            faltan.add("DNI (debe tener 8 dígitos)");
        } else {
            if (esVacio(datosPersonalesPanel.getApellidoYNombre())) {
                faltan.add("Apellido y Nombre");
            }
            if (datosPersonalesPanel.getFechaNacimientoPaciente() == null) {
                faltan.add("Fecha de Nacimiento");
            }
            if (esVacio(datosPersonalesPanel.getCelularPaciente())) {
                faltan.add("Celular");
            }
            if (esVacio(datosPersonalesPanel.getEmailPaciente())) {
                faltan.add("Email");
            }
        }

        if (solicitudAnalisisPanel.getPrestacionesSeleccionadas().isEmpty()) {
            faltan.add("Al menos un análisis solicitado");
        }

        String cobertura = tipoCoberturaPanel.getCoberturaSeleccionada();
        boolean necesitaObraSocial = TipoCoberturaPanel.OBRA_SOCIAL.equals(cobertura) || TipoCoberturaPanel.MIXTO.equals(cobertura);
        if (necesitaObraSocial && tipoCoberturaPanel.getObraSocialSeleccionada() == null) {
            faltan.add("Obra Social");
        }
        if (TipoCoberturaPanel.MIXTO.equals(cobertura)) {
            BigDecimal monto = tipoCoberturaPanel.getMontoEfectivo();
            if (monto == null || monto.signum() <= 0) {
                faltan.add("Monto que abona el paciente en efectivo");
            } else if (!solicitudAnalisisPanel.getPrestacionesSeleccionadas().isEmpty()
                    && monto.compareTo(solicitudAnalisisPanel.calcularTotal()) > 0) {
                // No tiene sentido que lo que abona el paciente en efectivo sea más que el total
                // del pedido -- la obra social terminaría "cubriendo" un monto negativo. Se
                // valida acá (y no sólo con el tope de dígitos del campo) porque un monto
                // inválido pero bien formado, como $999999 en un pedido de $3000, no lo detecta
                // el formato del campo.
                faltan.add("Monto en efectivo (no puede ser mayor al total del pedido)");
            }
        }

        return faltan;
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private void intentarGenerarOrden() {
        List<String> faltan = datosFaltantes();
        if (!faltan.isEmpty()) {
            Alert alerta = new Alert(AlertType.WARNING,
                    "Faltan completar los siguientes datos:\n\n• " + String.join("\n• ", faltan));
            alerta.setHeaderText(null);
            alerta.setTitle("Datos incompletos");
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
            return;
        }
        generarOrden();
    }

    /**
     * Guarda el paciente (nuevo o actualizado), el pedido, sus análisis y la cobertura elegida
     * en la base de datos, todo en una sola transacción (a través de
     * {@link controlador.NuevoAnalisisController}): si algo falla a mitad de camino se deshace
     * todo (rollback) para no dejar un pedido "a medias".
     */
    private void generarOrden() {
        boolean pacienteExistente = datosPersonalesPanel.esPacienteExistente();
        BigDecimal total = solicitudAnalisisPanel.calcularTotal();
        ObraSocial obraSocial = tipoCoberturaPanel.getObraSocialSeleccionada();

        DatosCobertura cobertura = new DatosCobertura(
                tipoCoberturaPanel.getCoberturaSeleccionada(),
                obraSocial,
                tipoCoberturaPanel.getPlan(),
                tipoCoberturaPanel.getNroAfiliado(),
                tipoCoberturaPanel.getMetodoPago(),
                tipoCoberturaPanel.getMontoEfectivo(),
                total);

        controlador.NuevoAnalisisController.ResultadoOrden resultado = PuenteEDT.ejecutar(
                () -> controlador.NuevoAnalisisController.generarOrden(
                        null,
                        pacienteExistente,
                        pacienteExistente ? datosPersonalesPanel.getIdPacienteExistente() : null,
                        pacienteExistente && datosPersonalesPanel.datosPacienteCambiaron(),
                        datosPersonalesPanel.construirPaciente(),
                        datosPersonalesPanel.getMedicoDerivante(),
                        solicitudAnalisisPanel.getPrestacionesSeleccionadas(),
                        cobertura),
                null);

        if (resultado == null) {
            return;
        }

        // Se arma ANTES de limpiar la pantalla -limpiarPantallaCompleta() vacía estos mismos
        // paneles enseguida después de mostrar el cartel de éxito-.
        reportes.ComprobanteOrdenDatos comprobante = new reportes.ComprobanteOrdenDatos(
                resultado.getPedido().getNumeroPedido(),
                new java.util.Date(),
                datosPersonalesPanel.getApellidoYNombre(),
                datosPersonalesPanel.getDniPaciente(),
                datosPersonalesPanel.getCelularPaciente(),
                datosPersonalesPanel.getEmailPaciente(),
                datosPersonalesPanel.getMedicoDerivante(),
                cobertura.getTipoCobertura(),
                obraSocial != null ? obraSocial.getNombre() : null,
                cobertura.getPlan(),
                cobertura.getNroAfiliado(),
                cobertura.getMetodoPago(),
                cobertura.getMontoEfectivo(),
                solicitudAnalisisPanel.getPrestacionesSeleccionadas(),
                total);

        StringBuilder mensaje = new StringBuilder();
        if (resultado.isPacienteNuevo()) {
            mensaje.append("Se registró un paciente nuevo.\n");
        } else if (resultado.isPacienteActualizado()) {
            mensaje.append("Se actualizaron los datos del paciente.\n");
        }
        mensaje.append("Orden generada correctamente: ").append(resultado.getPedido().getNumeroPedido());

        vistas.javafx.AvisoRapido.mostrar("Orden generada", mensaje.toString());

        ofrecerComprobante(comprobante);

        limpiarPantallaCompleta();
    }

    /**
     * Comprobante de la orden, según lo elegido en Configuración &gt; Informes PDF:
     * <ul>
     *   <li>"Preguntarme cada vez" (por defecto): pregunta si guardarlo en PDF, imprimirlo o nada.</li>
     *   <li>"Guardar el PDF automáticamente": lo guarda solo en la carpeta de comprobantes.</li>
     *   <li>"No hacer comprobante": no hace nada.</li>
     * </ul>
     * El PDF se escribe en segundo plano, así la pantalla queda libre para la próxima orden.
     */
    private void ofrecerComprobante(reportes.ComprobanteOrdenDatos comprobante) {
        String modo = utilidades.PreferenciasSistema.getModoComprobante();
        if (utilidades.PreferenciasSistema.COMPROBANTE_NUNCA.equals(modo)) {
            return;
        }
        if (utilidades.PreferenciasSistema.COMPROBANTE_AUTOMATICO.equals(modo)) {
            guardarComprobante(comprobante, false);
            return;
        }

        ButtonType guardarPdf = new ButtonType("Guardar PDF", javafx.scene.control.ButtonBar.ButtonData.YES);
        ButtonType imprimir = new ButtonType("Imprimir", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType nada = new ButtonType("No, gracias", javafx.scene.control.ButtonBar.ButtonData.NO);
        Alert pregunta = new Alert(AlertType.CONFIRMATION,
                "¿Querés el comprobante de la orden?\n\n(Si siempre hacés lo mismo, podés elegirlo una sola vez en "
                        + "Configuración > Informes PDF y no te lo vuelvo a preguntar.)",
                guardarPdf, imprimir, nada);
        pregunta.setHeaderText(null);
        pregunta.setTitle("Comprobante de la orden");
        vistas.javafx.AlertaUtil.estilizar(pregunta);
        pregunta.showAndWait().ifPresent(boton -> {
            if (boton == guardarPdf) {
                guardarComprobante(comprobante, true);
            } else if (boton == imprimir) {
                vistas.javafx.TareaFondo.ejecutar(() -> reportes.InformesPdfService.maquetarComprobante(comprobante),
                        doc -> javax.swing.SwingUtilities.invokeLater(() -> reportes.ImpresionUtil.imprimir(doc,
                                "Comprobante - Orden " + comprobante.getNumeroOrden())),
                        null);
            }
        });
    }

    private void guardarComprobante(reportes.ComprobanteOrdenDatos comprobante, boolean abrirAlTerminar) {
        vistas.javafx.TareaFondo.ejecutar(() -> {
            java.io.File archivo = reportes.InformesPdfService.guardarComprobante(comprobante);
            if (abrirAlTerminar) {
                utilidades.ArchivosUtil.abrir(archivo);
            }
            return archivo;
        }, archivo -> vistas.javafx.ToastUtil.exito("Comprobante guardado: " + archivo.getName()),
                error -> vistas.javafx.ToastUtil.error("No se pudo guardar el comprobante: "
                        + vistas.javafx.TareaFondo.mensaje(error)));
    }

    private void confirmarCancelar() {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Descartar los datos cargados y vaciar el formulario?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Cancelar");
        vistas.javafx.AlertaUtil.estilizar(confirmacion);
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.YES) {
                limpiarPantallaCompleta();
            }
        });
    }

    /**
     * Vacía las tres secciones de la pantalla (Datos del Paciente, Tipo de Cobertura vuelve a
     * Particular con sus campos vacíos, y la lista de análisis elegidos), para dejarla lista
     * para cargar la próxima orden.
     */
    private void limpiarPantallaCompleta() {
        datosPersonalesPanel.limpiarFormulario();
        solicitudAnalisisPanel.limpiarSeleccion();
        tipoCoberturaPanel.limpiarSeleccion();
    }
}
