package vistas.javafx.registros;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.DetalleOrden;
import reportes.InformesPdfService;
import reportes.ResultadosDatos;
import utilidades.ArchivosUtil;
import utilidades.PreferenciasSistema;
import utilidades.WhatsAppUtil;
import vistas.javafx.EstiloApp;
import vistas.javafx.TareaFondo;

/**
 * Popup "Enviar Resultados" de una ORDEN: manda el informe (todos los estudios terminados de la
 * orden, en un solo PDF) por email y/o WhatsApp.
 *
 * <p>Qué cambió respecto de antes:</p>
 * <ul>
 *   <li>El PDF lo genera el sistema solo, en el momento, con lo último cargado (resultados,
 *       colores y diseño). Ya no hay que imprimirlo con "Microsoft Print to PDF" ni ir a buscarlo
 *       con "Elegir archivo...": el sistema sabe dónde lo guarda.</li>
 *   <li>Por email el PDF va adjunto solo. Por WhatsApp (que no deja adjuntar archivos desde otro
 *       programa) se abre el chat con el mensaje escrito y el PDF queda COPIADO: en el chat se
 *       aprieta Ctrl+V y se pega. Además se abre la carpeta con el archivo ya marcado.</li>
 *   <li>Todo corre en segundo plano y se va mostrando el avance acá mismo, así la ventana no se
 *       tilda ("No responde") mientras se conecta con el servidor de email o abre WhatsApp.</li>
 *   <li>En la tabla {@code envios} queda anotado si cada envío fue con PDF y cuál archivo.</li>
 * </ul>
 */
public class EnviarResultadosDialog {

    private final DetalleOrden detalle;
    private final Runnable alTerminar;

    private Stage ventana;
    private ResultadosDatos datos;

    private CheckBox checkEmail;
    private CheckBox checkWhatsapp;
    private CheckBox checkAdjuntarPdf;
    private final Label valorEstudios = new Label("Cargando...");
    private final Label etiquetaPdf = new Label();
    private final ProgressIndicator progreso = new ProgressIndicator();
    private final Label etiquetaProgreso = new Label();
    private final VBox resumenEnvio = new VBox(6);
    private Button botonEnviar;
    private Button botonCancelar;
    private volatile boolean seEnvioAlgo;

    /**
     * @param alTerminar se ejecuta al cerrar el popup (haya mandado algo o no), para que
     *                    {@link RegistrosScreen} vuelva a cargar la tabla y el estado
     *                    "Enviado"/"Sin enviar" se actualice.
     */
    public EnviarResultadosDialog(DetalleOrden detalle, Runnable alTerminar) {
        this.detalle = detalle;
        this.alTerminar = alTerminar;
    }

    public void mostrar() {
        ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Enviar Resultados");
        ventana.setResizable(false);

        boolean tieneEmail = tieneTexto(detalle.getEmail());
        boolean tieneCelular = tieneTexto(detalle.getTelefono());

        VBox raiz = new VBox(16);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(24));
        raiz.setPrefWidth(540);

        progreso.setMaxSize(22, 22);
        progreso.setVisible(false);
        etiquetaProgreso.setWrapText(true);
        etiquetaProgreso.setMinHeight(Region.USE_PREF_SIZE);
        etiquetaProgreso.setStyle("-fx-font-size: 12px; -fx-text-fill: #3A4440;");
        HBox filaProgreso = new HBox(10, progreso, etiquetaProgreso);
        filaProgreso.setAlignment(Pos.CENTER_LEFT);

        raiz.getChildren().addAll(
                armarEncabezado(),
                armarResumen(),
                armarSeccionCanales(tieneEmail, tieneCelular),
                armarSeccionAdjunto(),
                filaProgreso,
                resumenEnvio,
                armarBotones());

        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        vistas.javafx.VentanaUtil.animarAparicion(ventana, raiz);
        ventana.setOnHidden(e -> {
            if (alTerminar != null) {
                alTerminar.run();
            }
        });

        cargarDatos();
        ventana.showAndWait();
    }

    /** Trae en segundo plano qué estudios van en el informe (para mostrarlo antes de mandar). */
    private void cargarDatos() {
        botonEnviar.setDisable(true);
        TareaFondo.ejecutar(() -> InformesPdfService.cargarDatos(detalle.getIdPedido()), d -> {
            datos = d;
            if (d.getEstudios().isEmpty()) {
                valorEstudios.setText("Todavía no hay estudios terminados en esta orden.");
                botonEnviar.setDisable(true);
                return;
            }
            String texto = d.nombresEstudios();
            if (!d.getEstudiosPendientes().isEmpty()) {
                texto += "\n(Todavía en proceso, van en otro envío: " + String.join(", ", d.getEstudiosPendientes()) + ")";
            }
            valorEstudios.setText(texto);
            etiquetaPdf.setText("📄 " + InformesPdfService.archivoInforme(d).getName()
                    + "  -- se arma de nuevo al enviar, con lo último cargado.");
            actualizarBotonEnviar();
        }, error -> {
            valorEstudios.setText("No se pudo cargar la orden: " + TareaFondo.mensaje(error));
            botonEnviar.setDisable(true);
        });
    }

    private Label armarEncabezado() {
        Label titulo = new Label("Enviar resultados — Orden #" + valorOGuion(detalle.getNumeroOrden()));
        titulo.getStyleClass().add("titulo-pantalla");
        titulo.setStyle("-fx-font-size: 18px;");
        return titulo;
    }

    private VBox armarResumen() {
        GridPane grilla = new GridPane();
        grilla.setHgap(24);
        grilla.setVgap(8);

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        agregarDato(grilla, 0, 0, "Paciente", new Label(valorOGuion(detalle.getPaciente())));
        agregarDato(grilla, 1, 0, "Fecha", new Label(detalle.getFecha() != null ? formatoFecha.format(detalle.getFecha()) : "-"));
        valorEstudios.setWrapText(true);
        valorEstudios.setMaxWidth(440);
        valorEstudios.setMinHeight(Region.USE_PREF_SIZE);
        Label etiqueta = new Label("Estudios que van en el informe (uno solo para toda la orden)");
        etiqueta.getStyleClass().add("etiqueta-campo");
        grilla.add(new VBox(4, etiqueta, valorEstudios), 0, 1, 2, 1);

        VBox seccion = new VBox(10, grilla);
        seccion.getStyleClass().add("tarjeta");
        return seccion;
    }

    private void agregarDato(GridPane grilla, int columna, int fila, String etiquetaTexto, Label valor) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        grilla.add(new VBox(4, etiqueta, valor), columna, fila);
    }

    private VBox armarSeccionCanales(boolean tieneEmail, boolean tieneCelular) {
        Label titulo = new Label("¿POR DÓNDE SE LO MANDAMOS?");
        titulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6E7874;");

        checkEmail = new CheckBox();
        checkEmail.setSelected(tieneEmail);
        checkEmail.setDisable(!tieneEmail);
        checkEmail.setOnAction(e -> actualizarBotonEnviar());
        checkWhatsapp = new CheckBox();
        checkWhatsapp.setSelected(tieneCelular);
        checkWhatsapp.setDisable(!tieneCelular);
        checkWhatsapp.setOnAction(e -> actualizarBotonEnviar());

        HBox filaEmail = armarFilaCanal(checkEmail, "✉ Email",
                tieneEmail ? detalle.getEmail() : "Sin email cargado en la ficha del paciente", tieneEmail);
        HBox filaWhatsapp = armarFilaCanal(checkWhatsapp, "📱 WhatsApp",
                tieneCelular ? detalle.getTelefono() : "Sin celular cargado en la ficha del paciente", tieneCelular);

        return new VBox(10, titulo, filaEmail, filaWhatsapp);
    }

    private HBox armarFilaCanal(CheckBox check, String nombreCanal, String destino, boolean listo) {
        Label etiquetaCanal = new Label(nombreCanal);
        etiquetaCanal.setStyle("-fx-font-weight: bold; -fx-text-fill: #141C19;");
        Label etiquetaDestino = new Label(destino);
        etiquetaDestino.setStyle("-fx-text-fill: #6E7874; -fx-font-size: 12.5px;");
        VBox textos = new VBox(2, etiquetaCanal, etiquetaDestino);

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        Label estado = new Label(listo ? "Disponible" : "Sin datos");
        estado.getStyleClass().add(listo ? "pill-activo" : "pill-inactivo");

        HBox fila = new HBox(12, check, textos, espaciador, estado);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add(listo ? "fila-canal-envio" : "fila-canal-envio-deshabilitada");
        return fila;
    }

    private VBox armarSeccionAdjunto() {
        checkAdjuntarPdf = new CheckBox("Adjuntar el informe en PDF");
        checkAdjuntarPdf.setStyle("-fx-font-weight: bold; -fx-text-fill: #141C19; -fx-font-size: 12.5px;");
        checkAdjuntarPdf.setSelected(true);

        etiquetaPdf.setWrapText(true);
        etiquetaPdf.setMaxWidth(490);
        etiquetaPdf.setMinHeight(Region.USE_PREF_SIZE);
        etiquetaPdf.setStyle("-fx-text-fill: #3A4440; -fx-font-size: 12px;");

        Button botonVistaPrevia = new Button("Ver cómo queda");
        botonVistaPrevia.getStyleClass().add("enlace-accion");
        botonVistaPrevia.setOnAction(e -> new VistaPreviaInformeDialog(detalle).sinBotonEnviar().mostrar());

        Label ayuda = new Label("Por email el PDF va adjunto solo. Por WhatsApp se abre el chat con el mensaje "
                + "escrito y el PDF queda copiado: en el chat apretá Ctrl+V para pegarlo (también se abre la "
                + "carpeta con el archivo marcado, por si preferís arrastrarlo).");
        ayuda.setWrapText(true);
        ayuda.setMaxWidth(490);
        ayuda.setMinHeight(Region.USE_PREF_SIZE);
        ayuda.getStyleClass().add("ayuda-diseno");

        HBox fila = new HBox(10, checkAdjuntarPdf, botonVistaPrevia);
        fila.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, fila, etiquetaPdf, ayuda);
    }

    private HBox armarBotones() {
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        botonEnviar = new Button("✓ Enviar Resultados");
        botonEnviar.getStyleClass().add("boton-primario");
        botonEnviar.setOnAction(evt -> enviar());

        HBox fila = new HBox(12, espaciador, botonCancelar, botonEnviar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    private void actualizarBotonEnviar() {
        boolean algunCanal = checkEmail.isSelected() || checkWhatsapp.isSelected();
        botonEnviar.setDisable(datos == null || datos.getEstudios().isEmpty() || !algunCanal);
    }

    // ------------------------------------------------------------------ envío (en segundo plano)

    private void enviar() {
        boolean porEmail = checkEmail.isSelected();
        boolean porWhatsapp = checkWhatsapp.isSelected();
        boolean conPdf = checkAdjuntarPdf.isSelected();

        botonEnviar.setDisable(true);
        botonCancelar.setDisable(true);
        checkEmail.setDisable(true);
        checkWhatsapp.setDisable(true);
        checkAdjuntarPdf.setDisable(true);
        progreso.setVisible(true);
        resumenEnvio.getChildren().clear();

        Thread hilo = new Thread(() -> {
            File pdf = null;
            if (conPdf) {
                paso("Preparando el PDF con lo último cargado...");
                try {
                    pdf = InformesPdfService.guardarInforme(detalle.getIdPedido());
                    linea(true, "PDF listo: " + pdf.getName());
                } catch (Exception e) {
                    linea(false, "No se pudo armar el PDF (" + TareaFondo.mensaje(e) + "). Se manda el aviso sin adjunto.");
                    pdf = null;
                }
            }

            if (porEmail) {
                paso("Enviando el email a " + detalle.getEmail() + "...");
                try {
                    String asunto = "Resultados de tus análisis -- Orden #" + valorOGuion(detalle.getNumeroOrden());
                    conexiones.EmailService.enviar(detalle.getEmail(), asunto, mensajeEmail(pdf != null), pdf);
                    seEnvioAlgo = true;
                    String falla = controlador.RegistroController.registrarEnvioConPdf(detalle.getIdPedido(), "email",
                            detalle.getEmail(), pdf != null, pdf == null ? null : pdf.getAbsolutePath());
                    linea(true, "Email enviado a " + detalle.getEmail() + (pdf != null ? " con el PDF adjunto." : ".")
                            + (falla != null ? " (Aviso: " + falla + ".)" : ""));
                } catch (Exception e) {
                    linea(false, "No se pudo enviar el email: " + TareaFondo.mensaje(e)
                            + " -- revisá la conexión a internet y email.properties.");
                }
            }

            if (porWhatsapp) {
                paso("Abriendo WhatsApp...");
                boolean seAbrio = WhatsAppUtil.abrirChat(detalle.getTelefono(), mensajeWhatsapp(pdf != null));
                if (seAbrio) {
                    seEnvioAlgo = true;
                    String falla = controlador.RegistroController.registrarEnvioConPdf(detalle.getIdPedido(), "whatsapp",
                            detalle.getTelefono(), pdf != null, pdf == null ? null : pdf.getAbsolutePath());
                    linea(true, "Se abrió WhatsApp con el mensaje listo para " + detalle.getTelefono()
                            + " -- apretá \"Enviar\" ahí para terminar." + (falla != null ? " (Aviso: " + falla + ".)" : ""));
                    if (pdf != null) {
                        File archivo = pdf;
                        Platform.runLater(() -> copiarAlPortapapeles(archivo));
                        try {
                            ArchivosUtil.mostrarEnCarpeta(archivo);
                        } catch (Exception e) {
                            // No es grave: el PDF ya quedó copiado para pegarlo con Ctrl+V.
                        }
                        linea(true, "El PDF quedó copiado: en el chat de WhatsApp apretá Ctrl+V para adjuntarlo.");
                    }
                } else {
                    linea(false, "No se pudo abrir WhatsApp en esta computadora.");
                }
            }

            Platform.runLater(() -> {
                progreso.setVisible(false);
                etiquetaProgreso.setText(seEnvioAlgo ? "Listo." : "No se pudo enviar nada.");
                botonCancelar.setText("Cerrar");
                botonCancelar.setDisable(false);
                botonCancelar.getStyleClass().setAll("button", "boton-primario");
                botonEnviar.setVisible(false);
                botonEnviar.setManaged(false);
            });
        }, "envio-resultados");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Copia el PDF al portapapeles de Windows como archivo (igual que hacer "Copiar" sobre él en
     * el explorador): así en WhatsApp alcanza con Ctrl+V para adjuntarlo.
     */
    private static void copiarAlPortapapeles(File archivo) {
        try {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putFiles(Collections.singletonList(archivo));
            Clipboard.getSystemClipboard().setContent(contenido);
        } catch (RuntimeException e) {
            // Si el sistema no deja copiar archivos, queda la carpeta abierta para arrastrarlo.
        }
    }

    private void paso(String texto) {
        Platform.runLater(() -> etiquetaProgreso.setText(texto));
    }

    private void linea(boolean bien, String texto) {
        Platform.runLater(() -> {
            Label l = new Label((bien ? "✓ " : "✗ ") + texto);
            l.setWrapText(true);
            l.setMaxWidth(490);
            l.setMinHeight(Region.USE_PREF_SIZE);
            l.setStyle(bien ? "-fx-text-fill: #1E5C3D; -fx-font-size: 12.5px;" : "-fx-text-fill: #9B1C1C; -fx-font-size: 12.5px;");
            resumenEnvio.getChildren().add(l);
            ventana.sizeToScene();
        });
    }

    private String estudiosTexto() {
        return datos == null ? "tus análisis" : datos.nombresEstudios();
    }

    private String mensajeEmail(boolean conPdfAdjunto) {
        String laboratorio = PreferenciasSistema.getNombreLaboratorio();
        String cuerpoResultado = conPdfAdjunto
                ? "Te adjuntamos en PDF los resultados de tus estudios (" + estudiosTexto() + ") -- Orden #"
                + valorOGuion(detalle.getNumeroOrden()) + "."
                : "Los resultados de tus estudios (" + estudiosTexto() + ") -- Orden #"
                + valorOGuion(detalle.getNumeroOrden()) + " -- ya están listos.\n\n"
                + "Podés pasar a retirarlos por " + laboratorio + " en nuestro horario habitual.";
        return "Hola " + valorOGuion(detalle.getPaciente()) + ",\n\n" + cuerpoResultado + "\n\n"
                + "Gracias por confiar en nosotros.\n" + laboratorio;
    }

    private String mensajeWhatsapp(boolean conPdfAdjunto) {
        String laboratorio = PreferenciasSistema.getNombreLaboratorio();
        String cuerpoResultado = conPdfAdjunto
                ? "te mandamos el PDF con los resultados de tus estudios (" + estudiosTexto() + ") -- Orden #"
                + valorOGuion(detalle.getNumeroOrden()) + "."
                : "los resultados de tus estudios (" + estudiosTexto() + ") -- Orden #"
                + valorOGuion(detalle.getNumeroOrden()) + " -- en " + laboratorio
                + " ya están listos. Podés pasar a retirarlos.";
        return "Hola " + valorOGuion(detalle.getPaciente()) + "! " + cuerpoResultado + " ¡Gracias! " + laboratorio;
    }

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    private static String valorOGuion(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor;
    }
}
