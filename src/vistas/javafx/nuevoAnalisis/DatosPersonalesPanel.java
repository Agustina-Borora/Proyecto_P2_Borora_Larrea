package vistas.javafx.nuevoAnalisis;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.Paciente;
import modelo.Sexo;
import vistas.javafx.PuenteEDT;

/**
 * Sub-panel de la pantalla "Nuevo Análisis" con los datos del paciente (nuevo o existente) --
 * versión JavaFX de {@link vistas.nuevoAnalisis.DatosPersonales}, con la misma lógica (bloqueo de
 * campos hasta completar el DNI, búsqueda automática, alta de médico nuevo) pero con una grilla
 * de 3 columnas por porcentaje en vez de anchos fijos en píxeles, para que se acomode sola al
 * ancho de la ventana.
 */
public class DatosPersonalesPanel {

    private static final int LARGO_DNI = 8;
    private static final int LARGO_MAX_CELULAR = 15;

    private final TextField campoDni = new TextField();
    private final TextField campoApellidoYNombre = new TextField();
    private final TextField campoCelular = new TextField();
    private final TextField campoEmail = new TextField();
    private final DatePicker pickerFecha = new DatePicker();
    private final ComboBox<Sexo> comboSexo = new ComboBox<>();
    private final ComboBox<String> comboMedico = new ComboBox<>();
    private final TextField campoObservacion = new TextField();

    private final VBox bloqueApellidoYNombre = new VBox(6);
    private final VBox bloqueCelular = new VBox(6);
    private final VBox bloqueEmail = new VBox(6);
    private final VBox bloqueFecha = new VBox(6);
    private final VBox bloqueSexo = new VBox(6);

    /** id_paciente si el DNI tipeado corresponde a un paciente ya registrado; null si es nuevo. */
    private Integer idPacienteExistente;

    /** Snapshot del paciente tal como estaba en la base al encontrarlo por DNI (null si es nuevo). */
    private Paciente pacienteOriginal;

    public Parent construir() {
        VBox raiz = new VBox(16);
        raiz.getStyleClass().add("tarjeta");

        Label titulo = new Label("Datos del Paciente");
        titulo.getStyleClass().add("titulo-seccion");

        GridPane grilla = armarGrilla();

        raiz.getChildren().addAll(titulo, grilla);

        cargarSexos();
        cargarMedicos();
        bloquearCamposPaciente();

        return raiz;
    }

    private GridPane armarGrilla() {
        GridPane grilla = new GridPane();
        grilla.setHgap(20);
        grilla.setVgap(12);

        for (int i = 0; i < 3; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / 3);
            columna.setHgrow(Priority.ALWAYS);
            grilla.getColumnConstraints().add(columna);
        }

        for (TextField campo : new TextField[]{campoDni, campoApellidoYNombre, campoCelular, campoEmail, campoObservacion}) {
            campo.getStyleClass().add("campo-form");
            campo.setMaxWidth(Double.MAX_VALUE);
        }
        comboSexo.getStyleClass().add("campo-form");
        comboMedico.getStyleClass().add("campo-form");
        pickerFecha.getStyleClass().add("campo-form");
        comboSexo.setMaxWidth(Double.MAX_VALUE);
        comboMedico.setMaxWidth(Double.MAX_VALUE);
        pickerFecha.setMaxWidth(Double.MAX_VALUE);
        pickerFecha.setPromptText("dd/mm/aaaa");

        campoDni.setPromptText("8 dígitos");
        campoDni.setOnKeyReleased(evt -> alCambiarDni());

        campoApellidoYNombre.setOnKeyReleased(evt -> filtrarSoloLetras(campoApellidoYNombre));
        campoCelular.setOnKeyReleased(evt -> filtrarSoloDigitos(campoCelular, LARGO_MAX_CELULAR));

        Button botonAgregarMedico = new Button("+");
        botonAgregarMedico.getStyleClass().add("boton-secundario");
        botonAgregarMedico.setOnAction(evt -> agregarMedicoNuevo());
        HBox filaMedico = new HBox(8, comboMedico, botonAgregarMedico);
        HBox.setHgrow(comboMedico, Priority.ALWAYS);

        int fila = 0;
        grilla.add(bloqueCampo("DNI", campoDni), 0, fila);
        grilla.add(bloqueCampo("Apellido y Nombre", campoApellidoYNombre, bloqueApellidoYNombre), 1, fila);
        grilla.add(bloqueCampo("Celular", campoCelular, bloqueCelular), 2, fila);
        fila++;

        grilla.add(bloqueCampo("Sexo", comboSexo, bloqueSexo), 0, fila);
        grilla.add(bloqueCampo("Email", campoEmail, bloqueEmail), 1, fila);
        grilla.add(bloqueCampo("Fecha de Nacimiento", pickerFecha, bloqueFecha), 2, fila);
        fila++;

        Label etiquetaMedico = new Label("Médico Derivante (Opcional)");
        etiquetaMedico.getStyleClass().add("etiqueta-campo");
        VBox bloqueMedico = new VBox(6, etiquetaMedico, filaMedico);
        grilla.add(bloqueMedico, 0, fila, 1, 1);
        grilla.add(bloqueCampo("Observación (Opcional)", campoObservacion), 1, fila, 2, 1);

        return grilla;
    }

    private VBox bloqueCampo(String etiquetaTexto, javafx.scene.control.Control campo) {
        return bloqueCampo(etiquetaTexto, campo, new VBox(6));
    }

    private VBox bloqueCampo(String etiquetaTexto, javafx.scene.control.Control campo, VBox contenedor) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        contenedor.getChildren().setAll(etiqueta, campo);
        return contenedor;
    }

    private static void filtrarSoloDigitos(TextField campo, int largoMax) {
        String texto = campo.getText();
        if (texto == null) {
            return;
        }
        String limpio = texto.replaceAll("[^0-9]", "");
        if (limpio.length() > largoMax) {
            limpio = limpio.substring(0, largoMax);
        }
        if (!limpio.equals(texto)) {
            campo.setText(limpio);
        }
    }

    private static void filtrarSoloLetras(TextField campo) {
        String texto = campo.getText();
        if (texto == null) {
            return;
        }
        String limpio = texto.replaceAll("[^\\p{L} ]", "");
        if (!limpio.equals(texto)) {
            campo.setText(limpio);
        }
    }

    private List<Sexo> listaSexos = new ArrayList<>();

    private void cargarSexos() {
        listaSexos = PuenteEDT.ejecutar(
                () -> controlador.SexoController.listarTodos(null),
                Collections.emptyList());
        comboSexo.getItems().setAll(listaSexos);
    }

    private void seleccionarSexoPorId(int idSexoBuscado) {
        for (Sexo s : listaSexos) {
            if (s.getIdSexo() == idSexoBuscado) {
                comboSexo.setValue(s);
                return;
            }
        }
    }

    private void cargarMedicos() {
        List<String> nombres = PuenteEDT.ejecutar(
                () -> controlador.MedicoController.listarNombres(null),
                Collections.emptyList());
        comboMedico.getItems().setAll("");
        comboMedico.getItems().addAll(nombres.toArray(new String[0]));
        comboMedico.setValue("");
    }

    /**
     * Botón "+" junto al combo de médicos: pide un nombre y, si no está ya en la lista (sin
     * importar mayúsculas/minúsculas), lo agrega y lo deja seleccionado. El médico recién se crea
     * de verdad en la base al generar la orden, vía {@code MedicoDAO.obtenerOCrear}.
     */
    private void agregarMedicoNuevo() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Nuevo Médico");
        dialogo.setHeaderText(null);
        dialogo.setContentText("Nombre del médico derivante:");
        Optional<String> resultado = dialogo.showAndWait();
        if (!resultado.isPresent()) {
            return;
        }
        String nombre = resultado.get().trim();
        if (nombre.isEmpty()) {
            return;
        }
        for (String existente : comboMedico.getItems()) {
            if (nombre.equalsIgnoreCase(existente)) {
                comboMedico.setValue(existente);
                return;
            }
        }
        comboMedico.getItems().add(nombre);
        comboMedico.setValue(nombre);
    }

    private void alCambiarDni() {
        filtrarSoloDigitos(campoDni, LARGO_DNI);
        String texto = campoDni.getText();
        if (texto.length() == LARGO_DNI) {
            buscarPacientePorDni(texto);
        } else {
            bloquearCamposPaciente();
        }
    }

    private void limpiarCamposPaciente() {
        idPacienteExistente = null;
        pacienteOriginal = null;
        campoApellidoYNombre.setText("");
        campoEmail.setText("");
        campoCelular.setText("");
        pickerFecha.setValue(null);
        if (!comboSexo.getItems().isEmpty()) {
            comboSexo.setValue(comboSexo.getItems().get(0));
        }
    }

    private void bloquearCamposPaciente() {
        limpiarCamposPaciente();
        campoApellidoYNombre.setDisable(true);
        campoEmail.setDisable(true);
        campoCelular.setDisable(true);
        pickerFecha.setDisable(true);
        comboSexo.setDisable(true);
    }

    private void desbloquearCamposPaciente() {
        campoApellidoYNombre.setDisable(false);
        campoEmail.setDisable(false);
        campoCelular.setDisable(false);
        pickerFecha.setDisable(false);
        comboSexo.setDisable(false);
    }

    private void buscarPacientePorDni(String dniTexto) {
        Paciente encontrado = PuenteEDT.ejecutar(
                () -> controlador.PacienteController.buscarPorDni(null, dniTexto),
                null);

        desbloquearCamposPaciente();

        if (encontrado != null) {
            idPacienteExistente = encontrado.getIdPaciente();
            pacienteOriginal = encontrado;
            campoApellidoYNombre.setText(encontrado.getNyaPaciente());
            campoEmail.setText(encontrado.getEmail());
            campoCelular.setText(encontrado.getTelefono());
            pickerFecha.setValue(fechaALocalDate(encontrado.getFechaNacimiento()));
            seleccionarSexoPorId(encontrado.getIdSexo());
        } else {
            limpiarCamposPaciente();
        }
    }

    public boolean esPacienteExistente() {
        return idPacienteExistente != null;
    }

    public Integer getIdPacienteExistente() {
        return idPacienteExistente;
    }

    public String getDniPaciente() {
        return campoDni.getText();
    }

    public String getApellidoYNombre() {
        return campoApellidoYNombre.getText();
    }

    public java.util.Date getFechaNacimientoPaciente() {
        return localDateAFecha(pickerFecha.getValue());
    }

    public String getEmailPaciente() {
        return campoEmail.getText();
    }

    public String getCelularPaciente() {
        return campoCelular.getText();
    }

    public String getMedicoDerivante() {
        String seleccionado = comboMedico.getValue();
        return seleccionado != null ? seleccionado : "";
    }

    public String getObservacionPedido() {
        return campoObservacion.getText();
    }

    public Integer getIdSexoSeleccionado() {
        Sexo s = comboSexo.getValue();
        return s == null ? null : s.getIdSexo();
    }

    public boolean datosPacienteCambiaron() {
        if (pacienteOriginal == null) {
            return false;
        }
        if (!normalizarTexto(campoApellidoYNombre.getText()).equals(normalizarTexto(pacienteOriginal.getNyaPaciente()))) {
            return true;
        }
        if (!normalizarTexto(campoEmail.getText()).equals(normalizarTexto(pacienteOriginal.getEmail()))) {
            return true;
        }
        if (!normalizarTexto(campoCelular.getText()).equals(normalizarTexto(pacienteOriginal.getTelefono()))) {
            return true;
        }
        if (!mismaFecha(pickerFecha.getValue(), fechaALocalDate(pacienteOriginal.getFechaNacimiento()))) {
            return true;
        }
        Integer idSexoActual = getIdSexoSeleccionado();
        return idSexoActual == null || idSexoActual != pacienteOriginal.getIdSexo();
    }

    private String normalizarTexto(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean mismaFecha(LocalDate a, LocalDate b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b);
    }

    public Paciente construirPaciente() {
        Paciente p = new Paciente();
        if (idPacienteExistente != null) {
            p.setIdPaciente(idPacienteExistente);
        }
        p.setNyaPaciente(campoApellidoYNombre.getText().trim());
        p.setDni(campoDni.getText().trim());
        p.setFechaNacimiento(getFechaNacimientoPaciente());
        Integer idSexo = getIdSexoSeleccionado();
        p.setIdSexo(idSexo != null ? idSexo : 0);
        p.setTelefono(campoCelular.getText().trim());
        p.setEmail(campoEmail.getText().trim());
        return p;
    }

    /**
     * Se llama después de generar una orden con éxito: vacía el DNI (y, con él, bloquea/limpia el
     * resto de los datos del paciente) y los dos campos que no dependen del DNI.
     */
    public void limpiarFormulario() {
        campoDni.setText("");
        bloquearCamposPaciente();
        if (!comboMedico.getItems().isEmpty()) {
            comboMedico.setValue(comboMedico.getItems().get(0));
        }
        campoObservacion.setText("");
    }

    private static LocalDate fechaALocalDate(java.util.Date fecha) {
        return fecha == null ? null : new Date(fecha.getTime()).toLocalDate();
    }

    private static java.util.Date localDateAFecha(LocalDate fecha) {
        return fecha == null ? null : Date.valueOf(fecha);
    }
}
