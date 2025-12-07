package Servidor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import Estadisticas.EstadisticasJuego;
import Persistencia.GestorRanking;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class ServidorJuego {
    private SSLServerSocket serverSocket;
    private final List<ManejadorCliente> clientes = new ArrayList<>();
    private final List<Pregunta> preguntas = new ArrayList<>();
    private int rondaActual = 0;
    private int jugadoresEsperados;
    private ExecutorService poolDeHilos;
    private Map<ManejadorCliente, Integer> fallosPorJugador = new HashMap<>();
    private Set<ManejadorCliente> jugadoresQueRespondieron = new HashSet<>();
    private Map<ManejadorCliente, Integer> preguntasBonusCorrectas = new HashMap<>();
    private EstadisticasJuego estadisticasJuego = new EstadisticasJuego();
    private CountDownLatch latchInicio;
    private volatile CountDownLatch latchRonda;
    private AtomicInteger contFallos = new AtomicInteger(0);
    private GestorRanking gestorRanking;
    private boolean crearBot = false;
    private volatile boolean juegoActivo = true;

    public boolean isJuegoActivo() {
        return this.juegoActivo;
    }

    public void iniciarServidor(int puerto) {
        try {
            System.setProperty("javax.net.ssl.keyStore", "keystore.p12");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");

            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

            gestorRanking = new GestorRanking();

            serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(puerto);
            //cargarPreguntasDesdeAPI(); // Cargar preguntas desde la API
            cargarPreguntasDesdeArchivo(); // Cargar preguntas desde el archivo preguntas.txt
            System.out.println("Servidor SEGURO (SSL/TLS) iniciado en el puerto " + puerto);
            configurarJuego();

            int humanosAEsperar = crearBot ? jugadoresEsperados - 1 : jugadoresEsperados;
            latchInicio = new CountDownLatch(humanosAEsperar);

            latchRonda = new CountDownLatch(1);

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

    // Metodo para que los clientes obtengan el latch actual y puedan esperar
    public synchronized CountDownLatch getLatchRonda() {
        return latchRonda;
    }

    private void configurarJuego() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("¿Cuántos jugadores HUMANOS participarán? (1-4)");
            int input = scanner.nextInt();

            if (input == 1) {
                System.out.println("Modo 1 Jugador detectado. Se añadirá un BOT como oponente.");
                jugadoresEsperados = 2; // 1 Humano + 1 Bot
                crearBot = true;
            } else {
                jugadoresEsperados = Math.min(4, input);
                crearBot = false;
            }
            System.out.println("Esperando conexión de " + (crearBot ? 1 : jugadoresEsperados) + " jugadores reales...");
        }
    }

    /*private void cargarPreguntasDesdeAPI() {
        try {
            OkHttpClient client = new OkHttpClient();
            String url = "https://opentdb.com/api.php?amount=50&type=multiple&encode=urlLegacy&lang=es";
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
    }*/

    private void cargarPreguntasDesdeArchivo() {
        preguntas.clear();
        File archivo = new File("C:\\Users\\diego\\IdeaProjects\\JuegoTriviaMultijugador\\src\\src\\main\\java\\Servidor\\preguntas.txt");

        if (!archivo.exists()) {
            System.err.println("Error: No se encuentra el archivo preguntas.txt en la raíz del proyecto.");
            return;
        }

        // Usamos FileReader envuelto en BufferedReader para leer línea a línea de forma eficiente
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            // Leemos la primera línea (enunciado) para empezar el bucle
            while ((linea = br.readLine()) != null) {
                // 1. Enunciado
                String enunciado = linea.trim();
                if (enunciado.isEmpty()) continue;

                // 2. Leer las 3 Opciones
                List<String> opciones = new ArrayList<>();
                String op1 = br.readLine();
                String op2 = br.readLine();
                String op3 = br.readLine();

                if (op1 == null || op2 == null || op3 == null) break;

                opciones.add(op1);
                opciones.add(op2);
                opciones.add(op3);

                // 3. Leer el índice de la respuesta correcta
                String respStr = br.readLine();
                if (respStr == null) break;

                try {
                    int correcta = Integer.parseInt(respStr.trim());
                    // Creamos y guardamos la pregunta
                    preguntas.add(new Pregunta(enunciado, opciones, correcta));
                } catch (NumberFormatException e) {
                    System.err.println("Error de formato en el archivo: se esperaba un número para la respuesta.");
                }
            }
            System.out.println("Se cargaron " + preguntas.size() + " preguntas desde el fichero local.");

        } catch (IOException e) {
            System.err.println("Error de i/o al leer el archivo: " + e.getMessage());
        }
    }

    private void manejarConexiones() {
        // Si hay bot, el socket solo debe esperar (Total - 1) conexiones reales
        int conexionesReales = crearBot ? jugadoresEsperados - 1 : jugadoresEsperados;

        while (clientes.size() < conexionesReales) {
            try {
                Socket clienteSocket = serverSocket.accept();
                ManejadorCliente cliente = new ManejadorCliente(clienteSocket, this, latchInicio);
                synchronized (this) {
                    clientes.add(cliente);
                    poolDeHilos.execute(cliente);
                }
                System.out.println("Cliente conectado: " + clienteSocket.getInetAddress() + " Total: " + clientes.size() + "/" + jugadoresEsperados);
            } catch (IOException e) { e.printStackTrace(); }
        }

        if (crearBot) {
            BotJugador bot = new BotJugador(this, latchInicio);
            synchronized (this) {
                clientes.add(bot);
                poolDeHilos.execute(bot); // Ejecutamos el bot en un hilo del pool
            }
            System.out.println("Bot añadido a la partida.");
        }

        try {
            latchInicio.await();
            System.out.println("¡Todos listos! Iniciando...");
            enviarEstadisticasIniciales();
            enviarPreguntaActual();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public synchronized void enviarPreguntaActual() {
        if (rondaActual < preguntas.size()) {
            Pregunta p = preguntas.get(rondaActual);
            contFallos.set(0);
            latchRonda = new CountDownLatch(1);

            boolean esBonus = (rondaActual + 1) % 5 == 0;
            String tituloRonda = "RONDA " + (rondaActual + 1);

            if (esBonus) {
                tituloRonda += " ¡¡PREGUNTA BONUS!! (x2 Puntos)";
            }

            notificarATodos("\n--- " + tituloRonda + " ---");
            notificarATodos(p.getEnunciado());

            List<String> opciones = p.getOpciones();
            for (int i = 0; i < opciones.size(); i++) {
                notificarATodos((i + 1) + ". " + opciones.get(i));
            }

            notificarATodos("Responde ahora (1-" + opciones.size() + "):");
        } else {
            finalizarJuego();
        }
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

    public void procesarRespuesta(ManejadorCliente cliente, int opcion) {
        synchronized(this) {
            if (latchRonda.getCount() == 0) return;

            Pregunta preguntaActual = preguntas.get(rondaActual);
            boolean acierto = preguntaActual.esRespuestaCorrecta(opcion);

            // CALCULAR SI ES BONUS Y LOS PUNTOS
            // Ronda 4 (5ª pregunta), 9 (10ª), etc. son bonus.
            boolean esBonus = (rondaActual + 1) % 5 == 0;
            int puntosASumar = esBonus ? 20 : 10;

            if (acierto) {
                // --- ACIERTO ---

                // 1. Sumamos los puntos calculados (10 o 20)
                cliente.getJugador().sumarPuntos(puntosASumar);

                // 2. Actualizamos estadísticas con los puntos correctos
                estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "General", true, puntosASumar);

                // 3. Lógica de Bonus para ganar
                if (esBonus) {
                    int bonusAcertados = preguntasBonusCorrectas.getOrDefault(cliente, 0) + 1;
                    preguntasBonusCorrectas.put(cliente, bonusAcertados);

                    cliente.enviarMensaje("¡CORRECTO! +" + puntosASumar + " puntos. (Bonus: " + bonusAcertados + "/5)");

                    if (bonusAcertados >= 5) {
                        notificarATodos("\n>>> ¡" + cliente.getJugador().getNombre() + " HA GANADO EL JUEGO POR BONUS!");
                        finalizarJuego();
                        return; // Salimos para no avanzar ronda
                    }
                } else {
                    cliente.enviarMensaje("¡CORRECTO! +" + puntosASumar + " puntos.");
                }

                notificarATodos("\n>>> ¡" + cliente.getJugador().getNombre() + " HA ACERTADO!");
                avanzarRonda();

            } else {
                // --- FALLO ---
                cliente.enviarMensaje("INCORRECTO. Debes esperar a que termine la ronda.");
                estadisticasJuego.actualizarEstadisticas(cliente.getJugador().getNombre(), "General", false, 0);

                int fallos = contFallos.incrementAndGet();

                if (fallos >= clientes.size()) {
                    notificarATodos("\n>>> NADIE ha acertado. La respuesta era: " + preguntaActual.getOpciones().get(preguntaActual.getRespuestaCorrecta() - 1));
                    avanzarRonda();
                }
            }
        }
    }

    private void avanzarRonda() {
        // 1. Abrimos para liberar a los que fallaron y estaban esperando
        latchRonda.countDown();

        // 2. Actualizamos estadísticas globales
        notificarEstadisticasActualizadas();

        // 3. Preparamos siguiente ronda
        rondaActual++;
        enviarPreguntaActual(); // Esto crea un nuevo latch para la siguiente
    }

    private void finalizarJuego() {
        juegoActivo = false;
        notificarATodos("El juego ha terminado. Gracias por participar.");

        // 1. Mostrar resultados de la PARTIDA ACTUAL y GUARDAR en XML
        notificarATodos("Resultados finales de esta sesión:");

        for (ManejadorCliente cliente : clientes) {
            // Mostrar puntuación de la sesión
            notificarATodos(cliente.getJugador().getNombre() + ": " + cliente.getJugador().getPuntos() + " puntos");

            // Guardamos la puntuación en el histórico si tiene puntos
            if (cliente.getJugador() != null && cliente.getJugador().getPuntos() > 0) {
                gestorRanking.registrarPuntuacion(cliente.getJugador().getNombre(), cliente.getJugador().getPuntos()
                );
            }
        }

        // 2. Enviar el RANKING HISTÓRICO a los clientes
        String textoHistorico = gestorRanking.getRankingTexto();

        notificarATodos("\n=== Estadísticas Generales ===\n" + textoHistorico);

        // 3. Logs del servidor
        System.out.println("Estadísticas guardadas en ranking.xml");

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
        System.out.println(mensaje);

        for (ManejadorCliente cliente : clientes) {
            cliente.enviarMensaje(mensaje);
        }
    }

    public synchronized void removerCliente(ManejadorCliente cliente) {
        if (clientes.remove(cliente)) {
            String nombre = (cliente.getJugador() != null) ? cliente.getJugador().getNombre() : "Desconocido";
            notificarATodos("¡" + nombre + " abandonó la partida!");

            boolean quedanHumanos = false;
            for (ManejadorCliente c : clientes) {
                // Si encontramos al menos uno que NO sea un Bot, el juego sigue
                if (!(c instanceof BotJugador)) {
                    quedanHumanos = true;
                    break;
                }
            }

            if (!quedanHumanos) {
                System.out.println("No quedan jugadores humanos. Cerrando partida y desconectando al Bot.");
                finalizarJuego(); // Esto cierra el pool de hilos y mata al Bot
                return;
            }

            if (clientes.isEmpty()) {
                System.out.println("Todos se fueron. Cerrando.");
            } else {
                if (latchRonda.getCount() > 0) { // Si la ronda sigue viva
                    int fallosActuales = contFallos.get();

                    if (fallosActuales >= clientes.size()) {
                        notificarATodos("Todos los jugadores restantes han fallado o abandonado.");
                        avanzarRonda();
                    }
                }
            }
        }
    }

    private void notificarEstadisticasActualizadas() {
        String estadisticas = estadisticasJuego.generarEstadisticas();
        int totalPreguntasLanzadas = rondaActual + 1;
        StringBuilder estadisticasActualizadas = new StringBuilder(estadisticas);
        for (ManejadorCliente cliente : clientes) {
            String jugadorNombre = cliente.getJugador().getNombre();
            int respuestasCorrectas = estadisticasJuego.respuestasCorrectasPorJugador.getOrDefault(jugadorNombre, Integer.valueOf(0));
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