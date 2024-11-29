package Servidor;

import java.io.*;
import java.net.Socket;

public class ManejadorCliente extends Thread {
    private Socket clienteSocket;
    private ServidorJuego servidor;
    private Jugador jugador;
    private BufferedReader entrada;
    private PrintWriter salida;
    private int respuesta;  // Campo para almacenar la respuesta seleccionada por el jugador

    public ManejadorCliente(Socket socket, ServidorJuego servidor) {
        this.clienteSocket = socket;
        this.servidor = servidor;
        try {
            entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
            salida = new PrintWriter(clienteSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void enviarMensaje(String mensaje) {
        salida.println(mensaje);
    }

    public void enviarPregunta(Pregunta pregunta) {
        salida.println("PREGUNTA: " + pregunta.getEnunciado());
        for (int i = 0; i < pregunta.getOpciones().size(); i++) {
            salida.println((i + 1) + ". " + pregunta.getOpciones().get(i));
        }
    }

    // Método para obtener la respuesta seleccionada por el jugador
    public int getRespuesta() {
        return respuesta;
    }

    @Override
    public void run() {
        try {
            // Enviar mensaje solicitando nombre al jugador
            salida.println("BIENVENIDO! Introduce tu nombre:");

            // Leer el nombre del jugador
            String nombre = entrada.readLine();

            // Verificar que el nombre no esté vacío
            if (nombre == null || nombre.trim().isEmpty()) {
                salida.println("Nombre no válido. Saliendo...");
                return;
            }

            jugador = new Jugador(nombre);

            // Notificar al servidor que este jugador ha ingresado su nombre
            servidor.nombreIngresado();

            // Una vez que el jugador ha sido registrado, se siguen las rondas del juego
            while (true) {
                // Leer y enviar las respuestas a cada ronda
                String respuestaStr = entrada.readLine(); // Leer la respuesta
                if (respuestaStr == null || respuestaStr.trim().isEmpty()) {
                    continue; // Si no hay respuesta, continuar esperando
                }
                respuesta = Integer.parseInt(respuestaStr); // Almacenar la respuesta seleccionada

                // Procesar la respuesta en el servidor y notificar a todos los jugadores
                servidor.procesarRespuesta(this, respuesta);  // Procesa la respuesta de inmediato
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para cerrar la conexión del cliente
    public void cerrarConexion() {
        try {
            entrada.close();
            salida.close();
            clienteSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
