package engine;

import com.triple.common.RDFTriple;
import eu.larkc.csparql.cep.esper.Producer;

/**
 * @author 杨帆玉
 * @date 2019/11/11 9:23 下午
 */
public class FriendProducer {
    private static String baseUri = "http://www.reasoning.org/ontologies#";
    public static final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

    public static int streamRate;
    public static int transNum;

    public FriendProducer(int streamRate) {
        this.streamRate = streamRate;
    }

    public static void main(String[] args) throws InterruptedException {
        streamRate = 10;
        transNum = 1;

        int count = 0;
        int count1  = 0;

        int aIndex = 1;
        int bIndex = streamRate + 1;
        int transIndex = streamRate * 2 + 1;

        boolean keeprunning = true;
        int warming = 10;

        long startTime, endTime;

        startTime = System.currentTimeMillis();

//        while (warming > 0) {
//            com.triple.common.RDFTriple rdfTriple = new com.triple.common.RDFTriple(baseUri + "person" + 0, rdfs + "friendOf", baseUri + "person" + 0);
//            Producer.sendMessagesTriple(Producer.TO_TOPIC, "1", rdfTriple, Producer.BROKER_LIST);
//
//            Thread.sleep(1000L);
//            warming--;
//        }

        while (keeprunning && (count < streamRate)) {
            try {
                startTime = System.currentTimeMillis();
                RDFTriple rdfTriple;
                // a 是 b 的朋友
                if (count1 == transNum) {
                    rdfTriple = new RDFTriple(baseUri + "person" + transIndex, rdfs + "friendOf", baseUri + "person" + (++transIndex));
                    count1 = 0;
                } else {
                    rdfTriple = new RDFTriple(baseUri + "person" + aIndex, rdfs + "friendOf", baseUri + "person" + bIndex);
                }
//                System.out.println("放入了: " + rdfTriple.toString());
                Producer.sendMessagesTriple(Producer.TO_TOPIC, "1", rdfTriple, Producer.BROKER_LIST);
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
