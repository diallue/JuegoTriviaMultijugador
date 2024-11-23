package Servidor;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorJuego {
    private ServerSocket serverSocket;
    private List<ManejadorCliente> clientes = new ArrayList<>();
    private List<Pregunta> preguntas = new ArrayList<>();
    private int rondaActual = 0;

    public void iniciarServidor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
            cargarPreguntas();
            System.out.println("Servidor iniciado en el puerto " + puerto);
            manejarConexiones();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manejarConexiones() {
        while (true) {
            try {
                Socket clienteSocket = serverSocket.accept();
                ManejadorCliente cliente = new ManejadorCliente(clienteSocket, this);
                clientes.add(cliente);
                cliente.start();
                System.out.println("Cliente conectado.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarPreguntas() {
        preguntas.add(new Pregunta("¿Cuál es la capital de Francia?", Arrays.asList("Madrid", "París", "Londres"), 1));
        preguntas.add(new Pregunta("¿Cuánto es 2+2?", Arrays.asList("3", "4", "5"), 1));
        preguntas.add(new Pregunta("¿Qué continente es conocido como el 'viejo continente'?", Arrays.asList("Asia", "América", "Europa"), 2));
        preguntas.add(new Pregunta("¿Quién escribió 'Cien años de soledad'?", Arrays.asList("Gabriel García Márquez", "Mario Vargas Llosa", "Julio Cortázar"), 1));
        preguntas.add(new Pregunta("¿En qué año cayó el muro de Berlín?", Arrays.asList("1989", "1979", "1991"), 0));
        preguntas.add(new Pregunta("¿Cuál es el río más largo del mundo?", Arrays.asList("Amazonas", "Nilo", "Yangtsé"), 0));
        preguntas.add(new Pregunta("¿Quién pintó la Mona Lisa?", Arrays.asList("Leonardo da Vinci", "Pablo Picasso", "Vincent van Gogh"), 0));
        preguntas.add(new Pregunta("¿Qué gas componen la mayor parte de la atmósfera terrestre?", Arrays.asList("Oxígeno", "Nitrógeno", "Dióxido de carbono"), 1));
        preguntas.add(new Pregunta("¿En qué país se originó el Tango?", Arrays.asList("Brasil", "Argentina", "Chile"), 1));
        preguntas.add(new Pregunta("¿Cuál es el nombre del primer hombre que caminó sobre la Luna?", Arrays.asList("Buzz Aldrin", "Yuri Gagarin", "Neil Armstrong"), 2));
        preguntas.add(new Pregunta("¿En qué ciudad se encuentran las famosas Pirámides de Egipto?", Arrays.asList("El Cairo", "Londres", "Roma"), 0));
        preguntas.add(new Pregunta("¿Qué instrumento se utiliza para medir la presión atmosférica?", Arrays.asList("Termómetro", "Barómetro", "Higrómetro"), 1));
        preguntas.add(new Pregunta("¿Cuál es el animal terrestre más grande?", Arrays.asList("Elefante", "Rinoceronte", "Ballena azul"), 0));
        preguntas.add(new Pregunta("¿Cómo se llama el líder de la Revolución Francesa?", Arrays.asList("Napoleón Bonaparte", "Robespierre", "Luis XVI"), 1));
        preguntas.add(new Pregunta("¿Qué país tiene el mayor número de habitantes?", Arrays.asList("India", "China", "Estados Unidos"), 1));
        preguntas.add(new Pregunta("¿Qué instrumento musical tiene 88 teclas?", Arrays.asList("Guitarra", "Piano", "Violín"), 1));
        preguntas.add(new Pregunta("¿Cuál es el océano más grande?", Arrays.asList("Atlántico", "Índico", "Pacífico"), 2));
        preguntas.add(new Pregunta("¿Qué deporte se juega con un balón naranja?", Arrays.asList("Fútbol", "Baloncesto", "Vóley"), 1));
        preguntas.add(new Pregunta("¿Cuál es el planeta más cercano al sol?", Arrays.asList("Mercurio", "Venus", "Marte"), 0));
        preguntas.add(new Pregunta("¿En qué año comenzó la Segunda Guerra Mundial?", Arrays.asList("1939", "1914", "1941"), 0));
        preguntas.add(new Pregunta("¿Qué país es conocido como la 'tierra del sol naciente'?", Arrays.asList("China", "Corea del Sur", "Japón"), 2));
        preguntas.add(new Pregunta("¿Cuál es la moneda oficial de Japón?", Arrays.asList("Yuan", "Won", "Yen"), 2));
        preguntas.add(new Pregunta("¿Quién fue el primer presidente de los Estados Unidos?", Arrays.asList("George Washington", "Abraham Lincoln", "Thomas Jefferson"), 0));
        preguntas.add(new Pregunta("¿Cuántos colores tiene el arco iris?", Arrays.asList("5", "6", "7"), 2));
        preguntas.add(new Pregunta("¿Cómo se llama el río que pasa por Egipto?", Arrays.asList("Amazonas", "Nilo", "Misisipi"), 1));
        preguntas.add(new Pregunta("¿Cuál es la capital de Australia?", Arrays.asList("Sídney", "Melbourne", "Canberra"), 2));
        preguntas.add(new Pregunta("¿Qué escritor es famoso por sus obras de terror como 'Drácula'?", Arrays.asList("Mary Shelley", "Edgar Allan Poe", "Bram Stoker"), 2));
        preguntas.add(new Pregunta("¿Qué animal es el símbolo de la WWF?", Arrays.asList("León", "Panda", "Tigre"), 1));
        preguntas.add(new Pregunta("¿Qué marca de automóviles tiene como logo una estrella?", Arrays.asList("BMW", "Audi", "Mercedes-Benz"), 2));
        preguntas.add(new Pregunta("¿Cuál es el símbolo químico del oro?", Arrays.asList("Au", "Ag", "Fe"), 0));
        preguntas.add(new Pregunta("¿En qué continente está el desierto del Sahara?", Arrays.asList("Asia", "África", "América"), 1));
        preguntas.add(new Pregunta("¿Cuántos continentes hay en el mundo?", Arrays.asList("5", "6", "7"), 2));
        preguntas.add(new Pregunta("¿Cómo se llama el perro más famoso de los dibujos animados?", Arrays.asList("Snoopy", "Scooby-Doo", "Pluto"), 1));
        preguntas.add(new Pregunta("¿Cuál es el continente más pequeño?", Arrays.asList("Asia", "Europa", "Oceanía"), 2));
        preguntas.add(new Pregunta("¿Qué evento deportivo se celebra cada cuatro años, reuniendo a los mejores atletas del mundo?", Arrays.asList("Copa Mundial de Fútbol", "Juegos Olímpicos", "Super Bowl"), 1));
        preguntas.add(new Pregunta("¿Qué instrumento musical se toca con arcos?", Arrays.asList("Piano", "Violín", "Saxofón"), 1));
        preguntas.add(new Pregunta("¿Qué país inventó el sushi?", Arrays.asList("China", "Japón", "Corea del Sur"), 1));
        preguntas.add(new Pregunta("¿En qué país se encuentran las ruinas de Machu Picchu?", Arrays.asList("Perú", "México", "Chile"), 0));
        preguntas.add(new Pregunta("¿Qué tipo de animal es el canguro?", Arrays.asList("Mamífero", "Reptil", "Aves"), 0));
        preguntas.add(new Pregunta("¿Qué sistema operativo es de código abierto?", Arrays.asList("Windows", "Linux", "macOS"), 1));
        preguntas.add(new Pregunta("¿Cómo se llama el satélite natural de la Tierra?", Arrays.asList("Luna", "Marte", "Sol"), 0));
        preguntas.add(new Pregunta("¿En qué parte del cuerpo humano se encuentra el corazón?", Arrays.asList("Cabeza", "Abdomen", "Torso"), 2));
        preguntas.add(new Pregunta("¿Qué es la fotosíntesis?", Arrays.asList("El proceso de digestión de las plantas", "El proceso de respiración de los animales", "El proceso de las plantas para obtener energía del sol"), 2));
        preguntas.add(new Pregunta("¿Cuál es el océano más pequeño?", Arrays.asList("Ártico", "Atlántico", "Índico"), 0));
        preguntas.add(new Pregunta("¿Qué nombre recibe el proceso por el cual las plantas producen su propio alimento?", Arrays.asList("Respiración", "Transpiración", "Fotosíntesis"), 2));
        preguntas.add(new Pregunta("¿Qué gas se utiliza en los globos para que suban?", Arrays.asList("Hidrógeno", "Helio", "Oxígeno"), 1));
        preguntas.add(new Pregunta("¿En qué país se originó el fútbol?", Arrays.asList("Brasil", "Inglaterra", "Argentina"), 1));
        preguntas.add(new Pregunta("¿Cuál es el órgano que bombea la sangre por todo el cuerpo?", Arrays.asList("Estómago", "Corazón", "Pulmón"), 1));
        preguntas.add(new Pregunta("¿Qué metal es el más abundante en la Tierra?", Arrays.asList("Hierro", "Oro", "Plata"), 0));
        preguntas.add(new Pregunta("¿Cuál es la capital de Italia?", Arrays.asList("Roma", "Milán", "Venecia"), 0));
        preguntas.add(new Pregunta("¿Cuál es el color del sol en el espacio?", Arrays.asList("Amarillo", "Blanco", "Rojo"), 1));
        preguntas.add(new Pregunta("¿Qué animal es conocido como el rey de la selva?", Arrays.asList("León", "Elefante", "Tigre"), 0));
        preguntas.add(new Pregunta("¿En qué país nació el artista Pablo Picasso?", Arrays.asList("España", "Francia", "Italia"), 0));
    }


    public synchronized void iniciarRonda() {
        if (rondaActual >= preguntas.size()) {
            System.out.println("No hay más rondas disponibles.");
            return;
        }
        Pregunta pregunta = preguntas.get(rondaActual++);
        clientes.forEach(cliente -> cliente.enviarPregunta(pregunta));
    }

    public synchronized void procesarRespuesta(ManejadorCliente cliente, int respuesta) {
        Pregunta pregunta = preguntas.get(rondaActual - 1);
        if (pregunta.validarRespuesta(respuesta)) {
            cliente.getJugador().actualizarPuntuacion(10);
        }
    }

    public static void main(String[] args) {
        ServidorJuego servidor = new ServidorJuego();
        servidor.iniciarServidor(12345);
    }
}

