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
                            break;  // Termina el juego
                        } else if (mensaje.startsWith("*** PREGUNTA BONUS")) {
                            System.out.println("\n" + mensaje + "\n"); // Mensaje especial para pregunta bonus
                        } else if (mensaje.startsWith("PREGUNTA:")) {
                            System.out.println(mensaje);
                        } else {
                            System.out.println(mensaje);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Enviar mensajes al servidor
            enviarMensajes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarMensajes() {
        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            String mensaje;
            while ((mensaje = teclado.readLine()) != null) {
                salida.println(mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClienteJuego cliente = new ClienteJuego();
        cliente.conectarAlServidor("localhost", 12345);
    }
}
