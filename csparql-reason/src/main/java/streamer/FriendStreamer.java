package streamer;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;

import java.util.Random;

/**
 * @author 杨帆玉
 * @date 2019/11/11 7:59 下午
 */
public class FriendStreamer extends RdfStream implements Runnable {

    public static final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

    private int streamRate;
    private String baseUri;
    private int transNum;

    public FriendStreamer(String iri, String baseUri, int streamRate, int transNum) {
        super(iri);
        this.baseUri = baseUri;
        this.streamRate = streamRate;
        this.transNum = transNum;
    }

    public FriendStreamer(String iri) {
        super(iri);
    }

    public void run() {

        Random random = new Random();

        int aIndex = 1;
        int bIndex = streamRate + 1;
        int transIndex = streamRate * 2 + 1;

        int count = 0;
        int count1 = 0;
        boolean keeprunning = true;

        long startTime, endTime;

        startTime = System.currentTimeMillis();

        while (keeprunning && (count < streamRate)) {
            try {
                startTime = System.currentTimeMillis();
                RdfQuadruple rdfQuadruple;
                // a 是 b 的朋友
                if (count1 == transNum) {
                    rdfQuadruple = new RdfQuadruple(baseUri + "person" + transIndex, rdfs + "friendOf", baseUri + "person" + (++transIndex), System.currentTimeMillis());
                    this.put(rdfQuadruple);
                    count1 = 0;
                } else {
                    rdfQuadruple = new RdfQuadruple(baseUri + "person" + aIndex, rdfs + "friendOf", baseUri + "person" + bIndex, System.currentTimeMillis());
                    this.put(rdfQuadruple);
                }
//                System.out.println("放入了: " + rdfQuadruple.toString());

                aIndex++;
                bIndex++;

                count++;
                count1++;

                if (count == streamRate) {
                    endTime = System.currentTimeMillis();
                    long time = 1000L - (endTime - startTime);
                    Thread.sleep(time > 0 ? time : 0);
                    System.out.println(count +
                            " triples have been completely updated, time spent: " + (endTime - startTime));
                    startTime = System.currentTimeMillis();
                    count = 0;
                    aIndex = 1;
                    bIndex = streamRate + 1;
                    transIndex = streamRate * 2 + 1;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
