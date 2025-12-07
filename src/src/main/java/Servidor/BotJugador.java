package Servidor;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class BotJugador extends ManejadorCliente {

    public BotJugador(ServidorJuego servidor, CountDownLatch latch) {
        // Pasamos null en el socket porque el bot no tiene conexión real
        super(null, servidor, latch);
        this.jugador = new Jugador("Bot-Player");
    }

    @Override
    public void run() {
        try {
            System.out.println("Bot iniciado en el servidor (Esperando al resto de jugadores...).");

            latchInicio.await();

            // 1. Simular registro
            servidor.notificarATodos("¡" + jugador.getNombre() + " se ha unido al juego!");
            latchInicio.countDown(); // Avisar que estamos listos

            Random random = new Random();

            // 2. Bucle de juego del Bot
            while (!Thread.currentThread().isInterrupted() && servidor.isJuegoActivo()) {

                // A. Simular tiempo de pensar (entre 2 y 5 segundos)
                int tiempoPensar = 2000 + random.nextInt(3000);
                Thread.sleep(tiempoPensar);

                if (!servidor.isJuegoActivo()) {
                    break;
                }

                // B. Generar respuesta aleatoria (1-3)
                int opcion = random.nextInt(3) + 1;

                System.out.println(jugador.getNombre() + " responde opción: " + opcion);

                // C. Sincronización Crítica: Capturamos el latch de esta ronda antes de actuar
                CountDownLatch latchDeEstaRonda = servidor.getLatchRonda();

                // D. Enviar respuesta
                servidor.procesarRespuesta(this, opcion);

                // E. Esperar
                // Si el Bot falló: se queda aquí bloqueado esperando a que alguien acierte.
                // Si el Bot acertó: el servidor habrá hecho countDown() dentro de procesarRespuesta, así que este await() no bloqueará y pasará directo a la siguiente ronda.
                if (latchDeEstaRonda != null) {
                    latchDeEstaRonda.await();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Bot detenido.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enviarMensaje(String mensaje) {
    }

    @Override
    public void cerrarConexion() {
    }
}