package Servidor;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorJuego {
    private ServerSocket serverSocket;
    private List<ManejadorCliente> clientes = new ArrayList<>();
    private List<Pregunta> preguntas = new ArrayList<>();
    private int rondaActual = 0;
    private int jugadoresEsperados;
    private Random random = new Random();
    private int bonusRespondidas = 0;

    public void iniciarServidor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
            cargarPreguntas();
            System.out.println("Servidor iniciado en el puerto " + puerto);
            configurarJuego();
            manejarConexiones();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarJuego() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("¿Cuántos jugadores participarán? (Máximo 4)");
            jugadoresEsperados = Math.min(4, scanner.nextInt());
            System.out.println("Esperando la conexión de " + jugadoresEsperados + " jugadores...");
        }
    }

    private void manejarConexiones() {
        // Crear un nuevo hilo para cada cliente que se conecta
        for (int i = 0; i < jugadoresEsperados; i++) {
            new Thread(() -> {
                try {
                    Socket clienteSocket = serverSocket.accept(); // Espera a un cliente
                    ManejadorCliente cliente = new ManejadorCliente(clienteSocket, this);

                    // Asegurarse de que se pueda agregar sin interferir en el registro de otros jugadores
                    synchronized (this) {
                        clientes.add(cliente);
                        cliente.start();
                        System.out.println("Cliente conectado. Total: " + clientes.size() + "/" + jugadoresEsperados);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // Asegurarse de que todos los jugadores se conecten antes de iniciar el juego
        while (clientes.size() < jugadoresEsperados) {
            try {
                Thread.sleep(500); // Espera hasta que todos los jugadores se conecten
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Todos los jugadores están conectados. Iniciando el juego...");
        iniciarRonda(); // Una vez que todos los jugadores están listos, iniciar el juego
    }

    private void cargarPreguntas() {
        preguntas.add(new Pregunta("¿Cuál es la capital de Francia?", Arrays.asList("Madrid", "París", "Londres"), 2));
        preguntas.add(new Pregunta("¿Cuánto es 2+2?", Arrays.asList("3", "4", "5"), 2));
        preguntas.add(new Pregunta("¿Qué color tiene el cielo?", Arrays.asList("Rojo", "Azul", "Verde"), 2));
        preguntas.add(new Pregunta("¿Cuántos días tiene un año?", Arrays.asList("365", "364", "366"), 1));
        preguntas.add(new Pregunta("¿Quién escribió 'El Quijote'?", Arrays.asList("Cervantes", "Shakespeare", "Borges"), 1));
    }

    public synchronized void iniciarRonda() {
        while (rondaActual < preguntas.size() && bonusRespondidas < 5) {
            boolean esBonus = (rondaActual + 1) % 5 == 0;
            Pregunta pregunta = preguntas.get(random.nextInt(preguntas.size()));
            int puntos = esBonus ? 20 : 10;

            // Indicar en el servidor si es una ronda de bonus
            if (esBonus) {
                System.out.println("\n*** PREGUNTA BONUS: Esta pregunta vale 20 puntos ***");
            }

            System.out.println("Enviando pregunta a los jugadores (Bonus: " + esBonus + ")");

            // Enviar mensaje especial a los clientes si es una pregunta de bonus
            clientes.forEach(cliente -> {
                if (esBonus) {
                    cliente.enviarMensaje("*** PREGUNTA BONUS: Esta pregunta vale 20 puntos ***");
                }
                cliente.enviarPregunta(pregunta);
            });

            esperarRespuestas(pregunta, puntos, esBonus);

            rondaActual++;
        }

        if (bonusRespondidas >= 5) {
            // Finalizar juego
            clientes.forEach(cliente -> cliente.enviarMensaje("El juego ha terminado. ¡Has respondido correctamente 5 preguntas bonus!"));
            System.out.println("El juego ha terminado. ¡5 preguntas bonus respondidas correctamente!");
        }

        // Cerrar conexiones
        cerrarConexiones();
    }

    // Método para esperar respuestas de los jugadores
    private void esperarRespuestas(Pregunta pregunta, int puntos, boolean esBonus) {
        // Lógica para esperar las respuestas de los jugadores
        // Al recibir la respuesta, procesar y verificar si es correcta
        clientes.forEach(cliente -> {
            if (esBonus) {
                cliente.enviarMensaje("*** PREGUNTA BONUS: Esta pregunta vale 20 puntos ***");
            }
            cliente.enviarPregunta(pregunta);
        });
    }

    public synchronized void procesarRespuesta(ManejadorCliente cliente, int opcion, boolean esBonus) {
        Pregunta pregunta = preguntas.get(random.nextInt(preguntas.size()));

        // Verificar si la respuesta es correcta
        if (opcion == pregunta.getRespuestaCorrecta()) {
            cliente.getJugador().sumarPuntos(esBonus ? 20 : 10);
            cliente.enviarMensaje("Respuesta correcta! Has ganado " + (esBonus ? 20 : 10) + " puntos.");
            if (esBonus) {
                bonusRespondidas++;
            }
        } else {
            cliente.enviarMensaje("Respuesta incorrecta.");
        }
    }

    // Método para cerrar las conexiones con los clientes
    private void cerrarConexiones() {
        clientes.forEach(cliente -> cliente.cerrarConexion());
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finalizarJuego() {
        System.out.println("El juego ha terminado. Resultados finales:");
        clientes.forEach(cliente -> {
            System.out.println(cliente.getJugador().getNombre() + ": " + cliente.getJugador().getPuntuacion() + " puntos.");
        });
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(12345); // Puerto de conexión
    }
}
