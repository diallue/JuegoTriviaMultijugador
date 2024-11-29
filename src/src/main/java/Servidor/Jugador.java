package Servidor;

public class Jugador {
    private String nombre;
    private int puntos;

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.puntos = 0;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPuntos() {
        return puntos;
    }

    // Método para sumar puntos al jugador
    public void sumarPuntos(int puntosASumar) {
        this.puntos += puntosASumar;
    }
}
