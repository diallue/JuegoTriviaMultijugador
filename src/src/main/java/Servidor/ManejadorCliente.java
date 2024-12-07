package Servidor;

import java.io.*;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
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

    @Override
    public void run() {
        try {
            // Solicitar y recibir el nombre del jugador
            salida.println("BIENVENIDO! Introduce tu nombre:");
            String nombre = entrada.readLine();
            jugador = new Jugador(nombre);  // Asignar el nombre al jugador

            // Notificar al servidor que el jugador ha ingresado su nombre
            synchronized (servidor) {
                servidor.notify();  // Notificar al servidor que este cliente está listo
            }

            servidor.notificarATodos("¡" + nombre + " se ha unido al juego!");

            // Escuchar continuamente las respuestas del cliente
            while (!Thread.currentThread().isInterrupted()) {
                String respuestaStr = entrada.readLine();

                // Validar la entrada del cliente
                if (respuestaStr == null || respuestaStr.trim().isEmpty()) {
                    continue; // Ignorar entradas vacías
                }

                try {
                    // Convertir la respuesta a un entero
                    int opcion = Integer.parseInt(respuestaStr.trim());
                    // Procesar la respuesta
                    servidor.procesarRespuesta(this, opcion);
                } catch (NumberFormatException e) {
                    // Notificar al cliente si la entrada no es válida
                    enviarMensaje("Por favor, introduce una opción válida (1, 2 o 3).");
                }
            }
        } catch (IOException e) {
            // Manejar la desconexión del cliente
            System.out.println("El cliente " + (jugador != null ? jugador.getNombre() : "desconocido") + " se ha desconectado.");
            servidor.removerCliente(this); // Remover al cliente del servidor
        } finally {
            // Cerrar la conexión del cliente
            cerrarConexion();
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
