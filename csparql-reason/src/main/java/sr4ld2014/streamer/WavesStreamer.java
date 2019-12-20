package sr4ld2014.streamer;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author 杨帆玉
 * @date 2019/10/24 3:28 下午
 */
public class WavesStreamer extends RdfStream implements Runnable {

    public static List<RDFTriple> WAVES = new ArrayList<>();

    public static String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

    private long sleepTime;
    private String baseUri;

    public WavesStreamer(String iri, String baseUri, long sleepTime) {
        super(iri);
        this.baseUri = baseUri;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        Random random = new Random();

        int senderIndex;
        int subjectIndex;
        int roomIndex;
        int postIndex;
        int numberOfPerson;

        while (true) {
            try {

                numberOfPerson = random.nextInt(3);
                senderIndex = random.nextInt(1000);
                subjectIndex = random.nextInt(5);
                roomIndex = random.nextInt(5);
//				postIndex = random.nextInt(Integer.MAX_VALUE);
                postIndex = random.nextInt(5);

                    RdfQuadruple rdfQuadruple = new RdfQuadruple(baseUri + "Student" + senderIndex, rdf + "type", baseUri + "UndergraduateStudent", System.currentTimeMillis());
//                    System.out.println(rdfQuadruple.toString());
                    this.put(rdfQuadruple);

                Thread.sleep(sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
