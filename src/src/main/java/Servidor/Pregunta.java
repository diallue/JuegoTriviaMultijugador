package Servidor;

import java.util.List;

public class Pregunta {
    private String enunciado;
    private List<String> opciones;
    private int respuestaCorrecta;

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

    public boolean validarRespuesta(int respuesta) {
        return respuesta == respuestaCorrecta;
    }
}

