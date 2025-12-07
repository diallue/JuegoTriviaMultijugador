package Cliente;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;

public class ClienteGUI extends JFrame {
    private static final Color COLOR_FONDO = new Color(30, 30, 30);
    private static final Color COLOR_PANEL = new Color(45, 45, 45);
    private static final Color COLOR_TEXTO = new Color(230, 230, 230);
    private static final Color COLOR_ACCENTO = new Color(0, 153, 204);
    private static final Color COLOR_ERROR = new Color(255, 85, 85);
    private static final Color COLOR_VERDE = new Color(40, 167, 69);
    private static final Color COLOR_CHAT = new Color(255, 105, 180);
    private static final Font FUENTE_PRINCIPAL = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 14);
    private JTabbedPane tabbedPane;
    private JTextPane panelJuego;
    private JTextPane panelStats;
    private JTextPane panelChat;
    private StyledDocument docJuego;
    private StyledDocument docStats;
    private StyledDocument docChat;
    private JTextField campoEntrada;
    private JButton botonEnviar;
    private JButton botonConectar;
    private JTextField txtHost;
    private JTextField txtPuerto;
    private SSLSocket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private boolean conectado = false;

    public ClienteGUI() {
        super("Trivial Online - Cliente Seguro");
        configurarLookAndFeel();
        configurarInterfaz();
    }

    private void configurarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("TabbedPane.background", COLOR_PANEL);
                    UIManager.put("TabbedPane.selectedBackground", COLOR_ACCENTO);
                    break;
                }
            }
        } catch (Exception e) {}
    }

    private void configurarInterfaz() {
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // 1. PANEL SUPERIOR
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelSuperior.setBackground(COLOR_PANEL);

        JLabel lblHost = new JLabel("Host:"); lblHost.setForeground(COLOR_TEXTO);
        txtHost = new JTextField("localhost", 10);
        JLabel lblPort = new JLabel("Puerto:"); lblPort.setForeground(COLOR_TEXTO);
        txtPuerto = new JTextField("55555", 5);
        botonConectar = crearBoton("Conectar", COLOR_ACCENTO);

        panelSuperior.add(lblHost); panelSuperior.add(txtHost);
        panelSuperior.add(lblPort); panelSuperior.add(txtPuerto);
        panelSuperior.add(botonConectar);
        add(panelSuperior, BorderLayout.NORTH);

        // 2. PANEL CENTRAL (Pestañas)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FUENTE_TITULO);

        // Pestaña 1: JUEGO
        panelJuego = crearTextPane();
        docJuego = panelJuego.getStyledDocument();
        tabbedPane.addTab("  Juego  ", new JScrollPane(panelJuego));

        // Pestaña 2: ESTADÍSTICAS
        panelStats = crearTextPane();
        docStats = panelStats.getStyledDocument();
        tabbedPane.addTab("  Ranking  ", new JScrollPane(panelStats));

        // Pestaña 3: CHAT
        panelChat = crearTextPane();
        docChat = panelChat.getStyledDocument();
        tabbedPane.addTab("  Sala de Chat  ", new JScrollPane(panelChat));

        add(tabbedPane, BorderLayout.CENTER);

        // 3. PANEL INFERIOR
        JPanel panelInferior = new JPanel(new BorderLayout(10, 10));
        panelInferior.setBackground(COLOR_PANEL);
        panelInferior.setBorder(new EmptyBorder(10, 10, 10, 10));

        campoEntrada = new JTextField();
        campoEntrada.setFont(FUENTE_PRINCIPAL);
        campoEntrada.setBackground(new Color(60, 60, 60));
        campoEntrada.setForeground(Color.WHITE);
        campoEntrada.setCaretColor(Color.WHITE);
        campoEntrada.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        botonEnviar = crearBoton("Enviar", COLOR_VERDE);
        botonEnviar.setEnabled(false);

        panelInferior.add(campoEntrada, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);

        // Listeners
        botonConectar.addActionListener(e -> iniciarConexion());
        ActionListener enviarAccion = e -> enviarMensaje();
        botonEnviar.addActionListener(enviarAccion);
        campoEntrada.addActionListener(enviarAccion);
    }

    private JTextPane crearTextPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setBackground(COLOR_FONDO);
        pane.setForeground(COLOR_TEXTO);
        pane.setFont(FUENTE_PRINCIPAL);
        pane.setMargin(new Insets(10, 10, 10, 10));
        return pane;
    }

    private JButton crearBoton(String texto, Color fondo) {
        JButton btn = new JButton(texto);
        btn.setFont(FUENTE_TITULO);
        btn.setBackground(fondo);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    // --- LÓGICA DE RED ---
    private void iniciarConexion() {
        String host = txtHost.getText();
        int puerto;
        try { puerto = Integer.parseInt(txtPuerto.getText()); } catch (Exception e) { return; }

        botonConectar.setEnabled(false);
        txtHost.setEnabled(false);
        txtPuerto.setEnabled(false);

        new Thread(() -> {
            try {
                agregarTexto(docJuego, "Conectando...", Color.GRAY);

                System.setProperty("javax.net.ssl.trustStore", "keystore.p12");
                System.setProperty("javax.net.ssl.trustStorePassword", "123456");

                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = (SSLSocket) sslSocketFactory.createSocket(host, puerto);
                socket.startHandshake();

                SwingUtilities.invokeLater(() -> {
                    agregarTexto(docJuego, "¡CONECTADO!\n", COLOR_ACCENTO);
                    conectado = true;
                    botonEnviar.setEnabled(true);
                    botonConectar.setText("Conectado");
                    campoEntrada.requestFocus();
                });

                salida = new PrintWriter(socket.getOutputStream(), true);
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                escucharServidor();

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    agregarTexto(docJuego, "Error: " + ex.getMessage() + "\n", COLOR_ERROR);
                    restaurarBotones();
                });
            }
        }).start();
    }

    private void escucharServidor() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                String msgFinal = mensaje;
                SwingUtilities.invokeLater(() -> procesarMensajeServidor(msgFinal));
                if (mensaje.contains("El juego ha terminado")) break;
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> agregarTexto(docJuego, "\nDesconectado.\n", COLOR_ERROR));
        } finally {
            cerrarConexion();
        }
    }

    private void procesarMensajeServidor(String msg) {
        // 1. Mensajes de CHAT
        if (msg.startsWith("[CHAT]")) {
            // Quitamos la etiqueta [CHAT] para que quede más limpio
            String contenido = msg.substring(7);
            agregarTexto(docChat, contenido + "\n", COLOR_CHAT);

            // Si no estamos en la pestaña de chat, avisamos visualmente
            if (tabbedPane.getSelectedIndex() != 2) {
                tabbedPane.setBackgroundAt(2, Color.MAGENTA);
            }
            return;
        }

        // 2. Estadísticas
        if (msg.contains("=== Estadísticas Generales ===")) {
            panelStats.setText("");
            agregarTexto(docStats, msg + "\n", Color.YELLOW);
            return;
        }
        if (msg.startsWith("Puntuaciones") || msg.startsWith("Jugador:")) {
            Color c = msg.startsWith("Jugador:") ? Color.WHITE : Color.LIGHT_GRAY;
            agregarTexto(docStats, msg + "\n", c);
            return;
        }

        // 3. Juego
        if (msg.contains("CORRECTO")) {
            // Si es respuesta incorrecta (contiene INcorrecta) -> Rojo, si no Verde
            if (msg.contains("INCORRECTO")) agregarTexto(docJuego, msg + "\n", COLOR_ERROR);
            else agregarTexto(docJuego, msg + "\n", COLOR_VERDE);
        } else if (msg.contains("RONDA")) {
            agregarTexto(docJuego, "\n------------------------------------------------\n", Color.GRAY);
            agregarTexto(docJuego, msg + "\n", Color.ORANGE);
            panelJuego.setCaretPosition(docJuego.getLength());
        } else if (msg.contains(">>>") || msg.contains("HA GANADO")) {
            agregarTexto(docJuego, msg + "\n", Color.CYAN);
        } else {
            agregarTexto(docJuego, msg + "\n", COLOR_TEXTO);
        }
    }

    private void enviarMensaje() {
        if (!conectado) return;
        String texto = campoEntrada.getText().trim();
        if (texto.isEmpty()) return;

        int tabIndex = tabbedPane.getSelectedIndex();

        // LÓGICA DE ENVÍO SEGÚN PESTAÑA
        if (tabIndex == 2) {
            // ESTAMOS EN LA PESTAÑA CHAT
            salida.println("CHAT:" + texto);
            tabbedPane.setBackgroundAt(2, null);
        } else {
            salida.println(texto);

            // Feedback visual local en el chat de juego
            try {
                Integer.parseInt(texto); // Si es un número (respuesta)
                agregarTexto(docJuego, "Tú: " + texto + "\n", new Color(100, 100, 100));
            } catch (NumberFormatException e) {
                // Si es texto
                agregarTexto(docJuego, "Tú: " + texto + "\n", new Color(100, 255, 100));
            }
        }
        campoEntrada.setText("");
        campoEntrada.requestFocus();
    }

    private void agregarTexto(StyledDocument doc, String texto, Color color) {
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, color);
        try { doc.insertString(doc.getLength(), texto, estilo); } catch (BadLocationException e) {}
    }

    private void cerrarConexion() {
        conectado = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
        SwingUtilities.invokeLater(this::restaurarBotones);
    }

    private void restaurarBotones() {
        botonConectar.setEnabled(true);
        botonEnviar.setEnabled(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClienteGUI().setVisible(true);
        });
    }
}