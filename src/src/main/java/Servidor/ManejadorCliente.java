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

    @Override
    public void run() {
        try {
            salida.println("BIENVENIDO! Introduce tu nombre:");
            String nombre = entrada.readLine();
            jugador = new Jugador(nombre);  // Aquí asignamos el nombre al jugador

            // Notificar al servidor que el jugador ha ingresado su nombre
            synchronized (servidor) {
                servidor.notify();  // Despertamos al servidor
            }

            servidor.notificarATodos("¡" + nombre + " se ha unido al juego!");

            while (true) {
                String respuestaStr = entrada.readLine();
                if (respuestaStr == null || respuestaStr.trim().isEmpty()) continue;

                int opcion = Integer.parseInt(respuestaStr);
                servidor.procesarRespuesta(this, opcion);
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
