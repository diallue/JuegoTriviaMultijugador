package Servidor;

import Estadisticas.EstadisticasJuego;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorJuego {
    private ServerSocket serverSocket;
    private final List<ManejadorCliente> clientes = new ArrayList<>();
    private final List<Pregunta> preguntas = new ArrayList<>();
    private Map<ManejadorCliente, Integer> contadorBonus = new HashMap<>();
    private int rondaActual = 0;
    private int jugadoresEsperados;
    private ExecutorService poolDeHilos;
    private Map<ManejadorCliente, Integer> fallosPorJugador = new HashMap<>();
    private Set<ManejadorCliente> jugadoresQueRespondieron = new HashSet<>();
    private Map<ManejadorCliente, Integer> preguntasBonusCorrectas = new HashMap<>();
    private EstadisticasJuego estadisticasJuego = new EstadisticasJuego();

    public void iniciarServidor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
            cargarPreguntasDesdeArchivo();
            System.out.println("Servidor iniciado en el puerto " + puerto);
            configurarJuego();

            poolDeHilos = Executors.newFixedThreadPool(jugadoresEsperados);

            manejarConexiones();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (poolDeHilos != null) {
                poolDeHilos.shutdown();
            }
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
                for (int i = 0; i < 3; i++) {
                    String opcion = reader.readLine().trim();
                    if (opcion != null && !opcion.isEmpty()) {
                        opciones.add(opcion);
                    }
                }
                String respuestaCorrectaStr = reader.readLine().trim();
                if (respuestaCorrectaStr != null && !respuestaCorrectaStr.isEmpty()) {
                    try {
                        int respuestaCorrecta = Integer.parseInt(respuestaCorrectaStr);
                        if (respuestaCorrecta >= 1 && respuestaCorrecta <= 3) {
                            preguntas.add(new Pregunta(enunciado, opciones, respuestaCorrecta));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Error al parsear la respuesta correcta: " + respuestaCorrectaStr);
                    }
                }
            }
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
                    poolDeHilos.execute(cliente);
                    System.out.println("Cliente conectado: " + clienteSocket.getInetAddress() + " Total: " + clientes.size() + "/" + jugadoresEsperados);
                    cliente.enviarMensaje("¡Bienvenido! Esperando a los demás jugadores...");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        esperarNombres();
        System.out.println("Todos los jugadores están conectados. Iniciando el juego...");
        notificarATodos("¡Todos los jugadores están conectados! Preparando la primera pregunta...");

        System.out.println("Todos los jugadores están conectados. Enviando estadísticas iniciales...");
        enviarEstadisticasIniciales(); // Nuevo método para enviar estadísticas iniciales

        iniciarRonda();
    }

    private void enviarEstadisticasIniciales() {
        // Generar estadísticas iniciales
        String estadisticas = estadisticasJuego.generarEstadisticas();

        // Incluir información de los jugadores
        StringBuilder estadisticasConJugadores = new StringBuilder(estadisticas);
        for (ManejadorCliente cliente : clientes) {
            String jugadorInfo = "Jugador: " + cliente.getJugador().getNombre() + " - Puntos: " + cliente.getJugador().getPuntos();
            estadisticasConJugadores.append("\n").append(jugadorInfo);
        }

        // Mostrar estadísticas completas en consola
        System.out.println("Estadísticas iniciales del juego:");
        System.out.println(estadisticasConJugadores);

        // Enviar estadísticas a cada cliente
        for (ManejadorCliente cliente : clientes) {
            cliente.enviarMensaje(estadisticasConJugadores.toString());
        }
    }

    private synchronized void esperarNombres() {
        for (ManejadorCliente cliente : clientes) {
            while (cliente.getJugador() == null || cliente.getJugador().getNombre() == null || cliente.getJugador().getNombre().isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            estadisticasJuego.registrarJugador(cliente.getJugador().getNombre(), "Equipo General");
        }
    }

    public synchronized void iniciarRonda() {
        jugadoresQueRespondieron.clear();
        fallosPorJugador.clear();

        if (rondaActual < preguntas.size()) {
            Pregunta pregunta = preguntas.get(rondaActual);

            boolean esPreguntaBonus = ((rondaActual + 1) % 6 == 0);
            if (esPreguntaBonus) {
                notificarATodos("¡Esta es una pregunta BONUS! Los puntos valen doble.");
            }

            notificarATodos("Nueva pregunta:");
            notificarATodos("PREGUNTA: " + pregunta.getEnunciado());

            for (int i = 0; i < pregunta.getOpciones().size(); i++) {
                notificarATodos((i + 1) + ". " + pregunta.getOpciones().get(i));
            }
        } else {
            finalizarJuego();
        }
    }

    public synchronized void procesarRespuesta(ManejadorCliente cliente, int opcion) {
        // Verificar si el jugador ya falló y no puede responder nuevamente
        if (fallosPorJugador.containsKey(cliente) && fallosPorJugador.get(cliente) >= 1) {
            cliente.enviarMensaje("Ya fallaste esta ronda y no puedes responder nuevamente.");
            return;
        }

        Pregunta preguntaActual = preguntas.get(rondaActual);
        boolean respuestaCorrecta = preguntaActual.esRespuestaCorrecta(opcion);
        boolean esPreguntaBonus = (rondaActual % 6 == 5); // Suponiendo que las preguntas bonus son cada sexta

        if (respuestaCorrecta) {
            // Si el jugador acertó
            jugadoresQueRespondieron.add(cliente);
            String mensaje = "¡" + cliente.getJugador().getNombre() + " ha acertado!";
            notificarATodos(mensaje);

            if (esPreguntaBonus) {
                // Si es una pregunta bonus
                int aciertosBonus = preguntasBonusCorrectas.getOrDefault(cliente, 0) + 1;
                preguntasBonusCorrectas.put(cliente, aciertosBonus);
                cliente.getJugador().sumarPuntos(10); // Puntos por respuesta correcta
                estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", true, 10);
                notificarEstadisticasActualizadas(); // Enviar actualización a las interfaces gráficas

                // Enviar mensaje indicando cuántas preguntas bonus ha acertado
                cliente.enviarMensaje("¡Has acertado una pregunta bonus! Llevas " + aciertosBonus + " respuestas bonus correctas.");

                // Si el jugador alcanza 5 aciertos bonus
                if (aciertosBonus >= 5) {
                    notificarATodos("¡" + cliente.getJugador().getNombre() + " ha ganado el juego al responder 5 preguntas bonus correctamente!");
                    finalizarJuego();
                    return;
                }
            }
            cliente.getJugador().sumarPuntos(10); // Puntos por respuesta correcta
            estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", true, 10);
            notificarEstadisticasActualizadas(); // Enviar actualización a las interfaces gráficas
            // Avanzar a la siguiente ronda
            rondaActual++;
            if (rondaActual < preguntas.size()) {
                iniciarRonda(); // Iniciar la siguiente pregunta
            } else {
                finalizarJuego(); // Si ya no hay más preguntas, finalizar el juego
            }
        } else {
            // Si el jugador falló
            fallosPorJugador.put(cliente, fallosPorJugador.getOrDefault(cliente, 0) + 1);
            cliente.enviarMensaje("¡Respuesta incorrecta! Ya no puedes responder en esta ronda.");
            estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", false, 0);
            notificarEstadisticasActualizadas(); // Enviar actualización a las interfaces gráficas

            // Si todos los jugadores han fallado
            if (fallosPorJugador.size() == clientes.size()) {
                notificarATodos("¡Todos los jugadores han fallado! Avanzando a la siguiente pregunta...");
                rondaActual++; // Avanzar a la siguiente ronda
                if (rondaActual < preguntas.size()) {
                    iniciarRonda(); // Iniciar la siguiente pregunta
                } else {
                    finalizarJuego(); // Si ya no hay más preguntas, finalizar el juego
                }
            }
        }
    }

    private void finalizarJuego() {
        notificarATodos("El juego ha terminado. Gracias por participar.");
        notificarATodos("Resultados finales:");

        for (ManejadorCliente cliente : clientes) {
            notificarATodos(cliente.getJugador().getNombre() + ": " + cliente.getJugador().getPuntos() + " puntos");
        }

        String estadisticasFinales = estadisticasJuego.generarEstadisticas();
        System.out.println("Estadísticas generales del juego: ");
        System.out.println(estadisticasFinales);
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

    public synchronized void removerCliente(ManejadorCliente cliente) {
        if (clientes.remove(cliente)) {
            String nombre = cliente.getJugador() != null ? cliente.getJugador().getNombre() : "un jugador desconocido";
            notificarATodos("¡" + nombre + " se ha desconectado del juego!");
        }
    }

    private void notificarEstadisticasActualizadas() {
        // Generar estadísticas actualizadas
        String estadisticas = estadisticasJuego.generarEstadisticas();

        // Número total de preguntas lanzadas
        int totalPreguntasLanzadas = rondaActual + 1; // Suponiendo que rondaActual comienza desde 0

        // Incluir información de los jugadores
        StringBuilder estadisticasActualizadas = new StringBuilder(estadisticas);
        for (ManejadorCliente cliente : clientes) {
            String jugadorNombre = cliente.getJugador().getNombre();
            int respuestasCorrectas = estadisticasJuego.respuestasCorrectasPorJugador.getOrDefault(jugadorNombre, 0);

            String jugadorInfo = String.format(
                    "Jugador: %s - Puntos: %d - Respuestas correctas: %d/%d",
                    jugadorNombre,
                    cliente.getJugador().getPuntos(),
                    respuestasCorrectas,
                    totalPreguntasLanzadas
            );
            estadisticasActualizadas.append("\n").append(jugadorInfo).append("\n");
        }

        // Enviar estadísticas actualizadas a cada cliente
        for (ManejadorCliente cliente : clientes) {
            cliente.enviarMensaje(estadisticasActualizadas.toString());
        }

        // Opcional: imprimir estadísticas en la consola del servidor para verificar
        System.out.println("Estadísticas actualizadas:");
        System.out.println(estadisticasActualizadas);
    }


    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(55555);
    }
}
