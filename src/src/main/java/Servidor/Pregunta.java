package Servidor;

import java.util.List;

public class Pregunta {
    private String enunciado;
    private List<String> opciones;
    private int respuestaCorrecta; // Índice de la respuesta correcta

    public Pregunta(String enunciado, List<String> opciones, int respuestaCorrecta) {
        this.enunciado = enunciado;
        this.opciones = opciones;
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public List<String> getOpciones() {
        return opciones;
    }

    public int getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    // Método para sumar puntos al jugador si la respuesta es correcta
    public boolean esRespuestaCorrecta(int opcionElegida) {
        return opcionElegida == respuestaCorrecta;
    }
}
