package streamer;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;

import java.util.Random;

/**
 * @author 杨帆玉
 * @date 2019/10/30 8:04 下午
 */
public class PostStream extends RdfStream implements Runnable {

    private int streamRate;
    private String baseUri;

    public PostStream(String iri, String baseUri, int streamRate) {
        super(iri);
        this.baseUri = baseUri;
        this.streamRate = streamRate;
    }

    public PostStream(String iri) {
        super(iri);
    }

    public void run() {

        Random random = new Random();

        int senderIndex;
        int sensorIndex;
        int receiverIndex;
        int roomIndex;
        int postIndex;
        int observationIndex;

        long count = 0L;
        boolean keeprunning = true;

        long startTime, endTime;

        startTime = System.currentTimeMillis();

        while (keeprunning && (count < streamRate)) {
            try {

                senderIndex = random.nextInt(100000);
                sensorIndex = random.nextInt(100000);
                receiverIndex = random.nextInt(100000);
                roomIndex = random.nextInt(100000);
//				postIndex = random.nextInt(Integer.MAX_VALUE);
                postIndex = random.nextInt(100000);
                observationIndex = random.nextInt(Integer.MAX_VALUE);

                // sendIndex 发帖postIndex
                RdfQuadruple rdfQuadruple = new RdfQuadruple(baseUri + "person" + senderIndex, baseUri + "posts", baseUri + "post" + postIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);
                // postIndex 是给 personReceiver 的
                rdfQuadruple = new RdfQuadruple(baseUri + "post" + postIndex, baseUri + "to", baseUri + "person" + receiverIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);
                // postIndex 在 roomIndex 中
                rdfQuadruple = new RdfQuadruple(baseUri + "post" + postIndex, baseUri + "where", baseUri + "room" + roomIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);

                // 传感器观察到了
                rdfQuadruple = new RdfQuadruple(baseUri + "sensor" + sensorIndex, baseUri + "observes", baseUri + "observation" + observationIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);
                // 当前有 number 个传感器看到了
//                for(int i = 0 ; i < numberOfPerson ; i++){
                rdfQuadruple = new RdfQuadruple(baseUri + "observation" + observationIndex, baseUri + "to", baseUri+"person" + receiverIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);
//                    receiverIndex = random.nextInt(10);
//                }
                // 被观察物 在 roomIndex 中?X rdf:type ub:GraduateStudent
                rdfQuadruple = new RdfQuadruple(baseUri + "observation" + observationIndex, baseUri + "where", baseUri + "room" + roomIndex, System.currentTimeMillis());
                this.put(rdfQuadruple);

                count = count + 6;

                if (count == streamRate) {
                    endTime = System.currentTimeMillis();
                    long time = 1000L - (endTime - startTime);
                    Thread.sleep(time > 0 ? time : 0);
                    System.out.println(count +
                            " triples have been completely updated, time spent: " + (endTime - startTime));
                    startTime = System.currentTimeMillis();
                    count = 0;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
