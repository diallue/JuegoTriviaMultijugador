package Interfaz;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterfazTrivia extends JFrame {
    private JLabel preguntaLabel;
    private JButton[] botonesRespuestas;
    private JTable tablaPuntuaciones;
    private DefaultTableModel modeloTabla;

    public InterfazTrivia() {
        setTitle("Trivia Multijugador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Crear el panel superior con el marcador
        JPanel panelSuperior = new JPanel(new BorderLayout());
        JLabel tituloPuntuaciones = new JLabel("Puntuaciones", SwingConstants.CENTER);
        tituloPuntuaciones.setFont(new Font("Arial", Font.BOLD, 16));
        panelSuperior.add(tituloPuntuaciones, BorderLayout.NORTH);

        String[] columnas = {"Jugador", "Puntos"};
        modeloTabla = new DefaultTableModel(columnas, 0);
        tablaPuntuaciones = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tablaPuntuaciones);
        panelSuperior.add(scrollTabla, BorderLayout.CENTER);

        // Agregar panelSuperior al norte de la ventana
        add(panelSuperior, BorderLayout.NORTH);

        // Crear el área central para la pregunta
        preguntaLabel = new JLabel("¿Cuál es la capital de Francia?", SwingConstants.CENTER);
        preguntaLabel.setFont(new Font("Arial", Font.BOLD, 18));
        preguntaLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(preguntaLabel, BorderLayout.CENTER);

        // Crear el panel inferior con los botones de respuesta
        JPanel panelRespuestas = new JPanel(new GridLayout(2, 2, 10, 10));
        botonesRespuestas = new JButton[4];
        String[] opciones = {"Madrid", "París", "Londres", "Berlín"};

        for (int i = 0; i < botonesRespuestas.length; i++) {
            botonesRespuestas[i] = new JButton(opciones[i]);
            botonesRespuestas[i].setFont(new Font("Arial", Font.PLAIN, 14));
            panelRespuestas.add(botonesRespuestas[i]);

            // Añadir ActionListener para manejar respuestas
            botonesRespuestas[i].addActionListener(new RespuestaListener(i + 1));
        }

        panelRespuestas.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panelRespuestas, BorderLayout.SOUTH);

        // Para asegurarse de que todo se pinta correctamente
        revalidate();
        repaint();
    }

    // Método para actualizar la pregunta
    public void actualizarPregunta(String pregunta, String[] opciones) {
        preguntaLabel.setText(pregunta);
        for (int i = 0; i < botonesRespuestas.length; i++) {
            botonesRespuestas[i].setText(opciones[i]);
        }
    }

    // Método para actualizar el marcador
    public void actualizarPuntuaciones(String[][] puntuaciones) {
        modeloTabla.setRowCount(0); // Limpiar la tabla
        for (String[] fila : puntuaciones) {
            modeloTabla.addRow(fila);
        }
    }

    // Listener para manejar respuestas
    private class RespuestaListener implements ActionListener {
        private int opcion;

        public RespuestaListener(int opcion) {
            this.opcion = opcion;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Respuesta seleccionada: Opción " + opcion);
            // Aquí puedes enviar la respuesta al servidor
        }
    }

    public static void main(String[] args) {
        // Simular datos iniciales
        InterfazTrivia trivia = new InterfazTrivia();

        // Actualizar la tabla de puntuaciones con datos de ejemplo
        trivia.actualizarPuntuaciones(new String[][]{
                {"Alice", "10"},
                {"Bob", "5"}
        });

        // Actualizar la pregunta inicial
        trivia.actualizarPregunta("¿Cuál es la capital de Francia?", new String[]{"Madrid", "París", "Londres", "Berlín"});

        // Hacer visible la ventana
        trivia.setVisible(true);
    }
}
