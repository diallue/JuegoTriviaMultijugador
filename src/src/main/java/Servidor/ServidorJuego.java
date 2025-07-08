package Servidor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Estadisticas.EstadisticasJuego;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

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
            cargarPreguntasDesdeAPI(); // Cargar preguntas desde la API
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

    private void cargarPreguntasDesdeAPI() {
        try {
            OkHttpClient client = new OkHttpClient();
            String url = "https://opentdb.com/api.php?amount=10&type=multiple&encode=urlLegacy";
            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                System.err.println("Error en la solicitud a la API: " + response.code());
                return;
            }

            String jsonData = response.body().string();
            JSONObject json = new JSONObject(jsonData);
            JSONArray results = json.getJSONArray("results");

            preguntas.clear(); // Limpiar preguntas existentes

            for (int i = 0; i < results.length(); i++) {
                JSONObject preguntaJson = results.getJSONObject(i);
                // Decodificar el enunciado y las opciones
                String enunciado = URLDecoder.decode(preguntaJson.getString("question"), "UTF-8");
                String correcta = URLDecoder.decode(preguntaJson.getString("correct_answer"), "UTF-8");
                JSONArray incorrectas = preguntaJson.getJSONArray("incorrect_answers");

                // Crear lista de opciones y mezclarlas
                List<String> opciones = new ArrayList<>();
                opciones.add(correcta);
                for (int j = 0; j < incorrectas.length(); j++) {
                    opciones.add(URLDecoder.decode(incorrectas.getString(j), "UTF-8"));
                }
                Collections.shuffle(opciones);

                // Determinar el índice de la respuesta correcta (1-based)
                int respuestaCorrecta = opciones.indexOf(correcta) + 1;

                // Crear objeto Pregunta y añadirlo a la lista
                Pregunta pregunta = new Pregunta(enunciado, opciones, respuestaCorrecta);
                preguntas.add(pregunta);
            }

            System.out.println("Se cargaron " + preguntas.size() + " preguntas desde la API.");
        } catch (Exception e) {
            System.err.println("Error al cargar preguntas desde la API: " + e.getMessage());
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
        enviarEstadisticasIniciales();

        iniciarRonda();
    }

    private void enviarEstadisticasIniciales() {
        String estadisticas = estadisticasJuego.generarEstadisticas();
        StringBuilder estadisticasConJugadores = new StringBuilder(estadisticas);
        for (ManejadorCliente cliente : clientes) {
            String jugadorInfo = "Jugador: " + cliente.getJugador().getNombre() + " - Puntos: " + cliente.getJugador().getPuntos();
            estadisticasConJugadores.append("\n").append(jugadorInfo);
        }
        System.out.println("Estadísticas iniciales del juego:");
        System.out.println(estadisticasConJugadores);
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
        if (fallosPorJugador.containsKey(cliente) && fallosPorJugador.get(cliente) >= 1) {
            cliente.enviarMensaje("Ya fallaste esta ronda y no puedes responder nuevamente.");
            return;
        }

        Pregunta preguntaActual = preguntas.get(rondaActual);
        boolean respuestaCorrecta = preguntaActual.esRespuestaCorrecta(opcion);
        boolean esPreguntaBonus = (rondaActual % 6 == 5);

        if (respuestaCorrecta) {
            jugadoresQueRespondieron.add(cliente);
            String mensaje = "¡" + cliente.getJugador().getNombre() + " ha acertado!";
            notificarATodos(mensaje);

            if (esPreguntaBonus) {
                int aciertosBonus = preguntasBonusCorrectas.getOrDefault(cliente, 0) + 1;
                preguntasBonusCorrectas.put(cliente, aciertosBonus);
                cliente.getJugador().sumarPuntos(10);
                estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", true, 10);
                notificarEstadisticasActualizadas();
                cliente.enviarMensaje("¡Has acertado una pregunta bonus! Llevas " + aciertosBonus + " respuestas bonus correctas.");
                if (aciertosBonus >= 5) {
                    notificarATodos("¡" + cliente.getJugador().getNombre() + " ha ganado el juego al responder 5 preguntas bonus correctamente!");
                    finalizarJuego();
                    return;
                }
            }
            cliente.getJugador().sumarPuntos(10);
            estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", true, 10);
            notificarEstadisticasActualizadas();
            rondaActual++;
            if (rondaActual < preguntas.size()) {
                iniciarRonda();
            } else {
                finalizarJuego();
            }
        } else {
            fallosPorJugador.put(cliente, fallosPorJugador.getOrDefault(cliente, 0) + 1);
            cliente.enviarMensaje("¡Respuesta incorrecta! Ya no puedes responder en esta ronda.");
            estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "Equipo General", false, 0);
            notificarEstadisticasActualizadas();
            if (fallosPorJugador.size() == clientes.size()) {
                notificarATodos("¡Todos los jugadores han fallado! Avanzando a la siguiente pregunta...");
                rondaActual++;
                if (rondaActual < preguntas.size()) {
                    iniciarRonda();
                } else {
                    finalizarJuego();
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
        String estadisticas = estadisticasJuego.generarEstadisticas();
        int totalPreguntasLanzadas = rondaActual + 1;
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
        for (ManejadorCliente cliente : clientes) {
            cliente.enviarMensaje(estadisticasActualizadas.toString());
        }
        System.out.println("Estadísticas actualizadas:");
        System.out.println(estadisticasActualizadas);
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(55555);
    }
}