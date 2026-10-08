package vistas.javafx.obrasociales;

import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import modelo.ObraSocial;
import vistas.javafx.EstiloApp;
import vistas.javafx.PuenteEDT;

/**
 * Formulario modal de Alta/Edición de una obra social, abierto desde {@link
 * ObrasSocialesScreen}. Si {@code obraSocial} es null es un Alta; si no, se edita esa instancia.
 *
 * <p>Los largos máximos de acá abajo ({@link #LARGO_NOMBRE} y compañía) coinciden exactamente con
 * el {@code VARCHAR} de cada columna real de `obras_sociales` (verificado con {@code SHOW CREATE
 * TABLE} el 2026-10-01), para que nunca se le mande a MySQL un valor más largo de lo que la
 * columna puede guardar -- eso tiraba antes un error de SQL en crudo, feo y sin explicación, en
 * vez de avisar acá mismo qué está mal. Si en algún momento se achica o agranda alguna columna,
 * hay que actualizar el número de acá para que siga coincidiendo.</p>
 */
public class ObraSocialFormDialog {

    private static final int LARGO_NOMBRE = 100;
    private static final int LARGO_CODIGO = 20;
    private static final int LARGO_CUIT = 13; // 11 dígitos + 2 guiones (formato 20-12345678-9) -- la columna admite hasta 20
    private static final int LARGO_TELEFONO = 25; // la columna admite hasta 30; se deja margen para separadores
    private static final int LARGO_EMAIL = 100; // la columna admite hasta 120; se deja margen
    private static final int LARGO_DIRECCION = 200;

    /**
     * Mismo criterio que {@code vistas.javafx.login.PedirEmailPanel.PATRON_EMAIL}: una
     * validación de formato básica (no exhaustiva), sólo para atajar un typo evidente.
     */
    private static final Pattern PATRON_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CARACTERES_CUIT = Pattern.compile("[0-9-]*");
    private static final Pattern CARACTERES_TELEFONO = Pattern.compile("[0-9 ()+-]*");

    private final ObraSocial obraSocial;
    private final Runnable alGuardar;

    private final TextField campoNombre = new TextField();
    private final TextField campoCodigo = new TextField();
    private final TextField campoCuit = new TextField();
    private final TextField campoTelefono = new TextField();
    private final TextField campoEmail = new TextField();
    private final TextField campoDireccion = new TextField();
    private final CheckBox campoActiva = new CheckBox("Activa");

    /**
     * @param obraSocial null para un Alta, o la obra social a editar.
     * @param alGuardar se ejecuta después de guardar con éxito, para que quien abrió el
     * formulario (la pantalla de listado) pueda refrescar su tabla.
     */
    public ObraSocialFormDialog(ObraSocial obraSocial, Runnable alGuardar) {
        this.obraSocial = obraSocial;
        this.alGuardar = alGuardar;
    }

    public void mostrar() {
        boolean esEdicion = obraSocial != null;

        Stage ventana = new Stage(StageStyle.UTILITY);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(esEdicion ? "Editar Obra Social" : "Nueva Obra Social");
        ventana.setResizable(false);

        VBox raiz = new VBox(18);
        raiz.getStyleClass().add("pantalla");
        raiz.setPadding(new Insets(24));

        Label titulo = new Label(esEdicion ? "Editar Obra Social" : "Nueva Obra Social");
        titulo.getStyleClass().add("titulo-pantalla");

        GridPane grilla = armarGrilla();
        precargarSiCorresponde();

        HBox botones = armarBotones(ventana);

        raiz.getChildren().addAll(titulo, grilla, botones);

        Scene escena = new Scene(raiz, 460, esEdicion ? 480 : 440);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        vistas.javafx.VentanaUtil.animarAparicion(ventana, raiz);
        ventana.showAndWait();
    }

    private GridPane armarGrilla() {
        GridPane grilla = new GridPane();
        grilla.setHgap(14);
        grilla.setVgap(12);

        for (TextField campo : new TextField[]{campoNombre, campoCodigo, campoCuit, campoTelefono, campoEmail, campoDireccion}) {
            campo.getStyleClass().add("campo-form");
            campo.setPrefWidth(260);
        }

        limitarLargo(campoNombre, LARGO_NOMBRE);
        limitarLargo(campoCodigo, LARGO_CODIGO);
        limitarCaracteres(campoCuit, CARACTERES_CUIT, LARGO_CUIT);
        campoCuit.setPromptText("20-12345678-9");
        limitarCaracteres(campoTelefono, CARACTERES_TELEFONO, LARGO_TELEFONO);
        limitarLargo(campoEmail, LARGO_EMAIL);
        limitarLargo(campoDireccion, LARGO_DIRECCION);

        int fila = 0;
        fila = agregarCampo(grilla, fila, "Nombre *", campoNombre);
        fila = agregarCampo(grilla, fila, "Código interno", campoCodigo);
        fila = agregarCampo(grilla, fila, "CUIT", campoCuit);
        fila = agregarCampo(grilla, fila, "Teléfono", campoTelefono);
        fila = agregarCampo(grilla, fila, "Email", campoEmail);
        fila = agregarCampo(grilla, fila, "Dirección", campoDireccion);

        if (obraSocial != null) {
            grilla.add(campoActiva, 1, fila);
        }

        return grilla;
    }

    private int agregarCampo(GridPane grilla, int fila, String etiquetaTexto, TextField campo) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        grilla.add(etiqueta, 0, fila);
        grilla.add(campo, 1, fila);
        return fila + 1;
    }

    private void precargarSiCorresponde() {
        if (obraSocial == null) {
            campoActiva.setSelected(true);
            return;
        }
        campoNombre.setText(obraSocial.getNombre());
        campoCodigo.setText(obraSocial.getCodigoInterno());
        campoCuit.setText(obraSocial.getCuit());
        campoTelefono.setText(obraSocial.getTelefono());
        campoEmail.setText(obraSocial.getEmail());
        campoDireccion.setText(obraSocial.getDireccion());
        campoActiva.setSelected(obraSocial.isActiva());
    }

    private HBox armarBotones(Stage ventana) {
        Button botonCancelar = new Button("Cancelar");
        botonCancelar.getStyleClass().add("boton-secundario");
        botonCancelar.setOnAction(evt -> ventana.close());

        Button botonGuardar = new Button("Guardar");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar(ventana));

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        HBox fila = new HBox(12, espaciador, botonCancelar, botonGuardar);
        fila.setAlignment(Pos.CENTER_RIGHT);
        return fila;
    }

    private void guardar(Stage ventana) {
        String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "El nombre es obligatorio.");
            return;
        }

        String cuit = vacioANull(campoCuit.getText());
        if (cuit != null && !esCuitValido(cuit)) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "El CUIT debe tener 11 dígitos (por ejemplo, 20-12345678-9). "
                    + "Dejá el campo vacío si todavía no lo tenés.");
            return;
        }

        String email = vacioANull(campoEmail.getText());
        if (email != null && !PATRON_EMAIL.matcher(email).matches()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "El email no tiene un formato válido (por ejemplo, contacto@obrasocial.com.ar).");
            return;
        }

        ObraSocial datos = new ObraSocial();
        datos.setNombre(nombre);
        datos.setCodigoInterno(vacioANull(campoCodigo.getText()));
        datos.setCuit(cuit);
        datos.setTelefono(vacioANull(campoTelefono.getText()));
        datos.setEmail(email);
        datos.setDireccion(vacioANull(campoDireccion.getText()));

        boolean ok;
        if (obraSocial == null) {
            datos.setActiva(true);
            Integer id = PuenteEDT.ejecutar(
                    () -> controlador.ObraSocialController.crear(null, datos),
                    null);
            ok = id != null;
        } else {
            datos.setIdObraSocial(obraSocial.getIdObraSocial());
            datos.setActiva(campoActiva.isSelected());
            ok = PuenteEDT.ejecutar(
                    () -> controlador.ObraSocialController.actualizar(null, datos),
                    false);
        }

        if (ok) {
            if (alGuardar != null) {
                alGuardar.run();
            }
            ventana.close();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar la obra social.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setHeaderText(null);
        vistas.javafx.AlertaUtil.estilizar(alerta);
        alerta.showAndWait();
    }

    private static String vacioANull(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    /**
     * Impide escribir (o pegar) más caracteres de los que la columna puede guardar, en vez de
     * dejar que se escriba cualquier cosa y recién enterarse del problema al guardar.
     */
    private static void limitarLargo(TextField campo, int largoMaximo) {
        campo.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= largoMaximo ? cambio : null));
    }

    /**
     * Igual que {@link #limitarLargo}, pero además sólo deja escribir los caracteres permitidos
     * por {@code permitidos} (por ejemplo, sólo dígitos y guiones para el CUIT) -- así ni
     * siquiera hace falta esperar a guardar para darse cuenta de que se tipeó una letra de más.
     */
    private static void limitarCaracteres(TextField campo, Pattern permitidos, int largoMaximo) {
        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            String nuevoTexto = cambio.getControlNewText();
            if (nuevoTexto.length() > largoMaximo || !permitidos.matcher(nuevoTexto).matches()) {
                return null;
            }
            return cambio;
        }));
    }

    /**
     * Un CUIT argentino son 11 dígitos (con o sin los guiones del formato XX-XXXXXXXX-X); acá
     * sólo se valida la cantidad de dígitos, no el dígito verificador.
     */
    private static boolean esCuitValido(String cuit) {
        String soloDigitos = cuit.replaceAll("[^0-9]", "");
        return soloDigitos.length() == 11;
    }
}
