package herramientas;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;
import modelo.Analito;
import modelo.AnalisisTipo;

/**
 * Herramienta de un solo uso para precargar el Catálogo de Exámenes con un paquete de análisis
 * clínicos estándar (Hemograma, Glucemia, Perfil Lipídico, etc.), cada uno con sus analitos
 * (parámetros) y valores de referencia ya cargados -- para que Zaira no tenga que arrancar el
 * catálogo desde cero, examen por examen, a mano.
 *
 * <p>Se corre UNA sola vez: clic derecho sobre este archivo en NetBeans -&gt; "Run File" (no
 * "Run Project" -- este archivo no se abre desde ningún botón ni pantalla de la app, es aparte).
 * Usa los mismos {@link controlador.AnalisisTipoController} que ya usa {@code ExamenFormDialog},
 * así que lo que crea se ve y se edita después exactamente igual que un examen cargado a mano
 * desde "+ Nuevo Examen". Ojo: si se corre dos veces, crea los mismos exámenes de nuevo,
 * duplicados -- no está pensado para correrse más de una vez tal cual.</p>
 *
 * <p>La lista de acá abajo ({@link #EXAMENES}) es un paquete común de laboratorio clínico, un
 * punto de partida -- no necesariamente igual, examen por examen, a lo que hace San Gregorio en
 * la realidad. Lo que no aplique se desactiva después desde el Catálogo (destildando "Activo" al
 * editarlo), y lo que falte se agrega con "+ Nuevo Examen" como siempre. También se puede editar
 * esta lista directamente (agregar, sacar o cambiar exámenes/analitos) y volver a correr el
 * archivo si todavía no se cargó nada.</p>
 */
public class SeedCatalogoExamenes {

    /** Un examen a precargar, con sus analitos. */
    private static final class ExamenSemilla {

        final String nombre;
        final String categoria;
        final AnalitoSemilla[] analitos;

        ExamenSemilla(String nombre, String categoria, AnalitoSemilla... analitos) {
            this.nombre = nombre;
            this.categoria = categoria;
            this.analitos = analitos;
        }
    }

    /** Un analito (parámetro) a precargar dentro de un examen. */
    private static final class AnalitoSemilla {

        final String nombre;
        final String tipoDato;
        final String unidad;
        final String valorReferencia;

        AnalitoSemilla(String nombre, String tipoDato, String unidad, String valorReferencia) {
            this.nombre = nombre;
            this.tipoDato = tipoDato;
            this.unidad = unidad;
            this.valorReferencia = valorReferencia;
        }
    }

    private static final List<ExamenSemilla> EXAMENES = Arrays.asList(
            new ExamenSemilla("Hemograma Completo", "Hematología",
                    new AnalitoSemilla("Hemoglobina", "numerico", "g/dL", "12.0 - 16.0 g/dL"),
                    new AnalitoSemilla("Hematocrito", "numerico", "%", "36 - 46 %"),
                    new AnalitoSemilla("Glóbulos Blancos", "numerico", "/mm³", "4500 - 11000 /mm³"),
                    new AnalitoSemilla("Glóbulos Rojos", "numerico", "x10⁶/µL", "4.2 - 5.4 x10⁶/µL"),
                    new AnalitoSemilla("Plaquetas", "numerico", "/mm³", "150000 - 450000 /mm³"),
                    new AnalitoSemilla("VSG", "numerico", "mm/h", "0 - 20 mm/h")),

            new ExamenSemilla("Glucemia", "Bioquímica",
                    new AnalitoSemilla("Glucosa", "numerico", "mg/dL", "70 - 110 mg/dL")),

            new ExamenSemilla("Perfil Lipídico", "Bioquímica",
                    new AnalitoSemilla("Colesterol Total", "numerico", "mg/dL", "Menor a 200 mg/dL"),
                    new AnalitoSemilla("Colesterol HDL", "numerico", "mg/dL", "Mayor a 40 mg/dL"),
                    new AnalitoSemilla("Colesterol LDL", "numerico", "mg/dL", "Menor a 130 mg/dL"),
                    new AnalitoSemilla("Triglicéridos", "numerico", "mg/dL", "Menor a 150 mg/dL")),

            new ExamenSemilla("Hepatograma", "Bioquímica",
                    new AnalitoSemilla("GOT (AST)", "numerico", "U/L", "5 - 40 U/L"),
                    new AnalitoSemilla("GPT (ALT)", "numerico", "U/L", "7 - 56 U/L"),
                    new AnalitoSemilla("Fosfatasa Alcalina", "numerico", "U/L", "44 - 147 U/L"),
                    new AnalitoSemilla("Bilirrubina Total", "numerico", "mg/dL", "0.3 - 1.2 mg/dL")),

            new ExamenSemilla("Función Renal (Urea y Creatinina)", "Bioquímica",
                    new AnalitoSemilla("Urea", "numerico", "mg/dL", "10 - 50 mg/dL"),
                    new AnalitoSemilla("Creatinina", "numerico", "mg/dL", "0.6 - 1.3 mg/dL")),

            new ExamenSemilla("Orina Completa", "Orina",
                    new AnalitoSemilla("Color", "cualitativo", null, "Amarillo"),
                    new AnalitoSemilla("Densidad", "numerico", null, "1.005 - 1.030"),
                    new AnalitoSemilla("pH", "numerico", null, "5.0 - 8.0"),
                    new AnalitoSemilla("Proteínas", "cualitativo", null, "Negativo"),
                    new AnalitoSemilla("Glucosa en orina", "cualitativo", null, "Negativo"),
                    new AnalitoSemilla("Sedimento", "texto", null, "Sin elementos patológicos")),

            new ExamenSemilla("Perfil Tiroideo", "Endocrinología",
                    new AnalitoSemilla("TSH", "numerico", "µUI/mL", "0.4 - 4.0 µUI/mL"),
                    new AnalitoSemilla("T4 Libre", "numerico", "ng/dL", "0.8 - 1.8 ng/dL")),

            new ExamenSemilla("Coagulograma", "Hematología",
                    new AnalitoSemilla("Tiempo de Protrombina", "numerico", "seg", "11 - 14 seg"),
                    new AnalitoSemilla("KPTT", "numerico", "seg", "25 - 35 seg")),

            new ExamenSemilla("Grupo y Factor", "Hematología",
                    new AnalitoSemilla("Grupo Sanguíneo", "cualitativo", null, "A, B, AB o O"),
                    new AnalitoSemilla("Factor Rh", "cualitativo", null, "Positivo o Negativo")),

            new ExamenSemilla("VDRL", "Serología",
                    new AnalitoSemilla("VDRL", "cualitativo", null, "No reactivo"))
    );

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(SeedCatalogoExamenes::cargarTodo);
    }

    private static void cargarTodo() {
        int examenesCreados = 0;
        int analitosCreados = 0;

        for (ExamenSemilla examenSemilla : EXAMENES) {
            AnalisisTipo tipo = new AnalisisTipo();
            tipo.setNombreAnalisis(examenSemilla.nombre);
            tipo.setCategoria(examenSemilla.categoria);

            Integer idExamen = controlador.AnalisisTipoController.crear(null, tipo);
            if (idExamen == null) {
                System.out.println("No se pudo crear: " + examenSemilla.nombre);
                continue;
            }
            examenesCreados++;

            int orden = 1;
            for (AnalitoSemilla analitoSemilla : examenSemilla.analitos) {
                Analito analito = new Analito();
                analito.setIdAnalisisTipo(idExamen);
                analito.setNombreAnalito(analitoSemilla.nombre);
                analito.setTipoDato(analitoSemilla.tipoDato);
                analito.setUnidad(analitoSemilla.unidad);
                analito.setValorReferencia(analitoSemilla.valorReferencia);
                analito.setOrdenAnalito(orden++);

                if (controlador.AnalisisTipoController.crearAnalito(null, analito) != null) {
                    analitosCreados++;
                }
            }
        }

        System.out.println("Listo: " + examenesCreados + " examenes y " + analitosCreados + " analitos cargados.");
        System.out.println("Ahora se pueden ver y editar en Catalogo de Examenes, dentro de la app.");
    }
}
