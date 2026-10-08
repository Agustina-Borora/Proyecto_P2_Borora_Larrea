package vistas.javafx.nuevoAnalisis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.ObraSocial;
import vistas.javafx.PuenteEDT;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" para elegir la cobertura de la orden: Particular,
 * Obra Social o Mixto -- y, según cuál se elija, los datos que hacen falta para esa cobertura
 * (siguiendo el diseño de Figma "Nuevo Análisis - Por parte de la bioquímica", nodo 246:2842):
 *
 * <ul>
 *   <li>Particular: método de pago.</li>
 *   <li>Obra Social: obra social, plan y nro. de afiliado.</li>
 *   <li>Mixto: obra social, plan, nro. de afiliado, método de pago y el monto que abona el
 *       paciente en efectivo (la obra social cubre el resto).</li>
 * </ul>
 *
 * <p>Antes de esta versión, Obra Social y Método de Pago vivían en una tarjeta aparte al final
 * de toda la pantalla ("Precio y Cobertura"); en Figma están acá, apenas se elige la cobertura
 * que los necesita -- así que se movieron a este panel.</p>
 */
public class TipoCoberturaPanel {

    public static final String PARTICULAR = "PARTICULAR";
    public static final String OBRA_SOCIAL = "OBRA_SOCIAL";
    public static final String MIXTO = "MIXTO";

    /**
     * Topes que coinciden con las columnas reales de la migración `2026-09-23_cobertura_pedido`
     * ({@code plan_obra_social VARCHAR(100)} y {@code nro_afiliado VARCHAR(50)}) -- antes no
     * había ningún límite acá, así que pegar un texto largo en cualquiera de los dos campos
     * tiraba un error de SQL en crudo recién al generar la orden.
     */
    private static final int LARGO_PLAN = 100;
    private static final int LARGO_NRO_AFILIADO = 50;

    /**
     * Hasta 8 dígitos enteros y 2 decimales -- el máximo que entra en la columna
     * {@code monto_efectivo DECIMAL(10,2)} sin desbordar. Reemplaza al filtro anterior
     * (`setOnKeyReleased`, que sólo limpiaba después de tipear y no bloqueaba un pegado con
     * Ctrl+V) por un {@link TextFormatter}, que valida ANTES de que el cambio se aplique.
     */
    private static final Pattern PATRON_MONTO = Pattern.compile("\\d{0,8}(\\.\\d{0,2})?");

    /**
     * Se avisa cada vez que cambia la tarjeta elegida, para que la pantalla principal recalcule
     * qué campos mostrar y, si hace falta, revalide.
     */
    public interface CoberturaListener {
        void onCoberturaCambiada(String clave);
    }

    private final Map<String, VBox> tarjetasCobertura = new LinkedHashMap<>();
    private final List<CoberturaListener> listenersCobertura = new ArrayList<>();
    private String coberturaSeleccionada;

    private final ComboBox<ObraSocial> comboObraSocial = new ComboBox<>();
    private final TextField campoPlan = new TextField();
    private final TextField campoNroAfiliado = new TextField();
    private final ComboBox<String> comboMetodoPago = new ComboBox<>();
    private final TextField campoMontoEfectivo = new TextField();

    private final VBox bloqueDatosObraSocial = new VBox(10);
    private final VBox bloqueMontoEfectivo = new VBox(6);
    private final VBox bloqueMetodoPago = new VBox(6);

    public Parent construir() {
        VBox raiz = new VBox(14);
        raiz.getStyleClass().add("tarjeta");

        Label titulo = new Label("Tipo de Cobertura");
        titulo.getStyleClass().add("titulo-seccion");

        HBox fila = new HBox(14);

        VBox tarjetaParticular = armarTarjeta("Particular", "El paciente abona el 100% del valor del estudio realizado");
        VBox tarjetaObraSocial = armarTarjeta("Obra Social", "La obra social cubre el 100% del estudio realizado");
        VBox tarjetaMixto = armarTarjeta("Mixto", "La obra social cubre parte del estudio -- el paciente abona la diferencia");

        registrarTarjeta(tarjetaParticular, PARTICULAR);
        registrarTarjeta(tarjetaObraSocial, OBRA_SOCIAL);
        registrarTarjeta(tarjetaMixto, MIXTO);

        fila.getChildren().addAll(tarjetaParticular, tarjetaObraSocial, tarjetaMixto);

        armarBloqueDatosObraSocial();
        armarBloqueMontoEfectivo();
        armarBloqueMetodoPago();

        raiz.getChildren().addAll(titulo, fila, bloqueDatosObraSocial, bloqueMontoEfectivo, bloqueMetodoPago);

        cargarObrasSociales();
        cargarMetodosPago();
        seleccionarCobertura(PARTICULAR);

        return raiz;
    }

    private VBox armarTarjeta(String tituloTexto, String descripcionTexto) {
        Label titulo = new Label(tituloTexto);
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label descripcion = new Label(descripcionTexto);
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-font-size: 12px; -fx-text-fill: #6E7874;");

        VBox tarjeta = new VBox(6, titulo, descripcion);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-seleccionable");
        tarjeta.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setPrefWidth(0);

        return tarjeta;
    }

    private void registrarTarjeta(VBox tarjeta, String clave) {
        tarjetasCobertura.put(clave, tarjeta);
        tarjeta.setOnMouseClicked(evt -> seleccionarCobertura(clave));
    }

    private void armarBloqueDatosObraSocial() {
        Label etiquetaSeccion = new Label("DATOS DE LA OBRA SOCIAL");
        etiquetaSeccion.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6E7874;");

        comboObraSocial.getStyleClass().add("campo-form");
        comboObraSocial.setPromptText("Seleccionar...");
        comboObraSocial.setMaxWidth(Double.MAX_VALUE);

        campoPlan.getStyleClass().add("campo-form");
        campoPlan.setPromptText("Ej: General, 210...");
        campoPlan.setMaxWidth(Double.MAX_VALUE);
        limitarLargo(campoPlan, LARGO_PLAN);

        campoNroAfiliado.getStyleClass().add("campo-form");
        campoNroAfiliado.setPromptText("Ej: 4-567890-2");
        campoNroAfiliado.setMaxWidth(Double.MAX_VALUE);
        limitarLargo(campoNroAfiliado, LARGO_NRO_AFILIADO);

        HBox fila = new HBox(16,
                bloqueCampo("Obra Social", comboObraSocial),
                bloqueCampo("Plan", campoPlan),
                bloqueCampo("Nro. de Afiliado", campoNroAfiliado));
        for (javafx.scene.Node columna : fila.getChildren()) {
            HBox.setHgrow(columna, Priority.ALWAYS);
        }

        bloqueDatosObraSocial.getChildren().setAll(etiquetaSeccion, fila);
        bloqueDatosObraSocial.setStyle("-fx-background-color: #F6FFF9; -fx-background-radius: 10; "
                + "-fx-border-color: #E6E9E7; -fx-border-radius: 10;");
        bloqueDatosObraSocial.setPadding(new Insets(14));
    }

    private void armarBloqueMontoEfectivo() {
        campoMontoEfectivo.getStyleClass().add("campo-form");
        campoMontoEfectivo.setPromptText("Ej: 4500");
        campoMontoEfectivo.setMaxWidth(Double.MAX_VALUE);
        campoMontoEfectivo.setTextFormatter(new TextFormatter<String>(cambio ->
                PATRON_MONTO.matcher(cambio.getControlNewText()).matches() ? cambio : null));

        Label etiqueta = new Label("Monto que abona el paciente en efectivo ($)");
        etiqueta.getStyleClass().add("etiqueta-campo");

        bloqueMontoEfectivo.getChildren().setAll(etiqueta, campoMontoEfectivo);
        bloqueMontoEfectivo.setStyle("-fx-background-color: #FFF8E6; -fx-background-radius: 10; "
                + "-fx-border-color: #E9D8A0; -fx-border-radius: 10;");
        bloqueMontoEfectivo.setPadding(new Insets(12));
    }

    private void armarBloqueMetodoPago() {
        comboMetodoPago.getStyleClass().add("campo-form");
        comboMetodoPago.setMaxWidth(Double.MAX_VALUE);

        Label etiqueta = new Label("Método de Pago");
        etiqueta.getStyleClass().add("etiqueta-campo");
        bloqueMetodoPago.getChildren().setAll(etiqueta, comboMetodoPago);
    }

    private VBox bloqueCampo(String etiquetaTexto, javafx.scene.control.Control campo) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        VBox bloque = new VBox(6, etiqueta, campo);
        bloque.setMaxWidth(Double.MAX_VALUE);
        return bloque;
    }

    /**
     * Impide escribir (o pegar) más caracteres de los que la columna puede guardar, en vez de
     * dejar que se escriba cualquier cosa y recién enterarse del problema al generar la orden.
     */
    private static void limitarLargo(TextField campo, int largoMaximo) {
        campo.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= largoMaximo ? cambio : null));
    }

    /**
     * Marca "clave" como la cobertura elegida, resalta solo esa tarjeta, muestra/oculta los
     * bloques de datos que corresponden y avisa a los listeners.
     */
    public void seleccionarCobertura(String clave) {
        if (!tarjetasCobertura.containsKey(clave)) {
            return;
        }
        coberturaSeleccionada = clave;
        for (Map.Entry<String, VBox> entrada : tarjetasCobertura.entrySet()) {
            boolean esElegida = entrada.getKey().equals(clave);
            VBox tarjeta = entrada.getValue();
            if (esElegida) {
                if (!tarjeta.getStyleClass().contains("tarjeta-seleccionada")) {
                    tarjeta.getStyleClass().add("tarjeta-seleccionada");
                }
            } else {
                tarjeta.getStyleClass().remove("tarjeta-seleccionada");
            }
        }

        boolean necesitaObraSocial = OBRA_SOCIAL.equals(clave) || MIXTO.equals(clave);
        boolean necesitaMontoEfectivo = MIXTO.equals(clave);
        boolean necesitaMetodoPago = PARTICULAR.equals(clave) || MIXTO.equals(clave);

        mostrar(bloqueDatosObraSocial, necesitaObraSocial);
        mostrar(bloqueMontoEfectivo, necesitaMontoEfectivo);
        mostrar(bloqueMetodoPago, necesitaMetodoPago);

        for (CoberturaListener listener : listenersCobertura) {
            listener.onCoberturaCambiada(clave);
        }
    }

    private void mostrar(VBox bloque, boolean visible) {
        bloque.setVisible(visible);
        bloque.setManaged(visible);
    }

    public void addCoberturaListener(CoberturaListener listener) {
        listenersCobertura.add(listener);
    }

    /**
     * "PARTICULAR", "OBRA_SOCIAL" o "MIXTO" (nunca null: Particular es el default).
     */
    public String getCoberturaSeleccionada() {
        return coberturaSeleccionada;
    }

    /**
     * Obra social elegida (con id, para poder guardarla), o null si no hay ninguna seleccionada
     * -- válido tanto si la cobertura es Particular como si todavía no se eligió ninguna.
     */
    public ObraSocial getObraSocialSeleccionada() {
        return comboObraSocial.getValue();
    }

    public String getPlan() {
        return campoPlan.getText();
    }

    public String getNroAfiliado() {
        return campoNroAfiliado.getText();
    }

    public String getMetodoPago() {
        return comboMetodoPago.getValue();
    }

    /**
     * Monto en efectivo tipeado, o null si el campo está vacío o no es un número válido.
     */
    public BigDecimal getMontoEfectivo() {
        String texto = campoMontoEfectivo.getText();
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void cargarObrasSociales() {
        List<ObraSocial> activas = PuenteEDT.ejecutar(
                () -> controlador.ObraSocialController.listarTodas(null).stream()
                        .filter(ObraSocial::isActiva)
                        .collect(Collectors.toList()),
                Collections.emptyList());
        comboObraSocial.getItems().setAll(activas);
    }

    private void cargarMetodosPago() {
        List<String> nombres = PuenteEDT.ejecutar(
                () -> controlador.MetodoPagoController.listarNombres(null),
                Collections.emptyList());
        comboMetodoPago.getItems().setAll(nombres);
        if (!nombres.isEmpty()) {
            comboMetodoPago.setValue(nombres.get(0));
        }
    }

    /**
     * Se llama después de generar una orden con éxito: vuelve a Particular y vacía todos los
     * campos de cobertura, para dejar la pantalla lista para la próxima orden.
     */
    public void limpiarSeleccion() {
        comboObraSocial.setValue(null);
        campoPlan.setText("");
        campoNroAfiliado.setText("");
        campoMontoEfectivo.setText("");
        if (!comboMetodoPago.getItems().isEmpty()) {
            comboMetodoPago.setValue(comboMetodoPago.getItems().get(0));
        }
        seleccionarCobertura(PARTICULAR);
    }
}
