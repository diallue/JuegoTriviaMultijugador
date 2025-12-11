package Persistencia;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GestorRanking {
    private static final String FILE_NAME = "src/main/resources/ranking.xml";
    private RankingHistorico ranking;

    public GestorRanking() {
        cargarRanking();
    }

    public void cargarRanking() {
        try {
            File file = new File(FILE_NAME);
            if (!file.exists()) {
                ranking = new RankingHistorico();
                return;
            }
            // Unmarshalling: XML -> Java
            JAXBContext context = JAXBContext.newInstance(RankingHistorico.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ranking = (RankingHistorico) unmarshaller.unmarshal(file);
            System.out.println("Ranking histórico cargado correctamente.");
        } catch (Exception e) {
            System.err.println("Error cargando ranking: " + e.getMessage());
            ranking = new RankingHistorico(); // Si falla, empezamos de cero
        }
    }

    public void guardarRanking() {
        try {
            // Marshalling: Java -> XML
            JAXBContext context = JAXBContext.newInstance(RankingHistorico.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); // XML bonito
            marshaller.marshal(ranking, new File(FILE_NAME));
            System.out.println("Ranking histórico guardado.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registrarPuntuacion(String nombre, int puntos) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        ranking.agregarPuntuacion(new Puntuacion(nombre, puntos, fecha));
        guardarRanking();
    }

    public String getRankingTexto() {
        return ranking.toStringFormato();
    }
}