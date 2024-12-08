package Interfaz;

import Estadisticas.EstadisticasJuego;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executors;

public class InterfazEstadisticas extends JFrame {
    private JTable tablaEstadisticas;
    private DefaultTableModel modeloTabla;
    private EstadisticasJuego estadisticasJuego;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public InterfazEstadisticas(String servidor, int puerto) {
        // Configuración inicial
        setTitle("Estadísticas del Juego");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Inicializar estadisticasJuego
        this.estadisticasJuego = new EstadisticasJuego();

        // Crear tabla
        modeloTabla = new DefaultTableModel(new Object[]{"Jugador", "Correctas", "Totales", "Equipo", "Puntos"}, 0);
        tablaEstadisticas = new JTable(modeloTabla);
        JScrollPane scrollPane = new JScrollPane(tablaEstadisticas);

        // Agregar componentes
        add(new JLabel("Estadísticas de los Jugadores", JLabel.CENTER), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Botón de desconexión
        JButton botonDesconectar = new JButton("Desconectar");
        botonDesconectar.addActionListener(e -> cerrarConexion());
        add(botonDesconectar, BorderLayout.SOUTH);

        // Conexión con el servidor
        conectarServidor(servidor, puerto);

        // Inicia el hilo para escuchar actualizaciones
        iniciarEscuchaActualizaciones();
    }

    /**
     * Conecta la interfaz al servidor.
     */
    private void conectarServidor(String servidor, int puerto) {
        try {
            socket = new Socket(servidor, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Conectado al servidor en " + servidor + ":" + puerto);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con el servidor. Por favor, verifica la conexión.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /**
     * Inicia un hilo para escuchar actualizaciones del servidor.
     */
    private void iniciarEscuchaActualizaciones() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    actualizarTabla(mensaje);
                }
            } catch (IOException e) {
                System.out.println("Se perdió la conexión con el servidor.");
                JOptionPane.showMessageDialog(this, "Conexión perdida con el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                cerrarConexion();
            }
        });
    }

    /**
     * Actualiza la tabla de estadísticas con datos recibidos del servidor.
     */
    private void actualizarTabla(String estadisticasTexto) {
        SwingUtilities.invokeLater(() -> {
            modeloTabla.setRowCount(0); // Limpiar tabla
            String[] lineas = estadisticasTexto.split("\n");

            // Filtramos las líneas que contienen estadísticas relevantes
            for (String linea : lineas) {
                if (linea.contains(":")) {
                    String[] partes = linea.split(":");
                    if (partes.length > 1) {
                        String jugador = partes[0].trim();
                        String[] stats = partes[1].trim().split("/");
                        if (stats.length == 2) {
                            try {
                                int correctas = Integer.parseInt(stats[0]);
                                int totales = Integer.parseInt(stats[1]);
                                String equipo = "N/A"; // Cambia si tienes esta información disponible
                                int puntos = correctas * 10; // 10 puntos por respuesta correcta
                                modeloTabla.addRow(new Object[]{jugador, correctas, totales, equipo, puntos});
                            } catch (NumberFormatException e) {
                                System.out.println("Error al convertir las estadísticas.");
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Cierra la conexión con el servidor.
     */
    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Conexión cerrada.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InterfazEstadisticas ventana = new InterfazEstadisticas("localhost", 55556);
            ventana.setVisible(true);
        });
    }
}
