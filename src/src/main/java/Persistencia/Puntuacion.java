package Persistencia;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "score")
public class Puntuacion implements Serializable, Comparable<Puntuacion> {
    private String nombre;
    private int puntos;
    private String fecha;

    public Puntuacion() {} // Constructor vacío obligatorio para JAXB

    public Puntuacion(String nombre, int puntos, String fecha) {
        this.nombre = nombre;
        this.puntos = puntos;
        this.fecha = fecha;
    }

    @XmlElement
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @XmlElement
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }

    @XmlAttribute // Guardamos la fecha como atributo del tag XML
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    // Para ordenar de mayor a menor puntuación
    @Override
    public int compareTo(Puntuacion o) {
        return Integer.compare(o.puntos, this.puntos);
    }
}