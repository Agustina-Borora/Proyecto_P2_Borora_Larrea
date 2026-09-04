package dao;

import javax.swing.JOptionPane;

/**
 * Centraliza el cartel de error que antes estaba copiado y pegado (con el
 * mismo título "ERROR!!!...") en los 10 DAO del sistema, 25 veces en total.
 * Cada DAO le pasa el contexto puntual (ej. "Error al listar pacientes") y
 * la excepción atrapada.
 */
public final class Mensajes {

    private Mensajes() {
    }

    public static void error(String contexto, Exception e) {
        JOptionPane.showMessageDialog(null, contexto + ": " + e.getMessage(),
                "ERROR!!!...", JOptionPane.ERROR_MESSAGE);
    }
}
