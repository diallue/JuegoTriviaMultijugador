package Servidor;

import java.io.*;
import java.net.*;

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

    public void enviarPregunta(Pregunta pregunta) {
        salida.println("PREGUNTA:" + pregunta.getEnunciado());
        for (int i = 0; i < pregunta.getOpciones().size(); i++) {
            salida.println((i + 1) + ". " + pregunta.getOpciones().get(i));
        }
    }

    @Override
    public void run() {
        try {
            salida.println("BIENVENIDO! Introduce tu nombre:");
            String nombre = entrada.readLine();
            jugador = new Jugador(nombre);
            servidor.iniciarRonda();

            while (true) {
                String respuesta = entrada.readLine();
                int opcion = Integer.parseInt(respuesta);
                servidor.procesarRespuesta(this, opcion);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

