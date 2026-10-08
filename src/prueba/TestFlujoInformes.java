package prueba;

import conexiones.Conexion;
import dao.AnalisisTipoDAO;
import dao.AnalitoDAO;
import dao.PacienteDAO;
import dao.PedidoAnalitoResultadoDAO;
import dao.PedidoDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import modelo.Analito;
import modelo.AnalisisTipo;
import modelo.Paciente;
import modelo.Parametro;
import modelo.PedidoCreado;
import modelo.TramoTexto;
import utilidades.TramosTextoUtil;

/**
 * Prueba de punta a punta (sin interfaz gráfica) del flujo de "salida de información": crear un
 * examen en el Catálogo de Exámenes (con sus analitos y valores de referencia, incluyendo un
 * tramo con color, para probar también esa parte de Mes 7) → crear un paciente de prueba →
 * generar un pedido (como hace "Nuevo Análisis" al presionar "Generar Orden") → cargar los
 * resultados (como hace "Registrar Resultados") → armar los datos que usaría el informe/PDF,
 * calculando el estado (Normal/Alto/Bajo) de cada valor igual que
 * {@link controlador.ResultadosController#obtenerParaImprimir}.
 *
 * <p><b>Importante:</b> a propósito NO usa {@code controlador.*} ni
 * {@code controlador.ConexionUtil} -- esas clases muestran un {@code JOptionPane} (ventana
 * emergente) si algo falla, y como esto se corre solo por consola (sin que haya nadie mirando la
 * pantalla para cerrar esa ventana) preferí llamar directo a los DAO con una Connection propia,
 * así cualquier error queda solo impreso por consola y el programa sigue/termina solo.</p>
 *
 * <p>Al final borra todo lo que creó (examen, analitos, paciente, pedido y resultados de
 * prueba), así se puede correr las veces que haga falta sin ir juntando basura en la base real.
 * Si algún paso falla a mitad de camino, igual intenta borrar lo que sí se llegó a crear.</p>
 *
 * <p><b>Cómo correrlo:</b> en NetBeans, clic derecho sobre este archivo → "Run File" (o Shift+F6).
 * Mirá la salida en la pestaña "Output" y fijate que todos los pasos digan "OK" -- si alguno dice
 * "FALLÓ", copiá el mensaje y lo revisamos.</p>
 */
public class TestFlujoInformes {

    private static int pasosOk = 0;
    private static int pasosFallidos = 0;

    // Mismas expresiones regulares que controlador.ResultadosController.calcularEstado -- se
    // copian acá (no se pueden reusar porque ese método es privado) para verificar, de forma
    // independiente, que el cálculo de Normal/Alto/Bajo da lo que se espera.
    private static final Pattern PATRON_NUMERICO = Pattern.compile("-?\\d+([.,]\\d+)?");
    private static final Pattern PATRON_RANGO =
            Pattern.compile("(-?\\d+(?:[.,]\\d+)?)\\s*-\\s*(-?\\d+(?:[.,]\\d+)?)");
    private static final Pattern PATRON_MAXIMO = Pattern.compile("[<≤]=?\\s*(-?\\d+(?:[.,]\\d+)?)");
    private static final Pattern PATRON_MINIMO = Pattern.compile("[>≥]=?\\s*(-?\\d+(?:[.,]\\d+)?)");

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println(" TEST: Catálogo de Exámenes -> Pedido -> Resultados -> Informe");
        System.out.println("=========================================================");

        Connection con = Conexion.conectar();
        if (con == null) {
            System.out.println("FALLÓ: no se pudo conectar a la base de datos. Fijate que MySQL "
                    + "esté corriendo y que los datos de conexión en conexiones.Conexion sean correctos.");
            return;
        }
        System.out.println("OK: conexión a la base establecida.");

        Integer idAnalisisTipo = null;
        Integer idAnalito1 = null;
        Integer idAnalito2 = null;
        Integer idPaciente = null;
        Integer idPedido = null;
        Integer idPedidoAnalisis = null;

        try {
            // ---------- 1) Catálogo de Exámenes: crear un examen de prueba ----------
            System.out.println("\n--- 1) Catálogo de Exámenes: crear examen ---");
            AnalisisTipo tipo = new AnalisisTipo();
            tipo.setNombreAnalisis("ZZZ_TEST_CLAUDE - Hemograma de prueba");
            tipo.setCategoria("Prueba automática");
            idAnalisisTipo = AnalisisTipoDAO.crear(con, tipo);
            paso(idAnalisisTipo != null, "Crear examen en el catálogo",
                    "id_analisis_tipo generado = " + idAnalisisTipo);

            if (idAnalisisTipo == null) {
                throw new AbortarPrueba("No se pudo crear el examen de prueba, no tiene sentido seguir.");
            }

            // Analito 1: referencia simple, en texto plano.
            Analito hemoglobina = new Analito();
            hemoglobina.setIdAnalisisTipo(idAnalisisTipo);
            hemoglobina.setNombreAnalito("Hemoglobina");
            hemoglobina.setTipoDato("numerico");
            hemoglobina.setUnidad("g/dL");
            hemoglobina.setOrdenAnalito(1);
            hemoglobina.setValorReferencia("12-16");
            idAnalito1 = AnalitoDAO.crear(con, hemoglobina);
            paso(idAnalito1 != null, "Crear analito \"Hemoglobina\" (referencia simple \"12-16\")",
                    "id_analito generado = " + idAnalito1);

            // Analito 2: referencia con un tramo de color (Mes 7) -- el texto sigue siendo un
            // rango válido ("4.5-11.0") para que el cálculo de Normal/Alto/Bajo funcione igual;
            // lo que cambia es que se guarda con el marcador de color de TramosTextoUtil.
            String referenciaConColor = TramosTextoUtil.serializar(
                    java.util.Collections.singletonList(new TramoTexto("4.5-11.0", "2563EB")));
            Analito globulosBlancos = new Analito();
            globulosBlancos.setIdAnalisisTipo(idAnalisisTipo);
            globulosBlancos.setNombreAnalito("Glóbulos Blancos");
            globulosBlancos.setTipoDato("numerico");
            globulosBlancos.setUnidad("mil/µL");
            globulosBlancos.setOrdenAnalito(2);
            globulosBlancos.setValorReferencia(referenciaConColor);
            idAnalito2 = AnalitoDAO.crear(con, globulosBlancos);
            paso(idAnalito2 != null, "Crear analito \"Glóbulos Blancos\" (referencia con color, azul)",
                    "id_analito generado = " + idAnalito2);

            // Releer del catálogo y verificar que separó bien texto plano vs. tramos de color.
            List<Parametro> parametrosCatalogo = AnalitoDAO.listarConReferencia(con, idAnalisisTipo, 0);
            paso(parametrosCatalogo.size() == 2, "Releer analitos del examen desde el catálogo",
                    "se esperaban 2, se encontraron " + parametrosCatalogo.size());

            Parametro paramGlobulos = buscarPorId(parametrosCatalogo, idAnalito2);
            boolean colorOk = paramGlobulos != null
                    && "4.5-11.0".equals(paramGlobulos.getValorReferencia())
                    && paramGlobulos.getTramosReferencia().size() == 1
                    && "2563EB".equals(paramGlobulos.getTramosReferencia().get(0).getColorHex());
            paso(colorOk, "Separar correctamente texto plano y tramo de color (TramosTextoUtil)",
                    paramGlobulos == null ? "no se encontró el parámetro"
                            : "texto plano=\"" + paramGlobulos.getValorReferencia() + "\", tramos="
                                    + paramGlobulos.getTramosReferencia().size());

            // ---------- 2) Nuevo Análisis: paciente de prueba + generar pedido ----------
            System.out.println("\n--- 2) Nuevo Análisis: paciente de prueba y generar pedido ---");
            Integer idSexo = obtenerUnIdSexo(con);
            paso(idSexo != null, "Obtener un id_sexo válido de la tabla `sexos`",
                    "id_sexo = " + idSexo);
            if (idSexo == null) {
                throw new AbortarPrueba("No hay ningún sexo cargado en la tabla `sexos`, no se puede crear el paciente de prueba.");
            }

            Paciente paciente = new Paciente();
            paciente.setNyaPaciente("ZZZ_TEST_CLAUDE, Paciente de Prueba");
            paciente.setDni("00000000");
            Calendar nacimiento = Calendar.getInstance();
            nacimiento.set(1990, Calendar.JANUARY, 1);
            paciente.setFechaNacimiento(nacimiento.getTime());
            paciente.setIdSexo(idSexo);
            paciente.setTelefono("0000000000");
            paciente.setEmail("");
            idPaciente = PacienteDAO.insertar(con, paciente);
            paso(idPaciente != null, "Crear paciente de prueba", "id_paciente generado = " + idPaciente);

            if (idPaciente == null) {
                throw new AbortarPrueba("No se pudo crear el paciente de prueba, no tiene sentido seguir.");
            }

            Integer idUsuario = obtenerUnIdUsuario(con);
            paso(idUsuario != null, "Obtener un id_usuario válido de la tabla `usuarios` (para \"registrado por\")",
                    "id_usuario = " + idUsuario);
            if (idUsuario == null) {
                throw new AbortarPrueba("No hay ningún usuario cargado, no se puede generar el pedido (id_registrado_por).");
            }

            PedidoCreado pedido = PedidoDAO.crearPedido(con, idPaciente, null, idUsuario);
            paso(pedido != null, "Generar pedido (\"Generar Orden\")",
                    pedido == null ? null : "numero_pedido = " + pedido.getNumeroPedido());
            if (pedido == null) {
                throw new AbortarPrueba("No se pudo generar el pedido, no tiene sentido seguir.");
            }
            idPedido = pedido.getIdPedido();

            boolean analisisAgregado = PedidoDAO.agregarAnalisis(con, idPedido, idAnalisisTipo);
            paso(analisisAgregado, "Agregar el examen de prueba al pedido (pedido_analisis)", null);
            if (!analisisAgregado) {
                throw new AbortarPrueba("No se pudo agregar el examen al pedido, no tiene sentido seguir.");
            }

            idPedidoAnalisis = obtenerIdPedidoAnalisis(con, idPedido, idAnalisisTipo);
            paso(idPedidoAnalisis != null, "Recuperar el id_pedido_analisis generado",
                    "id_pedido_analisis = " + idPedidoAnalisis);
            if (idPedidoAnalisis == null) {
                throw new AbortarPrueba("No se pudo recuperar el id_pedido_analisis, no tiene sentido seguir.");
            }

            // ---------- 3) Registrar Resultados: cargar valores ----------
            System.out.println("\n--- 3) Registrar Resultados: cargar valores ---");
            Map<Integer, String> valoresACargar = new LinkedHashMap<>();
            valoresACargar.put(idAnalito1, "14.2"); // dentro de "12-16" -> Normal
            valoresACargar.put(idAnalito2, "18.5"); // fuera de "4.5-11.0" -> Alto

            boolean guardado = PedidoAnalitoResultadoDAO.guardarTodos(con, idPedidoAnalisis, valoresACargar);
            paso(guardado, "Guardar los resultados cargados (pedido_analito_resultado)", null);

            boolean estadoActualizado = PedidoAnalitoResultadoDAO.actualizarEstado(con, idPedidoAnalisis, "completado");
            paso(estadoActualizado, "Marcar el examen como \"completado\"", null);

            Map<Integer, String> valoresGuardados = PedidoAnalitoResultadoDAO.listarResultados(con, idPedidoAnalisis);
            boolean valoresOk = "14.2".equals(valoresGuardados.get(idAnalito1))
                    && "18.5".equals(valoresGuardados.get(idAnalito2));
            paso(valoresOk, "Releer los resultados guardados y verificar los valores",
                    "leído: " + valoresGuardados);

            // ---------- 4) Armar los datos para el informe/PDF ----------
            System.out.println("\n--- 4) Armar los datos para el informe (como hace ResultadosController.obtenerParaImprimir) ---");
            List<Parametro> parametrosInforme = AnalitoDAO.listarConReferencia(con, idAnalisisTipo, 0);

            System.out.println(String.format("  %-20s %-8s %-10s %-12s %-8s", "Analito", "Valor", "Unidad", "Referencia", "Estado"));
            boolean hemoglobinaOk = false;
            boolean globulosOk = false;
            for (Parametro p : parametrosInforme) {
                String valor = valoresGuardados.get(p.getIdParametro());
                String estado = calcularEstado(valor, p.getValorReferencia());
                String colorInfo = p.getTramosReferencia().isEmpty() ? ""
                        : " (color: " + p.getTramosReferencia().get(0).getColorHex() + ")";
                System.out.println(String.format("  %-20s %-8s %-10s %-12s %-8s%s",
                        p.getNombreParametro(), valor, p.getUnidad(), p.getValorReferencia(), estado, colorInfo));

                if (p.getIdParametro() == idAnalito1) {
                    hemoglobinaOk = "Normal".equals(estado);
                }
                if (p.getIdParametro() == idAnalito2) {
                    globulosOk = "Alto".equals(estado);
                }
            }
            paso(hemoglobinaOk, "Calcular estado de \"Hemoglobina\" (14.2 dentro de 12-16 -> se espera \"Normal\")", null);
            paso(globulosOk, "Calcular estado de \"Glóbulos Blancos\" (18.5 fuera de 4.5-11.0 -> se espera \"Alto\")", null);

        } catch (AbortarPrueba e) {
            System.out.println("\n(Se interrumpió la prueba antes de tiempo: " + e.getMessage() + ")");
        } catch (Exception e) {
            System.out.println("\nFALLÓ con una excepción inesperada: " + e);
            e.printStackTrace();
            pasosFallidos++;
        } finally {
            System.out.println("\n--- Limpieza: borrando todo lo que creó esta prueba ---");
            limpiar(con, idPedidoAnalisis, idPedido, idPaciente, idAnalito1, idAnalito2, idAnalisisTipo);
            cerrar(con);
        }

        System.out.println("\n=========================================================");
        System.out.println(" RESULTADO: " + pasosOk + " paso(s) OK, " + pasosFallidos + " paso(s) FALLARON.");
        System.out.println("=========================================================");
    }

    /** Excepción interna, solo para cortar la prueba cuando un paso previo es indispensable. */
    private static class AbortarPrueba extends RuntimeException {
        AbortarPrueba(String mensaje) {
            super(mensaje);
        }
    }

    private static void paso(boolean ok, String descripcion, String detalle) {
        if (ok) {
            pasosOk++;
            System.out.println("  [OK]     " + descripcion + (detalle != null ? " -- " + detalle : ""));
        } else {
            pasosFallidos++;
            System.out.println("  [FALLÓ]  " + descripcion + (detalle != null ? " -- " + detalle : ""));
        }
    }

    private static Parametro buscarPorId(List<Parametro> parametros, Integer idAnalito) {
        if (idAnalito == null) {
            return null;
        }
        for (Parametro p : parametros) {
            if (p.getIdParametro() == idAnalito) {
                return p;
            }
        }
        return null;
    }

    private static Integer obtenerUnIdSexo(Connection con) {
        String sql = "SELECT id_sexo FROM sexos ORDER BY id_sexo LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : null;
        } catch (SQLException e) {
            System.out.println("  (error al buscar un id_sexo: " + e.getMessage() + ")");
            return null;
        }
    }

    private static Integer obtenerUnIdUsuario(Connection con) {
        String sql = "SELECT id_usuario FROM usuarios ORDER BY id_usuario LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : null;
        } catch (SQLException e) {
            System.out.println("  (error al buscar un id_usuario: " + e.getMessage() + ")");
            return null;
        }
    }

    private static Integer obtenerIdPedidoAnalisis(Connection con, int idPedido, int idAnalisisTipo) {
        String sql = "SELECT id_pedido_analisis FROM pedido_analisis "
                + "WHERE id_pedido = ? AND id_analisis_tipo = ? ORDER BY id_pedido_analisis DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.setInt(2, idAnalisisTipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            System.out.println("  (error al buscar el id_pedido_analisis: " + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Mismo cálculo de Normal/Alto/Bajo que {@code controlador.ResultadosController.calcularEstado}
     * (copiado porque ese método es privado), para poder verificarlo de forma independiente.
     */
    private static String calcularEstado(String valorTexto, String referencia) {
        if (valorTexto == null || referencia == null) {
            return null;
        }
        String valorLimpio = valorTexto.trim();
        String ref = referencia.trim();
        if (valorLimpio.isEmpty() || ref.isEmpty() || !PATRON_NUMERICO.matcher(valorLimpio).matches()) {
            return null;
        }
        double valor = parseDecimal(valorLimpio);

        Matcher rango = PATRON_RANGO.matcher(ref);
        if (rango.matches()) {
            double minimo = parseDecimal(rango.group(1));
            double maximo = parseDecimal(rango.group(2));
            if (valor < minimo) {
                return "Bajo";
            }
            if (valor > maximo) {
                return "Alto";
            }
            return "Normal";
        }

        Matcher maximo = PATRON_MAXIMO.matcher(ref);
        if (maximo.matches()) {
            return valor > parseDecimal(maximo.group(1)) ? "Alto" : "Normal";
        }

        Matcher minimo = PATRON_MINIMO.matcher(ref);
        if (minimo.matches()) {
            return valor < parseDecimal(minimo.group(1)) ? "Bajo" : "Normal";
        }

        return null;
    }

    private static double parseDecimal(String texto) {
        return Double.parseDouble(texto.replace(',', '.'));
    }

    /**
     * Borra, en orden inverso al de creación (para no chocar con las FK), todo lo que haya
     * llegado a crear esta prueba. Cada borrado va en su propio try/catch para que, si algo falla
     * o ya no existe, se sigan intentando los demás igual.
     */
    private static void limpiar(Connection con, Integer idPedidoAnalisis, Integer idPedido, Integer idPaciente,
            Integer idAnalito1, Integer idAnalito2, Integer idAnalisisTipo) {

        ejecutarBorrado(con, "pedido_analito_resultado", "id_pedido_analisis", idPedidoAnalisis);
        ejecutarBorrado(con, "pedido_analisis", "id_pedido_analisis", idPedidoAnalisis);
        ejecutarBorrado(con, "pedidos", "id_pedido", idPedido);
        if (idPaciente != null) {
            boolean ok = PacienteDAO.eliminar(con, idPaciente);
            paso(ok, "Borrar paciente de prueba (id_paciente=" + idPaciente + ")", null);
        }
        ejecutarBorrado(con, "valores_referencia", "id_analito", idAnalito1);
        ejecutarBorrado(con, "valores_referencia", "id_analito", idAnalito2);
        ejecutarBorrado(con, "analitos", "id_analito", idAnalito1);
        ejecutarBorrado(con, "analitos", "id_analito", idAnalito2);
        ejecutarBorrado(con, "analisis_tipos", "id_analisis_tipo", idAnalisisTipo);
    }

    private static void ejecutarBorrado(Connection con, String tabla, String columna, Integer id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM " + tabla + " WHERE " + columna + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            paso(true, "Borrar de " + tabla + " (" + columna + "=" + id + ")", filas + " fila(s)");
        } catch (SQLException e) {
            paso(false, "Borrar de " + tabla + " (" + columna + "=" + id + ")", e.getMessage());
        }
    }

    private static void cerrar(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
