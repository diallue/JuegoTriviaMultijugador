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
    private int jugadoresConNombre = 0; // Contador para los jugadores que han ingresado su nombre
    private Map<ManejadorCliente, Long> tiemposRespuestas = new HashMap<>();  // Guardar tiempos de respuesta
    private Map<ManejadorCliente, Integer> respuestasRecibidas = new HashMap<>();  // Guardar respuestas

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

    public void manejarConexiones() {
        for (int i = 0; i < jugadoresEsperados; i++) {
            new Thread(() -> {
                try {
                    Socket clienteSocket = serverSocket.accept();
                    ManejadorCliente cliente = new ManejadorCliente(clienteSocket, this);
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

        // Esperar hasta que todos los jugadores estén conectados y hayan ingresado su nombre
        synchronized (this) {
            while (clientes.size() < jugadoresEsperados || jugadoresConNombre < jugadoresEsperados) {
                try {
                    wait(); // Espera hasta que todos los jugadores hayan ingresado su nombre
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Todos los jugadores están conectados y han ingresado su nombre. Iniciando el juego...");
        iniciarRonda(); // Mover esta línea aquí para que inicie después de la inscripción de todos
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

            // Enviar pregunta a los clientes
            clientes.forEach(cliente -> {
                if (esBonus) {
                    cliente.enviarMensaje("*** PREGUNTA BONUS: Esta pregunta vale 20 puntos ***");
                }
                cliente.enviarPregunta(pregunta);
            });

            // Esperar respuestas de los jugadores antes de continuar
            esperarRespuestas(pregunta, puntos, esBonus);

            rondaActual++;
        }

        if (bonusRespondidas >= 5) {
            clientes.forEach(cliente -> cliente.enviarMensaje("El juego ha terminado. ¡Has respondido correctamente 5 preguntas bonus!"));
            System.out.println("El juego ha terminado. ¡5 preguntas bonus respondidas correctamente!");
        }

        // Cerrar conexiones
        cerrarConexiones();
    }

    private void esperarRespuestas(Pregunta pregunta, int puntos, boolean esBonus) {
        synchronized (this) {
            // Esperamos a que todos los jugadores respondan antes de procesar
            int respuestasEsperadas = clientes.size();
            int respuestasRecibidasCount = 0;

            while (respuestasRecibidasCount < respuestasEsperadas) {
                try {
                    Thread.sleep(100); // Esperar un poco antes de verificar las respuestas
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Procesar todas las respuestas una vez que todos hayan respondido
            ManejadorCliente ganador = null;
            long mejorTiempo = Long.MAX_VALUE;

            for (ManejadorCliente cliente : clientes) {
                int opcion = respuestasRecibidas.get(cliente);
                if (opcion == pregunta.getRespuestaCorrecta()) {
                    cliente.getJugador().sumarPuntos(esBonus ? 20 : 10);
                    cliente.enviarMensaje("¡Respuesta correcta! Has ganado " + (esBonus ? 20 : 10) + " puntos.");
                    if (esBonus) {
                        bonusRespondidas++;
                    }
                    if (ganador == null) {
                        ganador = cliente;
                        mejorTiempo = tiemposRespuestas.get(cliente);
                    }
                } else {
                    cliente.enviarMensaje("Respuesta incorrecta.");
                }
            }

            if (ganador != null) {
                ganador.getJugador().sumarPuntos(puntos); // El ganador obtiene puntos
                ganador.enviarMensaje("¡Has respondido primero! Ganaste " + puntos + " puntos.");
            }

            // Limpiar los mapas para la siguiente ronda
            respuestasRecibidas.clear();
            tiemposRespuestas.clear();
        }
    }

    public synchronized void procesarRespuesta(ManejadorCliente cliente, int opcion, boolean esBonus) {
        // Guardamos la respuesta y el tiempo en que fue dada
        respuestasRecibidas.put(cliente, opcion);
        tiemposRespuestas.put(cliente, System.currentTimeMillis());

        // Verificar si todos los jugadores han respondido
        if (respuestasRecibidas.size() == clientes.size()) {
            notifyAll();  // Notificar que todas las respuestas han sido recibidas
        }
    }

    public synchronized void nombreIngresado() {
        jugadoresConNombre++;
        if (jugadoresConNombre == jugadoresEsperados) {
            notifyAll(); // Notificar que todos los jugadores han ingresado su nombre
        }
    }

    private void cerrarConexiones() {
        clientes.forEach(cliente -> cliente.cerrarConexion());
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(12345); // Puerto de conexión
    }
}
