package vistas.javafx.comunes;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import reportes.DisenoInforme;

/**
 * Los controles para cambiar el diseño del informe PDF (posiciones, columnas, márgenes, letra),
 * pensados para que se entiendan sin explicación: cada grupo tiene un título, una ayuda corta, y
 * los cambios se ven al instante en la vista previa que tenga al lado.
 *
 * <p>Se usa en dos lugares: Configuración &gt; Informes PDF (diseño para todas las órdenes) y la
 * vista previa de cada orden (para ajustar solo esa).</p>
 */
public class EditorDisenoPanel {

    private DisenoInforme diseno;
    private final Consumer<DisenoInforme> alCambiar;
    private boolean actualizando;

    private final VBox vista = new VBox(16);

    private Slider sliderLetra;
    private Label valorLetra;
    private ToggleGroup grupoEspaciado;
    private Slider sliderDet;
    private Slider sliderRes;
    private Slider sliderRef;
    private Label valorDet;
    private Label valorRes;
    private Label valorRef;
    private Label valorEst;
    private final javafx.scene.layout.GridPane barraColumnas = new javafx.scene.layout.GridPane();
    private Slider sliderMargenSup;
    private Slider sliderMargenLat;
    private Slider sliderMargenInf;
    private Label valorMargenSup;
    private Label valorMargenLat;
    private Label valorMargenInf;
    private CheckBox chkMembretada;
    private ToggleGroup grupoEncabezado;
    private ToggleGroup grupoLogo;
    private ToggleGroup grupoFirma;
    private ToggleGroup grupoAlturaFirma;
    private ToggleGroup grupoTabla;
    private ToggleGroup grupoColor;
    private CheckBox chkHojaPorEstudio;
    private ToggleGroup grupoEstilo;
    private ToggleGroup grupoHoja;
    private ToggleGroup grupoColumnasClasico;
    private CheckBox chkFirma;
    private CheckBox chkRepetir;
    private Node cajaColumnas;
    private Node filaColumnasClasico;
    private Node filaTabla;
    private Node filaColor;

    /**
     * @param alCambiar se llama cada vez que se toca algo, con una copia del diseño nuevo (para
     *                  redibujar la vista previa).
     */
    public EditorDisenoPanel(DisenoInforme inicial, Consumer<DisenoInforme> alCambiar) {
        this.diseno = inicial == null ? DisenoInforme.deFabrica() : inicial.copia();
        this.alCambiar = alCambiar;
        armar();
        setDiseno(this.diseno);
    }

    public Node getVista() {
        return vista;
    }

    public DisenoInforme getDiseno() {
        return diseno.copia();
    }

    /** Pone los controles con los valores de {@code nuevo} (sin avisar como si fuera un cambio). */
    public final void setDiseno(DisenoInforme nuevo) {
        diseno = nuevo == null ? DisenoInforme.deFabrica() : nuevo.copia();
        actualizando = true;
        seleccionar(grupoEstilo, diseno.getEstilo());
        seleccionar(grupoHoja, diseno.getHoja());
        seleccionar(grupoColumnasClasico, diseno.getColumnasClasico());
        chkFirma.setSelected(diseno.isMostrarFirma());
        chkRepetir.setSelected(diseno.isRepetirDatos());
        sliderLetra.setValue(diseno.getTamanoLetra());
        seleccionar(grupoEspaciado, diseno.getEspaciado());
        sliderDet.setValue(diseno.getColDeterminacion());
        sliderRes.setValue(diseno.getColResultado());
        sliderRef.setValue(diseno.getColReferencia());
        sliderMargenSup.setValue(diseno.getMargenSuperiorMm());
        sliderMargenLat.setValue(diseno.getMargenLateralMm());
        sliderMargenInf.setValue(diseno.getMargenInferiorMm());
        chkMembretada.setSelected(!diseno.isMostrarEncabezado());
        seleccionar(grupoEncabezado, diseno.getAlineacionEncabezado());
        seleccionar(grupoLogo, diseno.getPosicionLogo());
        seleccionar(grupoFirma, diseno.getPosicionFirma());
        seleccionar(grupoAlturaFirma, diseno.isFirmaAlPie());
        seleccionar(grupoTabla, diseno.getEstiloTabla());
        seleccionar(grupoColor, diseno.getColorAcento());
        chkHojaPorEstudio.setSelected(diseno.isEstudioEnHojaNueva());
        actualizarEtiquetas();
        actualizando = false;
    }

    // ------------------------------------------------------------------ armado

    private void armar() {
        // 0) Estilo general
        grupoEstilo = new ToggleGroup();
        grupoHoja = new ToggleGroup();
        grupoColumnasClasico = new ToggleGroup();
        Integer[] opcionesColumnas = {1, 2};
        filaColumnasClasico = fila("Columnas", segmentos(grupoColumnasClasico, opcionesColumnas,
                n -> n == 1 ? "Una" : "Dos", n -> cambio(() -> diseno.setColumnasClasico(n))), null);
        vista.getChildren().add(grupo("Estilo del informe",
                "\"Clásico\" es como el informe que el laboratorio arma en Word: letra clásica, resultados en "
                        + "columnas con \"VR:\" al lado. \"Moderno\" lo arma en una tabla con la columna Estado.",
                fila("Estilo", segmentos(grupoEstilo, DisenoInforme.Estilo.values(), e -> e.etiqueta,
                        e -> cambio(() -> diseno.setEstilo(e))), null),
                fila("Tamaño de hoja", segmentos(grupoHoja, DisenoInforme.Hoja.values(), h -> h.etiqueta,
                        h -> cambio(() -> diseno.setHoja(h))), null),
                filaColumnasClasico));

        // 1) Letra y filas
        sliderLetra = slider(DisenoInforme.LETRA_MIN, DisenoInforme.LETRA_MAX, 0.5);
        valorLetra = valor();
        sliderLetra.valueProperty().addListener((o, a, n) -> cambio(() -> diseno.setTamanoLetra(n.doubleValue())));
        grupoEspaciado = new ToggleGroup();
        Node espaciado = segmentos(grupoEspaciado, DisenoInforme.Espaciado.values(), e -> e.etiqueta,
                e -> cambio(() -> diseno.setEspaciado(e)));
        vista.getChildren().add(grupo("Letra y filas de la tabla",
                "Si el informe queda muy apretado o muy largo, probá cambiando el tamaño de letra o el espacio entre filas.",
                fila("Tamaño de letra", sliderLetra, valorLetra),
                fila("Espacio entre filas", espaciado, null)));

        // 2) Columnas
        sliderDet = slider(DisenoInforme.COLUMNA_MIN, 70, 1);
        sliderRes = slider(DisenoInforme.COLUMNA_MIN, 60, 1);
        sliderRef = slider(DisenoInforme.COLUMNA_MIN, 70, 1);
        valorDet = valor();
        valorRes = valor();
        valorRef = valor();
        valorEst = valor();
        sliderDet.valueProperty().addListener((o, a, n) -> cambioColumnas());
        sliderRes.valueProperty().addListener((o, a, n) -> cambioColumnas());
        sliderRef.valueProperty().addListener((o, a, n) -> cambioColumnas());
        barraColumnas.setPrefHeight(26);
        barraColumnas.setMinWidth(0);
        barraColumnas.setMaxWidth(Double.MAX_VALUE);
        Label estadoTexto = new Label("Estado (lo que sobra)");
        estadoTexto.getStyleClass().add("etiqueta-campo");
        HBox filaEstado = new HBox(10, estadoTexto, espaciador(), valorEst);
        filaEstado.setAlignment(Pos.CENTER_LEFT);
        cajaColumnas = grupo("Ancho de las columnas (estilo Moderno)",
                "Cuánto ocupa cada columna de la tabla. La barra muestra cómo queda repartida la hoja. "
                        + "Los textos largos no se cortan: si no entran, bajan al renglón de abajo.",
                barraColumnas,
                fila("Determinación", sliderDet, valorDet),
                fila("Resultado", sliderRes, valorRes),
                fila("Valores de referencia", sliderRef, valorRef),
                filaEstado);
        vista.getChildren().add(cajaColumnas);

        // 3) Márgenes
        sliderMargenSup = slider(6, 80, 1);
        sliderMargenLat = slider(8, 30, 1);
        sliderMargenInf = slider(6, 50, 1);
        valorMargenSup = valor();
        valorMargenLat = valor();
        valorMargenInf = valor();
        sliderMargenSup.valueProperty().addListener((o, a, n) -> cambio(() -> diseno.setMargenSuperiorMm(n.doubleValue())));
        sliderMargenLat.valueProperty().addListener((o, a, n) -> cambio(() -> diseno.setMargenLateralMm(n.doubleValue())));
        sliderMargenInf.valueProperty().addListener((o, a, n) -> cambio(() -> diseno.setMargenInferiorMm(n.doubleValue())));
        chkMembretada = new CheckBox("Imprimo en hoja membretada (no dibujar el encabezado del laboratorio)");
        chkMembretada.setWrapText(true);
        chkMembretada.setOnAction(e -> cambio(() -> diseno.setMostrarEncabezado(!chkMembretada.isSelected())));
        vista.getChildren().add(grupo("Márgenes de la hoja (en milímetros)",
                "Si usás hojas con el membrete ya impreso, tildá la opción de abajo y subí el margen superior "
                        + "hasta que el informe empiece debajo del membrete.",
                fila("Arriba", sliderMargenSup, valorMargenSup),
                fila("A los costados", sliderMargenLat, valorMargenLat),
                fila("Abajo", sliderMargenInf, valorMargenInf),
                chkMembretada));

        // 4) Ubicaciones
        grupoEncabezado = new ToggleGroup();
        grupoLogo = new ToggleGroup();
        grupoFirma = new ToggleGroup();
        grupoAlturaFirma = new ToggleGroup();
        Boolean[] alturas = {Boolean.FALSE, Boolean.TRUE};
        DisenoInforme.Alineacion[] dosLados = {DisenoInforme.Alineacion.IZQUIERDA, DisenoInforme.Alineacion.DERECHA};
        DisenoInforme.Alineacion[] izqCentro = {DisenoInforme.Alineacion.IZQUIERDA, DisenoInforme.Alineacion.CENTRO};
        vista.getChildren().add(grupo("Dónde va cada cosa",
                "Con la firma \"debajo de los resultados\", si sobra hoja se puede cortar sin perder la firma.",
                fila("Nombre del laboratorio", segmentos(grupoEncabezado, izqCentro, a -> a.etiqueta,
                        a -> cambio(() -> diseno.setAlineacionEncabezado(a))), null),
                fila("Logo", segmentos(grupoLogo, DisenoInforme.Alineacion.values(), a -> a.etiqueta,
                        a -> cambio(() -> diseno.setPosicionLogo(a))), null),
                fila("Firma", segmentos(grupoFirma, DisenoInforme.Alineacion.values(), a -> a.etiqueta,
                        a -> cambio(() -> diseno.setPosicionFirma(a))), null),
                fila("Altura de la firma", segmentos(grupoAlturaFirma, alturas,
                        b -> b ? "Al pie de la hoja" : "Debajo de los resultados",
                        b -> cambio(() -> diseno.setFirmaAlPie(b))), null),
                chkFirma = checkFirma()));

        // 5) Estilo
        grupoTabla = new ToggleGroup();
        grupoColor = new ToggleGroup();
        chkHojaPorEstudio = new CheckBox("Empezar cada estudio en una hoja nueva");
        chkHojaPorEstudio.setOnAction(e -> cambio(() -> diseno.setEstudioEnHojaNueva(chkHojaPorEstudio.isSelected())));
        filaTabla = fila("Tabla (Moderno)", segmentos(grupoTabla, DisenoInforme.EstiloTabla.values(), t -> t.etiqueta,
                t -> cambio(() -> diseno.setEstiloTabla(t))), null);
        filaColor = fila("Color (Moderno)", segmentos(grupoColor, DisenoInforme.ColorAcento.values(), c -> c.etiqueta,
                c -> cambio(() -> diseno.setColorAcento(c))), null);
        chkRepetir = new CheckBox("Si ocupa más de una hoja, repetir el encabezado y los datos del paciente en cada hoja");
        chkRepetir.setWrapText(true);
        chkRepetir.setOnAction(e -> cambio(() -> diseno.setRepetirDatos(chkRepetir.isSelected())));
        vista.getChildren().add(grupo("Otros detalles",
                "Con los datos repetidos, si se imprimen y se separan las hojas, cada una sigue diciendo de quién es "
                        + "(y abajo dice \"Página 1 de 2\", \"Página 2 de 2\").",
                chkRepetir, filaTabla, filaColor, chkHojaPorEstudio));

        Button fabrica = new Button("Volver al diseño de fábrica");
        fabrica.getStyleClass().add("boton-secundario");
        fabrica.setOnAction(e -> {
            setDiseno(DisenoInforme.deFabrica());
            avisar();
        });
        vista.getChildren().add(fabrica);
    }

    // ------------------------------------------------------------------ cambios

    private void cambio(Runnable aplicar) {
        if (actualizando) {
            return;
        }
        aplicar.run();
        actualizarEtiquetas();
        avisar();
    }

    private void cambioColumnas() {
        if (actualizando) {
            return;
        }
        diseno.setColumnas((int) Math.round(sliderDet.getValue()), (int) Math.round(sliderRes.getValue()),
                (int) Math.round(sliderRef.getValue()));
        // Si el reparto no daba (no queda lugar para "Estado"), DisenoInforme lo ajusta: se
        // reflejan los valores ajustados en los controles.
        actualizando = true;
        sliderDet.setValue(diseno.getColDeterminacion());
        sliderRes.setValue(diseno.getColResultado());
        sliderRef.setValue(diseno.getColReferencia());
        actualizando = false;
        actualizarEtiquetas();
        avisar();
    }

    private void avisar() {
        if (alCambiar != null) {
            alCambiar.accept(diseno.copia());
        }
    }

    private CheckBox checkFirma() {
        CheckBox chk = new CheckBox("Dibujar la línea de firma (sacala si firmás o sellás a mano)");
        chk.setWrapText(true);
        chk.setOnAction(e -> cambio(() -> diseno.setMostrarFirma(chk.isSelected())));
        return chk;
    }

    private void actualizarEtiquetas() {
        // Lo que solo aplica a un estilo se apaga (gris) en el otro, para que no confunda.
        boolean clasico = diseno.getEstilo() == DisenoInforme.Estilo.CLASICO;
        if (cajaColumnas != null) {
            cajaColumnas.setDisable(clasico);
            filaTabla.setDisable(clasico);
            filaColor.setDisable(clasico);
            filaColumnasClasico.setDisable(!clasico);
        }
        valorLetra.setText(String.format(java.util.Locale.ROOT, "%.1f pt", diseno.getTamanoLetra()).replace(".0 ", " "));
        valorDet.setText(diseno.getColDeterminacion() + " %");
        valorRes.setText(diseno.getColResultado() + " %");
        valorRef.setText(diseno.getColReferencia() + " %");
        valorEst.setText(diseno.getColEstado() + " %");
        valorMargenSup.setText(Math.round(diseno.getMargenSuperiorMm()) + " mm");
        valorMargenLat.setText(Math.round(diseno.getMargenLateralMm()) + " mm");
        valorMargenInf.setText(Math.round(diseno.getMargenInferiorMm()) + " mm");
        dibujarBarra();
    }

    /** Barrita con las 4 columnas a escala, para ver de un vistazo cómo queda repartida la hoja. */
    private void dibujarBarra() {
        barraColumnas.getChildren().clear();
        barraColumnas.getColumnConstraints().clear();
        int[] porcentajes = {diseno.getColDeterminacion(), diseno.getColResultado(), diseno.getColReferencia(),
            diseno.getColEstado()};
        String[] textos = {"Determ.", "Resultado", "Referencia", "Estado"};
        String[] colores = {"#1E5C3D", "#2C9463", "#6BB68F", "#A9D3BC"};
        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
            col.setPercentWidth(porcentajes[i]);
            barraColumnas.getColumnConstraints().add(col);
            Label etiqueta = new Label(textos[i]);
            etiqueta.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
            etiqueta.setMinWidth(0);
            StackPane caja = new StackPane(etiqueta);
            caja.setStyle("-fx-background-color: " + colores[i] + ";");
            caja.setMinWidth(0);
            caja.setPrefHeight(26);
            caja.setMaxWidth(Double.MAX_VALUE);
            barraColumnas.add(caja, i, 0);
        }
    }

    // ------------------------------------------------------------------ piezas visuales

    private VBox grupo(String titulo, String ayuda, Node... contenido) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-grupo-diseno");
        VBox caja = new VBox(8, lblTitulo);
        if (ayuda != null) {
            Label lblAyuda = new Label(ayuda);
            lblAyuda.getStyleClass().add("ayuda-diseno");
            lblAyuda.setWrapText(true);
            caja.getChildren().add(lblAyuda);
        }
        caja.getChildren().addAll(contenido);
        caja.setPadding(new Insets(12));
        caja.setStyle("-fx-background-color: #F6F8F7; -fx-background-radius: 12;");
        return caja;
    }

    private HBox fila(String etiqueta, Node control, Label valor) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("etiqueta-campo");
        lbl.setMinWidth(130);
        lbl.setPrefWidth(130);
        lbl.setWrapText(true);
        HBox fila = new HBox(10, lbl, control);
        if (control instanceof Slider) {
            HBox.setHgrow(control, Priority.ALWAYS);
        }
        if (valor != null) {
            fila.getChildren().add(valor);
        }
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private static Slider slider(double min, double max, double paso) {
        Slider s = new Slider(min, max, min);
        s.setBlockIncrement(paso);
        s.setMajorTickUnit(paso);
        s.setMinorTickCount(0);
        s.setSnapToTicks(true);
        s.setMaxWidth(Double.MAX_VALUE);
        s.setMinWidth(120);
        return s;
    }

    private static Label valor() {
        Label l = new Label();
        l.setMinWidth(52);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #141C19; -fx-font-size: 12px;");
        return l;
    }

    private static Region espaciador() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Fila de botones "pestañita" (uno solo elegido a la vez). */
    private static <E> HBox segmentos(ToggleGroup grupo, E[] valores, Function<E, String> etiqueta, Consumer<E> alElegir) {
        HBox caja = new HBox(0);
        for (int i = 0; i < valores.length; i++) {
            E valor = valores[i];
            ToggleButton boton = new ToggleButton(etiqueta.apply(valor));
            boton.setToggleGroup(grupo);
            boton.setUserData(valor);
            boton.getStyleClass().add("segmento");
            if (i == 0) {
                boton.getStyleClass().add("segmento-primero");
            }
            if (i == valores.length - 1) {
                boton.getStyleClass().add("segmento-ultimo");
            }
            boton.setOnAction(e -> alElegir.accept(valor));
            caja.getChildren().add(boton);
        }
        // Que nunca quede ninguno sin elegir (si se vuelve a tocar el elegido, sigue elegido).
        grupo.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
        });
        return caja;
    }

    private static void seleccionar(ToggleGroup grupo, Object valor) {
        grupo.getToggles().forEach(t -> {
            if (valor != null && valor.equals(t.getUserData())) {
                t.setSelected(true);
            }
        });
    }
}
