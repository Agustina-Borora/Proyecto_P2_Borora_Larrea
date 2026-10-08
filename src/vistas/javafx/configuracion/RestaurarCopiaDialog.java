package vistas.javafx.configuracion;

import controlador.CopiasSeguridad;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import vistas.javafx.EstiloApp;
import vistas.javafx.TareaFondo;

/**
 * Ventana para volver la base de datos a como estaba en una copia de seguridad: muestra las
 * copias de la carpeta (la más nueva arriba), o deja elegir cualquier otro archivo .sql (por
 * ejemplo uno hecho con Workbench o traído en un pendrive). Antes de restaurar SIEMPRE hace una
 * copia de cómo está todo en ese momento, por si se eligió la copia equivocada.
 */
final class RestaurarCopiaDialog {

    private File elegido;

    void mostrar(Window duena) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        if (duena != null) {
            ventana.initOwner(duena);
        }
        ventana.setTitle("Restaurar desde una copia de seguridad");

        Label titulo = new Label("Restaurar desde una copia");
        titulo.getStyleClass().add("titulo-pantalla");
        Label explicacion = new Label("Elegí la copia a la que querés volver. Todo lo que se haya cargado DESPUÉS de "
                + "esa copia se reemplaza por lo que tenía la copia. Por las dudas, antes de restaurar el sistema "
                + "guarda una copia de cómo está todo ahora.");
        explicacion.setWrapText(true);
        explicacion.setStyle("-fx-font-size: 13px; -fx-text-fill: #3A4440;");

        ListView<File> lista = new ListView<>();
        lista.getItems().addAll(CopiasSeguridad.listar());
        lista.setPrefHeight(260);
        lista.setPlaceholder(new Label("No hay copias en la carpeta. Usá \"Elegir otro archivo...\"."));
        lista.setCellFactory(v -> new ListCell<File>() {
            @Override
            protected void updateItem(File f, boolean vacio) {
                super.updateItem(f, vacio);
                setText(vacio || f == null ? null : describir(f) + "   ·   " + tamano(f));
            }
        });

        Label lblElegido = new Label();
        lblElegido.setWrapText(true);
        lblElegido.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #141C19; -fx-font-weight: bold;");
        lista.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            elegido = n;
            lblElegido.setText(n == null ? "" : "Elegida: " + n.getName());
        });

        Button otro = new Button("Elegir otro archivo...");
        otro.getStyleClass().add("enlace-accion");
        otro.setOnAction(e -> {
            File f = vistas.javafx.SelectorArchivoFx.elegirArchivoSql(otro, CopiasSeguridad.carpeta());
            if (f != null) {
                lista.getSelectionModel().clearSelection();
                elegido = f;
                lblElegido.setText("Elegido: " + f.getAbsolutePath());
            }
        });

        ProgressIndicator girando = new ProgressIndicator();
        girando.setMaxSize(26, 26);
        girando.setVisible(false);
        Label lblProgreso = new Label();
        lblProgreso.setWrapText(true);
        lblProgreso.setStyle("-fx-font-size: 13px;");

        Button restaurar = new Button("Restaurar esta copia");
        restaurar.getStyleClass().add("boton-primario");
        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("boton-secundario");
        cerrar.setOnAction(e -> ventana.close());

        restaurar.setOnAction(e -> {
            if (elegido == null) {
                lblProgreso.setStyle("-fx-font-size: 13px; -fx-text-fill: #B45309;");
                lblProgreso.setText("Primero elegí una copia de la lista (o \"Elegir otro archivo...\").");
                return;
            }
            if (!confirmar(ventana, elegido)) {
                return;
            }
            File archivo = elegido;
            restaurar.setDisable(true);
            cerrar.setDisable(true);
            lista.setDisable(true);
            otro.setDisable(true);
            girando.setVisible(true);
            lblProgreso.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
            lblProgreso.setText("Restaurando... no cierres el sistema. Puede tardar unos segundos.");
            ventana.setOnCloseRequest(ev -> ev.consume()); // no se puede cerrar a mitad de camino
            TareaFondo.ejecutar(() -> CopiasSeguridad.restaurar(archivo), antes -> {
                girando.setVisible(false);
                cerrar.setDisable(false);
                ventana.setOnCloseRequest(null);
                lblProgreso.setStyle("-fx-font-size: 13px; -fx-text-fill: #237A4E; -fx-font-weight: bold;");
                lblProgreso.setText("✓ Listo: los datos quedaron como en la copia elegida.\n"
                        + "Cómo estaba todo antes quedó guardado en \"" + antes.getName() + "\" (por si hace falta volver).\n"
                        + "Conviene cerrar el sistema y volver a abrirlo para que todas las pantallas muestren lo restaurado.");
            }, error -> {
                girando.setVisible(false);
                cerrar.setDisable(false);
                restaurar.setDisable(false);
                lista.setDisable(false);
                otro.setDisable(false);
                ventana.setOnCloseRequest(null);
                lblProgreso.setStyle("-fx-font-size: 13px; -fx-text-fill: #B91C1C;");
                lblProgreso.setText("No se pudo restaurar: " + TareaFondo.mensaje(error)
                        + "\nSi algo quedó a medias, restaurá la copia \"antes-de-restaurar\" más nueva de la lista.");
                lista.getItems().setAll(CopiasSeguridad.listar());
            });
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox botones = new HBox(10, otro, espacio, cerrar, restaurar);
        botones.setAlignment(Pos.CENTER_LEFT);
        HBox progreso = new HBox(10, girando, lblProgreso);
        progreso.setAlignment(Pos.CENTER_LEFT);

        VBox raiz = new VBox(14, titulo, explicacion, lista, lblElegido, progreso, botones);
        raiz.setPadding(new Insets(24));
        raiz.getStyleClass().add("pantalla");
        raiz.setPrefWidth(640);
        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(EstiloApp.hojaDeEstilos());
        ventana.setScene(escena);
        ventana.showAndWait();
    }

    private static boolean confirmar(Window duena, File archivo) {
        ButtonType si = new ButtonType("Sí, restaurar", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Se va a volver a la copia:\n" + describir(archivo) + "\n\nLo cargado después de esa fecha se "
                + "reemplaza. Antes se guarda una copia de cómo está todo ahora.\n\n¿Seguimos?", si, no);
        alerta.initOwner(duena);
        alerta.setHeaderText(null);
        alerta.setTitle("Confirmar restauración");
        vistas.javafx.AlertaUtil.estilizar(alerta);
        return alerta.showAndWait().orElse(no) == si;
    }

    /** "Jueves 08/10/2026 a las 13:00 (automática)", sacado del nombre del archivo. */
    static String describir(File f) {
        String nombre = f.getName();
        String tipo = nombre.endsWith("_auto.sql") ? "automática" : nombre.endsWith("_manual.sql") ? "hecha a mano"
                : nombre.endsWith("_antes-de-restaurar.sql") ? "de antes de restaurar" : null;
        Date fecha = null;
        if (nombre.startsWith(dao.CopiaSeguridadDAO.PREFIJO) && nombre.length() >= dao.CopiaSeguridadDAO.PREFIJO.length() + 19) {
            String texto = nombre.substring(dao.CopiaSeguridadDAO.PREFIJO.length(), dao.CopiaSeguridadDAO.PREFIJO.length() + 19);
            try {
                fecha = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").parse(texto);
            } catch (ParseException e) {
                fecha = null;
            }
        }
        if (fecha == null) {
            fecha = new Date(f.lastModified());
        }
        String dia = new SimpleDateFormat("EEEE dd/MM/yyyy 'a las' HH:mm", new Locale("es", "AR")).format(fecha);
        dia = Character.toUpperCase(dia.charAt(0)) + dia.substring(1);
        return tipo == null ? dia + "  (" + nombre + ")" : dia + "  (" + tipo + ")";
    }

    private static String tamano(File f) {
        long b = f.length();
        if (b < 1024 * 1024) {
            return Math.max(1, b / 1024) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024.0));
    }
}
