package vistas.javafx.configuracion;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import modelo.EstadoNumeracion;
import utilidades.PreferenciasSistema;
import utilidades.SelectorArchivoUtil;
import vistas.javafx.PuenteEDT;
import vistas.javafx.obrasociales.ObrasSocialesScreen;

/**
 * Pantalla "Configuración" (sección Administración), en JavaFX puro (sin FXML), siguiendo la
 * distribución en pestañas de Figma (nodo 246:8124 y alrededores): Laboratorio | Sistema | Obras
 * Sociales | Informes PDF.
 *
 * <p>2026-10-01: Figma preveía acá una quinta pestaña, "Auditoría", que quedó como placeholder
 * ("todavía no armamos esta sección") hasta que esa pantalla existió de verdad (ver
 * {@link vistas.javafx.auditoria.AuditoriaScreen}, hoy "Historial" en el sidebar,
 * Administración). Se la sacó de acá a pedido de la clienta: el espacio de una pestaña dentro de
 * Configuración le quedaba chico para esa tabla, mejor una sola pantalla grande en vez de dos
 * (ésta, más chica, y la del sidebar) mostrando lo mismo.</p>
 *
 * <p>Reemplaza al stub vacío ("Form 11") que dejó el editor de formularios de NetBeans en
 * {@link vistas.formulariosPrincipales.Configuracion} -- ese archivo Swing queda sin usar (se
 * puede borrar desde NetBeans si se quiere limpiar el proyecto, no lo borra este cambio para no
 * tocar algo que no haga falta).</p>
 *
 * <p>"Obras Sociales" reutiliza tal cual {@link ObrasSocialesScreen}, la pantalla que antes vivía
 * directamente en el ítem "Pagos" del menú lateral (ver AppShell) y que en el diseño de Figma en
 * realidad va acá adentro. "Sistema" tiene 3 tarjetas, de arriba a abajo: "Numeración automática
 * de órdenes" y "Notificaciones del sistema" (las dos del diseño de Figma, con datos y toggles
 * reales -- ver {@link #armarTarjetaNumeracion()} y {@link #armarTarjetaNotificaciones()}), más
 * "Respaldo de datos" (Mes 6: Herramientas extra, no está en el diseño de Figma pero se deja como
 * herramienta extra -- ver {@link #armarTarjetaRespaldo()}).</p>
 *
 * <p><b>Mes 7 (Configuración e Informe de Resultados con colores):</b> "Laboratorio" pasó de
 * placeholder a un formulario real con TODOS los datos del encabezado que se imprimen en el
 * "Informe de Resultados" (ver {@link reportes.ResultadosPrintable} y
 * {@link #armarPanelLaboratorio()}), en dos tarjetas lado a lado ("Datos del Laboratorio" y
 * "Contacto y Ubicación", igual que el Figma que mandó la clienta) más el logo, opcional, debajo.
 * El campo "Nombre del laboratorio" que antes vivía en "Informes PDF" se movió para acá (misma
 * clave de {@link PreferenciasSistema}, no rompe el Comprobante de Orden que ya lo usaba) --
 * "Informes PDF" (ver {@link #armarPanelInformesPdf()}) ahora tiene una tarjeta de solo lectura
 * con ese mismo encabezado (para no repetir los campos editables en dos pestañas distintas) más
 * una tarjeta de "Opciones del informe" (qué mostrar en el Informe de Resultados) y una vista
 * previa con datos de ejemplo para ver el efecto de esas opciones sin tener que cargar un
 * resultado real.</p>
 *
 * <p>Mes 8 (rediseño visual): el {@link ScrollPane} propio que esta pantalla tenía para que la
 * pestaña activa pudiera bajar en vez de cortarse se sacó de acá -- ahora ese scroll lo pone
 * {@code AppShell#mostrarFx} una sola vez para TODAS las pantallas (ver el javadoc de ese método),
 * así que agregar uno acá también hubiera dejado dos barras de scroll anidadas.</p>
 */
public class ConfiguracionScreen {

    private static final String[] NOMBRES_PESTANAS = {
        "Laboratorio", "Sistema", "Obras Sociales", "Informes PDF"
    };

    // Estilos repetidos entre tarjetas (Mes 7: al agregar más tarjetas por pestaña, repetir el
    // literal de estilo en cada una empezaba a ser más difícil de mantener consistente).
    private static final String ESTILO_TITULO_TARJETA =
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #141C19;";
    private static final String ESTILO_DESCRIPCION_TARJETA =
            "-fx-font-size: 13px; -fx-text-fill: #8C9490;";

    private final HBox[] itemsPestanas = new HBox[NOMBRES_PESTANAS.length];
    private final Parent[] paneles = new Parent[NOMBRES_PESTANAS.length];
    private final StackPane areaContenido = new StackPane();

    public Parent construir() {
        VBox raiz = new VBox(20);
        raiz.getStyleClass().add("pantalla");

        paneles[0] = armarPanelLaboratorio();
        paneles[1] = armarPanelSistema();
        paneles[2] = new ObrasSocialesScreen().construir();
        paneles[3] = armarPanelInformesPdf();

        areaContenido.setAlignment(Pos.TOP_LEFT);

        raiz.getChildren().addAll(armarEncabezado(), armarBarraPestanas(), areaContenido);

        seleccionarPestana(0);
        return raiz;
    }

    private VBox armarEncabezado() {
        Label titulo = new Label("Configuración");
        titulo.getStyleClass().add("titulo-pantalla");
        Label subtitulo = new Label("Ajustes generales del sistema y del laboratorio");
        subtitulo.getStyleClass().add("subtitulo-pantalla");
        return new VBox(4, titulo, subtitulo);
    }

    private HBox armarBarraPestanas() {
        HBox barra = new HBox(28);
        for (int i = 0; i < NOMBRES_PESTANAS.length; i++) {
            int indice = i;
            HBox item = crearItemPestana(NOMBRES_PESTANAS[i]);
            item.setOnMouseClicked(evt -> seleccionarPestana(indice));
            itemsPestanas[i] = item;
            barra.getChildren().add(item);
        }
        barra.setStyle("-fx-border-color: transparent transparent #EEF1EE transparent; -fx-border-width: 0 0 1 0;");
        return barra;
    }

    private HBox crearItemPestana(String texto) {
        Label etiqueta = new Label(texto);
        HBox item = new HBox(0, etiqueta);
        item.getStyleClass().add("tab-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private void seleccionarPestana(int indice) {
        for (HBox item : itemsPestanas) {
            item.getStyleClass().remove("tab-item-activo");
        }
        itemsPestanas[indice].getStyleClass().add("tab-item-activo");
        areaContenido.getChildren().setAll(paneles[indice]);
    }

    /**
     * Pestaña "Laboratorio": todos los datos del encabezado que usa el "Informe de Resultados"
     * (ver {@link reportes.ResultadosPrintable}), en dos tarjetas lado a lado -- "Datos del
     * Laboratorio" (identidad: nombre, razón social, CUIT, bioquímica responsable + matrícula) y
     * "Contacto y Ubicación" (dirección, teléfono, email, localidad, habilitación) -- más el logo
     * debajo de las dos, igual que el diseño de Figma que mandó la clienta. Ningún campo es
     * obligatorio: el informe se imprime igual con lo que falte, mostrando sólo lo que sí está
     * cargado (mismo criterio que ya usa el Comprobante de Orden con la leyenda de pie).
     */
    private Parent armarPanelLaboratorio() {
        Label lblTitulo = new Label("Datos del laboratorio");
        lblTitulo.setStyle(ESTILO_TITULO_TARJETA);

        Label lblDescripcion = new Label(
                "Estos datos arman el encabezado del \"Informe de Resultados\" que se genera desde "
                + "Registros -- el mismo formato de un resultado de laboratorio en papel.");
        lblDescripcion.setStyle(ESTILO_DESCRIPCION_TARJETA);
        lblDescripcion.setWrapText(true);
        lblDescripcion.setMaxWidth(760);

        // Área de texto (no campo de una línea): el nombre puede ser largo y se puede cortar con
        // Enter donde se quiera que baje de renglón en el informe.
        javafx.scene.control.TextArea campoNombre = new javafx.scene.control.TextArea(PreferenciasSistema.getNombreLaboratorio());
        campoNombre.getStyleClass().add("campo-form");
        campoNombre.setWrapText(true);
        campoNombre.setPrefRowCount(2);
        campoNombre.setMaxWidth(Double.MAX_VALUE);
        campoNombre.setPromptText("Ej: Centro de Especialidades Médicas \"San Gregorio\" (Enter = bajar de renglón)");
        TextField campoSubtituloLab = campoTexto(PreferenciasSistema.getSubtitulo());
        campoSubtituloLab.setPromptText("Ej: Laboratorio de Análisis Clínicos");
        TextField campoRazonSocial = campoTexto(PreferenciasSistema.getRazonSocial());
        campoRazonSocial.setPromptText("Ej: San Gregorio S.A.");
        TextField campoCuit = campoTexto(PreferenciasSistema.getCuit());
        campoCuit.setPromptText("Ej: 30-71234567-8");
        TextField campoBioquimica = campoTexto(PreferenciasSistema.getBioquimicaResponsable());
        campoBioquimica.setPromptText("Ej: Fabiola Pantoja");
        TextField campoMatricula = campoTexto(PreferenciasSistema.getMatriculaBioquimica());
        campoMatricula.setPromptText("Ej: M.P 311");

        VBox tarjetaDatos = armarSubtarjeta("Datos del Laboratorio",
                campoConEtiqueta("Nombre del laboratorio (como sale en los informes)", campoNombre),
                campoConEtiqueta("Segunda línea del informe (opcional)", campoSubtituloLab),
                campoConEtiqueta("Razón Social (opcional)", campoRazonSocial),
                campoConEtiqueta("CUIT (opcional)", campoCuit),
                campoConEtiqueta("Bioquímica responsable (opcional)", campoBioquimica),
                campoConEtiqueta("Matrícula profesional (opcional)", campoMatricula));

        TextField campoDireccion = campoTexto(PreferenciasSistema.getDireccion());
        campoDireccion.setPromptText("Ej: Pedro Goyena N° 33");
        TextField campoTelefono = campoTexto(PreferenciasSistema.getTelefono());
        campoTelefono.setPromptText("Ej: 3888 - 480686");
        TextField campoEmail = campoTexto(PreferenciasSistema.getEmailContacto());
        campoEmail.setPromptText("Ej: contacto@labsangregorio.com");
        TextField campoLocalidad = campoTexto(PreferenciasSistema.getLocalidad());
        campoLocalidad.setPromptText("Ej: San Pedro de Jujuy");
        TextField campoHabilitacion = campoTexto(PreferenciasSistema.getHabilitacionNumero());
        campoHabilitacion.setPromptText("Ej: 1234/A");

        VBox tarjetaContacto = armarSubtarjeta("Contacto y Ubicación",
                campoConEtiqueta("Dirección (opcional)", campoDireccion),
                campoConEtiqueta("Teléfono (opcional)", campoTelefono),
                campoConEtiqueta("Email de contacto (opcional)", campoEmail),
                campoConEtiqueta("Localidad (opcional)", campoLocalidad),
                campoConEtiqueta("Habilitación N° (opcional)", campoHabilitacion));

        HBox filaSubtarjetas = new HBox(20, tarjetaDatos, tarjetaContacto);
        HBox.setHgrow(tarjetaDatos, Priority.ALWAYS);
        HBox.setHgrow(tarjetaContacto, Priority.ALWAYS);

        Label[] rutaLogoActual = {new Label(textoRutaLogo(PreferenciasSistema.getRutaLogo()))};
        rutaLogoActual[0].setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");
        String[] rutaLogoElegida = {PreferenciasSistema.getRutaLogo()};

        Button botonLogo = new Button("Elegir imagen...");
        botonLogo.getStyleClass().add("boton-secundario");
        botonLogo.setOnAction(evt -> {
            File elegido = vistas.javafx.SelectorArchivoFx.elegirImagenLogo(botonLogo);
            if (elegido != null) {
                rutaLogoElegida[0] = elegido.getAbsolutePath();
                rutaLogoActual[0].setText(textoRutaLogo(rutaLogoElegida[0]));
            }
        });
        Button botonQuitarLogo = new Button("Quitar");
        botonQuitarLogo.getStyleClass().add("boton-fila");
        botonQuitarLogo.setOnAction(evt -> {
            rutaLogoElegida[0] = "";
            rutaLogoActual[0].setText(textoRutaLogo(""));
        });
        HBox filaLogo = new HBox(10, botonLogo, botonQuitarLogo, rutaLogoActual[0]);
        filaLogo.setAlignment(Pos.CENTER_LEFT);

        // Firma digitalizada (imagen escaneada o foto de la firma/sello)
        Label[] rutaFirmaActual = {new Label(textoRutaFirma(PreferenciasSistema.getRutaFirma()))};
        rutaFirmaActual[0].setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");
        String[] rutaFirmaElegida = {PreferenciasSistema.getRutaFirma()};
        Button botonFirma = new Button("Elegir imagen de la firma...");
        botonFirma.getStyleClass().add("boton-secundario");
        botonFirma.setOnAction(evt -> {
            File elegido = vistas.javafx.SelectorArchivoFx.elegirImagenLogo(botonFirma);
            if (elegido != null) {
                rutaFirmaElegida[0] = elegido.getAbsolutePath();
                rutaFirmaActual[0].setText(textoRutaFirma(rutaFirmaElegida[0]));
            }
        });
        Button botonQuitarFirma = new Button("Quitar");
        botonQuitarFirma.getStyleClass().add("boton-fila");
        botonQuitarFirma.setOnAction(evt -> {
            rutaFirmaElegida[0] = "";
            rutaFirmaActual[0].setText(textoRutaFirma(""));
        });
        HBox filaFirma = new HBox(10, botonFirma, botonQuitarFirma, rutaFirmaActual[0]);
        filaFirma.setAlignment(Pos.CENTER_LEFT);
        Label ayudaFirma = new Label("Foto o escaneo de la firma (y sello) sobre fondo blanco, idealmente PNG con fondo "
                + "transparente. Se imprime sobre la línea de firma del informe. Es una firma digitalizada (imagen), "
                + "no una firma digital con certificado.");
        ayudaFirma.setWrapText(true);
        ayudaFirma.setStyle("-fx-font-size: 11px; -fx-text-fill: #8C9490;");

        // Nombre del sistema (lo que se ve arriba a la izquierda de la app y en el login)
        TextField campoNombreSistema = campoTexto(PreferenciasSistema.getNombreSistemaGuardado());
        campoNombreSistema.setPromptText("Ej: Laboratorio San Gregorio (si lo dejás vacío, usa el nombre del laboratorio)");
        TextField campoSubtituloSistema = campoTexto(PreferenciasSistema.getSubtituloSistema());
        CheckBox chkLogoSistema = new CheckBox("Mostrar el logo arriba a la izquierda (en vez de las iniciales)");
        chkLogoSistema.setSelected(PreferenciasSistema.isUsarLogoEnSistema());
        // Inicio de sesión: logo y foto de fondo del panel de la izquierda.
        CheckBox chkLogoLogin = new CheckBox("Mostrar el logo en la pantalla de inicio de sesión");
        chkLogoLogin.setSelected(PreferenciasSistema.isLogoEnLogin());
        Label[] rutaFotoActual = {new Label(textoRutaFoto(PreferenciasSistema.getFotoLogin()))};
        rutaFotoActual[0].setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");
        String[] rutaFotoElegida = {PreferenciasSistema.getFotoLogin()};
        Button botonFoto = new Button("Elegir foto...");
        botonFoto.getStyleClass().add("boton-secundario");
        botonFoto.setOnAction(evt -> {
            File elegido = vistas.javafx.SelectorArchivoFx.elegirImagenLogo(botonFoto);
            if (elegido != null) {
                rutaFotoElegida[0] = elegido.getAbsolutePath();
                rutaFotoActual[0].setText(textoRutaFoto(rutaFotoElegida[0]));
            }
        });
        Button botonQuitarFoto = new Button("Quitar");
        botonQuitarFoto.getStyleClass().add("boton-fila");
        botonQuitarFoto.setOnAction(evt -> {
            rutaFotoElegida[0] = "";
            rutaFotoActual[0].setText(textoRutaFoto(""));
        });
        HBox filaFoto = new HBox(10, botonFoto, botonQuitarFoto, rutaFotoActual[0]);
        filaFoto.setAlignment(Pos.CENTER_LEFT);
        Label ayudaFoto = new Label("Foto de fondo del lado izquierdo del inicio de sesión (por ejemplo, el frente o el "
                + "interior del laboratorio). Se le pone un velo verde para que el nombre se lea bien. Se ve la "
                + "próxima vez que se abra el sistema.");
        ayudaFoto.setWrapText(true);
        ayudaFoto.setStyle("-fx-font-size: 11px; -fx-text-fill: #8C9490;");
        VBox tarjetaSistema = armarSubtarjeta("Nombre del sistema e inicio de sesión",
                campoConEtiqueta("Nombre", campoNombreSistema),
                campoConEtiqueta("Bajada", campoSubtituloSistema),
                chkLogoSistema, chkLogoLogin,
                campoConEtiqueta("Foto del inicio de sesión (opcional)", filaFoto), ayudaFoto);

        Button botonGuardar = new Button("Guardar cambios");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> {
            PreferenciasSistema.setNombreLaboratorio(campoNombre.getText());
            PreferenciasSistema.setSubtitulo(campoSubtituloLab.getText());
            PreferenciasSistema.setRutaFirma(rutaFirmaElegida[0]);
            PreferenciasSistema.setNombreSistema(campoNombreSistema.getText());
            PreferenciasSistema.setSubtituloSistema(campoSubtituloSistema.getText());
            PreferenciasSistema.setUsarLogoEnSistema(chkLogoSistema.isSelected());
            PreferenciasSistema.setLogoEnLogin(chkLogoLogin.isSelected());
            PreferenciasSistema.setFotoLogin(rutaFotoElegida[0]);
            PreferenciasSistema.setRazonSocial(campoRazonSocial.getText());
            PreferenciasSistema.setCuit(campoCuit.getText());
            PreferenciasSistema.setBioquimicaResponsable(campoBioquimica.getText());
            PreferenciasSistema.setMatriculaBioquimica(campoMatricula.getText());
            PreferenciasSistema.setDireccion(campoDireccion.getText());
            PreferenciasSistema.setTelefono(campoTelefono.getText());
            PreferenciasSistema.setEmailContacto(campoEmail.getText());
            PreferenciasSistema.setLocalidad(campoLocalidad.getText());
            PreferenciasSistema.setHabilitacionNumero(campoHabilitacion.getText());
            PreferenciasSistema.setRutaLogo(rutaLogoElegida[0]);
            // Toast en vez del texto fijo "✓ Guardado" de al lado del botón (ver ToastUtil) --
            // además, como el header de arriba de toda la app ahora lee el nombre del
            // laboratorio en vivo (ver HeaderPanel#armarMarca), el cambio se nota ahí mismo sin
            // tener que reabrir Configuración.
            vistas.javafx.ToastUtil.exito("Datos del laboratorio guardados");
            vistas.javafx.shell.HeaderPanel.refrescarMarca();
            // Estos datos salen en el encabezado y la firma de cada PDF: los PDF ya guardados se
            // rehacen solos con lo nuevo, en segundo plano.
            vistas.javafx.TareaFondo.ejecutar(reportes.InformesPdfService::regenerarTodos, cantidad -> {
                if (cantidad > 0) {
                    vistas.javafx.ToastUtil.info("Se actualizaron " + cantidad + " PDF ya guardados");
                }
            }, null);
        });

        VBox contenido = new VBox(18, lblTitulo, lblDescripcion, filaSubtarjetas,
                campoConEtiqueta("Logo (opcional)", filaLogo),
                campoConEtiqueta("Firma digitalizada (opcional)", new VBox(6, filaFirma, ayudaFirma)),
                tarjetaSistema,
                new HBox(12, botonGuardar));

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        caja.setMaxWidth(920);
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);
        return caja;
    }

    private TextField campoTexto(String valorInicial) {
        TextField campo = new TextField(valorInicial);
        campo.getStyleClass().add("campo-form");
        // Sin ancho fijo (a diferencia de antes): así se estira para ocupar el ancho real de la
        // subtarjeta que lo contiene (ver armarSubtarjeta) en vez de quedar todo en una sola
        // columna angosta -- eso era justo lo que se veía "en fila" y desarmaba el encuadre.
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private VBox campoConEtiqueta(String etiquetaTexto, Node campo) {
        Label etiqueta = new Label(etiquetaTexto);
        etiqueta.getStyleClass().add("etiqueta-campo");
        return new VBox(6, etiqueta, campo);
    }

    private String textoRutaFirma(String ruta) {
        return ruta == null || ruta.trim().isEmpty() ? "(sin firma)" : new File(ruta).getName();
    }

    private String textoRutaFoto(String ruta) {
        return ruta == null || ruta.trim().isEmpty() ? "(sin foto: fondo verde)" : new File(ruta).getName();
    }

    private String textoRutaLogo(String ruta) {
        return ruta == null || ruta.trim().isEmpty() ? "(sin logo)" : new File(ruta).getName();
    }

    /**
     * Una "subtarjeta" gris clarito adentro de una tarjeta blanca más grande -- el mismo recurso
     * visual que ya usaban la vista previa de "Informes PDF" o el editor de tramos de color, acá
     * reutilizado para separar dos grupos de campos relacionados (por ejemplo "Datos del
     * Laboratorio" y "Contacto y Ubicación") uno al lado del otro sin agregar una clase CSS nueva.
     * Con {@code HBox.setHgrow(ALWAYS)} sobre el resultado, dos de éstas en una misma fila quedan
     * del mismo ancho -- ver {@link #armarPanelLaboratorio()} y {@link #armarPanelInformesPdf()}.
     */
    private VBox armarSubtarjeta(String titulo, Node... filas) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        VBox caja = new VBox(12, lblTitulo);
        caja.getChildren().addAll(filas);
        caja.setPadding(new Insets(16));
        caja.setStyle("-fx-background-color: #F6F8F7; -fx-background-radius: 10;");
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    /** Una línea "etiqueta chica arriba, valor abajo" de solo lectura -- ver {@link #armarPanelInformesPdf()}. */
    private VBox lineaSoloLectura(String etiqueta, String valor) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.setStyle("-fx-font-size: 11px; -fx-text-fill: #8C9490;");
        Label lblValor = new Label(valor == null || valor.trim().isEmpty() ? "(sin cargar)" : valor.trim());
        lblValor.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
        lblValor.setWrapText(true);
        return new VBox(2, lblEtiqueta, lblValor);
    }

    /**
     * Pestaña "Sistema": las 3 tarjetas, de arriba a abajo -- ver el javadoc de la clase.
     */
    private Parent armarPanelSistema() {
        VBox columna = new VBox(20,
                armarTarjetaNumeracion(),
                armarTarjetaNotificaciones(),
                new CopiasSeguridadPanel().construir(),
                armarTarjetaRespaldo());
        columna.setMaxWidth(620);
        return columna;
    }

    /**
     * "Numeración automática de órdenes" (Figma): muestra el próximo número que le va a tocar a
     * una orden nueva, el mes en curso y cuántas se generaron ya -- datos reales, salen de
     * {@link dao.PedidoDAO#obtenerEstadoNumeracion} a través de
     * {@link controlador.ConfiguracionController}. El número en sí ya se genera solo (ver
     * {@code PedidoDAO#crearPedido}) así que acá no hay nada para configurar, sólo para mostrar.
     */
    private Parent armarTarjetaNumeracion() {
        Label lblTitulo = new Label("Numeración automática de órdenes");
        lblTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label lblDescripcion = new Label(
                "El sistema genera y reinicia el número automáticamente al comenzar cada mes. "
                + "El formato es AAAA-MM-NNN -- no requiere configuración.");
        lblDescripcion.setStyle("-fx-font-size: 13px; -fx-text-fill: #8C9490;");
        lblDescripcion.setWrapText(true);
        lblDescripcion.setMaxWidth(500);

        EstadoNumeracion estado = PuenteEDT.ejecutar(
                () -> controlador.ConfiguracionController.obtenerEstadoNumeracion(null),
                null);
        String proximoNumero = estado != null ? estado.getProximoNumero() : "-";
        String mesEnCurso = estado != null ? estado.getMesEnCurso() : "-";
        String ordenesEsteMes = estado != null ? String.valueOf(estado.getOrdenesEsteMes()) : "-";

        HBox tarjetasStat = new HBox(14,
                armarEstadistica("Próxima orden", proximoNumero),
                armarEstadistica("Mes en curso", mesEnCurso),
                armarEstadistica("Órdenes este mes", ordenesEsteMes + " registradas"));

        Label lblBanner = new Label("✓ Al comenzar " + nombreProximoMes()
                + " la numeración pasará automáticamente a " + prefijoProximoMes()
                + "-001. No se requiere ninguna acción.");
        lblBanner.setWrapText(true);
        lblBanner.setMaxWidth(500);
        lblBanner.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #1E7A42; -fx-background-color: #E8F5E9; "
                + "-fx-background-radius: 8; -fx-padding: 10 14 10 14;");

        VBox contenido = new VBox(14, lblTitulo, lblDescripcion, tarjetasStat, lblBanner);

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        return caja;
    }

    private VBox armarEstadistica(String titulo, String valor) {
        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #8C9490;");

        VBox caja = new VBox(4, lblValor, lblTitulo);
        caja.setPadding(new Insets(12, 16, 12, 16));
        caja.setStyle("-fx-background-color: #F6F8F7; -fx-background-radius: 10;");
        caja.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    private static String prefijoProximoMes() {
        Calendar proximoMes = Calendar.getInstance();
        proximoMes.setTime(new Date());
        proximoMes.add(Calendar.MONTH, 1);
        return new SimpleDateFormat("yyyy-MM").format(proximoMes.getTime());
    }

    private static String nombreProximoMes() {
        Calendar proximoMes = Calendar.getInstance();
        proximoMes.setTime(new Date());
        proximoMes.add(Calendar.MONTH, 1);
        return new SimpleDateFormat("MMMM yyyy", new java.util.Locale("es", "AR")).format(proximoMes.getTime());
    }

    /**
     * "Notificaciones del sistema" (Figma): 3 toggles que se guardan al toque en
     * {@link PreferenciasSistema} apenas se tocan (no hay un botón aparte de "Guardar" para esta
     * tarjeta -- son 3 interruptores, no un formulario).
     */
    private Parent armarTarjetaNotificaciones() {
        Label lblTitulo = new Label("Notificaciones del sistema");
        lblTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        CheckBox chkTecnico = new CheckBox("Notificar al técnico cuando un análisis queda listo");
        chkTecnico.setSelected(PreferenciasSistema.isNotificarTecnico());
        chkTecnico.setOnAction(evt -> PreferenciasSistema.setNotificarTecnico(chkTecnico.isSelected()));

        CheckBox chkAlertas = new CheckBox("Alertas de valores fuera de rango al cargar resultados");
        chkAlertas.setSelected(PreferenciasSistema.isAlertasFueraDeRango());
        chkAlertas.setOnAction(evt -> PreferenciasSistema.setAlertasFueraDeRango(chkAlertas.isSelected()));

        CheckBox chkRecordatorio = new CheckBox("Recordatorio de análisis pendientes al iniciar el día");
        chkRecordatorio.setSelected(PreferenciasSistema.isRecordatorioPendientes());
        chkRecordatorio.setOnAction(evt -> PreferenciasSistema.setRecordatorioPendientes(chkRecordatorio.isSelected()));

        for (CheckBox chk : new CheckBox[]{chkTecnico, chkAlertas, chkRecordatorio}) {
            chk.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #141C19;");
        }

        VBox contenido = new VBox(16, lblTitulo, chkTecnico, chkAlertas, chkRecordatorio);

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        return caja;
    }

    /**
     * "Respaldo de datos" (Mes 6: Herramientas extra -- no está en el diseño de Figma, se deja
     * como una tarjeta más acá abajo). Exporta las tablas principales del laboratorio a archivos
     * .csv, uno por tabla, en la carpeta que elija la usuaria -- ver {@link dao.RespaldoDAO} para
     * qué tablas exactamente y por qué.
     */
    private Parent armarTarjetaRespaldo() {
        Label lblTitulo = new Label("Exportar a Excel (.csv)");
        lblTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #141C19;");

        Label lblDescripcion = new Label(
                "Exporta una copia de las tablas principales del laboratorio (pacientes, médicos, "
                + "obras sociales, pedidos, exámenes y pagos) a archivos .csv, uno por tabla, en la "
                + "carpeta que elijas. Sirve como respaldo o para pasar los datos a otro lado, por "
                + "ejemplo abrirlos en Excel.");
        lblDescripcion.setStyle("-fx-font-size: 13px; -fx-text-fill: #8C9490;");
        lblDescripcion.setWrapText(true);
        lblDescripcion.setMaxWidth(500);

        Button botonRespaldo = new Button("Elegir carpeta y exportar");
        botonRespaldo.getStyleClass().add("boton-primario");
        botonRespaldo.setOnAction(evt -> ejecutarRespaldo());

        VBox contenido = new VBox(14, lblTitulo, lblDescripcion, botonRespaldo);

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        return caja;
    }

    private void ejecutarRespaldo() {
        File carpeta = vistas.javafx.SelectorArchivoFx.elegirCarpeta(areaContenido, "Elegir carpeta para el respaldo");
        if (carpeta == null) {
            return;
        }

        // En segundo plano: exportar todas las tablas puede tardar unos segundos y no tiene que
        // congelar la pantalla mientras tanto.
        vistas.javafx.ToastUtil.info("Exportando el respaldo...");
        vistas.javafx.TareaFondo.ejecutar(() -> controlador.RespaldoController.exportarTodo(null, carpeta), exportadas -> {
            if (exportadas > 0) {
                vistas.javafx.AvisoRapido.mostrar("Respaldo listo",
                        "Se exportaron " + exportadas + " tablas a:\n" + carpeta.getAbsolutePath());
                return;
            }
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setHeaderText(null);
            alerta.setTitle("Respaldo de datos");
            alerta.setContentText(exportadas > 0
                    ? "Se exportaron " + exportadas + " tablas a:\n" + carpeta.getAbsolutePath()
                    : "No se pudo exportar ninguna tabla. Revisá la conexión a la base de datos.");
            vistas.javafx.AlertaUtil.estilizar(alerta);
            alerta.showAndWait();
        }, error -> vistas.javafx.ToastUtil.error("No se pudo hacer el respaldo: " + vistas.javafx.TareaFondo.mensaje(error)));
    }

    /**
     * Pestaña "Informes PDF" (2026-10-08). De arriba a abajo, a la izquierda:
     * <ol>
     *   <li>"¿Qué se muestra en el informe?" -- las opciones de siempre (logo, referencias,
     *       rojo en Alto/Bajo, médico, habilitación) más "agregar el comprobante al final".</li>
     *   <li>"Diseño y posiciones" -- tamaño de letra, ancho de columnas, márgenes, dónde van el
     *       logo y la firma, etc. Es el diseño PREDETERMINADO (para todas las órdenes); cada orden
     *       se puede ajustar aparte desde su vista previa en Registros.</li>
     *   <li>"Comprobante de la orden" -- si al crear una orden se pregunta, se guarda solo o no se
     *       hace, y la leyenda del pie.</li>
     *   <li>"Dónde se guardan los PDF" -- la carpeta donde el sistema guarda solo cada PDF.</li>
     * </ol>
     * A la derecha, la vista previa: es el informe REAL (el mismo motor que arma el PDF), con
     * datos de ejemplo, y se redibuja al instante con cada cambio. Donde falta un dato del
     * laboratorio, la vista previa lo avisa en naranja (eso nunca sale en un PDF real).
     */
    private Parent armarPanelInformesPdf() {
        Label lblTitulo = new Label("Informes PDF");
        lblTitulo.setStyle(ESTILO_TITULO_TARJETA);
        Label lblDescripcion = new Label(
                "Acá se elige qué muestra el informe de resultados, cómo se acomoda en la hoja y dónde se "
                + "guardan los PDF. A la derecha ves cómo queda (con datos de ejemplo). Los datos del "
                + "laboratorio (nombre, dirección, bioquímica, logo) se cargan en la pestaña \"Laboratorio\".");
        lblDescripcion.setStyle(ESTILO_DESCRIPCION_TARJETA);
        lblDescripcion.setWrapText(true);

        Label lblCambios = new Label();
        lblCambios.setStyle("-fx-font-size: 12px; -fx-text-fill: #B45309; -fx-font-weight: bold;");

        // ---- 1) Qué se muestra
        CheckBox chkLogo = new CheckBox("Mostrar el logo del laboratorio");
        chkLogo.setSelected(PreferenciasSistema.isInformeMostrarLogo());
        CheckBox chkReferencia = new CheckBox("Mostrar los valores de referencia (con sus colores)");
        chkReferencia.setSelected(PreferenciasSistema.isInformeIncluirReferencia());
        CheckBox chkResaltar = new CheckBox("Marcar en rojo el estado \"Alto\" / \"Bajo\" (solo esa celda)");
        chkResaltar.setSelected(PreferenciasSistema.isInformeResaltarFueraDeRango());
        CheckBox chkMedico = new CheckBox("Mostrar el médico derivante (si está cargado)");
        chkMedico.setSelected(PreferenciasSistema.isInformeIncluirMedico());
        CheckBox chkHabilitacion = new CheckBox("Mostrar el N° de habilitación al pie");
        chkHabilitacion.setSelected(PreferenciasSistema.isInformeMostrarHabilitacion());
        CheckBox chkFirmaImagen = new CheckBox("Poner la firma digitalizada sobre la línea de firma (si hay una cargada)");
        chkFirmaImagen.setSelected(PreferenciasSistema.isInformeUsarFirmaImagen());
        CheckBox chkComprobante = new CheckBox("Agregar el comprobante de la orden como última hoja del informe");
        chkComprobante.setSelected(PreferenciasSistema.isInformeIncluirComprobante());
        chkComprobante.setWrapText(true);
        CheckBox[] opciones = {chkLogo, chkReferencia, chkResaltar, chkMedico, chkHabilitacion, chkFirmaImagen, chkComprobante};
        for (CheckBox chk : opciones) {
            chk.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
            chk.setWrapText(true);
        }
        VBox tarjetaOpciones = armarSubtarjeta("¿Qué se muestra en el informe?", opciones);

        // ---- 3) Comprobante
        javafx.scene.control.ToggleGroup grupoComprobante = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.RadioButton rbPreguntar = new javafx.scene.control.RadioButton(
                "Preguntarme cada vez (guardar PDF, imprimir o nada)");
        javafx.scene.control.RadioButton rbAutomatico = new javafx.scene.control.RadioButton(
                "Guardar el PDF del comprobante automáticamente");
        javafx.scene.control.RadioButton rbNunca = new javafx.scene.control.RadioButton("No hacer comprobante");
        for (javafx.scene.control.RadioButton rb : new javafx.scene.control.RadioButton[]{rbPreguntar, rbAutomatico, rbNunca}) {
            rb.setToggleGroup(grupoComprobante);
            rb.setWrapText(true);
            rb.setStyle("-fx-font-size: 13px; -fx-text-fill: #141C19;");
        }
        String modo = PreferenciasSistema.getModoComprobante();
        (PreferenciasSistema.COMPROBANTE_AUTOMATICO.equals(modo) ? rbAutomatico
                : PreferenciasSistema.COMPROBANTE_NUNCA.equals(modo) ? rbNunca : rbPreguntar).setSelected(true);
        Label lblAyudaComprobante = new Label("Al generar una orden en Nuevo Análisis. Si además lo querés junto "
                + "con los resultados, tildá \"Agregar el comprobante...\" arriba.");
        lblAyudaComprobante.getStyleClass().add("ayuda-diseno");
        lblAyudaComprobante.setWrapText(true);
        TextField campoLeyenda = new TextField(PreferenciasSistema.getLeyendaPie());
        campoLeyenda.getStyleClass().add("campo-form");
        campoLeyenda.setMaxWidth(Double.MAX_VALUE);
        campoLeyenda.setPromptText("Ej: Los resultados deben ser interpretados por su médico");
        VBox tarjetaComprobante = armarSubtarjeta("Comprobante de la orden", lblAyudaComprobante,
                rbPreguntar, rbAutomatico, rbNunca,
                campoConEtiqueta("Leyenda al pie de cada hoja (informe y comprobante, opcional)", campoLeyenda));

        // ---- 4) Carpeta
        String[] carpetaElegida = {PreferenciasSistema.getCarpetaPdf()};
        Label lblCarpeta = new Label(carpetaElegida[0]);
        lblCarpeta.setWrapText(true);
        lblCarpeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #3A4440; -fx-font-weight: bold;");
        Button botonCambiarCarpeta = new Button("Cambiar carpeta...");
        botonCambiarCarpeta.getStyleClass().add("boton-secundario");
        Button botonAbrirCarpeta = new Button("Abrir carpeta");
        botonAbrirCarpeta.getStyleClass().add("enlace-accion");
        botonAbrirCarpeta.setOnAction(evt -> vistas.javafx.TareaFondo.ejecutar(() -> {
            File carpeta = new File(carpetaElegida[0]);
            if (!carpeta.isDirectory()) {
                carpeta.mkdirs();
            }
            utilidades.ArchivosUtil.mostrarEnCarpeta(carpeta);
            return null;
        }, null, error -> vistas.javafx.ToastUtil.error("No se pudo abrir la carpeta")));
        Button botonActualizarTodos = new Button("Actualizar todos los PDF ya guardados");
        botonActualizarTodos.getStyleClass().add("enlace-accion");
        botonActualizarTodos.setOnAction(evt -> {
            botonActualizarTodos.setDisable(true);
            botonActualizarTodos.setText("Actualizando...");
            vistas.javafx.TareaFondo.ejecutar(reportes.InformesPdfService::regenerarTodos, cantidad -> {
                botonActualizarTodos.setDisable(false);
                botonActualizarTodos.setText("Actualizar todos los PDF ya guardados");
                vistas.javafx.ToastUtil.exito(cantidad == 0 ? "No había PDF guardados para actualizar"
                        : "Se actualizaron " + cantidad + " PDF");
            }, null);
        });
        Label lblAyudaCarpeta = new Label("Ahí el sistema guarda solo cada PDF (en \"Informes\" y \"Comprobantes\") "
                + "y lo recuerda, así no hay que ir a buscarlo para mandarlo. Si cambiás un color o un rango en el "
                + "Catálogo, o un resultado, los PDF ya guardados se actualizan solos. Si cambiás la carpeta, los PDF "
                + "que ya estaban se mudan solos a la nueva.");
        lblAyudaCarpeta.getStyleClass().add("ayuda-diseno");
        lblAyudaCarpeta.setWrapText(true);
        HBox filaCarpeta = new HBox(10, botonCambiarCarpeta, botonAbrirCarpeta);
        filaCarpeta.setAlignment(Pos.CENTER_LEFT);
        VBox tarjetaCarpeta = armarSubtarjeta("Dónde se guardan los PDF", lblCarpeta, filaCarpeta, lblAyudaCarpeta,
                botonActualizarTodos);

        // ---- Vista previa (derecha)
        javafx.scene.image.ImageView vistaHoja = new javafx.scene.image.ImageView();
        vistaHoja.setPreserveRatio(true);
        vistaHoja.setFitWidth(470);
        javafx.scene.control.ProgressIndicator dibujando = new javafx.scene.control.ProgressIndicator();
        dibujando.setMaxSize(40, 40);
        dibujando.setVisible(false);
        StackPane hoja = new StackPane(vistaHoja, dibujando);
        hoja.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.1, 0, 3);");
        hoja.setMaxWidth(Region.USE_PREF_SIZE);
        Label lblPaginas = new Label();
        lblPaginas.getStyleClass().add("ayuda-diseno");
        Button botonAmpliar = new Button("🔍 Ver más grande");
        botonAmpliar.getStyleClass().add("enlace-accion");
        java.util.List<javafx.scene.image.Image> paginasVista = new java.util.ArrayList<>();
        botonAmpliar.setOnAction(evt -> ampliarVistaPrevia(paginasVista));
        Label lblTituloVista = new Label("Vista previa");
        lblTituloVista.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        Label lblNotaVista = new Label("Con datos de ejemplo -- en naranja, lo que falta completar (no sale en el PDF real).");
        lblNotaVista.getStyleClass().add("ayuda-diseno");
        lblNotaVista.setWrapText(true);
        HBox filaVista = new HBox(10, lblPaginas, botonAmpliar);
        filaVista.setAlignment(Pos.CENTER_LEFT);
        VBox columnaVista = new VBox(10, lblTituloVista, lblNotaVista, hoja, filaVista);
        columnaVista.setPadding(new Insets(14));
        columnaVista.setStyle("-fx-background-color: #E4E8E6; -fx-background-radius: 12;");
        columnaVista.setMinWidth(500);
        columnaVista.setMaxWidth(500);

        // Estado compartido para redibujar con lo que está en pantalla (guardado o no).
        java.util.concurrent.atomic.AtomicInteger version = new java.util.concurrent.atomic.AtomicInteger();
        reportes.DisenoInforme[] disenoEnPantalla = {reportes.InformesPdfService.disenoPredeterminado()};
        javafx.animation.PauseTransition demora = new javafx.animation.PauseTransition(javafx.util.Duration.millis(220));
        Runnable redibujar = () -> {
            int v = version.incrementAndGet();
            reportes.DisenoInforme diseno = disenoEnPantalla[0].copia();
            boolean logo = chkLogo.isSelected();
            boolean ref = chkReferencia.isSelected();
            boolean rojo = chkResaltar.isSelected();
            boolean medico = chkMedico.isSelected();
            boolean habilitacion = chkHabilitacion.isSelected();
            boolean comprobante = chkComprobante.isSelected();
            boolean firmaImagen = chkFirmaImagen.isSelected();
            String leyenda = campoLeyenda.getText();
            dibujando.setVisible(true);
            vistas.javafx.TareaFondo.ejecutar(() -> {
                reportes.ConfigInforme c = reportes.ConfigInforme.actual();
                c.mostrarLogo = logo;
                c.incluirReferencia = ref;
                c.resaltarFueraDeRango = rojo;
                c.incluirMedico = medico;
                c.mostrarHabilitacion = habilitacion;
                c.incluirComprobante = comprobante;
                c.usarFirmaImagen = firmaImagen;
                c.leyendaPie = leyenda;
                reportes.ResultadosDatos ejemplo = reportes.ResultadosDatos.ejemplo();
                ejemplo.setComprobante(comprobanteDeEjemplo(ejemplo));
                reportes.DocumentoMaquetado doc = reportes.MaquetadorInforme.armar(ejemplo, diseno, c, true);
                java.util.List<java.awt.image.BufferedImage> imagenes = new java.util.ArrayList<>();
                for (reportes.DocumentoMaquetado.Pagina p : doc.getPaginas()) {
                    imagenes.add(reportes.RenderizadorGraphics.aImagen(p, 1.6));
                }
                return imagenes;
            }, imagenes -> {
                if (v != version.get()) {
                    return;
                }
                paginasVista.clear();
                for (java.awt.image.BufferedImage img : imagenes) {
                    paginasVista.add(javafx.embed.swing.SwingFXUtils.toFXImage(img, null));
                }
                vistaHoja.setImage(paginasVista.isEmpty() ? null : paginasVista.get(0));
                lblPaginas.setText("Hoja 1 de " + paginasVista.size()
                        + (paginasVista.size() > 1 ? " (el resto, con \"Ver más grande\")" : ""));
                dibujando.setVisible(false);
            }, error -> dibujando.setVisible(false));
        };
        demora.setOnFinished(e -> redibujar.run());

        // ---- 2) Diseño
        vistas.javafx.comunes.EditorDisenoPanel editor = new vistas.javafx.comunes.EditorDisenoPanel(
                disenoEnPantalla[0], nuevo -> {
                    disenoEnPantalla[0] = nuevo;
                    lblCambios.setText("Hay cambios sin guardar");
                    demora.playFromStart();
                });
        Label lblTituloDiseno = new Label("Diseño y posiciones (para todas las órdenes)");
        lblTituloDiseno.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #141C19;");
        Label lblAyudaDiseno = new Label("Es el diseño predeterminado. Si una orden puntual necesita otro "
                + "acomodo, se ajusta desde Registros > ⋮ > \"Ver y acomodar el informe\".");
        lblAyudaDiseno.getStyleClass().add("ayuda-diseno");
        lblAyudaDiseno.setWrapText(true);
        VBox tarjetaDiseno = new VBox(10, lblTituloDiseno, lblAyudaDiseno, editor.getVista());

        Runnable marcarCambio = () -> {
            lblCambios.setText("Hay cambios sin guardar");
            demora.playFromStart();
        };
        for (CheckBox chk : opciones) {
            chk.setOnAction(evt -> marcarCambio.run());
        }
        campoLeyenda.textProperty().addListener((o, a, n) -> marcarCambio.run());
        grupoComprobante.selectedToggleProperty().addListener((o, a, n) -> lblCambios.setText("Hay cambios sin guardar"));
        botonCambiarCarpeta.setOnAction(evt -> {
            javafx.stage.DirectoryChooser selector = new javafx.stage.DirectoryChooser();
            selector.setTitle("Elegir la carpeta donde se guardan los PDF");
            File actual = new File(carpetaElegida[0]);
            if (actual.isDirectory()) {
                selector.setInitialDirectory(actual);
            }
            File elegida = selector.showDialog(botonCambiarCarpeta.getScene().getWindow());
            if (elegida == null || elegida.getAbsolutePath().equals(carpetaElegida[0])) {
                return;
            }
            // Se aplica en el momento y se mueven ahí todos los PDF ya guardados, para que no
            // queden unos en la carpeta vieja y otros en la nueva.
            String nueva = elegida.getAbsolutePath();
            botonCambiarCarpeta.setDisable(true);
            lblCarpeta.setText("Moviendo los PDF a " + nueva + "...");
            vistas.javafx.TareaFondo.ejecutar(() -> reportes.InformesPdfService.cambiarCarpeta(nueva), movidos -> {
                botonCambiarCarpeta.setDisable(false);
                carpetaElegida[0] = nueva;
                lblCarpeta.setText(nueva);
                vistas.javafx.ToastUtil.exito(movidos == 0 ? "Listo: los PDF se van a guardar en la carpeta nueva"
                        : "Listo: se movieron " + movidos + " PDF a la carpeta nueva");
            }, error -> {
                botonCambiarCarpeta.setDisable(false);
                carpetaElegida[0] = PreferenciasSistema.getCarpetaPdf();
                lblCarpeta.setText(carpetaElegida[0]);
                vistas.javafx.ToastUtil.error(vistas.javafx.TareaFondo.mensaje(error));
            });
        });

        Runnable guardar = () -> {
            PreferenciasSistema.setInformeMostrarLogo(chkLogo.isSelected());
            PreferenciasSistema.setInformeIncluirReferencia(chkReferencia.isSelected());
            PreferenciasSistema.setInformeResaltarFueraDeRango(chkResaltar.isSelected());
            PreferenciasSistema.setInformeIncluirMedico(chkMedico.isSelected());
            PreferenciasSistema.setInformeMostrarHabilitacion(chkHabilitacion.isSelected());
            PreferenciasSistema.setInformeIncluirComprobante(chkComprobante.isSelected());
            PreferenciasSistema.setInformeUsarFirmaImagen(chkFirmaImagen.isSelected());
            PreferenciasSistema.setLeyendaPie(campoLeyenda.getText());
            PreferenciasSistema.setModoComprobante(rbAutomatico.isSelected() ? PreferenciasSistema.COMPROBANTE_AUTOMATICO
                    : rbNunca.isSelected() ? PreferenciasSistema.COMPROBANTE_NUNCA : PreferenciasSistema.COMPROBANTE_PREGUNTAR);
            PreferenciasSistema.setDisenoInforme(editor.getDiseno().aTexto());
            lblCambios.setText("");
            vistas.javafx.ToastUtil.exito("Opciones de los informes guardadas");
            // Los PDF ya guardados se rehacen con lo nuevo, en segundo plano.
            vistas.javafx.TareaFondo.ejecutar(reportes.InformesPdfService::regenerarTodos, cantidad -> {
                if (cantidad > 0) {
                    vistas.javafx.ToastUtil.info("Se actualizaron " + cantidad + " PDF ya guardados");
                }
            }, null);
        };
        Button botonGuardar = new Button("Guardar cambios");
        botonGuardar.getStyleClass().add("boton-primario");
        botonGuardar.setOnAction(evt -> guardar.run());
        Button botonGuardarArriba = new Button("Guardar cambios");
        botonGuardarArriba.getStyleClass().add("boton-primario");
        botonGuardarArriba.setOnAction(evt -> guardar.run());

        Region espaciadorTitulo = new Region();
        HBox.setHgrow(espaciadorTitulo, Priority.ALWAYS);
        HBox filaTitulo = new HBox(12, lblTitulo, espaciadorTitulo, lblCambios, botonGuardarArriba);
        filaTitulo.setAlignment(Pos.CENTER_LEFT);

        VBox columnaIzquierda = new VBox(18, tarjetaOpciones, tarjetaDiseno, tarjetaComprobante, tarjetaCarpeta,
                new HBox(12, botonGuardar));
        columnaIzquierda.setMinWidth(420);
        HBox.setHgrow(columnaIzquierda, Priority.ALWAYS);

        HBox columnas = new HBox(24, columnaIzquierda, columnaVista);
        columnas.setAlignment(Pos.TOP_LEFT);

        VBox contenido = new VBox(18, filaTitulo, lblDescripcion, columnas);

        StackPane caja = new StackPane(contenido);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(24));
        caja.setMaxWidth(1220);
        StackPane.setAlignment(contenido, Pos.TOP_LEFT);

        redibujar.run();
        return caja;
    }

    /** Comprobante inventado para la vista previa (cuando está tildado "agregar el comprobante"). */
    private static reportes.ComprobanteOrdenDatos comprobanteDeEjemplo(reportes.ResultadosDatos ejemplo) {
        java.util.List<modelo.Prestacion> analisis = new java.util.ArrayList<>();
        analisis.add(new modelo.Prestacion(0, 475, "Hemograma completo", null));
        analisis.add(new modelo.Prestacion(0, 412, "Glucemia", null));
        analisis.add(new modelo.Prestacion(0, 711, "Orina completa", null));
        analisis.add(new modelo.Prestacion(0, 690, "Perfil tiroideo", null));
        return new reportes.ComprobanteOrdenDatos(ejemplo.getNumeroOrden(), new Date(), ejemplo.getNombrePaciente(),
                ejemplo.getDni(), "388 4123456", "ana@ejemplo.com", ejemplo.getMedicoDerivante(), "OBRA_SOCIAL", "OSDE",
                "210", "123456/01", null, null, analisis, new java.math.BigDecimal("12850.50"));
    }

    /** Ventana con todas las hojas de la vista previa en grande. */
    private void ampliarVistaPrevia(java.util.List<javafx.scene.image.Image> paginas) {
        if (paginas.isEmpty()) {
            return;
        }
        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setTitle("Vista previa del informe (datos de ejemplo)");
        VBox hojas = new VBox(18);
        hojas.setAlignment(Pos.TOP_CENTER);
        hojas.setPadding(new Insets(20));
        hojas.setStyle("-fx-background-color: #E4E8E6;");
        for (javafx.scene.image.Image img : paginas) {
            javafx.scene.image.ImageView v = new javafx.scene.image.ImageView(img);
            v.setPreserveRatio(true);
            v.setFitWidth(760);
            StackPane hoja = new StackPane(v);
            hoja.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0.1, 0, 3);");
            hoja.setMaxWidth(Region.USE_PREF_SIZE);
            hojas.getChildren().add(hoja);
        }
        ScrollPane scroll = new ScrollPane(hojas);
        scroll.setFitToWidth(true);
        javafx.scene.Scene escena = vistas.javafx.VentanaUtil.escenaAdaptable(ventana, scroll, 860, 1000);
        escena.getStylesheets().add(vistas.javafx.EstiloApp.hojaDeEstilos());
        ventana.showAndWait();
    }

}
