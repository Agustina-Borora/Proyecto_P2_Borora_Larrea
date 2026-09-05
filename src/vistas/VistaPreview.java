/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vistas;

/**
 * Ventana de prueba para ver una pantalla del paquete vistas SOLA, sin
 * conectarla todavía al resto de la app (Login, menú, etc.). No forma
 * parte del flujo real -- es solo para poder correrla y mirarla mientras
 * se la sigue armando.
 *
 * Para usarla en NetBeans: abrí este archivo y hacé clic derecho -> "Run
 * File" (o Shift+F6 con el archivo abierto). No hace falta que sea la
 * clase principal del proyecto.
 *
 * Para previsualizar otra pantalla, cambiar la línea que instancia el
 * panel más abajo y ajustar el tamaño de ventana según corresponda: las
 * pantallas de autenticación (Login, CambiarContraseña) usan una ventana
 * fija de 900x550, mientras que las pantallas del panel principal (como
 * CatalogoExamenes) necesitan una ventana más ancha, ya que varios de sus
 * componentes se estiran para ocupar el espacio disponible en lugar de
 * tener un ancho fijo -- una ventana demasiado chica o demasiado grande
 * para la pantalla que se está probando distorsiona su apariencia real.
 *
 * Esta clase puede borrarse una vez que todas las pantallas del paquete
 * vistas estén conectadas al flujo real de la aplicación.
 *
 * @author agust
 */
public class VistaPreview {

    public static void main(String[] args) {
        /* Mismo tema que usa el resto de la app (ver Login.java) */
        try {
            com.formdev.flatlaf.FlatIntelliJLaf.setup();
        } catch (Exception ex) {
            System.err.println("Fallo al inicializar FlatLaf");
        }

        java.awt.EventQueue.invokeLater(() -> {
            javax.swing.JPanel pantalla = new EscritorioDetalledeOrden();

            javax.swing.JFrame ventana = new javax.swing.JFrame(
                    "Vista previa - " + pantalla.getClass().getSimpleName());
            ventana.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            ventana.getContentPane().add(pantalla);
            ventana.setSize(980, 780); // Pantalla de panel principal, con AbsoluteLayout ancho
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
