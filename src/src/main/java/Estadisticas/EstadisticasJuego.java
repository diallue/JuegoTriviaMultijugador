package Estadisticas;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EstadisticasJuego {
    private Map<String, Integer> respuestasCorrectasPorJugador = new ConcurrentHashMap<>();
    private Map<String, Integer> respuestasTotalesPorJugador = new ConcurrentHashMap<>();
    private Map<String, Integer> puntuacionesPorEquipo = new ConcurrentHashMap<>();

    public void registrarJugador(String jugador, String equipo) {
        respuestasCorrectasPorJugador.put(jugador, 0);
        respuestasTotalesPorJugador.put(jugador, 0);
        puntuacionesPorEquipo.putIfAbsent(equipo, 0);
    }

    public synchronized void actualizarEstadisticas(String jugador, String equipo, boolean acierto, int puntos) {
        respuestasTotalesPorJugador.put(jugador, respuestasTotalesPorJugador.getOrDefault(jugador, 0) + 1);

        if (acierto) {
            respuestasCorrectasPorJugador.put(jugador, respuestasCorrectasPorJugador.getOrDefault(jugador, 0) + 1);
            puntuacionesPorEquipo.put(equipo, puntuacionesPorEquipo.get(equipo) + puntos);
        }
    }

    public String generarEstadisticas() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n=== Estadísticas Generales ===\n");
        builder.append("Puntuaciones por jugador:\n");
        respuestasCorrectasPorJugador.forEach((jugador, correctas) -> {
            int totales = respuestasTotalesPorJugador.get(jugador);
            builder.append(String.format("- %s: %d/%d respuestas correctas\n", jugador, correctas, totales));
        });

        builder.append("\nPuntuaciones por equipo:\n");
        puntuacionesPorEquipo.forEach((equipo, puntos) -> {
            builder.append(String.format("- Equipo %s: %d puntos\n", equipo, puntos));
        });

        return builder.toString();
    }
}
