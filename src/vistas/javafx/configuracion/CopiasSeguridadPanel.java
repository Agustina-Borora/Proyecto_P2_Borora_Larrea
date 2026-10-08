package vistas.javafx.configuracion;

import controlador.CopiasSeguridad;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import utilidades.PreferenciasSistema;
import vistas.javafx.TareaFondo;
import vistas.javafx.ToastUtil;

/**
 * Tarjeta "Copias de seguridad" de Configuración &gt; Sistema: copia automática diaria a la hora
 * elegida, en la carpeta elegida (puede ser una de Google Drive/OneDrive para que además se suba
 * a la nube), "Hacer una copia ahora" y "Restaurar desde una copia". Los cambios se aplican en el
 * momento (no hay que apretar "Guardar").
 */
final class CopiasSeguridadPanel {

    private final Label lblCarpeta = new Label();
    private final Label lblEstado = new Label();

    Parent construir() {
        Label titulo = new Label("Copias de seguridad de la base de datos");
        titulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        Label descripcion = new Label("Una copia COMPLETA de todo (pacientes, órdenes, resultados, catálogo, pagos, "
                + "usuarios...) en un archivo .sql, que queda guardado en esta PC. Si algún día el sistema o la base "
                + "fallan, desde acá se vuelve todo a como estaba. Si elegís una carpeta de Google Drive u OneDrive, "
                + "además se sube sola a la nube.");
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-font-size: 13px; -fx-text-fill: #5F6966;");

        // --- automática
        CheckBox chkAuto = new CheckBox("Hacer una copia automática todos los días");
        chkAuto.setSelected(PreferenciasSistema.isRespaldoAutomatico());
        chkAuto.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        ComboBox<String> comboHora = new ComboBox<>();
        for (int h = 6; h <= 23; h++) {
            comboHora.getItems().add(String.format("%02d:00", h));
            comboHora.getItems().add(String.format("%02d:30", h));
        }
        String horaActual = PreferenciasSistema.getHoraRespaldo();
        if (!comboHora.getItems().contains(horaActual)) {
            comboHora.getItems().add(0, horaActual);
        }
        comboHora.setValue(horaActual);
        comboHora.setVisibleRowCount(10);
        Label lblHora = etiqueta("a las");
        Label lblHoraAyuda = new Label("(si a esa hora la PC está apagada, se hace apenas se abra el sistema)");
        lblHoraAyuda.getStyleClass().add("ayuda-diseno");
        lblHoraAyuda.setWrapText(true);
        HBox filaHora = new HBox(10, lblHora, comboHora);
        filaHora.setAlignment(Pos.CENTER_LEFT);

        // Cuántos días hacia atrás se guardan las copias automáticas (se hace una por día, así que
        // 30 días = las últimas 30 copias). Las más viejas se borran solas para no llenar el disco.
        // Las copias hechas a mano no se borran nunca.
        javafx.scene.control.Spinner<Integer> spinDias = new javafx.scene.control.Spinner<>(1, 365,
                PreferenciasSistema.getRespaldosAConservar(), 1);
        spinDias.setEditable(true);
        spinDias.setPrefWidth(90);
        spinDias.valueProperty().addListener((o, a, n) -> {
            if (n != null) {
                PreferenciasSistema.setRespaldosAConservar(n);
            }
        });
        // Si escribe el número a mano y sale del campo sin apretar Enter, igual se toma.
        spinDias.focusedProperty().addListener((o, antes, ahora) -> {
            if (!ahora) {
                try {
                    int n = Integer.parseInt(spinDias.getEditor().getText().trim());
                    spinDias.getValueFactory().setValue(Math.max(1, Math.min(365, n)));
                } catch (NumberFormatException e) {
                    spinDias.getEditor().setText(String.valueOf(spinDias.getValue()));
                }
            }
        });
        HBox filaConservar = new HBox(10, etiqueta("Guardar las copias de los últimos"), spinDias, etiqueta("días"));
        filaConservar.setAlignment(Pos.CENTER_LEFT);
        Label ayudaConservar = new Label("Se hace una copia por día. Las que tienen más días que eso se borran solas, "
                + "para no llenar el disco (las que hagas a mano con el botón de abajo no se borran nunca).");
        ayudaConservar.getStyleClass().add("ayuda-diseno");
        ayudaConservar.setWrapText(true);

        Runnable habilitar = () -> {
            filaHora.setDisable(!chkAuto.isSelected());
            filaConservar.setDisable(!chkAuto.isSelected());
            ayudaConservar.setDisable(!chkAuto.isSelected());
        };
        habilitar.run();
        chkAuto.setOnAction(e -> {
            PreferenciasSistema.setRespaldoAutomatico(chkAuto.isSelected());
            habilitar.run();
            actualizarEstado();
        });
        comboHora.valueProperty().addListener((o, a, n) -> {
            if (n != null && n.matches("\\d{1,2}:\\d{2}")) {
                PreferenciasSistema.setHoraRespaldo(n);
                actualizarEstado();
            }
        });
        // --- carpeta
        lblCarpeta.setWrapText(true);
        lblCarpeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #3A4440; -fx-font-weight: bold;");
        lblCarpeta.setText(CopiasSeguridad.carpeta().getAbsolutePath());
        Button cambiarCarpeta = new Button("Cambiar carpeta...");
        cambiarCarpeta.getStyleClass().add("boton-secundario");
        cambiarCarpeta.setOnAction(e -> {
            File elegida = vistas.javafx.SelectorArchivoFx.elegirCarpeta(cambiarCarpeta, "Carpeta para las copias de seguridad");
            if (elegida != null) {
                usarCarpeta(elegida);
            }
        });
        Button abrirCarpeta = new Button("Abrir carpeta");
        abrirCarpeta.getStyleClass().add("enlace-accion");
        abrirCarpeta.setOnAction(e -> TareaFondo.ejecutar(() -> {
            File carpeta = CopiasSeguridad.carpeta();
            carpeta.mkdirs();
            utilidades.ArchivosUtil.mostrarEnCarpeta(carpeta);
            return null;
        }, null, error -> ToastUtil.error("No se pudo abrir la carpeta")));
        FlowPane filaCarpeta = new FlowPane(10, 8, cambiarCarpeta, abrirCarpeta);
        filaCarpeta.setAlignment(Pos.CENTER_LEFT);
        // Atajos a las carpetas de la nube que haya en esta PC.
        for (File nube : CopiasSeguridad.carpetasEnLaNube()) {
            Button usar = new Button("☁ Usar " + nombreNube(nube));
            usar.getStyleClass().add("enlace-accion");
            usar.setOnAction(e -> usarCarpeta(new File(nube, "Laboratorio - Copias de seguridad")));
            filaCarpeta.getChildren().add(usar);
        }

        // --- acciones
        lblEstado.setWrapText(true);
        lblEstado.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #237A4E;");
        actualizarEstado();

        Button copiarAhora = new Button("Hacer una copia ahora");
        copiarAhora.getStyleClass().add("boton-primario");
        copiarAhora.setOnAction(e -> {
            copiarAhora.setDisable(true);
            copiarAhora.setText("Copiando...");
            TareaFondo.ejecutar(() -> CopiasSeguridad.hacerCopia("manual"), archivo -> {
                copiarAhora.setDisable(false);
                copiarAhora.setText("Hacer una copia ahora");
                actualizarEstado();
                vistas.javafx.AvisoRapido.mostrar("Copia de seguridad guardada", archivo.getName());
            }, error -> {
                copiarAhora.setDisable(false);
                copiarAhora.setText("Hacer una copia ahora");
                ToastUtil.error("No se pudo hacer la copia: " + TareaFondo.mensaje(error));
            });
        });
        Button restaurar = new Button("Restaurar desde una copia...");
        restaurar.getStyleClass().add("boton-secundario");
        restaurar.setOnAction(e -> {
            new RestaurarCopiaDialog().mostrar(restaurar.getScene().getWindow());
            actualizarEstado();
        });
        HBox filaAcciones = new HBox(10, copiarAhora, restaurar);
        filaAcciones.setAlignment(Pos.CENTER_LEFT);

        VBox contenido = new VBox(12, titulo, descripcion, chkAuto, filaHora, lblHoraAyuda, filaConservar, ayudaConservar,
                etiqueta("Carpeta donde se guardan:"), lblCarpeta, filaCarpeta, lblEstado, filaAcciones);
        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        return caja;
    }

    private void usarCarpeta(File carpeta) {
        // Si en la carpeta vieja había copias, se quedan ahí (y se siguen pudiendo restaurar
        // eligiendo el archivo); las nuevas van a la nueva.
        PreferenciasSistema.setCarpetaRespaldo(carpeta.getAbsolutePath());
        lblCarpeta.setText(carpeta.getAbsolutePath());
        actualizarEstado();
        ToastUtil.exito("Las copias se van a guardar en la carpeta nueva");
    }

    private void actualizarEstado() {
        List<File> copias = CopiasSeguridad.listar();
        List<String> partes = new ArrayList<>();
        if (copias.isEmpty()) {
            partes.add("Todavía no hay ninguna copia en esta carpeta.");
        } else {
            partes.add("Última copia: " + RestaurarCopiaDialog.describir(copias.get(0)) + "  ·  " + copias.size()
                    + (copias.size() == 1 ? " copia guardada" : " copias guardadas"));
        }
        if (PreferenciasSistema.isRespaldoAutomatico()) {
            long proximo = CopiasSeguridad.proximoMomento();
            partes.add(proximo <= System.currentTimeMillis() + 90_000 ? "Próxima copia automática: en un momento"
                    : "Próxima copia automática: " + cuando(proximo));
        } else {
            partes.add("La copia automática está apagada.");
        }
        lblEstado.setText(String.join("\n", partes));
    }

    private static String cuando(long momento) {
        Calendar hoy = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(momento);
        String hora = new SimpleDateFormat("HH:mm").format(new Date(momento));
        if (c.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == hoy.get(Calendar.YEAR)) {
            return "hoy a las " + hora;
        }
        hoy.add(Calendar.DAY_OF_MONTH, 1);
        if (c.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == hoy.get(Calendar.YEAR)) {
            return "mañana a las " + hora;
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(momento));
    }

    private static String nombreNube(File carpeta) {
        String n = carpeta.getName();
        if (n.startsWith("OneDrive")) {
            return "OneDrive";
        }
        if (n.equals("Dropbox")) {
            return "Dropbox";
        }
        return "Google Drive";
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
        return l;
    }
}
