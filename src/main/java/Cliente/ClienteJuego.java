package Cliente;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class ClienteJuego {
    private SSLSocket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public void conectarAlServidor(String host, int puerto) {
        try {
            // Conectar al servidor
            System.setProperty("javax.net.ssl.trustStore", "src/main/resources/keystore.p12");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");

            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            socket = (SSLSocket) sslSocketFactory.createSocket(host, puerto);
            socket.startHandshake();

            System.out.println("Conexión segura establecida con el servidor.");

            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Iniciar un hilo para escuchar mensajes del servidor
            Thread escuchaServidor = new Thread(() -> escucharMensajes());
            escuchaServidor.start();

            // Enviar respuestas a preguntas
            responderPreguntas();

            // Esperar a que el hilo de escucha termine
            escuchaServidor.join();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error al conectar al servidor: " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    private void escucharMensajes() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                // Si el servidor indica que el juego ha terminado, salir
                if (mensaje.contains("El juego ha terminado")) {
                    System.out.println(mensaje);
                    break;
                }
                // Mostrar mensajes del servidor
                System.out.println(mensaje);
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida con el servidor: " + e.getMessage());
        }
    }

    public void responderPreguntas() {
        try {
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
            String respuesta;

            while (!socket.isClosed()) {
                // Leer una pregunta o mensaje del servidor
                String pregunta = entrada.readLine();
                if (pregunta == null) {
                    System.out.println("El servidor ha cerrado la conexión.");
                    break;
                }

                System.out.println(pregunta);

                // Leer y enviar la respuesta del jugador
                respuesta = teclado.readLine();
                if (respuesta == null || respuesta.trim().isEmpty()) {
                    System.out.println("Respuesta vacía, intenta de nuevo.");
                    continue;
                }

                salida.println(respuesta);
            }
        } catch (IOException e) {
            System.err.println("Error al enviar respuestas: " + e.getMessage());
        }
    }

    private void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ClienteJuego cliente = new ClienteJuego();
        cliente.conectarAlServidor("localhost", 55555); // Conectar al servidor
    }
}
