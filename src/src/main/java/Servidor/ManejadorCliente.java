package Servidor;

import java.io.*;
import java.net.Socket;

public class ManejadorCliente extends Thread {
    private Socket clienteSocket;
    private ServidorJuego servidor;
    private Jugador jugador;
    private BufferedReader entrada;
    private PrintWriter salida;

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

            // Una vez que el jugador ha sido registrado, se siguen las rondas del juego
            while (true) {
                String respuesta = entrada.readLine(); // Leer las respuestas
                if (respuesta == null || respuesta.trim().isEmpty()) {
                    continue; // Si no hay respuesta, continuar esperando
                }
                int opcion = Integer.parseInt(respuesta);
                servidor.procesarRespuesta(this, opcion, false); // Aquí puedes indicar si es una pregunta bonus
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
