package dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Copia de seguridad COMPLETA de la base "laboratorio" en un archivo .sql, y restauración desde
 * ese archivo. Es lo mismo que haría {@code mysqldump} (o "Data Export" de Workbench), pero hecho
 * en Java puro con JDBC: así funciona en cualquier PC sin tener que instalar ni encontrar
 * {@code mysqldump}, y sin ninguna librería extra.
 *
 * <p>El archivo que genera es SQL común y corriente: se puede abrir con el Bloc de notas o
 * importar desde MySQL Workbench (Server &gt; Data Import &gt; "Import from Self-Contained File")
 * si algún día el sistema no abre. Y "Restaurar" acepta tanto estas copias como un dump hecho con
 * Workbench.</p>
 *
 * <p>A diferencia del respaldo en .csv, esta copia incluye TODAS las tablas (también
 * {@code usuarios}), porque tiene que poder dejar el sistema exactamente como estaba.</p>
 */
public final class CopiaSeguridadDAO {

    /** Todas las copias empiezan con esto (así el sistema sabe cuáles son suyas para listarlas o borrar las viejas). */
    public static final String PREFIJO = "Copia_laboratorio_";

    private static final int FILAS_POR_INSERT = 200;

    private CopiaSeguridadDAO() {
    }

    /** Resultado de una restauración. */
    public static final class Resultado {
        public final int sentencias;
        public final int tablas;

        Resultado(int sentencias, int tablas) {
            this.sentencias = sentencias;
            this.tablas = tablas;
        }
    }

    // ================================================================== crear copia

    /**
     * Escribe la copia en {@code carpeta}. Se escribe primero a un archivo temporal y recién al
     * final se le pone el nombre definitivo: si se corta la luz a mitad de camino, no queda una
     * copia "rota" que parezca buena.
     *
     * @param tipo "auto", "manual" o "antes-de-restaurar" (va en el nombre del archivo)
     */
    public static File crearCopia(Connection con, File carpeta, String tipo) throws SQLException, IOException {
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
        String fecha = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File destino = new File(carpeta, PREFIJO + fecha + "_" + tipo + ".sql");
        File temporal = new File(carpeta, destino.getName() + ".parcial");

        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temporal), StandardCharsets.UTF_8))) {
            String base = con.getCatalog();
            w.write("-- Copia de seguridad del sistema del laboratorio\n");
            w.write("-- Base de datos: " + base + "\n");
            w.write("-- Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "\n");
            w.write("-- Para restaurarla: Configuración > Sistema > Restaurar desde una copia,\n");
            w.write("-- o desde MySQL Workbench: Server > Data Import > Import from Self-Contained File.\n\n");
            w.write("SET NAMES utf8mb4;\n");
            w.write("SET FOREIGN_KEY_CHECKS=0;\n");
            w.write("SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';\n\n");

            List<String> tablas = new ArrayList<>();
            List<String> vistas = new ArrayList<>();
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SHOW FULL TABLES")) {
                while (rs.next()) {
                    if ("VIEW".equalsIgnoreCase(rs.getString(2))) {
                        vistas.add(rs.getString(1));
                    } else {
                        tablas.add(rs.getString(1));
                    }
                }
            }
            for (String tabla : tablas) {
                volcarTabla(con, w, tabla);
            }
            for (String vista : vistas) {
                try (Statement st = con.createStatement();
                        ResultSet rs = st.executeQuery("SHOW CREATE VIEW `" + vista + "`")) {
                    if (rs.next()) {
                        w.write("DROP VIEW IF EXISTS `" + vista + "`;\n");
                        w.write(rs.getString(2) + ";\n\n");
                    }
                }
            }
            w.write("SET FOREIGN_KEY_CHECKS=1;\n");
            w.write("-- Copia completa: " + tablas.size() + " tablas.\n");
        } catch (SQLException | IOException | RuntimeException e) {
            temporal.delete();
            throw e;
        }
        Files.move(temporal.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    private static void volcarTabla(Connection con, Writer w, String tabla) throws SQLException, IOException {
        w.write("-- ----------------------------------------------------------- " + tabla + "\n");
        w.write("DROP TABLE IF EXISTS `" + tabla + "`;\n");
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SHOW CREATE TABLE `" + tabla + "`")) {
            if (rs.next()) {
                w.write(rs.getString(2) + ";\n\n");
            }
        }
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla + "`")) {
            ResultSetMetaData md = rs.getMetaData();
            int columnas = md.getColumnCount();
            StringBuilder nombres = new StringBuilder();
            for (int i = 1; i <= columnas; i++) {
                if (i > 1) {
                    nombres.append(", ");
                }
                nombres.append('`').append(md.getColumnName(i)).append('`');
            }
            int enEsteInsert = 0;
            while (rs.next()) {
                if (enEsteInsert == 0) {
                    w.write("INSERT INTO `" + tabla + "` (" + nombres + ") VALUES\n");
                } else {
                    w.write(",\n");
                }
                StringBuilder fila = new StringBuilder("(");
                for (int i = 1; i <= columnas; i++) {
                    if (i > 1) {
                        fila.append(", ");
                    }
                    fila.append(valorSql(rs, i, md.getColumnType(i)));
                }
                fila.append(')');
                w.write(fila.toString());
                enEsteInsert++;
                if (enEsteInsert == FILAS_POR_INSERT) {
                    w.write(";\n");
                    enEsteInsert = 0;
                }
            }
            if (enEsteInsert > 0) {
                w.write(";\n");
            }
            w.write("\n");
        }
    }

    private static String valorSql(ResultSet rs, int i, int tipo) throws SQLException {
        switch (tipo) {
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB: {
                byte[] bytes = rs.getBytes(i);
                if (bytes == null) {
                    return "NULL";
                }
                if (bytes.length == 0) {
                    return "''";
                }
                StringBuilder hex = new StringBuilder("0x");
                for (byte b : bytes) {
                    hex.append(String.format("%02X", b & 0xFF));
                }
                return hex.toString();
            }
            case Types.BIT:
            case Types.BOOLEAN: {
                Object o = rs.getObject(i);
                if (o == null) {
                    return "NULL";
                }
                if (o instanceof Boolean) {
                    return ((Boolean) o) ? "1" : "0";
                }
                if (o instanceof byte[]) {
                    long n = 0;
                    for (byte b : (byte[]) o) {
                        n = (n << 8) | (b & 0xFF);
                    }
                    return String.valueOf(n);
                }
                return o.toString();
            }
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT: {
                String s = rs.getString(i);
                return s == null ? "NULL" : s;
            }
            case Types.DECIMAL:
            case Types.NUMERIC: {
                BigDecimal d = rs.getBigDecimal(i);
                return d == null ? "NULL" : d.toPlainString();
            }
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE: {
                String s = rs.getString(i);
                return s == null ? "NULL" : s;
            }
            default: {
                // Texto, fechas, horas, enum... van entre comillas. Las fechas se leen como texto
                // para que salgan tal cual están en la base (sin corrimientos de zona horaria).
                String s = rs.getString(i);
                return s == null ? "NULL" : comillas(s);
            }
        }
    }

    static String comillas(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8).append('\'');
        for (int k = 0; k < s.length(); k++) {
            char ch = s.charAt(k);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\0': sb.append("\\0"); break;
                case '\u001A': sb.append("\\Z"); break;
                default: sb.append(ch);
            }
        }
        return sb.append('\'').toString();
    }

    // ================================================================== restaurar

    /**
     * Ejecuta, una por una, las sentencias del archivo. Entiende los comentarios, los textos
     * entre comillas (con ; adentro) y el "DELIMITER" de los dumps de Workbench. Si una sentencia
     * falla, se corta y se avisa cuál fue.
     */
    public static Resultado restaurar(Connection con, File archivo) throws SQLException, IOException {
        int sentencias = 0;
        int tablas = 0;
        try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8));
                Statement st = con.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
            LectorSql lector = new LectorSql(r);
            String sql;
            while ((sql = lector.siguiente()) != null) {
                try {
                    st.execute(sql);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1065) { // "Query was empty": era solo un comentario
                        continue;
                    }
                    throw new SQLException("La restauración se detuvo en la sentencia " + (sentencias + 1) + ":\n"
                            + resumir(sql) + "\n\n" + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
                }
                sentencias++;
                if (sql.regionMatches(true, 0, "CREATE TABLE", 0, 12)) {
                    tablas++;
                }
            }
            st.execute("SET FOREIGN_KEY_CHECKS=1");
        }
        return new Resultado(sentencias, tablas);
    }

    /** Mira el principio del archivo para ver si parece una copia de la base (no cualquier .sql). */
    public static boolean pareceCopia(File archivo) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            char[] buffer = new char[200_000];
            int leidos = r.read(buffer);
            String inicio = leidos <= 0 ? "" : new String(buffer, 0, leidos).toUpperCase();
            return inicio.contains("CREATE TABLE");
        } catch (IOException e) {
            return false;
        }
    }

    private static String resumir(String sql) {
        String una = sql.replaceAll("\\s+", " ").trim();
        return una.length() > 160 ? una.substring(0, 160) + "..." : una;
    }

    /**
     * Separa un archivo .sql en sentencias. Se lee renglón por renglón (así se reconoce el
     * "DELIMITER" de los dumps de Workbench) y cada renglón carácter por carácter, sabiendo si se
     * está dentro de un texto entre comillas o de un comentario.
     */
    static final class LectorSql {
        private final BufferedReader r;
        private String delimitador = ";";
        private String linea;
        private int pos;
        private char comilla = 0;
        /** 0 = no; 1 = comentario común (se descarta); 2 = comentario /*! ... *\/ (se manda a MySQL). */
        private int comentario = 0;

        LectorSql(Reader r) {
            this.r = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
        }

        /** La próxima sentencia (sin el delimitador), o null al terminar el archivo. */
        String siguiente() throws IOException {
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (linea == null || pos >= linea.length()) {
                    String leida = r.readLine();
                    if (leida == null) {
                        String ultima = sb.toString().trim();
                        return ultima.isEmpty() ? null : ultima;
                    }
                    if (comilla == 0 && comentario == 0 && sb.toString().trim().isEmpty()) {
                        String t = leida.trim();
                        if (t.toUpperCase().startsWith("DELIMITER ")) {
                            delimitador = t.substring(10).trim();
                            linea = null;
                            continue;
                        }
                        if (t.startsWith("#")) {
                            linea = null;
                            continue;
                        }
                    }
                    linea = leida + "\n";
                    pos = 0;
                }
                char ch = linea.charAt(pos++);
                char sig = pos < linea.length() ? linea.charAt(pos) : 0;

                if (comentario != 0) {
                    if (comentario == 2) {
                        sb.append(ch);
                    }
                    if (ch == '*' && sig == '/') {
                        pos++;
                        if (comentario == 2) {
                            sb.append('/');
                        } else {
                            sb.append(' ');
                        }
                        comentario = 0;
                    }
                    continue;
                }
                if (comilla != 0) {
                    sb.append(ch);
                    if (ch == '\\' && comilla != '`' && sig != 0) {
                        sb.append(sig);
                        pos++;
                    } else if (ch == comilla) {
                        comilla = 0;
                    }
                    continue;
                }
                if (ch == '-' && sig == '-') {
                    char tercero = pos + 1 < linea.length() ? linea.charAt(pos + 1) : '\n';
                    if (Character.isWhitespace(tercero)) {
                        linea = null; // comentario hasta el final del renglón
                        sb.append('\n');
                        continue;
                    }
                }
                if (ch == '/' && sig == '*') {
                    pos++;
                    if (pos < linea.length() && linea.charAt(pos) == '!') {
                        comentario = 2;
                        sb.append("/*");
                    } else {
                        comentario = 1;
                    }
                    continue;
                }
                if (ch == '\'' || ch == '"' || ch == '`') {
                    comilla = ch;
                    sb.append(ch);
                    continue;
                }
                sb.append(ch);
                int n = delimitador.length();
                if (sb.length() >= n && sb.substring(sb.length() - n).equals(delimitador)) {
                    sb.setLength(sb.length() - n);
                    String sentencia = sb.toString().trim();
                    sb.setLength(0);
                    if (!sentencia.isEmpty()) {
                        return sentencia;
                    }
                }
            }
        }
    }
}
