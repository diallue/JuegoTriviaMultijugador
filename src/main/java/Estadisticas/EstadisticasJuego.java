package Estadisticas;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EstadisticasJuego {
    public Map<String, Integer> respuestasCorrectasPorJugador = new ConcurrentHashMap<>();
    public Map<String, Integer> respuestasTotalesPorJugador = new ConcurrentHashMap<>();
    public Map<String, Integer> puntuacionesPorJugador = new ConcurrentHashMap<>();
    public Map<String, Integer> puntuacionesPorEquipo = new ConcurrentHashMap<>();

    public void registrarJugador(String jugador, String equipo) {
        respuestasCorrectasPorJugador.put(jugador, 0);
        respuestasTotalesPorJugador.put(jugador, 0);
        puntuacionesPorJugador.put(jugador, 0);
        puntuacionesPorEquipo.putIfAbsent(equipo, 0);
    }

    public synchronized void actualizarEstadisticas(String jugador, String equipo, boolean acierto, int puntos) {
        respuestasTotalesPorJugador.put(jugador, respuestasTotalesPorJugador.getOrDefault(jugador, 0) + 1);

        if (acierto) {
            respuestasCorrectasPorJugador.put(jugador, respuestasCorrectasPorJugador.getOrDefault(jugador, 0) + 1);
            puntuacionesPorJugador.put(jugador, puntuacionesPorJugador.getOrDefault(jugador, 0) + puntos);
            puntuacionesPorEquipo.put(equipo, puntuacionesPorEquipo.getOrDefault(equipo, 0) + puntos);
        }
    }

    public String generarEstadisticas() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n=== Estadísticas Generales ===\n");
        builder.append("Puntuaciones por jugador:\n");
        respuestasCorrectasPorJugador.forEach((jugador, correctas) -> {
            int totales = respuestasTotalesPorJugador.get(jugador);
            int puntos = puntuacionesPorJugador.getOrDefault(jugador, 0);
        });

        return builder.toString();
    }
}
