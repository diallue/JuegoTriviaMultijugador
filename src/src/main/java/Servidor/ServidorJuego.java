package Servidor;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorJuego {
    private ServerSocket serverSocket;
    private final List<ManejadorCliente> clientes = new ArrayList<>();
    private final List<Pregunta> preguntas = new ArrayList<>();
    private Map<ManejadorCliente, Integer> contadorBonus = new HashMap<>();
    private int rondaActual = 0;
    private int jugadoresEsperados;

    public void iniciarServidor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
            cargarPreguntasDesdeArchivo();
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

    private void cargarPreguntasDesdeArchivo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\Public\\Documents\\IDEAProjects\\JuegoTriviaMultijugador\\src\\src\\main\\java\\Servidor\\preguntas.txt"))) {
            String enunciado;
            while ((enunciado = reader.readLine()) != null && !enunciado.trim().isEmpty()) {
                List<String> opciones = new ArrayList<>();
                // Leemos las 3 opciones
                for (int i = 0; i < 3; i++) {
                    String opcion = reader.readLine().trim();
                    if (opcion != null && !opcion.isEmpty()) {
                        opciones.add(opcion);
                    } else {
                        System.out.println("Error en el formato de la opción. Faltan opciones para la pregunta: " + enunciado);
                        break;
                    }
                }

                // Leemos la respuesta correcta
                String respuestaCorrectaStr = reader.readLine().trim();
                if (respuestaCorrectaStr != null && !respuestaCorrectaStr.isEmpty()) {
                    try {
                        int respuestaCorrecta = Integer.parseInt(respuestaCorrectaStr);
                        if (respuestaCorrecta >= 1 && respuestaCorrecta <= 3) {
                            preguntas.add(new Pregunta(enunciado, opciones, respuestaCorrecta));
                        } else {
                            System.out.println("Respuesta correcta fuera de rango (1-3) para la pregunta: " + enunciado);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error al parsear la respuesta correcta: " + respuestaCorrectaStr);
                    }
                } else {
                    System.out.println("Respuesta correcta no encontrada para la pregunta: " + enunciado);
                }
            }

            System.out.println("Total de preguntas cargadas: " + preguntas.size());  // Verificación de las preguntas cargadas

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void manejarConexiones() {
        while (clientes.size() < jugadoresEsperados) {
            try {
                Socket clienteSocket = serverSocket.accept();
                ManejadorCliente cliente = new ManejadorCliente(clienteSocket, this);
                synchronized (this) {
                    clientes.add(cliente);
                    cliente.start();
                    System.out.println("Cliente conectado: " + clienteSocket.getInetAddress() +
                            ". Total: " + clientes.size() + "/" + jugadoresEsperados);
                    cliente.enviarMensaje("¡Bienvenido! Esperando a los demás jugadores...");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Ahora que todos los jugadores se han conectado, esperamos a que todos ingresen su nombre
        esperarNombres();

        System.out.println("Todos los jugadores están conectados. Iniciando el juego...");
        notificarATodos("¡Todos los jugadores están conectados! Preparando la primera pregunta...");
        iniciarRonda();
    }

    private synchronized void esperarNombres() {
        // Asegurarse de que el jugador ha ingresado su nombre
        for (ManejadorCliente cliente : clientes) {
            while (cliente.getJugador() == null || cliente.getJugador().getNombre() == null || cliente.getJugador().getNombre().isEmpty()) {
                try {
                    wait();  // Esperamos hasta que el jugador ingrese su nombre
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void iniciarRonda() {
        // Si hemos llegado al final de la lista de preguntas, termina el juego.
        if (rondaActual < preguntas.size()) {
            Pregunta pregunta = preguntas.get(rondaActual);
            boolean esPreguntaBonus = (rondaActual % 6 == 5);  // Cada sexta pregunta es bonus

            // Notificar la nueva pregunta
            notificarATodos("Nueva pregunta:");
            notificarATodos("PREGUNTA: " + pregunta.getEnunciado());

            // Enviar las opciones de respuesta
            for (int i = 0; i < pregunta.getOpciones().size(); i++) {
                notificarATodos((i + 1) + ". " + pregunta.getOpciones().get(i));
            }

            // Si es una pregunta bonus, se notifica.
            if (esPreguntaBonus) {
                notificarATodos("¡Esta es una pregunta bonus!");
            }
        } else {
            // Si no hay más preguntas, finaliza el juego.
            finalizarJuego();
        }
    }

    public synchronized void procesarRespuesta(ManejadorCliente cliente, int opcion) {
        Pregunta preguntaActual = preguntas.get(rondaActual);
        boolean esPreguntaBonus = (rondaActual % 6 == 5);  // Verificar si es una pregunta bonus
        boolean respuestaCorrecta = preguntaActual.esRespuestaCorrecta(opcion);

        // Verificar si la respuesta es correcta
        if (respuestaCorrecta) {
            // Mensaje para notificar que el jugador acertó
            String mensaje = "¡" + cliente.getJugador().getNombre() + " ha acertado!";
            notificarATodos(mensaje);  // Notificar a todos los jugadores que alguien ha acertado

            // Si es una pregunta bonus, incrementar los puntos extra
            if (esPreguntaBonus) {
                cliente.getJugador().sumarPuntos(20);  // Puntos extra por respuesta correcta en una pregunta bonus
                contadorBonus.put(cliente, contadorBonus.getOrDefault(cliente, 0) + 1);

                // Si el jugador ha respondido 5 preguntas bonus correctamente
                if (contadorBonus.get(cliente) >= 5) {
                    notificarATodos(cliente.getJugador().getNombre() + " ha respondido 5 preguntas bonus correctamente y ha ganado el juego.");
                    finalizarJuego();  // Termina el juego cuando un jugador responde 5 preguntas bonus correctamente
                    return;  // Detener el procesamiento
                }
            } else {
                // Puntos normales por una respuesta correcta en una pregunta regular
                cliente.getJugador().sumarPuntos(10);
            }

            // Incrementar la ronda actual
            rondaActual++;

            // Enviar la siguiente pregunta a todos los jugadores
            iniciarRonda();
        } else {
            // Si la respuesta es incorrecta, solo al jugador que ha fallado
            cliente.enviarMensaje("¡Respuesta incorrecta! Sigue intentando.");
        }
    }

    private void finalizarJuego() {
        notificarATodos("El juego ha terminado. Gracias por participar.");
        notificarATodos("Resultados finales:");
        for (ManejadorCliente cliente : clientes) {
            notificarATodos(cliente.getJugador().getNombre() + ": " + cliente.getJugador().getPuntos() + " puntos");
        }
        cerrarConexiones();
    }

    private void cerrarConexiones() {
        for (ManejadorCliente cliente : clientes) {
            cliente.cerrarConexion();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized void notificarATodos(String mensaje) {
        for (ManejadorCliente cliente : clientes) {
            cliente.enviarMensaje(mensaje);
        }
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(55555);
    }
}
