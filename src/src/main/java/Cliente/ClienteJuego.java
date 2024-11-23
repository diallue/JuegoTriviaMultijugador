package Cliente;

import java.io.*;
import java.net.*;

public class ClienteJuego {
    private Socket servidorSocket;
    private BufferedReader entrada;
    private PrintWriter salida;

    public void conectarAlServidor(String host, int puerto) {
        try {
            servidorSocket = new Socket(host, puerto);
            entrada = new BufferedReader(new InputStreamReader(servidorSocket.getInputStream()));
            salida = new PrintWriter(servidorSocket.getOutputStream(), true);

            new Thread(() -> escucharServidor()).start();
            manejarEntradaUsuario();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void escucharServidor() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                System.out.println(mensaje);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manejarEntradaUsuario() {
        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            String linea;
            while ((linea = teclado.readLine()) != null) {
                salida.println(linea);
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
