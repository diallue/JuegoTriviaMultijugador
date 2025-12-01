package Servidor;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class ManejadorCliente implements Runnable {
    private Socket clienteSocket;
    private ServidorJuego servidor;
    private Jugador jugador;
    private BufferedReader entrada;
    private PrintWriter salida;
    private CountDownLatch latchInicio;

    public ManejadorCliente(Socket socket, ServidorJuego servidor, CountDownLatch latch) {
        this.clienteSocket = socket;
        this.servidor = servidor;
        this.latchInicio = latch;
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
            if (nombre == null) return;
            jugador = new Jugador(nombre);  // Asignar el nombre al jugador

            servidor.notificarATodos("¡" + nombre + " se ha unido al juego!");

            // AVISAMOS AL SERVIDOR QUE ESTAMOS LISTOS
            latchInicio.countDown();

            salida.println("Esperando al resto de jugadores...");

            // Escuchar continuamente las respuestas del cliente
            while (!Thread.currentThread().isInterrupted()) {
                String respuestaStr = entrada.readLine();
                if (respuestaStr == null) break;

                try {
                    // Convertir la respuesta a un entero
                    int opcion = Integer.parseInt(respuestaStr.trim());
                    // Procesar la respuesta
                    CountDownLatch latchDeEstaRonda = servidor.getLatchRonda();

                    // 2. Procesar la respuesta
                    servidor.procesarRespuesta(this, opcion);

                    // 3. Esperar en el latch que obtuvimos al principio
                    if (latchDeEstaRonda != null) {
                        latchDeEstaRonda.await();
                    }
                } catch (NumberFormatException e) {
                    // Notificar al cliente si la entrada no es válida
                    enviarMensaje("Por favor, introduce una opción válida (1, 2 o 3).");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (clienteSocket != null) clienteSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
