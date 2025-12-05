package Persistencia;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@XmlRootElement(name = "ranking_historico")
public class RankingHistorico {

    private List<Puntuacion> listaPuntuaciones = new ArrayList<>();

    // Envolvemos la lista en una etiqueta <lista> para que el XML quede limpio
    // [cite: 1283]
    @XmlElementWrapper(name = "top_scores")
    @XmlElement(name = "jugador")
    public List<Puntuacion> getListaPuntuaciones() {
        return listaPuntuaciones;
    }

    public void setListaPuntuaciones(List<Puntuacion> listaPuntuaciones) {
        this.listaPuntuaciones = listaPuntuaciones;
    }

    public void agregarPuntuacion(Puntuacion p) {
        listaPuntuaciones.add(p);
        // Ordenar y mantener solo el Top 10
        Collections.sort(listaPuntuaciones);
        if (listaPuntuaciones.size() > 10) {
            listaPuntuaciones.remove(listaPuntuaciones.size() - 1);
        }
    }

    public String toStringFormato() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== SALÓN DE LA FAMA (TOP 10) ===\n");
        int pos = 1;
        for (Puntuacion p : listaPuntuaciones) {
            sb.append(String.format("%d. %s - %d pts (%s)\n", pos++, p.getNombre(), p.getPuntos(), p.getFecha()));
        }
        return sb.toString();
    }
}