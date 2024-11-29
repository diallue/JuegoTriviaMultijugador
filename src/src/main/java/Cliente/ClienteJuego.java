package Cliente;

import java.io.*;
import java.net.*;

public class ClienteJuego {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public void conectarAlServidor(String host, int puerto) {
        try {
            socket = new Socket(host, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Escucha de mensajes del servidor
            new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        if (mensaje.contains("El juego ha terminado")) {
                            System.out.println(mensaje);  // Mensaje del servidor indicando fin del juego
                            break;
                        }
                        System.out.println(mensaje);  // Mostrar mensaje del servidor
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Enviar nombre al servidor
            System.out.println("Escribe tu nombre:");
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
            String nombre = teclado.readLine();
            salida.println(nombre);

            // Aquí puedes añadir lógica para responder a las preguntas
            responderPreguntas();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void responderPreguntas() {
        try {
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String pregunta = entrada.readLine();  // Recibe la pregunta del servidor
                System.out.println(pregunta);
                long tiempoInicio = System.currentTimeMillis();

                // Esperar respuesta del jugador
                String respuesta = teclado.readLine();
                long tiempoFin = System.currentTimeMillis();

                // Enviar respuesta al servidor
                salida.println(respuesta);
                salida.println(tiempoFin - tiempoInicio);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClienteJuego cliente = new ClienteJuego();
        cliente.conectarAlServidor("localhost", 12345); // Conectar al servidor
    }
}
