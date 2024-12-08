package Servidor;

import Estadisticas.EstadisticasJuego;
import Interfaz.InterfazEstadisticas;

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
    private final List<Socket> interfacesConectadas = new ArrayList<>();

    public void iniciarServidor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
            cargarPreguntasDesdeArchivo();
            System.out.println("Servidor iniciado en el puerto " + puerto);
            configurarJuego();

            poolDeHilos = Executors.newFixedThreadPool(jugadoresEsperados);

            manejarConexionesInterfaces();
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
        iniciarRonda();
    }

    private void manejarConexionesInterfaces() {
        new Thread(() -> {
            try (ServerSocket serverSocketInterfaces = new ServerSocket(55556)) { // Puerto dedicado para las interfaces gráficas
                System.out.println("Esperando conexiones de interfaces gráficas en el puerto 55556...");
                while (true) {
                    Socket interfazSocket = serverSocketInterfaces.accept();
                    synchronized (interfacesConectadas) {
                        interfacesConectadas.add(interfazSocket);
                    }
                    System.out.println("Interfaz gráfica conectada desde: " + interfazSocket.getInetAddress());
                    enviarEstadisticasIniciales(interfazSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void enviarEstadisticasIniciales(Socket interfazSocket) {
        try {
            PrintWriter salida = new PrintWriter(interfazSocket.getOutputStream(), true);
            String estadisticas = estadisticasJuego.generarEstadisticas();
            salida.println(estadisticas);
        } catch (IOException e) {
            e.printStackTrace();
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

            cliente.getJugador().sumarPuntos(10); // Puntos por respuesta correcta
            estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", true, 10);
            notificarEstadisticasActualizadas(); // Enviar actualización a las interfaces gráficas

            if (esPreguntaBonus) {
                // Si es una pregunta bonus
                int aciertosBonus = preguntasBonusCorrectas.getOrDefault(cliente, 0) + 1;
                preguntasBonusCorrectas.put(cliente, aciertosBonus);

                // Enviar mensaje indicando cuántas preguntas bonus ha acertado
                cliente.enviarMensaje("¡Has acertado una pregunta bonus! Llevas " + aciertosBonus + " respuestas bonus correctas.");

                // Si el jugador alcanza 5 aciertos bonus
                if (aciertosBonus >= 5) {
                    notificarATodos("¡" + cliente.getJugador().getNombre() + " ha ganado el juego al responder 5 preguntas bonus correctamente!");
                    finalizarJuego();
                    return;
                }
            }

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

        // Mostrar puntos de todos los jugadores
        for (ManejadorCliente cliente : clientes) {
            notificarATodos(cliente.getJugador().getNombre() + ": " + cliente.getJugador().getPuntos() + " puntos");
        }

        // Generar estadísticas finales y mostrarlas
        String estadisticasFinales = estadisticasJuego.generarEstadisticas();
        notificarATodos("Estadísticas generales del juego: ");
        notificarATodos(estadisticasFinales);

        // Si es necesario enviar estas estadísticas a las interfaces gráficas
        notificarEstadisticasActualizadas(); // Esto se encargará de actualizar las interfaces gráficas con los puntos finales.

        // Cerrar conexiones después de que el juego haya terminado
        cerrarConexiones();
    }

    private void cerrarConexionesInterfaces() {
        synchronized (interfacesConectadas) {
            Iterator<Socket> iterador = interfacesConectadas.iterator();
            while (iterador.hasNext()) {
                Socket socket = iterador.next();
                try {
                    socket.close();
                    iterador.remove(); // Eliminar después de cerrar
                } catch (IOException e) {
                    System.out.println("Error cerrando conexión con una interfaz gráfica.");
                }
            }
        }
    }

    private void cerrarConexiones() {
        for (ManejadorCliente cliente : clientes) {
            cliente.cerrarConexion();
        }
        cerrarConexionesInterfaces(); // Cerrar conexiones con interfaces gráficas
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
        String estadisticas = estadisticasJuego.generarEstadisticas();
        System.out.println("Enviando estadísticas a interfaces gráficas: " + estadisticas); // Añadir esta línea
        synchronized (interfacesConectadas) {
            Iterator<Socket> iterador = interfacesConectadas.iterator();
            while (iterador.hasNext()) {
                Socket socket = iterador.next();
                try {
                    PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                    salida.println(estadisticas);
                    salida.flush();
                } catch (IOException e) {
                    System.out.println("Se perdió la conexión con una interfaz gráfica.");
                    iterador.remove(); // Eliminar la interfaz desconectada
                }
            }
        }
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(55555);
    }
}
