package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Acceso a la tabla {@code password_reset_tokens}: los códigos de
 * verificación de 6 dígitos que se generan cuando alguien pide recuperar
 * su contraseña desde {@link controlador.PasswordController}.
 *
 * El código nunca se guarda en texto plano: se guarda hasheado con
 * {@link controlador.PasswordHasher}, igual que las contraseñas, así que
 * ni con acceso directo a la base se puede leer el código vigente de
 * nadie. Ver {@code sql/migracion_06_password_reset.sql} para la
 * definición de la tabla.
 */
public final class PasswordResetDAO {

    private PasswordResetDAO() {
    }

    /**
     * Crea un token nuevo para el usuario. No invalida los tokens
     * anteriores que hubiera sin usar (quedan vencidos solos con el
     * tiempo, o se ignoran porque {@link #buscarVigente} siempre trae el
     * más reciente).
     */
    public static void crear(Connection conexion, int idUsuario, String codigoHash, Timestamp expiracion) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (id_usuario, codigo_hash, fecha_creacion, fecha_expiracion, usado) "
                + "VALUES (?, ?, NOW(), ?, 0)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, codigoHash);
            ps.setTimestamp(3, expiracion);
            ps.executeUpdate();
        }
    }

    /**
     * Trae el token vigente (no usado y no vencido) más reciente del
     * usuario, o {@code null} si no tiene ninguno.
     */
    public static Token buscarVigente(Connection conexion, int idUsuario) throws SQLException {
        String sql = "SELECT id_token, codigo_hash FROM password_reset_tokens "
                + "WHERE id_usuario = ? AND usado = 0 AND fecha_expiracion > NOW() "
                + "ORDER BY fecha_creacion DESC LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Token(rs.getInt("id_token"), rs.getString("codigo_hash"));
            }
        }
    }

    /** Marca un token como usado, para que el mismo código no pueda canjearse dos veces. */
    public static void marcarUsado(Connection conexion, int idToken) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET usado = 1 WHERE id_token = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idToken);
            ps.executeUpdate();
        }
    }

    /**
     * Datos mínimos de un token vigente: su id (para poder marcarlo como
     * usado) y el hash del código (para poder verificarlo con
     * {@link controlador.PasswordHasher#verificar}).
     */
    public static final class Token {
        private final int idToken;
        private final String codigoHash;

        public Token(int idToken, String codigoHash) {
            this.idToken = idToken;
            this.codigoHash = codigoHash;
        }

        public int getIdToken() {
            return idToken;
        }

        public String getCodigoHash() {
            return codigoHash;
        }
    }
}
