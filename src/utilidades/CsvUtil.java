package utilidades;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad compartida para exportar tablas de datos a archivos .csv, sin agregar ninguna
 * librería nueva al proyecto (Mes 6: Exportación de datos / Herramientas extra -respaldo-).
 *
 * <p>Usa punto y coma como separador (lo que Excel en español espera por defecto al abrir un
 * .csv con doble clic) y agrega un BOM UTF-8 al principio del archivo para que Excel en Windows
 * reconozca bien los acentos y la "ñ".</p>
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Escribe encabezados + filas ya armadas como texto a un archivo .csv.
     */
    public static void escribir(File archivo, String[] encabezados, List<String[]> filas) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            escritor.write('﻿');
            escritor.write(armarLinea(encabezados));
            escritor.newLine();
            for (String[] fila : filas) {
                escritor.write(armarLinea(fila));
                escritor.newLine();
            }
        }
    }

    /**
     * Vuelca directamente un ResultSet completo (los nombres de columna tal cual los devuelve la
     * consulta, y cada valor convertido a texto con String.valueOf) -- pensado para el respaldo
     * de tablas completas, donde no hace falta mapear cada columna a mano.
     */
    public static void escribirDesdeResultSet(File archivo, ResultSet rs) throws SQLException, IOException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnas = meta.getColumnCount();

        String[] encabezados = new String[columnas];
        for (int i = 0; i < columnas; i++) {
            encabezados[i] = meta.getColumnLabel(i + 1);
        }

        List<String[]> filas = new ArrayList<>();
        while (rs.next()) {
            String[] fila = new String[columnas];
            for (int i = 0; i < columnas; i++) {
                Object valor = rs.getObject(i + 1);
                fila[i] = valor == null ? "" : String.valueOf(valor);
            }
            filas.add(fila);
        }

        escribir(archivo, encabezados, filas);
    }

    private static String armarLinea(String[] campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escapar(campos[i]));
        }
        return sb.toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        boolean necesitaComillas = valor.contains(";") || valor.contains("\"") || valor.contains("\n");
        String limpio = valor.replace("\"", "\"\"");
        return necesitaComillas ? "\"" + limpio + "\"" : limpio;
    }
}
