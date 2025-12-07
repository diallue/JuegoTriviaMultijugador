package Servidor;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class ManejadorCliente implements Runnable {
    protected Socket clienteSocket;
    protected ServidorJuego servidor;
    protected Jugador jugador;
    protected BufferedReader entrada;
    protected PrintWriter salida;
    protected CountDownLatch latchInicio;

    public ManejadorCliente(Socket socket, ServidorJuego servidor, CountDownLatch latch) {
        this.clienteSocket = socket;
        this.servidor = servidor;
        this.latchInicio = latch;

        if (socket != null) {
            try {
                entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                salida = new PrintWriter(clienteSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            jugador = new Jugador(nombre);

            servidor.notificarATodos("¡" + nombre + " se ha unido al juego!");

            // AVISAMOS AL SERVIDOR QUE ESTAMOS LISTOS
            latchInicio.countDown();

            salida.println("Esperando al resto de jugadores...");

            // Escuchar continuamente las respuestas del cliente
            while (!Thread.currentThread().isInterrupted()) {
                String respuestaStr = entrada.readLine();
                if (respuestaStr == null) break;

                // --- LÓGICA DE CHAT ---
                if (respuestaStr.startsWith("CHAT:")) {
                    // Extraemos el mensaje real
                    String textoMensaje = respuestaStr.substring(5);
                    String nombreJugador = (jugador != null) ? jugador.getNombre() : "Anónimo";

                    // Enviamos a todos con una etiqueta especial [CHAT] para que la GUI sepa dónde ponerlo
                    servidor.notificarATodos("[CHAT] " + nombreJugador + ": " + textoMensaje);
                    continue;
                }

                try {
                    int opcion = Integer.parseInt(respuestaStr.trim());

                    CountDownLatch latchDeEstaRonda = servidor.getLatchRonda();
                    servidor.procesarRespuesta(this, opcion);

                    if (latchDeEstaRonda != null) {
                        latchDeEstaRonda.await();
                    }
                } catch (NumberFormatException e) {
                    enviarMensaje("Por favor, introduce una opción válida (1-4).");
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (IOException e) {
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
